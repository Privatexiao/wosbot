package dev.frostguard.app.panel.city;

import java.util.Map;

import dev.frostguard.api.configs.ConfigurationKeyEnum;
import dev.frostguard.app.shared.AbstractProfileController;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;

public class HospitalLayoutController extends AbstractProfileController {

    @FXML
    private CheckBox checkBoxHospitalHeal;

    @FXML
    private CheckBox checkBoxFieldHospital;

    @FXML
    private CheckBox checkBoxCityHospital;

    @FXML
    private TextField textFieldMaxWaitMinutes;

    @FXML
    private CheckBox checkBoxUseSpeedup;

    @FXML
    private void initialize() {
        registerHospitalControls();
        applyUnsupportedFeatureGuards();
        initializeChangeEvents();
    }

    private void registerHospitalControls() {
        Map.of(
                checkBoxHospitalHeal, ConfigurationKeyEnum.HOSPITAL_HEAL_ENABLED_BOOL,
                checkBoxFieldHospital, ConfigurationKeyEnum.HOSPITAL_HEAL_FIELD_ENABLED_BOOL,
                checkBoxCityHospital, ConfigurationKeyEnum.HOSPITAL_HEAL_CITY_ENABLED_BOOL,
                checkBoxUseSpeedup, ConfigurationKeyEnum.HOSPITAL_HEAL_USE_SPEEDUP_BOOL)
                .forEach(this::registerCheckBox);

        Map.of(textFieldMaxWaitMinutes, ConfigurationKeyEnum.HOSPITAL_HEAL_MAX_WAIT_MINUTES_INT)
                .forEach(this::registerTextField);
    }

    private void applyUnsupportedFeatureGuards() {
        checkBoxCityHospital.setDisable(true);
        checkBoxCityHospital.setTooltip(new Tooltip(
                "City hospital entry requires saved-frame calibration before it can be enabled."));
        checkBoxUseSpeedup.setDisable(true);
        checkBoxUseSpeedup.setTooltip(new Tooltip(
                "Healing speedups remain disabled until item-only payment can be verified safely."));
    }
}
