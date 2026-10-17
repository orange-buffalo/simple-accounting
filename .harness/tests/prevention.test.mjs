import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { Controller, catalogText, curate, knowledge } from '../../.opencode/plugins/sa-review/core.js';

const rule = { id: 'slurm-ownership', category: 'backend', appliesWhen: 'Reading workspace cargo',
  instruction: 'Enforce workspace ownership before reading cargo.', reason: 'A cargo identifier alone does not establish access.' };
const catalog = { version: 2, rules: [rule] };
const items = [{ ref: 'F1', file: 'app/Slurm.kt', line: 1, comment: 'Use the existing ownership rule' }];
const proposal = () => ({ questions: [], upsert: [], retire: [], dispositions: [{ ref: 'F1', feedback: items[0].comment,
  diagnosis: 'not-applied', action: 'Fixed cargo access', evidence: 'Slurm.kt ownership guard was missing',
  prevention: 'Apply the existing ownership rule at implementation start', ruleIds: [rule.id] }],
caseCheck: { analogous: 'Invoice cargo also requires ownership', nonApplicable: 'Public planet names do not require workspace access' } });
const answer = (value) => ({ text: JSON.stringify(value), cost: 0 });

async function fixture() {
  const root = await mkdtemp('/tmp/opencode/sa-prevention-');
  await mkdir(path.join(root, '.harness'));
  await writeFile(path.join(root, '.harness/prevention.json'), JSON.stringify(catalog));
  await writeFile(path.join(root, '.harness/policy.json'), JSON.stringify({ version: 1, maxFixCycles: 1,
    maxElapsedMinutes: 10, maxCostUsd: 10, maxKnowledgeChars: 30000,
    reviewers: ['review-1', 'review-2', 'compliance-1', 'compliance-2'], reviewerModel: { providerID: 'openai', id: 'gpt-5.6-terra' }, models: {} }));
  return root;
}

test('active rules reach knowledge intact and oversized or invalid catalogs fail closed', async () => {
  const root = await fixture();
  assert.deepEqual(JSON.parse(await knowledge(root)), [rule]);
  await assert.rejects(knowledge(root, 10), /exceeds/);
  for (const invalid of [{ ...rule, evidence: 'Private source' }, { ...rule, appliesWhen: '' }, { ...rule, id: '../slurm' }]) {
    assert.throws(() => catalogText({ version: 2, rules: [invalid] }));
  }
  assert.throws(() => catalogText({ version: 2, rules: [rule, rule] }), /Invalid prevention rule/);
  await writeFile(path.join(root, '.harness/prevention.json'), '{}');
  await assert.rejects(knowledge(root), /Invalid prevention catalog/);
});

test('curation reuses rules and preserves unrelated experience while replacing obsolete guidance', () => {
  assert.deepEqual(curate(catalog, proposal(), items), catalog);
  const replacement = { ...rule, id: 'cargo-access', instruction: 'Check ownership for cargo reads and writes.' };
  const other = { ...rule, id: 'robot-oil', instruction: 'Validate robot oil currency.' };
  const update = proposal();
  update.upsert = [replacement];
  update.retire = [{ id: rule.id, replacements: [replacement.id], reason: 'Consolidate cargo read/write access' }];
  update.dispositions[0].ruleIds = [replacement.id];
  assert.deepEqual(curate({ version: 2, rules: [rule, other] }, update, items).rules, [other, replacement]);
  update.retire[0].replacements = ['missing'];
  assert.throws(() => curate(catalog, update, items), /Missing replacement/);
  const incomplete = proposal();
  incomplete.dispositions = [];
  assert.throws(() => curate(catalog, incomplete, items), /Incomplete/);
  const changed = proposal();
  changed.dispositions[0].feedback = 'A different comment';
  assert.throws(() => curate(catalog, changed, items), /feedback disposition/);
});

test('learning existing guidance needs no new rule or blind ceremony and deploys only the operational catalog', async () => {
  const root = await fixture();
  const roles = [];
  const controller = new Controller(root, async ({ role, prompt }) => {
    roles.push(role);
    assert.match(prompt, /Never dismiss/);
    assert.ok(prompt.includes(items[0].comment));
    assert.match(prompt, /Original run unavailable/);
    return answer(role === 'feedback-analysis' ? proposal() : { valid: true, evidence: ['Verified source guard and existing scope'], issues: [] });
  });
  const result = await controller.learn({ runId: 'review-fry', feedback: JSON.stringify({ kind: 'user-confirmed-policy', items }) });
  assert.equal(result.status, 'verified');
  assert.deepEqual(roles, ['feedback-analysis', 'learning-validator']);
  assert.deepEqual(JSON.parse(await knowledge(root)), [rule]);
  assert.match(result.report, /slurm-ownership/);
  assert.match(result.report, /not measured improvement/);
  assert.doesNotMatch(await knowledge(root), /Fixed cargo access|ownership guard was missing/);
  assert.equal(await readFile(result.reportFile, 'utf8'), result.report);
});

test('unverified, incomplete and oversized prevention never replaces the active catalog', async () => {
  for (const failure of ['verification', 'omission', 'oversized', 'cases', 'conflict', 'drift']) {
    const root = await fixture();
    const candidate = proposal();
    if (failure === 'omission') candidate.dispositions = [];
    if (failure === 'oversized') candidate.upsert = [{ ...rule, instruction: 'Slurm '.repeat(10000) }];
    if (failure === 'cases') delete candidate.caseCheck;
    if (failure === 'conflict') candidate.questions = [{ question: 'Which scope?', reasoning: 'Instructions conflict', options: ['Workspace cargo', 'Public cargo'] }];
    const controller = new Controller(root, async ({ role }) => {
      if (role === 'feedback-analysis') return answer(candidate);
      if (failure === 'drift') await writeFile(path.join(root, '.harness/prevention.json'), JSON.stringify({ version: 2, rules: [] }));
      return answer({ valid: failure !== 'verification', evidence: ['Checked scope'], issues: failure === 'verification' ? ['Unsupported expansion'] : [] });
    });
    const result = await controller.learn({ runId: 'review-fry', feedback: JSON.stringify({ kind: 'user-confirmed-policy', items }) });
    assert.equal(result.status, 'candidate', failure);
    assert.equal(result.requiresUser, true);
    assert.ok(result.report.includes(items[0].comment));
    assert.deepEqual(JSON.parse(await knowledge(root)), failure === 'drift' ? [] : [rule]);
  }
});
