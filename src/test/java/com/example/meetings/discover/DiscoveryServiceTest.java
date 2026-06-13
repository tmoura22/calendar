package com.example.meetings.discover;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscoveryServiceTest {

    @Mock
    private EventProvider provider1;

    @Mock
    private EventProvider provider2;

    @Test
    void search_blankQuery_returnsEmpty() {
        DiscoveryService service = new DiscoveryService(List.of(provider1));
        assertThat(service.search("")).isEmpty();
        assertThat(service.search(null)).isEmpty();
    }

    @Test
    void search_ignoresUnconfiguredProviders() {
        DiscoveryService service = new DiscoveryService(List.of(provider1));
        when(provider1.isConfigured()).thenReturn(false);

        List<DiscoveredEvent> results = service.search("query");
        assertThat(results).isEmpty();
    }

    @Test
    void search_mergesAndDedupesAndSorts() {
        DiscoveryService service = new DiscoveryService(List.of(provider1, provider2));
        when(provider1.isConfigured()).thenReturn(true);
        when(provider2.isConfigured()).thenReturn(true);

        DiscoveredEvent event1 = new DiscoveredEvent("P1", "1", "Event 1", "desc", Instant.parse("2026-07-02T10:00:00Z"), null, "http://url1", null);
        DiscoveredEvent event2 = new DiscoveredEvent("P2", "1", "Event 2", "desc", Instant.parse("2026-07-01T10:00:00Z"), null, "http://url1", null); // Duplicate URL
        DiscoveredEvent event3 = new DiscoveredEvent("P2", "2", "Event 3", "desc", Instant.parse("2026-06-01T10:00:00Z"), null, "http://url2", null);

        when(provider1.search("query")).thenReturn(List.of(event1));
        when(provider2.search("query")).thenReturn(List.of(event2, event3));

        List<DiscoveredEvent> results = service.search("query");

        // event2 is ignored because its URL matches event1 (already seen)
        // event3 is included and should be first because it is earliest
        assertThat(results).hasSize(2);
        assertThat(results.get(0)).isEqualTo(event3);
        assertThat(results.get(1)).isEqualTo(event1);
    }
}
