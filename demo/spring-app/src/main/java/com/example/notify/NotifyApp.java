package com.example.notify;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public final class NotifyApp {

    private NotifyApp() {
    }

    public static void main(String[] args) {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(AppConfig.class)) {
            NotificationService notifications = ctx.getBean(NotificationService.class);
            NightlyCleanup cleanup = ctx.getBean(NightlyCleanup.class);

            System.out.println("== 1. normal call");
            System.out.println("  result: " + notifications.send("ada@example.com", "Your report is ready"));

            System.out.println("== 2. failing call: friendly message on stderr, default value returned");
            boolean sent = notifications.send("not-an-address", "Hello");
            System.out.println("  result: " + sent + " (the service keeps running)");

            System.out.println("== 3. silent failure: crash report only, no user message");
            int purged = cleanup.purgeExpiredMessages();
            System.out.println("  purged: " + purged);

            System.out.println("== done");
        }
    }
}
