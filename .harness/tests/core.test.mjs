import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, readFile, writeFile, stat, chmod } from 'node:fs/promises';
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

test('review stops after two rounds even with a larger legacy repair budget or resume', async (t) => {
  const { root, input } = await fixture(t, { maxFixCycles: 5 });
  let fixes = 0;
  const controller = new Controller(root, async ({ role, fixer }) => {
    if (fixer) {
      fixes++;
      await mkdir(path.join(root, 'frontend'), { recursive: true });
      await writeFile(path.join(root, 'frontend/slurm.ts'), `Slurm ${fixes}`);
      return { text: 'fixed', cost: 0 };
    }
    if (role === 'finding-validator') return answer({ accepted: [{ ...finding, id: 'functional-1' }], rejected: [], gaps: [] });
    return answer({ ...report, findings: role === 'functional' ? [finding] : [] });
  });
  const result = await controller.review({ ...input, profile: 'full' });
  assert.equal(result.status, 'review-exhausted');
  assert.equal(result.reviewRoundsExhausted, true);
  assert.equal(result.nextAction, 'finish-implementation-and-validation');
  assert.equal(fixes, 1);
  const state = JSON.parse(await readFile(path.join(result.evidence, 'state.json'), 'utf8'));
  assert.equal(state.rounds.length, 2);
  const resumed = await controller.review({ ...input, profile: 'full', runId: result.runId });
  assert.equal(resumed.status, 'review-exhausted');
  assert.match(resumed.error, /Two-round/);
  assert.match(resumed.error, /finish implementation and validation/);
  assert.equal(resumed.nextAction, 'finish-implementation-and-validation');
  assert.equal(fixes, 1);
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

test('new human feedback renews exhausted budgets without erasing usage or review rounds', async (t) => {
  for (const exhausted of ['elapsed', 'cost', 'both']) {
    const { root, input } = await fixture(t, { maxCostUsd: 1 });
    let calls = 0;
    const controller = new Controller(root, async () => {
      calls++;
      return answer({ ...report, gaps: ['Await Fry feedback'] }, 0.1);
    });
    const first = await controller.review(input);
    const stateFile = path.join(first.evidence, 'state.json');
    const state = JSON.parse(await readFile(stateFile));
    if (exhausted !== 'cost') state.started = 0;
    if (exhausted !== 'elapsed') state.cost = 2;
    await writeFile(stateFile, JSON.stringify(state));
    const feedback = 'Fry requests representative country assertions, not full provider lists';
    const resumed = await controller.review({ ...input, runId: first.runId, clarification: feedback });
    assert.match(resumed.error, /Incomplete review/);
    assert.equal(calls, 8);
    const renewed = JSON.parse(await readFile(stateFile));
    assert.equal(renewed.started, state.started);
    assert.equal(renewed.rounds.length, 2);
    assert.equal(renewed.budgetHistory.length, 1);
    assert.equal(renewed.budgetHistory[0].cost, state.cost);
    assert.equal(renewed.budgetCostBaseline, state.cost);
    assert.ok(Math.abs(renewed.cost - state.cost - 0.4) < 0.000001);
    const capped = await controller.review({ ...input, runId: first.runId,
      clarification: 'Leela additionally requests component-owned full-stack coverage' });
    assert.equal(capped.status, 'review-exhausted');
    assert.equal(capped.nextAction, 'finish-implementation-and-validation');
    assert.match(capped.error, /Two-round/);
    assert.equal(calls, 8);
  }
});

test('repeated feedback or ordinary resume cannot renew expired budgets', async (t) => {
  const { root, input } = await fixture(t);
  let calls = 0;
  const controller = new Controller(root, async () => {
    calls++;
    return answer({ ...report, gaps: ['Await Fry feedback'] });
  });
  const feedback = 'Fry requests representative countries';
  const result = await controller.review({ ...input, clarification: feedback });
  const stateFile = path.join(result.evidence, 'state.json');
  const state = JSON.parse(await readFile(stateFile));
  state.started = 0;
  await writeFile(stateFile, JSON.stringify(state));
  for (const clarification of [undefined, ` ${feedback} `]) {
    const resumed = await controller.review({ ...input, runId: result.runId, clarification });
    assert.match(resumed.error, /Elapsed-time budget/);
    assert.equal(calls, 4);
    assert.equal(JSON.parse(await readFile(stateFile)).budgetHistory, undefined);
  }
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
    if (role === 'blind-candidate') assert.doesNotMatch(bundle, /PRIVATE_FRY_EVIDENCE|Slurm leak/);
    if (role === 'feedback-analysis') return answer({ dispositions: [{ feedback: 'Slurm leak', classification: 'defect', evidence: 'PRIVATE_FRY_EVIDENCE', hypothesis: 'coverage gap', action: 'Fixed ownership', lesson: 'Verify ownership' }],
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
  assert.equal(bundles.filter((entry) => entry.role === 'blind-candidate').every((entry) => entry.bundle.includes('Verify ownership')), true);
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
  assert.equal(result.requiresUser, true);
  assert.ok(result.questions[0].reasoning);
  assert.ok(result.questions[0].options.length);
  assert.match(result.report, /New preference/);
  assert.equal(await readFile(result.reportFile, 'utf8'), result.report);
});

test('every human preference is investigated, independently verified and reported as policy learning', async (t) => {
  const { root, input } = await fixture(t);
  const controller = new Controller(root, async () => answer(report));
  const original = await controller.review(input);
  const items = [
    { ref: 'F1', file: 'app/SlurmTest.kt', line: 62, comment: 'Use representative third-party countries, not the full list', action: 'Replaced exhaustive fixture' },
    { ref: 'F2', file: 'app/SlurmTest.kt', line: 83, comment: 'Move component cases to component full-stack tests' },
  ];
  const roles = [];
  controller.agent = async ({ role, prompt }) => {
    roles.push(role);
    assert.match(prompt, /Never (ignore|dismiss)/);
    for (const item of items) assert.ok(prompt.includes(item.comment));
    if (role === 'feedback-analysis') return answer({ questions: [], scope: 'API fixtures and component tests',
      guidance: 'Use representative third-party fixtures; keep component cases in component full-stack classes',
      dispositions: items.map((item) => ({ ref: item.ref, feedback: item.comment, classification: 'new requirement/preference',
        evidence: 'Human instruction and original snapshot', hypothesis: 'Historic cause unknown', action: 'Applied correction', lesson: `Follow policy ${item.ref}` })),
    });
    return answer({ valid: true, evidence: ['Both policies match the human instructions and source'], issues: [] });
  };
  const result = await controller.learn({ runId: original.runId, feedback: JSON.stringify({ kind: 'user-confirmed-policy', items }) });
  assert.equal(result.status, 'verified');
  assert.equal(result.requiresUser, false);
  assert.deepEqual(roles, ['feedback-analysis', 'learning-validator']);
  for (const item of items) assert.ok(result.report.includes(`| ${item.ref} | ${item.file}:${item.line} | ${item.comment} |`));
  const lesson = JSON.parse(await readFile(path.join(root, '.harness/lessons', `${result.lessonId}.json`), 'utf8'));
  assert.equal(lesson.kind, 'user-confirmed-policy');
  assert.match(lesson.limitations, /no measured reviewer improvement/);
  assert.equal(lesson.evidence.dispositions.length, 2);
  assert.equal(JSON.parse(await knowledge(root)).length, 1);
  assert.deepEqual(JSON.parse(await knowledge(root))[0].instructions, [
    { ref: 'F1', instruction: 'Follow policy F1' },
    { ref: 'F2', instruction: 'Follow policy F2' },
  ]);
  assert.match(result.report, /Active instruction .*\/F1: Follow policy F1/);
  assert.match(result.report, /reviewer improvement not measured/);
  const received = [];
  controller.agent = async ({ knowledge: bundle }) => {
    received.push(JSON.parse(bundle)[0].instructions);
    return answer(report);
  };
  assert.equal((await controller.review(input)).status, 'passed');
  assert.equal(received.length, 4);
  for (const instructions of received) assert.deepEqual(instructions, [
    { ref: 'F1', instruction: 'Follow policy F1' },
    { ref: 'F2', instruction: 'Follow policy F2' },
  ]);
  const withoutInstructions = JSON.stringify([{ id: result.lessonId, guidance: lesson.guidance, scope: lesson.scope }], null, 2);
  await assert.rejects(knowledge(root, withoutInstructions.length + 1), /exceeds/);
});

test('omitted, dismissed, disputed or unverified human feedback requires user decision without promotion', async (t) => {
  for (const failure of ['omitted', 'dismissed', 'disputed', 'verification']) {
    await t.test(failure, async (t) => {
      const { root, input } = await fixture(t);
      const controller = new Controller(root, async () => answer(report));
      const original = await controller.review(input);
      const items = [{ ref: 'F1', file: 'app/Slurm.kt', line: 1, comment: 'Human instruction' },
        { ref: 'F2', file: 'app/Fry.kt', line: 2, comment: 'Another human instruction' }];
      const dispositions = items.map((item) => ({ ref: item.ref, feedback: item.comment, classification: 'new requirement/preference',
        action: 'Fixed', lesson: 'Prevent recurrence', evidence: 'source', hypothesis: 'unknown' }));
      if (failure === 'omitted') dispositions.pop();
      if (failure === 'dismissed') dispositions[0].classification = 'invalid';
      controller.agent = async ({ role }) => role === 'feedback-analysis'
        ? answer({ scope: 'tests', guidance: 'Follow instructions', dispositions,
          questions: failure === 'disputed' ? [{ question: 'Which behavior?', arguments: 'Source conflict', reasoning: 'Two incompatible requirements', options: ['A', 'B'] }] : [] })
        : answer({ valid: false, evidence: ['Source conflict'], issues: ['Explain conflict to user and choose A or B'] });
      const result = await controller.learn({ runId: original.runId, feedback: JSON.stringify({ kind: 'user-confirmed-policy', items }) });
      assert.equal(result.status, 'candidate');
      assert.equal(result.requiresUser, true);
      assert.ok(result.questions.length);
      for (const item of items) assert.ok(result.report.includes(item.comment));
      assert.equal(await knowledge(root), '[]');
      assert.equal((await stat(result.reportFile)).mode & 0o777, 0o600);
    });
  }
});

test('every feedback kind requires action and lesson, including legacy plain-text feedback', async (t) => {
  for (const kind of ['review-improvement', 'user-confirmed-policy', 'plain-text']) {
    await t.test(kind, async (t) => {
      const { root, input } = await fixture(t);
      const controller = new Controller(root, async () => answer(report));
      const original = await controller.review(input);
      const items = [{ ref: 'F1', file: 'app/Slurm.kt', line: 1, comment: 'Human instruction' }];
      let verifierCalls = 0;
      controller.agent = async ({ role }) => {
        if (role === 'learning-validator') verifierCalls++;
        return answer({ scope: 'tests', guidance: 'Follow instruction', dispositions: [{
          ref: kind === 'plain-text' ? '1' : 'F1', feedback: 'Human instruction', classification: 'defect',
          evidence: 'source', hypothesis: 'unknown',
        }] });
      };
      const result = await controller.learn({ runId: original.runId,
        feedback: kind === 'plain-text' ? 'Human instruction' : JSON.stringify({ kind, items }) });
      assert.equal(result.requiresUser, true);
      assert.match(result.error, /requires an action and lesson/);
      assert.equal(verifierCalls, 0);
      assert.equal(await knowledge(root), '[]');
    });
  }
});

test('learning preserves feedback on preflight failures without invoking agents', async (t) => {
  for (const failure of ['missing run', 'invalid run ID', 'original state', 'missing reports', 'policy', 'knowledge integrity']) {
    await t.test(failure, async (t) => {
      const { root, input } = await fixture(t);
      const controller = new Controller(root, async () => answer(report));
      const original = await controller.review(input);
      const originalFile = path.join(original.evidence, 'state.json');
      const runId = failure === 'missing run' ? 'review-missing' : failure === 'invalid run ID' ? '../../slurm' : original.runId;
      if (failure === 'original state') await writeFile(originalFile, 'Slurm');
      if (failure === 'missing reports') {
        const state = JSON.parse(await readFile(originalFile, 'utf8'));
        state.rounds[0].reports = [];
        await writeFile(originalFile, JSON.stringify(state));
      }
      if (failure === 'policy') await writeFile(path.join(root, '.harness/policy.json'), '{}');
      if (failure === 'knowledge integrity') {
        await mkdir(path.join(root, '.harness/lessons'));
        await writeFile(path.join(root, '.harness/lessons/slurm.json'), '{}');
        await writeFile(path.join(root, '.harness/knowledge.json'), JSON.stringify({ version: 1,
          lessons: [{ id: 'slurm', sha256: hash('Bender') }],
        }));
      }
      const originalState = await readFile(originalFile, 'utf8');
      const registryFile = path.join(root, '.harness/knowledge.json');
      const registry = await readFile(registryFile, 'utf8');
      let calls = 0;
      controller.agent = async () => { calls++; throw new Error('No model should run'); };
      const result = await controller.learn({ runId, feedback: 'Fry correction' });
      assert.equal(result.status, 'candidate');
      assert.ok(result.error);
      if (failure === 'knowledge integrity') assert.match(result.error, /Lesson integrity failure/);
      if (failure === 'policy') assert.match(result.error, /Invalid controller policy/);
      if (failure === 'missing run') assert.match(result.error, /ENOENT/);
      if (failure === 'invalid run ID') assert.match(result.error, /Invalid artifact ID/);
      if (failure === 'missing reports') assert.match(result.error, /Original reviewer evidence unavailable/);
      const state = JSON.parse(await readFile(path.join(result.evidence, 'state.json'), 'utf8'));
      assert.equal(state.originalRun, runId);
      assert.equal(state.feedback, 'Fry correction');
      assert.equal(state.error, result.error);
      assert.equal((await stat(path.join(result.evidence, 'state.json'))).mode & 0o777, 0o600);
      assert.equal(calls, 0);
      assert.equal(result.cost, 0);
      assert.equal(await readFile(originalFile, 'utf8'), originalState);
      assert.equal(await readFile(registryFile, 'utf8'), registry);
    });
  }
});

test('learning investigation receives actual reviewer prompts and tool investigation', async (t) => {
  const { root, input } = await fixture(t);
  const controller = new Controller(root, async () => ({ ...answer(report), messages: [{ type: 'assistant', content: [
    { type: 'tool', name: 'read', state: { input: { path: 'slurm.kt' }, content: [{ type: 'text', text: 'Slurm ownership code' }] } },
  ] }] }));
  const original = await controller.review(input);
  const roles = [];
  controller.agent = async ({ role, prompt }) => {
    assert.doesNotMatch(prompt, /Slurm ownership code/);
    assert.match(prompt, /agent-1-prompt\.txt/);
    const source = JSON.parse(prompt.split('Original evidence:\n')[1]);
    const investigation = JSON.parse(await readFile(source.artifacts[0].investigationFile, 'utf8'));
    assert.deepEqual(investigation[0].input, { path: 'slurm.kt' });
    assert.match(await readFile(investigation[0].file, 'utf8'), /Slurm ownership code/);
    roles.push(role);
    return role === 'feedback-analysis' ? answer({ guidance: 'Check ownership', dispositions: [
      { feedback: 'Slurm leak', classification: 'defect', evidence: 'source', hypothesis: 'unknown', action: 'Fixed ownership', lesson: 'Verify ownership' },
    ] })
      : answer({ valid: false, evidence: [], issues: ['Incomplete proposal'] });
  };
  assert.equal((await controller.learn({ runId: original.runId, feedback: 'Slurm leak' })).status, 'candidate');
  assert.deepEqual(roles, ['feedback-analysis', 'learning-validator']);
});

test('large learning histories keep bounded prompts and complete private evidence', async (t) => {
  const { root, input } = await fixture(t);
  const tools = Array.from({ length: 120 }, (_, index) => ({
    type: 'tool', id: `cargo-${index}`, name: 'read', state: {
      status: 'completed', metadata: { provider: 'Planet Express' }, time: { start: index, end: index + 1 },
      input: { path: `slurm-${index}.kt`, detail: 'Fry input '.repeat(100) },
      content: [{ type: 'text', text: `Slurm ownership ${index} ${'Bender cargo '.repeat(200)}` }],
    },
  }));
  const controller = new Controller(root, async ({ role }) => ({
    ...answer(role === 'functional' ? { ...report, findings: [finding] } : role === 'finding-validator'
      ? { accepted: [], rejected: [{ id: 'functional-1', reason: 'Not a defect' }], gaps: [] } : report),
    messages: [{ type: 'assistant', id: 'msg_fry', time: { created: 1, completed: 2 }, content: [...tools,
      { type: 'image', mimeType: 'image/png', data: 'Slurm cargo' }, { type: 'text', text: 'Fry investigation' },
    ] }, { type: 'user', id: 'msg_leela', text: 'Planet Express request' }],
  }));
  const original = await controller.review(input);
  const stateFile = path.join(original.evidence, 'state.json');
  const originalState = await readFile(stateFile, 'utf8');
  const roles = [];
  controller.agent = async ({ role, prompt, artifactRoots }) => {
    roles.push(role);
    assert.ok(prompt.length < 15000);
    assert.doesNotMatch(prompt, /Bender cargo|Fry input/);
    const source = JSON.parse(prompt.split('Original evidence:\n')[1]);
    assert.equal(source.artifacts.length, reviewers.length + 1);
    assert.equal('artifactDirectory' in source, false);
    for (const entry of [...source.rounds, ...source.artifacts]) {
      for (const [key, file] of Object.entries(entry)) {
        if (key.endsWith('File')) {
          const relative = path.relative(artifactRoots[0], file);
          assert.equal(path.isAbsolute(relative), false);
          assert.equal(relative.startsWith('..'), false);
        }
      }
    }
    const saved = JSON.parse(originalState);
    for (const [index, artifact] of source.artifacts.entries()) {
      assert.equal(artifact.toolCount, 120);
      assert.equal(path.dirname(artifact.investigationFile), artifactRoots[0]);
      assert.equal((await stat(artifact.investigationFile)).mode & 0o777, 0o600);
      assert.equal(path.dirname(artifact.artifactFile), artifactRoots[0]);
      assert.equal((await stat(artifact.artifactFile)).mode & 0o777, 0o600);
      assert.deepEqual(await readFile(artifact.artifactFile), await readFile(path.join(original.evidence, `agent-${index + 1}.json`)));
      assert.equal(await readFile(artifact.promptFile, 'utf8'), saved.artifacts[index].prompt);
      assert.equal(await readFile(artifact.resultFile, 'utf8'), saved.artifacts[index].text);
      const investigation = JSON.parse(await readFile(artifact.investigationFile, 'utf8'));
      assert.equal(investigation.length, tools.length);
      for (const [toolIndex, entry] of investigation.entries()) {
        assert.equal(entry.pointer, `/messages/0/content/${toolIndex}`);
        assert.equal(entry.id, tools[toolIndex].id);
        assert.equal(entry.status, tools[toolIndex].state.status);
        assert.deepEqual(entry.metadata, tools[toolIndex].state.metadata);
        assert.deepEqual(entry.time, tools[toolIndex].state.time);
        assert.deepEqual(entry.input, tools[toolIndex].state.input);
        assert.equal((await stat(entry.file)).mode & 0o777, 0o600);
        assert.equal(await readFile(entry.file, 'utf8'),
          `Tool: read\nInput: ${JSON.stringify(entry.input, null, 2)}\n${tools[toolIndex].state.content[0].text}`);
      }
      assert.equal((await stat(artifact.messagesFile)).mode & 0o777, 0o600);
      const navigation = JSON.parse(await readFile(artifact.messagesFile, 'utf8'));
      assert.equal(navigation.artifactFile, artifact.artifactFile);
      assert.equal(navigation.messages[0].pointer, '/messages/0');
      assert.equal(navigation.messages[0].id, 'msg_fry');
      assert.deepEqual(navigation.messages[0].time, { created: 1, completed: 2 });
      assert.deepEqual(navigation.messages[1], { pointer: '/messages/1', id: 'msg_leela', type: 'user',
        parts: [{ pointer: '/messages/1/text', type: 'text' }],
      });
      assert.deepEqual(navigation.messages[0].parts, saved.artifacts[index].messages[0].content.map((part, partIndex) => ({
        pointer: `/messages/0/content/${partIndex}`, ...(part.id ? { id: part.id } : {}), type: part.type,
      })));
    }
    assert.equal(await readFile(source.rounds[0].snapshotFile, 'utf8'), JSON.stringify(saved.rounds[0].snapshot, null, 2));
    const sources = JSON.parse(await readFile(source.rounds[0].sourcesFile, 'utf8'));
    assert.deepEqual(sources.map((entry) => entry.file), saved.rounds[0].snapshot.files);
    for (const entry of sources) {
      assert.equal(await readFile(entry.contentFile, 'utf8'), saved.rounds[0].snapshot.contents[entry.file]);
      assert.equal((await stat(entry.contentFile)).mode & 0o777, 0o600);
    }
    assert.equal(await readFile(source.rounds[0].diffFile, 'utf8'), saved.rounds[0].snapshot.diff);
    assert.deepEqual(JSON.parse(await readFile(source.rounds[0].reportFile, 'utf8')), {
      reports: saved.rounds[0].reports, accepted: saved.rounds[0].accepted, rejected: saved.rounds[0].rejected,
    });
    return role === 'feedback-analysis' ? answer({ guidance: 'Check ownership', dispositions: [
      { feedback: 'Slurm leak', classification: 'defect', evidence: 'source', hypothesis: 'unknown', action: 'Fixed ownership', lesson: 'Verify ownership' },
    ] })
      : answer({ valid: false, evidence: [], issues: ['Incomplete proposal'] });
  };
  const result = await controller.learn({ runId: original.runId, feedback: 'Slurm leak' });
  assert.equal(result.status, 'candidate');
  assert.match(result.error, /not independently verified/);
  assert.deepEqual(roles, ['feedback-analysis', 'learning-validator']);
  assert.equal(await readFile(stateFile, 'utf8'), originalState);
  const learningState = JSON.parse(await readFile(path.join(result.evidence, 'state.json'), 'utf8'));
  assert.equal(learningState.originalRun, original.runId);
  assert.equal(learningState.feedback, 'Slurm leak');
  assert.equal(await knowledge(root), '[]');
});

test('identical unsupported learning judgments cannot promote knowledge', async (t) => {
  const { root, input } = await fixture(t);
  const controller = new Controller(root, async () => answer(report));
  const original = await controller.review(input);
  controller.agent = async ({ role }) => {
    if (role === 'feedback-analysis') return answer({ dispositions: [{ feedback: 'Slurm leak', classification: 'defect', evidence: 'source', hypothesis: 'coverage gap', action: 'Fixed ownership', lesson: 'Verify ownership' }], scope: 'deliveries', guidance: 'Check ownership',
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
    if (['blind-candidate', 'blind-cumulative'].includes(role)) assert.doesNotMatch(bundle, /PRIVATE_FRY_EVIDENCE|Slurm leak/);
    if (role === 'feedback-analysis') return answer({ dispositions: [{ feedback: 'Slurm leak', classification: 'defect', evidence: 'PRIVATE_FRY_EVIDENCE', hypothesis: 'coverage gap', action: 'Fixed ownership', lesson: 'Verify ownership' }], scope: 'delivery access', guidance: 'Check ownership',
      replay: { input: 'Fry defect', expected: 'leak' }, heldOut: { input: 'Leela defect', expected: 'leak' }, clean: { input: 'Bender correct', expected: 'no findings' } });
    if (role === 'learning-validator') return answer({ valid: true, evidence: ['Source check'], issues: [] });
    if (role === 'blind-cumulative') { cumulativeCalls++; assert.match(bundle, /cargo integrity/); assert.match(bundle, /Check ownership/); assert.match(bundle, /Verify ownership/); }
    if (role === 'evaluation-judge') return answer({ aCorrect: prompt.includes('Bender correct'), bCorrect: true, correct: true, evidence: 'Verified defect/clean result' });
    return answer({ findings: role !== 'blind-baseline' && !prompt.includes('Bender correct') ? [{ evidence: 'Ownership leak' }] : [], gaps: [] });
  };
  assert.equal((await controller.learn({ runId: original.runId, feedback: 'Slurm leak' })).status, 'verified');
  assert.equal(cumulativeCalls, 3);
});

test('publication accounts for instructions and never deploys private disposition evidence', async (t) => {
  for (const oversized of [false, true]) {
    const { root, input } = await fixture(t, { maxKnowledgeChars: 1000 });
    await mkdir(path.join(root, '.harness/runtime'), { recursive: true });
    await chmod(path.join(root, '.harness/runtime'), 0o755);
    const controller = new Controller(root, async () => answer(report));
    const original = await controller.review(input);
    for (const directory of ['.harness/runtime', path.relative(root, original.evidence),
      path.relative(root, path.join(original.evidence, 'sources')), path.relative(root, path.join(original.evidence, 'sources/round-1'))]) {
      assert.equal((await stat(path.join(root, directory))).mode & 0o777, 0o700);
    }
    const instruction = oversized ? 'Check independent country expectations. '.repeat(100) : 'Check independent country expectations';
    controller.agent = async ({ role }) => role === 'feedback-analysis'
      ? answer({ questions: [], scope: 'API tests', guidance: 'Use independent assertions', dispositions: [{
        ref: 'F1', feedback: 'Private Fry feedback', classification: 'new requirement/preference',
        evidence: 'PRIVATE_SOURCE_ARTIFACT', hypothesis: 'PRIVATE_INVESTIGATION', action: 'PRIVATE_FIX_DETAILS', lesson: instruction,
      }] })
      : answer({ valid: true, evidence: ['Source checked'], issues: [] });
    const result = await controller.learn({ runId: original.runId, feedback: JSON.stringify({ kind: 'user-confirmed-policy',
      items: [{ ref: 'F1', file: 'app/SlurmTest.kt', line: 1, comment: 'Private Fry feedback' }] }) });
    if (oversized) {
      assert.equal(result.status, 'candidate');
      assert.match(result.error, /New knowledge exceeds budget/);
      assert.equal(await knowledge(root), '[]');
      await assert.rejects(readFile(path.join(root, '.harness/lessons', `${result.lessonId}.json`)), { code: 'ENOENT' });
    } else {
      assert.equal(result.status, 'verified');
      const bundle = await knowledge(root);
      assert.ok(bundle.includes(instruction));
      assert.doesNotMatch(bundle, /PRIVATE_|Private Fry feedback/);
    }
    for (const directory of ['.harness/runtime', path.relative(root, result.evidence),
      path.relative(root, path.join(result.evidence, 'original-evidence'))]) {
      assert.equal((await stat(path.join(root, directory))).mode & 0o777, 0o700);
    }
  }
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
