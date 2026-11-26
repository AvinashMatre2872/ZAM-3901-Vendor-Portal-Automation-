package com.zamplia.tests;

import com.zamplia.base.BaseTest;
import com.zamplia.pages.ExportHistoryPanel;
import com.zamplia.pages.ExportHistoryPanel.ExportRow;
import com.zamplia.utils.ConfigReader;
import com.zamplia.utils.CsvUtils;
import com.zamplia.utils.DownloadUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Export flow on Calibr8 Report (background export):
 * Export button -> new job in "Export History" -> status Completed -> Download -> CSV.
 */
public class Calibr8ExportTest extends BaseTest {

    /** Max seconds to wait for an export job to reach "Completed". */
    private static final int EXPORT_TIMEOUT_SECONDS = 300;

    private final int maxDays = ConfigReader.getInt("maxRangeDaysInclusive");
    private final LocalDate start = LocalDate.parse(ConfigReader.get("baseDate"));
    private final LocalDate end = start.plusDays(maxDays - 1L);
    private ExportHistoryPanel history;

    @BeforeMethod(alwaysRun = true)
    public void openCalibr8() {
        calibr8 = goToCalibr8();
        history = new ExportHistoryPanel(driver);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /** Clicks Export, waits for the new job in Export History to complete. Panel stays open. */
    private ExportRow exportAndWaitForCompletion() {
        history.open();
        int before = history.getLatestId();
        history.close();

        calibr8.clickExport();
        Assert.assertFalse(calibr8.isErrorDisplayed(2), "Error after clicking Export: " + calibr8.getErrorText());

        history.open();
        int id = history.waitForNewExport(before, 60);
        Assert.assertTrue(id > before, "No new export job appeared in Export History (latest # was " + before + ")");

        ExportRow row = history.waitForCompletion(id, EXPORT_TIMEOUT_SECONDS);
        Assert.assertNotNull(row, "Export #" + id + " disappeared from Export History");
        System.out.println("Export job: " + row);
        Assert.assertFalse(row.isFailed(), "Export #" + id + " failed: " + row);
        Assert.assertTrue(row.isCompleted(),
                "Export #" + id + " not completed within " + EXPORT_TIMEOUT_SECONDS + "s: " + row);
        return row;
    }

    /** Clicks Download on the given job and returns the downloaded CSV. */
    private File download(ExportRow row) {
        Set<String> before = DownloadUtils.snapshot(downloadDir);
        history.clickDownload(row.id);
        File f = DownloadUtils.waitForNewFile(downloadDir, before, 60);
        Assert.assertNotNull(f, "Download of export #" + row.id + " did not produce a file in " + downloadDir);
        System.out.println("Downloaded: " + f.getAbsolutePath() + " (" + f.length() + " bytes)");
        return f;
    }

    /** CSV checks: header present, data rows = "Rows" column, all dates inside the selected range (+/-1 day for timezone). */
    private List<List<String>> verifyCsv(File csv, ExportRow row, LocalDate from, LocalDate to) throws IOException {
        Assert.assertTrue(csv.getName().toLowerCase().endsWith(".csv"), "Export is not a CSV: " + csv.getName());
        Assert.assertTrue(csv.length() > 0, "CSV is empty");

        List<List<String>> rows = CsvUtils.read(csv);
        Assert.assertFalse(rows.isEmpty(), "CSV has no header row");
        List<String> header = rows.get(0);
        System.out.println("CSV header: " + header);
        Assert.assertTrue(header.size() > 1, "CSV header looks wrong: " + header);

        int dataRows = rows.size() - 1;
        Assert.assertEquals(dataRows, row.rows,
                "CSV data rows (" + dataRows + ") != 'Rows' in Export History (" + row.rows + ")");

        int dateCol = CsvUtils.columnIndex(header, "date", "created", "timestamp", "time");
        if (dateCol < 0) {
            System.out.println("No date column found in CSV - date range check skipped");
            return rows;
        }
        for (int i = 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            if (dateCol >= r.size()) continue;
            LocalDate d = CsvUtils.extractDate(r.get(dateCol));
            if (d == null) continue;
            Assert.assertFalse(d.isBefore(from.minusDays(1)) || d.isAfter(to.plusDays(1)),
                    "CSV row " + i + " date " + d + " is outside selected range " + from + " -> " + to);
        }
        return rows;
    }

    // =====================================================================
    // Tests
    // =====================================================================

    @Test(priority = 1, description = "TC_EX_01: Export and Export History buttons are visible and enabled")
    public void exportButtonsVisible() {
        Assert.assertTrue(calibr8.isExportButtonEnabled(), "Export button disabled/missing");
        Assert.assertTrue(calibr8.isExportHistoryButtonDisplayed(), "Export History button missing");
    }

    @Test(priority = 2, description = "TC_EX_02: Export History panel shows correct columns, Refresh and closes")
    public void exportHistoryPanel() {
        history.open();
        List<String> headers = history.getHeaders();
        for (String col : new String[]{"#", "Requested", "Status", "Rows", "Size", "Completed"}) {
            Assert.assertTrue(headers.stream().anyMatch(h -> h.equalsIgnoreCase(col)),
                    "Column '" + col + "' missing in Export History: " + headers);
        }
        Assert.assertTrue(history.isRefreshButtonDisplayed(), "Refresh button missing");
        history.refresh();

        List<ExportRow> rows = history.getRows();
        for (int i = 1; i < rows.size(); i++) {
            Assert.assertTrue(rows.get(i - 1).id > rows.get(i).id, "Export History is not newest-first: " + rows);
        }
        history.close();
        Assert.assertFalse(history.isOpen(), "Export History did not close");
        Assert.assertTrue(calibr8.isLoaded(), "Calibr8 page not usable after closing Export History");
    }

    @Test(priority = 3, description = "TC_EX_03 (Positive): 1-month export -> job Completed -> CSV matches rows and date range")
    public void exportOneMonth() throws IOException {
        calibr8.setDateRange(start, end);
        calibr8.clickApplyFilters();

        ExportRow row = exportAndWaitForCompletion();
        Assert.assertTrue(row.rows > 0, "1-month export has 0 rows - pick a baseDate that has data");
        Assert.assertFalse(row.size.isEmpty() || row.size.startsWith("0 "), "Export size looks empty: " + row.size);
        Assert.assertFalse(row.completed.isEmpty(), "Completed time is empty");
        Assert.assertTrue(history.isDownloadButtonDisplayed(row.id), "Download button missing for #" + row.id);

        File csv = download(row);
        verifyCsv(csv, row, start, end);
    }

    @Test(priority = 4, description = "TC_EX_04 (Positive): 11-day export works (was blocked under old 10-day rule)")
    public void exportElevenDays() throws IOException {
        LocalDate elevenEnd = start.plusDays(10);
        calibr8.setDateRange(start, elevenEnd);
        calibr8.clickApplyFilters();

        ExportRow row = exportAndWaitForCompletion();
        File csv = download(row);
        verifyCsv(csv, row, start, elevenEnd);
    }

    @Test(priority = 5, description = "TC_EX_05 (Positive): 1-month + Country export contains only that country")
    public void exportWithCountry() throws IOException {
        calibr8.setDateRange(start, end);
        String country = calibr8.selectFirstRealOption("COUNTRY");
        calibr8.clickApplyFilters();

        ExportRow row = exportAndWaitForCompletion();
        File csv = download(row);
        List<List<String>> rows = verifyCsv(csv, row, start, end);

        int countryCol = CsvUtils.columnIndex(rows.get(0), "country");
        if (countryCol >= 0 && rows.size() > 1) {
            Set<String> values = CsvUtils.distinctValues(rows, countryCol);
            System.out.println("Selected country: " + country + " | CSV countries: " + values);
            Assert.assertTrue(values.size() <= 1,
                    "Export filtered by '" + country + "' contains several countries: " + values);
        }
    }

    @Test(priority = 6, description = "TC_EX_06: Export total rows >= export rows with a Country filter")
    public void filteredExportNotBiggerThanAll() {
        calibr8.setDateRange(start, end);
        calibr8.clickApplyFilters();
        ExportRow all = exportAndWaitForCompletion();
        history.close();

        calibr8.selectFirstRealOption("COUNTRY");
        calibr8.clickApplyFilters();
        ExportRow filtered = exportAndWaitForCompletion();

        Assert.assertTrue(filtered.rows <= all.rows,
                "Country export rows (" + filtered.rows + ") > all-countries rows (" + all.rows + ")");
    }

    @Test(priority = 7, description = "TC_EX_07 (Negative): No export job is created for a range above 1 month")
    public void exportAboveOneMonthBlocked() {
        history.open();
        int before = history.getLatestId();
        history.close();

        LocalDate badStart = end.minusDays(maxDays); // maxDays + 1 days
        calibr8.setDateRange(badStart, end);
        if (!calibr8.isRangeShown(badStart, end)) {
            return; // date picker refused the range -> export cannot be requested with it -> pass
        }
        calibr8.clickExport();
        boolean error = calibr8.isErrorDisplayed(5);

        history.open();
        pause(5000);
        history.refresh();
        int after = history.getLatestId();
        Assert.assertEquals(after, before, "Export job #" + after + " was created for a "
                + (maxDays + 1) + "-day range (error shown=" + error + ")");
    }

    @Test(priority = 8, description = "TC_EX_08: Download an existing completed export from Export History")
    public void downloadExistingExport() throws IOException {
        history.open();
        ExportRow completed = history.getRows().stream()
                .filter(ExportRow::isCompleted).findFirst().orElse(null);
        if (completed == null) {
            history.close();
            completed = exportAndWaitForCompletion();
        }
        File csv = download(completed);
        Assert.assertTrue(CsvUtils.read(csv).size() >= 1, "Downloaded CSV has no header");
    }

    private static void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
