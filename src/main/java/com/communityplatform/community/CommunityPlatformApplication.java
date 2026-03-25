package com.communityplatform.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point.
 *
 * @SpringBootApplication is a convenience annotation that combines:
 *   @Configuration       — marks this class as a source of bean definitions
 *   @EnableAutoConfiguration — tells Spring Boot to auto-configure based on classpath
 *   @ComponentScan       — scans this package and all sub-packages for components
 */
@SpringBootApplication
public class CommunityPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommunityPlatformApplication.class, args);
    }
}
