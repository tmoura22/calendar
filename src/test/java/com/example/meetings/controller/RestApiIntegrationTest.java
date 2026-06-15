package com.example.meetings.controller;

import com.example.meetings.model.User;
import com.example.meetings.repository.UserRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RestApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void accessSecuredEndpoint_withoutLogin_redirectsToLogin() {
        given()
                .redirects().follow(false)
                .when()
                .get("/calendar")
                .then()
                .statusCode(302)
                .header("Location", containsString("http://localhost:" + port + "/login"));
    }

    @Test
    void getIcalFeed_withValidToken_returnsTextCalendar() {
        String uniqueName = "icaluser-" + java.util.UUID.randomUUID().toString();
        User user = new User(uniqueName, uniqueName + "@example.com", "hash");
        userRepository.save(user);

        given()
                .when()
                .get("/ical/" + user.getIcalToken() + ".ics")
                .then()
                .statusCode(200)
                .contentType("text/calendar;charset=UTF-8")
                .body(containsString("BEGIN:VCALENDAR"));
    }
}
