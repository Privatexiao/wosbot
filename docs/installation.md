# Installation

This guide covers installing a verified Frostguard build, configuring the
required emulator, and building the project from source on Windows.

## Choose a build

| Build | Use it when | Download |
|:------|:------------|:---------|
| Stable | You want a tested, versioned build that changes only with a release | [Latest Stable](https://github.com/Shederator/wosbot/releases/latest/download/frostguard-windows-desktop-bundle.zip) |
| Nightly | You want the latest `main` build, updated daily | [Latest Nightly](https://github.com/Shederator/wosbot/releases/download/nightly/frostguard-windows-desktop-bundle.zip) |
| PR build | You want to test one or more open pull requests | Run `/build-pr` in Discord `#request-a-build` |

Stable and Nightly are public Windows desktop bundles. They require Java 21,
but not Git, Git LFS, or Maven. Nightly may contain unfinished changes; PR
builds additionally contain unmerged code.

The self-contained Windows installer is being introduced for Frostguard 3.0.
Until it is published through the release workflows, the download links above
continue to provide the existing ZIP distribution.

## Install a downloaded build

1. Install a Java 21 JDK, such as [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=21).
2. Download the desired ZIP from the table above.
3. Extract the complete ZIP into an empty folder. Do not run Frostguard from inside the ZIP.
4. Double-click `Start Frostguard.bat`.
5. Open **Configuration** and select the emulator command-line controller.

Keep the extracted installation together. Its launcher, application JAR,
runtime libraries, OCR data and templates are all required.

## Emulator Setup

Supported emulators are MuMu Player, LDPlayer, and MEmu. MuMu Player is recommended.

Use these emulator display settings:

- Resolution: `720x1280`
- DPI: `320`
- CPU: 4 cores recommended
- Memory: 2 GB recommended
- Frame rate: 30 FPS optional

Start the emulator once and confirm Android boots normally.

## Game Setup

Install Whiteout Survival from Google Play inside the emulator.

In game settings:

- Set the language to English.
- Disable day/night effects.
- Disable snow effects.
- Use normal graphics and 30 FPS if available.

## Configure Frostguard

Open the Configuration screen and select the emulator's command-line
controller, not its graphical executable. A common MuMu path is:

```text
C:\Program Files\Netease\MuMuPlayer\nx_main\MuMuManager.exe
```

## Build from source

Source builds additionally require basic Git and terminal usage plus Git LFS.
The checked-in Maven Wrapper downloads the pinned Maven version. Install the
common tools from PowerShell:

```powershell
winget install Microsoft.Git
winget install EclipseAdoptium.Temurin.21.JDK
winget install GitHub.GitLFS
```

Verify the toolchain from the repository root:

```powershell
java -version
mvnw.cmd -version
git lfs version
```

### Source checkout

Clone the repository and fetch LFS assets:

```sh
git clone https://github.com/Shederator/wosbot.git
cd wosbot
git lfs install
git lfs pull
```

### Build

Run the full build from the repository root:

```sh
./mvnw package
```

On Windows Command Prompt, use the wrapper batch launcher:

```batch
mvnw.cmd package
```

The build writes module artifacts below their respective `target` directories
and the transitional desktop bundle ZIP below `packaging/desktop/target`. End
users should extract the ZIP and launch `Start Frostguard.bat`; individual
module JARs are not standalone distributions.

### Build the native Windows package

Native packages must be built on Windows. Build and smoke-test the
self-contained application image with the JDK 21 `jpackage` tool:

```powershell
.\mvnw.cmd -Pwindows-app-image package
python build-support/verification/verify_app_image.py packaging/desktop/target/app-image/Frostguard
powershell -ExecutionPolicy Bypass -File build-support/verification/smoke_test_app_image.ps1 -ImagePath packaging/desktop/target/app-image/Frostguard
```

This produces
`packaging/desktop/target/app-image/Frostguard/Frostguard.exe`. The image
contains its Java runtime, so a machine running it does not need a separate
JDK.

Building the versioned installer additionally requires WiX Toolset 3.14.1 with
`candle.exe` and `light.exe` on `PATH`:

```powershell
.\mvnw.cmd "-Pwindows-app-image,windows-installer" package
```

The installer is written below `packaging/desktop/target/installers`. It is a
per-user installer and defaults to
`%LOCALAPPDATA%\Frostguard`; the installer can offer another location.
Normal `mvn package` remains platform-neutral and does not invoke `jpackage` or
install Frostguard.

Native Stable and Nightly builds can expose a channel-specific update feed in
**Config > Updates**. Development and pull-request builds cannot install from
release feeds. Frostguard accepts an update only after the manifest identity,
download size, SHA-256, and Windows Authenticode signer all match. The current
public ZIP feeds are not used by this updater; automatic installer updates stay
disabled until signed Frostguard 3.0 release manifests are published.

### Run a source build

Run the application from the repository root through the same Maven Wrapper:

```sh
./mvnw javafx:run
```

On Windows Command Prompt, use `mvnw.cmd javafx:run`; in PowerShell, use
`.\mvnw.cmd javafx:run`. The JavaFX goal compiles the required reactor modules
and starts only the desktop module; developers do not need to locate a
versioned JAR or assemble its classpath. It automatically uses the ignored
`.frostguard-dev/` workspace in that clone or worktree. No runtime argument is
required, and simultaneous production and worktree runs do not share data.

Installed runs use named workspaces below
`%USERPROFILE%\.frostguard\workspaces\<channel>\<name>\`. Each workspace owns
its database, configuration, logs, custom tasks, cache, Telegram watcher state,
and process lock. A workspace can be opened by only one Frostguard process at a
time; use a different workspace for another bot instance.

## Migrating an older installation

Do not overwrite a new workspace with an entire old Frostguard folder. Frostguard
3.0 does not migrate the legacy flat `.frostguard` watcher files or a 2.x
database. Keep a backup and recreate settings; copy only reviewed custom-task
source files into the new workspace.
