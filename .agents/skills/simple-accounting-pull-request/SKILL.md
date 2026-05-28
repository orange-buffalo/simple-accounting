---
name: simple-accounting-pull-request
description: >
  Publishes the current local branch to remote, creates a pull request against
  master, and enables squash auto-merge after committing pending changes,
  rebasing onto origin/master, force-pushing safely, and waiting for PR CI.
compatibility: "requires: gh CLI, git, repository write access"
---

# Simple Accounting Pull Request Skill

Use this skill when the user wants to publish the current local branch, commit pending changes, create a GitHub pull request, or enable auto-merge for a branch.

This workflow is intentionally local and stateful. It may create commits, rebase the current branch, force-push with lease, create a pull request, enable squash auto-merge, wait for PR CI, and fix CI failures. Only use it when the user explicitly requests this publish-and-PR workflow or an equivalent operation.

## Context Requirement

Before deciding commit metadata, PR metadata, or remediation actions, account for the full available conversation and session context. Do not rely only on the user's last turn.

The full context includes:

- The original user request and any later refinements.
- All code changes made during the session, including generated files.
- Validation already run, skipped, failed, or fixed earlier in the conversation.
- Constraints, decisions, and tradeoffs discussed before the PR workflow was invoked.
- Any known issue references, branch purpose, or release context mentioned earlier.

If the full conversation context is insufficient to write accurate metadata or decide a safe remediation, ask the user or perform the necessary inspection. Do not guess from the latest turn alone.

## Preconditions

Run these checks before changing anything:

```bash
gh auth status
gh repo view --json nameWithOwner,url,defaultBranchRef
git rev-parse --is-inside-work-tree
git branch --show-current
git status --short
git status --branch --short
git log --oneline --decorate -n 10
```

Stop immediately if any condition is not met:

- `gh auth status` must show an authenticated GitHub CLI session.
- The current directory must be inside the Simple Accounting git repository.
- The current branch must not be `master`.
- The current branch name must be non-empty.
- The repository remote must be reachable through `gh repo view`.

Rules:

- Never commit files that likely contain secrets, such as `.env`, private keys, credentials, tokens, or local configuration files. Stop and ask if such files are present and appear relevant.
- Do not use destructive git commands such as `git reset --hard` or `git checkout --`.
- Do not use interactive git commands.
- Use `--force-with-lease`, never plain `--force`.
- Do not push directly to `master`.

## Pull Request Workflow

Execute these steps in order. Report command failures verbatim and stop unless the remediation is explicitly listed.

### 1. Inspect Pending Changes

Collect the current working tree state and recent commit context:

```bash
git status --short
git diff --stat
git diff --cached --stat
git log --oneline --decorate -n 20
```

If there are no staged, unstaged, or untracked changes, do not create an empty commit. Continue with the existing branch commits if the branch already contains unpublished or PR-worthy commits.

When there are pending changes:

- Commit all pending changes, including staged, unstaged, and untracked files.
- Do not attempt to split, filter, or selectively stage files.
- Still stop and ask before committing files that likely contain secrets, such as `.env`, private keys, credentials, tokens, or local configuration files.

### 2. Define Commit Message, PR Title, and PR Description

Use the full conversation and session context to define the commit message, PR title, and PR description. Do not perform a full branch-scope review by default; it can be too expensive and duplicates work expected from previous actors.

If the full context is insufficient to write accurate metadata, ask the user whether they want you to execute a full branch review before continuing. Do not guess.

Use the repository guidelines:

- Commit messages must follow Conventional Commits: `<type>[optional scope]: <concise description>`.
- Use single-line commit messages only.
- Use `tests` for test-code-only changes.
- Keep the message concise and add an issue reference at the end when applicable, for example `fix: Correct tax calculation (#123)`.
- Pull request titles must follow Conventional Commits. In most cases, use the commit message as the PR title.
- Pull request descriptions must follow `docs/pull_request_template.md`.

Inspect the pull request template:

```bash
git ls-files docs/pull_request_template.md
```

If the template exists, read it and use its sections exactly. Fill unavailable sections with concise, honest values such as `N/A` only when appropriate. Do not invent issue links or product impact.

PR description rules:

- Summarize the user-visible or maintainer-visible change.
- Do not include validation, testing, linting, compilation, CI, or build statements in the PR description.
- If the template has a validation or testing section, leave it empty. Do not write `N/A`, commands, prior results, skipped validation, CI status, or any other validation-related text in the PR description.
- Keep the description factual and concise.

### 3. Commit Pending Changes

If there are relevant pending changes, stage and commit them:

```bash
git add --all
git status --short
git commit -m "<conventional-commit-message>"
```

After committing, verify the result:

```bash
git status --short
git log --oneline --decorate -n 5
```

If commit hooks modify files and the commit succeeds, check whether the hook-created changes belong to the same commit. If they do, create a follow-up commit unless the user explicitly requested an amend. Do not amend by default.

If the commit fails:

- Report the exact failure.
- Fix only issues directly related to the failure if the fix is clear.
- Retry with a new normal commit after fixing.
- Never amend a failed or rejected commit.

### 4. Fetch Remote Master

Fetch the latest remote `master` branch:

```bash
git fetch origin master
git rev-parse origin/master
```

Stop if `origin/master` is unavailable.

### 5. Rebase Onto Remote Master

Rebase the current branch onto `origin/master`:

```bash
git rebase origin/master
```

After a successful rebase, verify the branch state:

```bash
git status --short
git log --oneline --decorate origin/master..HEAD
```

Conflict handling:

- If the rebase stops with conflicts, report the conflicted files and inspect them.
- Resolve conflicts only when the correct resolution is clear and limited to the task.
- Continue with `git rebase --continue` after resolving conflicts.
- If the correct resolution is unclear, stop and ask the user.
- Do not abort the rebase unless the user requests it or continuing would be unsafe.

### 6. Push the Branch

Determine the current branch and push it to `origin` using force-with-lease:

```bash
current_branch="$(git branch --show-current)"
git push --force-with-lease -u origin "$current_branch"
```

Rules:

- Use `--force-with-lease` because the branch was rebased.
- Never use plain `--force`.
- Never force-push `master`.
- If the force-with-lease push is rejected, fetch the remote branch and inspect divergence before doing anything else.

### 7. Create the Pull Request

Before creating a PR, check whether one already exists for the branch:

```bash
gh pr list --head "$current_branch" --json number,title,url,state,isDraft
```

If an open PR already exists for this branch, do not create a duplicate. Update the existing PR title and description using the metadata defined from the full session context:

```bash
gh pr edit "$pr_number" \
  --title "<pr-title>" \
  --body-file "<pr-description-file>"
```

Then use the existing PR for the auto-merge step and final report.

Create a PR against `master` when none exists:

```bash
gh pr create \
  --base master \
  --head "$current_branch" \
  --title "<pr-title>" \
  --body-file "<pr-description-file>"
```

Use a temporary file for the PR body so Markdown formatting is preserved. Do not inline a large PR body into a shell command.

After creation, capture the PR URL and number:

```bash
pr_url="$(gh pr view --json url --jq .url)"
pr_number="$(gh pr view --json number --jq .number)"
```

### 8. Enable Squash Auto-Merge

Enable auto-merge with squash for the pull request:

```bash
gh pr merge "$pr_number" --auto --squash
```

Rules:

- Use squash auto-merge only.
- Do not merge immediately unless GitHub performs the merge automatically because all requirements are already satisfied.
- If auto-merge cannot be enabled because repository settings disallow it, report that clearly and leave the PR open.
- If required checks are pending, auto-merge should remain enabled and GitHub will merge after requirements pass.

### 9. Wait For PR CI

Wait for the pull request's CI checks to complete after auto-merge is enabled:

```bash
gh pr checks "$pr_number" --watch
```

If all required checks pass, continue to the final state verification.

If any CI check fails:

- Disable auto-merge before making changes:

```bash
gh pr merge "$pr_number" --disable-auto
```

- Inspect the failed check details with `gh pr checks "$pr_number"`, `gh run view`, or `gh run view --log-failed` as appropriate.
- Investigate the root cause locally, using the full conversation context and the failed CI output.
- Fix only issues that are necessary for CI to pass and are consistent with the branch's intended scope.
- Run the smallest relevant local validation for the fix, following the repository validation workflow.
- Commit the fix with a normal Conventional Commit message. Do not amend unless the user explicitly requested it.
- Rebase onto the latest `origin/master` again if needed.
- Push with `--force-with-lease`.
- Wait for PR CI again.
- Repeat this investigate/fix/push/wait loop until CI passes or until blocked by an unclear failure that requires user input.
- Leave auto-merge disabled after pushing any CI fix. The user must verify the fix and merge manually later.

Do not leave auto-merge enabled while pushing a fix for failed CI. Do not re-enable auto-merge after a CI failure has required a fix.

### 10. Verify Final State

Collect final status:

```bash
git status --branch --short
gh pr view "$pr_number" --json number,title,url,state,isDraft,autoMergeRequest,mergeStateStatus,reviewDecision,statusCheckRollup
```

Use this information in the final response.

## Validation

Do not run tests, linting, compilation, builds, schema generation, GraphQL code generation, or other validation commands before creating the PR as part of this skill.

Assume validation and generated artifacts were handled by previous actors before this publish workflow was invoked. The exception is CI failure remediation after the PR exists: in that case, run the smallest relevant local validation needed to confirm the fix before pushing.

## Final Report

Report the result with these fields:

- Commit message: the final commit message used, or `No new commit created`.
- Branch: current branch name.
- Pull request: PR URL.
- Auto-merge: enabled with squash, already merged, or not enabled with the reason.
- CI: passed, still pending, or failed with the unresolved reason.
- Base: `origin/master` commit used for the rebase.

Keep the report short and factual.

## Failure Handling

- If GitHub CLI auth fails, stop and tell the user to run `gh auth login`.
- If the branch is `master`, stop and tell the user this workflow requires a feature branch.
- If likely secret files are pending, stop and ask before staging them.
- If the full conversation context is insufficient for accurate commit or PR metadata, ask whether to perform a full branch review before continuing.
- If rebase conflicts are unclear, stop and ask the user how to resolve them.
- If `git push --force-with-lease` fails, do not retry with plain `--force`; fetch and inspect remote divergence.
- If PR creation fails, report the exact `gh` error and provide the `gh pr create ...` command for retry.
- If auto-merge fails, leave the PR open and report the exact reason.
- If CI fails after auto-merge is enabled, disable auto-merge, investigate, fix, push, wait again until CI passes or user input is required, and leave auto-merge disabled for the user to verify and merge manually.
- Never delete branches, reset history, or force-push `master`.
