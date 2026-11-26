package com.zamplia.pages;

import org.openqa.selenium.WebDriver;

public class ReportsPage extends BasePage {

    public static final String[] REPORT_TABS = {
            "Report", "Calibr8 Report", "API Vendor Traffic Report", "API Vendor Term Report",
            "Reconciliation Report", "Downloaded Report", "Reconcile"
    };

    public ReportsPage(WebDriver driver) {
        super(driver);
    }

    public boolean isReportTabDisplayed(String name) {
        return isDisplayed(innermostExactText(name), 5) || isDisplayed(innermostText(name), 2);
    }

    public Calibr8ReportPage openCalibr8Report() {
        click(innermostText("Calibr8"));
        Calibr8ReportPage page = new Calibr8ReportPage(driver);
        page.waitUntilLoaded();
        return page;
    }
}
