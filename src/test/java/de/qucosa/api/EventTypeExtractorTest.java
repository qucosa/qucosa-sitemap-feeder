package de.qucosa.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Test for extract event type from jms event uri.")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EventTypeExtractorTest {

    @Test
    @DisplayName("Test extract create event.")
    public void testExtractCreate() {
        String event = EventTypeExtractor.extract("https://www.w3.org/ns/activitystreams#Create");
        assertEquals("create", event);
    }

    @Test
    @DisplayName("Test extract update event.")
    public void testExtractUpdate() {
        String event = EventTypeExtractor.extract("https://www.w3.org/ns/activitystreams#Update");
        assertEquals("update", event);
    }

    @Test
    @DisplayName("Test extract delete event.")
    public void testExtractDelete() {
        String event = EventTypeExtractor.extract("https://www.w3.org/ns/activitystreams#Delete");
        assertEquals("delete", event);
    }
}
