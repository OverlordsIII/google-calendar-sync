package io.github.overlordsiii.google_calendar_sync.rest;

import com.google.api.services.calendar.model.Event;
import io.github.overlordsiii.google_calendar_sync.GoogleCalendarSyncApplication;
import io.github.overlordsiii.google_calendar_sync.utils.GoogleCalendarUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@Service
public class PushNotificationController {
// google should send push notification here
    @PostMapping("/")
    public void pushNotification(@RequestBody String message) throws IOException {
        List<Event> changedEvents = GoogleCalendarUtil.getAllEvents(GoogleCalendarSyncApplication.CONFIG.getConfigOption("primary-calendar-id"), GoogleCalendarSyncApplication.CONFIG.getConfigOption("sync-token"));
        for (Event event : changedEvents) {
            String status = event.getStatus();
            String primaryEventId = event.getId();
            if (status.contains("cancelled")) {
                String secondaryEventId = GoogleCalendarSyncApplication.EVENT_CONFIG.getBase().get(primaryEventId).getAsString();
                GoogleCalendarUtil.deleteEvent(GoogleCalendarSyncApplication.CONFIG.getConfigOption("secondary-calendar-id"), secondaryEventId);
                GoogleCalendarSyncApplication.EVENT_CONFIG.getBase().remove(primaryEventId);
            } else if (!status.contains("cancelled") && event.getCreated().equals(event.getUpdated())) { // event was added
                GoogleCalendarUtil.copyEventTo(GoogleCalendarSyncApplication.CONFIG.getConfigOption("secondary-calendar-id"), event);
                GoogleCalendarSyncApplication.EVENT_CONFIG.save();
            } else {
                // event was updated in some other way
                // we are going to delete the existing event on secondary id and then copy the event over
                String secondaryEventId = GoogleCalendarSyncApplication.EVENT_CONFIG.getBase().get(primaryEventId).getAsString();
                GoogleCalendarUtil.deleteEvent(GoogleCalendarSyncApplication.CONFIG.getConfigOption("secondary-calendar-id"), secondaryEventId);
                GoogleCalendarUtil.copyEventTo(GoogleCalendarSyncApplication.CONFIG.getConfigOption("secondary-calendar-id"), event);
                GoogleCalendarSyncApplication.EVENT_CONFIG.save();
            }
        }
    }
}
