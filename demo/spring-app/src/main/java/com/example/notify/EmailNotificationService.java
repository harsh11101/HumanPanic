package com.example.notify;

import io.pants.humanpanic.HumanPanic;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService implements NotificationService {

    // The bean is reached through a JDK proxy of NotificationService. In 1.x this failed with an
    // NPE inside HumanPanic. Now the annotation is bound from the target method.
    @HumanPanic(message = "We could not send your email. Nothing was lost; please try again.")
    @Override
    public boolean send(String recipient, String body) {
        if (!recipient.contains("@")) {
            throw new IllegalArgumentException("Invalid e-mail address: " + recipient);
        }
        System.out.println("  -> sent to " + recipient + ": " + body);
        return true;
    }
}
