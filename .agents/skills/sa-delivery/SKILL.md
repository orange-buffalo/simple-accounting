---
name: sa-delivery
description: Use when implementing or changing Simple Accounting, including fixes requested in human review; automatically verify and review before handing work over.
---

# SA delivery

The repository controller owns the review/fix loop. Do not substitute an informal self-review or invoke CE's publishing workflows.

1. Read `AGENTS.md` and `docs/AgentHarness.md`. Clarify requirements that cannot safely be inferred. Record acceptance criteria and the current `git rev-parse HEAD` **before editing**. Preserve the original criteria, including subsequent user clarifications, in the review request. For a review of existing branch commits, use their explicitly selected base instead of HEAD.
2. Implement using this repository's conventions and appropriate tests. Preserve unrelated changes. Never invoke Bun directly. Regenerate GraphQL artifacts with the Gradle tasks in `AGENTS.md`.
3. Call `sa_review` with the original requirements, recorded base SHA, and validation profile. Prefer `targeted` with explicit fully qualified `testClasses` when affected backend/API/full-stack scenarios can be identified, even for local GraphQL or layout changes; inspect related rendering reports. Use `full` only when the blast radius cannot reasonably be covered by focused classes (e.g. widespread shared infrastructure changes). `harness` is only for changes wholly contained in the agent harness/docs. The controller runs Gradle, specialist review, independent finding validation, bounded fixes, and re-review automatically.
4. Hand over the returned status, run ID, evidence paths, and unresolved findings. A blocked/incomplete run is not complete work. Do not silently extend the budget, restart under a new run ID, or fix after a successful receipt without a fresh review. Resume after a restart only with the existing run ID.
5. Do not commit, push, publish, or merge unless requested. The existing rule about committing prior work **before addressing human review feedback** still applies. Use the existing PR/release skills only on explicit request.

## Human review feedback

For corrections to an earlier reviewed implementation, first retain its run ID. Follow the existing commit-before-feedback rule, apply accepted corrections, and review the corrected state. Then call `sa_learn` with the original run ID and the human feedback. It investigates raw reviewer and synthesis artifacts, independently checks the findings, evaluates proposed guidance against blind replay/variation/clean cases, and records a verified lesson or an unpromoted candidate. Do not add an unverified candidate to `AGENTS.md` or the approved knowledge registry.

If a comment adds a new requirement rather than identifying a miss, record it as specification evolution. If no original run exists, acknowledge the missing evidence; never invent a reviewer failure explanation.

All delegated implementation/review agents must receive the current approved knowledge bundle. On other harnesses without the executable controller, report automation unavailable rather than pretending these steps were enforced.
