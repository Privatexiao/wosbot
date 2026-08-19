package dev.frostguard.tasks.city.hospital;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HospitalPageEvidencePolicyTest {

    @Test
    void recognizesStructurallyValidWoundedCount() {
        HospitalPageEvidencePolicy.WoundedCountEvidence evidence =
                HospitalPageEvidencePolicy.parseWoundedCount("1,250 / 30.0K");

        assertTrue(evidence.recognized());
        assertEquals(1_250, evidence.count());
        assertEquals(30_000, evidence.capacity());
    }

    @Test
    void rejectsMalformedOrInconsistentWoundedCount() {
        assertFalse(HospitalPageEvidencePolicy.parseWoundedCount(null).recognized());
        assertFalse(HospitalPageEvidencePolicy.parseWoundedCount("1250").recognized());
        assertFalse(HospitalPageEvidencePolicy.parseWoundedCount("unknown/30000").recognized());
        assertFalse(HospitalPageEvidencePolicy.parseWoundedCount("30001/30000").recognized());
        assertFalse(HospitalPageEvidencePolicy.parseWoundedCount("0/0").recognized());
    }

    @Test
    void requiresHospitalSpecificEvidenceForPageIdentity() {
        HospitalPageEvidencePolicy.WoundedCountEvidence unknown =
                HospitalPageEvidencePolicy.WoundedCountEvidence.unknown();

        assertFalse(HospitalPageEvidencePolicy.recognizesHealScreen(unknown, false));
        assertTrue(HospitalPageEvidencePolicy.recognizesHealScreen(unknown, true));
        assertTrue(HospitalPageEvidencePolicy.recognizesHealScreen(
                HospitalPageEvidencePolicy.parseWoundedCount("0/30000"), false));
    }

    @Test
    void distinguishesZeroWoundedFromRecognitionFailureAfterInput() {
        assertTrue(HospitalPageEvidencePolicy.provesNoWounded(
                HospitalPageEvidencePolicy.parseWoundedCount("0/30000")));
        assertFalse(HospitalPageEvidencePolicy.provesNoWounded(
                HospitalPageEvidencePolicy.parseWoundedCount("1/30000")));
        assertEquals(HospitalPageEvidencePolicy.PostInputResult.READY,
                HospitalPageEvidencePolicy.classifyAfterInput(true,
                        HospitalPageEvidencePolicy.WoundedCountEvidence.unknown()));
        assertEquals(HospitalPageEvidencePolicy.PostInputResult.NO_WOUNDED,
                HospitalPageEvidencePolicy.classifyAfterInput(false,
                        HospitalPageEvidencePolicy.parseWoundedCount("0/30000")));
        assertEquals(HospitalPageEvidencePolicy.PostInputResult.RECOGNITION_FAILURE,
                HospitalPageEvidencePolicy.classifyAfterInput(false,
                        HospitalPageEvidencePolicy.parseWoundedCount("12/30000")));
        assertEquals(HospitalPageEvidencePolicy.PostInputResult.RECOGNITION_FAILURE,
                HospitalPageEvidencePolicy.classifyAfterInput(false,
                        HospitalPageEvidencePolicy.WoundedCountEvidence.unknown()));
    }
}
