import test from 'node:test';
import assert from 'node:assert/strict';
import plugin from '../../.opencode/plugins/accounting-review/index.js';

test('plugin registers executable tools and injects knowledge before every model request', async () => {
  const hooks = new Map();
  const tools = [];
  const skills = [];
  await plugin.setup({
    session: { hook: async (kind, handler) => { hooks.set(kind, handler); } },
    tool: { transform: async (callback) => callback({ add: (tool) => tools.push(tool) }) },
    skill: { transform: async (callback) => callback({ add: (skill) => skills.push(skill) }) },
  });
  assert.deepEqual(tools.map((tool) => tool.name), ['accounting_review', 'accounting_learn']);
  for (const kind of ['context', 'generate']) {
    const event = { sessionID: 'ses_fry', system: [] };
    await hooks.get(kind)(event);
    assert.match(event.system[0].text, /Approved repository lessons/);
  }
});
