package com.ecommerce.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.mail")
@Getter
@Setter
public class MailProperties {

    private boolean enabled = true;
    private String host = "";
    private int port = 587;
    private String username = "";
    private String password = "";
    private String from = "no-reply@rozbazaar.local";
    private String fromName = "Roz Bazaar";
}
