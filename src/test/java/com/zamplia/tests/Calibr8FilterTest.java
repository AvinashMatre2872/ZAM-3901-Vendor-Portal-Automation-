package com.zamplia.tests;

import com.zamplia.base.BaseTest;
import com.zamplia.pages.Calibr8ReportPage.Metric;
import com.zamplia.utils.ConfigReader;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.List;

/** Country / Risk Level / Flag Key dropdowns, Reset, metric cards and sub tabs - all on a 1-month range. */
public class Calibr8FilterTest extends BaseTest {

    private static final String COUNTRY = "COUNTRY";
    private static final String RISK = "RISK LEVEL";
    private static final String FLAG = "FLAG KEY";

    private final LocalDate start = LocalDate.parse(ConfigReader.get("baseDate"));
    private final LocalDate end = start.plusDays(ConfigReader.getInt("maxRangeDaysInclusive") - 1L);

    @BeforeMethod(alwaysRun = true)
    public void openWithOneMonthRange() {
        calibr8 = goToCalibr8();
        calibr8.setDateRange(start, end);
    }

    private int overallTotal() {
        calibr8.clickApplyFilters();
        return calibr8.getTotalResponses();
    }

    // ---------------------------------------------------------------- COUNTRY
    @Test(priority = 1, description = "TC_FL_01: Country dropdown displayed with default 'All Countries'")
    public void countryDefault() {
        Assert.assertTrue(calibr8.isDropdownDisplayed(COUNTRY), "Country dropdown missing");
        Assert.assertTrue(calibr8.getSelectedValue(COUNTRY).toLowerCase().contains("all"),
                "Default should be 'All Countries', was: " + calibr8.getSelectedValue(COUNTRY));
    }

    @Test(priority = 2, description = "TC_FL_02: Country dropdown opens and lists countries")
    public void countryOptionsListed() {
        List<String> opts = calibr8.getDropdownOptions(COUNTRY);
        Assert.assertFalse(opts.isEmpty(), "Country dropdown has no options");
    }

    @Test(priority = 3, description = "TC_FL_03: Searching in Country dropdown filters the list")
    public void countrySearch() {
        if (!calibr8.isDropdownSearchable(COUNTRY)) throw new SkipException("Country dropdown is not searchable");
        List<String> all = calibr8.getDropdownOptions(COUNTRY);
        String first = all.stream().filter(o -> !o.toLowerCase().startsWith("all")).findFirst()
                .orElseThrow(() -> new SkipException("No country option to search"));
        String term = first.substring(0, Math.min(3, first.length()));
        List<String> filtered = calibr8.searchInDropdown(COUNTRY, term);
        calibr8.closeDropdown();
        Assert.assertFalse(filtered.isEmpty(), "No results for '" + term + "'");
        for (String o : filtered) {
            Assert.assertTrue(o.toLowerCase().contains(term.toLowerCase()), "'" + o + "' doesn't match search '" + term + "'");
        }
    }

    @Test(priority = 4, description = "TC_FL_04 (Negative): Search non-existing country shows no data")
    public void countrySearchNoMatch() {
        if (!calibr8.isDropdownSearchable(COUNTRY)) throw new SkipException("Country dropdown is not searchable");
        List<String> res = calibr8.searchInDropdown(COUNTRY, "zzqx123");
        boolean empty = res.isEmpty() || calibr8.isNoDataShownInDropdown();
        calibr8.closeDropdown();
        Assert.assertTrue(empty, "Unexpected options for garbage search: " + res);
    }

    @Test(priority = 5, description = "TC_FL_05: Select a country + 1-month range -> data filtered, total <= overall")
    public void selectCountryAndApply() {
        int all = overallTotal();
        String country = calibr8.selectFirstRealOption(COUNTRY);
        Assert.assertTrue(calibr8.getSelectedValue(COUNTRY).contains(country), "Country not shown as selected");
        calibr8.clickApplyFilters();
        Assert.assertFalse(calibr8.isErrorDisplayed(2), "Error: " + calibr8.getErrorText());
        int filtered = calibr8.getTotalResponses();
        Assert.assertTrue(filtered <= all, country + " total " + filtered + " > overall " + all);
    }

    // ---------------------------------------------------------------- RISK LEVEL
    @Test(priority = 6, description = "TC_FL_06: Risk Level options contain Low, Medium, High, Critical")
    public void riskLevelOptions() {
        String joined = String.join("|", calibr8.getDropdownOptions(RISK)).toLowerCase();
        for (String lvl : new String[]{"low", "medium", "high", "critical"}) {
            Assert.assertTrue(joined.contains(lvl), "Risk level option missing: " + lvl + " in " + joined);
        }
    }

    @DataProvider(name = "riskLevels")
    public Object[][] riskLevels() {
        return new Object[][]{
                {"Low", "LOW RISK"}, {"Medium", "MEDIUM RISK"}, {"High", "HIGH RISK"}, {"Critical", "CRITICAL"}
        };
    }

    @Test(priority = 7, dataProvider = "riskLevels",
            description = "TC_FL_07: Filter by each risk level -> other risk cards show 0")
    public void filterByRiskLevel(String option, String cardLabel) {
        int all = overallTotal();
        calibr8.selectOptionContaining(RISK, option);
        calibr8.clickApplyFilters();
        Assert.assertFalse(calibr8.isErrorDisplayed(2), "Error: " + calibr8.getErrorText());
        Assert.assertTrue(calibr8.getTotalResponses() <= all, "Filtered total > overall total");
        for (String other : new String[]{"LOW RISK", "MEDIUM RISK", "HIGH RISK", "CRITICAL"}) {
            if (other.equals(cardLabel)) continue;
            Metric m = calibr8.getMetricOrNull(other);
            if (m != null) Assert.assertEquals(m.count, 0, other + " should be 0 when filtering by " + option);
        }
    }

    // ---------------------------------------------------------------- FLAG KEY
    @Test(priority = 8, description = "TC_FL_08: Flag Key dropdown displayed with default 'All Flags' and has options")
    public void flagKeyOptions() {
        Assert.assertTrue(calibr8.getSelectedValue(FLAG).toLowerCase().contains("all"),
                "Default should be 'All Flags', was: " + calibr8.getSelectedValue(FLAG));
        Assert.assertFalse(calibr8.getDropdownOptions(FLAG).isEmpty(), "Flag Key has no options");
    }

    @Test(priority = 9, description = "TC_FL_09: Select a flag key and apply -> total <= overall")
    public void selectFlagAndApply() {
        int all = overallTotal();
        calibr8.selectFirstRealOption(FLAG);
        calibr8.clickApplyFilters();
        Assert.assertFalse(calibr8.isErrorDisplayed(2), "Error: " + calibr8.getErrorText());
        Assert.assertTrue(calibr8.getTotalResponses() <= all, "Flag-filtered total > overall");
    }

    // ---------------------------------------------------------------- COMBINED / RESET
    @Test(priority = 10, description = "TC_FL_10: Combined filters (1 month + country + risk + flag) apply without error")
    public void combinedFilters() {
        calibr8.selectFirstRealOption(COUNTRY);
        calibr8.selectOptionContaining(RISK, "Low");
        calibr8.selectFirstRealOption(FLAG);
        calibr8.clickApplyFilters();
        Assert.assertFalse(calibr8.isErrorDisplayed(2), "Error: " + calibr8.getErrorText());
        Assert.assertTrue(calibr8.isRangeShown(start, end), "Date range lost after combined filter");
        Assert.assertTrue(calibr8.getTotalResponses() >= 0);
    }

    @Test(priority = 11, description = "TC_FL_11: Reset clears Country, Risk Level and Flag Key")
    public void resetClearsDropdowns() {
        calibr8.selectFirstRealOption(COUNTRY);
        calibr8.selectOptionContaining(RISK, "High");
        calibr8.selectFirstRealOption(FLAG);
        calibr8.clickApplyFilters();
        calibr8.clickReset();
        Assert.assertTrue(calibr8.getSelectedValue(COUNTRY).toLowerCase().contains("all"), "Country not reset");
        Assert.assertTrue(calibr8.getSelectedValue(RISK).toLowerCase().contains("all"), "Risk level not reset");
        Assert.assertTrue(calibr8.getSelectedValue(FLAG).toLowerCase().contains("all"), "Flag key not reset");
    }

    // ---------------------------------------------------------------- METRICS
    @Test(priority = 12, description = "TC_FL_12: Risk counts add up and percentages are correct for 1-month range")
    public void metricCardsConsistent() {
        calibr8.clickApplyFilters();
        int total = calibr8.getTotalResponses();
        int sum = 0;
        for (String label : new String[]{"LOW RISK", "MEDIUM RISK", "HIGH RISK", "CRITICAL"}) {
            Metric m = calibr8.getMetricOrNull(label);
            if (m == null) continue;
            sum += m.count;
            if (m.percent != null && total > 0) {
                double expected = m.count * 100.0 / total;
                Assert.assertEquals(m.percent.doubleValue(), expected, 0.15, label + " % mismatch (count " + m.count + "/" + total + ")");
            }
        }
        Assert.assertTrue(sum <= total, "Sum of risk cards (" + sum + ") > Total Responses (" + total + ")");
    }

    // ---------------------------------------------------------------- SUB TABS
    @DataProvider(name = "subTabs")
    public Object[][] subTabs() {
        return new Object[][]{{"Overview"}, {"Deep Analysis"}, {"Responses"}, {"Geo Heatmap"}};
    }

    @Test(priority = 13, dataProvider = "subTabs", description = "TC_FL_13: Each sub tab opens with 1-month range applied")
    public void subTabOpens(String tab) {
        calibr8.clickApplyFilters();
        calibr8.openSubTab(tab);
        Assert.assertTrue(calibr8.isSubTabActive(tab), tab + " tab not active");
        Assert.assertFalse(calibr8.isErrorDisplayed(2), "Error on " + tab + ": " + calibr8.getErrorText());
    }
}
