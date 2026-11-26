# Test Cases – Calibr8 Report: Date Range 10 Days → 1 Month

**Ticket:** Vendors need the Calibr8 Report date range extended from 10 days to 1 month.
**Path:** https://vendor.dev.zamplia.com/ → Login (`lucid_api` / `123456789`, Remember me ticked) → Reports → Calibr8 Report
**Rule under test:** max range = 31 days inclusive (e.g. 01-Oct → 31-Oct). Old max = 10 days.

> Confirm with dev: is "1 month" = 31 days, 30 days, or "same date next month"? Change `maxRangeDaysInclusive` in `config.properties` to match; every boundary test recalculates from it.

## 1. Login & Navigation

| ID | Scenario | Type | Expected Result | Automated |
|---|---|---|---|---|
| TC_LG_01 | Login page shows User Name, Password, Remember me, Login, Sign Up | Positive | All elements visible | LoginTest.loginPageElementsDisplayed |
| TC_LG_08 | Remember me is unticked by default and can be ticked | Positive | Checkbox becomes ticked | rememberMeCheckbox |
| TC_LG_02 | Password masked; eye icon reveals it | Positive | type=password → type=text | passwordMaskAndToggle |
| TC_LG_03 | Click Login with both fields empty | Negative | Stays on login, no dashboard | emptyCredentials |
| TC_LG_04 | Valid username + wrong password | Negative | Error shown, no dashboard | wrongPassword |
| TC_LG_05 | Invalid username + valid password | Negative | Error shown, no dashboard | invalidUsername |
| TC_LG_06 | Username only, blank password | Negative | Stays on login | passwordBlank |
| TC_LG_07 | Valid login (lucid_api / 123456789) with Remember me ticked | Positive | "Hello, Lucid_API !" shown | validLogin |
| TC_NAV_01 | Reports shows 7 tabs; Calibr8 Report opens with filters | Positive | All tabs + date picker visible | endToEndNavigationToCalibr8 |

## 2. Date Range (core of the ticket)

| ID | Scenario | Type | Expected Result | Automated |
|---|---|---|---|---|
| TC_DR_01 | Date range picker displayed | Positive | Start & End inputs visible | datePickerDisplayed |
| TC_DR_02 | Default pre-filled range | Positive | ≤ 31 days | defaultRangeWithinLimit |
| TC_DR_03a | Single day (start = end) | Boundary | Accepted, data loads | validRangeAccepted |
| TC_DR_03b | Exactly 10 days (old limit) | Positive | Accepted | validRangeAccepted |
| TC_DR_03c | **11 days (was blocked before)** | Positive | **Now accepted** | validRangeAccepted |
| TC_DR_03d | 15 days | Positive | Accepted | validRangeAccepted |
| TC_DR_03e | 30 days (max − 1) | Boundary | Accepted | validRangeAccepted |
| TC_DR_03f | **31 days (exact max)** | Boundary | **Accepted** | validRangeAccepted |
| TC_DR_03g | Full 31-day month (Oct) | Positive | Accepted | validRangeAccepted |
| TC_DR_03h | Full 30-day month (Sep) | Positive | Accepted | validRangeAccepted |
| TC_DR_03i | Cross-month (15 Sep – 14 Oct) | Positive | Accepted | validRangeAccepted |
| TC_DR_03j | February non-leap (28 days) | Positive | Accepted | validRangeAccepted |
| TC_DR_03k | February leap year (29 days) | Positive | Accepted | validRangeAccepted |
| TC_DR_03l | Cross-year (20 Dec – 10 Jan) | Positive | Accepted | validRangeAccepted |
| TC_DR_04a | **32 days (max + 1)** | Boundary/Negative | **Blocked** (disabled date or validation message) | rangeAboveOneMonthRejected |
| TC_DR_04b | 33 days | Negative | Blocked | rangeAboveOneMonthRejected |
| TC_DR_04c | 45 days | Negative | Blocked | rangeAboveOneMonthRejected |
| TC_DR_04d | 60 days | Negative | Blocked | rangeAboveOneMonthRejected |
| TC_DR_04e | 90 days | Negative | Blocked | rangeAboveOneMonthRejected |
| TC_DR_04f | 365 days | Negative | Blocked | rangeAboveOneMonthRejected |
| TC_DR_04g | Two full months (1 Sep – 31 Oct) | Negative | Blocked | rangeAboveOneMonthRejected |
| TC_DR_05 | Calendar: day 11 & day 31 enabled, day 32 disabled after picking start | Boundary | Correct enabled/disabled cells | calendarBoundaryCells |
| TC_DR_06 | Pick end date by mouse click (31st day) | Positive | Range shown in inputs | selectRangeByCalendarClick |
| TC_DR_07 | Start date after end date | Negative | Rejected or auto-corrected (start ≤ end) | startAfterEnd |
| TC_DR_08 | Garbage text / invalid date (abcd, 2026/13/45) | Negative | Not accepted | invalidDateFormat |
| TC_DR_09 | Clear range and Apply | Negative | Validation or default data, no crash | applyWithEmptyRange |
| TC_DR_10 | 31-day total ≥ total of its first 10 days | Data | Superset never returns fewer rows | largerRangeHasMoreOrEqualData |
| TC_DR_11 | Range persists across Overview / Deep Analysis / Responses / Geo Heatmap | Positive | Same dates on every tab | rangePersistsAcrossSubTabs |
| TC_DR_12 | Reset restores default range | Positive | Default dates back | resetRestoresDefaultRange |
| TC_DR_13 | Apply twice with same range | Positive | Same range, same totals | applyTwiceKeepsRange |

## 3. Filters (Country / Risk Level / Flag Key) with 1-month range

| ID | Scenario | Type | Expected Result | Automated |
|---|---|---|---|---|
| TC_FL_01 | Country default value | Positive | "All Countries" | countryDefault |
| TC_FL_02 | Country dropdown opens | Positive | Country list shown | countryOptionsListed |
| TC_FL_03 | Type partial name in Country | Positive | List filtered to matches | countrySearch |
| TC_FL_04 | Search non-existing country | Negative | "No data" / empty list | countrySearchNoMatch |
| TC_FL_05 | Select a country + Apply | Positive | Total ≤ all-countries total | selectCountryAndApply |
| TC_FL_06 | Risk Level options | Positive | Low, Medium, High, Critical present | riskLevelOptions |
| TC_FL_07 | Filter by each risk level (×4) | Positive | Other risk cards = 0 | filterByRiskLevel |
| TC_FL_08 | Flag Key default + options | Positive | "All Flags", options present | flagKeyOptions |
| TC_FL_09 | Select Flag Key + Apply | Positive | Total ≤ overall | selectFlagAndApply |
| TC_FL_10 | 1 month + Country + Risk + Flag together | Positive | Applies, range kept | combinedFilters |
| TC_FL_11 | Reset after selecting all filters | Positive | All dropdowns back to "All…" | resetClearsDropdowns |
| TC_FL_12 | Card math | Data | Σ risk counts ≤ Total; % = count/total | metricCardsConsistent |
| TC_FL_13 | Each sub tab opens (×4) | Positive | Tab active, no error | subTabOpens |

## 4. Export (background job → Export History → Download CSV)

| ID | Scenario | Type | Expected Result | Automated |
|---|---|---|---|---|
| TC_EX_01 | Export & Export History visible | Positive | Both visible, Export enabled | exportButtonsVisible |
| TC_EX_02 | Export History panel | Positive | Columns #, Requested, Status, Rows, Size, Completed; Refresh works; newest first; closes | exportHistoryPanel |
| TC_EX_03 | Export with 31-day range | Positive | New job → Completed, rows > 0, Download gives CSV, CSV rows = "Rows", dates within range | exportOneMonth |
| TC_EX_04 | Export with 11-day range | Positive | Job completes, CSV dates within 11 days | exportElevenDays |
| TC_EX_05 | Export 1 month + Country | Positive | CSV contains only the selected country | exportWithCountry |
| TC_EX_06 | All-countries vs Country export | Data | Country rows ≤ all rows | filteredExportNotBiggerThanAll |
| TC_EX_07 | Export with 32-day range | Negative | No new job in Export History | exportAboveOneMonthBlocked |
| TC_EX_08 | Download an existing completed export | Positive | CSV downloads | downloadExistingExport |

## 5. Manual-only checks (not automated)

| ID | Scenario | Expected |
|---|---|---|
| TC_M_01 | Validation message wording when > 1 month | Says "1 month / 31 days", not "10 days" |
| TC_M_02 | Tooltip / helper text near date picker | No leftover "10 days" text |
| TC_M_03 | API call directly with 32-day range (Postman) | Backend also rejects (not only UI) |
| TC_M_04 | Exported file rows are only inside the selected dates | No rows outside range |
| TC_M_05 | Other report tabs (Report, API Vendor Traffic, etc.) | Their date rules unchanged (regression) |
| TC_M_06 | Different vendor accounts (dynataapi_USD, MakeOpinion) | Same 1-month rule applies |
| TC_M_07 | Performance with 31-day range | Charts load in acceptable time |
