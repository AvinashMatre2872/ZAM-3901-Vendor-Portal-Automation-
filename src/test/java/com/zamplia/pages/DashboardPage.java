package com.zamplia.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class DashboardPage extends BasePage {

    private final By greeting = By.xpath(
            "//*[starts-with(normalize-space(.),'Hello,') and not(.//*[starts-with(normalize-space(.),'Hello,')])]");
    private final By reportsMenu = innermostExactText("Reports");

    public DashboardPage(WebDriver driver) {
        super(driver);
    }

    public boolean isLoaded() {
        return isDisplayed(greeting, 20);
    }

    /** Short check used by negative tests so they don't wait 20s. */
    public boolean isLoadedQuick() {
        return isDisplayed(greeting, 3);
    }

    public String getGreetingText() {
        return visible(greeting).getText();
    }

    public ReportsPage openReports() {
        click(reportsMenu);
        waitForLoading();
        return new ReportsPage(driver);
    }
}
