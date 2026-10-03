#!/usr/bin/env python3
"""Install a checksum-pinned, data-only subset of Compound Engineering skills."""
import hashlib
import io
import json
from pathlib import Path
import tarfile
import urllib.request

ROOT = Path(__file__).resolve().parent.parent


def install():
    lock = json.loads((ROOT / '.harness/upstream.json').read_text())
    url = f"https://codeload.github.com/{lock['repository']}/tar.gz/{lock['revision']}"
    data = urllib.request.urlopen(url, timeout=120).read()
    if hashlib.sha256(data).hexdigest() != lock['sha256']:
        raise RuntimeError('Upstream checksum mismatch')
    destination = ROOT / '.harness/vendor'
    destination.mkdir(parents=True, exist_ok=True)
    prefix = f"compound-engineering-plugin-{lock['revision']}/"
    with tarfile.open(fileobj=io.BytesIO(data), mode='r:gz') as archive:
        for member in archive.getmembers():
            relative = member.name.removeprefix(prefix)
            if member.name == prefix + 'LICENSE':
                target = destination / 'LICENSE'
            else:
                parts = Path(relative).parts
                if len(parts) < 3 or parts[0] != 'skills' or parts[1] not in lock['skills']:
                    continue
                if '..' in parts or Path(relative).is_absolute():
                    raise RuntimeError('Unsafe archive path')
                target = destination / Path(*parts)
            if not member.isfile():
                continue
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(archive.extractfile(member).read())
    for skill in lock['skills']:
        if not (destination / 'skills' / skill / 'SKILL.md').is_file():
            raise RuntimeError(f'Missing upstream skill: {skill}')
    (destination / 'installed.json').write_text(json.dumps(lock, indent=2) + '\n')
    print('Installed pinned CE skills; reload the OpenCode project location.')


if __name__ == '__main__':
    install()
