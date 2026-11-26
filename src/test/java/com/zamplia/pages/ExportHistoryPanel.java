package com.zamplia.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.ArrayList;
import java.util.List;

/**
 * "Export History" side panel on Calibr8 Report.
 * Columns: # | Requested | Status | Rows | Size | Completed | Download
 * Exports run in the background: Export button -> new row -> status "Completed" -> Download (CSV).
 */
public class ExportHistoryPanel extends BasePage {

    private static final String PANEL =
            "//*[(contains(@class,'ant-drawer-content') or contains(@class,'ant-modal-content'))"
                    + " and .//*[normalize-space(.)='Export History']]";

    private final By openButton = By.xpath("//button[contains(normalize-space(.),'Export History')]");
    private final By panel = By.xpath(PANEL);
    private final By refreshButton = By.xpath(PANEL + "//button[contains(normalize-space(.),'Refresh')]");
    private final By closeButton = By.xpath(PANEL
            + "//*[contains(@class,'ant-drawer-close') or contains(@class,'ant-modal-close')]");
    private final By headerCells = By.xpath(PANEL + "//thead//th");
    private final By bodyRows = By.xpath(PANEL + "//tbody/tr[not(contains(@class,'ant-table-measure-row'))"
            + " and not(contains(@class,'ant-table-placeholder'))]");

    public ExportHistoryPanel(WebDriver driver) {
        super(driver);
    }

    /** One row of the Export History table. */
    public static class ExportRow {
        public int id;
        public String requested;
        public String status;
        public int rows;
        public String size;
        public String completed;

        public boolean isCompleted() {
            return status != null && status.toLowerCase().contains("complete");
        }

        public boolean isFailed() {
            String s = status == null ? "" : status.toLowerCase();
            return s.contains("fail") || s.contains("error");
        }

        @Override
        public String toString() {
            return "#" + id + " | " + requested + " | " + status + " | rows=" + rows + " | " + size + " | " + completed;
        }
    }

    // ------------------------------------------------------------ open / close
    public boolean isOpen() {
        for (WebElement p : driver.findElements(panel)) {
            if (p.isDisplayed()) return true;
        }
        return false;
    }

    public ExportHistoryPanel open() {
        if (!isOpen()) click(openButton);
        visible(panel);
        waitForLoading();
        pause(800);
        return this;
    }

    public void close() {
        if (!isOpen()) return;
        click(closeButton);
        wait.until(ExpectedConditions.invisibilityOfElementLocated(panel));
        pause(400);
    }

    public void refresh() {
        click(refreshButton);
        waitForLoading();
        pause(1000);
    }

    public boolean isRefreshButtonDisplayed() {
        return isDisplayed(refreshButton, 3);
    }

    // ------------------------------------------------------------ table
    public List<String> getHeaders() {
        List<String> out = new ArrayList<>();
        for (WebElement th : driver.findElements(headerCells)) {
            out.add(th.getText().trim());
        }
        return out;
    }

    private static int indexOf(List<String> headers, String name) {
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).equalsIgnoreCase(name)) return i;
        }
        return -1;
    }

    private static int toInt(String s) {
        String digits = s == null ? "" : s.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? -1 : Integer.parseInt(digits);
    }

    private static String cell(List<WebElement> tds, int idx) {
        return (idx >= 0 && idx < tds.size()) ? tds.get(idx).getText().trim() : "";
    }

    /** Reads all rows on the current page of the table (retries if the table re-renders). */
    public List<ExportRow> getRows() {
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                List<String> h = getHeaders();
                int iId = indexOf(h, "#"), iReq = indexOf(h, "Requested"), iStatus = indexOf(h, "Status"),
                        iRows = indexOf(h, "Rows"), iSize = indexOf(h, "Size"), iDone = indexOf(h, "Completed");
                List<ExportRow> out = new ArrayList<>();
                for (WebElement tr : driver.findElements(bodyRows)) {
                    List<WebElement> tds = tr.findElements(By.xpath("./td"));
                    if (tds.size() < 3) continue;
                    ExportRow r = new ExportRow();
                    r.id = toInt(cell(tds, iId));
                    r.requested = cell(tds, iReq);
                    r.status = cell(tds, iStatus);
                    r.rows = toInt(cell(tds, iRows));
                    r.size = cell(tds, iSize);
                    r.completed = cell(tds, iDone);
                    if (r.id > 0) out.add(r);
                }
                return out;
            } catch (StaleElementReferenceException e) {
                pause(500);
            }
        }
        return new ArrayList<>();
    }

    /** Highest export # in the table, 0 if the table is empty. */
    public int getLatestId() {
        int max = 0;
        for (ExportRow r : getRows()) max = Math.max(max, r.id);
        return max;
    }

    public ExportRow findRow(int id) {
        for (ExportRow r : getRows()) {
            if (r.id == id) return r;
        }
        return null;
    }

    // ------------------------------------------------------------ waiting
    /** Waits until a row with # greater than previousMaxId appears. Returns its #, or -1 on timeout. */
    public int waitForNewExport(int previousMaxId, int timeoutSeconds) {
        long end = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < end) {
            int latest = getLatestId();
            if (latest > previousMaxId) return latest;
            pause(3000);
            refresh();
        }
        return -1;
    }

    /** Waits until export #id is Completed or Failed. Returns the last row read (check its status). */
    public ExportRow waitForCompletion(int id, int timeoutSeconds) {
        long end = System.currentTimeMillis() + timeoutSeconds * 1000L;
        ExportRow row = null;
        while (System.currentTimeMillis() < end) {
            row = findRow(id);
            if (row != null && (row.isCompleted() || row.isFailed())) return row;
            pause(5000);
            refresh();
        }
        return row;
    }

    // ------------------------------------------------------------ download
    public void clickDownload(int id) {
        By btn = By.xpath(PANEL + "//tbody/tr[td[1][normalize-space(.)='" + id + "']]"
                + "//*[self::button or self::a][contains(normalize-space(.),'Download')]");
        click(btn);
    }

    public boolean isDownloadButtonDisplayed(int id) {
        By btn = By.xpath(PANEL + "//tbody/tr[td[1][normalize-space(.)='" + id + "']]"
                + "//*[self::button or self::a][contains(normalize-space(.),'Download')]");
        return isDisplayed(btn, 3);
    }
}
