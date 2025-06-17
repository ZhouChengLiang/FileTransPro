package org.zcl.filewatcher.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private String watchPath;
    private String qrCodeOutputPath;
    private int maxDepth;
    private int healthCheckInterval;
}   