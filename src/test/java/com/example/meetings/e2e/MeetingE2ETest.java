package com.example.meetings.e2e;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.time.Duration;

import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MeetingE2ETest {

    @LocalServerPort
    private int port;

    private WebDriver driver;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    void setupTest() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
    }

    @AfterEach
    void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void userCanRegisterLoginAndProposeMeeting() {
        String baseUrl = "http://localhost:" + port;
        String uniqueUsername = "user" + System.currentTimeMillis();

        driver.get(baseUrl + "/register");
        driver.findElement(By.id("username")).sendKeys(uniqueUsername);
        driver.findElement(By.id("email")).sendKeys(uniqueUsername + "@example.com");
        driver.findElement(By.id("password")).sendKeys("password");
        driver.findElement(By.xpath("//button[text()='Register']")).click();

        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.urlContains("/login?registered"));

        driver.findElement(By.id("username")).sendKeys(uniqueUsername);
        driver.findElement(By.id("password")).sendKeys("password");
        driver.findElement(By.xpath("//button[text()='Sign in']")).click();

        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.urlContains("/calendar"));

        driver.get(baseUrl + "/meetings/new");
        driver.findElement(By.id("title")).sendKeys("E2E Test Sync");
        driver.findElement(By.id("description")).sendKeys("This is an end to end test.");

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("document.getElementById('start').value = '2030-01-01T10:00';");
        js.executeScript("document.getElementById('end').value = '2030-01-01T11:00';");

        driver.findElement(By.xpath("//button[text()='Propose']")).click();

        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.urlContains("/calendar"));

        String pageSource = driver.getPageSource();
        assertThat(pageSource).contains("E2E Test Sync");
        assertThat(pageSource).contains(uniqueUsername);
    }
}
