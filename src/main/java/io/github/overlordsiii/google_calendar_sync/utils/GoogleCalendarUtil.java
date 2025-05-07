package io.github.overlordsiii.google_calendar_sync.utils;

import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import io.github.overlordsiii.google_calendar_sync.GoogleCalendarSyncApplication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static io.github.overlordsiii.google_calendar_sync.GoogleCalendarSyncApplication.SERVICE;

public class GoogleCalendarUtil {

    private static String getOrCreateCopy(String cbeId) throws IOException {
        String secondaryId = GoogleCalendarSyncApplication.CONFIG.getConfigOption("secondary-calendar-id");
        if (secondaryId != null) {
            return secondaryId;
        }
        com.google.api.services.calendar.model.Calendar calendar = new com.google.api.services.calendar.model.Calendar();
        calendar.setSummary("CBE-IT Schedule");
        calendar.setTimeZone("America/Los_Angeles");

        calendar = SERVICE.calendars().insert(calendar).execute();

        for (Event event : getAllEvents(cbeId)) {
            GoogleCalendarSyncApplication.LOGGER.info("Copying event: " + event.getSummary());

            boolean archnet = event.getSummary().contains("ArchNet");

            Event newEvent = new Event()
                    .setSummary(archnet ? "ArchNet" : "Digital Commons")
                    .setDescription(event.getDescription())
                    .setStart(event.getStart())
                    .setEnd(event.getEnd())
                    .setColorId(archnet ? "11" : "1");

            SERVICE.events().insert(calendar.getId(), newEvent).execute();
        }



        return calendar.getId();
    }

    private static String getCBECalendarId() throws IOException {
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

    private static List<Event> getAllEvents(String calendarId) throws IOException {
        List<Event> items = new ArrayList<>();
        Events events = SERVICE.events().list(calendarId).execute();
        while (events != null) {
            items.addAll(events.getItems());
            if (events.getNextPageToken() != null) {
                events = SERVICE
                        .events()
                        .list(calendarId)
                        .setPageToken(events.getNextPageToken())
                        .execute();
            } else {
                break;
            }
        }
        return items;
    }
}
