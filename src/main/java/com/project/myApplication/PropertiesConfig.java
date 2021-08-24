package com.project.myApplication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import lombok.Getter;


@Configuration
@PropertySource(value="classpath:config.properties", ignoreResourceNotFound = true)
@Getter
public class PropertiesConfig {
	
	@Value("${storage.root}")
	private String storageRoot;
	
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcePlaceHolderCongifurer() {
		return new  PropertySourcesPlaceholderConfigurer();
	}
	
	
}
