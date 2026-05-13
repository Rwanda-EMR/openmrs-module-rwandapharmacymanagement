<%
    ui.includeJavascript("uicommons", "datetimepicker/bootstrap-datetimepicker.min.js")
    if (org.openmrs.api.context.Context.getLocale().getLanguage() != "en") {
        ui.includeJavascript("uicommons", "datetimepicker/locales/bootstrap-datetimepicker.${ org.openmrs.api.context.Context.getLocale() }.js")
    }
    ui.includeCss("uicommons", "datetimepicker.css")
%>

<style type="text/css">
    .rpm-date-wrapper {
        display: inline-flex;
        align-items: stretch;
        min-width: 160px;
        position: relative;
    }

    .openmrs-date-field {
        background-color: #fff;
        cursor: pointer;
        min-width: 160px;
        padding-right: 6px;
        box-sizing: border-box;
    }

    .openmrs-date-field[readonly] {
        background-color: #f8fafc;
        cursor: default;
    }

    .rpm-date-wrapper .add-on {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-width: 30px;
        padding: 0 8px;
        border: 1px solid #cccccc;
        border-left: 0;
        background: #f5f5f5;
        color: #555555;
        cursor: pointer;
        line-height: 1;
    }

    .rpm-date-wrapper .add-on .icon-calendar,
    .rpm-date-wrapper .add-on .rpm-date-icon {
        font-style: normal;
        font-size: 14px;
    }
</style>

<script type="text/javascript">
    (function() {
        function resolveJQuery() {
            if (typeof jq !== 'undefined' && jq && jq.fn) {
                return jq;
            }
            if (typeof \$j !== 'undefined' && \$j && \$j.fn) {
                return \$j;
            }
            if (typeof jQuery !== 'undefined' && jQuery && jQuery.fn) {
                return jQuery;
            }
            return null;
        }

        function ensureWrapper(field, jquery) {
            var parent = field.parent();
            if (parent.hasClass('rpm-date-wrapper')) {
                return parent;
            }
            field.wrap('<span class="rpm-date-wrapper date"></span>');
            parent = field.parent();
            parent.append('<span class="add-on"><span class="rpm-date-icon">&#128197;</span></span>');
            return parent;
        }

        function initializeOpenmrsDateFields(scope) {
            var root = scope || document;
            var jquery = resolveJQuery();
            if (!jquery || !jquery.fn || !jquery.fn.datetimepicker) {
                return;
            }
            jquery(root).find('.openmrs-date-field').each(function() {
                var field = jquery(this);
                if (field.data('rpmDatepickerBound')) {
                    return;
                }
                if ((field.attr('type') || '').toLowerCase() === 'date') {
                    return;
                }
                field.attr('autocomplete', 'off');
                var wrapper = ensureWrapper(field, jquery);
                wrapper.datetimepicker({
                    minView: 2,
                    autoclose: true,
                    pickerPosition: 'bottom-left',
                    todayHighlight: false,
                    format: 'yyyy-mm-dd',
                    language: '${ org.openmrs.api.context.Context.getLocale() }'
                });
                field.data('rpmDatepickerBound', true);
            });
        }

        window.initializeOpenmrsDateFields = initializeOpenmrsDateFields;
        var jquery = resolveJQuery();
        if (jquery) {
            jquery(function() {
                initializeOpenmrsDateFields(document);
            });
        }
    })();
</script>
