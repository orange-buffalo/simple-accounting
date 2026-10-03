import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, readFile, writeFile } from 'node:fs/promises';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import path from 'node:path';
import { Controller, hash, knowledge, snapshot, validateFindings, validateBlindReport, reviewerScopes } from '../../.opencode/plugins/sa-review/core.js';

const exec = promisify(execFile);
const report = { findings: [], coverage: ['workspace isolation'], gaps: [] };
const finding = { id: 'slurm', severity: 'medium', file: 'AGENTS.md', line: 1, evidence: 'Original requirement missing', remedy: 'Add required guidance' };
const answer = (value, cost = 0) => ({ text: JSON.stringify(value), cost, sessionId: 'ses_fry' });
const reviewers = ['functional', 'security', 'code', 'ux'];
const reviewerModel = { providerID: 'openai', id: 'gpt-5.6-terra', variant: 'medium' };

async function fixture(t, overrides = {}) {
  await mkdir('/tmp/opencode', { recursive: true });
  const root = await mkdtemp('/tmp/opencode/sa-controller-');
  const git = (...args) => exec('git', args, { cwd: root });
  await git('init', '--quiet');
  await git('config', 'user.name', 'Fry');
  await git('config', 'user.email', 'fry@planet.express');
  await mkdir(path.join(root, '.harness'));
  await writeFile(path.join(root, '.gitignore'), '.harness/runtime/\n.harness/vendor/\n');
  await writeFile(path.join(root, 'AGENTS.md'), 'Planet Express\n');
  const policy = { version: 1, maxFixCycles: 2, maxElapsedMinutes: 60, maxCostUsd: 20, maxKnowledgeChars: 30000,
    reviewers, reviewerModel, models: {}, ...overrides };
  await writeFile(path.join(root, '.harness/policy.json'), JSON.stringify(policy));
  await writeFile(path.join(root, '.harness/knowledge.json'), JSON.stringify({ version: 1, lessons: [] }));
  await git('add', '.');
  await git('commit', '-qm', 'feat: start Planet Express');
  const base = (await git('rev-parse', 'HEAD')).stdout.trim();
  await writeFile(path.join(root, 'AGENTS.md'), 'Planet Express review\n');
  const input = { base, requirements: 'Keep Planet Express deliveries secure', profile: 'harness' };
  return { root, input, policy };
}

test('passes only after every configured reviewer without running validation', async (t) => {
  const { root, input } = await fixture(t);
  const roles = [];
  const controller = new Controller(root, async ({ role, knowledge: bundle, model }) => {
    roles.push(role); assert.equal(bundle, '[]'); assert.deepEqual(model, reviewerModel); return answer(report);
  });
  controller.validate = () => { throw new Error('Review must not execute validation'); };
  const result = await controller.review(input);
  assert.equal(result.status, 'passed');
  assert.deepEqual(roles, reviewers);
  assert.equal(result.fingerprint, (await snapshot(root, input.base)).fingerprint);
});

test('reviewers receive focused source indexes without logs or duplicated full snapshots', async (t) => {
  const { root, input } = await fixture(t);
  const untrackedSource = 'Robot oil\n'.repeat(10000);
  await writeFile(path.join(root, '.harness/slurm.txt'), untrackedSource);
  await writeFile(path.join(root, 'gradlew'), '#!/bin/sh\nexit 1\n');
  await exec('git', ['add', 'gradlew', '.harness/slurm.txt'], { cwd: root });
  await exec('git', ['commit', '-qm', 'tests: add failing validation'], { cwd: root });
  const base = (await exec('git', ['rev-parse', 'HEAD'], { cwd: root })).stdout.trim();
  await writeFile(path.join(root, '.harness/slurm.txt'), `${untrackedSource}Slurm supplies\n`);
  const controller = new Controller(root, async ({ prompt }) => {
    assert.ok(prompt.length < 10000);
    assert.match(prompt, /Changed-source index/);
    assert.doesNotMatch(prompt, /gradle-1\.log|Controller validation evidence|Robot oil/);
    return answer(report);
  });
  const result = await controller.review({ ...input, base });
  assert.equal(result.status, 'passed');
  const state = JSON.parse(await readFile(path.join(result.evidence, 'state.json')));
  const source = state.rounds[0].sources.find(({ file }) => file === '.harness/slurm.txt');
  assert.match(await readFile(source.artifact, 'utf8'), /\+Slurm supplies/);
  assert.equal(state.rounds[0].validation, undefined);
});

test('targeted reviews use selected classes as source hints and freeze the selection', async (t) => {
  const { root, input } = await fixture(t);
  const testClasses = ['io.orangebuffalo.simpleaccounting.business.api.analytics.WorkspaceAnalyticsQueryTest'];
  const controller = new Controller(root, async ({ prompt }) => {
    assert.match(prompt, /Suggested test source classes/);
    assert.ok(prompt.includes(testClasses[0]));
    return answer(report);
  });
  assert.equal((await controller.review({ ...input, profile: 'targeted', testClasses })).status, 'passed');
  const missing = await controller.review({ ...input, profile: 'targeted' });
  assert.match(missing.error, /requires backend test classes/);
  const invalid = await controller.review({ ...input, profile: 'targeted', testClasses: ['--all'] });
  assert.match(invalid.error, /Invalid test classes/);
});

test('aggregate diffs above the command buffer limit do not block file-backed review', async (t) => {
  const { root, input } = await fixture(t);
  const files = Array.from({ length: 24 }, (_, index) => `.harness/generated-${index}.txt`);
  const original = 'Planet Express Slurm deliveries\n'.repeat(6500);
  const updated = 'Planet Express robot deliveries\n'.repeat(6500);
  for (const file of files) await writeFile(path.join(root, file), original);
  await exec('git', ['add', '.harness'], { cwd: root });
  await exec('git', ['commit', '-qm', 'tests: add generated delivery schedules'], { cwd: root });
  const base = (await exec('git', ['rev-parse', 'HEAD'], { cwd: root })).stdout.trim();
  for (const file of files) await writeFile(path.join(root, file), updated);
  const before = await snapshot(root, base);
  assert.ok(Buffer.byteLength(before.diff) > 8 * 1024 * 1024);
  const controller = new Controller(root, async ({ prompt }) => {
    assert.ok(prompt.length < 10000);
    assert.doesNotMatch(prompt, /Planet Express robot deliveries/);
    return answer(report);
  });
  const result = await controller.review({ ...input, base });
  assert.equal(result.status, 'passed');
  assert.equal(result.fingerprint, before.fingerprint);
  const state = JSON.parse(await readFile(path.join(result.evidence, 'state.json')));
  for (const file of files) {
    const source = state.rounds[0].sources.find((entry) => entry.file === file);
    assert.match(await readFile(source.artifact, 'utf8'), /\+Planet Express robot deliveries/);
    assert.equal(state.rounds[0].snapshot.contents[file], updated);
  }
  await writeFile(path.join(root, files[0]), `${updated}New Slurm route\n`);
  assert.notEqual((await snapshot(root, base)).fingerprint, result.fingerprint);
});

test('large generated-source snapshots remain file-backed without expanding reviewer prompts', async (t) => {
  const { root, input } = await fixture(t);
  const files = ['graphql.ts', 'gql.ts', 'schema-types.ts', 'schema.graphqls'];
  const generated = 'Slurm delivery types\n'.repeat(9000);
  const directory = path.join(root, 'frontend/generated');
  await mkdir(directory, { recursive: true });
  for (const file of files) await writeFile(path.join(directory, file), generated);
  await exec('git', ['add', 'frontend/generated'], { cwd: root });
  await exec('git', ['commit', '-qm', 'tests: add generated delivery types'], { cwd: root });
  const base = (await exec('git', ['rev-parse', 'HEAD'], { cwd: root })).stdout.trim();
  for (const file of files) await writeFile(path.join(directory, file), `${generated}New Slurm delivery\n`);
  const before = await snapshot(root, base);
  assert.ok(JSON.stringify(before).length > 600000);
  const roles = [];
  const controller = new Controller(root, async ({ role, prompt }) => {
    roles.push(role);
    assert.ok(prompt.length < 10000);
    assert.doesNotMatch(prompt, /Slurm delivery types/);
    return answer(report);
  });
  const result = await controller.review({ ...input, base, profile: 'full' });
  assert.equal(result.status, 'passed');
  assert.deepEqual(roles, reviewers);
  assert.equal(result.fingerprint, before.fingerprint);
  const state = JSON.parse(await readFile(path.join(result.evidence, 'state.json')));
  for (const file of files) {
    const source = state.rounds[0].sources.find((entry) => entry.file === `frontend/generated/${file}`);
    const packet = await readFile(source.artifact, 'utf8');
    assert.match(packet, /\+New Slurm delivery/);
    assert.ok(packet.length < 1000);
    assert.equal(state.rounds[0].snapshot.contents[source.file], `${generated}New Slurm delivery\n`);
  }
  await writeFile(path.join(directory, files[0]), `${generated}Old Slurm delivery\n`);
  assert.notEqual((await snapshot(root, base)).fingerprint, result.fingerprint);
});

test('validates submitted findings, repairs and re-reviews every exclusive scope', async (t) => {
  const { root, input } = await fixture(t);
  let fixed = false;
  const roles = [];
  const controller = new Controller(root, async ({ role, fixer, model, artifactRoots }) => {
    roles.push(role);
    assert.deepEqual(model, fixer ? undefined : reviewerModel);
    assert.equal(artifactRoots.length, 1);
    assert.match(artifactRoots[0], /\/\.harness\/runtime\/review-[a-z0-9-]+\/sources$/);
    if (fixer) { fixed = true; await mkdir(path.join(root, 'frontend/src'), { recursive: true }); await writeFile(path.join(root, 'frontend/src/slurm.ts'), 'Secure Slurm deliveries\n'); return { text: 'fixed', cost: 0 }; }
    if (role === 'finding-validator') return answer({ accepted: [{ ...finding, id: 'functional-1' }], rejected: [], gaps: [] });
    return answer({ ...report, findings: fixed || role !== 'functional' ? [] : [finding] });
  });
  const result = await controller.review({ ...input, profile: 'full' });
  assert.equal(result.status, 'passed'); assert.equal(result.fixes, 1);
  assert.deepEqual(roles, [...reviewers, 'finding-validator', 'fixer', ...reviewers]);
});

test('repair cap cannot be exceeded', async (t) => {
  const { root, input } = await fixture(t, { maxFixCycles: 0 });
  const controller = new Controller(root, async ({ role }) => role === 'finding-validator'
    ? answer({ accepted: [{ ...finding, id: 'functional-1' }], rejected: [{ id: 'security-1', reason: 'Duplicate contract' }], gaps: [] })
    : answer({ ...report, findings: ['functional', 'security'].includes(role) ? [finding] : [] }));
  const result = await controller.review(input);
  assert.equal(result.status, 'needs-human'); assert.equal(result.fixes, 0);
});

test('failed reviewer blocks, records all concurrent costs, and resumes the same budget', async (t) => {
  const { root, input } = await fixture(t);
  let calls = 0;
  let failed = true;
  const controller = new Controller(root, async ({ role }) => { calls++; return { ...answer(report, 0.25), failed: failed && role === 'functional' }; });
  const blocked = await controller.review(input);
  assert.equal(blocked.status, 'blocked'); assert.equal(calls, 4); assert.equal(blocked.cost, 1);
  failed = false;
  const resumed = await controller.review({ ...input, runId: blocked.runId });
  assert.equal(resumed.status, 'passed'); assert.equal(resumed.runId, blocked.runId);
  assert.equal(resumed.cost, 2);
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
    });
    assert.equal((await controller.review(input)).status, 'blocked');
  }
});

test('cost budget and expired elapsed budget block, including resume', async (t) => {
  const { root, input } = await fixture(t, { maxCostUsd: 1 });
  const controller = new Controller(root, async () => answer(report, 2));
  const result = await controller.review(input);
  assert.equal(result.status, 'blocked'); assert.equal(result.cost, 8);
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

test('snapshot still refuses oversized, binary and non-regular individual files', async (t) => {
  for (const kind of ['oversized', 'binary', 'directory']) {
    const { root, input } = await fixture(t);
    if (kind === 'directory') {
      await exec('git', ['rm', '-f', 'AGENTS.md'], { cwd: root });
      await mkdir(path.join(root, 'AGENTS.md'));
    } else {
      await writeFile(path.join(root, 'AGENTS.md'), kind === 'binary' ? Buffer.from([0]) : 'S'.repeat(256001));
    }
    await assert.rejects(snapshot(root, input.base), kind === 'binary' ? /Binary review file/ : /Unsupported review file/);
  }
});

test('harness profile refuses application changes', async (t) => {
  const { root, input } = await fixture(t);
  await mkdir(path.join(root, 'frontend')); await writeFile(path.join(root, 'frontend/slurm.ts'), 'export {};');
  const controller = new Controller(root, async () => answer(report));
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
  const controller = new Controller(root, async () => answer(report));
  await assert.rejects(controller.review({ ...input, runId: '../../slurm' }), /artifact ID/);
});

test('verified learning promotes only after baseline miss and successful candidate/clean/held-out', async (t) => {
  const { root, input } = await fixture(t);
  const controller = new Controller(root, async () => answer(report));
  const original = await controller.review(input);
  const bundles = [];
  controller.agent = async ({ role, knowledge: bundle, prompt, model, artifactRoots }) => {
    assert.deepEqual(model, reviewerModel);
    if (['feedback-analysis', 'learning-validator'].includes(role)) {
      assert.equal(artifactRoots.length, 1);
      assert.match(artifactRoots[0], /\/\.harness\/runtime\/lesson-[a-z0-9-]+\/original-evidence$/);
    } else assert.deepEqual(artifactRoots, []);
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
  for (const role of ['feedback-analysis', 'learning-validator', 'blind-baseline', 'blind-candidate', 'evaluation-judge']) {
    assert.ok(bundles.some((entry) => entry.role === role), `Configured model verified for ${role}`);
  }
});

test('unsupported learning remains candidate and never changes knowledge', async (t) => {
  const { root, input } = await fixture(t);
  const controller = new Controller(root, async () => answer(report));
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
  ] }] }));
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
  const controller = new Controller(root, async () => answer(report));
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
  const controller = new Controller(root, async () => answer(report));
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
  const controller = new Controller(root, async ({ role }) => {
    if (role === 'functional') await new Promise((resolve) => { release = resolve; });
    return answer(report);
  });
  const first = controller.review(input);
  while (!release) await new Promise((resolve) => setImmediate(resolve));
  await assert.rejects(controller.review(input), { code: 'EEXIST' });
  release(); assert.equal((await first).status, 'passed');
});

test('protected infrastructure drift blocks before post-fix review', async (t) => {
  for (const target of ['gradlew', 'buildSrc/Slurm.kt', 'frontend/package.json', 'frontend/build-config/slurm.js']) {
    const { root, input } = await fixture(t);
    const roles = [];
    const controller = new Controller(root, async ({ role, fixer }) => {
      roles.push(role);
      if (fixer) {
        await mkdir(path.dirname(path.join(root, target)), { recursive: true });
        await writeFile(path.join(root, target), 'Bender altered validation');
        return { text: 'fixed', cost: 0 };
      }
      return role === 'finding-validator' ? answer({ accepted: [{ ...finding, id: 'functional-1' }], rejected: [], gaps: [] })
        : answer({ ...report, findings: role === 'functional' ? [finding] : [] });
    });
    const result = await controller.review({ ...input, profile: 'full' });
    assert.equal(result.status, 'blocked'); assert.match(result.error, /protected infrastructure/);
    assert.deepEqual(roles, [...reviewers, 'finding-validator', 'fixer']);
  }
});

test('reviewers start concurrently, persist deterministic artifacts and account for every cost', async (t) => {
  const { root, input } = await fixture(t);
  const started = [];
  const releases = new Map();
  let allStarted;
  const barrier = new Promise((resolve) => { allStarted = resolve; });
  const controller = new Controller(root, async ({ role }) => {
    started.push(role);
    const pending = new Promise((resolve) => releases.set(role, resolve));
    if (started.length === reviewers.length) allStarted();
    await pending;
    return answer(report, 0.25);
  });
  const running = controller.review(input);
  await barrier;
  assert.deepEqual(started, reviewers);
  for (const role of [...reviewers].reverse()) releases.get(role)();
  const result = await running;
  assert.equal(result.status, 'passed'); assert.equal(result.cost, 1);
  const state = JSON.parse(await readFile(path.join(result.evidence, 'state.json')));
  assert.equal(state.pendingAgents, undefined);
  assert.deepEqual(state.artifacts.map(({ role }) => role), reviewers);
  for (const [index, role] of reviewers.entries()) {
    assert.equal(JSON.parse(await readFile(path.join(result.evidence, `agent-${index + 1}.json`))).role, role);
  }
});

test('review prompts give each reviewer only its exclusive responsibility', async (t) => {
  const { root, input } = await fixture(t);
  const controller = new Controller(root, async ({ role, prompt }) => {
    assert.ok(prompt.includes(reviewerScopes[role]));
    for (const other of reviewers.filter((axis) => axis !== role)) assert.ok(!prompt.includes(reviewerScopes[other]));
    assert.match(prompt, /Never inspect build\/test results/);
    assert.match(prompt, /not-applicable areas in coverage, NEVER in gaps/);
    assert.match(prompt, /other reviewers' responsibilities are not gaps/);
    return answer(report);
  });
  assert.equal((await controller.review(input)).status, 'passed');
});

test('explicit role model overrides never inherit the calling model', async (t) => {
  const override = { providerID: 'openai', id: 'gpt-6.1-sol', variant: 'low' };
  const { root, input } = await fixture(t, { models: { security: override } });
  const controller = new Controller(root, async ({ role, model }) => {
    assert.deepEqual(model, role === 'security' ? override : reviewerModel);
    return answer(report);
  });
  assert.equal((await controller.review({ ...input, defaultModel: override })).status, 'passed');
});

test('unknown, repeated or missing reviewers and absent configured models fail closed', async (t) => {
  for (const overrides of [{ reviewers: ['functional', 'security'] }, { reviewers: [...reviewers, 'quality'] },
    { reviewers: [...reviewers, 'code'] }, { reviewerModel: undefined }, { reviewerModel: { id: 'gpt-5.6-terra' } }]) {
    const { root, input } = await fixture(t, overrides);
    const controller = new Controller(root, async () => { throw new Error('Invalid policy must not launch workers'); });
    await assert.rejects(controller.review(input), /Invalid controller policy|Invalid worker model/);
  }
});

test('unaccounted concurrent worker failure preserves completed artifacts and blocks resume', async (t) => {
  const { root, input } = await fixture(t);
  const controller = new Controller(root, async ({ role }) => {
    if (role === 'security') throw new Error('Unknown Leela session cost');
    return answer(report, 0.25);
  });
  const result = await controller.review(input);
  assert.equal(result.status, 'blocked'); assert.equal(result.cost, 0.75);
  const state = JSON.parse(await readFile(path.join(result.evidence, 'state.json')));
  assert.deepEqual(state.pendingAgents, ['security']); assert.equal(state.artifacts.length, 4);
  await assert.rejects(controller.review({ ...input, runId: result.runId }), /Unreconciled interrupted agent/);
});

test('delivery requires explicit primary-agent validation after controller repairs before handover', async () => {
  const skill = await readFile(new URL('../../.agents/skills/sa-delivery/SKILL.md', import.meta.url), 'utf8');
  const postReview = skill.indexOf('4. After `sa_review` returns');
  const handover = skill.indexOf('5. Hand over');
  assert.ok(postReview > skill.indexOf('3. Call `sa_review`') && handover > postReview);
  const step = skill.slice(postReview, handover);
  assert.match(step, /fixes > 0/);
  assert.match(step, /primary implementation agent must rerun the selected Gradle validation and check its results before handover/);
  assert.match(step, /rendering reports/);
});
