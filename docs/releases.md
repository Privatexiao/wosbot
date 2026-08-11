# Releases

Frostguard publishes signed installed releases for Stable and Nightly plus
temporary ZIP builds for pull-request testing. The existing daily and Stable
ZIP workflows remain transitional until #155 separates validation from public
Nightly publication.

| Type | Audience | Lifetime | Discord notification |
|---|---|---|---|
| Stable `vX.Y.Z` | Regular users | Permanent | Update the maintained Stable message |
| Daily `nightly` | Testers | Replaced daily | Update the daily download, no mass mention |
| PR test `pr-test-*` | Requester/testers | Temporary | Reply only to the requester |

## Signed installed releases

Run **Signed Windows Channel Release** manually from `main`. It requires a
semantic version, the target `stable` or `nightly` identity, and the minimum
supported updater version. Stable versions use `X.Y.Z`; Nightly versions use an
immutable prerelease such as `3.1.0-nightly.20260811.1`.

Windows Installer compares only three numeric version fields. Stable maps
directly to `X.Y.Z`. Nightly derives an independent, monotonically increasing
Windows identity from `YYYYMMDD.N`; for example, the Nightly above uses
`26.8.11001`. Use the current date and a sequence from 1 through 999, increasing
the sequence for additional Nightlies on the same day.

The workflow requires these repository secrets:

- `FROSTGUARD_WINDOWS_SIGNING_CERTIFICATE_BASE64` — the PFX encoded as Base64;
- `FROSTGUARD_WINDOWS_SIGNING_CERTIFICATE_PASSWORD` — the PFX password;
- `FROSTGUARD_AUTHENTICODE_PUBLISHER` — the exact certificate subject embedded
  into the application and required by every update manifest.

Stable and Nightly use different application IDs, upgrade UUIDs, install
directories, shortcuts, workspaces, and feeds. The workflow builds and smokes
the selected identity, signs the installer, verifies the exact subject,
uploads and re-downloads the immutable installer, derives its final size and
SHA-256, and publishes the manifest last. Stable exposes its manifest through
the latest immutable release; Nightly points `updates-nightly` at an installer
stored in an immutable `nightly-<version>` release.

A failure before publication removes the abandoned draft release and tag so the
same immutable version can be retried. If a Nightly release becomes public but
promotion of the rolling `updates-nightly` manifest fails afterward, leave the
immutable release intact and keep the previous rolling manifest active. Recover
by verifying and promoting the manifest asset from that immutable release; do
not rebuild or replace its installer.

## Transitional ZIP promotion

The legacy Stable ZIP workflow promotes an already successful `Nightly Windows Bundle` run from
`main`; they do not rebuild a different tree. Run **Stable Windows Release**
manually with:

- `version`: the `X.Y.Z` value declared in `pom.xml`;
- `daily_run_id`: a successful scheduled or manually triggered daily run from
  `main` after the intended release commit.

This workflow rejects Frostguard 3.x. Installed 3.x releases must use the
signed channel workflow so an unsigned ZIP can never become the latest Stable
product accidentally.

The workflow pins the run's exact commit, downloads its versioned artifact,
re-runs structural and launch verification, creates the immutable `vX.Y.Z`
release, verifies its public download URL and then updates the maintained
Stable download without mentioning users. Existing stable tags are never
replaced.

## Discord `#download`

Keep the channel read-only for regular users. Pin the maintained Stable message
and keep exactly one Nightly message directly below it. Both cards are edited
in place; GitHub Releases remains the permanent release history.

### Pinned guide

```text
📥 Frostguard Downloads

Stable — versioned
A tested build that changes only when a new Stable is published:
https://github.com/Shederator/wosbot/releases/latest/download/frostguard-windows-desktop-bundle.zip

Nightly — testing
The newest automated development build. It may contain unfinished changes:
https://github.com/Shederator/wosbot/releases/download/nightly/frostguard-windows-desktop-bundle.zip

Extract the complete archive and use the included Frostguard launcher.
Java 21 or newer is required.
```

The Stable URL is deliberately a direct, version-independent asset URL. GitHub
redirects it to the asset on the latest non-prerelease release. Store the
webhook-owned card ID in `DISCORD_STABLE_MESSAGE_ID`. A Stable promotion updates
the card automatically; `Refresh Stable Discord Message` repairs it manually
from GitHub's Latest release when necessary.

### Nightly message

```text
Latest Nightly — Frostguard <version>

The newest automated development build. It may contain unfinished or unstable
changes.

Download Frostguard <version> for Windows

Changes since the previous Nightly
- <linked PR title or direct commit subject>

Extract the complete archive and use the included Frostguard launcher.
Java 21 or newer is required.
```

The URL is deliberately version-independent. Do not post a new Discord message
for every daily build. Store the webhook-owned message ID in the repository
variable `DISCORD_DAILY_MESSAGE_ID`; successful builds edit that message. Show
at most five linked first-parent changes since the previous Nightly and collapse
older entries into a count.
Build failures remain visible in Actions and do not replace the last working
public download.

## Migration

1. Create `#download` and post the Stable and Nightly messages.
2. Publish the first real Stable release before presenting the Stable download.
3. Store both maintained webhook message IDs as repository variables.
4. Move `/build-pr` results to `#request-a-build`.
5. Archive redundant legacy release channels after their links are replaced.

## Native installer update contract

The Frostguard 3.0 updater uses one immutable HTTPS manifest per channel. Do
not publish a manifest until its installer has been built, Authenticode-signed,
uploaded, and smoke-tested. Signing credentials remain outside the repository.

### Manifest schema 1

```json
{
  "schemaVersion": 1,
  "channel": "stable",
  "version": "3.0.1",
  "publishedAt": "2026-08-10T04:00:00Z",
  "minimumUpdaterVersion": "3.0.0",
  "releaseNotesUrl": "https://example.invalid/releases/3.0.1",
  "artifacts": {
    "windows-x64": {
      "operatingSystem": "windows",
      "architecture": "x64",
      "fileName": "Frostguard-3.0.1-windows-x64.exe",
      "url": "https://example.invalid/releases/3.0.1/Frostguard-3.0.1-windows-x64.exe",
      "sha256": "<64 lowercase hexadecimal characters>",
      "size": 123456789,
      "signature": {
        "type": "authenticode",
        "publisher": "<exact certificate subject>"
      }
    }
  }
}
```

Unknown fields, unsupported schemas, mutable filenames, insecure URLs, and
Windows artifacts without an Authenticode publisher are rejected. Calculate
the hash and size after signing because Authenticode changes the file.

### Build inputs

Embed the Stable endpoint and exact trusted certificate subject at packaging
time:

```powershell
.\mvnw.cmd -Dfrostguard.update.manifest.stable=https://updates.example.invalid/stable.json `
  "-Dfrostguard.update.authenticode-publisher=CN=Frostguard Project, O=Frostguard" `
  "-Pwindows-app-image,windows-installer" package
```

Nightly adds its separate packaging identity and embeds both public endpoints:

```powershell
.\mvnw.cmd -Dfrostguard.update.manifest.stable=https://example.invalid/stable.json `
  -Dfrostguard.update.manifest.nightly=https://example.invalid/nightly.json `
  "-Dfrostguard.update.authenticode-publisher=CN=Frostguard Project, O=Frostguard" `
  "-Pwindows-app-image,windows-installer,windows-nightly" package
```

The checked-in endpoint and publisher defaults are empty, so ordinary local
builds cannot contact or trust a release feed accidentally. PR packaging also
embeds `frostguard.update.pullRequestBuild=true`. Development builds, PR builds,
and builds without a pinned publisher cannot update even if someone supplies a
manifest URL manually. The manifest publisher must match the value embedded in
the application JAR before its installer can be downloaded or executed.

### Publication order

1. Build and smoke-test the native application image.
2. Build the channel-specific installer with its stable upgrade identity.
3. Authenticode-sign the final installer outside the repository.
4. Verify the signature and exact certificate subject on Windows.
5. Calculate the final byte size and SHA-256.
6. Upload the installer to an immutable versioned HTTPS URL.
7. Publish the manifest atomically as the final step.

Never publish a PR artifact, unsigned installer, mutable rolling filename, or
manifest whose artifact has not completed the same verification sequence.

### Runtime and recovery

Downloads belong to the selected workspace under
`cache/updates/<channel>/<version>`. Incomplete data uses a `.part` suffix and
is never exposed as a completed installer. Completion requires an atomic rename
after size and hash verification.

The external Windows handoff receives the Frostguard PID plus a one-time token.
Frostguard authorizes the staged waiter immediately before coordinated
shutdown. The waiter cannot start the installer while the Frostguard PID is
alive, and a failed shutdown deletes the token so a later unrelated application
exit cannot launch the staged installer.
