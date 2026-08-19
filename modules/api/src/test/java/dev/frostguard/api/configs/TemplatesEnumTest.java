package dev.frostguard.api.configs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TemplatesEnumTest {

    @Test
    void mapsBackpackButtonToShippedAssetName() {
        assertEquals(
                "/templates/home/bottombar/backpackButton.png",
                TemplatesEnum.GAME_HOME_BOTTOM_BAR_BACKPACK_BUTTON.resourcePath());
    }

    @Test
    void leavesUnverifiedHospitalTemplatesUnregistered() {
        assertFalse(TemplatesEnum.HOSPITAL_CITY_BUILDING.existsAtPath());
        assertFalse(TemplatesEnum.HOSPITAL_HELP_BUTTON.existsAtPath());
        assertFalse(TemplatesEnum.HOSPITAL_CONFIRM_BUTTON.existsAtPath());
        assertFalse(TemplatesEnum.HOSPITAL_CANCEL_BUTTON.existsAtPath());
    }

    @Test
    void keepsUnsupportedHospitalFeaturesDisabledByDefault() {
        assertEquals("false", ConfigurationKeyEnum.HOSPITAL_HEAL_CITY_ENABLED_BOOL.getDefaultValue());
        assertEquals("false", ConfigurationKeyEnum.HOSPITAL_HEAL_USE_SPEEDUP_BOOL.getDefaultValue());
    }
}
