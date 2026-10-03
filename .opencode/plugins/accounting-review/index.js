import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { readFile } from 'node:fs/promises';
import { randomUUID } from 'node:crypto';
import { Controller, knowledge, hash } from './core.js';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const schema = (properties, required) => ({ type: 'object', properties, required, additionalProperties: false });
const string = { type: 'string', minLength: 1 };

export async function createWorkers(ctx, directory = root) {
    const frozen = new Map();
    const delegations = new Map();
    const contexts = new Map();
    const bindWorker = async (session, work) => {
      if (session.parentID !== work.parentSessionID) throw new Error('Accounting worker must be a child of the calling session');
      work.sessionId = session.id;
      frozen.set(session.id, work.bundle);
      const agent = await ctx.agent.get({ agentID: work.fixer ? 'accounting-fixer' : 'accounting-reviewer' });
      const permissions = agent.data?.permissions ?? agent.permissions;
      if (!Array.isArray(permissions)) throw new Error('Worker permission rules unavailable');
      await ctx.session.update({ sessionID: session.id, permissions: work.blind
        ? [{ action: '*', resource: '*', effect: 'deny' }] : permissions });
    };
    await ctx.session.hook('prompt', async (event) => {
      const token = event.prompt.text.match(/^ACCOUNTING_WORKER:([a-f0-9-]+)\n/)?.[1];
      const work = token && delegations.get(token);
      if (!work) return;
      const session = await ctx.session.get({ sessionID: event.sessionID });
      await bindWorker(session, work);
      event.metadata = { ...event.metadata, accountingRole: work.role, knowledgeHash: hash(work.bundle), accountingParent: work.parentSessionID };
    });
    for (const kind of ['context', 'generate']) {
      await ctx.session.hook(kind, async (event) => {
        if (!frozen.has(event.sessionID) && delegations.size) {
          const session = await ctx.session.get({ sessionID: event.sessionID });
          const work = [...delegations.values()].find((entry) => entry.parentSessionID === session.parentID && session.title === `Accounting ${entry.role}`);
          if (work) await bindWorker(session, work);
        }
        const work = [...delegations.values()].find((entry) => entry.sessionId === event.sessionID);
        if (work && event.tools) {
          const allowed = work.blind ? [] : work.fixer ? ['read', 'glob', 'patch', 'edit', 'write'] : ['read', 'glob'];
          for (const id of Object.keys(event.tools)) if (!allowed.includes(id)) delete event.tools[id];
        }
        const bundle = frozen.get(event.sessionID) ?? await knowledge(directory);
        event.system.push({ type: 'text', text: `Approved repository lessons (apply when relevant):\n${bundle}` });
      });
    }
    const execute = async ({ role, prompt, knowledge: bundle, fixer, model, parentSessionID }) => {
      const blind = role.startsWith('blind-') || role === 'evaluation-judge';
      const context = contexts.get(parentSessionID);
      const subagent = (await ctx.tool.list()).find((tool) => tool.id === 'subagent');
      if (!context || !subagent) throw new Error('Native subagent delegation unavailable; standalone sessions are not permitted');
      const token = randomUUID();
      const work = { role, bundle, fixer, blind, parentSessionID };
      delegations.set(token, work);
      try {
        const delegated = await subagent.execute({ agent: fixer ? 'accounting-fixer' : 'accounting-reviewer', description: `Accounting ${role}`,
          prompt: `ACCOUNTING_WORKER:${token}\n${prompt}`, background: false,
          ...(model ? { model: `${model.providerID}/${model.id}${model.variant ? `#${model.variant}` : ''}` } : {}),
        }, context);
        if (!work.sessionId) throw new Error(`Subagent did not bind a child worker: ${JSON.stringify(delegated)}`);
        const info = await ctx.session.get({ sessionID: work.sessionId });
        const messages = await ctx.session.context({ sessionID: work.sessionId });
        const assistant = messages.filter((message) => message.type === 'assistant').at(-1);
        return { text: assistant?.content.filter((part) => part.type === 'text').map((part) => part.text).join('\n') ?? '',
          sessionId: work.sessionId, parentSessionID: info.parentID, cost: info.cost, messages,
          failed: info.outcome !== 'succeeded' || !assistant || Boolean(assistant.error) || assistant.finish !== 'stop' };
      } catch (error) {
        if (!work.sessionId) throw error;
        const info = await ctx.session.get({ sessionID: work.sessionId });
        return { text: error.message, failed: true, sessionId: work.sessionId, parentSessionID: info.parentID,
          cost: info.cost, messages: await ctx.session.context({ sessionID: work.sessionId }) };
      } finally { if (work.sessionId) frozen.delete(work.sessionId); delegations.delete(token); }
    };
    return { contexts, execute };
}

export default {
  id: 'simple-accounting.review-controller',
  async setup(ctx) {
    const { contexts, execute } = await createWorkers(ctx);
    const controller = new Controller(root, execute);
    const withCallerModel = async (input, context) => {
      const caller = await ctx.session.get({ sessionID: context.sessionID });
      const messages = await ctx.session.context({ sessionID: context.sessionID });
      const defaultModel = caller.model ?? messages.filter((message) => message.type === 'assistant').at(-1)?.model;
      if (!defaultModel) throw new Error('Caller model unavailable; select a model before running the controller');
      return { ...input, defaultModel, parentSessionID: context.sessionID };
    };
    const run = async (method, input, context) => {
      contexts.set(context.sessionID, context);
      try { return { content: JSON.stringify(await controller[method](await withCallerModel(input, context)), null, 2) }; }
      finally { contexts.delete(context.sessionID); }
    };
    await ctx.tool.transform((editor) => {
      editor.add({
        name: 'accounting_review', description: 'Run Gradle validation and bounded specialist review/fix cycles. Original requirements and pre-edit base SHA are required. Never commits or publishes.',
        input: schema({ requirements: string, base: string, profile: { type: 'string', enum: ['harness', 'frontend', 'backend', 'full'] }, runId: string, clarification: string }, ['requirements', 'base', 'profile']),
        execute: async (input, context) => run('review', input, context),
      });
      editor.add({
        name: 'accounting_learn', description: 'Investigate human corrections against original review artifacts; independently verify and blind-evaluate before promoting lessons. Does not implement feedback or commit.',
        input: schema({ runId: string, feedback: string }, ['runId', 'feedback']),
        execute: async (input, context) => run('learn', input, context),
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
