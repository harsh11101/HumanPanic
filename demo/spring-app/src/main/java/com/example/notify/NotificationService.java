package com.example.notify;

/** Sends notifications. Beans with an interface get JDK dynamic proxies in plain Spring. */
public interface NotificationService {
    boolean send(String recipient, String body);
}
