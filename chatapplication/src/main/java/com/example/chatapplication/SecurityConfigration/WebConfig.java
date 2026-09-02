package com.example.chatapplication.SecurityConfigration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolve chats-frontend directory relative to working directory or parent directory
        File frontendDir = new File("chats-frontend");
        if (!frontendDir.exists()) {
            frontendDir = new File("../chats-frontend");
        }

        String path = frontendDir.getAbsolutePath().replace("\\", "/");
        if (!path.endsWith("/")) {
            path += "/";
        }

        registry.addResourceHandler("/**")
                .addResourceLocations("file:" + path, "classpath:/static/");
    }
}
