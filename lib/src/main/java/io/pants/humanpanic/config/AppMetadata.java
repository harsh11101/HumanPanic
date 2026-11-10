package io.pants.humanpanic.config;

import java.util.Arrays;
import java.util.Objects;

/**
 * Describes the application that uses HumanPanic. The values appear in the user message and in
 * crash reports.
 *
 * <p>Empty strings and an empty author list mean "not provided", and the matching section is left
 * out of the user message. {@code null} values are tolerated and treated the same way.
 *
 * <p>This is a mutable JavaBean, so frameworks can bind it. It is not thread-safe: configure it
 * completely before handing it to HumanPanic. {@link io.pants.humanpanic.PanicHandler} takes a
 * defensive copy.
 */
public class AppMetadata {

    /** Name used when no application name is configured. */
    public static final String DEFAULT_NAME = "Unknown Application";

    /** Version used when no application version is configured. */
    public static final String DEFAULT_VERSION = "Unknown Version";

    private static final String[] NO_AUTHORS = new String[0];

    private String name = DEFAULT_NAME;

    private String version = DEFAULT_VERSION;

    private String[] authors = NO_AUTHORS;

    private String homepage = "";

    private String supportUrl = "";

    private String issueUrl = "";

    /** Creates metadata with default values. */
    public AppMetadata() {
    }

    /**
     * Copy constructor.
     *
     * @param other the metadata to copy; must not be {@code null}
     */
    public AppMetadata(AppMetadata other) {
        Objects.requireNonNull(other, "other");
        this.name = other.name;
        this.version = other.version;
        this.authors = copy(other.authors);
        this.homepage = other.homepage;
        this.supportUrl = other.supportUrl;
        this.issueUrl = other.issueUrl;
    }

    /**
     * Returns the application name.
     *
     * @return the application name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the application name.
     *
     * @param name the application name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the application version.
     *
     * @return the application version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Sets the application version.
     *
     * @param version the application version
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * Returns a copy of the author list.
     *
     * @return a copy of the authors (possibly empty), or {@code null} if {@code null} was set
     */
    public String[] getAuthors() {
        return copy(authors);
    }

    /**
     * Sets the authors. The array is copied.
     *
     * @param authors the authors
     */
    public void setAuthors(String[] authors) {
        this.authors = copy(authors);
    }

    /**
     * Returns the homepage URL.
     *
     * @return the homepage URL, or {@code ""} if not provided
     */
    public String getHomepage() {
        return homepage;
    }

    /**
     * Sets the homepage URL.
     *
     * @param homepage the homepage URL
     */
    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    /**
     * Returns the support URL.
     *
     * @return the support URL, or {@code ""} if not provided
     */
    public String getSupportUrl() {
        return supportUrl;
    }

    /**
     * Sets the support URL shown as "Get help at".
     *
     * @param supportUrl the support URL
     */
    public void setSupportUrl(String supportUrl) {
        this.supportUrl = supportUrl;
    }

    /**
     * Returns the issue tracker URL.
     *
     * @return the issue tracker URL, or {@code ""} if not provided
     */
    public String getIssueUrl() {
        return issueUrl;
    }

    /**
     * Sets the issue tracker URL shown as "Open an issue at".
     *
     * @param issueUrl the issue tracker URL
     */
    public void setIssueUrl(String issueUrl) {
        this.issueUrl = issueUrl;
    }

    /**
     * Parses a comma-separated author list. Entries are trimmed, and blank entries are dropped.
     *
     * @param csv comma-separated authors; may be {@code null}
     * @return the authors, never {@code null}
     */
    static String[] parseAuthors(String csv) {
        if (csv == null || csv.isBlank()) {
            return NO_AUTHORS;
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    private static String[] copy(String[] array) {
        return array == null ? null : array.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AppMetadata)) {
            return false;
        }
        AppMetadata that = (AppMetadata) o;
        return Objects.equals(name, that.name)
                && Objects.equals(version, that.version)
                && Arrays.equals(authors, that.authors)
                && Objects.equals(homepage, that.homepage)
                && Objects.equals(supportUrl, that.supportUrl)
                && Objects.equals(issueUrl, that.issueUrl);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(name, version, homepage, supportUrl, issueUrl);
        return 31 * result + Arrays.hashCode(authors);
    }

    @Override
    public String toString() {
        return "AppMetadata(name=" + name + ", version=" + version
                + ", authors=" + Arrays.toString(authors) + ", homepage=" + homepage
                + ", supportUrl=" + supportUrl + ", issueUrl=" + issueUrl + ")";
    }
}
