# Custom Features Synchronous Maintenance Rule

Whenever adding, updating, or repairing any feature, bug fix, or UI adjustment in this repository (`Privatexiao/wosbot`) that differs from or extends upstream (`Shederator/wosbot`):

1. **Synchronous Documentation Update**: Update [docs/custom-features.md](file:///E:/MeComputer/Desktop/wosbot/docs/custom-features.md) immediately with the technical rationale, background, and implementation details.
2. **Merge Safety**: When pulling or merging future upstream updates, ensure that every custom feature listed in `docs/custom-features.md` is preserved and merged cleanly into the new module structure.
3. **No Unrequested Git Push**: Never run `git push` unless the user explicitly requests to commit and push changes (`提交推送git`).
