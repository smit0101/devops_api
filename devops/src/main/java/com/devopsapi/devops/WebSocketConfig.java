package com.devopsapi.devops;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final DeploymentWebSocketHandler deploymentWebSocketHandler;

    public WebSocketConfig(DeploymentWebSocketHandler deploymentWebSocketHandler) {
        this.deploymentWebSocketHandler = deploymentWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(deploymentWebSocketHandler, "/ws").setAllowedOrigins("http://localhost:8081");
    }
}
