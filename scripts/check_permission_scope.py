#!/usr/bin/env python3
"""Conservative guardrail check for the Android manifest / accessibility declaration.

This isn't a malware scanner or a substitute for an independent security audit.
"""
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
A = "{http://schemas.android.com/apk/res/android}"
manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
service_config = ET.parse(ROOT / "app/src/main/res/xml/accessibility_service.xml").getroot()
gradle = (ROOT / "app/build.gradle.kts").read_text()

errors = []
requested = {tag.attrib.get(A + "name", "") for tag in manifest
             if tag.tag in ("uses-permission", "uses-permission-sdk-23")}
for permission in ("android.permission.INTERNET", "android.permission.READ_SMS",
                   "android.permission.RECEIVE_SMS", "android.permission.QUERY_ALL_PACKAGES"):
    if permission in requested:
        errors.append(f"Unexpected sensitive permission: {permission}")

services = manifest.findall(".//service")
matching = [s for s in services if s.get(A + "name") == ".automation.AccessibilityAutomationService"]
if len(matching) != 1:
    errors.append("Accessibility service declaration missing or ambiguous")
elif matching[0].get(A + "permission") != "android.permission.BIND_ACCESSIBILITY_SERVICE":
    errors.append("AccessibilityService binding permission changed")

if service_config.get(A + "isAccessibilityTool") != "false":
    errors.append("This general automation app must NOT claim to be a disability accessibility tool")
if not re.search(r"targetSdk\s*=\s*36\b", gradle):
    errors.append("Google Play requires targetSdk 36+ for new phone apps after 2026-08-31")
if not re.search(r"compileSdk\s*=\s*36\b", gradle):
    errors.append("Expected compileSdk 36")

if errors:
    print("FAILED:\n- " + "\n- ".join(errors), file=sys.stderr)
    raise SystemExit(1)
print("PASS: known manifest permission constraints and API 36 target.")
print("NOTE: Static checks do not establish Google Play acceptance or app safety.")
