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

class TicketmasterProviderTest {

  private StubServer server;
  private TicketmasterProvider provider;

  @BeforeEach
  void start() {
    server = new StubServer().run();
    provider = new TicketmasterProvider("dummy-key", "PT");

    RestClient testClient = RestClient.builder()
        .baseUrl("http://localhost:" + server.getPort() + "/discovery/v2")
        .build();
    ReflectionTestUtils.setField(provider, "http", testClient);
  }

  @AfterEach
  void stop() {
    if (server != null) {
      server.stop();
    }
  }

  @Test
  void search_returnsCorrectlyParsedList() {
    String jsonResponse = """
            {
              "_embedded": {
                "events": [
                  {
                    "id": "Z698xZq2Z1785Gv",
                    "name": "Parque Linkado",
                    "url": "http://ticketmaster.com/event1",
                    "info": "Some info",
                    "dates": {
                      "start": {
                        "dateTime": "2026-07-01T20:00:00Z"
                      }
                    },
                    "_embedded": {
                      "venues": [
                        { "name": "Meo Arena" }
                      ]
                    }
                  }
                ]
              }
            }
        """;

    whenHttp(server)
        .match(get("/discovery/v2/events.json"), parameter("apikey", "dummy-key"), parameter("keyword", "Lisbon"))
        .then(status(OK_200), contentType("application/json"), stringContent(jsonResponse));

    List<DiscoveredEvent> events = provider.search("Lisbon");

    assertThat(events).hasSize(1);
    DiscoveredEvent event = events.get(0);
    assertThat(event.title()).isEqualTo("Parque Linkado");
    assertThat(event.source()).isEqualTo("Ticketmaster");
    assertThat(event.externalId()).isEqualTo("Z698xZq2Z1785Gv");
    assertThat(event.url()).isEqualTo("http://ticketmaster.com/event1");
    assertThat(event.venue()).isEqualTo("Meo Arena");
  }

  @Test
  void search_unconfigured_returnsEmptyList() {
    TicketmasterProvider unconfigured = new TicketmasterProvider("", "PT");
    assertThat(unconfigured.search("Lisbon")).isEmpty();
  }
}
