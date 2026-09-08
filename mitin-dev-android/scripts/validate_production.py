"""Explicitly gated, bounded production exercise. Never calls prepare/confirm.

Three sessions and at most nine model calls. No retry of the whole scenario.
UI matrix reopens the final session with GET; it never regenerates a brief.
"""
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

out = Path('evidence/instrumentation'); out.mkdir(parents=True, exist_ok=True)
runner = 'dev.mitin.app.test/androidx.test.runner.AndroidJUnitRunner'
failures = []


def adb(*args):
    return subprocess.run(['adb', *args], check=True, capture_output=True, text=True, timeout=60).stdout


def phase(label, selector, *extra):
    command = ['adb','shell','am','instrument','-w','-r','-e','class','dev.mitin.internal.'+selector,*extra,runner]
    try:
        result = subprocess.run(command, capture_output=True, text=True, timeout=900)
        text = result.stdout + result.stderr
        exit_code = result.returncode
    except subprocess.TimeoutExpired:
        text = 'Instrumentation exceeded its bounded timeout.'
        exit_code = 1
    text = re.sub(r'Bearer\s+[A-Za-z0-9._-]+', 'Bearer <redacted>', text)
    success = exit_code == 0 and 'OK (1 test)' in text and 'FAILURES!!!' not in text
    (out / (label+'.txt')).write_text(text)
    suite = ET.Element('testsuite', name=label, tests='1', failures='0' if success else '1')
    case = ET.SubElement(suite,'testcase',name=selector)
    if not success: ET.SubElement(case,'failure').text='See redacted instrumentation output'
    ET.ElementTree(suite).write(out / (label+'.xml'),encoding='utf-8',xml_declaration=True)
    if not success: failures.append(label)
    print(label+(': PASS' if success else ': FAIL'),flush=True)


for width in (320, 360, 412):
    for font in (1.0, 1.6):
        adb('shell','wm','size',f'{width*3}x{round(width*3*20/9)}')
        adb('shell','wm','density','480')
        adb('shell','settings','put','system','font_scale',str(font))
        phase(f'composer-{width}-{font}', 'ProductionComposerTest')
adb('shell','wm','size','reset'); adb('shell','wm','density','reset')
adb('shell','settings','put','system','font_scale','1.0')
phase('presentation-errors','ProductionPresentationTest')
adb('shell','am','force-stop','dev.mitin.app')
adb('shell','pm','clear','dev.mitin.app')
adb('shell','am','force-stop','com.google.android.apps.nexuslauncher')
adb('shell','svc','wifi','disable'); adb('shell','svc','data','disable')
adb('shell','cmd','connectivity','airplane-mode','enable')
phase('offline-start','ProductionOfflineTest')
phase('real-ai','ProductionValidationTest#realProductionThreeBriefsNoSubmission','-e','authorizedProductionAi','yes')
for width in (320,360,412):
    for font in (1.0,1.6):
        label=f'{width}dp-{font}'
        adb('shell','wm','size',f'{width*3}x{round(width*3*20/9)}')
        adb('shell','wm','density','480')
        adb('shell','settings','put','system','font_scale',str(font))
        adb('shell','am','force-stop','dev.mitin.app')
        phase(label,'ProductionValidationTest#retainedRealBriefMatrix','-e','matrix',label)
adb('shell','wm','size','reset'); adb('shell','wm','density','reset')
adb('shell','settings','put','system','font_scale','1.0')
if failures:
    raise RuntimeError('Production validation failed: ' + ', '.join(failures))
