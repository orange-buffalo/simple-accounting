import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, readFile, writeFile } from 'node:fs/promises';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import path from 'node:path';
import { Controller, hash, knowledge, snapshot, validateFindings, validateBlindReport } from '../../.opencode/plugins/accounting-review/core.js';

const exec = promisify(execFile);
const report = { findings: [], coverage: ['workspace isolation'], gaps: [] };
const finding = { id: 'slurm', severity: 'medium', file: 'AGENTS.md', line: 1, evidence: 'Original requirement missing', remedy: 'Add required guidance' };
const answer = (value, cost = 0) => ({ text: JSON.stringify(value), cost, sessionId: 'ses_fry' });

async function fixture(t, overrides = {}) {
  await mkdir('/tmp/opencode', { recursive: true });
  const root = await mkdtemp('/tmp/opencode/accounting-controller-');
  const git = (...args) => exec('git', args, { cwd: root });
  await git('init', '--quiet');
  await git('config', 'user.name', 'Fry');
  await git('config', 'user.email', 'fry@planet.express');
  await mkdir(path.join(root, '.harness'));
  await writeFile(path.join(root, '.gitignore'), '.harness/runtime/\n.harness/vendor/\n');
  await writeFile(path.join(root, 'AGENTS.md'), 'Planet Express\n');
  const policy = { version: 1, maxFixCycles: 2, maxElapsedMinutes: 60, maxCostUsd: 20, maxKnowledgeChars: 30000,
    reviewers: ['requirements', 'security'], models: {}, validation: { harness: ['checkAgentHarness'], full: ['assemble', 'check'] }, ...overrides };
  await writeFile(path.join(root, '.harness/policy.json'), JSON.stringify(policy));
  await writeFile(path.join(root, '.harness/knowledge.json'), JSON.stringify({ version: 1, lessons: [] }));
  await git('add', '.');
  await git('commit', '-qm', 'feat: start Planet Express');
  const base = (await git('rev-parse', 'HEAD')).stdout.trim();
  await writeFile(path.join(root, 'AGENTS.md'), 'Planet Express review\n');
  const input = { base, requirements: 'Keep Planet Express deliveries secure', profile: 'harness' };
  return { root, input, policy };
}

test('passes only after every required reviewer and validation', async (t) => {
  const { root, input } = await fixture(t);
  const roles = [];
  const controller = new Controller(root, async ({ role, knowledge: bundle }) => {
    roles.push(role); assert.equal(bundle, '[]'); return answer(report);
  }, async (_, tasks) => { assert.deepEqual(tasks, ['checkAgentHarness']); return { code: 0 }; });
  const result = await controller.review(input);
  assert.equal(result.status, 'passed');
  assert.deepEqual(roles, ['requirements', 'security']);
  assert.equal(result.fingerprint, (await snapshot(root, input.base)).fingerprint);
});

test('validates findings, repairs, reruns validation and all axes', async (t) => {
  const { root, input } = await fixture(t);
  let fixed = false;
  let validations = 0;
  const roles = [];
  const controller = new Controller(root, async ({ role, fixer }) => {
    roles.push(role);
    if (fixer) { fixed = true; await mkdir(path.join(root, 'frontend/src'), { recursive: true }); await writeFile(path.join(root, 'frontend/src/slurm.ts'), 'Secure Slurm deliveries\n'); return { text: 'fixed', cost: 0 }; }
    if (role === 'finding-validator') return answer({ accepted: [{ ...finding, id: 'requirements-1' }], rejected: [], gaps: [] });
    return answer({ ...report, findings: fixed || role !== 'requirements' ? [] : [finding] });
  }, async () => { validations++; return { code: 0 }; });
  const result = await controller.review({ ...input, profile: 'full' });
  assert.equal(result.status, 'passed'); assert.equal(result.fixes, 1); assert.equal(validations, 2);
  assert.deepEqual(roles, ['requirements', 'security', 'finding-validator', 'fixer', 'requirements', 'security']);
});

test('repair cap cannot be exceeded', async (t) => {
  const { root, input } = await fixture(t, { maxFixCycles: 0 });
  const controller = new Controller(root, async ({ role }) => role === 'finding-validator'
    ? answer({ accepted: [{ ...finding, id: 'requirements-1' }], rejected: [{ id: 'security-1', reason: 'Duplicate contract' }], gaps: [] }) : answer({ ...report, findings: [finding] }), async () => ({ code: 0 }));
  const result = await controller.review(input);
  assert.equal(result.status, 'needs-human'); assert.equal(result.fixes, 0);
});

test('validation failure blocks without calling models and resumes same budget', async (t) => {
  const { root, input } = await fixture(t);
  let calls = 0;
  const controller = new Controller(root, async () => { calls++; return answer(report); }, async () => ({ code: 1 }));
  const blocked = await controller.review(input);
  assert.equal(blocked.status, 'blocked'); assert.equal(calls, 0);
  controller.validate = async () => ({ code: 0 });
  const resumed = await controller.review({ ...input, runId: blocked.runId });
  assert.equal(resumed.status, 'passed'); assert.equal(resumed.runId, blocked.runId);
  assert.equal(resumed.error, undefined);
  assert.equal(JSON.parse(await readFile(path.join(resumed.evidence, 'state.json'))).error, undefined);
});

test('incomplete coverage, invalid output and drift are never passes', async (t) => {
  for (const behavior of ['gap', 'invalid', 'drift']) {
    const { root, input } = await fixture(t);
    const controller = new Controller(root, async () => {
      if (behavior === 'gap') return answer({ ...report, gaps: ['Missing rendering evidence'] });
      if (behavior === 'invalid') return { text: 'Looks fine!', cost: 0 };
      await writeFile(path.join(root, 'AGENTS.md'), 'Unexpected Bender edit\n'); return answer(report);
    }, async () => ({ code: 0 }));
    assert.equal((await controller.review(input)).status, 'blocked');
  }
});

test('cost budget and expired elapsed budget block, including resume', async (t) => {
  const { root, input } = await fixture(t, { maxCostUsd: 1 });
  const controller = new Controller(root, async () => answer(report, 2), async () => ({ code: 0 }));
  const result = await controller.review(input);
  assert.equal(result.status, 'blocked'); assert.equal(result.cost, 2);
  assert.match(result.error, /Cost budget/);
  const stateFile = path.join(result.evidence, 'state.json');
  const state = JSON.parse(await readFile(stateFile)); state.started = 0;
  await writeFile(stateFile, JSON.stringify(state));
  const resumed = await controller.review({ ...input, runId: result.runId });
  assert.match(resumed.error, /Elapsed-time budget/);
});

test('snapshot includes untracked files, refuses secrets and invalid base', async (t) => {
  const { root, input } = await fixture(t);
  await writeFile(path.join(root, 'slurm.txt'), 'Slurm cargo');
  assert.equal((await snapshot(root, input.base)).contents['slurm.txt'], 'Slurm cargo');
  await assert.rejects(snapshot(root, '--help'), /full commit SHA/);
  await writeFile(path.join(root, '.env'), 'SLURM_SECRET=secret');
  await assert.rejects(snapshot(root, input.base), /Secret-like/);
});

test('harness profile refuses application changes', async (t) => {
  const { root, input } = await fixture(t);
  await mkdir(path.join(root, 'frontend')); await writeFile(path.join(root, 'frontend/slurm.ts'), 'export {};');
  const controller = new Controller(root, async () => answer(report), async () => ({ code: 0 }));
  assert.match((await controller.review(input)).error, /Harness profile/);
});

test('knowledge integrity, promotion status and limits fail closed', async (t) => {
  const { root } = await fixture(t);
  await mkdir(path.join(root, '.harness/lessons'));
  const lesson = JSON.stringify({ status: 'verified', guidance: 'Check Slurm authorization', scope: 'deliveries', evidence: { replay: true } });
  await writeFile(path.join(root, '.harness/lessons/slurm.json'), lesson);
  await writeFile(path.join(root, '.harness/knowledge.json'), JSON.stringify({ version: 1, lessons: [{ id: 'slurm', sha256: hash(lesson) }] }));
  assert.match(await knowledge(root), /Slurm authorization/);
  await assert.rejects(knowledge(root, 1), /exceeds/);
  await writeFile(path.join(root, '.harness/lessons/slurm.json'), lesson + ' ');
  await assert.rejects(knowledge(root), /integrity/);
});

test('invalid findings and unsafe run IDs are rejected', async (t) => {
  for (const invalid of [null, 1, '', ' ']) assert.throws(() => validateFindings({ ...report, coverage: [invalid] }), /Incomplete/);
  assert.throws(() => validateFindings({ ...report, findings: [{ id: 'slurm' }] }), /Invalid finding/);
  const { root, input } = await fixture(t);
  const controller = new Controller(root, async () => answer(report), async () => ({ code: 0 }));
  await assert.rejects(controller.review({ ...input, runId: '../../slurm' }), /artifact ID/);
});

test('verified learning promotes only after baseline miss and successful candidate/clean/held-out', async (t) => {
  const { root, input } = await fixture(t);
  const controller = new Controller(root, async () => answer(report), async () => ({ code: 0 }));
  const original = await controller.review(input);
  const bundles = [];
  controller.agent = async ({ role, knowledge: bundle, prompt }) => {
    bundles.push({ role, bundle });
    if (role === 'feedback-analysis') return answer({ dispositions: [{ feedback: 'Slurm leak', classification: 'defect', evidence: 'source', hypothesis: 'coverage gap' }],
      scope: 'delivery access', guidance: 'Check delivery ownership', replay: { input: 'Fry defect', expected: 'leak' }, heldOut: { input: 'Leela defect', expected: 'leak' }, clean: { input: 'Bender correct', expected: 'no findings' } });
    if (role === 'learning-validator') return answer({ valid: true, evidence: ['source confirms leak'], issues: [] });
    if (role === 'evaluation-judge') return answer({ aCorrect: prompt.includes('Bender correct'), bCorrect: true, evidence: 'candidate catches leak without false positives' });
    return answer({ findings: role === 'blind-candidate' && !prompt.includes('Bender correct') ? [{ evidence: 'Delivery ownership leak' }] : [], gaps: [] });
  };
  const learned = await controller.learn({ runId: original.runId, feedback: 'Slurm leak' });
  assert.equal(learned.status, 'verified');
  assert.match(await knowledge(root), /delivery ownership/);
  assert.equal(bundles.filter((entry) => entry.role === 'blind-baseline').every((entry) => entry.bundle === '[]'), true);
  assert.equal(bundles.filter((entry) => entry.role === 'blind-candidate').every((entry) => entry.bundle.includes('delivery ownership')), true);
});

test('unsupported learning remains candidate and never changes knowledge', async (t) => {
  const { root, input } = await fixture(t);
  const controller = new Controller(root, async () => answer(report), async () => ({ code: 0 }));
  const original = await controller.review(input);
  controller.agent = async ({ role }) => role === 'learning-validator'
    ? answer({ valid: false, evidence: [], issues: ['new requirement'] }) : answer({ guidance: 'Invented preference' });
  const result = await controller.learn({ runId: original.runId, feedback: 'New preference' });
  assert.equal(result.status, 'candidate'); assert.equal(await knowledge(root), '[]');
});

test('learning investigation receives actual reviewer prompts and tool investigation', async (t) => {
  const { root, input } = await fixture(t);
  const controller = new Controller(root, async () => ({ ...answer(report), messages: [{ type: 'assistant', content: [
    { type: 'tool', name: 'read', state: { input: { path: 'slurm.kt' }, content: [{ type: 'text', text: 'Slurm ownership code' }] } },
  ] }] }), async () => ({ code: 0 }));
  const original = await controller.review(input);
  const roles = [];
  controller.agent = async ({ role, prompt }) => {
    assert.match(prompt, /Slurm ownership code/);
    assert.match(prompt, /agent-1-prompt\.txt/);
    roles.push(role);
    return role === 'feedback-analysis' ? answer({ guidance: 'Check ownership' })
      : answer({ valid: false, evidence: [], issues: ['Incomplete proposal'] });
  };
  assert.equal((await controller.learn({ runId: original.runId, feedback: 'Slurm leak' })).status, 'candidate');
  assert.deepEqual(roles, ['feedback-analysis', 'learning-validator']);
});

test('identical unsupported learning judgments cannot promote knowledge', async (t) => {
  const { root, input } = await fixture(t);
  const controller = new Controller(root, async () => answer(report), async () => ({ code: 0 }));
  const original = await controller.review(input);
  controller.agent = async ({ role }) => {
    if (role === 'feedback-analysis') return answer({ dispositions: [{ feedback: 'Slurm leak', classification: 'defect', evidence: 'source', hypothesis: 'coverage gap' }], scope: 'deliveries', guidance: 'Check ownership',
      replay: { input: 'Fry defect', expected: 'leak' }, heldOut: { input: 'Leela defect', expected: 'leak' }, clean: { input: 'Bender correct', expected: 'no findings' } });
    if (role === 'learning-validator') return answer({ valid: true, evidence: ['Source check'], issues: [] });
    if (role === 'evaluation-judge') return answer({ aCorrect: false, bCorrect: true, evidence: 'Invented improvement' });
    return answer({ findings: [], gaps: [] });
  };
  const result = await controller.learn({ runId: original.runId, feedback: 'Slurm leak' });
  assert.equal(result.status, 'candidate'); assert.match(result.error, /Contradictory/); assert.equal(await knowledge(root), '[]');
});

test('blind reports cannot hide missing coverage', () => {
  assert.throws(() => validateBlindReport({ findings: [], gaps: ['Missing source'] }), /Incomplete/);
  assert.throws(() => validateBlindReport({ findings: [{ evidence: '' }], gaps: [] }), /Incomplete/);
});

test('intervening cumulative knowledge is evaluated before promotion', async (t) => {
  const { root, input } = await fixture(t);
  const controller = new Controller(root, async () => answer(report), async () => ({ code: 0 }));
  const original = await controller.review(input);
  await mkdir(path.join(root, '.harness/lessons'));
  const lesson = JSON.stringify({ status: 'verified', scope: 'deliveries', guidance: 'Preserve Slurm cargo integrity', evidence: { verified: true } });
  await writeFile(path.join(root, '.harness/lessons/slurm.json'), lesson);
  await writeFile(path.join(root, '.harness/knowledge.json'), JSON.stringify({ version: 1, lessons: [{ id: 'slurm', sha256: hash(lesson) }] }));
  let cumulativeCalls = 0;
  controller.agent = async ({ role, prompt, knowledge: bundle }) => {
    if (role === 'feedback-analysis') return answer({ dispositions: [{ feedback: 'Slurm leak', classification: 'defect', evidence: 'source', hypothesis: 'coverage gap' }], scope: 'delivery access', guidance: 'Check ownership',
      replay: { input: 'Fry defect', expected: 'leak' }, heldOut: { input: 'Leela defect', expected: 'leak' }, clean: { input: 'Bender correct', expected: 'no findings' } });
    if (role === 'learning-validator') return answer({ valid: true, evidence: ['Source check'], issues: [] });
    if (role === 'blind-cumulative') { cumulativeCalls++; assert.match(bundle, /cargo integrity/); assert.match(bundle, /Check ownership/); }
    if (role === 'evaluation-judge') return answer({ aCorrect: prompt.includes('Bender correct'), bCorrect: true, correct: true, evidence: 'Verified defect/clean result' });
    return answer({ findings: role !== 'blind-baseline' && !prompt.includes('Bender correct') ? [{ evidence: 'Ownership leak' }] : [], gaps: [] });
  };
  assert.equal((await controller.learn({ runId: original.runId, feedback: 'Slurm leak' })).status, 'verified');
  assert.equal(cumulativeCalls, 3);
});

test('worktree lock serializes controller invocations', async (t) => {
  const { root, input } = await fixture(t);
  let release;
  const controller = new Controller(root, async () => answer(report), () => new Promise((resolve) => { release = resolve; }));
  const first = controller.review(input);
  while (!release) await new Promise((resolve) => setImmediate(resolve));
  await assert.rejects(controller.review(input), { code: 'EEXIST' });
  release({ code: 1 }); await first;
});

test('protected infrastructure drift blocks before executing post-fix validation', async (t) => {
  for (const target of ['gradlew', 'buildSrc/Slurm.kt', 'frontend/package.json', 'frontend/build-config/slurm.js']) {
    const { root, input } = await fixture(t);
    let validations = 0;
    const controller = new Controller(root, async ({ role, fixer }) => {
      if (fixer) {
        await mkdir(path.dirname(path.join(root, target)), { recursive: true });
        await writeFile(path.join(root, target), 'Bender altered validation');
        return { text: 'fixed', cost: 0 };
      }
      return role === 'finding-validator' ? answer({ accepted: [{ ...finding, id: 'requirements-1' }], rejected: [], gaps: [] })
        : answer({ ...report, findings: role === 'requirements' ? [finding] : [] });
    }, async () => { validations++; return { code: 0 }; });
    const result = await controller.review({ ...input, profile: 'full' });
    assert.equal(result.status, 'blocked'); assert.match(result.error, /protected infrastructure/); assert.equal(validations, 1);
  }
});
