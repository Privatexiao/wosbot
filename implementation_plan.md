# Implement Hospital Heal Calculation and Monitor Loop

## Background
The Hospital Heal routine needs to automatically calculate the batch size of troops to heal based on single-troop heal time and an estimated alliance help reduction. After starting the heal, it needs to monitor the remaining time. If the remaining time after helps exceeds HOSPITAL_HEAL_MAX_WAIT_MINUTES_INT, it should cancel the heal and readjust the batch size.

## Proposed Changes

### HospitalHealRoutine.java

#### 1. [MODIFY] HospitalHealRoutine.java
- **READ State**:
  - Tap the troop input box and input 1 to activate the Heal button.
  - OCR the heal time to get singleTroopTime.
- **CALCULATE State**:
  - Calculate atchedAmountToHeal using a baseline alliance help estimate (e.g., 20 helps * 60 seconds).
  - Formula: atchedAmountToHeal = (EstimatedHelpSeconds) / singleTroopTime.
- **INPUT State**:
  - Tap the input box and write atchedAmountToHeal.
- **START State & REQUEST_HELP State**:
  - Click Heal, then click Request Help.
- **MONITOR State**:
  - Wait briefly for helps to apply (or read the timer on the field hospital).
  - If the timer exceeds HOSPITAL_HEAL_MAX_WAIT_MINUTES_INT * 60, click the Cancel button (we need to define the Cancel button click logic).
  - If canceled, reduce the estimated help seconds and transition back to CALCULATE to try a smaller batch.
  - If within threshold, transition to COMPLETE.

## Open Questions
- What is the best coordinate to click "Cancel"? In Whiteout Survival, the cancel button is usually an 'X' or a 'Cancel' text button next to the progress bar.
- How long should we wait in MONITOR before deciding to cancel? Alliance helps might take a few minutes to roll in. Should we wait 1-2 minutes before checking the threshold?

