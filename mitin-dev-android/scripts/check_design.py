"""Offline source contract; run before Gradle. Does not generate the application."""
import hashlib
import json
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
design = root / "app/src/main/assets/design/mitin-dev.m3e.json"
assert hashlib.sha256(design.read_bytes()).hexdigest() == "23eed7305902b56bf527c7ee4b66f3b2f69dad5d48b08f4bd537aef349b01e7d"
frames = json.loads(design.read_text())["frames"]
assert len(frames) == len({frame["id"] for frame in frames}) == 52
coverage = (root / "docs/DESIGN_COVERAGE.md").read_text()
assert all(f'`{frame["id"]}`' in coverage for frame in frames)
manifest = ET.parse(root / "app/src/main/AndroidManifest.xml").getroot()
assert not manifest.findall("uses-permission")
android = "{http://schemas.android.com/apk/res/android}"
assert manifest.find("application").get(android + "allowBackup") == "false"
assert manifest.find("application").get(android + "fullBackupContent") == "false"
backup = ET.parse(root / "app/src/main/res/xml/data_extraction_rules.xml").getroot()
assert all(backup.find(kind).findall("exclude") for kind in ("cloud-backup", "device-transfer"))
source = "\n".join(p.read_text() for p in (root / "app/src/main/java").rglob("*.kt"))
assert "WebView" not in source
assert "https://" not in source and "http://" not in source
print("52 target frames preserved; native offline source boundary verified")
