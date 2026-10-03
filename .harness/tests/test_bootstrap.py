import hashlib
import importlib.util
import io
import json
from pathlib import Path
import tarfile
import tempfile
import unittest
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location('bootstrap', Path(__file__).parent.parent / 'bootstrap.py')
BOOTSTRAP = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(BOOTSTRAP)


def archive(entries):
    data = io.BytesIO()
    with tarfile.open(fileobj=data, mode='w:gz') as output:
        for name, content in entries.items():
            member = tarfile.TarInfo('compound-engineering-plugin-fry/' + name)
            member.size = len(content)
            output.addfile(member, io.BytesIO(content))
    return data.getvalue()


class BootstrapTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        Path('/tmp/opencode').mkdir(parents=True, exist_ok=True)

    def install(self, data, digest=None):
        with tempfile.TemporaryDirectory(prefix='accounting-bootstrap-', dir='/tmp/opencode') as directory:
            root = Path(directory)
            (root / '.harness').mkdir()
            (root / '.harness/upstream.json').write_text(json.dumps({
                'repository': 'EveryInc/compound-engineering-plugin', 'revision': 'fry',
                'sha256': digest or hashlib.sha256(data).hexdigest(), 'skills': ['ce-code-review'],
            }))
            with patch.object(BOOTSTRAP, 'ROOT', root), patch.object(BOOTSTRAP.urllib.request, 'urlopen', return_value=io.BytesIO(data)):
                BOOTSTRAP.install()
            return {str(file.relative_to(root)): file.read_bytes() for file in root.rglob('*') if file.is_file()}

    def test_selected_skills_references_and_license_are_installed(self):
        files = self.install(archive({
            'LICENSE': b'MIT', 'skills/ce-code-review/SKILL.md': b'Slurm review',
            'skills/ce-code-review/references/Slurm.md': b'Ownership', 'skills/unselected/SKILL.md': b'Not selected',
        }))
        self.assertEqual(files['.harness/vendor/skills/ce-code-review/SKILL.md'], b'Slurm review')
        self.assertEqual(files['.harness/vendor/LICENSE'], b'MIT')
        self.assertNotIn('.harness/vendor/skills/unselected/SKILL.md', files)

    def test_checksum_mismatch_is_rejected(self):
        with self.assertRaisesRegex(RuntimeError, 'checksum mismatch'):
            self.install(archive({'LICENSE': b'MIT'}), digest='incorrect')

    def test_unsafe_archive_path_is_rejected(self):
        with self.assertRaisesRegex(RuntimeError, 'Unsafe archive path'):
            self.install(archive({'skills/ce-code-review/../../../Slurm': b'Robot oil'}))

    def test_missing_selected_skill_is_rejected(self):
        with self.assertRaisesRegex(RuntimeError, 'Missing upstream skill'):
            self.install(archive({'LICENSE': b'MIT'}))
