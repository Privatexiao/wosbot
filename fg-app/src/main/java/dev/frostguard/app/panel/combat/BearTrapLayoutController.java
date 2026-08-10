package dev.frostguard.app.panel.combat;

import dev.frostguard.api.configs.BearTrapParticipationTriggerEnum;
import dev.frostguard.api.configs.ConfigurationKeyEnum;
import dev.frostguard.app.panel.profile.ProfileAux;
import dev.frostguard.app.shared.AbstractProfileController;
import dev.frostguard.app.shared.UtcDateTimeEditor;
import dev.frostguard.app.shared.UtcDateTimeValue;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanExpression;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.LocalDateTime;
import java.util.List;

public class BearTrapLayoutController extends AbstractProfileController {

    @FXML
    private CheckBox checkBoxEnableBearTrap;

    @FXML
    private ComboBox<ProtectionMode> comboBoxProtectionTimer1;

    @FXML
    private Label labelProtectionHelperTimer1;

    @FXML
    private UtcDateTimeEditor timer1DateTimeEditor;

    @FXML
    private ComboBox<ProtectionMode> comboBoxProtectionTimer2;

    @FXML
    private Label labelProtectionHelperTimer2;

    @FXML
    private UtcDateTimeEditor timer2DateTimeEditor;

    @FXML
    private TextField textFieldPreparationTime;

    @FXML
    private CheckBox checkBoxActivePets;

    @FXML
    private CheckBox checkBoxRecallTroops;

    @FXML
    private ComboBox<Integer> comboBoxTrapNumber;

    @FXML
    private ComboBox<BearTrapParticipationTriggerEnum> comboBoxParticipationTrigger;

    @FXML
    private Label labelParticipationHelper;

    @FXML
    private Label labelParticipationWarning;

    @FXML
    private Label labelSelectedTimerWarning;

    @FXML
    private Label labelTimerRecommendation;

    @FXML
    private Label labelTrapSelectionHelper;

    @FXML
    private Label labelParticipationTriggerInfo;

    @FXML
    private CheckBox checkBoxCallRally;

    @FXML
    private ComboBox<Integer> comboBoxRallyFlag;

    @FXML
    private CheckBox checkBoxEnableJoin;

    @FXML private ComboBox<String> comboBoxJoinFlag1;
    @FXML private ComboBox<String> comboBoxJoinFlag2;
    @FXML private ComboBox<String> comboBoxJoinFlag3;
    @FXML private ComboBox<String> comboBoxJoinFlag4;
    @FXML private ComboBox<String> comboBoxJoinFlag5;
    @FXML private ComboBox<String> comboBoxJoinFlag6;

    private List<TimerBinding> timerBindings;
    private boolean loadingParticipationTrigger;
    private boolean loadingProtectionModes;

    @FXML
    private void initialize() {
        timerBindings = List.of(
                new TimerBinding(timer1DateTimeEditor, comboBoxProtectionTimer1, labelProtectionHelperTimer1,
                        ConfigurationKeyEnum.BEAR_TRAP_SCHEDULE_DATETIME_STRING,
                        ConfigurationKeyEnum.BEAR_TRAP_TIMER_1_ENABLED_BOOL,
                        ConfigurationKeyEnum.BEAR_TRAP_TIMER_1_BLOCK_RALLIES_BOOL,
                        ConfigurationKeyEnum.BEAR_TRAP_TIMER_1_PAUSE_ALL_TASKS_BOOL,
                        1),
                new TimerBinding(timer2DateTimeEditor, comboBoxProtectionTimer2, labelProtectionHelperTimer2,
                        ConfigurationKeyEnum.BEAR_TRAP_TIMER_2_SCHEDULE_DATETIME_STRING,
                        ConfigurationKeyEnum.BEAR_TRAP_TIMER_2_ENABLED_BOOL,
                        ConfigurationKeyEnum.BEAR_TRAP_TIMER_2_BLOCK_RALLIES_BOOL,
                        ConfigurationKeyEnum.BEAR_TRAP_TIMER_2_PAUSE_ALL_TASKS_BOOL,
                        2));
        registerConfigurationFields();
        populateFlagControls();
        configureProtectionModes();
        configureParticipationTrigger();
        configureParticipationTriggerTooltip();
        configureParticipationScheduleHelp();
        configureDateTimeEditors();
        refreshAvailableParticipationTimers();
        bindEnabledState();
        initializeChangeEvents();
    }

    private void registerConfigurationFields() {
        checkBoxMappings.put(checkBoxEnableBearTrap, ConfigurationKeyEnum.BEAR_TRAP_EVENT_BOOL);
        checkBoxMappings.put(checkBoxActivePets, ConfigurationKeyEnum.BEAR_TRAP_ACTIVE_PETS_BOOL);
        checkBoxMappings.put(checkBoxRecallTroops, ConfigurationKeyEnum.BEAR_TRAP_RECALL_TROOPS_BOOL);
        checkBoxMappings.put(checkBoxCallRally, ConfigurationKeyEnum.BEAR_TRAP_CALL_RALLY_BOOL);
        checkBoxMappings.put(checkBoxEnableJoin, ConfigurationKeyEnum.BEAR_TRAP_JOIN_RALLY_BOOL);

        textFieldMappings.put(textFieldPreparationTime, ConfigurationKeyEnum.BEAR_TRAP_PREPARATION_TIME_INT);

        comboBoxMappings.put(comboBoxTrapNumber, ConfigurationKeyEnum.BEAR_TRAP_NUMBER_INT);
        comboBoxMappings.put(comboBoxRallyFlag, ConfigurationKeyEnum.BEAR_TRAP_RALLY_FLAG_INT);
        comboBoxMappings.put(comboBoxJoinFlag1, ConfigurationKeyEnum.BEAR_TRAP_JOIN_MARCH_1_FLAG_STRING);
        comboBoxMappings.put(comboBoxJoinFlag2, ConfigurationKeyEnum.BEAR_TRAP_JOIN_MARCH_2_FLAG_STRING);
        comboBoxMappings.put(comboBoxJoinFlag3, ConfigurationKeyEnum.BEAR_TRAP_JOIN_MARCH_3_FLAG_STRING);
        comboBoxMappings.put(comboBoxJoinFlag4, ConfigurationKeyEnum.BEAR_TRAP_JOIN_MARCH_4_FLAG_STRING);
        comboBoxMappings.put(comboBoxJoinFlag5, ConfigurationKeyEnum.BEAR_TRAP_JOIN_MARCH_5_FLAG_STRING);
        comboBoxMappings.put(comboBoxJoinFlag6, ConfigurationKeyEnum.BEAR_TRAP_JOIN_MARCH_6_FLAG_STRING);
    }

    private static final java.util.List<String> FLAG_OPTIONS = java.util.List.of(
            "No Flag", "1", "2", "3", "4", "5", "6", "7", "8");

    private void populateFlagControls() {
        comboBoxTrapNumber.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer trapNumber) {
                return trapNumber == null ? "" : "熊陷阱 " + trapNumber + " (定时器 " + trapNumber + ")";
            }

            @Override
            public Integer fromString(String value) {
                return comboBoxTrapNumber.getValue();
            }
        });
        comboBoxRallyFlag.getItems().setAll(1, 2, 3, 4, 5, 6, 7, 8);
        for (ComboBox<String> cb : new ComboBox[]{comboBoxJoinFlag1, comboBoxJoinFlag2, comboBoxJoinFlag3,
                comboBoxJoinFlag4, comboBoxJoinFlag5, comboBoxJoinFlag6}) {
            cb.getItems().setAll(FLAG_OPTIONS);
            cb.setValue("No Flag");
        }
    }

    private void configureParticipationTrigger() {
        comboBoxParticipationTrigger.getItems().setAll(BearTrapParticipationTriggerEnum.values());
        comboBoxParticipationTrigger.setValue(BearTrapParticipationTriggerEnum.TIMER_ONLY);
        comboBoxParticipationTrigger.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }
            updateParticipationHelp();
            if (!loadingParticipationTrigger) {
                publishWhenReady(ConfigurationKeyEnum.BEAR_TRAP_ICON_PARTICIPATION_FALLBACK_BOOL,
                        newValue.isIconFallbackEnabled());
            }
        });
        updateParticipationHelp();
    }

    private void configureParticipationTriggerTooltip() {
        Tooltip tooltip = new Tooltip(
                "熊图标检测始终通过阻止无关的集结任务来保护活动。此设置仅控制图标是否也可触发打熊参与。");
        tooltip.setWrapText(true);
        tooltip.setMaxWidth(420);
        tooltip.setShowDelay(Duration.millis(150));
        tooltip.setShowDuration(Duration.seconds(20));
        labelParticipationTriggerInfo.setTooltip(tooltip);
    }

    private void configureProtectionModes() {
        timerBindings.forEach(binding -> {
            binding.protectionMode().getItems().setAll(ProtectionMode.values());
            binding.protectionMode().setValue(ProtectionMode.OFF);
            binding.protectionMode().valueProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue == null) {
                    return;
                }
                updateProtectionHelper(binding);
                updateParticipationHelp();
                if (!loadingProtectionModes) {
                    publishProtectionMode(binding, newValue);
                }
            });
            updateProtectionHelper(binding);
        });
    }

    private void publishProtectionMode(TimerBinding binding, ProtectionMode mode) {
        publishWhenReady(binding.enabledKey(), mode != ProtectionMode.OFF);
        publishWhenReady(binding.blockRalliesKey(), mode == ProtectionMode.BLOCK_RALLIES);
        publishWhenReady(binding.pauseAllKey(), mode == ProtectionMode.PAUSE_ALL);
    }

    private void configureParticipationScheduleHelp() {
        comboBoxTrapNumber.valueProperty().addListener((obs, oldValue, newValue) -> updateParticipationHelp());
        checkBoxEnableBearTrap.selectedProperty().addListener((obs, oldValue, newValue) -> updateParticipationHelp());
        updateParticipationHelp();
    }

    private void updateParticipationHelp() {
        BearTrapParticipationTriggerEnum trigger = comboBoxParticipationTrigger.getValue();
        Integer trapNumber = comboBoxTrapNumber.getValue();
        boolean iconFallback = trigger == BearTrapParticipationTriggerEnum.TIMER_ICON_FALLBACK;
        if (trapNumber == null) {
            labelParticipationHelper.setText("请选择已应用的活动定时器以计划参与。");
        } else {
            labelParticipationHelper.setText(iconFallback
                    ? "使用定时器 " + trapNumber + " 的 UTC 计划；熊陷阱图标也可触发参与。"
                    : "使用定时器 " + trapNumber + " 的 UTC 计划或立即运行。");
        }
        labelParticipationWarning.setVisible(iconFallback);
        labelParticipationWarning.setManaged(iconFallback);

        ProtectionMode selectedMode = trapNumber == null
                ? ProtectionMode.OFF
                : timerBindings.get(trapNumber - 1).protectionMode().getValue();
        boolean selectedProtectionEnabled = selectedMode != null && selectedMode != ProtectionMode.OFF;
        boolean showProtectionWarning = checkBoxEnableBearTrap.isSelected()
                && trapNumber != null
                && !selectedProtectionEnabled;
        labelSelectedTimerWarning.setText(trapNumber == null
                ? ""
                : "定时器 " + trapNumber
                        + " 保护已禁用，但参与仍使用其 UTC 计划。");
        labelSelectedTimerWarning.setVisible(showProtectionWarning);
        labelSelectedTimerWarning.setManaged(showProtectionWarning);
    }

    private void updateProtectionHelper(TimerBinding binding) {
        if (!binding.editor().hasCommittedDateTime()) {
            binding.protectionHelper().setText("请在选择保护模式前应用此活动定时器。");
            return;
        }
        ProtectionMode mode = binding.protectionMode().getValue();
        binding.protectionHelper().setText(mode == null ? "" : mode.helperText());
    }

    private void refreshAvailableParticipationTimers() {
        Integer selectedTrap = comboBoxTrapNumber.getValue();
        List<Integer> availableTraps = timerBindings.stream()
                .filter(binding -> binding.editor().hasCommittedDateTime())
                .map(TimerBinding::trapNumber)
                .toList();

        boolean wasLoadingProfile = isLoadingProfile;
        isLoadingProfile = true;
        try {
            comboBoxTrapNumber.getItems().setAll(availableTraps);
            comboBoxTrapNumber.setValue(availableTraps.contains(selectedTrap) ? selectedTrap : null);
        } finally {
            isLoadingProfile = wasLoadingProfile;
        }
        comboBoxTrapNumber.setPromptText(availableTraps.isEmpty()
                ? "请先设置活动定时器"
                : "选择已配置的定时器");

        boolean bothTimersConfigured = availableTraps.size() == timerBindings.size();
        labelTimerRecommendation.setText(bothTimersConfigured
                ? "两个 48 小时活动定时器均已配置。"
                : "强烈建议：设置两个 UTC 活动定时器，即使不开启自动参与，也能随时开启保护。");
        labelTimerRecommendation.setStyle(bothTimersConfigured
                ? "-fx-background-color: rgba(34, 197, 94, 0.12); -fx-background-radius: 4; -fx-padding: 6 8; -fx-text-fill: #86efac; -fx-font-size: 11px;"
                : "-fx-background-color: rgba(245, 158, 11, 0.14); -fx-background-radius: 4; -fx-padding: 6 8; -fx-text-fill: #fbbf24; -fx-font-size: 11px;");
        labelTrapSelectionHelper.setText(availableTraps.isEmpty()
                ? "请先在上方设置完整的 UTC 日期与时间并点击应用。"
                : "仅已应用的活动定时器可被选择。");
        timerBindings.forEach(binding -> {
            binding.protectionMode().setDisable(!binding.editor().hasCommittedDateTime());
            updateProtectionHelper(binding);
        });
        updateParticipationHelp();
    }

    private void bindEnabledState() {
        timerBindings.forEach(this::bindTimerState);

        BooleanExpression disabledUntilEnabled = checkBoxEnableBearTrap.selectedProperty().not();
        checkBoxActivePets.disableProperty().bind(disabledUntilEnabled);
        checkBoxRecallTroops.disableProperty().bind(disabledUntilEnabled);
        comboBoxTrapNumber.disableProperty().bind(
                disabledUntilEnabled.or(Bindings.isEmpty(comboBoxTrapNumber.getItems())));
        comboBoxParticipationTrigger.disableProperty().bind(disabledUntilEnabled);
        checkBoxCallRally.disableProperty().bind(disabledUntilEnabled);
        checkBoxEnableJoin.disableProperty().bind(disabledUntilEnabled);

        comboBoxRallyFlag.disableProperty().bind(disabledUntilEnabled.or(checkBoxCallRally.selectedProperty().not()));
        BooleanExpression joinDisabled = disabledUntilEnabled.or(checkBoxEnableJoin.selectedProperty().not());
        for (ComboBox<String> cb : new ComboBox[]{comboBoxJoinFlag1, comboBoxJoinFlag2, comboBoxJoinFlag3,
                comboBoxJoinFlag4, comboBoxJoinFlag5, comboBoxJoinFlag6}) {
            cb.disableProperty().bind(joinDisabled);
            bindManagedVisibility(cb, checkBoxEnableJoin.selectedProperty());
        }

        bindManagedVisibility(comboBoxRallyFlag, checkBoxCallRally.selectedProperty());
    }

    private void bindTimerState(TimerBinding timer) {
        BooleanExpression selectedForParticipation = Bindings.createBooleanBinding(
                () -> checkBoxEnableBearTrap.isSelected()
                        && Integer.valueOf(timer.trapNumber()).equals(comboBoxTrapNumber.getValue()),
                checkBoxEnableBearTrap.selectedProperty(),
                comboBoxTrapNumber.valueProperty());
        BooleanExpression protectionEnabled = Bindings.createBooleanBinding(
                () -> timer.protectionMode().getValue() != null
                        && timer.protectionMode().getValue() != ProtectionMode.OFF,
                timer.protectionMode().valueProperty());
        BooleanExpression timerInUse = protectionEnabled.or(selectedForParticipation);
        timer.editor().timerEnabledProperty().bind(timerInUse);
    }

    private void configureDateTimeEditors() {
        timerBindings.forEach(binding -> {
            binding.editor().setOnCommit(value -> {
                boolean firstConfiguration = !comboBoxTrapNumber.getItems().contains(binding.trapNumber());
                publishWhenReady(binding.scheduleKey(), UtcDateTimeValue.formatPersisted(value));
                if (firstConfiguration && binding.protectionMode().getValue() == ProtectionMode.OFF) {
                    binding.protectionMode().setValue(ProtectionMode.BLOCK_RALLIES);
                }
                refreshAvailableParticipationTimers();
            });
            binding.editor().setOnClear(() -> {
                publishWhenReady(binding.scheduleKey(), "");
                loadingProtectionModes = true;
                try {
                    binding.protectionMode().setValue(ProtectionMode.OFF);
                } finally {
                    loadingProtectionModes = false;
                }
                publishProtectionMode(binding, ProtectionMode.OFF);
                refreshAvailableParticipationTimers();
            });
        });
    }

    @Override
    public void onProfileLoad(ProfileAux profile) {
        super.onProfileLoad(profile);
        isLoadingProfile = true;
        loadingParticipationTrigger = true;
        loadingProtectionModes = true;
        try {
            boolean iconFallback = Boolean.TRUE.equals(profile.<Boolean>getConfiguration(
                    ConfigurationKeyEnum.BEAR_TRAP_ICON_PARTICIPATION_FALLBACK_BOOL));
            comboBoxParticipationTrigger.setValue(
                    BearTrapParticipationTriggerEnum.fromIconFallbackEnabled(iconFallback));
            timerBindings.forEach(binding -> binding.editor().setDateTime(
                    loadSavedDateTime(profile, binding.scheduleKey())));
            timerBindings.forEach(binding -> binding.protectionMode().setValue(
                    loadProtectionMode(profile, binding)));
            refreshAvailableParticipationTimers();
        } finally {
            loadingProtectionModes = false;
            loadingParticipationTrigger = false;
            isLoadingProfile = false;
        }
        updateParticipationHelp();
    }

    private ProtectionMode loadProtectionMode(ProfileAux profile, TimerBinding binding) {
        boolean explicitlyEnabled = Boolean.TRUE.equals(
                profile.<Boolean>getConfiguration(binding.enabledKey()));
        boolean legacyTimer1Enabled = binding.trapNumber() == 1
                && !profileHasConfiguration(profile, binding.enabledKey())
                && Boolean.TRUE.equals(profile.<Boolean>getConfiguration(ConfigurationKeyEnum.BEAR_TRAP_EVENT_BOOL));
        if (!explicitlyEnabled && !legacyTimer1Enabled) {
            return ProtectionMode.OFF;
        }
        if (Boolean.TRUE.equals(profile.<Boolean>getConfiguration(binding.pauseAllKey()))) {
            return ProtectionMode.PAUSE_ALL;
        }
        if (legacyTimer1Enabled
                || Boolean.TRUE.equals(profile.<Boolean>getConfiguration(binding.blockRalliesKey()))) {
            return ProtectionMode.BLOCK_RALLIES;
        }
        return ProtectionMode.OFF;
    }

    private boolean profileHasConfiguration(ProfileAux profile, ConfigurationKeyEnum key) {
        return profile.getConfigs().stream().anyMatch(config -> key.name().equalsIgnoreCase(config.getName()));
    }

    private LocalDateTime loadSavedDateTime(ProfileAux profile, ConfigurationKeyEnum key) {
        return profile.getConfigs().stream()
                .filter(config -> key.name().equalsIgnoreCase(config.getName()))
                .map(config -> config.getValue())
                .map(UtcDateTimeValue::parsePersisted)
                .flatMap(java.util.Optional::stream)
                .findFirst()
                .orElse(null);
    }

    private static void bindManagedVisibility(javafx.scene.Node node, javafx.beans.value.ObservableBooleanValue visible) {
        node.visibleProperty().bind(visible);
        node.managedProperty().bind(node.visibleProperty());
    }

    private record TimerBinding(
            UtcDateTimeEditor editor,
            ComboBox<ProtectionMode> protectionMode,
            Label protectionHelper,
            ConfigurationKeyEnum scheduleKey,
            ConfigurationKeyEnum enabledKey,
            ConfigurationKeyEnum blockRalliesKey,
            ConfigurationKeyEnum pauseAllKey,
            int trapNumber) {
    }

    private enum ProtectionMode {
        OFF("关闭 (Off)", "此定时器不阻止任何计划任务。"),
        BLOCK_RALLIES("阻止发起集结任务 (推荐)",
                "防止极地恶魔、英雄任务和雇佣兵发起集结。"),
        PAUSE_ALL("暂停所有计划任务",
                "在保护时间窗口内阻止所有非必要计划任务。");

        private final String label;
        private final String helperText;

        ProtectionMode(String label, String helperText) {
            this.label = label;
            this.helperText = helperText;
        }

        String helperText() {
            return helperText;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
