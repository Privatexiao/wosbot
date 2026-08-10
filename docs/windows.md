# Windows Setup

This document summarizes Windows-specific setup for Frostguard.

The [latest Stable Windows bundle](https://github.com/Shederator/wosbot/releases/latest/download/frostguard-windows-desktop-bundle.zip)
is versioned and remains unchanged until the next Stable release. The
[latest Nightly](https://github.com/Shederator/wosbot/releases/download/nightly/frostguard-windows-desktop-bundle.zip)
is rebuilt daily from `main`. Git, Git LFS and Maven are needed only when
building from source.

## Build Requirements

- Windows 10 or Windows 11.
- Java JDK 21 or newer.
- Git and Git LFS.

WiX Toolset 3.14.1 is required only when producing the native EXE installer.
Running an installed native build does not require a separately installed JDK.

## Native application updates

Installed Frostguard builds provide **Config > Updates** when their product
identity has a configured release manifest. The update flow:

1. selects only a newer artifact for the running channel, Windows, and x64;
2. shows the version, release notes, channel, and size before confirmation;
3. downloads below the selected workspace's `cache\updates` directory;
4. verifies the declared size, SHA-256, and exact Authenticode signer;
5. stops scheduling and workspace services, closes SQLite, and releases the
   workspace lock;
6. exits before an external waiter launches the installer.

Development and PR-test packages cannot use automatic release updates. An
unsigned installer is never handed off. Public automatic updates remain
disabled until the release workflow can provide signed Frostguard 3.0
installers and atomically promoted manifests.

Interrupted downloads remain `.part` files and resume when the server supports
byte ranges. A failed size, hash, or signature check prevents installer handoff
and leaves the current installation untouched.

Recommended installs:

```powershell
winget install Microsoft.Git
winget install EclipseAdoptium.Temurin.21.JDK
winget install GitHub.GitLFS
```

From the repository root, verify:

```powershell
java -version
.\mvnw.cmd -version
git lfs version
```

## Build Commands

Use the checked-in Maven Wrapper:

```powershell
.\mvnw.cmd package
```

The wrapper only builds the reactor. It does not stop running processes, install
Frostguard, or mutate user data.

To build a self-contained Windows application image:

```powershell
.\mvnw.cmd -Pwindows-app-image package
python build-support/verification/verify_app_image.py packaging/desktop/target/app-image/Frostguard
powershell -ExecutionPolicy Bypass -File build-support/verification/smoke_test_app_image.ps1 -ImagePath packaging/desktop/target/app-image/Frostguard
```

To build both the application image and a versioned per-user EXE installer,
install WiX Toolset 3.14.1, ensure `candle.exe` and `light.exe` are on `PATH`,
then run:

```powershell
.\mvnw.cmd "-Pwindows-app-image,windows-installer" package
```

Outputs remain below `packaging/desktop/target`: the directly runnable image is
at `app-image/Frostguard`, and the installer is under `installers`. Native
packaging is opt-in because it is Windows-specific; ordinary `mvn package`
continues to build and test the platform-neutral reactor.

## Runtime Requirements

Configure the emulator for a stable `720x1280` display at `320 DPI`. MuMu Player is recommended.

Inside Whiteout Survival:

- Set language to English.
- Disable day/night effects.
- Disable snow effects.
- Keep graphics settings stable between runs.

The application currently packages Windows ADB and Tesseract assets from `tools/`.

## Starting Frostguard

After downloading a desktop bundle, extract the complete ZIP into an empty
folder and double-click `Start Frostguard.bat`. The launcher locates the
versioned application JAR and reports a clear error if Java 21 is missing.

For a native Frostguard 3.0 build, run the installed `Frostguard.exe` instead.
The per-user installer defaults to `%LOCALAPPDATA%\Frostguard`, while
all mutable databases, configuration, logs, watcher state, and custom tasks
remain in the selected workspace below `%USERPROFILE%\.frostguard`. The
installation directory is treated as read-only application content.

For a source build, run from the repository root:

```powershell
.\mvnw.cmd javafx:run
```

This automatically creates and uses `<worktree>\.frostguard-dev\`. Each
worktree therefore has isolated database, logs, custom tasks, cache, and
Telegram watcher state. `git clean -xdf` intentionally removes this disposable
development workspace.

For automatic startup through scripts or Task Scheduler:

```powershell
.\mvnw.cmd "-Djavafx.args=--autostart" javafx:run
```

Installed and extracted bundle deployments should use their supplied launcher;
the Maven command is only for source development.

## Scheduled Automation

Optional Task Scheduler templates are in `docs/schedule-autostart/`.

Use them when the machine should wake, run Frostguard for a fixed window, stop the emulator, and return to standby. Edit imported task actions before enabling them:

- Update the path to `launch.ps1`.
- Update the working directory to your Frostguard installation.
- Adjust the schedule times.
- Confirm the emulator process name, for example `MuMuNxMain`.

The templates are examples and should be reviewed on the target Windows machine before unattended use.
