package com.example.chatapplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class ChatapplicationApplication {

	static {
		System.setProperty("CONSOLE_LOG_CHARSET", "UTF-8");
		System.setProperty("FILE_LOG_CHARSET", "UTF-8");
		System.setProperty("spring.output.ansi.enabled", "never");
		System.setProperty("spring.main.banner-mode", "off");
		System.setProperty("org.springframework.boot.logging.LoggingSystem", "none");
	}

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
		SpringApplication app = new SpringApplication(ChatapplicationApplication.class);
		app.setBannerMode(org.springframework.boot.Banner.Mode.OFF);
		app.run(args);
	}

}
