#!/usr/bin/env python3
"""Verify Stable/Nightly packaging and release-publication contracts."""

from __future__ import annotations

import unittest
import xml.etree.ElementTree as ET
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def properties(element: ET.Element) -> dict[str, str]:
    node = element.find("m:properties", NS)
    if node is None:
        return {}
    return {child.tag.rsplit("}", 1)[-1]: child.text or "" for child in node}


class ChannelPackagingTest(unittest.TestCase):
    def test_stable_and_nightly_use_distinct_durable_windows_identities(self):
        root = ET.parse(REPO_ROOT / "packaging/desktop/pom.xml").getroot()
        stable = properties(root)
        nightly_profile = next(
            profile for profile in root.findall("m:profiles/m:profile", NS)
            if profile.find("m:id", NS).text == "windows-nightly"
        )
        nightly = properties(nightly_profile)

        expected_stable = {
            "frostguard.release.channel": "stable",
            "frostguard.product.name": "Frostguard",
            "frostguard.product.identifier": "dev.frostguard.desktop",
            "frostguard.product.install-dir": "Frostguard",
        }
        expected_nightly = {
            "frostguard.release.channel": "nightly",
            "frostguard.product.name": "Frostguard Nightly",
            "frostguard.product.identifier": "dev.frostguard.desktop.nightly",
            "frostguard.product.install-dir": "Frostguard Nightly",
        }
        for key, value in expected_stable.items():
            self.assertEqual(value, stable[key])
        for key, value in expected_nightly.items():
            self.assertEqual(value, nightly[key])
        self.assertNotEqual(stable["frostguard.product.upgrade-uuid"],
                            nightly["frostguard.product.upgrade-uuid"])

        pom = (REPO_ROOT / "packaging/desktop/pom.xml").read_text(encoding="utf-8")
        for contract in (
            "-Dfrostguard.application.id=${frostguard.product.identifier}",
            "-Dfrostguard.channel=${frostguard.release.channel}",
            "-Dfrostguard.update.manifest.stable=${frostguard.update.manifest.stable}",
            "-Dfrostguard.update.manifest.nightly=${frostguard.update.manifest.nightly}",
            "${project.build.directory}/installers/${frostguard.release.channel}",
        ):
            self.assertIn(contract, pom)

    def test_signed_release_publishes_manifest_after_signed_installer_verification(self):
        workflow = (REPO_ROOT / ".github/workflows/signed-windows-channel-release.yml").read_text(
            encoding="utf-8")
        ordered_steps = (
            "Sign and verify immutable installer",
            "Create draft release and verify uploaded installer",
            "Generate update manifest from the signed installer",
            "Publish immutable release and channel manifest last",
        )
        positions = [workflow.index(step) for step in ordered_steps]
        self.assertEqual(sorted(positions), positions)
        self.assertIn("FROSTGUARD_WINDOWS_SIGNING_CERTIFICATE_BASE64", workflow)
        self.assertIn("Get-AuthenticodeSignature", workflow)
        self.assertIn("windows_installer_version.py", workflow)
        self.assertIn("gh release upload updates-nightly $env:MANIFEST", workflow)
        self.assertIn("Remove an abandoned draft release", workflow)
        self.assertIn("--cleanup-tag --yes", workflow)
        legacy_stable = (REPO_ROOT / ".github/workflows/stable-windows-release.yml").read_text(
            encoding="utf-8")
        self.assertIn("Frostguard 3.x must use Signed Windows Channel Release", legacy_stable)


if __name__ == "__main__":
    unittest.main(verbosity=2)
