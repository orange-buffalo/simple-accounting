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

Ask for an implementation normally, or use `/sa-implement <request>`. `AGENTS.md` routes implementation work
through `sa-delivery`. Save the pre-edit base commit and explicit acceptance criteria. Before handover,
the agent calls `sa_review(requirements, base, profile)`.
When the user steers an active task, preserve the original contract and add their change through the optional
`clarification` field on the same run. Genuinely new human feedback renews expired elapsed-time or exhausted cost
budgets, recording the prior budget epoch and retaining total usage and all evidence. Repeated feedback and ordinary
continuation do not renew budgets. The caller must supply actual new user feedback, not invent a clarification to
work around a blocker. Renewal never resets the two-round review limit or repair count.

The controller:

1. Freezes original requirements, approved knowledge, policy, and the changed-file snapshot (including untracked files).
2. Writes a changed-source index with per-file diff/source artifacts. Workers read only relevant files rather than
    receiving the complete snapshot and duplicated file contents in every prompt.
    Snapshot evidence size is independent of reviewer context: the context limit applies to the source index, not the
    aggregate full-file contents stored privately on disk. Individual-file safety limits still apply.
3. Launches four read-only reviewers **in parallel** as native subagents of the calling session, each with an exclusive scope:
   - **Functional**: every functional requirement implemented and covered by test assertions, including branches,
     conditions, boundaries, failures and regressions. Excludes security/access-rule coverage.
   - **Security**: authentication, authorization, workspace ownership/isolation and other data-access/security rules,
     with positive and negative test coverage. Excludes non-security functional behavior.
   - **Code**: repository patterns, standards, guidelines and consistency with surrounding code, including test conventions.
     Excludes behavior/test-coverage, security and usability checks.
   - **UX**: user-friendly, convenient, accessible UI, interaction flow, i18n and loading/error/empty feedback, when applicable.
     Excludes business-rule/test-coverage, security and code-convention checks; explicitly reports N/A for non-UI changes.
   Fixers and learning workers are also children, not standalone top-level sessions.
4. Independently validates only submitted findings and deduplicates them; missing source coverage or failed reviewers
    prevent a clean review receipt. The validator is not another general reviewer.
5. Applies validated findings through a restricted fixer, then re-reviews all axes in parallel.
6. Stops at a clean receipt, a blocker, or the configured repair budget, with at most two review rounds (initial review
    and one re-review), including resumed attempts. No automatic budget extension or restart to bypass this limit.
   After the controller returns, the primary implementation agent must rerun its selected Gradle validation and check
   results before handover if repairs occurred (`fixes > 0`), including applicable regenerated rendering reports. Any
    subsequent source fix invalidates a passed receipt; resume with the same ID when rounds remain.
    Exhausting rounds returns `review-exhausted` with `nextAction: finish-implementation-and-validation`.
    The primary agent must finish required fixes and validation, not stop or ask permission solely because review
    rounds ran out. Report the two-round limit, receipt status and any remaining issues without claiming a clean review.
    Genuine ambiguity and external blockers still require user direction. Never start another run to evade the cap.

Configure `.harness/policy.json`: `maxReviewRounds` (1–2, default 2), `maxFixCycles` (0–5, default 1, still bounded by
the two-round limit), elapsed minutes, USD cost, the four required reviewers, and
`reviewerModel`, defaulting to `{ "providerID": "openai", "id": "gpt-5.6-terra", "variant": "medium" }`.
Optional `models` entries override individual roles with the same model-reference shape. All read-only workers,
including finding and learning verifiers, receive an explicit configured model; they never inherit the caller's model.
Fixers may inherit it unless overridden. Time and cost limits are checked **between operations**; an in-flight parallel
batch may exceed them. These are not hard spend caps. The controller awaits every worker in a batch, accounts for all
reported costs and saves separate artifacts before proceeding or blocking. Interrupted/unaccounted workers prevent resume.
The worktree lock serializes controller invocations, not reviewers within a run. Review detects snapshot drift.

Profiles: `frontend`, `backend`, `targeted`, `full`; `harness` only for agent-workflow/docs changes, not application/build changes.
Profiles describe review scope, not validation tasks. Prefer `targeted` with fully qualified `testClasses` (up to 10)
for bounded API/backend/full-stack changes; these classes are source-navigation hints, never execution receipts.
The controller **never runs builds/tests or checks their results**. Workers never read build/test logs, result reports,
CI status or pass/fail receipts, and cannot execute verification commands. Functional/security coverage is assessed from
test source and assertions, not whether tests passed. The primary implementation agent runs and checks appropriate Gradle
validation under `AGENTS.md`, including after fixes; CI may independently perform mechanical validation as needed.
Do not include build/test results in review requests. UI rendering still requires the primary agent to run the related
full-stack test and inspect generated PNGs. UX may inspect visual artifacts only to assess an applicable usability concern,
not to verify test success. A clean review receipt does not claim successful builds/tests.
Fixers cannot change harness, CI, AGENTS, or build configuration and cannot run shell commands. Generated GraphQL files,
protected changes require the primary agent's explicit repair and resume with the **same run ID**.

Artifacts are private under `.harness/runtime/<run-id>/`: state, snapshots, exact worker prompts/transcripts, knowledge
hashes, findings/dispositions, changed-source packets, and receipts. Treat them as potentially sensitive source data; do not
commit or publish them. A passed receipt applies only to its recorded fingerprint. New edits require review when
rounds remain; after exhaustion, finish validation and disclose that the final source has no clean review receipt.
After an interrupted process leaves `controller.lock`, inspect its PID and active sessions before manually
removing that **single stale file**. Never delete active-run locks or reset budgets to work around a blocker.

Workers use glob plus read for investigation. Grep is denied because OpenCode's grep permission resource is the search
pattern, not the paths traversed; a path-based secret deny would not protect recursive searches. Read denies cover
`.env*`, `.test-config.yaml`, `.pem`, and `.key` files for both reviewers and fixers.
Worker read permissions also deny runtime state/transcripts, build test-result/report directories and `.log` files;
only the delegation's own changed-source packets are readable within runtime. Learning investigators/verifiers receive
an explicit grant for their lesson run's original-evidence files; other runs remain denied. Visual artifacts under
`app/build/rendering-report/` remain readable for UX. Host instructions and arbitrary user text are not a perfect sandbox;
do not paste build/test results into worker inputs.

## Verified human-feedback learning

Use `/sa-feedback <original run ID and corrections>`. First follow the existing commit-before-feedback rule,
apply accepted corrections, and review the corrected state. Then invoke `sa_learn(runId, feedback)`.
The automatic pre-feedback progress commit remains the default, but explicit user instructions override repository
workflow defaults. A no-commit instruction suspends this default until authorized; observing that precedence is not
an error or issue. Authorization to commit prior progress does not authorize committing new corrections.

Every human comment must be investigated and included in learning; new scope or preferences cannot be dismissed.
If feedback appears wrong or conflicts with evidence, the agent must ask the user with arguments, reasoning and
concrete options before resolving it. Unverified learning returns `requiresUser`, clarification questions and a private
`feedback-report.md`; it is unresolved work, not a completed or dismissed correction. No unverified lesson is injected.

Pass an indexed JSON batch as the `feedback` string: `{"kind":"user-confirmed-policy","items":[{"ref":"F1",
"file":"path","line":1,"comment":"exact initial comment","action":"applied correction"}]}`. Stable refs and exact
comments ensure every item has an investigated disposition, corrective action and preventive lesson. This mode learns
explicit human instructions through independent source/intent verification; it does not claim measured reviewer
improvement or require a preference to predate the user's instruction. Use `review-improvement` for claims requiring
the blind evaluation below. Omitted, changed, disputed or dismissed comments block promotion and require user direction.

After addressing human review, always present the returned table with columns **ref index**, **file:line**,
**initial user comment**, and **agent action**, including fixes and recorded lessons or unresolved status. Carry forward
all unresolved earlier feedback in the session. File references must identify real source locations.
Each verified row identifies its active instruction by lesson ID and feedback ref, with the exact preventive rule
and lesson file path. Policy verification is distinguished from measured reviewer improvement.

The deployed knowledge bundle includes every verified disposition's `ref` and `lesson` as an `instructions` entry,
alongside the general guidance and scope. This also activates detailed instructions in existing verified lesson files;
they are not merely archival evidence. The same projection is used for candidate evaluations and publication size
checks, so detailed instructions cannot disappear between learning, evaluation and agent delivery. Integrity checks
and the knowledge-size limit apply to the full bundle; no instructions are silently truncated.

The learning controller examines the original snapshots and actual review artifacts, keeping evidence size independent
of prompt context size. Its compact index points to per-round source packets, per-agent investigation manifests and
exact private tool transcripts; investigators read relevant artifacts rather than receiving duplicated tool inputs/outputs
inline. Evidence is preserved without truncation, and the index still has a context safety limit. Learning state records
the original run ID and feedback even when evaluation cannot start. The controller classifies corrections and treats
reviewer-miss explanations as hypotheses. A fresh verifier checks source grounding and the proposed prevention. Fresh
read-only sessions then review a blind original-defect replay, a distinct analogous case, and a clean counterexample
with old and proposed guidance. When intervening lessons exist, the cumulative deployed bundle is also tested before
promotion. A separate evaluator checks outcomes. Promotion requires baseline replay failure,
candidate success on all three cases, independent grounding, and complete feedback dispositions.

Independently verified human-policy lessons record the complete dispositions and their verification separately from
measured reviewer-improvement lessons; preferences are learned without inventing reviewer-miss explanations.
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
