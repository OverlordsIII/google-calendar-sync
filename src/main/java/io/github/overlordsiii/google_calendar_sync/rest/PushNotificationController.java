package io.github.overlordsiii.google_calendar_sync.rest;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Service
public class PushNotificationController {
// google should send push notification here
    @PostMapping("/")
    public void pushNotification(@RequestBody String message) {

    }
}
