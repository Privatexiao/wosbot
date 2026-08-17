# Rotating Events And Deals Navigation

Whiteout Survival fills the Events and Deals header carousels from the currently active
content, so tab ordering and the number of required swipes are not stable. Frostguard searches
the measured header strip for target-specific templates instead of tapping a remembered slot.

The navigator first checks the visible strip, then performs at most three reset-direction swipes
and seven scan-direction swipes. The one-second delay after every gesture is intentional: the
carousel continues moving after ADB reports the swipe as complete, and matching during that
animation has missed otherwise high-confidence templates in live runs.

A tab match establishes only where to tap. Navigation succeeds only when a target-specific
template proves that the intended screen opened. Selected header variants can provide this proof
when their visual state is distinct; otherwise controls from the destination body are required.
An unverified tap recovers through Home once and repeats the complete bounded flow. A second
failure is reported as unavailable or unknown rather than allowing the calling task to continue.

Bank real-frame evidence lives under `modules/tasks/src/test/resources/bank`. The hidden frame
rejects the Bank template below the 90% runtime threshold, while the exposed carousel frame finds
it inside the shared header region. The Events calendar fixture under
`modules/automation/src/test/resources/navigation/rotating-menu` proves that Alliance Mobilization
matches in the header but cannot be selected from the repeated calendar artwork below it. A live
MuMu slot 1 run confirmed Alliance Mobilization through its selected header and Bank through its
active-deposit control. Remaining rotating targets still require representative saved frames and
live-log confirmation before merge readiness. Alliance Championship has current saved-frame proof
for both its unselected header tab and registered destination screen, plus live navigation confirmed
through the Register destination control on MuMu slot 1.

The in-game calendar is not part of this navigation contract. Its truncated labels and differently
scaled artwork require separate evidence. It may later supply availability or schedule hints, but
those hints must not replace destination verification.
