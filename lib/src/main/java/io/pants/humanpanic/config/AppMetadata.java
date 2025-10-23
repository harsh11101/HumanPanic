package io.pants.humanpanic.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configuration model for application metadata
 */
@Data
@Getter
@Setter
@NoArgsConstructor
public class AppMetadata {
    @JsonProperty("name")
    private String name = "Unknown Application";
    
    @JsonProperty("version")
    private String version = "Unknown Version";
    
    @JsonProperty("authors")
    private String[] authors = new String[]{"Unknown Authors"};
    
    @JsonProperty("homepage")
    private String homepage = "Unknown Homepage";
    
    @JsonProperty("support_url")
    private String supportUrl = "Unknown Support-URL";
    
    @JsonProperty("issue_url")
    private String issueUrl = "Unknown Issue_URL";
}