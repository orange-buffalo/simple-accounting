import { fileURLToPath } from 'node:url';
import path from 'node:path';
import { readFile } from 'node:fs/promises';
import { randomUUID } from 'node:crypto';
import { Controller, knowledge, hash } from './core.js';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const schema = (properties, required) => ({ type: 'object', properties, required, additionalProperties: false });
const string = { type: 'string', minLength: 1 };
const workerPrompt = (text) => text?.replace(/^You are a subagent spawned by another session\.\n/, '');
const workerToken = (text) => workerPrompt(text)?.match(/^SA_WORKER:([a-f0-9-]+)\n/)?.[1];
const unwrap = (response) => response?.data ?? response;

export async function createWorkers(ctx, directory = root) {
  const frozen = new Map();
  const delegations = new Map();
  const contexts = new Map();
  const bindWorker = async (session, work) => {
    if (session.parentID !== work.parentSessionID) throw new Error('SA worker must be a child of the calling session');
    if (work.sessionId && work.sessionId !== session.id) throw new Error('SA worker token is already bound to another session');
    work.sessionId = session.id;
    const agent = await ctx.agent.get({ agentID: work.fixer ? 'sa-fixer' : 'sa-reviewer' });
    const permissions = agent.data?.permissions ?? agent.permissions;
    if (!Array.isArray(permissions)) throw new Error('Worker permission rules unavailable');
    await ctx.session.update({ sessionID: session.id, permissions: work.blind
      ? [{ action: '*', resource: '*', effect: 'deny' }] : [...permissions,
        { action: 'read', resource: '**/.harness/runtime/**', effect: 'deny' },
        ...work.artifactReads,
        { action: 'read', resource: '**/build/test-results/**', effect: 'deny' },
        { action: 'read', resource: '**/build/reports/**', effect: 'deny' },
        { action: 'read', resource: '**/*.log', effect: 'deny' },
      ] });
    frozen.set(session.id, work.bundle);
  };
  await ctx.session.hook('prompt', async (event) => {
    const token = workerToken(event.prompt.text);
    const work = token && delegations.get(token);
    if (!work) return;
    const session = unwrap(await ctx.session.get({ sessionID: event.sessionID }));
    await bindWorker(session, work);
    event.metadata = { ...event.metadata, saRole: work.role, knowledgeHash: hash(work.bundle), saParent: work.parentSessionID };
  });
  for (const kind of ['context', 'generate']) {
    await ctx.session.hook(kind, async (event) => {
      let token;
      if (!frozen.has(event.sessionID) && ctx.session.get) {
        const session = unwrap(await ctx.session.get({ sessionID: event.sessionID }));
        if (session?.parentID) {
          const messages = unwrap(await ctx.session.context({ sessionID: event.sessionID }));
          token = workerToken(messages.find((message) => message.type === 'user')?.text);
          const work = token && delegations.get(token);
          if (work) await bindWorker(session, work);
        }
      }
      const work = [...delegations.values()].find((entry) => entry.sessionId === event.sessionID);
      if (token && !frozen.has(event.sessionID)) {
        if (event.tools) for (const id of Object.keys(event.tools)) delete event.tools[id];
        await ctx.session.update({ sessionID: event.sessionID, permissions: [{ action: '*', resource: '*', effect: 'deny' }] });
        throw new Error('SA worker has no active restricted binding');
      }
      if (work && event.tools) {
        const allowed = work.blind ? [] : work.fixer ? ['read', 'glob', 'patch', 'edit', 'write'] : ['read', 'glob'];
        for (const id of Object.keys(event.tools)) if (!allowed.includes(id)) delete event.tools[id];
      }
      if (work) event.system.push({ type: 'text', text: 'You are a delegated review/fix/learning worker, not the primary implementation agent. Never run builds/tests or inspect their results, logs, pass/fail status or CI status. Inspect source-level behavior and assertions only. Visual artifacts may be inspected for usability, never for test-pass verification. Perform your assigned responsibility independently; paired reviews intentionally overlap.' });
      const bundle = frozen.get(event.sessionID) ?? await knowledge(directory);
      event.system.push({ type: 'text', text: `Approved repository lessons: apply relevant constraints BEFORE implementation, not only during review. The complete active catalog follows; preserve its scopes and exceptions.\n${bundle}` });
    });
  }
  const readWorker = async (sessionId, parentSessionID) => {
    const info = unwrap(await ctx.session.get({ sessionID: sessionId }));
    if (info.id !== sessionId || info.parentID !== parentSessionID) throw new Error('SA worker session or parent mismatch');
    return info;
  };
  const waitForWorker = async (sessionId) => ctx.session.wait({ sessionID: sessionId });
  const releaseWorker = (token) => {
    const work = delegations.get(token);
    if (work?.sessionId) frozen.delete(work.sessionId);
    delegations.delete(token);
  };
  const reconcile = async ({ sessionId, parentSessionID, prompt }) => {
    let info = await readWorker(sessionId, parentSessionID);
    const messages = unwrap(await ctx.session.context({ sessionID: sessionId }));
    const text = workerPrompt(messages.find((message) => message.type === 'user')?.text);
    const token = workerToken(text);
    if (!token || text !== `SA_WORKER:${token}\n${prompt}`) throw new Error('SA worker recovery prompt mismatch');
    if (!info.outcome) {
      const work = delegations.get(token);
      if (work && !frozen.has(sessionId)) await bindWorker(info, work);
      if (!frozen.has(sessionId)) {
        await ctx.session.update({ sessionID: sessionId, permissions: [{ action: '*', resource: '*', effect: 'deny' }] });
        await ctx.session.interrupt({ sessionID: sessionId, resume: false });
      }
      await waitForWorker(sessionId);
      info = await readWorker(sessionId, parentSessionID);
    }
    if (!info.outcome) throw new Error('SA worker has no terminal outcome');
    if (delegations.get(token)?.sessionId === sessionId) releaseWorker(token);
    return { cost: info.cost, outcome: info.outcome };
  };
  const execute = async ({ role, prompt, knowledge: bundle, fixer, model, artifactRoots = [], parentSessionID, recordSession }) => {
    if (!fixer && !model) throw new Error('An explicit configured reviewer model is required');
    const artifactReads = artifactRoots.map((artifactRoot) => {
      const relative = path.relative(path.join(directory, '.harness/runtime'), path.resolve(artifactRoot));
      if (!/^[a-zA-Z0-9_-]+\/(sources|original-evidence)$/.test(relative)
        || (relative.endsWith('/original-evidence') && !['feedback-analysis', 'learning-validator'].includes(role))) {
        throw new Error('Invalid delegated artifact root');
      }
      return { action: 'read', resource: `**/.harness/runtime/${relative}/**`, effect: 'allow' };
    });
    const blind = role.startsWith('blind-') || role === 'evaluation-judge';
    const context = contexts.get(parentSessionID);
    const subagent = (await ctx.tool.list()).find((tool) => tool.id === 'subagent');
    if (!context || !subagent) throw new Error('Native subagent delegation unavailable; standalone sessions are not permitted');
    const token = randomUUID();
    const work = { role, bundle, fixer, blind, artifactReads, parentSessionID };
    delegations.set(token, work);
    let sessionId;
    let terminal = false;
    try {
      const delegated = await subagent.execute({ agent: fixer ? 'sa-fixer' : 'sa-reviewer', description: `SA ${role}`,
        prompt: `SA_WORKER:${token}\n${prompt}`, background: false,
        ...(model ? { model: `${model.providerID}/${model.id}${model.variant ? `#${model.variant}` : ''}` } : {}),
      }, context);
      sessionId = delegated?.output?.sessionID ?? work.sessionId;
      if (!sessionId) throw new Error('Native delegation returned no child session ID');
      work.sessionId ??= sessionId;
      await recordSession?.(sessionId);
      await readWorker(sessionId, parentSessionID);
      if (delegated?.output?.status === 'running') await waitForWorker(sessionId);
      const info = await readWorker(sessionId, parentSessionID);
      if (!info.outcome) throw new Error('Native child has no terminal outcome');
      terminal = true;
      if (work.sessionId !== sessionId || !frozen.has(sessionId)) {
        return { text: 'Native child did not bind its worker restrictions', failed: true, sessionId,
          parentSessionID, cost: info.cost };
      }
      const messages = unwrap(await ctx.session.context({ sessionID: sessionId }));
      const assistant = messages.filter((message) => message.type === 'assistant').at(-1);
      return { text: assistant?.content.filter((part) => part.type === 'text').map((part) => part.text).join('\n') ?? '',
        sessionId: work.sessionId, parentSessionID: info.parentID, cost: info.cost, outcome: info.outcome, messages,
        failed: info.outcome !== 'succeeded' || !assistant || Boolean(assistant.error) || assistant.finish !== 'stop' };
    } catch (error) {
      if (!sessionId && work.sessionId) await recordSession?.(work.sessionId);
      return { text: error.message, failed: true, sessionId: sessionId ?? work.sessionId, parentSessionID };
    } finally {
      // An interrupted wait does not mean the child stopped running.
      if (terminal || !work.sessionId) releaseWorker(token);
    }
  };
  return { contexts, execute, reconcile };
}

export default {
  id: 'sa-review-controller',
  async setup(ctx) {
    const { contexts, execute, reconcile } = await createWorkers(ctx);
    const controller = new Controller(root, execute, reconcile);
    const run = async (method, input, context) => {
      contexts.set(context.sessionID, context);
      try { return { content: JSON.stringify(await controller[method]({ ...input, parentSessionID: context.sessionID }), null, 2) }; }
      finally { contexts.delete(context.sessionID); }
    };
    await ctx.tool.transform((editor) => {
      editor.add({
        name: 'sa_review', description: 'After orchestrator self-review, all relevant tests completion and own verifications, run two identical full reviews and two prevention compliance reviews in parallel. Repairs return awaiting-verification; complete preflight again before resuming the same run. At most two rounds per turn. Never runs tests, commits or publishes.',
        input: schema({ requirements: string, base: string, profile: { type: 'string', enum: ['harness', 'frontend', 'backend', 'targeted', 'full'] }, testClasses: { type: 'array', items: string }, runId: string, clarification: string,
          preflight: schema({ selfReviewCompleted: { type: 'boolean' }, testsCompleted: { type: 'boolean' }, verificationsCompleted: { type: 'boolean' } }, ['selfReviewCompleted', 'testsCompleted', 'verificationsCompleted']) }, ['requirements', 'base', 'profile']),
        execute: async (input, context) => run('review', input, context),
      });
      editor.add({
        name: 'sa_learn', description: 'Diagnose every human correction, reuse or curate compact preventive rules, and independently verify their scope before publication. Does not implement feedback or commit; verification is not measured improvement.',
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
