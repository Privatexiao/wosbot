package dev.frostguard.engine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dev.frostguard.api.configs.FlowStepKind;
import dev.frostguard.api.domain.AutomationBlueprint;
import dev.frostguard.api.domain.AutomationStep;
import dev.frostguard.api.runtime.WorkspacePaths;

class TaskBuilderServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void savesBuilderJsonAndGeneratedJavaBesideIt() throws Exception {
        String originalWorkspace = System.getProperty(WorkspacePaths.WORKSPACE_PROPERTY);
        System.setProperty(WorkspacePaths.WORKSPACE_PROPERTY, tempDir.toString());
        try {
            TaskBuilderService service = new TaskBuilderService();
            service.startSession("Expert Idle Exploration", "0");

            AutomationStep step = new AutomationStep(1, FlowStepKind.WAIT);
            step.setNodeName("Pause before bag");
            step.setParam("durationMs", "200");
            service.addNode(step);

            Path builderFile = tempDir.resolve("custom-tasks").resolve("expert_idle_exploration.json");
            TaskBuilderService.CustomTaskSaveResult saved =
                    service.saveCurrentTaskToCustomTasks("Expert Idle Exploration", builderFile);

            assertEquals("expert_idle_exploration", saved.className());
            assertTrue(Files.exists(saved.builderFile()));
            assertTrue(Files.exists(saved.javaFile()));

            String javaSource = Files.readString(saved.javaFile());
            assertTrue(javaSource.contains("// Pause before bag"));

            AutomationBlueprint loaded = service.loadDefinition(saved.builderFile().toFile(), "1");
            assertEquals("Expert Idle Exploration", loaded.getName());
            assertEquals("Pause before bag", loaded.getNodes().get(0).getNodeName());
            assertEquals("1", service.getActiveEmulatorNumber());
        } finally {
            restoreWorkspace(originalWorkspace);
        }
    }

    /**
     * Saving a flow that contained a template-search node used to abort
     * part-way through writing and leave an unparseable file on disk, so the
     * flow could never be reopened. The whole save/reload cycle is exercised
     * here through the service, exactly as the editor drives it.
     */
    @Test
    void savesAndReloadsAFlowContainingATemplateSearchNode() throws Exception {
        String originalWorkspace = System.getProperty(WorkspacePaths.WORKSPACE_PROPERTY);
        System.setProperty(WorkspacePaths.WORKSPACE_PROPERTY, tempDir.toString());
        try {
            TaskBuilderService service = new TaskBuilderService();
            service.startSession("Dead Shot", "0");

            AutomationStep find = new AutomationStep(1, FlowStepKind.TEMPLATE_SEARCH);
            find.setNodeName("search deal");
            find.setParam("templatePath", "HOME_DEALS_BUTTON");
            find.setParam("threshold", "90");
            service.addNode(find);

            AutomationStep wait = new AutomationStep(2, FlowStepKind.WAIT);
            wait.setNodeName("wait for panel");
            wait.setParam("durationMs", "1500");
            service.addNode(wait);

            Path builderFile = tempDir.resolve("custom-tasks").resolve("dead_shot.json");
            TaskBuilderService.CustomTaskSaveResult saved =
                    service.saveCurrentTaskToCustomTasks("Dead Shot", builderFile);

            String builderJson = Files.readString(saved.builderFile());
            JsonNode parsed = new ObjectMapper().readTree(builderJson);
            assertEquals("Dead Shot", parsed.path("title").asText());
            assertEquals(2, parsed.path("steps").size());
            assertEquals("search deal",
                    parsed.path("steps").path(0).path("attributes").path("nodeName").asText());

            // Legacy duplicate spellings must not reappear in the saved file.
            for (String legacyKey : new String[] {"\"id\"", "\"type\"", "\"params\"",
                    "\"canvasX\"", "\"nextNodeId\"", "\"summary\"", "\"nodes\""}) {
                assertFalse(builderJson.contains(legacyKey),
                        "legacy key leaked into the saved file: " + legacyKey);
            }

            AutomationBlueprint reloaded = service.loadDefinition(saved.builderFile().toFile(), "0");
            assertEquals(2, reloaded.getNodes().size());
            assertEquals(FlowStepKind.TEMPLATE_SEARCH, reloaded.getNodes().get(0).getKind());
            assertEquals("search deal", reloaded.getNodes().get(0).getNodeName());
            assertEquals("HOME_DEALS_BUTTON", reloaded.getNodes().get(0).getParam("templatePath"));
            assertEquals("wait for panel", reloaded.getNodes().get(1).getNodeName());
        } finally {
            restoreWorkspace(originalWorkspace);
        }
    }

    private void restoreWorkspace(String originalWorkspace) {
        if (originalWorkspace == null) {
            System.clearProperty(WorkspacePaths.WORKSPACE_PROPERTY);
        } else {
            System.setProperty(WorkspacePaths.WORKSPACE_PROPERTY, originalWorkspace);
        }
    }
}
