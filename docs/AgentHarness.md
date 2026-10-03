# Repository agent workflow

This repository combines checksum-pinned Compound Engineering reference skills with a local OpenCode V2 controller.
It is not CE's `lfg` pipeline and does not commit, push, create PRs, or merge. Existing PR/release skills remain opt-in.

## Setup

Requires OpenCode V2 (tested with 2.0.18), an authenticated model, Node.js 22+, Python 3, Git, and the project prerequisites
(Java 25, Bun through Gradle, Docker, Playwright). No global skill installation is needed.

```sh
./gradlew bootstrapAgentHarness doctorAgentHarness checkAgentHarness --console=plain
```

Bootstrap downloads only the four skills in `.harness/upstream.json`, their references, and the upstream MIT license.
It verifies the locked archive checksum and never executes upstream scripts. The ignored `.harness/vendor/` directory
can be recreated on every machine. Changing upstream versions requires deliberately updating both revision and digest.
Reload the OpenCode project location after setup or configuration edits. Global/ancestor configuration still applies;
repository configuration is not a sandbox against the host or the primary implementation agent.

## Everyday use

Ask for an implementation normally, or use `/accounting-implement <request>`. `AGENTS.md` routes implementation work
through `simple-accounting-delivery`. Save the pre-edit base commit and explicit acceptance criteria. Before handover,
the agent calls `accounting_review(requirements, base, profile)`.
When the user steers an active task, preserve the original contract and add their change through the optional
`clarification` field on the same run; this does not reset its budgets.

The controller:

1. Freezes original requirements, approved knowledge, policy, and the changed-file snapshot (including untracked files).
2. Runs the profile's Gradle tasks in one invocation, never invoking Bun directly or cancelling a build.
3. Launches read-only requirements, quality, security, consistency, and UX reviewers as native subagents of the calling
   session. Fixers and learning workers are also children, not standalone top-level sessions.
4. Independently validates findings; missing coverage or failed reviewers block handover.
5. Applies validated findings through a restricted fixer, then validates and re-reviews all axes.
6. Stops at a clean receipt, a blocker, or the configured repair budget. No automatic budget extension.

Configure `.harness/policy.json`: `maxFixCycles` (0–5), elapsed minutes, USD cost, reviewers, validation profiles, and
optional per-role model references using OpenCode's `{ "providerID": "...", "id": "..." }` shape. Without overrides,
workers inherit the calling session's active model. Time and cost limits are checked **between operations**; an in-flight
model session or build may exceed them. These are not hard spend caps. The controller serializes builds/repair runs
within a worktree via a lock, but other tools/processes can still modify files; review detects snapshot drift.

Profiles: `frontend`, `backend`, `full`; `harness` only for agent-workflow/docs changes, not application/build changes.
Use full for GraphQL, shared infrastructure, or unclear blast radius. UI rendering still requires the related full-stack
test and manual inspection of generated PNGs as specified in `AGENTS.md`; reviewers must report missing evidence.
`full` runs the repository's required `assemble check` sequence, including its regular full-stack tests. The separate
Docker distribution E2E task is not included: the existing task currently reports NO-SOURCE locally and must not be
represented as executed E2E coverage. Configure and validate that task separately if distribution behavior is in scope.
Fixers cannot change harness, CI, AGENTS, or build configuration and cannot run shell commands. Generated GraphQL files,
protected changes, and failed validation require the primary agent's explicit repair and resume with the **same run ID**.

Artifacts are private under `.harness/runtime/<run-id>/`: state, snapshots, exact worker prompts/transcripts, knowledge
hashes, findings/dispositions, validation logs, and receipts. Treat them as potentially sensitive source data; do not
commit or publish them. A passed receipt applies only to its recorded fingerprint. New edits require new review.
After an interrupted process leaves `controller.lock`, inspect its PID and active sessions/builds before manually
removing that **single stale file**. Never delete active-run locks or reset budgets to work around a blocker.

Workers use glob plus read for investigation. Grep is denied because OpenCode's grep permission resource is the search
pattern, not the paths traversed; a path-based secret deny would not protect recursive searches. Read denies cover
`.env*`, `.test-config.yaml`, `.pem`, and `.key` files for both reviewers and fixers.

## Verified human-feedback learning

Use `/accounting-feedback <original run ID and corrections>`. First follow the existing commit-before-feedback rule,
apply accepted corrections, and review the corrected state. Then invoke `accounting_learn(runId, feedback)`.

The learning controller examines the original snapshots and actual review artifacts, classifies corrections, and treats
reviewer-miss explanations as hypotheses. A fresh verifier checks source grounding and the proposed prevention. Fresh
read-only sessions then review a blind original-defect replay, a distinct analogous case, and a clean counterexample
with old and proposed guidance. When intervening lessons exist, the cumulative deployed bundle is also tested before
promotion. A separate evaluator checks outcomes. Promotion requires baseline replay failure,
candidate success on all three cases, independent grounding, and complete feedback dispositions.

Approved lesson JSON and its integrity digest are added to `.harness/lessons/` and `.harness/knowledge.json` (uncommitted).
Review these changes like code. Unverified proposals stay in runtime artifacts and never enter agent instructions.
Missing original runs, new requirements, invalid feedback, baseline already succeeding, and inconclusive evaluations
must not be described as proven reviewer improvement. Synthetic cases and a single stochastic trial are weak evidence,
not proof that recurrence has fallen. Prefer adding executable regressions alongside guidance.

The plugin injects the complete small approved bundle before every agent-loop/generate model call, including delegated
workers. It fails rather than silently truncating oversized bundles. Review workers receive the run's frozen bundle;
blind baseline/candidate workers receive their explicitly selected bundle, not the newly approved one.
Blind workers and evaluators have all tools denied at session level, preventing checkout/history reads through tools.
They still receive host system instructions; this is not a fully hermetic evaluation sandbox and host prompts can
influence outcomes. An interrupted model operation blocks resume until its session and cost are reconciled.

Track escaped defects and repeated defects per applicable task exposure, blind recall/false positives, run costs,
knowledge hashes, and unresolved coverage over subsequent real tasks. The stored evidence enables comparison; it does
not claim automatic model training or demonstrated long-term improvement before there are real feedback runs.

## Limitations and validation

Automation begins when the delivery skill calls the controller; it is not an unconditional server-side handover gate.
Host permissions, global plugins, or a primary agent bypass can weaken enforcement. Unsupported binary/large review
files and secret-like paths block rather than silently disappear. All model output is untrusted and schema-checked.
The fake-agent regression suite covers controller mechanics, not model quality. `checkAgentHarness` is included in
Gradle `check`; bootstrap/doctor remain explicit to keep ordinary CI offline and independent of model credentials.
CE skills are references only and are subordinate to this repository's validation, permissions, and publishing rules.
