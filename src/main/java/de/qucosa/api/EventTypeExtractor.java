package de.qucosa.api;

public class EventTypeExtractor {

    public static final String extract(String eventType) {
        String event = eventType.substring((eventType.lastIndexOf("#") + 1), eventType.length());
        return event.toLowerCase();
    }
}
