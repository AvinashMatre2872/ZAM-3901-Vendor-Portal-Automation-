package com.zamplia.tests;

import com.zamplia.base.BaseTest;
import com.zamplia.pages.Calibr8ReportPage;
import com.zamplia.utils.ConfigReader;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Ticket: Calibr8 Report date range limit changed from 10 days to 1 month.
 * maxRangeDaysInclusive (config) = 31 by default -> 1 Oct to 31 Oct is the largest allowed range.
 */
public class Calibr8DateRangeTest extends BaseTest {

    private final int maxDays = ConfigReader.getInt("maxRangeDaysInclusive");
    private final int oldMaxDays = ConfigReader.getInt("oldMaxRangeDaysInclusive");
    private final LocalDate base = LocalDate.parse(ConfigReader.get("baseDate"));
    private final DateTimeFormatter fmt = Calibr8ReportPage.FMT;

    @BeforeMethod(alwaysRun = true)
    public void openCalibr8() {
        calibr8 = goToCalibr8();
    }

    private static LocalDate endFor(LocalDate start, int inclusiveDays) {
        return start.plusDays(inclusiveDays - 1L);
    }

    private static long inclusiveDays(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end) + 1;
    }

    private void addIfWithinMax(List<Object[]> rows, String name, LocalDate s, LocalDate e) {
        if (inclusiveDays(s, e) <= maxDays) rows.add(new Object[]{name, s, e});
    }

    // ------------------------------------------------------------------ data
    @DataProvider(name = "validRanges")
    public Object[][] validRanges() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"Single day (start = end)", base, base});
        rows.add(new Object[]{"Old limit: " + oldMaxDays + " days", base, endFor(base, oldMaxDays)});
        rows.add(new Object[]{"Old limit + 1: " + (oldMaxDays + 1) + " days (blocked before ticket)", base, endFor(base, oldMaxDays + 1)});
        rows.add(new Object[]{"Mid range: 15 days", base, endFor(base, 15)});
        rows.add(new Object[]{"Max - 1: " + (maxDays - 1) + " days", base, endFor(base, maxDays - 1)});
        rows.add(new Object[]{"Exact max boundary: " + maxDays + " days", base, endFor(base, maxDays)});
        addIfWithinMax(rows, "Full 31-day calendar month (Oct 2026)", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        addIfWithinMax(rows, "Full 30-day calendar month (Sep 2026)", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        addIfWithinMax(rows, "Cross-month range (15 Sep - 14 Oct)", LocalDate.of(2026, 9, 15), LocalDate.of(2026, 10, 14));
        addIfWithinMax(rows, "February non-leap (28 days)", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        addIfWithinMax(rows, "February leap year (29 days)", LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29));
        addIfWithinMax(rows, "Cross-year range (20 Dec - 10 Jan)", LocalDate.of(2025, 12, 20), LocalDate.of(2026, 1, 10));
        return rows.toArray(new Object[0][]);
    }

    @DataProvider(name = "invalidRanges")
    public Object[][] invalidRanges() {
        LocalDate anchorEnd = endFor(base, maxDays); // e.g. 2026-10-31, ranges end here so no future dates
        return new Object[][]{
                {"Max + 1: " + (maxDays + 1) + " days (boundary)", anchorEnd.minusDays(maxDays), anchorEnd},
                {"Max + 2: " + (maxDays + 2) + " days", anchorEnd.minusDays(maxDays + 1L), anchorEnd},
                {"45 days", anchorEnd.minusDays(44), anchorEnd},
                {"60 days (~2 months)", anchorEnd.minusDays(59), anchorEnd},
                {"90 days (quarter)", anchorEnd.minusDays(89), anchorEnd},
                {"365 days (1 year)", anchorEnd.minusDays(364), anchorEnd},
                {"Two full calendar months (1 Sep - 31 Oct)", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 31)},
        };
    }

    // ------------------------------------------------------------------ tests
    @Test(priority = 1, description = "TC_DR_01: Date range picker is displayed on Calibr8 Report")
    public void datePickerDisplayed() {
        Assert.assertTrue(calibr8.isDateRangePickerDisplayed(), "Date range picker not displayed");
    }

    @Test(priority = 2, description = "TC_DR_02: Default date range (if pre-filled) is within 1 month")
    public void defaultRangeWithinLimit() {
        String s = calibr8.getStartValue();
        String e = calibr8.getEndValue();
        if (s.isEmpty() || e.isEmpty()) {
            throw new SkipException("No default date range pre-filled (start='" + s + "', end='" + e + "')");
        }
        long days = inclusiveDays(LocalDate.parse(s, fmt), LocalDate.parse(e, fmt));
        Assert.assertTrue(days >= 1 && days <= maxDays, "Default range is " + days + " days, max is " + maxDays);
    }

    @Test(priority = 3, dataProvider = "validRanges",
            description = "TC_DR_03 (Positive): Ranges up to 1 month are accepted and data loads")
    public void validRangeAccepted(String scenario, LocalDate start, LocalDate end) {
        calibr8.setDateRange(start, end);
        Assert.assertEquals(calibr8.getStartValue(), start.format(fmt), scenario + " -> start date not accepted");
        Assert.assertEquals(calibr8.getEndValue(), end.format(fmt),
                scenario + " -> end date not accepted (" + inclusiveDays(start, end) + " days must be allowed)");

        calibr8.clickApplyFilters();
        Assert.assertFalse(calibr8.isErrorDisplayed(3),
                scenario + " -> unexpected validation error: " + calibr8.getErrorText());
        Assert.assertEquals(calibr8.getStartValue(), start.format(fmt), scenario + " -> start changed after Apply");
        Assert.assertEquals(calibr8.getEndValue(), end.format(fmt), scenario + " -> end changed after Apply");
        Assert.assertTrue(calibr8.getTotalResponses() >= 0, scenario + " -> Total Responses not rendered");
    }

    @Test(priority = 4, dataProvider = "invalidRanges",
            description = "TC_DR_04 (Negative): Ranges longer than 1 month are blocked")
    public void rangeAboveOneMonthRejected(String scenario, LocalDate start, LocalDate end) {
        calibr8.setDateRange(start, end);
        boolean accepted = calibr8.isRangeShown(start, end);
        boolean error = calibr8.isErrorDisplayed(2);
        if (accepted && !error) {
            // some implementations validate on Apply instead of disabling dates
            calibr8.clickApplyFilters();
            error = calibr8.isErrorDisplayed(5);
        }
        Assert.assertTrue(!accepted || error, scenario + " -> range of " + inclusiveDays(start, end)
                + " days was accepted without any validation (max " + maxDays + ")");
    }

    @Test(priority = 5, description = "TC_DR_05 (Boundary): Calendar enables day 1..max and disables day max+1")
    public void calendarBoundaryCells() {
        calibr8.openCalendarWithStartDate(base);
        Boolean oldLimitPlusOne = calibr8.isCalendarDateDisabled(endFor(base, oldMaxDays + 1));
        Boolean lastAllowed = calibr8.isCalendarDateDisabled(endFor(base, maxDays));
        Boolean firstBlocked = calibr8.isCalendarDateDisabled(endFor(base, maxDays + 1));
        int disabledCount = calibr8.countDisabledCellsInOpenCalendar();
        calibr8.closePicker();

        if (lastAllowed == null || firstBlocked == null) {
            throw new SkipException("Boundary dates not rendered in the open calendar panel");
        }
        if (disabledCount == 0) {
            throw new SkipException("App does not disable calendar dates - validation covered by TC_DR_04");
        }
        Assert.assertEquals(oldLimitPlusOne, Boolean.FALSE, "Day " + (oldMaxDays + 1) + " still disabled - old 10-day limit not removed");
        Assert.assertEquals(lastAllowed, Boolean.FALSE, "Day " + maxDays + " should be selectable");
        Assert.assertEquals(firstBlocked, Boolean.TRUE, "Day " + (maxDays + 1) + " should be disabled");
    }

    @Test(priority = 6, description = "TC_DR_06 (Positive): Select end date by mouse click in calendar (1 month)")
    public void selectRangeByCalendarClick() {
        LocalDate end = endFor(base, maxDays);
        calibr8.openCalendarWithStartDate(base);
        if (calibr8.isCalendarDateDisabled(end) == null) {
            calibr8.closePicker();
            throw new SkipException("End date " + end + " not visible in calendar panel");
        }
        calibr8.clickCalendarDate(end);
        calibr8.closePicker();
        Assert.assertTrue(calibr8.isRangeShown(base, end),
                "Expected " + base + " -> " + end + " but got " + calibr8.getStartValue() + " -> " + calibr8.getEndValue());
    }

    @Test(priority = 7, description = "TC_DR_07 (Negative): Start date after end date")
    public void startAfterEnd() {
        LocalDate start = base.plusDays(10);
        calibr8.setDateRange(start, base);
        String s = calibr8.getStartValue();
        String e = calibr8.getEndValue();
        if (calibr8.isErrorDisplayed(2) || s.isEmpty() || e.isEmpty()) return; // rejected -> pass
        Assert.assertFalse(LocalDate.parse(s, fmt).isAfter(LocalDate.parse(e, fmt)),
                "Start (" + s + ") is after End (" + e + ") - reverse range must not be accepted");
    }

    @Test(priority = 8, description = "TC_DR_08 (Negative): Invalid / garbage date text is not accepted")
    public void invalidDateFormat() {
        calibr8.setDateRange("abcd", "2026/13/45");
        Assert.assertNotEquals(calibr8.getStartValue(), "abcd", "Garbage start date accepted");
        Assert.assertNotEquals(calibr8.getEndValue(), "2026/13/45", "Invalid end date accepted");
    }

    @Test(priority = 9, description = "TC_DR_09 (Negative): Apply with empty date range does not break the page")
    public void applyWithEmptyRange() {
        calibr8.clearDateRange();
        calibr8.clickApplyFilters();
        Assert.assertTrue(calibr8.isLoaded(), "Page broke after applying empty date range");
        Assert.assertTrue(calibr8.isErrorDisplayed(2) || calibr8.getTotalResponses() >= 0,
                "Either a validation message or default data should be shown");
    }

    @Test(priority = 10, description = "TC_DR_10 (Data): 1-month range returns >= responses of its first 10 days")
    public void largerRangeHasMoreOrEqualData() {
        calibr8.setDateRange(base, endFor(base, oldMaxDays));
        calibr8.clickApplyFilters();
        int tenDayTotal = calibr8.getTotalResponses();

        calibr8.setDateRange(base, endFor(base, maxDays));
        calibr8.clickApplyFilters();
        int monthTotal = calibr8.getTotalResponses();

        Assert.assertTrue(monthTotal >= tenDayTotal,
                "1-month total (" + monthTotal + ") < 10-day total (" + tenDayTotal + ")");
    }

    @Test(priority = 11, description = "TC_DR_11: Selected 1-month range persists across Overview/Deep Analysis/Responses/Geo Heatmap")
    public void rangePersistsAcrossSubTabs() {
        LocalDate end = endFor(base, maxDays);
        calibr8.setDateRange(base, end);
        calibr8.clickApplyFilters();
        for (String tab : new String[]{"Deep Analysis", "Responses", "Geo Heatmap", "Overview"}) {
            calibr8.openSubTab(tab);
            Assert.assertTrue(calibr8.isRangeShown(base, end), "Date range lost after opening tab: " + tab);
            Assert.assertFalse(calibr8.isErrorDisplayed(1), "Error on tab " + tab + ": " + calibr8.getErrorText());
        }
    }

    @Test(priority = 12, description = "TC_DR_12: Reset restores the default date range")
    public void resetRestoresDefaultRange() {
        String defStart = calibr8.getStartValue();
        String defEnd = calibr8.getEndValue();
        calibr8.setDateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 20));
        calibr8.clickApplyFilters();
        calibr8.clickReset();
        Assert.assertEquals(calibr8.getStartValue(), defStart, "Start not reset");
        Assert.assertEquals(calibr8.getEndValue(), defEnd, "End not reset");
    }

    @Test(priority = 13, description = "TC_DR_13: Range is kept after page re-applies (Apply twice)")
    public void applyTwiceKeepsRange() {
        LocalDate end = endFor(base, maxDays);
        calibr8.setDateRange(base, end);
        calibr8.clickApplyFilters();
        int first = calibr8.getTotalResponses();
        calibr8.clickApplyFilters();
        Assert.assertTrue(calibr8.isRangeShown(base, end), "Range changed on second Apply");
        Assert.assertEquals(calibr8.getTotalResponses(), first, "Same filters should return the same total");
    }
}
