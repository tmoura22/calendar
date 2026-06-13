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

class AgendaLxProviderTest {

    private StubServer server;
    private AgendaLxProvider provider;

    @BeforeEach
    void start() {
        server = new StubServer().run();
        provider = new AgendaLxProvider();
        
        RestClient testClient = RestClient.builder()
                .baseUrl("http://localhost:" + server.getPort())
                .defaultHeader("User-Agent", "Mozilla/5.0 (compatible; meetings-app/0.1; +http://localhost)")
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
            [
              {
                "id": 999,
                "title": { "rendered": "Art Expo" },
                "description": ["<p>Some art.</p>"],
                "occurences": ["2026-08-01"],
                "string_times": "qui: 18h30",
                "link": "http://agendalx.pt/expo",
                "venue": {
                  "123": { "name": "Gallery" }
                }
              }
            ]
        """;

        whenHttp(server)
            .match(get("/events"), parameter("search", "Lisbon"))
            .then(status(OK_200), contentType("application/json"), stringContent(jsonResponse));

        List<DiscoveredEvent> events = provider.search("Lisbon");

        assertThat(events).hasSize(1);
        DiscoveredEvent event = events.get(0);
        assertThat(event.title()).isEqualTo("Art Expo");
        assertThat(event.source()).isEqualTo("Agenda Cultural de Lisboa");
        assertThat(event.externalId()).isEqualTo("999");
        assertThat(event.url()).isEqualTo("http://agendalx.pt/expo");
        assertThat(event.venue()).isEqualTo("Gallery");
        assertThat(event.description()).isEqualTo("Some art.");
    }
}
