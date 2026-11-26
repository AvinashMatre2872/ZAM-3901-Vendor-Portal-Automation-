package com.zamplia.base;

import com.zamplia.pages.Calibr8ReportPage;
import com.zamplia.pages.DashboardPage;
import com.zamplia.pages.LoginPage;
import com.zamplia.utils.ConfigReader;
import com.zamplia.utils.DriverFactory;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** One browser per test class; logs in once (unless the class overrides loginBeforeClass). */
public abstract class BaseTest {

    protected WebDriver driver;
    protected Calibr8ReportPage calibr8;
    protected File downloadDir;

    protected boolean loginBeforeClass() {
        return true;
    }

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        downloadDir = new File(ConfigReader.get("downloadDir")).getAbsoluteFile();
        downloadDir.mkdirs();
        driver = DriverFactory.create(downloadDir);
        if (loginBeforeClass()) {
            DashboardPage dashboard = new LoginPage(driver).open()
                    .loginAs(ConfigReader.get("username"), ConfigReader.get("password"));
            Assert.assertTrue(dashboard.isLoaded(), "Login failed - dashboard greeting not displayed");
        }
    }

    /** Fresh Calibr8 page for every test (refresh clears any previous filter state). */
    protected Calibr8ReportPage goToCalibr8() {
        driver.navigate().refresh();
        DashboardPage dashboard = new DashboardPage(driver);
        Assert.assertTrue(dashboard.isLoaded(), "Session lost - header greeting not displayed");
        return dashboard.openReports().openCalibr8Report();
    }

    @AfterMethod(alwaysRun = true)
    public void screenshotOnFailure(ITestResult result) {
        if (result.getStatus() != ITestResult.FAILURE || driver == null) return;
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File dir = new File("target/screenshots");
            dir.mkdirs();
            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            File dest = new File(dir, result.getMethod().getMethodName() + "_" + ts + ".png");
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Screenshot saved: " + dest.getAbsolutePath());
        } catch (IOException | RuntimeException e) {
            System.out.println("Could not save screenshot: " + e.getMessage());
        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (driver != null) driver.quit();
    }
}
