"""Verify the actual InternalApp on a disposable CI emulator, no API required."""
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

output = Path('evidence/navigation')
output.mkdir(parents=True, exist_ok=True)


def adb(*args):
    return subprocess.run(['adb', *args], check=True, capture_output=True, text=True, timeout=240).stdout


def scenario(width, font, mode):
    label = f'{width}dp-font{font}-{mode}'
    adb('shell', 'wm', 'size', f'{width * 3}x{round(width * 3 * 20 / 9)}')
    adb('shell', 'wm', 'density', '480')
    adb('shell', 'settings', 'put', 'system', 'font_scale', str(font / 100))
    adb('shell', 'am', 'force-stop', 'dev.mitin.app.internal')
    command = ['adb', 'shell', 'am', 'instrument', '-w', '-r',
               '-e', 'class', 'dev.mitin.internal.InternalNavigationTest',
               '-e', 'navCase', label, '-e', 'expectedWidthDp', str(width),
               '-e', 'expectedFontScale', str(font / 100),
               'dev.mitin.app.internal.test/androidx.test.runner.AndroidJUnitRunner']
    result = subprocess.run(command, capture_output=True, text=True, timeout=240)
    text = result.stdout + result.stderr
    (output / (label + '.txt')).write_text(text, encoding='utf-8')
    success = (result.returncode == 0 and re.search(r'OK \(1 test\)', text) is not None
               and 'FAILURES!!!' not in text
               and 'INSTRUMENTATION_STATUS: test=fullLabelsTouchTargetsInsetsAndSystemBack' in text)
    suite = ET.Element('testsuite', name=label, tests='1', failures='0' if success else '1', errors='0', skipped='0')
    case = ET.SubElement(suite, 'testcase', classname='dev.mitin.internal.InternalNavigationTest',
                         name='fullLabelsTouchTargetsInsetsAndSystemBack')
    if not success:
        ET.SubElement(case, 'failure').text = 'See instrumentation output'
    ET.ElementTree(suite).write(output / (label + '.xml'), encoding='utf-8', xml_declaration=True)
    if not success:
        raise RuntimeError('Navigation verification failed: ' + label)
    print(label + ': PASS', flush=True)


try:
    adb('install', '--no-streaming', '-r', 'app/build/outputs/apk/internal/debug/app-internal-debug.apk')
    adb('install', '--no-streaming', '-r', 'app/build/outputs/apk/androidTest/internal/debug/app-internal-debug-androidTest.apk')
    adb('shell', 'cmd', 'overlay', 'enable-exclusive', '--category', 'com.android.internal.systemui.navbar.gestural')
    for width in (320, 360, 412):
        for font in (100, 160):
            scenario(width, font, 'gesture')
    adb('shell', 'cmd', 'overlay', 'enable-exclusive', '--category', 'com.android.internal.systemui.navbar.threebutton')
    scenario(320, 160, 'threebutton')
finally:
    subprocess.run(['adb', 'pull', '/sdcard/Download/mitin-navigation', str(output / 'screenshots')], timeout=60)
    adb('shell', 'wm', 'size', 'reset')
    adb('shell', 'wm', 'density', 'reset')
    adb('shell', 'settings', 'put', 'system', 'font_scale', '1.0')
    adb('shell', 'cmd', 'overlay', 'enable-exclusive', '--category', 'com.android.internal.systemui.navbar.gestural')
