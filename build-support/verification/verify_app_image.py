#!/usr/bin/env python3
"""Verify a native Frostguard jpackage application image."""

from __future__ import annotations

import argparse
import re
import zipfile
from pathlib import Path

MINIMUM_RUNTIME_JARS = 50
REQUIRED_FILES = (
    "Frostguard.exe",
    "FrostguardWatcher.exe",
    "runtime/bin/server/jvm.dll",
    "app/Frostguard.cfg",
    "app/FrostguardWatcher.cfg",
    "app/lib/adb/adb.exe",
    "app/lib/adb/AdbWinApi.dll",
    "app/lib/adb/AdbWinUsbApi.dll",
    "app/lib/tesseract/eng.traineddata",
    "app/lib/tesseract/osd.traineddata",
    "app/lib/tesseract/chi_sim.traineddata",
)
FORBIDDEN_NAMES = {
    "frostguard-workspace.json",
    "frostguard.db",
    "telegram-watcher.properties",
}
BUILD_METADATA = "dev/frostguard/app/frostguard-build.properties"
COMMON_JAVA_OPTIONS = (
    "java-options=-Dfrostguard.channel=stable",
    "java-options=-Duser.dir=$APPDIR",
    "java-options=-Dfrostguard.launcher=$APPDIR/../Frostguard.exe",
    "java-options=-Dfrostguard.watcher.launcher=$APPDIR/../FrostguardWatcher.exe",
)
CONFIG_SETTINGS = {
    "app/Frostguard.cfg": (
        "app.mainclass=dev.frostguard.app.bootstrap.Main",
        "java-options=-Dfrostguard.update.manifest.stable=",
        *COMMON_JAVA_OPTIONS,
    ),
    "app/FrostguardWatcher.cfg": (
        "app.mainclass=dev.frostguard.watcher.TelegramWatcher",
        *COMMON_JAVA_OPTIONS,
    ),
}


def inspect_image(image: Path) -> list[str]:
    problems: list[str] = []
    if not image.is_dir():
        return [f"Application image does not exist: {image}"]

    files = {
        path.relative_to(image).as_posix(): path
        for path in image.rglob("*")
        if path.is_file()
    }
    for required in REQUIRED_FILES:
        if required not in files:
            problems.append(f"Application image is missing {required}")

    runtime_jars = [name for name in files if re.match(r"^app/lib/[^/]+\.jar$", name)]
    if len(runtime_jars) < MINIMUM_RUNTIME_JARS:
        problems.append(
            f"Only {len(runtime_jars)} runtime JARs found; expected at least "
            f"{MINIMUM_RUNTIME_JARS}"
        )
    for pattern, description in (
        (r"^app/frostguard-desktop-[^/]+\.jar$", "desktop JAR"),
        (r"^app/frostguard-watcher-[^/]+\.jar$", "watcher JAR"),
        (r"^app/lib/frostguard-update-[^/]+\.jar$", "update module"),
        (r"^app/lib/opencv-[^/]+\.jar$", "OpenCV runtime"),
        (r"^app/lib/tess4j-[^/]+\.jar$", "Tess4J runtime"),
        (r"^app/lib/javafx-graphics-[^/]+-win\.jar$", "Windows JavaFX runtime"),
        (r"^app/templates/.+\.png$", "template browser assets"),
        (r"^app/custom_tasks/.+$", "custom task examples"),
    ):
        if not any(re.match(pattern, name) for name in files):
            problems.append(f"Application image has no {description}")

    config_identities: dict[str, str] = {}
    for config_path, settings in CONFIG_SETTINGS.items():
        if config_path not in files:
            continue
        config = files[config_path].read_text(encoding="utf-8")
        for setting in settings:
            if setting not in config:
                problems.append(f"{Path(config_path).name} is missing: {setting}")
        identity = re.search(
            r"-Dfrostguard\.update\.pullRequestBuild=(true|false)", config
        )
        if identity is None:
            problems.append(
                f"{Path(config_path).name} is missing its PR-build update identity"
            )
        else:
            config_identities[config_path] = identity.group(1)

    desktop_jars = [
        path for name, path in files.items()
        if re.match(r"^app/frostguard-desktop-[^/]+\.jar$", name)
    ]
    if len(desktop_jars) == 1:
        try:
            with zipfile.ZipFile(desktop_jars[0]) as desktop_jar:
                metadata = desktop_jar.read(BUILD_METADATA).decode("utf-8")
            metadata_values = {}
            for line in metadata.splitlines():
                key, separator, value = line.partition("=")
                if not separator or key in metadata_values:
                    metadata_values = {}
                    break
                metadata_values[key] = value
            if (set(metadata_values) != {"pullRequestBuild", "authenticodePublisher"}
                    or metadata_values["pullRequestBuild"] not in {"true", "false"}):
                problems.append("Desktop JAR has an invalid PR-build update identity")
            else:
                embedded_identity = metadata_values["pullRequestBuild"]
                for config_path, config_identity in config_identities.items():
                    if config_identity != embedded_identity:
                        problems.append(
                            f"{Path(config_path).name} PR-build identity does not match the desktop JAR"
                        )
        except (KeyError, UnicodeDecodeError, zipfile.BadZipFile):
            problems.append("Desktop JAR has no valid embedded PR-build update identity")

    for relative in files:
        path = Path(relative)
        lower_name = path.name.lower()
        if lower_name in FORBIDDEN_NAMES or lower_name.endswith((".db-wal", ".db-shm", ".log")):
            problems.append(f"Runtime/user data leaked into the application image: {relative}")
        if any(part.lower() in {".frostguard", ".frostguard-dev", "logs"} for part in path.parts):
            problems.append(f"Runtime/user-data directory leaked into the application image: {relative}")
    return problems


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("image", type=Path)
    args = parser.parse_args(argv)
    problems = inspect_image(args.image)
    if problems:
        for problem in problems:
            print(f"::error::{problem}")
        return 1
    print(f"Native application image verification passed: {args.image}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
