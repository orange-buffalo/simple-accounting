---
name: sa-feedback
description: Use when human feedback identifies a defect or preference missed during an earlier Simple Accounting implementation or review.
---

# Verified feedback learning

Follow the human-feedback section of `sa-delivery`. Inspect the original run before changing its evidence. Investigate EVERY human comment; never ignore, dismiss or reject one autonomously. If a comment appears incorrect or conflicting, ask the user with arguments, reasoning and concrete options before choosing a resolution. New requirements and preferences are lessons too, not grounds to skip learning.

Commit each implementation turn's completed changes separately under `AGENTS.md`, including feedback corrections. Explicit user instructions override this default: a no-commit instruction suspends automatic commits until authorized. This precedence is not an error or reviewer defect. Scoped authorization covers only its stated scope.

Call `sa_learn` once for the complete indexed feedback batch before reviewing, so verified lessons are deployed to reviewers. Distinct user feedback starts a fresh two-round turn on the original run; review cumulative changes with refreshed lessons, including analogous issues in earlier-turn code. Duplicate feedback and ordinary continuation do not reset limits. Pass JSON in `feedback`: `{"kind":"user-confirmed-policy","items":[{"ref":"F1","file":"path","line":1,"comment":"exact initial user comment","action":"concise applied correction"}]}` for explicit human instructions/preferences. Use `review-improvement` only when claiming measured reviewer improvement, with the additional blind evaluation gates. Every item requires an investigated disposition and preventive lesson. A candidate with `requiresUser` is unresolved: present its questions with evidence and options, never hand it over as dismissed or completed.

After addressing feedback, present the returned report table: ref index, file:line, initial user comment, agent action (correction and lesson status). Preserve stable references across follow-up discussions and carry earlier unresolved comments forward.

Learning is not a model weight update. Only registry-approved, independently verified lessons influence future agents. Explicit human policy can be learned without inventing historic reviewer failure or proving a baseline miss; this is not measured reviewer improvement. Unsupported proposals remain unpromoted and require user clarification, not unilateral dismissal.
