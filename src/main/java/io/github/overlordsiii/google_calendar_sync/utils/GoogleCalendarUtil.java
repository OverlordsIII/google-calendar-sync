package io.github.overlordsiii.google_calendar_sync.utils;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.gson.JsonObject;
import io.github.overlordsiii.google_calendar_sync.GoogleCalendarSyncApplication;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.github.overlordsiii.google_calendar_sync.GoogleCalendarSyncApplication.SERVICE;

public class GoogleCalendarUtil {

    public static String getOrCreateCopy(String cbeId) throws IOException {
        String secondaryId = GoogleCalendarSyncApplication.CONFIG.getConfigOption("secondary-calendar-id");
        if (secondaryId != null) {
            return secondaryId;
        }
        com.google.api.services.calendar.model.Calendar calendar = new com.google.api.services.calendar.model.Calendar();
        calendar.setSummary("CBE-IT Schedule");
        calendar.setTimeZone("America/Los_Angeles");

        calendar = SERVICE.calendars().insert(calendar).execute();

        for (Event event : getAllEvents(cbeId, null)) {
            GoogleCalendarSyncApplication.LOGGER.info("Copying event: " + event.getSummary());

            copyEventTo(calendar.getId(), event);
        }
        GoogleCalendarSyncApplication.EVENT_CONFIG.save();

        return calendar.getId();
    }

    public static String getCBECalendarId() throws IOException {
        String cbeCalendarId = null;

        for (CalendarListEntry item : SERVICE.calendarList().list().execute().getItems()) {
            if (item.getSummary().contains("College of Built Environments")) {
                cbeCalendarId = item.getId();
            }
        }

        if (cbeCalendarId != null) {
            GoogleCalendarSyncApplication.LOGGER.info("CBE-Calendar ID: " + cbeCalendarId);
            GoogleCalendarSyncApplication.CONFIG.setConfigOption("primary-calendar-id", cbeCalendarId);
            GoogleCalendarSyncApplication.CONFIG.save();
        }

        return cbeCalendarId;
    }

    // null when you want to init events, non-null when you want new events (likely not going to be over 2500 i hope )
    public static List<Event> getAllEvents(String calendarId, @Nullable String syncToken) throws IOException {
        List<Event> items = new ArrayList<>();
        Calendar.Events.List eventsList = SERVICE.events().list(calendarId).setMaxResults(2500);
        if (syncToken != null) {
            eventsList.setSyncToken(syncToken);
        }
        Events events = eventsList.execute();
        syncToken = events.getNextSyncToken();
        while (events != null) {
            items.addAll(events.getItems());
            if (events.getNextPageToken() != null) {
                events = SERVICE
                        .events()
                        .list(calendarId)
                        .setMaxResults(2500)
                        .setPageToken(events.getNextPageToken())
                        .execute();
            } else {
                syncToken = events.getNextSyncToken(); // get the last sync token
                break;
            }
        }
        GoogleCalendarSyncApplication.CONFIG.setConfigOption("sync-token", syncToken);
        GoogleCalendarSyncApplication.CONFIG.save();
        return items;
    }

    public static void deleteEvent(String calendarId, String eventId) throws IOException {
        SERVICE.events().delete(calendarId, eventId).execute();
    }

    public static void copyEventTo(String calendarId, Event event) throws IOException {
        boolean archnet = event.getSummary().contains("ArchNet");

        Event newEvent = new Event()
                .setSummary(archnet ? "ArchNet" : "Digital Commons")
                .setDescription(event.getDescription())
                .setStart(event.getStart())
                .setEnd(event.getEnd())
                .setColorId(archnet ? "11" : "1");

        newEvent = SERVICE.events().insert(calendarId, newEvent).execute();
        GoogleCalendarSyncApplication.EVENT_CONFIG.getBase().addProperty(event.getId(), newEvent.getId());
    }
}
