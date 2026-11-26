# Calibr8 Report – Selenium Java (TestNG) Automation

## Run
```
mvn clean test                                   # all suites (Chrome)
mvn clean test -Dbrowser=edge -Dheadless=true    # override any config key
mvn clean test -Dtest=Calibr8DateRangeTest       # one class
```
Requires Java 11+, Maven, Chrome/Edge/Firefox. Selenium Manager downloads the driver automatically.

## Structure
```
src/test/java/com/zamplia/
  base/BaseTest.java              browser setup, login, screenshot on failure
  pages/LoginPage, DashboardPage, ReportsPage, Calibr8ReportPage   (all locators live here)
  tests/LoginTest, Calibr8DateRangeTest, Calibr8FilterTest, Calibr8ExportTest
  utils/ConfigReader, DriverFactory, DownloadUtils
src/test/resources/config.properties   URL, credentials, max range days, base date
TEST_CASES.md                           full test case list
```
Reports: `target/surefire-reports/index.html` · Failure screenshots: `target/screenshots/` · Downloads: `target/downloads/`

## Notes
- Locators target Ant Design components (RangePicker, Select, Tabs) as seen in the screenshots.
  If one fails on the first run, fix it only in the page class.
- `maxRangeDaysInclusive=31` defines "1 month". Change to 30 if dev implemented 30 days.
- `baseDate` should be inside a period that has data (October 2026 has data for lucid_api).
