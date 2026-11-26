package com.zamplia.pages;

import com.zamplia.utils.ConfigReader;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.testng.Assert;

public class LoginPage extends BasePage {

    private final By usernameInput = By.xpath(
            "//input[@placeholder='Enter Your User Name' or @name='username' or @id='username']");
    private final By passwordInput = By.xpath(
            "//input[@placeholder='Enter Your Password' or @name='password' or @id='password']");
    private final By loginButton = By.xpath("//button[normalize-space(.)='Login']");
    /** Used only if the base URL lands on a landing page instead of the login form. */
    private final By loginEntryLink = By.xpath(
            "//a[contains(translate(normalize-space(.),'LOGIN','login'),'login')]"
                    + " | //button[contains(translate(normalize-space(.),'LOGIN','login'),'login')]");

    // "Remember me" is a custom checkbox (from DevTools):
    // <div class="w-5 h-5 ... bg-gradient-to-r from-customPurple ..."><svg .../></div>
    // <span class="block text-sm ...">Remember me</span>
    private final By rememberMeLabel = By.xpath("//span[normalize-space(.)='Remember me']");
    private final By rememberMeBox = By.xpath("//span[normalize-space(.)='Remember me']/preceding-sibling::div[1]");

    private final By passwordToggle = By.xpath(
            "(//input[@placeholder='Enter Your Password']/ancestor::*[.//*[name()='svg']][1]//*[name()='svg'])[1]");
    private final By signUpLink = By.xpath("//*[normalize-space(.)='Sign Up']");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    public LoginPage open() {
        driver.get(ConfigReader.get("baseUrl"));
        if (!isDisplayed(usernameInput, 8)) {
            click(loginEntryLink);
            visible(usernameInput);
        }
        return this;
    }

    public boolean isLoginFormDisplayed() {
        return isDisplayed(usernameInput, 10) && isDisplayed(passwordInput, 2) && isDisplayed(loginButton, 2);
    }

    public boolean isLoginButtonDisplayed(int seconds) {
        return isDisplayed(loginButton, seconds);
    }

    // ------------------------- Remember me -------------------------
    public boolean isRememberMeDisplayed() {
        return isDisplayed(rememberMeLabel, 5) && isDisplayed(rememberMeBox, 2);
    }

    /** Ticked state = the box has the purple gradient classes. */
    public boolean isRememberMeChecked() {
        String cls = visible(rememberMeBox).getAttribute("class");
        return cls != null && (cls.contains("from-customPurple") || cls.contains("bg-gradient"));
    }

    /** Ticks or unticks Remember me (only clicks if the state needs to change). */
    public LoginPage setRememberMe(boolean checked) {
        if (isRememberMeChecked() != checked) {
            click(rememberMeBox);
            pause(400);
        }
        return this;
    }

    public LoginPage checkRememberMe() {
        return setRememberMe(true);
    }

    // ------------------------- Actions -------------------------
    public boolean isSignUpLinkDisplayed() {
        return isDisplayed(signUpLink, 3);
    }

    public LoginPage enterUsername(String u) {
        type(usernameInput, u);
        return this;
    }

    public LoginPage enterPassword(String p) {
        type(passwordInput, p);
        return this;
    }

    public void clickLogin() {
        click(loginButton);
    }

    /** Username -> Password -> tick Remember me -> Login. */
    public DashboardPage loginAs(String user, String pass) {
        enterUsername(user).enterPassword(pass).checkRememberMe();
        Assert.assertTrue(isRememberMeChecked(), "'Remember me' checkbox is not ticked before login");
        clickLogin();
        return new DashboardPage(driver);
    }

    public String getPasswordFieldType() {
        return visible(passwordInput).getAttribute("type");
    }

    public void togglePasswordVisibility() {
        WebElement eye = visible(passwordToggle);
        new Actions(driver).moveToElement(eye).click().perform();
        pause(300);
    }
}
