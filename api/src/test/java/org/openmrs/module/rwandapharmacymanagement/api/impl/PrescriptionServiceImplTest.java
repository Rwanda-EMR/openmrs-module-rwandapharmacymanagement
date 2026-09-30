package org.openmrs.module.rwandapharmacymanagement.api.impl;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openmrs.module.rwandapharmacymanagement.api.dto.PrescriptionLineCommand;

import java.io.File;
import java.math.BigDecimal;

public class PrescriptionServiceImplTest {

    @BeforeClass
    public static void configureOpenmrsApplicationDirectory() {
        File appDataDirectory = new File(System.getProperty("java.io.tmpdir"), "openmrs-test-appdata");
        appDataDirectory.mkdirs();
        new File(appDataDirectory, "configuration").mkdirs();
        System.setProperty("OPENMRS_APPLICATION_DATA_DIRECTORY", appDataDirectory.getAbsolutePath());
        System.setProperty("application_data_directory", appDataDirectory.getAbsolutePath());
    }

    @Test
    public void calculateRequestedQuantityShouldPreserveSubmittedQuantity() {
        PrescriptionLineCommand line = new PrescriptionLineCommand();
        line.setDose(new BigDecimal("1"));
        line.setFrequency("Twice daily");
        line.setDuration(Integer.valueOf(5));
        line.setDurationUnit("Day(s)");
        line.setQuantity(new BigDecimal("5"));

        BigDecimal quantity = new PrescriptionServiceImpl().calculateRequestedQuantity(line);

        Assert.assertEquals(new BigDecimal("5"), quantity);
    }

    @Test
    public void calculateRequestedQuantityShouldPreserveManualQuantity() {
        PrescriptionLineCommand line = new PrescriptionLineCommand();
        line.setDose(new BigDecimal("1"));
        line.setFrequency("Twice daily");
        line.setDuration(Integer.valueOf(5));
        line.setDurationUnit("Day(s)");
        line.setQuantity(new BigDecimal("12"));

        BigDecimal quantity = new PrescriptionServiceImpl().calculateRequestedQuantity(line);

        Assert.assertEquals(new BigDecimal("12"), quantity);
    }

    @Test
    public void calculateRequestedQuantityShouldCalculateWhenQuantityIsMissing() {
        PrescriptionLineCommand line = new PrescriptionLineCommand();
        line.setDose(new BigDecimal("0.5"));
        line.setFrequency("Three times a day");
        line.setDuration(Integer.valueOf(3));
        line.setDurationUnit("Day(s)");

        BigDecimal quantity = new PrescriptionServiceImpl().calculateRequestedQuantity(line);

        Assert.assertEquals(new BigDecimal("5"), quantity);
    }

    @Test
    public void calculateRequestedQuantityShouldSupportCommonThreeTimesDailyAliases() {
        PrescriptionLineCommand line = new PrescriptionLineCommand();
        line.setDose(new BigDecimal("1"));
        line.setFrequency("TDS");
        line.setDuration(Integer.valueOf(5));
        line.setDurationUnit("Day(s)");

        BigDecimal quantity = new PrescriptionServiceImpl().calculateRequestedQuantity(line);

        Assert.assertEquals(new BigDecimal("15"), quantity);
    }

    @Test
    public void calculateRequestedQuantityShouldParseFrequencyNumberWordsAndPeriods() {
        assertCalculatedQuantity("TWICE A DAY", 1, "Day(s)", "2");
        assertCalculatedQuantity("ONCE A DAY", 1, "Day(s)", "1");
        assertCalculatedQuantity("ONCE A MONTH", 1, "Month(s)", "1");
        assertCalculatedQuantity("THREE A DAY", 1, "Day(s)", "3");
        assertCalculatedQuantity("TWICE A MONTH", 1, "Month(s)", "2");
        assertCalculatedQuantity("Four times a day", 1, "Day(s)", "4");
        assertCalculatedQuantity("FIVE TIMES A DAY", 1, "Day(s)", "5");
        assertCalculatedQuantity("in the morning", 1, "Day(s)", "1");
        assertCalculatedQuantity("IN THE EVENING", 1, "Day(s)", "1");
        assertCalculatedQuantity("EVERY OTHER DAY", 2, "Day(s)", "1");
        assertCalculatedQuantity("SIX TIMES A DAY", 1, "Day(s)", "6");
        assertCalculatedQuantity("ONCE A WEEK", 1, "Week(s)", "1");
        assertCalculatedQuantity("ONCE A DAY AT NIGHT", 1, "Day(s)", "1");
        assertCalculatedQuantity("SEVEN TIMES A WEEK", 1, "Week(s)", "7");
        assertCalculatedQuantity("TWENTY TIMES A MONTH", 1, "Month(s)", "20");
        assertCalculatedQuantity("8 TIMES A DAY", 1, "Day(s)", "8");
    }

    @Test
    public void isPerNeedFrequencyShouldRecognizePrnLabels() {
        PrescriptionServiceImpl service = new PrescriptionServiceImpl();

        Assert.assertTrue(service.isPerNeedFrequency("Per need"));
        Assert.assertTrue(service.isPerNeedFrequency("As needed"));
        Assert.assertTrue(service.isPerNeedFrequency("PRN"));
        Assert.assertFalse(service.isPerNeedFrequency("Once a day"));
    }

    @Test
    public void normalizeFreeTextInstructionsShouldRemoveGeneratedDoseScheduleParts() {
        PrescriptionServiceImpl service = new PrescriptionServiceImpl();

        String instructions = service.normalizeFreeTextInstructions(
                "Dose: 4; Route: ORAL; Frequency: TWICE A DAY; Duration: 5 DAYS; "
                        + "Dose: 3; Route: ORAL; Frequency: ONCE A DAY; Duration: 4 DAYS; none");

        Assert.assertEquals("none", instructions);
    }

    private void assertCalculatedQuantity(String frequency, int duration, String durationUnit, String expectedQuantity) {
        PrescriptionLineCommand line = new PrescriptionLineCommand();
        line.setDose(BigDecimal.ONE);
        line.setFrequency(frequency);
        line.setDuration(Integer.valueOf(duration));
        line.setDurationUnit(durationUnit);

        BigDecimal quantity = new PrescriptionServiceImpl().calculateRequestedQuantity(line);

        Assert.assertEquals(new BigDecimal(expectedQuantity), quantity);
    }
}
