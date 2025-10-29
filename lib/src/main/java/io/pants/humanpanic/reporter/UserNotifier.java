package io.pants.humanpanic.reporter;

import io.pants.humanpanic.config.AppMetadata;
import io.pants.humanpanic.config.ConfigLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Displays human-friendly panic messages like Rust's human-panic
 */
@RequiredArgsConstructor
@Slf4j
public class UserNotifier {

    private final ConfigLoader configLoader;

    public void notifyWithReport(String customMessage, String reportPath) {
        AppMetadata metadata = configLoader.getMetadata();

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("Well, this is embarrassing.\n");
        sb.append("\n");
        sb.append(metadata.getName()).append(" had a problem and crashed. To help us diagnose\n");
        sb.append("the problem you can send us a crash report.\n");
        sb.append("\n");

        if (customMessage != null && !customMessage.isEmpty() && !customMessage.equals("An error occurred")) {
            sb.append(customMessage).append("\n");
            sb.append("\n");
        }

        if (reportPath != null) {
            sb.append("We have generated a report file at:\n");
            sb.append("\n");
            sb.append("  ").append(reportPath).append("\n");
            sb.append("\n");
        }

        sb.append("Submit an issue or email with the subject of:\n");
        sb.append("\n");
        sb.append("  ").append(metadata.getName()).append(" Crash Report\n");
        sb.append("\n");

        if (!metadata.getIssueUrl().isEmpty()) {
            sb.append("- Open an issue at:\n");
            sb.append("    ").append(metadata.getIssueUrl()).append("\n");
            sb.append("\n");
        }

        if (!metadata.getSupportUrl().isEmpty()) {
            sb.append("- Get help at:\n");
            sb.append("    ").append(metadata.getSupportUrl()).append("\n");
            sb.append("\n");
        }

        if (metadata.getAuthors().length > 0 && !metadata.getAuthors()[0].equals("Unknown")) {
            sb.append("- Contact the authors:\n");
            for (String author : metadata.getAuthors()) {
                sb.append("    ").append(author).append("\n");
            }
            sb.append("\n");
        }

        sb.append("We take privacy seriously, and do not perform any automated error collection.\n");
        sb.append("In order to improve the software, we rely on people to submit reports.\n");
        sb.append("\n");
        sb.append("Thank you kindly!\n");

        log.info("{}", sb);
    }

    public void notify(String message, Throwable throwable) {
        AppMetadata metadata = configLoader.getMetadata();

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("Well, this is embarrassing.\n");
        sb.append("\n");
        sb.append(metadata.getName()).append(" encountered an error:\n");

        if (message != null && !message.isEmpty() && !message.equals("An error occurred")) {
            sb.append("  ").append(message).append("\n");
        }

        sb.append("  ").append(throwable.getClass().getSimpleName()).append("\n");
        if (throwable.getMessage() != null) {
            sb.append("  ").append(throwable.getMessage()).append("\n");
        }
        sb.append("\n");
        sb.append("The application will continue running.\n");

        log.info("{}", sb);
    }
}