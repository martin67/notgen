package se.terrassorkestern.notgen.web;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import se.terrassorkestern.notgen.NotgenApplication;

import javax.servlet.ServletContext;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest(classes = NotgenApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Disabled
class WebTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ServletContext servletContext;

    private static WebDriver driver;

    @BeforeAll
    static void setup() {
        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
    }

    @AfterAll
    static void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    void basicNavigation() {
        // Always wait 30 seconds to locate elements
        driver.manage().timeouts().implicitlyWait(60, SECONDS);

        driver.get("http://localhost:" + port + servletContext.getContextPath() + "/");

        // Verify first page title
        assertEquals("Notgeneratorn", driver.getTitle());

        // Click on link
        driver.findElement(By.linkText("Repertoire")).click();
        driver.findElement(By.cssSelector("tr:nth-child(1) .material-icons")).click();
        driver.findElement(By.linkText("Tillbaka")).click();

        driver.findElement(By.linkText("Låtlistor")).click();
        driver.findElement(By.cssSelector("tr:nth-child(1) a:nth-child(1) > .material-icons")).click();
        driver.findElement(By.linkText("Tillbaka")).click();
    }
}
