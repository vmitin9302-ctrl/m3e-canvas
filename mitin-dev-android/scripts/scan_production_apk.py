"""Scan the distributed APK, never the separate instrumentation APK."""
from pathlib import Path
import hashlib
import json
import re
import xml.etree.ElementTree as ET
import zipfile

root = Path('evidence')
apk = root / 'MITIN-DEV-Public-v1.apk'
manifest = ET.parse(root / 'manifest.xml').getroot()
ns = '{http://schemas.android.com/apk/res/android}'
app = manifest.find('application')
assert app.get(ns + 'usesCleartextTraffic') == 'false'
assert app.get(ns + 'allowBackup') == 'false'
assert app.get(ns + 'networkSecurityConfig') is None
assert manifest.get('package') == 'dev.mitin.app'
patterns = [rb'gh[pousr]_[A-Za-z0-9]{30,}', rb'-----BEGIN (?:RSA |EC )?PRIVATE KEY-----', rb'postgres(?:ql)?://[^\s]+:[^\s]+@']
forbidden = [b'client-a@example.com', b'synthetic@example.com', b'mitin-ci-disposable-only', b'  synthetic mobile test password  ']
with zipfile.ZipFile(apk) as archive:
    assert not any('mitin_test_ca' in n or 'mitin-test-ca' in n or n.endswith('.pem') for n in archive.namelist())
    for name in archive.namelist():
        data = archive.read(name)
        assert not any(p in data for p in forbidden), 'Test credential found'
        assert not any(re.search(p, data) for p in patterns), 'Secret-like value found'
        if name.endswith('.dex'):
            assert b'DemoRepository' not in data
digest = hashlib.sha256(apk.read_bytes()).hexdigest()
(root / 'security.json').write_text(json.dumps({'apk_sha256': digest, 'system_tls_only': True, 'custom_ca': False,
    'cleartext': False, 'embedded_secret_scan': 'PASS', 'signing': 'ephemeral debug signing, not release key'}, indent=2))
print('APK boundary scan: PASS')
