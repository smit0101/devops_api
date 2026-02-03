package com.devopsapi.devops.service;

import com.devopsapi.devops.model.Deployment;
import com.devopsapi.devops.repository.DeploymentRepository;
import com.devopsapi.devops.DeploymentWebSocketController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GitHubService {

    @Value("${github.token:}")
    private String githubTokenEnv;

    @Autowired
    private com.devopsapi.devops.repository.AppSettingRepository appSettingRepository;

    @Autowired
    private DeploymentRepository deploymentRepository;

    @Autowired
    private DeploymentWebSocketController webSocketController;

    private final RestTemplate restTemplate = new RestTemplate();

    private String getGitHubToken() {
        return appSettingRepository.findById("GITHUB_TOKEN")
                .map(com.devopsapi.devops.model.AppSetting::getValue)
                .filter(s -> !s.isEmpty())
                .orElse(githubTokenEnv);
    }

    public List<String> getBranches(String repoUrl) {
        String token = getGitHubToken();
        String ownerRepo = extractOwnerRepo(repoUrl);
        if (ownerRepo == null) return Collections.emptyList();

        String url = String.format("https://api.github.com/repos/%s/branches", ownerRepo);
        HttpHeaders headers = createHeaders(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                List<Map<String, Object>> branches = response.getBody();
                return branches.stream().map(b -> (String) b.get("name")).toList();
            }
        } catch (Exception e) {
            System.err.println("Error fetching branches: " + e.getMessage());
        }
        return Collections.singletonList("main"); // Default fallback
    }

    public Map<String, Object> getWorkflowJobs(String repoUrl, Long runId) {
        String token = getGitHubToken();
        String ownerRepo = extractOwnerRepo(repoUrl);
        if (ownerRepo == null || runId == null || token.isEmpty()) return Collections.emptyMap();

        String url = String.format("https://api.github.com/repos/%s/actions/runs/%d/jobs", ownerRepo, runId);
        HttpHeaders headers = createHeaders(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            return response.getBody();
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    public void triggerWorkflow(Deployment deployment) {
        String token = getGitHubToken();
        if (token.isEmpty()) {
            System.err.println("WARNING: Cannot trigger workflow. No GitHub token configured.");
            return;
        }

        String ownerRepo = extractOwnerRepo(deployment.getRepositoryUrl());
        if (ownerRepo == null) return;

        try {
            // Find first workflow
            String workflowsUrl = String.format("https://api.github.com/repos/%s/actions/workflows", ownerRepo);
            ResponseEntity<Map> wfResp = restTemplate.exchange(workflowsUrl, HttpMethod.GET, new HttpEntity<>(createHeaders(token)), Map.class);
            
            List workflows = (List) wfResp.getBody().get("workflows");
            if (workflows == null || workflows.isEmpty()) return;
            
            Object workflowId = ((Map) workflows.get(0)).get("id");
            String triggerUrl = String.format("https://api.github.com/repos/%s/actions/workflows/%s/dispatches", ownerRepo, workflowId);
            
            Map<String, Object> body = new HashMap<>();
            body.put("ref", deployment.getBranch() != null ? deployment.getBranch() : "main");
            
            restTemplate.postForEntity(triggerUrl, new HttpEntity<>(body, createHeaders(token)), Void.class);
            
            // Note: We don't get the runId back immediately from dispatches.
            // We'll rely on the Webhook to update the workflowRunId when it starts.
        } catch (Exception e) {
            System.err.println("Error triggering workflow: " + e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 10000)
    public void syncInProgressDeployments() {
        if (getGitHubToken().isEmpty()) return;
        deploymentRepository.findAll().stream()
                .filter(d -> d.getStatus() == Deployment.DeploymentStatus.IN_PROGRESS)
                .forEach(this::checkAndUpdateStatus);
    }

    private void checkAndUpdateStatus(Deployment deployment) {
        String token = getGitHubToken();
        String ownerRepo = extractOwnerRepo(deployment.getRepositoryUrl());
        String url = String.format("https://api.github.com/repos/%s/actions/runs?per_page=1", ownerRepo);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(createHeaders(token)), Map.class);
            Map latestRun = (Map) ((List) response.getBody().get("workflow_runs")).get(0);
            
            String ghStatus = (String) latestRun.get("status");
            String ghConclusion = (String) latestRun.get("conclusion");
            Long runId = Long.valueOf(latestRun.get("id").toString());

            deployment.setWorkflowRunId(runId);
            
            Deployment.DeploymentStatus newStatus = deployment.getStatus();
            if ("completed".equals(ghStatus)) {
                newStatus = "success".equals(ghConclusion) ? Deployment.DeploymentStatus.COMPLETED : Deployment.DeploymentStatus.FAILED;
            }

            if (newStatus != deployment.getStatus()) {
                deployment.setStatus(newStatus);
                deploymentRepository.save(deployment);
                webSocketController.notifyDeploymentUpdate(deployment);
            }
        } catch (Exception e) {}
    }

    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null && !token.isEmpty()) {
            headers.set("Authorization", "Bearer " + token);
        }
        headers.set("Accept", "application/vnd.github.v3+json");
        return headers;
    }

    private String extractOwnerRepo(String url) {
        if (url == null) return null;
        Pattern pattern = Pattern.compile("github\\.com/([^/]+)/([^/\\.]*)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) return matcher.group(1) + "/" + matcher.group(2);
        return null;
    }
}
