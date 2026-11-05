package com.example.bank_transfer_app.config;

import jakarta.servlet.SessionTrackingMode;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class WebConfig {

    @Bean
    public ServletContextInitializer servletContextInitializer() {
        return servletContext -> {

            servletContext.setSessionTrackingModes(Set.of(SessionTrackingMode.COOKIE));
        };
    }
}
