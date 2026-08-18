package dev.frostguard.tasks.combat;

import dev.frostguard.api.configs.TemplatesEnum;
import dev.frostguard.api.domain.AreaData;
import dev.frostguard.api.domain.ImageSearchResultData;
import dev.frostguard.api.domain.PointData;
import dev.frostguard.engine.nav.CommonGameAreas;
import dev.frostguard.engine.helper.TemplateSearchHelper;
import dev.frostguard.engine.service.BotOcrEngine;
import dev.frostguard.engine.emulator.EmulatorController;
import dev.frostguard.vision.convert.CompactGameNumberParser;
import dev.frostguard.vision.convert.GameTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BearRallyScanner {

    private static final Logger log = LoggerFactory.getLogger(BearRallyScanner.class);
    
    private final EmulatorController emulator;
    private final BotOcrEngine ocrEngine;
    private final TemplateSearchHelper searchHelper;

    public BearRallyScanner(EmulatorController emulator, BotOcrEngine ocrEngine, TemplateSearchHelper searchHelper) {
        this.emulator = emulator;
        this.ocrEngine = ocrEngine;
        this.searchHelper = searchHelper;
    }

    /**
     * Scans the current screen for bear rally candidates.
     * Extracts all joinable cards and parses their details from the same frame.
     */
    public List<BearRallyCandidate> scanCandidates(Instant now) {
        List<BearRallyCandidate> candidates = new ArrayList<>();
        
        // Use the default matching params but maybe higher confidence.
        List<ImageSearchResultData> joinButtons = searchHelper.locateAllPatterns(
                TemplatesEnum.BEAR_JOIN_PLUS_ICON, 
                TemplateSearchHelper.SearchConfig.builder().withThreshold(80).build()
        );
        
        if (joinButtons.isEmpty()) {
            return candidates;
        }
        
        // Process from top to bottom
        joinButtons.sort(Comparator.comparingInt(img -> img.getPoint().getY()));
        
        for (ImageSearchResultData btn : joinButtons) {
            PointData p = btn.getPoint();
            // p is the top-left of the match
            
            // Reconstruct full card AreaData for debugging
            AreaData cardArea = new AreaData(
                new PointData(0, p.getY() + CommonGameAreas.BEAR_TRAP_COUNTDOWN_DY1 - 10),
                new PointData(720, p.getY() + 60)
            );
            
            String hostName = readHostName(p);
            String membersRaw = readMembers(p);
            String capacityRaw = readCapacity(p);
            String countdownRaw = readCountdown(p);
            
            // Parse Capacity: "Remaining / Total"
            long remaining = -1, total = -1;
            if (capacityRaw != null && capacityRaw.contains("/")) {
                String[] parts = capacityRaw.split("/", 2);
                remaining = CompactGameNumberParser.parseCompactNumber(parts[0]);
                total = CompactGameNumberParser.parseCompactNumber(parts[1]);
            }
            
            // Parse Members: "Current / Max"
            int currentMem = -1, maxMem = -1;
            if (membersRaw != null && membersRaw.contains("/")) {
                String[] parts = membersRaw.split("/", 2);
                currentMem = (int) CompactGameNumberParser.parseCompactNumber(parts[0]);
                maxMem = (int) CompactGameNumberParser.parseCompactNumber(parts[1]);
            }
            
            // Parse Countdown
            Duration cd = GameTimeUtils.parseMinutesSeconds(countdownRaw);
            
            // Calculate current troops based on remaining and total. 
            long currentTroops = -1;
            if (total != -1 && remaining != -1 && total >= remaining) {
                currentTroops = total - remaining;
            }

            BearRallyCandidate candidate = new BearRallyCandidate(
                p, cardArea, hostName, currentMem, maxMem, currentTroops, total, remaining, cd, now, true
            );
            
            log.info("Scanned Bear Candidate: {}", candidate.getCandidateKey());
            candidates.add(candidate);
        }
        
        return candidates;
    }

    private String readHostName(PointData btnPoint) {
        AreaData area = new AreaData(
            new PointData(CommonGameAreas.BEAR_TRAP_INITIATOR_X1, btnPoint.getY() + CommonGameAreas.BEAR_TRAP_INITIATOR_DY1),
            new PointData(CommonGameAreas.BEAR_TRAP_INITIATOR_X2, btnPoint.getY() + CommonGameAreas.BEAR_TRAP_INITIATOR_DY2)
        );
        try {
            return ocrEngine.extractText(null, area.topLeft(), area.bottomRight());
        } catch (Exception e) {
            return null;
        }
    }

    private String readMembers(PointData btnPoint) {
        AreaData area = new AreaData(
            new PointData(CommonGameAreas.BEAR_TRAP_MEMBERS_X1, btnPoint.getY() + CommonGameAreas.BEAR_TRAP_MEMBERS_DY1),
            new PointData(CommonGameAreas.BEAR_TRAP_MEMBERS_X2, btnPoint.getY() + CommonGameAreas.BEAR_TRAP_MEMBERS_DY2)
        );
        try {
            return ocrEngine.extractText(null, area.topLeft(), area.bottomRight());
        } catch (Exception e) {
            return null;
        }
    }

    private String readCapacity(PointData btnPoint) {
        AreaData area = new AreaData(
            new PointData(CommonGameAreas.BEAR_TRAP_CAPACITY_X1, btnPoint.getY() + CommonGameAreas.BEAR_TRAP_CAPACITY_DY1),
            new PointData(CommonGameAreas.BEAR_TRAP_CAPACITY_X2, btnPoint.getY() + CommonGameAreas.BEAR_TRAP_CAPACITY_DY2)
        );
        try {
            return ocrEngine.extractText(null, area.topLeft(), area.bottomRight());
        } catch (Exception e) {
            return null;
        }
    }

    private String readCountdown(PointData btnPoint) {
        AreaData area = new AreaData(
            new PointData(CommonGameAreas.BEAR_TRAP_COUNTDOWN_X1, btnPoint.getY() + CommonGameAreas.BEAR_TRAP_COUNTDOWN_DY1),
            new PointData(CommonGameAreas.BEAR_TRAP_COUNTDOWN_X2, btnPoint.getY() + CommonGameAreas.BEAR_TRAP_COUNTDOWN_DY2)
        );
        try {
            return ocrEngine.extractText(null, area.topLeft(), area.bottomRight());
        } catch (Exception e) {
            return null;
        }
    }
}
