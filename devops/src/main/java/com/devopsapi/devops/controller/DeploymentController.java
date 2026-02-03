package com.devopsapi.devops.controller;

import com.devopsapi.devops.model.Deployment;
import com.devopsapi.devops.repository.DeploymentRepository;
import com.devopsapi.devops.DeploymentWebSocketController; // Import the WebSocket controller
import com.devopsapi.devops.service.GitHubService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/deployments")
public class DeploymentController {

    @Autowired
    private DeploymentRepository deploymentRepository;

    @Autowired
    private DeploymentWebSocketController deploymentWebSocketController; // Inject DeploymentWebSocketController

    @Autowired
    private GitHubService gitHubService;

    @GetMapping("/branches")
    public List<String> getBranches(@RequestParam String repoUrl) {
        return gitHubService.getBranches(repoUrl);
    }

    @GetMapping("/{id}/jobs")
    public ResponseEntity<Map<String, Object>> getDeploymentJobs(@PathVariable Long id) {
        return deploymentRepository.findById(id)
                .map(d -> {
                    if (d.getWorkflowRunId() == null) {
                        return ResponseEntity.badRequest().<Map<String, Object>>body(Collections.singletonMap("error", "Workflow run ID not yet available"));
                    }
                    return ResponseEntity.ok(gitHubService.getWorkflowJobs(d.getRepositoryUrl(), d.getWorkflowRunId()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Deployment> getAllDeployments() {
        return deploymentRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Deployment> createDeployment(@RequestBody Deployment deployment) {
        Deployment savedDeployment = deploymentRepository.save(deployment);
        deploymentWebSocketController.notifyDeploymentUpdate(savedDeployment); // Notify WebSocket clients
        
        // Trigger GitHub Action
        gitHubService.triggerWorkflow(savedDeployment);
        
        return new ResponseEntity<>(savedDeployment, HttpStatus.CREATED);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllDeployments() {
        deploymentRepository.deleteAll();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeployment(@PathVariable Long id) {
        if (deploymentRepository.existsById(id)) {
            deploymentRepository.deleteById(id);
            deploymentWebSocketController.notifyDeploymentDelete(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Deployment> updateDeploymentStatus(@PathVariable Long id, @RequestParam Deployment.DeploymentStatus status) {
        return deploymentRepository.findById(id)
                .map(deployment -> {
                    deployment.setStatus(status);
                    Deployment updatedDeployment = deploymentRepository.save(deployment);
                    deploymentWebSocketController.notifyDeploymentUpdate(updatedDeployment);
                    return new ResponseEntity<>(updatedDeployment, HttpStatus.OK);
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
