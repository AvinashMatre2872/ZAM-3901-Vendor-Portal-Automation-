package com.zamplia.pages;

import com.zamplia.utils.ConfigReader;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public abstract class BasePage {

    /** XPath fragment: upper-cased, space-normalised text of the current node (labels are CSS upper-cased). */
    protected static final String UPPER =
            "translate(normalize-space(.),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')";

    /** Common validation / toast containers (Ant Design, Toastify, SweetAlert, generic). */
    protected static final String ERROR_CSS = String.join(", ",
            ".ant-message-error", ".ant-message-warning",
            ".ant-notification-notice-error", ".ant-notification-notice-warning",
            ".ant-form-item-explain-error", ".ant-alert-error",
            ".Toastify__toast--error", ".Toastify__toast--warning",
            ".swal2-icon-error", ".invalid-feedback", ".error-message");

    protected final WebDriver driver;
    protected final WebDriverWait wait;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(ConfigReader.getInt("explicitWait")));
    }

    protected WebElement visible(By by) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    protected WebElement clickable(By by) {
        return wait.until(ExpectedConditions.elementToBeClickable(by));
    }

    protected void click(By by) {
        WebElement el = clickable(by);
        scrollIntoView(el);
        try {
            el.click();
        } catch (ElementClickInterceptedException | StaleElementReferenceException e) {
            jsClick(driver.findElement(by));
        }
    }

    protected void jsClick(WebElement el) {
        js().executeScript("arguments[0].click();", el);
    }

    protected void type(By by, String text) {
        WebElement el = visible(by);
        el.sendKeys(Keys.chord(selectAllKey(), "a"), Keys.BACK_SPACE);
        if (text != null && !text.isEmpty()) el.sendKeys(text);
    }

    protected boolean isDisplayed(By by, int seconds) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(seconds))
                    .until(ExpectedConditions.visibilityOfElementLocated(by));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    protected void scrollIntoView(WebElement el) {
        js().executeScript("arguments[0].scrollIntoView({block:'center'});", el);
    }

    protected JavascriptExecutor js() {
        return (JavascriptExecutor) driver;
    }

    protected Keys selectAllKey() {
        return System.getProperty("os.name").toLowerCase().contains("mac") ? Keys.COMMAND : Keys.CONTROL;
    }

    protected void pressEscape() {
        new Actions(driver).sendKeys(Keys.ESCAPE).perform();
        pause(300);
    }

    /** Waits for spinners / skeletons to go away (data refresh after Apply etc.). */
    public void waitForLoading() {
        pause(500);
        By loaders = By.cssSelector(".ant-spin-spinning, .ant-skeleton-active, .loader, .spinner-border");
        try {
            new WebDriverWait(driver, Duration.ofSeconds(30))
                    .until(ExpectedConditions.invisibilityOfElementLocated(loaders));
        } catch (TimeoutException ignored) {
            // page keeps a spinner somewhere - continue, assertions will catch real problems
        }
    }

    public boolean isErrorDisplayed(int seconds) {
        return isDisplayed(By.cssSelector(ERROR_CSS), seconds);
    }

    public String getErrorText() {
        List<WebElement> els = driver.findElements(By.cssSelector(ERROR_CSS));
        StringBuilder sb = new StringBuilder();
        for (WebElement e : els) {
            if (e.isDisplayed()) sb.append(e.getText().trim()).append(" | ");
        }
        return sb.toString();
    }

    protected static void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /** Deepest element whose text CONTAINS the value (ignores script/style/title). */
    protected static By innermostText(String text) {
        return By.xpath(String.format(
                "//*[not(self::script or self::style or self::title)][contains(normalize-space(.),'%1$s')"
                        + " and not(.//*[contains(normalize-space(.),'%1$s')])]", text));
    }

    /** Deepest element whose text EQUALS the value. */
    protected static By innermostExactText(String text) {
        return By.xpath(String.format(
                "//*[not(self::script or self::style or self::title)][normalize-space(.)='%1$s'"
                        + " and not(.//*[normalize-space(.)='%1$s'])]", text));
    }
}
