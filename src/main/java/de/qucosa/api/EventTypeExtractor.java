package de.qucosa.api;

public class EventTypeExtractor {

    public static String extract(String eventType) {
        String event = eventType.substring((eventType.lastIndexOf("#") + 1));
        return event.toLowerCase();
    }
}
