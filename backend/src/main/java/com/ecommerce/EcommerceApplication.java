package com.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the E-Commerce Management System REST API.
 *
 * <p>{@code @EnableAsync} is required by the WhatsApp notification pipeline: order
 * notifications are dispatched on a background executor so a slow or failing Twilio
 * call can never delay or roll back a customer's checkout.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class EcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcommerceApplication.class, args);
    }
}
