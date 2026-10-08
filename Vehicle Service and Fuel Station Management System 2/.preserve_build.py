import json, subprocess, shutil
from pathlib import Path

root=Path.cwd()
saved=root/'.build_baseline'
if not saved.exists():
    saved.mkdir()
    tracked=subprocess.check_output(['git','ls-files','backend/target'],text=True).splitlines()
    reports=[str(p.relative_to(root)).replace('\\','/') for p in (root/'backend/target/surefire-reports').glob('*')]
    manifest={'tracked':tracked,'reports':reports}
    (saved/'manifest.json').write_text(json.dumps(manifest))
    for name in set(tracked+reports):
        src=root/name
        if src.is_file():
            dest=saved/name
            dest.parent.mkdir(parents=True,exist_ok=True)
            shutil.copy2(src,dest)
