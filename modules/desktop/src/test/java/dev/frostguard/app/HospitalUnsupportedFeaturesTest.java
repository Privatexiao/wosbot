package dev.frostguard.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class HospitalUnsupportedFeaturesTest {

    @Test
    void keepsUnverifiedHospitalControlsDisabledInFxml() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/layout/HospitalLayout.fxml")) {
            assertNotNull(stream);
            Document document = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(stream);

            assertEquals("true", control(document, "checkBoxCityHospital").getAttribute("disable"));
            assertEquals("true", control(document, "checkBoxUseSpeedup").getAttribute("disable"));
            assertEquals("", control(document, "checkBoxFieldHospital").getAttribute("disable"));
        }
    }

    private static Element control(Document document, String id) {
        NodeList controls = document.getElementsByTagName("CheckBox");
        for (int index = 0; index < controls.getLength(); index++) {
            Element control = (Element) controls.item(index);
            if (id.equals(control.getAttribute("fx:id"))) {
                return control;
            }
        }
        throw new AssertionError("Missing hospital control: " + id);
    }
}
