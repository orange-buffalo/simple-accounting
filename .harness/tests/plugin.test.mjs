import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import plugin, { createWorkers } from '../../.opencode/plugins/sa-review/index.js';

const repository = fileURLToPath(new URL('../../', import.meta.url));
const canRead = (rules, resource) => rules.filter((rule) => ['*', 'read'].includes(rule.action)
  && (rule.resource === '*' || path.matchesGlob(path.relative(repository, resource), rule.resource))).at(-1)?.effect === 'allow';

test('plugin registers executable tools and injects knowledge before every model request', async () => {
  const hooks = new Map();
  const tools = [];
  const skills = [];
  await plugin.setup({
    session: { hook: async (kind, handler) => { hooks.set(kind, handler); } },
    tool: { transform: async (callback) => callback({ add: (tool) => tools.push(tool) }) },
    skill: { transform: async (callback) => callback({ add: (skill) => skills.push(skill) }) },
  });
  assert.deepEqual(tools.map((tool) => tool.name), ['sa_review', 'sa_learn']);
  for (const kind of ['context', 'generate']) {
    const event = { sessionID: 'ses_fry', system: [] };
    await hooks.get(kind)(event);
    assert.match(event.system[0].text, /Approved repository lessons/);
  }
});

test('worker binding preserves repository secret-read and protected-edit denials', async () => {
  const config = JSON.parse(await readFile(new URL('../../.opencode/opencode.jsonc', import.meta.url), 'utf8'));
  for (const role of ['security', 'fixer', 'feedback-analysis', 'learning-validator']) {
    const fixer = role === 'fixer';
    const folder = ['feedback-analysis', 'learning-validator'].includes(role) ? 'original-evidence' : 'sources';
    const hooks = new Map();
    let actualRules;
    const originalRules = config.agents[fixer ? 'sa-fixer' : 'sa-reviewer'].permissions;
    const ctx = {
      agent: { get: async ({ agentID }) => {
        assert.equal(agentID, fixer ? 'sa-fixer' : 'sa-reviewer');
        return { permissions: originalRules };
      } },
      session: {
        hook: async (kind, handler) => hooks.set(kind, handler),
        update: async ({ permissions }) => { actualRules = permissions; },
        get: async () => ({ id: 'ses_leela', parentID: 'ses_fry', outcome: 'succeeded', cost: 0 }),
        context: async () => [{ type: 'assistant', finish: 'stop', content: [{ type: 'text', text: 'done' }] }],
      },
      tool: { list: async () => [{ id: 'subagent', execute: async (input) => {
        await hooks.get('prompt')({ sessionID: 'ses_leela', prompt: { text: input.prompt } });
        return { content: 'done' };
      } }] },
    };
    const workers = await createWorkers(ctx);
    workers.contexts.set('ses_fry', { sessionID: 'ses_fry' });
    await workers.execute({ role, prompt: 'Review Slurm', knowledge: '[]', fixer,
      artifactRoots: [path.join(repository, '.harness/runtime/review-fry', folder)],
      model: fixer ? undefined : { providerID: 'openai', id: 'gpt-5.6-terra', variant: 'medium' }, parentSessionID: 'ses_fry' });
    assert.deepEqual(actualRules.slice(0, originalRules.length), originalRules);
    assert.equal(canRead(actualRules, `${repository}/.harness/runtime/review-fry/${folder}/1.txt`), true);
    assert.equal(canRead(actualRules, `${repository}/.harness/runtime/review-leela/${folder}/1.txt`), false);
    assert.equal(canRead(actualRules, `${repository}/.harness/runtime/review-fry/state.json`), false);
    const otherFolder = folder === 'sources' ? 'original-evidence' : 'sources';
    assert.equal(canRead(actualRules, `${repository}/.harness/runtime/review-fry/${otherFolder}/1.txt`), false);
    for (const resource of ['**/.env*', '**/.test-config.yaml', '**/*.pem', '**/*.key']) {
      assert.ok(actualRules.some((rule) => rule.action === 'read' && rule.resource === resource && rule.effect === 'deny'));
    }
    if (fixer) {
      for (const resource of ['**/.git/**', '**/gradlew', '**/gradle/**', '**/buildSrc/**', '**/package.json',
        '**/docs/AgentHarness.md', '**/.harness/**', '**/.opencode/**', '**/.agents/**', '**/.github/**', '**/AGENTS.md',
        '**/build.gradle.kts', '**/settings.gradle.kts', '**/.env*', '**/.test-config.yaml']) {
        assert.ok(actualRules.some((rule) => rule.action === 'edit' && rule.resource === resource && rule.effect === 'deny'));
      }
    }
  }
});

test('workers use native child sessions, freeze knowledge and deny blind tools', async () => {
  const hooks = new Map();
  const permissions = [];
  let workerPrompt;
  let calls = 0;
  const context = { sessionID: 'ses_fry', id: 'call_leela' };
  const model = { providerID: 'openai', id: 'gpt-5.6-terra', variant: 'medium' };
  const ctx = {
    agent: { get: async () => ({ location: { directory: '/tmp/opencode' }, data: { permissions: [{ action: 'edit', resource: '*', effect: 'deny' }] } }) },
    session: {
      update: async (rules) => permissions.push(rules),
      hook: async (kind, handler) => hooks.set(kind, handler),
      create: async () => { throw new Error('Standalone session creation is forbidden'); },
      get: async () => ({ id: 'ses_leela', parentID: 'ses_fry', title: 'SA blind-baseline', outcome: 'succeeded', cost: 0.25 }),
      context: async () => [{ type: 'user', text: workerPrompt },
        { type: 'assistant', finish: 'stop', content: [{ type: 'text', text: '{"findings":[]}' }] }],
    },
    tool: { list: async () => [{ id: 'subagent', execute: async (input, actualContext) => {
      workerPrompt = `You are a subagent spawned by another session.\n${input.prompt}`;
      calls++; assert.equal(actualContext, context); assert.equal(input.background, false);
      assert.equal(input.agent, 'sa-reviewer'); assert.equal(input.model, 'openai/gpt-5.6-terra#medium');
      const event = { sessionID: 'ses_leela', system: [], tools: { read: {}, shell: {} } };
      await hooks.get('context')(event);
      assert.ok(event.system.some(({ text }) => text.includes('Frozen Fry lesson')));
      assert.ok(event.system.some(({ text }) => text.includes('Never run builds/tests or inspect their results')));
      assert.deepEqual(event.tools, {});
      return { content: 'complete' };
    } }] },
  };
  const workers = await createWorkers(ctx);
  workers.contexts.set('ses_fry', context);
  await assert.rejects(workers.execute({ role: 'blind-baseline', prompt: 'Review Slurm', knowledge: 'Frozen Fry lesson', parentSessionID: 'ses_fry' }), /explicit configured reviewer model/);
  const result = await workers.execute({ role: 'blind-baseline', prompt: 'Review Slurm', knowledge: 'Frozen Fry lesson', model, parentSessionID: 'ses_fry' });
  assert.equal(calls, 1); assert.equal(result.parentSessionID, 'ses_fry'); assert.equal(result.failed, false);
  assert.deepEqual(permissions, [{ sessionID: 'ses_leela', permissions: [{ action: '*', resource: '*', effect: 'deny' }] }]);
});

test('concurrent native workers bind separate child sessions and restrict result reads', async () => {
  const hooks = new Map();
  const permissions = new Map();
  const sessions = new Map();
  const releases = [];
  const model = { providerID: 'openai', id: 'gpt-5.6-terra', variant: 'medium' };
  const ctx = {
    agent: { get: async () => ({ permissions: [{ action: '*', resource: '*', effect: 'deny' }, { action: 'read', resource: '*', effect: 'allow' }] }) },
    session: {
      hook: async (kind, handler) => hooks.set(kind, handler),
      update: async ({ sessionID, permissions: rules }) => permissions.set(sessionID, rules),
      get: async ({ sessionID }) => sessions.get(sessionID),
      context: async () => [{ type: 'assistant', finish: 'stop', content: [{ type: 'text', text: '{"findings":[],"coverage":["Slurm"],"gaps":[]}' }] }],
    },
    tool: { list: async () => [{ id: 'subagent', execute: async (input) => {
      const sessionID = `ses_${input.description.split(' ')[1]}`;
      sessions.set(sessionID, { id: sessionID, parentID: 'ses_fry', outcome: 'succeeded', cost: 0.25 });
      const pending = new Promise((resolve) => releases.push(resolve));
      if (releases.length === 2) releases.forEach((release) => release());
      await pending;
      await hooks.get('prompt')({ sessionID, prompt: { text: input.prompt } });
      const event = { sessionID, system: [], tools: { read: {}, glob: {}, shell: {}, subagent: {}, sa_review: {} } };
      await hooks.get('context')(event);
      assert.deepEqual(Object.keys(event.tools), ['read', 'glob']);
      assert.ok(event.system.some(({ text }) => text.includes(`${input.description} lesson`)));
      assert.equal(input.model, 'openai/gpt-5.6-terra#medium');
      return { content: 'done' };
    } }] },
  };
  const workers = await createWorkers(ctx);
  workers.contexts.set('ses_fry', { sessionID: 'ses_fry' });
  const results = await Promise.all(['functional', 'security'].map((role) => workers.execute({ role, prompt: 'Review Slurm',
    knowledge: `SA ${role} lesson`, model, parentSessionID: 'ses_fry',
    artifactRoots: [path.join(repository, `.harness/runtime/review-${role}/sources`)] })));
  assert.deepEqual(results.map(({ sessionId }) => sessionId), ['ses_functional', 'ses_security']);
  for (const [sessionID, rules] of permissions.entries()) {
    for (const resource of ['**/.harness/runtime/**', '**/build/test-results/**', '**/build/reports/**', '**/*.log']) {
      assert.ok(rules.some((rule) => rule.action === 'read' && rule.resource === resource && rule.effect === 'deny'));
    }
    const role = sessionID.slice('ses_'.length);
    assert.ok(rules.some((rule) => rule.resource === `**/.harness/runtime/review-${role}/sources/**` && rule.effect === 'allow'));
    assert.equal(canRead(rules, `${repository}/.harness/runtime/review-${role}/sources/1.txt`), true);
    const otherRole = role === 'functional' ? 'security' : 'functional';
    assert.equal(canRead(rules, `${repository}/.harness/runtime/review-${otherRole}/sources/1.txt`), false);
    assert.equal(canRead(rules, `${repository}/.harness/runtime/review-${role}/original-evidence/1.txt`), false);
  }
});

test('artifact grants reject runtime state, wildcard runs, path escapes and unrelated learning evidence', async () => {
  const workers = await createWorkers({ session: { hook: async () => {} } });
  const input = { role: 'security', prompt: 'Review Slurm', knowledge: '[]',
    model: { providerID: 'openai', id: 'gpt-5.6-terra', variant: 'medium' }, parentSessionID: 'ses_fry' };
  for (const folder of ['.harness/runtime/review-fry', '.harness/runtime/*/sources', '../sources',
    '.harness/runtime/review-fry/original-evidence']) {
    await assert.rejects(workers.execute({ ...input, artifactRoots: [path.join(repository, folder)] }), /Invalid delegated artifact root/);
  }
});

test('matching child titles cannot acquire worker grants and tokens cannot rebind sessions', async () => {
  const hooks = new Map();
  const permissions = new Map();
  let workerPrompt;
  const ctx = {
    agent: { get: async () => ({ permissions: [{ action: 'read', resource: '*', effect: 'allow' }] }) },
    session: {
      hook: async (kind, handler) => hooks.set(kind, handler),
      update: async ({ sessionID, permissions: rules }) => permissions.set(sessionID, rules),
      get: async ({ sessionID }) => ({ id: sessionID, parentID: 'ses_fry', title: 'SA security', outcome: 'succeeded', cost: 0 }),
      context: async ({ sessionID }) => [{ type: 'user', text: sessionID === 'ses_leela'
        ? workerPrompt : 'SA_WORKER:00000000-0000-0000-0000-000000000000\nReview Slurm' },
        { type: 'assistant', finish: 'stop', content: [{ type: 'text', text: '{"findings":[]}' }] }],
    },
    tool: { list: async () => [{ id: 'subagent', execute: async (input) => {
      workerPrompt = input.prompt;
      for (const kind of ['context', 'generate']) {
        await hooks.get(kind)({ sessionID: 'ses_zoidberg', system: [], tools: {} });
        assert.equal(permissions.has('ses_zoidberg'), false);
      }
      await hooks.get('context')({ sessionID: 'ses_leela', system: [], tools: {} });
      assert.equal(permissions.has('ses_leela'), true);
      await assert.rejects(hooks.get('prompt')({ sessionID: 'ses_zoidberg', prompt: { text: input.prompt } }), /already bound/);
      assert.equal(permissions.has('ses_zoidberg'), false);
      return { content: 'done' };
    } }] },
  };
  const workers = await createWorkers(ctx);
  workers.contexts.set('ses_fry', { sessionID: 'ses_fry' });
  const result = await workers.execute({ role: 'security', prompt: 'Review Slurm', knowledge: '[]', parentSessionID: 'ses_fry',
    model: { providerID: 'openai', id: 'gpt-5.6-terra', variant: 'medium' },
    artifactRoots: [path.join(repository, '.harness/runtime/review-fry/sources')] });
  assert.equal(result.failed, false);
  assert.equal(result.sessionId, 'ses_leela');
});
