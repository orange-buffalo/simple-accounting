#!/usr/bin/env python3
"""Check local workflow prerequisites without changing credentials or configuration."""
import json
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parent.parent


def doctor():
    missing = []
    for command in ['node', 'python3', 'opencode', 'git', 'java', 'docker']:
        if shutil.which(command) is None:
            missing.append(command)
    if not missing:
        version = subprocess.check_output(['opencode', '--version'], text=True).strip()
        if not version.startswith('opencode v2.'):
            missing.append('OpenCode V2')
        major = int(subprocess.check_output(['node', '--version'], text=True).strip().lstrip('v').split('.')[0])
        if major < 22:
            missing.append('Node.js 22+')
    expected = json.loads((ROOT / '.harness/upstream.json').read_text())
    receipt = ROOT / '.harness/vendor/installed.json'
    if not receipt.exists() or json.loads(receipt.read_text()) != expected:
        missing.append('pinned CE skills: run ./gradlew bootstrapAgentHarness')
    for skill in expected['skills']:
        if not (ROOT / '.harness/vendor/skills' / skill / 'SKILL.md').is_file():
            missing.append(skill)
    if missing:
        raise SystemExit('Missing: ' + ', '.join(missing))
    print('Local prerequisites present. Model authentication, Docker daemon, Java 25, and effective OpenCode permissions must also be checked before application validation.')


if __name__ == '__main__':
    doctor()
