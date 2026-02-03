package com.devopsapi.devops;

import com.devopsapi.devops.model.Deployment;
import com.devopsapi.devops.model.WebSocketMessage;
import org.springframework.stereotype.Controller;
import java.io.IOException;

@Controller
public class DeploymentWebSocketController {

    private final DeploymentWebSocketHandler deploymentWebSocketHandler;

    public DeploymentWebSocketController(DeploymentWebSocketHandler deploymentWebSocketHandler) {
        this.deploymentWebSocketHandler = deploymentWebSocketHandler;
    }

    public void notifyDeploymentUpdate(Deployment deployment) {
        System.out.println("Sending deployment update via raw WebSocket: " + deployment);
        try {
            WebSocketMessage message = new WebSocketMessage("UPDATE", deployment);
            deploymentWebSocketHandler.sendMessage(message);
        } catch (IOException e) {
            System.err.println("Error sending WebSocket message: " + e.getMessage());
        }
    }

    public void notifyDeploymentDelete(Long id) {
        System.out.println("Sending deployment deletion via raw WebSocket: " + id);
        try {
            WebSocketMessage message = new WebSocketMessage("DELETE", id);
            deploymentWebSocketHandler.sendMessage(message);
        } catch (IOException e) {
            System.err.println("Error sending WebSocket message: " + e.getMessage());
        }
    }
}
