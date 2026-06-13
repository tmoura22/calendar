package com.example.meetings.discover;

import com.xebialabs.restito.server.StubServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.status;
import static com.xebialabs.restito.semantics.Action.stringContent;
import static com.xebialabs.restito.semantics.Condition.get;
import static com.xebialabs.restito.semantics.Condition.parameter;
import static org.assertj.core.api.Assertions.assertThat;
import static org.glassfish.grizzly.http.util.HttpStatus.OK_200;

class SeatGeekProviderTest {

    private StubServer server;
    private SeatGeekProvider provider;

    @BeforeEach
    void start() {
        server = new StubServer().run();
        provider = new SeatGeekProvider("dummy-client-id");
        
        RestClient testClient = RestClient.builder()
                .baseUrl("http://localhost:" + server.getPort())
                .build();
        ReflectionTestUtils.setField(provider, "http", testClient);
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop();
    }

    @Test
    void search_returnsCorrectlyParsedList() {
        String jsonResponse = """
            {
              "events": [
                {
                  "id": 12345,
                  "title": "A Game",
                  "short_title": "Game",
                  "datetime_utc": "2026-07-01T15:00:00",
                  "url": "http://seatgeek.com/12345",
                  "description": "Sporting event",
                  "venue": {
                    "name": "Stadium"
                  }
                }
              ]
            }
        """;

        whenHttp(server)
            .match(get("/events"), parameter("client_id", "dummy-client-id"), parameter("q", "Lisbon"))
            .then(status(OK_200), contentType("application/json"), stringContent(jsonResponse));

        List<DiscoveredEvent> events = provider.search("Lisbon");

        assertThat(events).hasSize(1);
        DiscoveredEvent event = events.get(0);
        assertThat(event.title()).isEqualTo("A Game");
        assertThat(event.source()).isEqualTo("SeatGeek");
        assertThat(event.externalId()).isEqualTo("12345");
        assertThat(event.url()).isEqualTo("http://seatgeek.com/12345");
        assertThat(event.venue()).isEqualTo("Stadium");
    }

    @Test
    void search_unconfigured_returnsEmptyList() {
        SeatGeekProvider unconfigured = new SeatGeekProvider("");
        assertThat(unconfigured.search("Lisbon")).isEmpty();
    }
}
