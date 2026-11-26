package com.zamplia.tests;

import com.zamplia.base.BaseTest;
import com.zamplia.pages.Calibr8ReportPage;
import com.zamplia.pages.DashboardPage;
import com.zamplia.pages.LoginPage;
import com.zamplia.pages.ReportsPage;
import com.zamplia.utils.ConfigReader;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class LoginTest extends BaseTest {

    private final String user = ConfigReader.get("username");
    private final String pass = ConfigReader.get("password");
    private LoginPage login;

    @Override
    protected boolean loginBeforeClass() {
        return false;
    }

    @BeforeMethod(alwaysRun = true)
    public void freshSession() {
        driver.get(ConfigReader.get("baseUrl"));
        driver.manage().deleteAllCookies();
        ((org.openqa.selenium.JavascriptExecutor) driver)
                .executeScript("window.localStorage.clear(); window.sessionStorage.clear();");
        login = new LoginPage(driver).open();
    }

    @Test(priority = 1, description = "TC_LG_01: Login page shows username, password, Remember me, Login and Sign Up")
    public void loginPageElementsDisplayed() {
        Assert.assertTrue(login.isLoginFormDisplayed(), "Login form not displayed");
        Assert.assertTrue(login.isRememberMeDisplayed(), "Remember me checkbox/label missing");
        Assert.assertTrue(login.isSignUpLinkDisplayed(), "Sign Up link missing");
    }

    @Test(priority = 2, description = "TC_LG_08: 'Remember me' checkbox can be ticked and unticked")
    public void rememberMeCheckbox() {
        Assert.assertTrue(login.isRememberMeDisplayed(), "Remember me missing");
        login.setRememberMe(true);
        Assert.assertTrue(login.isRememberMeChecked(), "Remember me did not get ticked");
        login.setRememberMe(false);
        Assert.assertFalse(login.isRememberMeChecked(), "Remember me did not get unticked");
        login.setRememberMe(true);
        Assert.assertTrue(login.isRememberMeChecked(), "Remember me did not get ticked again");
    }

    @Test(priority = 2, description = "TC_LG_02: Password is masked and eye icon toggles visibility")
    public void passwordMaskAndToggle() {
        login.enterPassword(pass);
        Assert.assertEquals(login.getPasswordFieldType(), "password", "Password should be masked");
        login.togglePasswordVisibility();
        Assert.assertEquals(login.getPasswordFieldType(), "text", "Eye icon should reveal password");
    }

    @Test(priority = 3, description = "TC_LG_03 (Negative): Empty username and password")
    public void emptyCredentials() {
        login.clickLogin();
        Assert.assertTrue(login.isLoginButtonDisplayed(5), "User should remain on login page");
        Assert.assertFalse(new DashboardPage(driver).isLoadedQuick(), "Dashboard must not open");
    }

    @Test(priority = 4, description = "TC_LG_04 (Negative): Valid username, wrong password")
    public void wrongPassword() {
        login.loginAs(user, "wrongPass@123");
        Assert.assertTrue(login.isErrorDisplayed(8) || login.isLoginButtonDisplayed(5),
                "Error expected for wrong password");
        Assert.assertFalse(new DashboardPage(driver).isLoadedQuick(), "Dashboard must not open");
    }

    @Test(priority = 5, description = "TC_LG_05 (Negative): Invalid username")
    public void invalidUsername() {
        login.loginAs("invalid_user_xyz", pass);
        Assert.assertTrue(login.isErrorDisplayed(8) || login.isLoginButtonDisplayed(5),
                "Error expected for invalid username");
        Assert.assertFalse(new DashboardPage(driver).isLoadedQuick(), "Dashboard must not open");
    }

    @Test(priority = 6, description = "TC_LG_06 (Negative): Username only, password blank")
    public void passwordBlank() {
        login.enterUsername(user).clickLogin();
        Assert.assertTrue(login.isLoginButtonDisplayed(5), "User should remain on login page");
        Assert.assertFalse(new DashboardPage(driver).isLoadedQuick(), "Dashboard must not open");
    }

    @Test(priority = 7, description = "TC_LG_07: Valid login -> dashboard greets user")
    public void validLogin() {
        DashboardPage dashboard = login.loginAs(user, pass);
        Assert.assertTrue(dashboard.isLoaded(), "Dashboard not loaded after valid login");
        Assert.assertTrue(dashboard.getGreetingText().toLowerCase().contains(user.toLowerCase()),
                "Greeting should contain user name, was: " + dashboard.getGreetingText());
    }

    @Test(priority = 8, description = "TC_NAV_01: Login -> Reports shows all report tabs -> Calibr8 Report opens")
    public void endToEndNavigationToCalibr8() {
        DashboardPage dashboard = login.loginAs(user, pass);
        Assert.assertTrue(dashboard.isLoaded());
        ReportsPage reports = dashboard.openReports();
        for (String tab : ReportsPage.REPORT_TABS) {
            Assert.assertTrue(reports.isReportTabDisplayed(tab), "Report tab missing: " + tab);
        }
        Calibr8ReportPage calibr8Page = reports.openCalibr8Report();
        Assert.assertTrue(calibr8Page.isLoaded(), "Calibr8 Report filters not displayed");
        Assert.assertTrue(calibr8Page.isDateRangePickerDisplayed(), "Date range picker not displayed");
    }
}
