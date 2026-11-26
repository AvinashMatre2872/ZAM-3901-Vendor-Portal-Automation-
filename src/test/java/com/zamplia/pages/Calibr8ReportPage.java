package com.zamplia.pages;

import com.zamplia.utils.ConfigReader;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reports -> Calibr8 Report.
 * Locators are written for Ant Design components (RangePicker / Select / Tabs) which is what the
 * screenshots show. If a locator fails on first run, fix it here only - tests do not hold locators.
 */
public class Calibr8ReportPage extends BasePage {

    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern(ConfigReader.get("dateFormat"));

    // ---------------- Date range ----------------
    private static final String RANGE = "(//div[contains(@class,'ant-picker-range')])[1]";
    private final By rangePicker = By.xpath(RANGE);
    private final By startInput = By.xpath("(" + RANGE + "//input)[1]");
    private final By endInput = By.xpath("(" + RANGE + "//input)[2]");
    private final By clearIcon = By.xpath(RANGE + "//*[contains(@class,'ant-picker-clear')]");
    private static final String OPEN_PICKER_PANEL =
            "//div[contains(@class,'ant-picker-dropdown') and not(contains(@class,'ant-picker-dropdown-hidden'))]";

    // ---------------- Dropdowns ----------------
    private static final String OPEN_SELECT =
            "//div[contains(@class,'ant-select-dropdown') and not(contains(@class,'ant-select-dropdown-hidden'))]";
    private static final String OPTIONS = OPEN_SELECT + "//div[contains(@class,'ant-select-item-option')]";
    private static final String EMPTY = OPEN_SELECT
            + "//*[contains(@class,'ant-empty') or contains(@class,'ant-select-item-empty')]";

    // ---------------- Buttons ----------------
    private final By applyBtn = By.xpath("//button[normalize-space(.)='Apply Filters']");
    private final By resetBtn = By.xpath("//button[normalize-space(.)='Reset']");
    private final By exportBtn = By.xpath("//button[normalize-space(.)='Export']");
    private final By exportHistoryBtn = By.xpath("//button[contains(normalize-space(.),'Export History')]");
    private final By successMsg = By.cssSelector(
            ".ant-message-success, .ant-notification-notice-success, .Toastify__toast--success, .ant-message-info");
    private final By dialog = By.cssSelector(".ant-modal-content, .ant-drawer-open .ant-drawer-content, [role='dialog']");

    public Calibr8ReportPage(WebDriver driver) {
        super(driver);
    }

    public void waitUntilLoaded() {
        visible(applyBtn);
        waitForLoading();
    }

    public boolean isLoaded() {
        return isDisplayed(applyBtn, 10);
    }

    // =====================================================================
    // DATE RANGE
    // =====================================================================
    public boolean isDateRangePickerDisplayed() {
        return isDisplayed(rangePicker, 10) && isDisplayed(startInput, 2) && isDisplayed(endInput, 2);
    }

    public void setDateRange(LocalDate start, LocalDate end) {
        setDateRange(start.format(FMT), end.format(FMT));
    }

    /** Types both dates (Ant RangePicker commits each value on ENTER). */
    public void setDateRange(String start, String end) {
        typeIntoPicker(startInput, start);
        typeIntoPicker(endInput, end);
        closePicker();
    }

    private void typeIntoPicker(By input, String value) {
        WebElement el = clickable(input);
        scrollIntoView(el);
        el.click();
        el.sendKeys(Keys.chord(selectAllKey(), "a"), Keys.BACK_SPACE);
        el.sendKeys(value);
        el.sendKeys(Keys.ENTER);
        pause(400);
    }

    public void closePicker() {
        if (!driver.findElements(By.xpath(OPEN_PICKER_PANEL)).isEmpty()) {
            pressEscape();
        }
        // blur the picker so Ant Design reverts any uncommitted / disabled value
        js().executeScript("if (document.activeElement) { document.activeElement.blur(); }");
        pause(300);
    }

    /** Types the start date + ENTER so the calendar stays open on the END side. */
    public void openCalendarWithStartDate(LocalDate start) {
        typeIntoPicker(startInput, start.format(FMT));
        if (driver.findElements(By.xpath(OPEN_PICKER_PANEL)).isEmpty()) {
            click(endInput);
        }
        visible(By.xpath(OPEN_PICKER_PANEL));
    }

    /** @return TRUE disabled, FALSE enabled, NULL if the date is not rendered in the open calendar. */
    public Boolean isCalendarDateDisabled(LocalDate date) {
        List<WebElement> cells = driver.findElements(
                By.xpath(OPEN_PICKER_PANEL + "//td[@title='" + date.format(FMT) + "']"));
        if (cells.isEmpty()) return null;
        return cells.get(0).getAttribute("class").contains("ant-picker-cell-disabled");
    }

    public int countDisabledCellsInOpenCalendar() {
        return driver.findElements(By.xpath(OPEN_PICKER_PANEL + "//td[contains(@class,'ant-picker-cell-disabled')]")).size();
    }

    public void clickCalendarDate(LocalDate date) {
        WebElement cell = visible(By.xpath(OPEN_PICKER_PANEL + "//td[@title='" + date.format(FMT) + "']"));
        cell.click();
        pause(400);
    }

    public String getStartValue() {
        return visible(startInput).getAttribute("value").trim();
    }

    public String getEndValue() {
        return visible(endInput).getAttribute("value").trim();
    }

    public boolean isRangeShown(LocalDate start, LocalDate end) {
        return getStartValue().equals(start.format(FMT)) && getEndValue().equals(end.format(FMT));
    }

    public void clearDateRange() {
        new Actions(driver).moveToElement(visible(rangePicker)).perform();
        pause(300);
        List<WebElement> clear = driver.findElements(clearIcon);
        if (!clear.isEmpty() && clear.get(0).isDisplayed()) {
            new Actions(driver).moveToElement(clear.get(0)).click().perform();
        } else {
            type(startInput, "");
            type(endInput, "");
        }
        closePicker();
    }

    // =====================================================================
    // DROPDOWNS (label = "COUNTRY", "RISK LEVEL", "FLAG KEY")
    // =====================================================================
    private By selectBox(String label) {
        String up = label.toUpperCase();
        return By.xpath("//*[" + UPPER + "='" + up + "' and not(.//*[" + UPPER + "='" + up + "'])]"
                + "/following::div[contains(@class,'ant-select')][1]");
    }

    public boolean isDropdownDisplayed(String label) {
        return isDisplayed(selectBox(label), 10);
    }

    public void openDropdown(String label) {
        WebElement box = visible(selectBox(label));
        scrollIntoView(box);
        List<WebElement> selector = box.findElements(By.cssSelector(".ant-select-selector"));
        WebElement target = selector.isEmpty() ? box : selector.get(0);
        try {
            target.click();
        } catch (ElementClickInterceptedException e) {
            jsClick(target);
        }
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(OPTIONS + " | " + EMPTY)));
        pause(300);
    }

    public void closeDropdown() {
        if (!driver.findElements(By.xpath(OPEN_SELECT)).isEmpty()) pressEscape();
    }

    private static String optionText(WebElement o) {
        String t = o.getAttribute("title");
        if (t == null || t.isBlank()) t = o.getText();
        return t == null ? "" : t.trim();
    }

    private List<String> visibleOptions() {
        List<String> out = new ArrayList<>();
        for (WebElement o : driver.findElements(By.xpath(OPTIONS))) {
            String t = optionText(o);
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    /** Note: Ant Select uses a virtual list, so only the rendered (first ~10) options are returned. */
    public List<String> getDropdownOptions(String label) {
        openDropdown(label);
        List<String> opts = visibleOptions();
        closeDropdown();
        return opts;
    }

    public boolean isDropdownSearchable(String label) {
        List<WebElement> inputs = visible(selectBox(label)).findElements(By.tagName("input"));
        return !inputs.isEmpty() && inputs.get(0).getAttribute("readonly") == null;
    }

    /** Opens the dropdown and types a search term. Returns the visible options (caller should closeDropdown()). */
    public List<String> searchInDropdown(String label, String term) {
        openDropdown(label);
        WebElement input = visible(selectBox(label)).findElement(By.tagName("input"));
        input.sendKeys(term);
        pause(800);
        return visibleOptions();
    }

    public boolean isNoDataShownInDropdown() {
        return !driver.findElements(By.xpath(EMPTY)).isEmpty();
    }

    public String selectOptionContaining(String label, String text) {
        openDropdown(label);
        WebElement match = findOption(text);
        if (match == null && isDropdownSearchable(label)) {
            visible(selectBox(label)).findElement(By.tagName("input")).sendKeys(text);
            pause(800);
            match = findOption(text);
        }
        if (match == null) {
            closeDropdown();
            throw new NoSuchElementException("Option containing '" + text + "' not found in " + label);
        }
        String chosen = optionText(match);
        match.click();
        pause(300);
        closeDropdown();
        return chosen;
    }

    private WebElement findOption(String text) {
        for (WebElement o : driver.findElements(By.xpath(OPTIONS))) {
            if (optionText(o).toLowerCase().contains(text.toLowerCase())) return o;
        }
        return null;
    }

    /** Selects the first option that is not an "All ..." option. */
    public String selectFirstRealOption(String label) {
        openDropdown(label);
        for (WebElement o : driver.findElements(By.xpath(OPTIONS))) {
            String t = optionText(o);
            if (!t.isEmpty() && !t.toLowerCase().startsWith("all")) {
                o.click();
                pause(300);
                closeDropdown();
                return t;
            }
        }
        closeDropdown();
        throw new NoSuchElementException("No selectable option in " + label);
    }

    public String getSelectedValue(String label) {
        WebElement box = visible(selectBox(label));
        List<WebElement> items = box.findElements(By.cssSelector(".ant-select-selection-item"));
        if (!items.isEmpty()) {
            return items.stream().map(e -> {
                String t = e.getAttribute("title");
                return (t == null || t.isBlank()) ? e.getText().trim() : t.trim();
            }).collect(Collectors.joining(","));
        }
        List<WebElement> ph = box.findElements(By.cssSelector(".ant-select-selection-placeholder"));
        return ph.isEmpty() ? "" : ph.get(0).getText().trim();
    }

    // =====================================================================
    // BUTTONS
    // =====================================================================
    public void clickApplyFilters() {
        click(applyBtn);
        waitForLoading();
    }

    public void clickReset() {
        click(resetBtn);
        waitForLoading();
    }

    public boolean isExportButtonEnabled() {
        return visible(exportBtn).isEnabled();
    }

    public void clickExport() {
        click(exportBtn);
    }

    public boolean isExportSuccessMessageDisplayed(int seconds) {
        return isDisplayed(successMsg, seconds);
    }

    public void clickExportHistory() {
        click(exportHistoryBtn);
        waitForLoading();
    }

    public boolean isExportHistoryButtonDisplayed() {
        return isDisplayed(exportHistoryBtn, 5);
    }

    public boolean isDialogDisplayed(int seconds) {
        return isDisplayed(dialog, seconds);
    }

    public void closeDialog() {
        List<WebElement> close = driver.findElements(By.cssSelector(".ant-modal-close, .ant-drawer-close"));
        if (!close.isEmpty() && close.get(0).isDisplayed()) close.get(0).click();
        else pressEscape();
        pause(400);
    }

    // =====================================================================
    // SUB TABS: Overview / Deep Analysis / Responses / Geo Heatmap
    // =====================================================================
    private By subTab(String name) {
        return By.xpath("(//*[@role='tab'][contains(normalize-space(.),'" + name + "')]"
                + " | //div[contains(@class,'ant-tabs-tab')][contains(normalize-space(.),'" + name + "')])[1]");
    }

    public void openSubTab(String name) {
        click(subTab(name));
        waitForLoading();
    }

    public boolean isSubTabActive(String name) {
        WebElement tab = visible(subTab(name));
        if ("true".equals(tab.getAttribute("aria-selected"))) return true;
        return !tab.findElements(By.xpath("./ancestor-or-self::*[contains(@class,'active')]")).isEmpty();
    }

    // =====================================================================
    // METRIC CARDS
    // =====================================================================
    public static class Metric {
        public final int count;
        public final Double percent; // null for TOTAL RESPONSES

        Metric(int count, Double percent) {
            this.count = count;
            this.percent = percent;
        }

        @Override
        public String toString() {
            return count + (percent == null ? "" : " (" + percent + "%)");
        }
    }

    /** label e.g. "TOTAL RESPONSES", "LOW RISK", "MEDIUM RISK", "HIGH RISK", "CRITICAL". */
    public Metric getMetric(String label) {
        String up = label.toUpperCase();
        WebElement node = visible(By.xpath(
                "//*[" + UPPER + "='" + up + "' and not(.//*[" + UPPER + "='" + up + "'])]"));
        for (int i = 0; i < 4; i++) {
            node = node.findElement(By.xpath(".."));
            List<String> lines = Arrays.stream(node.getText().split("\\R"))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
            int idx = -1;
            for (int j = 0; j < lines.size(); j++) {
                if (lines.get(j).equalsIgnoreCase(label)) { idx = j; break; }
            }
            if (idx > 0 && lines.get(idx - 1).replace(",", "").matches("\\d+")) {
                int count = Integer.parseInt(lines.get(idx - 1).replace(",", ""));
                Double pct = null;
                if (idx + 1 < lines.size() && lines.get(idx + 1).matches("[\\d.]+%")) {
                    pct = Double.parseDouble(lines.get(idx + 1).replace("%", ""));
                }
                return new Metric(count, pct);
            }
        }
        throw new NoSuchElementException("Could not read metric card: " + label);
    }

    public int getTotalResponses() {
        return getMetric("TOTAL RESPONSES").count;
    }

    /** Critical card may sit on a second row (screenshot 5) - returns null if not found. */
    public Metric getMetricOrNull(String label) {
        try {
            return getMetric(label);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
