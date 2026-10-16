---
name: sa-feedback
description: Use when human feedback identifies a defect or preference missed during an earlier Simple Accounting implementation or review.
---

# Verified feedback learning

Follow the human-feedback section of `sa-delivery`. Inspect the original run before changing its evidence. Investigate EVERY human comment; never ignore, dismiss or reject one autonomously. If a comment appears incorrect or conflicting, ask the user with arguments, reasoning and concrete options before choosing a resolution. New requirements and preferences are lessons too, not grounds to skip learning.

Commit each implementation turn's completed changes separately under `AGENTS.md`, including feedback corrections. Explicit user instructions override this default: a no-commit instruction suspends automatic commits until authorized. This precedence is not an error or reviewer defect. Scoped authorization covers only its stated scope.

Call `sa_learn` once for the complete indexed feedback batch before reviewing, so verified prevention reaches reviewers. Distinct feedback starts a fresh two-round turn on the original run; review cumulative changes with refreshed rules, including analogous earlier-turn issues. Duplicate feedback and ordinary continuation do not reset limits. Pass JSON in `feedback`: `{"kind":"user-confirmed-policy","items":[{"ref":"F1","file":"path","line":1,"comment":"exact initial user comment","action":"concise applied correction"}]}`. The legacy `review-improvement` label is accepted but does not establish measured improvement. Diagnose missing knowledge, ignored guidance, ambiguity/new preference or execution/tooling failure. Every item requires an action and prevention remedy; reusing an existing rule or changing the workflow is valid and avoids duplicate guidance. A candidate with `requiresUser` is unresolved: present its questions with evidence and options, never dismiss it.

After addressing feedback, present the returned report table: ref index, file:line, initial user comment, agent action (correction and lesson status). Preserve stable references across follow-up discussions and carry earlier unresolved comments forward.

Learning is not a model weight update. Only independently verified curation enters `.harness/prevention.json`; full private investigations remain in ignored runtime storage. Merge duplicates and replace superseded rules while preserving scope and exceptions. Check analogous and non-applicable cases where generalization is nontrivial; explicit preferences need no blind replay ceremony. Track actual recurring corrections and initial-review defects across comparable tasks rather than claiming improvement from verification alone. Unsupported proposals remain unpromoted and require clarification, not unilateral dismissal.
