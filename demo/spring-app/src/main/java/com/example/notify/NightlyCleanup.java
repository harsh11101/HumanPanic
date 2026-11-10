package com.example.notify;

import io.pants.humanpanic.HumanPanic;
import org.springframework.stereotype.Component;

/** A class-based bean (CGLIB proxy). Failures are recorded silently for operators. */
@Component
public class NightlyCleanup {

    @HumanPanic(silent = true)
    public int purgeExpiredMessages() {
        throw new IllegalStateException("Message store is locked by another process");
    }
}
