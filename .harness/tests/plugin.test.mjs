import test from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, mkdir, readFile, writeFile } from 'node:fs/promises';
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
    assert.match(event.system[0].text, /BEFORE implementation/);
    assert.match(event.system[0].text, /live-consumer-migration/);
  }
});

test('context and generation hooks inject every rule beyond the former knowledge cap', async () => {
  const directory = await mkdtemp('/tmp/opencode/sa-injection-');
  await mkdir(path.join(directory, '.harness'));
  const rules = Array.from({ length: 200 }, (_, index) => ({ id: `cargo-${index}`, category: 'backend',
    appliesWhen: `Reading cargo ${index}`, instruction: 'Check workspace ownership. '.repeat(10), reason: 'Preserve isolation.' }));
  await writeFile(path.join(directory, '.harness/prevention.json'), JSON.stringify({ version: 2, rules }));
  const hooks = new Map();
  await createWorkers({ session: { hook: async (kind, handler) => hooks.set(kind, handler) } }, directory);
  for (const kind of ['context', 'generate']) {
    const event = { sessionID: 'ses_fry', system: [] };
    await hooks.get(kind)(event);
    const bundle = event.system[0].text.split('\n').slice(1).join('\n');
    assert.ok(bundle.length > 30000);
    assert.deepEqual(JSON.parse(bundle), rules);
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
      model: fixer ? undefined : { providerID: 'openai', id: 'gpt-6.1-sol', variant: 'medium' }, parentSessionID: 'ses_fry' });
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
  let recorded;
  let completed = false;
  const context = { sessionID: 'ses_fry', id: 'call_leela' };
  const model = { providerID: 'openai', id: 'gpt-6.1-sol', variant: 'medium' };
  const ctx = {
    agent: { get: async () => ({ location: { directory: '/tmp/opencode' }, data: { permissions: [{ action: 'edit', resource: '*', effect: 'deny' }] } }) },
    session: {
      update: async (rules) => permissions.push(rules),
      hook: async (kind, handler) => hooks.set(kind, handler),
      create: async () => { throw new Error('Standalone session creation is forbidden'); },
      get: async () => ({ data: { id: 'ses_leela', parentID: 'ses_fry', outcome: completed ? 'succeeded' : undefined,
        cost: completed ? 0.25 : undefined } }),
      context: async () => ({ data: [{ type: 'user', text: workerPrompt },
        ...(completed ? [{ type: 'assistant', finish: 'stop', content: [{ type: 'text', text: '{"findings":[]}' }] }] : [])] }),
      wait: async ({ sessionID }) => {
        assert.equal(sessionID, 'ses_leela');
        assert.equal(recorded, sessionID);
        const event = { sessionID, system: [], tools: { read: {}, shell: {} } };
        await hooks.get('context')(event);
        assert.ok(event.system.some(({ text }) => text.includes('Frozen Fry lesson')));
        assert.ok(event.system.some(({ text }) => text.includes('Never run builds/tests or inspect their results')));
        assert.deepEqual(event.tools, {});
        completed = true;
      },
    },
    tool: { list: async () => [{ id: 'subagent', execute: async (input, actualContext) => {
      workerPrompt = `You are a subagent spawned by another session.\n${input.prompt}`;
      calls++; assert.equal(actualContext, context); assert.equal(input.background, false);
      assert.equal(input.agent, 'sa-reviewer'); assert.equal(input.model, 'openai/gpt-6.1-sol#medium');
      return { output: { status: 'running', sessionID: 'ses_leela' } };
    } }] },
  };
  const workers = await createWorkers(ctx);
  workers.contexts.set('ses_fry', context);
  await assert.rejects(workers.execute({ role: 'blind-baseline', prompt: 'Review Slurm', knowledge: 'Frozen Fry lesson', parentSessionID: 'ses_fry' }), /explicit configured reviewer model/);
  const result = await workers.execute({ role: 'blind-baseline', prompt: 'Review Slurm', knowledge: 'Frozen Fry lesson',
    model, parentSessionID: 'ses_fry', recordSession: async (id) => { recorded = id; } });
  assert.equal(calls, 1); assert.equal(result.parentSessionID, 'ses_fry'); assert.equal(result.failed, false);
  assert.deepEqual(permissions, [{ sessionID: 'ses_leela', permissions: [{ action: '*', resource: '*', effect: 'deny' }] }]);
  assert.deepEqual(await workers.reconcile({ sessionId: recorded, parentSessionID: 'ses_fry', prompt: 'Review Slurm' }),
    { cost: 0.25, outcome: 'succeeded' });
  await assert.rejects(workers.reconcile({ sessionId: recorded, parentSessionID: 'ses_fry', prompt: 'Review Robot Oil' }),
    /recovery prompt mismatch/);
  await assert.rejects(workers.reconcile({ sessionId: recorded, parentSessionID: 'ses_bender', prompt: 'Review Slurm' }),
    /session or parent mismatch/);
});

test('concurrent native workers bind separate child sessions and restrict result reads', async () => {
  const hooks = new Map();
  const permissions = new Map();
  const sessions = new Map();
  const releases = [];
  const model = { providerID: 'openai', id: 'gpt-6.1-sol', variant: 'medium' };
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
      assert.equal(input.model, 'openai/gpt-6.1-sol#medium');
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

test('interrupted worker waits preserve live restrictions and fail closed after reload', async () => {
  for (const interruption of ['before binding', 'after binding', 'before reload']) {
    const hooks = new Map();
    const sessionID = 'ses_leela';
    const parentSessionID = 'ses_fry';
    const binding = { sessionID, parentSessionID };
    let text;
    let outcome;
    let waits = 0;
    let interrupts = 0;
    const permissions = [];
    const generate = async () => {
      const event = { sessionID, system: [], tools: { read: {}, shell: {} } };
      await hooks.get('generate')(event);
      assert.deepEqual(Object.keys(event.tools), ['read']);
      assert.ok(event.system.some(({ text: instruction }) => instruction.includes('Frozen Leela lesson')));
    };
    const ctx = {
      agent: { get: async () => ({ permissions: [{ action: 'read', resource: '*', effect: 'allow' }] }) },
      session: {
        hook: async (kind, handler) => hooks.set(kind, handler),
        update: async (request) => permissions.push(request.permissions),
        get: async () => ({ id: sessionID, parentID: parentSessionID, outcome, cost: 0.25 }),
        context: async () => [{ type: 'user', text }],
        wait: async () => {
          if (++waits === 1) {
            if (interruption !== 'before binding') await generate();
            throw new Error('Wait disconnected');
          }
          if (interruption !== 'before reload') await generate();
          outcome = interruption === 'before reload' ? 'interrupted' : 'succeeded';
        },
        interrupt: async (input) => {
          assert.deepEqual(input, { sessionID, resume: false });
          assert.deepEqual(permissions.at(-1), [{ action: '*', resource: '*', effect: 'deny' }]);
          interrupts++;
        },
      },
      tool: { list: async () => [{ id: 'subagent', execute: async (input) => {
        text = input.prompt;
        return { output: { sessionID, status: 'running' } };
      } }] },
    };
    const workers = await createWorkers(ctx);
    workers.contexts.set(parentSessionID, { sessionID: parentSessionID });
    const result = await workers.execute({ role: 'review-1', prompt: 'Review Slurm', knowledge: 'Frozen Leela lesson',
      model: { providerID: 'openai', id: 'gpt-6.1-sol' }, parentSessionID });
    assert.equal(result.text, 'Wait disconnected');
    assert.equal(result.failed, true);
    const reloaded = interruption === 'before reload';
    const recovery = reloaded ? await createWorkers(ctx) : workers;
    if (reloaded) {
      await assert.rejects(generate(), /no active restricted binding/);
    } else if (interruption === 'after binding') {
      await generate();
    }
    assert.deepEqual(await recovery.reconcile({ sessionId: binding.sessionID, parentSessionID, prompt: 'Review Slurm' }),
      { cost: 0.25, outcome: reloaded ? 'interrupted' : 'succeeded' });
    assert.equal(interrupts, reloaded ? 1 : 0);
  }
});

test('native reports cannot pass without both a verified binding and a successful terminal outcome', async () => {
  for (const [bind, outcome, diagnostic] of [
    [false, 'succeeded', /did not bind/],
    [true, undefined, /no terminal outcome/],
    [true, 'failed', /clean-looking report/],
  ]) {
    const hooks = new Map();
    const sessionID = 'ses_leela';
    const parentSessionID = 'ses_fry';
    const ctx = {
      agent: { get: async () => ({ permissions: [] }) },
      session: {
        hook: async (kind, handler) => hooks.set(kind, handler),
        update: async () => {},
        get: async () => ({ id: sessionID, parentID: parentSessionID, outcome, cost: 0.25 }),
        context: async () => [{ type: 'assistant', finish: 'stop', content: [{ type: 'text', text: 'clean-looking report' }] }],
      },
      tool: { list: async () => [{ id: 'subagent', execute: async (input) => {
        if (bind) await hooks.get('prompt')({ sessionID, prompt: { text: input.prompt } });
        return { output: { sessionID } };
      } }] },
    };
    const workers = await createWorkers(ctx);
    workers.contexts.set(parentSessionID, { sessionID: parentSessionID });
    const result = await workers.execute({ role: 'review-1', prompt: 'Review Slurm', knowledge: '[]',
      model: { providerID: 'openai', id: 'gpt-6.1-sol' }, parentSessionID });
    assert.equal(result.failed, true);
    assert.match(result.text, diagnostic);
  }
});

test('artifact grants reject runtime state, wildcard runs, path escapes and unrelated learning evidence', async () => {
  const workers = await createWorkers({ session: { hook: async () => {} } });
  const input = { role: 'security', prompt: 'Review Slurm', knowledge: '[]',
    model: { providerID: 'openai', id: 'gpt-6.1-sol', variant: 'medium' }, parentSessionID: 'ses_fry' };
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
        ? workerPrompt : 'Review Slurm' },
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
    model: { providerID: 'openai', id: 'gpt-6.1-sol', variant: 'medium' },
    artifactRoots: [path.join(repository, '.harness/runtime/review-fry/sources')] });
  assert.equal(result.failed, false);
  assert.equal(result.sessionId, 'ses_leela');
});
