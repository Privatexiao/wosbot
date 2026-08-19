package dev.frostguard.app.i18n;

import javafx.collections.ListChangeListener;
import javafx.beans.property.Property;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fully Automated & Self-Translating Localization Engine for Frostguard (wosbot).
 */
public class I18nService {

    private static final Logger LOG = LoggerFactory.getLogger(I18nService.class);
    private static final Map<String, String> DICTIONARY = new ConcurrentHashMap<>();
    private static final Map<String, String> WORD_DICTIONARY = new ConcurrentHashMap<>();
    private static final String I18N_MARKER = "i18n_done_flag_v4";
    private static final String I18N_LIVE_MARKER = "i18n_live_listener_v1";

    static {
        loadBuiltinDictionary();
    }

    private static void loadBuiltinDictionary() {
        DICTIONARY.clear();
        WORD_DICTIONARY.clear();
        String resourcePath = "/i18n/messages_zh_CN.properties";
        try (InputStream is = I18nService.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
                            continue;
                        }
                        int eqIdx = line.indexOf('=');
                        if (eqIdx > 0) {
                            String key = line.substring(0, eqIdx).trim();
                            String val = line.substring(eqIdx + 1).trim();
                            if (!key.isEmpty() && !val.isEmpty()) {
                                DICTIONARY.put(key, val);
                                if (!key.contains(" ") && key.length() >= 3) {
                                    WORD_DICTIONARY.put(key, val);
                                }
                            }
                        }
                    }
                }
                LOG.info("I18nService: Loaded {} clean translation pairs into automated engine", DICTIONARY.size());
            } else {
                LOG.warn("I18nService: Resource {} not found on classpath", resourcePath);
            }
        } catch (Exception e) {
            LOG.error("I18nService: Error loading dictionary", e);
        }
    }

    public static String tr(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        String trimmed = text.trim();

        // 1. Exact match (Highest Priority)
        if (DICTIONARY.containsKey(trimmed)) {
            return DICTIONARY.get(trimmed);
        }

        // 2. Case-insensitive exact match
        for (Map.Entry<String, String> entry : DICTIONARY.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(trimmed)) {
                return entry.getValue();
            }
        }

        // 3. Prefix & substring match for status indicators & log messages with dynamic parameters
        if (trimmed.startsWith("Whiteout Survival is not running")) {
            return "无尽冬日游戏未运行，正在启动游戏...";
        }
        if (trimmed.contains("is running.")) {
            return trimmed.replace("is running.", "正处于运行状态。");
        }
        if (trimmed.startsWith("Checking emulator status")) {
            return "正在检查模拟器运行状态...";
        }
        if (trimmed.startsWith("Launching all queues")) {
            return "正在启动所有账号队列";
        }
        if (trimmed.startsWith("Custom tasks discovered:")) {
            return "扫描发现自定义任务数量: " + trimmed.substring("Custom tasks discovered:".length()).trim();
        }
        if (trimmed.startsWith("Initial schedule:")) {
            return "初始计划执行时间: " + trimmed.substring("Initial schedule:".length()).trim();
        }
        if (trimmed.startsWith("Successfully updated profile:")) {
            return "成功更新账号配置: " + trimmed.substring("Successfully updated profile:".length());
        }
        if (trimmed.startsWith("Successfully created profile:")) {
            return "成功创建账号配置: " + trimmed.substring("Successfully created profile:".length());
        }
        if (trimmed.startsWith("Successfully deleted profile:")) {
            return "成功删除账号配置: " + trimmed.substring("Successfully deleted profile:".length());
        }
        if (trimmed.startsWith("Loading module:")) {
            return "正在加载模块: " + tr(trimmed.substring("Loading module:".length()).trim());
        }
        if (trimmed.startsWith("Verifying character:")) {
            return "正在验证游戏角色: " + trimmed.substring("Verifying character:".length());
        }
        if (trimmed.startsWith("Executing task:")) {
            return "正在执行任务: " + tr(trimmed.substring("Executing task:".length()).trim());
        }

        for (Map.Entry<String, String> entry : DICTIONARY.entrySet()) {
            String key = entry.getKey();
            if (key.endsWith(":") && trimmed.startsWith(key)) {
                return entry.getValue() + " " + trimmed.substring(key.length()).trim();
            }
        }

        return text;
    }

    public static void registerAutoTranslation(Scene scene) {
        if (scene == null) return;

        LOG.info("I18nService: Registering live auto-translation listener on Scene");

        translateNode(scene.getRoot());
        attachTreeListener(scene.getRoot());

        scene.rootProperty().addListener((obs, oldRoot, newRoot) -> {
            if (newRoot != null) {
                translateNode(newRoot);
                attachTreeListener(newRoot);
            }
        });
    }

    private static void attachTreeListener(Node node) {
        if (node instanceof Parent parent) {
            try {
                parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>) c -> {
                    while (c.next()) {
                        if (c.wasAdded()) {
                            for (Node added : c.getAddedSubList()) {
                                translateNode(added);
                                attachTreeListener(added);
                            }
                        }
                    }
                });

                for (Node child : parent.getChildrenUnmodifiable()) {
                    attachTreeListener(child);
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static void translateNode(Node root) {
        if (root == null) return;

        try {
            translateSingleNode(root);

            if (root instanceof Parent parent) {
                for (Node child : parent.getChildrenUnmodifiable()) {
                    translateNode(child);
                }
            }

            if (root instanceof ScrollPane scrollPane) {
                translateNode(scrollPane.getContent());
            }

            if (root instanceof BorderPane borderPane) {
                translateNode(borderPane.getTop());
                translateNode(borderPane.getLeft());
                translateNode(borderPane.getCenter());
                translateNode(borderPane.getRight());
                translateNode(borderPane.getBottom());
            }

            if (root instanceof SplitPane splitPane) {
                for (Node item : splitPane.getItems()) {
                    translateNode(item);
                }
            }

            if (root instanceof Accordion accordion) {
                for (TitledPane tp : accordion.getPanes()) {
                    translateNode(tp);
                }
            }

            if (root instanceof DialogPane dialogPane) {
                translateNode(dialogPane.getContent());
                translateNode(dialogPane.getHeader());
            }

            if (root instanceof TabPane tabPane) {
                for (Tab tab : tabPane.getTabs()) {
                    if (tab.getText() != null && !Boolean.TRUE.equals(tab.getProperties().get(I18N_MARKER))) {
                        translateProperty(tab.getProperties(), tab.textProperty());
                        tab.getProperties().put(I18N_MARKER, Boolean.TRUE);
                    }
                    if (tab.getContent() != null) {
                        translateNode(tab.getContent());
                    }
                }
            }

            if (root instanceof TableView<?> tableView) {
                for (TableColumn<?, ?> column : tableView.getColumns()) {
                    if (column.getText() != null && !Boolean.TRUE.equals(column.getProperties().get(I18N_MARKER))) {
                        translateProperty(column.getProperties(), column.textProperty());
                        column.getProperties().put(I18N_MARKER, Boolean.TRUE);
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("I18nService translation exception: {}", e.getMessage());
        }
    }

    private static void translateSingleNode(Node node) {
        if (node == null) return;
        if (Boolean.TRUE.equals(node.getProperties().get(I18N_MARKER))) {
            return;
        }

        if (node instanceof MenuButton menuButton) {
            translateProperty(menuButton.getProperties(), menuButton.textProperty());
            for (MenuItem item : menuButton.getItems()) {
                translateProperty(item.getProperties(), item.textProperty());
            }
        } else if (node instanceof TitledPane titledPane) {
            translateProperty(titledPane.getProperties(), titledPane.textProperty());
            if (titledPane.getContent() != null) translateNode(titledPane.getContent());
        } else if (node instanceof Labeled labeled) {
            if (labeled.getText() != null && !labeled.getText().trim().isEmpty()) {
                labeled.setText(tr(labeled.getText()));
            }
            installLiveTranslation(labeled.getProperties(), labeled.textProperty());
        } else if (node instanceof Text textNode) {
            if (textNode.getText() != null && !textNode.getText().trim().isEmpty()) {
                textNode.setText(tr(textNode.getText()));
            }
            installLiveTranslation(textNode.getProperties(), textNode.textProperty());
        }

        if (node instanceof TextInputControl textInput) {
            if (textInput.getPromptText() != null && !textInput.getPromptText().trim().isEmpty()) {
                textInput.setPromptText(tr(textInput.getPromptText()));
            }
            installLiveTranslation(textInput.getProperties(), textInput.promptTextProperty());
        }

        if (node instanceof ComboBox comboBox) {
            if (comboBox.getPromptText() != null && !comboBox.getPromptText().trim().isEmpty()) {
                comboBox.setPromptText(tr(comboBox.getPromptText()));
            }
            installLiveTranslation(comboBox.getProperties(), comboBox.promptTextProperty());
            if (!Boolean.TRUE.equals(comboBox.getProperties().get("i18n_combo_cell_set"))
                    && comboBox.getCellFactory() == null && comboBox.getButtonCell() == null) {
                comboBox.getProperties().put("i18n_combo_cell_set", Boolean.TRUE);
                try {
                    comboBox.setCellFactory(lv -> new ListCell<Object>() {
                        @Override
                        protected void updateItem(Object item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                            } else {
                                setText(tr(item.toString()));
                            }
                        }
                    });
                    comboBox.setButtonCell(new ListCell<Object>() {
                        @Override
                        protected void updateItem(Object item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                            } else {
                                setText(tr(item.toString()));
                            }
                        }
                    });
                } catch (Exception ignored) {}
            }
        }

        if (node instanceof ChoiceBox choiceBox) {
            if (!Boolean.TRUE.equals(choiceBox.getProperties().get("i18n_choice_cell_set"))
                    && choiceBox.getConverter() == null) {
                choiceBox.getProperties().put("i18n_choice_cell_set", Boolean.TRUE);
                try {
                    choiceBox.setConverter(new javafx.util.StringConverter<Object>() {
                        @Override
                        public String toString(Object object) {
                            return object == null ? "" : tr(object.toString());
                        }

                        @Override
                        public Object fromString(String string) {
                            return string;
                        }
                    });
                } catch (Exception ignored) {}
            }
        }

        if (node instanceof Control control && control.getTooltip() != null) {
            Tooltip tt = control.getTooltip();
            if (tt.getText() != null && !Boolean.TRUE.equals(tt.getProperties().get(I18N_MARKER))) {
                translateProperty(tt.getProperties(), tt.textProperty());
                tt.getProperties().put(I18N_MARKER, Boolean.TRUE);
            }
        }

        node.getProperties().put(I18N_MARKER, Boolean.TRUE);
    }

    private static void translateProperty(Map<Object, Object> properties, Property<String> property) {
        if (property.getValue() != null && !property.getValue().trim().isEmpty()) {
            property.setValue(tr(property.getValue()));
        }
        installLiveTranslation(properties, property);
    }

    private static void installLiveTranslation(Map<Object, Object> properties, Property<String> property) {
        if (Boolean.TRUE.equals(properties.get(I18N_LIVE_MARKER))) return;
        properties.put(I18N_LIVE_MARKER, Boolean.TRUE);
        property.addListener((observable, oldValue, newValue) -> {
            if (newValue == null || newValue.isBlank()) return;
            String translated = tr(newValue);
            if (!Objects.equals(newValue, translated)) property.setValue(translated);
        });
    }
}
