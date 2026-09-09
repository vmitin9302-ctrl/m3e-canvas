"""Read-only production UI matrix. No account, email or AI submission actions."""
import argparse
import json
from pathlib import Path
import subprocess
import time

parser=argparse.ArgumentParser()
parser.add_argument('--adb',default='adb')
parser.add_argument('--serial')
parser.add_argument('--output',type=Path,default=Path('evidence/auth-entry'))
args=parser.parse_args()
args.output.mkdir(parents=True,exist_ok=True)
adb=[args.adb]+(['-s',args.serial] if args.serial else [])
def call(*command,timeout=90):
    return subprocess.run(adb+list(command),capture_output=True,text=True,encoding='utf-8',errors='replace',timeout=timeout,check=True).stdout
def shell(*command): return call('shell',*command)
def wait_network():
    deadline=time.monotonic()+60
    while time.monotonic()<deadline:
        if any('WIFI CONNECTED' in line and 'IS_VALIDATED' in line for line in shell('dumpsys','connectivity').splitlines()):return
        time.sleep(0.25)
    raise RuntimeError('Emulator Wi-Fi did not become validated')
def phase(case,cls,count):
    shell('am','force-stop','dev.mitin.app')
    output=call('shell','am','instrument','-w','-r','-e','class','dev.mitin.internal.'+cls,'-e','authCase',case,'dev.mitin.app.test/androidx.test.runner.AndroidJUnitRunner',timeout=480)
    (args.output/(case+'-'+cls+'.txt')).write_text(output,encoding='utf-8')
    return f'OK ({count} test'+('s' if count!=1 else '')+')' in output
results=[]
try:
    for width in (320,360,412):
        for font in (1.0,1.6):
            case=f'{width}-{round(font*100)}'
            shell('wm','density','320');shell('wm','size',f'{width*2}x1440')
            shell('settings','put','system','font_scale',str(font))
            shell('cmd','connectivity','airplane-mode','disable');shell('svc','wifi','enable');shell('svc','data','enable')
            wait_network()
            shell('pm','clear','dev.mitin.app')
            ui=phase(case,'AuthEntryPresentationTest',3)
            call('pull','/sdcard/Android/data/dev.mitin.app/files/auth-entry',str(args.output/(case+'-screenshots')))
            shell('cmd','connectivity','airplane-mode','enable');shell('svc','wifi','disable');shell('svc','data','disable')
            offline=phase(case,'ProductionOfflineTest',1)
            results.append(dict(case=case,ui='PASS' if ui else 'FAIL',offline='PASS' if offline else 'FAIL'))
            (args.output/'matrix.json').write_text(json.dumps(results,indent=2),encoding='utf-8')
            print(json.dumps(results[-1]),flush=True)
finally:
    shell('cmd','connectivity','airplane-mode','disable');shell('svc','wifi','enable');shell('svc','data','enable')
    shell('settings','put','system','font_scale','1.0')
if len(results)!=6 or any(row['ui']!='PASS' or row['offline']!='PASS' for row in results):
    raise SystemExit('Auth entry matrix did not pass all six configurations')
