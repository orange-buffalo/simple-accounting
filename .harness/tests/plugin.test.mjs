import test from 'node:test';
import assert from 'node:assert/strict';
import plugin, { createWorkers } from '../../.opencode/plugins/sa-review/index.js';

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

test('workers use native child sessions, freeze knowledge and deny blind tools', async () => {
  const hooks = new Map();
  const permissions = [];
  let calls = 0;
  const context = { sessionID: 'ses_fry', id: 'call_leela' };
  const ctx = {
    agent: { get: async () => ({ location: { directory: '/tmp/opencode' }, data: { permissions: [{ action: 'edit', resource: '*', effect: 'deny' }] } }) },
    session: {
      update: async (rules) => permissions.push(rules),
      hook: async (kind, handler) => hooks.set(kind, handler),
      create: async () => { throw new Error('Standalone session creation is forbidden'); },
      get: async () => ({ id: 'ses_leela', parentID: 'ses_fry', title: 'SA blind-baseline', outcome: 'succeeded', cost: 0.25 }),
      context: async () => [{ type: 'assistant', finish: 'stop', content: [{ type: 'text', text: '{"findings":[]}' }] }],
    },
    tool: { list: async () => [{ id: 'subagent', execute: async (input, actualContext) => {
      calls++; assert.equal(actualContext, context); assert.equal(input.background, false);
      assert.equal(input.agent, 'sa-reviewer'); assert.equal(input.model, undefined);
      const event = { sessionID: 'ses_leela', system: [], tools: { read: {}, shell: {} } };
      await hooks.get('context')(event);
      assert.match(event.system[0].text, /Frozen Fry lesson/);
      assert.deepEqual(event.tools, {});
      return { content: 'complete' };
    } }] },
  };
  const workers = await createWorkers(ctx);
  workers.contexts.set('ses_fry', context);
  const result = await workers.execute({ role: 'blind-baseline', prompt: 'Review Slurm', knowledge: 'Frozen Fry lesson', parentSessionID: 'ses_fry' });
  assert.equal(calls, 1); assert.equal(result.parentSessionID, 'ses_fry'); assert.equal(result.failed, false);
  assert.deepEqual(permissions, [{ sessionID: 'ses_leela', permissions: [{ action: '*', resource: '*', effect: 'deny' }] }]);
});
