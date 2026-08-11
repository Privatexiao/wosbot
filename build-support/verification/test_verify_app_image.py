#!/usr/bin/env python3

from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import verify_app_image  # noqa: E402


class VerifyAppImageTest(unittest.TestCase):

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.image = Path(self.temp.name) / "Frostguard"
        files = list(verify_app_image.REQUIRED_FILES) + [
            "app/frostguard-desktop-3.0.0.jar",
            "app/frostguard-watcher-3.0.0.jar",
            "app/lib/opencv-4.9.0.jar",
            "app/lib/tess4j-5.14.0.jar",
            "app/lib/javafx-graphics-23.0.1-win.jar",
            "app/templates/home/world.png",
            "app/custom_tasks/shield.java",
        ] + [f"app/lib/runtime-{index}.jar" for index in range(60)]
        for relative in files:
            path = self.image / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(b"payload")
        common_options = "\n".join(verify_app_image.COMMON_JAVA_OPTIONS) + "\n"
        (self.image / "app/Frostguard.cfg").write_text(
            "app.mainclass=dev.frostguard.app.bootstrap.Main\n" + common_options,
            encoding="utf-8")
        (self.image / "app/FrostguardWatcher.cfg").write_text(
            "app.mainclass=dev.frostguard.watcher.TelegramWatcher\n" + common_options,
            encoding="utf-8")

    def tearDown(self):
        self.temp.cleanup()

    def test_accepts_complete_image(self):
        self.assertEqual([], verify_app_image.inspect_image(self.image))

    def test_rejects_missing_bundled_runtime(self):
        (self.image / "runtime/bin/server/jvm.dll").unlink()
        self.assertTrue(any("jvm.dll" in item for item in verify_app_image.inspect_image(self.image)))

    def test_rejects_development_channel_launcher(self):
        config = self.image / "app/Frostguard.cfg"
        config.write_text(config.read_text().replace("channel=stable", "channel=development"))
        self.assertTrue(any("channel=stable" in item for item in verify_app_image.inspect_image(self.image)))

    def test_rejects_watcher_without_stable_channel(self):
        config = self.image / "app/FrostguardWatcher.cfg"
        config.write_text(config.read_text().replace("channel=stable", "channel=development"))
        self.assertTrue(any("FrostguardWatcher.cfg" in item for item in verify_app_image.inspect_image(self.image)))

    def test_rejects_runtime_data(self):
        leaked = self.image / "app/logs/frostguard.log"
        leaked.parent.mkdir(parents=True)
        leaked.write_text("private runtime log")
        self.assertTrue(any("leaked" in item for item in verify_app_image.inspect_image(self.image)))


if __name__ == "__main__":
    unittest.main(verbosity=2)
