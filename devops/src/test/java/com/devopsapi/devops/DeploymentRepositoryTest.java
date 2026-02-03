package com.devopsapi.devops;

import com.devopsapi.devops.model.Deployment;
import com.devopsapi.devops.repository.DeploymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class DeploymentRepositoryTest {

    @Autowired
    private DeploymentRepository deploymentRepository;

    @BeforeEach
    void setUp() {
        deploymentRepository.deleteAll();
    }

    @Test
    void testCreateDeployment() {
        Deployment deployment = new Deployment("Test Deployment", "Description for test", "http://github.com/test/repo", "main", Deployment.DeploymentStatus.PENDING);
        Deployment savedDeployment = deploymentRepository.save(deployment);

        assertThat(savedDeployment).isNotNull();
        assertThat(savedDeployment.getId()).isNotNull();
        assertThat(savedDeployment.getName()).isEqualTo("Test Deployment");
        assertThat(savedDeployment.getStatus()).isEqualTo(Deployment.DeploymentStatus.PENDING);
        assertThat(savedDeployment.getCreatedAt()).isNotNull();
        assertThat(savedDeployment.getUpdatedAt()).isNotNull();
    }

    @Test
    void testFindDeploymentById() {
        Deployment deployment = new Deployment("Another Deployment", "Description", "http://github.com/test/repo", "main", Deployment.DeploymentStatus.IN_PROGRESS);
        deploymentRepository.save(deployment);

        Optional<Deployment> foundDeployment = deploymentById(deployment.getId());

        assertThat(foundDeployment).isPresent();
        assertThat(foundDeployment.get().getName()).isEqualTo("Another Deployment");
    }

    @Test
    void testFindAllDeployments() {
        deploymentRepository.save(new Deployment("Deployment 1", "Desc 1", "http://github.com/test/repo1", "main", Deployment.DeploymentStatus.COMPLETED));
        deploymentRepository.save(new Deployment("Deployment 2", "Desc 2", "http://github.com/test/repo2", "develop", Deployment.DeploymentStatus.FAILED));

        List<Deployment> deployments = deploymentRepository.findAll();

        assertThat(deployments).hasSize(2);
    }

    @Test
    void testUpdateDeployment() {
        Deployment deployment = new Deployment("Update Test", "Initial Desc", "http://github.com/test/repo", "main", Deployment.DeploymentStatus.PENDING);
        deploymentRepository.save(deployment);

        deployment.setName("Updated Deployment Name");
        deployment.setStatus(Deployment.DeploymentStatus.COMPLETED);
        deployment.setDescription("Updated Description");
        deployment.setUpdatedAt(LocalDateTime.now());
        Deployment updatedDeployment = deploymentRepository.save(deployment);

        assertThat(updatedDeployment.getName()).isEqualTo("Updated Deployment Name");
        assertThat(updatedDeployment.getStatus()).isEqualTo(Deployment.DeploymentStatus.COMPLETED);
        assertThat(updatedDeployment.getDescription()).isEqualTo("Updated Description");
        assertThat(updatedDeployment.getUpdatedAt()).isAfter(deployment.getCreatedAt());
    }

    @Test
    void testDeleteDeployment() {
        Deployment deployment = new Deployment("Delete Test", "Desc", "http://github.com/test/repo", "main", Deployment.DeploymentStatus.PENDING);
        deploymentRepository.save(deployment);

        deploymentRepository.deleteById(deployment.getId());

        Optional<Deployment> foundDeployment = deploymentRepository.findById(deployment.getId());
        assertThat(foundDeployment).isNotPresent();
    }

    private Optional<Deployment> deploymentById(Long id) {
        return deploymentRepository.findById(id);
    }
}
