# Repository agent workflow

This repository combines checksum-pinned Compound Engineering reference skills with a local OpenCode V2 controller.
It is not CE's `lfg` pipeline. The controller does not commit, push, create PRs, or merge. The primary agent commits
each implementation turn separately under `AGENTS.md`; explicit user instructions override this default.
Existing PR/release skills remain opt-in.

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

### Trivial-removal exception

Trivial, behavior-preserving removals do not launch `sa_review`. For example, remove test-local viewport overrides
while retaining shared defaults, rendering captures and business assertions. Self-review the diff, check analogous
occurrences, and run the smallest relevant validation instead; report that full review was intentionally skipped.
Human-feedback learning and normal commit requirements still apply. This exception does not cover production behavior,
access/security, API/schema, persistence, test assertion changes, or executable harness changes. When uncertain, use the
normal review workflow. Explicit user instructions about review take precedence.

Ask for an implementation normally, or use `/sa-implement <request>`. `AGENTS.md` routes implementation work
through `sa-delivery`. Save the pre-edit base commit and explicit acceptance criteria. Before EACH review round,
finish self-review, wait for all relevant tests to complete, check their results, and finish all own verifications.
Only then call the tool with an object containing all three completed preflight attestations:

```js
sa_review({
  requirements,
  base,
  profile,
  preflight: {
    selfReviewCompleted: true,
    testsCompleted: true,
    verificationsCompleted: true,
  },
})
```

After controller repairs, repeat self-review and all applicable validation and verification, then resume with
the returned `runId` and fresh preflight attestations on the SAME run; do not start a new run or review while checks
are pending.
When the user steers an active task, preserve the original contract and add their change through the optional
`clarification` field on the same run. Distinct new human review feedback starts a fresh review turn, resetting its
round, repair, elapsed-time and cost limits while retaining total usage, original base and all evidence. Whitespace-equivalent
duplicate feedback and ordinary continuation do not renew limits. Never invent feedback to work around a blocker.
Passed, blocked and exhausted runs can start a new feedback turn; unreconciled workers still prevent resumption.

The controller:

1. Preserves original requirements and base, freezes policy and the changed-file snapshot (including untracked files),
   and refreshes approved knowledge for each new feedback turn. Review cumulative changes, not only the latest delta:
    apply learned instructions to analogous issues in relevant code from earlier turns of the session.
    On feedback turns, both full reviewers must record a source-grounded per-file feedback audit for every cumulative
    non-generated code file, including earlier-turn changes. Missing or duplicate file dispositions block a clean
    receipt. Reviewers must justify exceptional mocks and other deviations rather than stopping at the commented file.
2. Writes a changed-source index with per-file diff/source artifacts. Workers read only relevant files rather than
    receiving the complete snapshot and duplicated file contents in every prompt.
    Snapshot evidence size is independent of reviewer context: the context limit applies to the source index, not the
    aggregate full-file contents stored privately on disk. Individual-file safety limits still apply.
3. Requires the primary implementation agent (orchestrator) to finish self-review, wait for ALL relevant tests to
   complete, check results, and finish all own verifications before EVERY round. Resolve failures first; never launch
   reviews alongside pending tests or verification. Call with `preflight: { selfReviewCompleted: true,
   testsCompleted: true, verificationsCompleted: true }` only after completion. Missing/false attestations return
   `awaiting-verification` without consuming a round or launching workers. These attestations are an agent-owned gate,
   not independent proof of execution; the controller does not inspect results or logs.
   Then launches four read-only reviewers **in parallel** as native subagents of the calling session:
   - **review-1 / review-2**: two independent full reviews with identical prompts, model and permissions. Each covers
     functional requirements and source-level assertions (branches, boundaries, failures, regressions), security/access
     rules and positive/negative coverage, repository conventions, and applicable UX/accessibility/i18n/feedback.
     Non-UI changes explicitly justify UX N/A. Neither reviewer excludes aspects assigned to the other.
   - **compliance-1 / compliance-2**: two independent compliance reviews of ALL cumulative changes against EVERY rule
     in the frozen prevention.json bundle, preserving scopes and exceptions. Each returns source-grounded compliance
     or justified N/A evidence for every rule ID, with fileAudit groups disposing of every cumulative changed file
     exactly once per rule. Group files only when they share the same source-grounded disposition. Missing, duplicate,
     unknown or unevidenced rule/file dispositions block a clean receipt.
   Fixers and learning workers are also children, not standalone top-level sessions.
4. Independently validates only submitted findings and deduplicates them; missing source coverage or failed reviewers
    prevent a clean review receipt. The validator is not another general reviewer.
5. Applies validated findings through a restricted fixer, then returns `awaiting-verification` with
   `nextAction: self-review-and-complete-verifications-then-resume`. It NEVER launches the next review batch automatically.
   The orchestrator repeats self-review, completes validation and all own checks, then resumes the SAME run with fresh
   preflight attestations. All four reviewers run again against the updated cumulative snapshot.
6. Stops at a clean receipt, a blocker, or the configured repair budget, with at most two review rounds per turn
    (initial review and one re-review), including resumed attempts in that turn. Distinct human feedback starts a fresh
    turn on the same run; it does not erase prior rounds, repairs, receipts, knowledge bundles or usage.
   After the controller returns, the primary implementation agent must rerun its selected Gradle validation and check
   results before handover if repairs occurred (`fixes > 0`), including applicable regenerated rendering reports. Any
    subsequent source fix invalidates a passed receipt; resume with the same ID when rounds remain.
    Exhausting rounds returns `review-exhausted` with `nextAction: finish-implementation-and-validation`.
    The primary agent must finish required fixes and validation, not stop or ask permission solely because review
    rounds ran out. Report the two-round limit, receipt status and any remaining issues without claiming a clean review.
     Genuine ambiguity and external blockers still require user direction. Never start another run to evade a turn's cap.

Configure `.harness/policy.json`: `maxReviewRounds` (1–2, default 2), `maxFixCycles` (0–5, default 1, still bounded by
the per-turn two-round limit), elapsed minutes, USD cost, the four required `reviewTopology` roles (`review-1`, `review-2`,
`compliance-1`, `compliance-2`), and
`reviewerModel`, defaulting to `{ "providerID": "openai", "id": "gpt-6.1-sol", "variant": "medium" }`.
Optional `models` entries override other worker roles with the same model-reference shape; overrides for the four
review roles are rejected to keep paired configurations identical. All read-only workers,
including finding and learning verifiers, receive an explicit configured model; they never inherit the caller's model.
Fixers may inherit it unless overridden. Time and cost limits are checked **between operations**; an in-flight parallel
batch may exceed them. These are not hard spend caps. The controller awaits every worker in a batch, accounts for all
reported costs and saves separate artifacts before proceeding or blocking. Interrupted/unaccounted workers prevent resume.
The worktree lock serializes controller invocations, not reviewers within a run. Review detects snapshot drift.
The stored `reviewers` list remains a legacy compatibility bridge; updated consumers use `reviewTopology` instead.
When `reviewTopology` is present, both lists are validated independently; an invalid legacy bridge fails closed.
Old loaded consumers can still read policy without rejecting the new role names, but cannot implement the new gate.
Reload the project location to activate updated plugin code before reviewing. Keep the bridge until all old consumers
have reloaded. On resumption, legacy four-specialist runs adopt the paired topology while retaining prior evidence,
the frozen model, knowledge, usage and per-turn limits. Conflicting frozen per-role model overrides fail closed with
explicit reconciliation required; migration never silently discards them or resets budgets.

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

## Prevention before implementation

`.harness/prevention.json` is the active, curated experience catalog. Each rule has a stable `id`, `category`,
`appliesWhen`, actionable `instruction`, a brief `reason`, and an optional useful `example`. Apply its relevant
constraints **before coding**. The plugin injects every rule before model calls; review workers freeze that complete
bundle for their turn. There is no heuristic relevance filter and no silent truncation. Validation and publication
use exactly the same projection and size limit as injection.

The initial migration accounts for all 67 original feedback instructions (F1–F68, with no F47).
`.harness/migrate-knowledge.mjs` records their explicit destinations. Forty scoped rules consolidate repeated guidance;
four additional rules capture this prevention workflow and safe live-consumer migration. F14/F20's obsolete lifetime
limits are replaced by F51's per-feedback-turn limits, retaining duplicate-feedback protections. General guidance
about authorization/version precedence, legacy repairs, evidence accuracy and verification limitations is retained.
The old detailed investigations remain recoverable from Git rather than being part of the active catalog.

Stage a new format separately before changing any file consumed by a running hook. Make the loader compatible first,
then reload the project location and confirm the new tools/hooks are active before retiring old data. The empty
version-one `.harness/knowledge.json` is a compatibility bridge for already-loaded old hooks: they remain runnable,
but must reload to consume the new catalog. Until reload, read the active catalog directly under `AGENTS.md`.
The loader retains version-one support for unmigrated worktrees; only those worktrees use the legacy learning path.

## Human-feedback learning

Use `/sa-feedback <original run ID and corrections>`. Apply accepted corrections, then invoke `sa_learn(runId, feedback)`
before reviewing so verified lessons reach the new turn's reviewers. Resume the original run with distinct feedback as
clarification and review cumulative changes against its original base. Commit the completed turn separately under `AGENTS.md`.
Explicit user instructions override this default. A no-commit instruction suspends automatic commits until authorized;
observing that precedence is not an error or issue. Scoped authorization covers only its stated scope.

Every human comment must be investigated and included in learning; new scope or preferences cannot be dismissed.
If feedback appears wrong or conflicts with evidence, the agent must ask the user with arguments, reasoning and
concrete options before resolving it. Unverified learning returns `requiresUser`, clarification questions and a private
`feedback-report.md`; it is unresolved work, not a completed or dismissed correction. No unverified lesson is injected.

Pass an indexed JSON batch as the `feedback` string: `{"kind":"user-confirmed-policy","items":[{"ref":"F1",
"file":"path","line":1,"comment":"exact initial comment","action":"applied correction"}]}`. Stable refs and exact
comments ensure every item has an investigated disposition, corrective action and prevention remedy. Explicit new
preferences are valid without pretending they were earlier requirements. The legacy `review-improvement` label remains
accepted, but is not a measurement claim. Omitted, changed, disputed or dismissed comments block publication.

After addressing human review, always present the returned table with columns **ref index**, **file:line**,
**initial user comment**, and **agent action**, including fixes and recorded lessons or unresolved status. Carry forward
all unresolved earlier feedback in the session. File references must identify real source locations.
Each verified row identifies its active rule IDs and catalog path, or a concrete workflow/automation remedy that
requires no new rule. Verification is distinguished from measured improvement.

Learning uses two focused workers: diagnosis/curation and independent verification. Diagnose missing knowledge,
failure to apply existing guidance, ambiguity/new requirements, or execution/tooling failure. Reuse existing rules,
clarify them, or improve the implementation workflow/automated safeguard before adding a new rule. Explicit upserts
and replacements preserve unrelated experience; retirement requires active replacements and a reason. The verifier
checks the entire resulting catalog for preserved scope, exceptions and conflicts. Nontrivial generalizations need an
analogous and a non-applicable case; simple preferences may justify why those checks are unnecessary. Blind replay
ceremonies are not required. Missing original runs are disclosed rather than used to invent a historical diagnosis.

Only compact approved rules are published, atomically and after checking for intervening catalog edits. Feedback,
original source packets and investigation details stay in ignored private runtime files; none enter ordinary prompts.
Candidates never change active knowledge. Review catalog edits like code. Learning is not a model-weight update.

For comparable tasks, inspect initial-round accepted defects separately from later review repairs, and count substantive
human corrections and recurring rule IDs in learning dispositions. Review rejected findings for false alarms rather than
treating every rejection as a false positive. Compare like task scopes/exposures and deduplicate repeated feedback by
original run/ref/comment. These lightweight observations use existing runtime state; no growing public audit ledger is
needed. Smaller files, verified rules and synthetic checks do not establish fewer real mistakes.

## Limitations and validation

Automation begins when the delivery skill calls the controller; it is not an unconditional server-side handover gate.
Host permissions, global plugins, or a primary agent bypass can weaken enforcement. Unsupported binary/large review
files and secret-like paths block rather than silently disappear. All model output is untrusted and schema-checked.
The fake-agent regression suite covers controller mechanics, not model quality. `checkAgentHarness` is included in
Gradle `check`; bootstrap/doctor remain explicit to keep ordinary CI offline and independent of model credentials.
CE skills are references only and are subordinate to this repository's validation, permissions, and publishing rules.
