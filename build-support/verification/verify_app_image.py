#!/usr/bin/env python3
"""Verify a native Frostguard jpackage application image."""

from __future__ import annotations

import argparse
import re
import zipfile
from pathlib import Path

MINIMUM_RUNTIME_JARS = 50
STATIC_REQUIRED_FILES = (
    "FrostguardWatcher.exe",
    "runtime/bin/server/jvm.dll",
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


def inspect_image(
        image: Path, channel: str = "stable", product_name: str = "Frostguard"
) -> list[str]:
    problems: list[str] = []
    if not image.is_dir():
        return [f"Application image does not exist: {image}"]

    files = {
        path.relative_to(image).as_posix(): path
        for path in image.rglob("*")
        if path.is_file()
    }
    required_files = (*STATIC_REQUIRED_FILES, f"{product_name}.exe", f"app/{product_name}.cfg")
    for required in required_files:
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

    common_java_options = (
        f"java-options=-Dfrostguard.channel={channel}",
        f"java-options=-Dfrostguard.application.id=dev.frostguard.desktop{'.nightly' if channel == 'nightly' else ''}",
        "java-options=-Duser.dir=$APPDIR",
        "java-options=-Dfrostguard.watcher.launcher=$APPDIR/../FrostguardWatcher.exe",
    )
    config_settings = {
        f"app/{product_name}.cfg": (
            "app.mainclass=dev.frostguard.app.bootstrap.Main",
            "java-options=-Dfrostguard.update.manifest.stable=",
            "java-options=-Dfrostguard.update.manifest.nightly=",
            *common_java_options,
        ),
        "app/FrostguardWatcher.cfg": (
            "app.mainclass=dev.frostguard.watcher.TelegramWatcher",
            f"java-options=-Dfrostguard.launcher=$APPDIR/../{product_name}.exe",
            *common_java_options,
        ),
    }
    config_identities: dict[str, str] = {}
    for config_path, settings in config_settings.items():
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
    parser.add_argument("--channel", choices=("stable", "nightly"), default="stable")
    parser.add_argument("--product-name", default="Frostguard")
    args = parser.parse_args(argv)
    problems = inspect_image(args.image, args.channel, args.product_name)
    if problems:
        for problem in problems:
            print(f"::error::{problem}")
        return 1
    print(f"Native application image verification passed: {args.image}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
