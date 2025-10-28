package io.pants.humanpanic.reporter;

import io.pants.humanpanic.config.AppMetadata;
import io.pants.humanpanic.config.ConfigLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Displays human-friendly panic messages like Rust's human-panic
 */
@Component
@RequiredArgsConstructor
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

        System.err.println(sb.toString());
    }

    public void notify(String message, Throwable throwable) {
        AppMetadata metadata = configLoader.getMetadata();

        System.err.println("\n");
        System.err.println("Well, this is embarrassing.");
        System.err.println("\n");
        System.err.println(metadata.getName() + " encountered an error:");

        if (message != null && !message.isEmpty() && !message.equals("An error occurred")) {
            System.err.println("  " + message);
        }

        System.err.println("  " + throwable.getClass().getSimpleName());
        if (throwable.getMessage() != null) {
            System.err.println("  " + throwable.getMessage());
        }
        System.err.println("\n");
        System.err.println("The application will continue running.");
        System.err.println();
    }
}