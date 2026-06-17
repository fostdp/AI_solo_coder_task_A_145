package com.fishwash.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FishWashProperties.class)
public class PropertiesConfig {
}
