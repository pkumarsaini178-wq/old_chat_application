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

		// Clean up old static folder files if present
		try {
			java.io.File[] staticDirs = new java.io.File[] {
				new java.io.File("src/main/resources/static"),
				new java.io.File("chatapplication/src/main/resources/static")
			};
			for (java.io.File dir : staticDirs) {
				if (dir.exists() && dir.isDirectory()) {
					java.io.File[] files = dir.listFiles();
					if (files != null) {
						for (java.io.File f : files) {
							if (f.isFile()) {
								f.delete();
							}
						}
					}
				}
			}
		} catch (Exception e) {}
	}

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
		SpringApplication app = new SpringApplication(ChatapplicationApplication.class);
		app.setBannerMode(org.springframework.boot.Banner.Mode.OFF);
		app.run(args);
	}

}
