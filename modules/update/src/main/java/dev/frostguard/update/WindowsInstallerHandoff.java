package dev.frostguard.update;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WindowsInstallerHandoff implements InstallerHandoff {
    static final String INSTALLER_ENV = "FROSTGUARD_UPDATE_INSTALLER";
    static final String PID_ENV = "FROSTGUARD_UPDATE_PARENT_PID";
    static final String TOKEN_PATH_ENV = "FROSTGUARD_UPDATE_TOKEN_PATH";
    static final String TOKEN_VALUE_ENV = "FROSTGUARD_UPDATE_TOKEN_VALUE";
    private static final String HANDOFF_SCRIPT = "$targetPid = [int]$env:" + PID_ENV + "; "
            + "$deadline = [DateTime]::UtcNow.AddMinutes(5); "
            + "while ((Get-Process -Id $targetPid -ErrorAction SilentlyContinue) -and "
            + "[DateTime]::UtcNow -lt $deadline) { Start-Sleep -Milliseconds 200 }; "
            + "if (Get-Process -Id $targetPid -ErrorAction SilentlyContinue) { exit 2 }; "
            + "if (-not (Test-Path -LiteralPath $env:" + TOKEN_PATH_ENV + ")) { exit 3 }; "
            + "$token = Get-Content -Raw -LiteralPath $env:" + TOKEN_PATH_ENV + "; "
            + "if ($token -ne $env:" + TOKEN_VALUE_ENV + ") { exit 4 }; "
            + "Remove-Item -LiteralPath $env:" + TOKEN_PATH_ENV + " -Force; "
            + "Start-Process -FilePath $env:" + INSTALLER_ENV;

    private final DetachedProcessStarter processStarter;

    public WindowsInstallerHandoff() {
        this((command, environment) -> {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.environment().putAll(environment);
            builder.start();
        });
    }

    WindowsInstallerHandoff(DetachedProcessStarter processStarter) {
        this.processStarter = processStarter;
    }

    @Override
    public HandoffSession stage(Path installer, long parentPid) throws UpdateException {
        if (!Files.isRegularFile(installer)) {
            throw new UpdateException("Installer handoff target does not exist: " + installer);
        }
        if (parentPid <= 0) {
            throw new UpdateException("Installer handoff requires a valid Frostguard process ID");
        }
        List<String> command = List.of(
                "powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden",
                "-ExecutionPolicy", "Bypass", "-Command", HANDOFF_SCRIPT);
        String tokenValue = UUID.randomUUID().toString();
        Path tokenPath = installer.resolveSibling(installer.getFileName() + ".handoff-ready");
        try {
            Files.deleteIfExists(tokenPath);
        } catch (IOException exception) {
            throw new UpdateException("Could not clear an earlier installer handoff token", exception);
        }
        Map<String, String> environment = Map.of(
                INSTALLER_ENV, installer.toAbsolutePath().normalize().toString(),
                PID_ENV, Long.toString(parentPid),
                TOKEN_PATH_ENV, tokenPath.toAbsolutePath().normalize().toString(),
                TOKEN_VALUE_ENV, tokenValue);
        try {
            processStarter.start(command, environment);
        } catch (IOException exception) {
            throw new UpdateException(
                    "Could not start the external installer handoff: " + exception.getMessage(), exception);
        }
        return new HandoffSession() {
            @Override
            public void authorize() throws UpdateException {
                Path temporary = tokenPath.resolveSibling(tokenPath.getFileName() + ".tmp");
                try {
                    Files.writeString(temporary, tokenValue);
                    try {
                        Files.move(temporary, tokenPath, StandardCopyOption.ATOMIC_MOVE,
                                StandardCopyOption.REPLACE_EXISTING);
                    } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
                        Files.move(temporary, tokenPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException exception) {
                    throw new UpdateException("Could not authorize the installer handoff", exception);
                }
            }

            @Override
            public void cancel() {
                try {
                    Files.deleteIfExists(tokenPath);
                    Files.deleteIfExists(tokenPath.resolveSibling(tokenPath.getFileName() + ".tmp"));
                } catch (IOException ignored) {
                }
            }
        };
    }

    @FunctionalInterface
    interface DetachedProcessStarter {
        void start(List<String> command, Map<String, String> environment) throws IOException;
    }
}
