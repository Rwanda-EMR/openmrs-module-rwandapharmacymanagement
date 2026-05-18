/*
Manual duplicate pharmacy item cleanup.

Purpose:
  Merge active rpm_item duplicates that have the same normalized name.

Canonical choice:
  For every duplicate-name group, the script chooses MIN(item_id) as the
  preferred/canonical item. All non-preferred item references are moved to it.

Important:
  1. Run the preview queries first.
  2. Take a database backup before running the cleanup section.
  3. This is intentionally NOT a Liquibase migration.
  4. This targets duplicates by lower(trim(name)). Review the preview output if
     your data contains two legitimately different items with the same name.
*/

/* -------------------------------------------------------------------------- */
/* PREVIEW ONLY                                                               */
/* -------------------------------------------------------------------------- */

/* Duplicate groups and chosen canonical item. */
select
    lower(trim(name)) as normalized_name,
    min(item_id) as canonical_item_id,
    count(*) as item_count,
    group_concat(item_id order by item_id) as item_ids,
    group_concat(coalesce(code, '') order by item_id separator ' | ') as codes,
    group_concat(coalesce(program, '') order by item_id separator ' | ') as programs
from rpm_item
where voided = 0
group by lower(trim(name))
having count(*) > 1
order by normalized_name;

/* Stock affected by duplicate groups. */
select
    lower(trim(i.name)) as normalized_name,
    i.item_id,
    i.name,
    i.code,
    i.program,
    ib.pharmacy_location_id,
    pl.name as pharmacy,
    ib.batch_number,
    ib.expiry_date,
    ib.quantity_on_hand,
    ib.unit_price
from rpm_item i
join (
    select lower(trim(name)) as normalized_name
    from rpm_item
    where voided = 0
    group by lower(trim(name))
    having count(*) > 1
) dup on dup.normalized_name = lower(trim(i.name))
left join rpm_inventory_balance ib on ib.item_id = i.item_id and ib.voided = 0
left join rpm_pharmacy_location pl on pl.pharmacy_location_id = ib.pharmacy_location_id
where i.voided = 0
order by normalized_name, i.item_id, pharmacy, ib.batch_number;

/* -------------------------------------------------------------------------- */
/* CLEANUP                                                                    */
/* -------------------------------------------------------------------------- */

start transaction;

drop temporary table if exists tmp_rpm_duplicate_item_group;
create temporary table tmp_rpm_duplicate_item_group as
select
    lower(trim(name)) as normalized_name,
    min(item_id) as canonical_item_id
from rpm_item
where voided = 0
group by lower(trim(name))
having count(*) > 1;

drop temporary table if exists tmp_rpm_duplicate_item_map;
create temporary table tmp_rpm_duplicate_item_map as
select
    i.item_id as duplicate_item_id,
    g.canonical_item_id,
    g.normalized_name
from rpm_item i
join tmp_rpm_duplicate_item_group g
    on g.normalized_name = lower(trim(i.name))
where i.voided = 0
  and i.item_id <> g.canonical_item_id;

/* Inventory balances need a merge, not just item_id rewiring, because some
   code expects one balance row per item + pharmacy + batch. */
drop temporary table if exists tmp_rpm_inventory_balance_group;
create temporary table tmp_rpm_inventory_balance_group as
select
    g.canonical_item_id,
    ib.pharmacy_location_id,
    coalesce(ib.batch_number, '') as batch_key,
    min(ib.inventory_balance_id) as preferred_inventory_balance_id,
    sum(coalesce(ib.quantity_on_hand, 0)) as merged_quantity_on_hand,
    min(ib.expiry_date) as merged_expiry_date,
    max(ib.unit_price) as merged_unit_price
from rpm_inventory_balance ib
join rpm_item i on i.item_id = ib.item_id
join tmp_rpm_duplicate_item_group g
    on g.normalized_name = lower(trim(i.name))
where ib.voided = 0
group by
    g.canonical_item_id,
    ib.pharmacy_location_id,
    coalesce(ib.batch_number, '');

update rpm_inventory_balance ib
join tmp_rpm_inventory_balance_group grp
    on grp.preferred_inventory_balance_id = ib.inventory_balance_id
set ib.item_id = grp.canonical_item_id,
    ib.batch_number = nullif(grp.batch_key, ''),
    ib.quantity_on_hand = grp.merged_quantity_on_hand,
    ib.expiry_date = coalesce(ib.expiry_date, grp.merged_expiry_date),
    ib.unit_price = coalesce(ib.unit_price, grp.merged_unit_price);

update rpm_inventory_balance ib
join tmp_rpm_inventory_balance_group grp
    on grp.canonical_item_id = (
        select g.canonical_item_id
        from rpm_item i
        join tmp_rpm_duplicate_item_group g
            on g.normalized_name = lower(trim(i.name))
        where i.item_id = ib.item_id
        limit 1
    )
   and grp.pharmacy_location_id <=> ib.pharmacy_location_id
   and grp.batch_key = coalesce(ib.batch_number, '')
set ib.voided = 1,
    ib.date_voided = now(),
    ib.void_reason = concat('Merged into inventory balance ', grp.preferred_inventory_balance_id)
where ib.voided = 0
  and ib.inventory_balance_id <> grp.preferred_inventory_balance_id;

/* Move all non-inventory item references to the canonical item. */
update rpm_stock_transaction t
join tmp_rpm_duplicate_item_map m on m.duplicate_item_id = t.item_id
set t.item_id = m.canonical_item_id;

update rpm_patient_dispensing_request_line l
join tmp_rpm_duplicate_item_map m on m.duplicate_item_id = l.item_id
set l.item_id = m.canonical_item_id;

update rpm_dispense_record_line l
join tmp_rpm_duplicate_item_map m on m.duplicate_item_id = l.item_id
set l.item_id = m.canonical_item_id;

update rpm_inter_pharmacy_transfer_line l
join tmp_rpm_duplicate_item_map m on m.duplicate_item_id = l.item_id
set l.item_id = m.canonical_item_id;

update rpm_delivery_receipt_line l
join tmp_rpm_duplicate_item_map m on m.duplicate_item_id = l.item_id
set l.item_id = m.canonical_item_id;

update rpm_stock_adjustment_line l
join tmp_rpm_duplicate_item_map m on m.duplicate_item_id = l.item_id
set l.item_id = m.canonical_item_id;

update rpm_supplier_product_mapping spm
join tmp_rpm_duplicate_item_map m on m.duplicate_item_id = spm.item_id
set spm.item_id = m.canonical_item_id;

update rpm_supplier_price sp
join tmp_rpm_duplicate_item_map m on m.duplicate_item_id = sp.item_id
set sp.item_id = m.canonical_item_id;

update rpm_supplier_order_line l
join tmp_rpm_duplicate_item_map m on m.duplicate_item_id = l.item_id
set l.item_id = m.canonical_item_id;

update rpm_lab_supply_request_line l
join tmp_rpm_duplicate_item_map m on m.duplicate_item_id = l.item_id
set l.item_id = m.canonical_item_id;

update rpm_pharmacy_item_request_line l
join tmp_rpm_duplicate_item_map m on m.duplicate_item_id = l.item_id
set l.item_id = m.canonical_item_id;

update rpm_item_product_master_map map
join tmp_rpm_duplicate_item_map m on m.duplicate_item_id = map.item_id
set map.item_id = m.canonical_item_id;

/* Void the non-canonical pharmacy items. */
update rpm_item i
join tmp_rpm_duplicate_item_map m on m.duplicate_item_id = i.item_id
set i.voided = 1,
    i.date_voided = now(),
    i.void_reason = concat('Merged into canonical item ', m.canonical_item_id);

/* Keep the transaction open until post-checks are reviewed.
   Run COMMIT if the post-checks look correct, otherwise run ROLLBACK. */

/* -------------------------------------------------------------------------- */
/* POST-CHECKS BEFORE COMMIT                                                  */
/* -------------------------------------------------------------------------- */

/* Should return no non-voided duplicate names. */
select
    lower(trim(name)) as normalized_name,
    count(*) as active_count,
    group_concat(item_id order by item_id) as active_item_ids
from rpm_item
where voided = 0
group by lower(trim(name))
having count(*) > 1;

/* Should return no duplicate active balances for the same item/pharmacy/batch. */
select
    item_id,
    pharmacy_location_id,
    coalesce(batch_number, '') as batch_number,
    count(*) as balance_rows,
    sum(coalesce(quantity_on_hand, 0)) as total_quantity
from rpm_inventory_balance
where voided = 0
group by item_id, pharmacy_location_id, coalesce(batch_number, '')
having count(*) > 1;

/* Review what was voided in this transaction. */
select
    i.item_id,
    i.name,
    i.code,
    i.program,
    i.void_reason
from rpm_item i
join tmp_rpm_duplicate_item_map m on m.duplicate_item_id = i.item_id
order by m.normalized_name, i.item_id;

/* After reviewing:
   commit;
   -- or
   rollback;
*/
