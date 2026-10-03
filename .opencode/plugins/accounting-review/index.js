import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { readFile } from 'node:fs/promises';
import { Controller, knowledge, hash } from './core.js';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const schema = (properties, required) => ({ type: 'object', properties, required, additionalProperties: false });
const string = { type: 'string', minLength: 1 };

export default {
  id: 'simple-accounting.review-controller',
  async setup(ctx) {
    const frozen = new Map();
    for (const kind of ['context', 'generate']) {
      await ctx.session.hook(kind, async (event) => {
        const bundle = frozen.get(event.sessionID) ?? await knowledge(root);
        event.system.push({ type: 'text', text: `Approved repository lessons (apply when relevant):\n${bundle}` });
      });
    }
    const controller = new Controller(root, async ({ role, prompt, knowledge: bundle, fixer, model }) => {
      const blind = role.startsWith('blind-') || role === 'evaluation-judge';
      const session = await ctx.session.create({
        title: `Accounting: ${role}`, agent: fixer ? 'accounting-fixer' : 'accounting-reviewer',
        location: { directory: root }, metadata: { accountingRole: role, knowledgeHash: hash(bundle) },
        ...(model ? { model } : {}),
        ...(blind ? { permissions: [{ action: '*', resource: '*', effect: 'deny' }] } : {}),
      });
      frozen.set(session.id, bundle);
      try {
        await ctx.session.prompt({ sessionID: session.id, text: prompt });
        await ctx.session.wait({ sessionID: session.id });
        const info = await ctx.session.get({ sessionID: session.id });
        const messages = await ctx.session.context({ sessionID: session.id });
        const assistant = messages.filter((message) => message.type === 'assistant').at(-1);
        return { text: assistant?.content.filter((part) => part.type === 'text').map((part) => part.text).join('\n') ?? '',
          sessionId: session.id, cost: info.cost, messages,
          failed: info.outcome !== 'succeeded' || !assistant || Boolean(assistant.error) || assistant.finish !== 'stop' };
      } finally { frozen.delete(session.id); }
    });
    const withCallerModel = async (input, context) => {
      const caller = await ctx.session.get({ sessionID: context.sessionID });
      const messages = await ctx.session.context({ sessionID: context.sessionID });
      const defaultModel = caller.model ?? messages.filter((message) => message.type === 'assistant').at(-1)?.model;
      if (!defaultModel) throw new Error('Caller model unavailable; select a model before running the controller');
      return { ...input, defaultModel };
    };
    await ctx.tool.transform((editor) => {
      editor.add({
        name: 'accounting_review', description: 'Run Gradle validation and bounded specialist review/fix cycles. Original requirements and pre-edit base SHA are required. Never commits or publishes.',
        input: schema({ requirements: string, base: string, profile: { type: 'string', enum: ['harness', 'frontend', 'backend', 'full'] }, runId: string }, ['requirements', 'base', 'profile']),
        execute: async (input, context) => ({ content: JSON.stringify(await controller.review(await withCallerModel(input, context)), null, 2) }),
      });
      editor.add({
        name: 'accounting_learn', description: 'Investigate human corrections against original review artifacts; independently verify and blind-evaluate before promoting lessons. Does not implement feedback or commit.',
        input: schema({ runId: string, feedback: string }, ['runId', 'feedback']),
        execute: async (input, context) => ({ content: JSON.stringify(await controller.learn(await withCallerModel(input, context)), null, 2) }),
      });
    });
    const upstream = JSON.parse(await readFile(path.join(root, '.harness/upstream.json'), 'utf8'));
    const skills = [];
    for (const id of upstream.skills) {
      const file = path.join(root, '.harness/vendor/skills', id, 'SKILL.md');
      try {
        const content = await readFile(file, 'utf8');
        skills.push({ id, name: id, description: `Pinned Compound Engineering ${id}; repository delivery rules take precedence`, path: file, content });
      } catch (error) { if (error.code !== 'ENOENT') throw error; }
    }
    await ctx.skill.transform((editor) => { for (const skill of skills) editor.add(skill); });
  },
};
