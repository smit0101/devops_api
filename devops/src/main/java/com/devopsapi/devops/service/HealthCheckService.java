package com.devopsapi.devops.service;

import com.devopsapi.devops.model.Deployment;
import com.devopsapi.devops.repository.DeploymentRepository;
import com.devopsapi.devops.DeploymentWebSocketController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Service
public class HealthCheckService {

    @Autowired
    private DeploymentRepository deploymentRepository;

    @Autowired
    private DeploymentWebSocketController webSocketController;

    private final RestTemplate restTemplate;

    public HealthCheckService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    public void checkHealth() {
        deploymentRepository.findAll().stream()
                .filter(d -> d.getServiceUrl() != null && !d.getServiceUrl().isEmpty())
                .forEach(this::performCheck);
    }

    private void performCheck(Deployment deployment) {
        Deployment.HealthStatus oldStatus = deployment.getHealthStatus();
        Deployment.HealthStatus newStatus;

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(deployment.getServiceUrl(), String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                newStatus = Deployment.HealthStatus.HEALTHY;
            } else {
                newStatus = Deployment.HealthStatus.UNHEALTHY;
            }
        } catch (Exception e) {
            newStatus = Deployment.HealthStatus.UNHEALTHY;
        }

        if (oldStatus != newStatus) {
            deployment.setHealthStatus(newStatus);
            deploymentRepository.save(deployment);
            webSocketController.notifyDeploymentUpdate(deployment);
        }
    }
}
