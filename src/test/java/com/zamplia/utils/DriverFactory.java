package com.zamplia.utils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public final class DriverFactory {

    private DriverFactory() { }

    public static WebDriver create(File downloadDir) {
        String browser = ConfigReader.get("browser").toLowerCase();
        boolean headless = Boolean.parseBoolean(ConfigReader.get("headless"));

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", downloadDir.getAbsolutePath());
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("safebrowsing.enabled", true);

        WebDriver driver;
        switch (browser) {
            case "firefox": {
                FirefoxOptions fo = new FirefoxOptions();
                fo.addPreference("browser.download.folderList", 2);
                fo.addPreference("browser.download.dir", downloadDir.getAbsolutePath());
                fo.addPreference("browser.helperApps.neverAsk.saveToDisk",
                        "text/csv,application/csv,application/vnd.ms-excel,"
                                + "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/octet-stream");
                if (headless) fo.addArguments("-headless");
                driver = new FirefoxDriver(fo);
                break;
            }
            case "edge": {
                EdgeOptions eo = new EdgeOptions();
                eo.setExperimentalOption("prefs", prefs);
                eo.addArguments("--window-size=1920,1080");
                if (headless) eo.addArguments("--headless=new");
                driver = new EdgeDriver(eo);
                break;
            }
            default: {
                ChromeOptions co = new ChromeOptions();
                co.setExperimentalOption("prefs", prefs);
                co.addArguments("--window-size=1920,1080", "--disable-notifications");
                if (headless) co.addArguments("--headless=new");
                driver = new ChromeDriver(co);
            }
        }
        if (!headless) driver.manage().window().maximize();
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
        return driver;
    }
}
