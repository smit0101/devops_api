package com.devopsapi.devops.controller;

import com.devopsapi.devops.model.Deployment;
import com.devopsapi.devops.repository.DeploymentRepository;
import com.devopsapi.devops.DeploymentWebSocketController;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    @Autowired
    private DeploymentRepository deploymentRepository;

    @Autowired
    private com.devopsapi.devops.repository.AppSettingRepository appSettingRepository;

    @Autowired
    private DeploymentWebSocketController webSocketController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/github")
    public ResponseEntity<Void> handleGithubWebhook(@RequestBody String payload, 
                                                   @RequestHeader("X-GitHub-Event") String eventType,
                                                   @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {
        
        System.out.println("Received Webhook: " + eventType);
        
        // Validate signature if secret is configured
        String secret = getWebhookSecret();
        if (secret != null && !secret.isEmpty()) {
            System.out.println("Validating signature...");
            if (signature == null || !isValidSignature(payload, signature, secret)) {
                System.err.println("INVALID WEBHOOK SIGNATURE. Received: " + signature);
                return ResponseEntity.status(401).build();
            }
            System.out.println("Signature Validated.");
        } else {
            System.out.println("No secret configured, skipping validation.");
        }

        if ("ping".equals(eventType)) {
            return ResponseEntity.ok().build();
        }

        if ("workflow_run".equals(eventType)) {
            try {
                JsonNode root = objectMapper.readTree(payload);
                String repoUrl = root.path("repository").path("html_url").asText();
                JsonNode workflowRun = root.path("workflow_run");
                String status = workflowRun.path("status").asText();
                String conclusion = workflowRun.path("conclusion").asText();
                Long runId = workflowRun.path("id").asLong();

                // Find and update ALL active deployments for this repository
                deploymentRepository.findAll().stream()
                        .filter(d -> d.getRepositoryUrl() != null && 
                                    d.getRepositoryUrl().replaceAll("/$", "").equalsIgnoreCase(repoUrl.replaceAll("/$", "")))
                        .forEach(deployment -> {
                            if (runId != 0) {
                                deployment.setWorkflowRunId(runId);
                            }
                            updateStatus(deployment, status, conclusion);
                        });

            } catch (Exception e) {
                System.err.println("ERROR: Webhook processing failed: " + e.getMessage());
            }
        }

        return ResponseEntity.ok().build();
    }

    private void updateStatus(Deployment deployment, String githubStatus, String conclusion) {
        Deployment.DeploymentStatus newStatus;

        if ("completed".equals(githubStatus)) {
            newStatus = "success".equals(conclusion) ? 
                        Deployment.DeploymentStatus.COMPLETED : 
                        Deployment.DeploymentStatus.FAILED;
        } else if ("in_progress".equals(githubStatus)) {
            newStatus = Deployment.DeploymentStatus.IN_PROGRESS;
        } else {
            newStatus = Deployment.DeploymentStatus.PENDING;
        }

        deployment.setStatus(newStatus);
        Deployment saved = deploymentRepository.save(deployment);
        webSocketController.notifyDeploymentUpdate(saved);
    }

    private String getWebhookSecret() {
        return appSettingRepository.findById("WEBHOOK_SECRET")
                .map(com.devopsapi.devops.model.AppSetting::getValue)
                .filter(s -> !s.isEmpty())
                .orElse(null);
    }

    private boolean isValidSignature(String payload, String signature, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = "sha256=" + HexFormat.of().formatHex(hash);
            return expected.equalsIgnoreCase(signature);
        } catch (Exception e) {
            return false;
        }
    }
}
