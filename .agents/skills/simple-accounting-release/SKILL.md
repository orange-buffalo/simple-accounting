---
name: simple-accounting-release
description: >
  Creates a Simple Accounting release locally: verifies GitHub CLI and git state,
  runs the Gradle semver release task, generates JReleaser-style notes, pushes
  the release tag, waits for CI and Docker publication, and publishes the latest GitHub release.
compatibility: "requires: gh CLI, git, Java 25, Gradle wrapper, Docker CLI"
---

# Simple Accounting Release Skill

Use this skill when the user wants to create a new Simple Accounting release, draft a GitHub release, publish release notes, or run the local release workflow.

This skill intentionally performs all repository-writing release operations locally. CI must not create tags or GitHub releases; CI only publishes the Docker image when it sees a release version on `master`.

## Preconditions

Run these checks before changing anything:

```bash
gh auth status
gh repo view --json nameWithOwner,url
git rev-parse --is-inside-work-tree
git branch --show-current
git status --short
git fetch origin master --tags
```

Stop immediately if any condition is not met:

- `gh auth status` must show an authenticated GitHub CLI session.
- The current branch must be exactly `master`; releases are only supported from the main branch.
- The working copy must be clean, including no staged changes and no untracked files.
- `origin/master` must exist and be reachable.

After fetching, verify local `master` is current:

```bash
git status --branch --short
git rev-parse HEAD
git rev-parse origin/master
```

If the clean local `master` is behind `origin/master`, automatically run `git pull --ff-only origin master`, without asking for confirmation. Recheck that the working tree is clean and `HEAD` equals `origin/master` before continuing. If local `master` is ahead or diverged, stop and ask how to reconcile it; never reset, rebase, or force-push automatically.

## Release Workflow

Execute these steps in order. Report command failures verbatim and stop unless the remediation is explicitly listed.

### 1. Capture the Previous GitHub Release

Use GitHub releases as the source of the previously published version:

```bash
previous_release_json="$(gh release view --json tagName,name,url,createdAt,isDraft,isPrerelease 2>/dev/null || true)"
previous_tag="$(gh release view --json tagName --jq .tagName 2>/dev/null || true)"
previous_release_url="$(gh release view --json url --jq .url 2>/dev/null || true)"
```

If no GitHub release exists, fall back to the latest remote version tag:

```bash
if [[ -z "$previous_tag" ]]; then
  previous_tag="$(git tag --list 'v[0-9]*.[0-9]*.[0-9]*' --sort=-version:refname | head -n 1)"
fi
```

Rules:

- Prefer `gh release view` over git tags because the report must reference the latest GitHub release.
- Use the previous release tag as the exclusive lower bound for commit collection.
- If there is no previous GitHub release and no previous version tag, use the first repository commit as the lower bound and say this is the first release.
- Preserve the tag exactly as GitHub reports it, usually `vX.Y.Z`.

### 2. Create the Release Commit and Tag

Run the semver plugin release task:

```bash
./gradlew releaseVersion --console=plain
```

Expected result:

- The task creates a release commit.
- The task creates a local release tag.
- The working copy remains clean after the task completes.

After it completes, collect the new version and tag:

```bash
new_version="$(./gradlew -q printVersion | grep -E '^[0-9]+\.[0-9]+\.[0-9]+' | tail -n 1)"
new_tag="v${new_version}"
git rev-parse "$new_tag"
git status --short
```

Stop if:

- `new_version` is empty.
- `new_version` contains `-SNAPSHOT`.
- `new_tag` does not exist locally.
- The working copy is dirty.
- The tag already exists on the remote. This command should fail for a new release; if it succeeds, stop:

```bash
git ls-remote --exit-code --tags origin "refs/tags/${new_tag}"
```

If the remote tag exists, stop and report that the release appears to have already been published.

### 3. Collect Commits for Release Notes

Collect commits between the previous release, exclusive, and current `HEAD`, inclusive.

If a previous release tag exists:

```bash
git log "${previous_tag}..HEAD" --no-merges --pretty=format:'%H%x09%s%x09%an'
```

If this is the first release:

```bash
git log --no-merges --pretty=format:'%H%x09%s%x09%an'
```

Also collect summary stats:

```bash
if [[ -n "$previous_tag" ]]; then
  git log "${previous_tag}..HEAD" --no-merges --pretty=format:'%H' | wc -l
  git shortlog -sn "${previous_tag}..HEAD"
  git diff --stat "${previous_tag}..HEAD"
else
  git log --no-merges --pretty=format:'%H' | wc -l
  git shortlog -sn HEAD
  git diff --stat "$(git rev-list --max-parents=0 HEAD)..HEAD"
fi
```

For a first release, omit the range prefix for commit logs and use all reachable commits.

Commit collection rules:

- Exclude merge commits.
- Exclude bot or infrastructure-only authors from contributor-facing notes where appropriate: `[bot]`, `GitHub`, and `orange-buffalo`.
- Do not include uncategorized commits in the release notes unless they describe user-visible behavior.
- Keep commit hashes available for the final report, but release notes should be readable and not just a raw commit dump.

### 4. Build JReleaser-Style Release Notes

Create the release notes in a temp file:

```bash
notes_file="$(mktemp /tmp/opencode/simple-accounting-release-notes.XXXXXX.md)"
```

The notes must follow the current JReleaser-style output used by the project:

```markdown
## What's Changed

### New Features
- Clear user-facing feature description (#123)

### Bug Fixes
- Clear user-facing bug fix description (#124)

### Build & CI
- Build or CI change that matters to maintainers (#125)

### Documentation
- Documentation update (#126)

### Refactorings
- Refactoring summary only when useful for maintainers (#128)

### Tests
- Test-only change summary only when useful for maintainers (#129)

### Dependency Updates
- Dependency update summary (#127)
```

Formatting rules:

- Start with `## What's Changed`.
- Use Markdown headings exactly as shown above.
- Use plain ASCII headings; do not add emoji.
- Keep `Dependency Updates` as the last section when present.
- Omit empty categories.
- Skip merge commits.
- Hide uncategorized commits by default.
- Hide contributor-only lines; this project does not currently use the generated contributors section.
- Prefer pull request references from commit subjects, e.g. `(#2623)`.
- Preserve issue or PR references when already present.
- Rewrite commit subjects into concise release-note entries when needed.
- Use sentence case and no trailing periods unless the entry contains multiple sentences.

Classification rules:

- `feat` commits go under `New Features`.
- `fix` commits go under `Bug Fixes`.
- `build` and `ci` commits go under `Build & CI`.
- `docs` commits go under `Documentation`.
- Dependabot-style `chore: bump ...` commits go under `Dependency Updates`.
- `refactor` commits go under `Refactorings`.
- `test` or `tests` commits go under `Tests`.
- `perf` commits go under `Bug Fixes` if they fix user-visible slowness, otherwise omit unless important.
- `chore` commits are omitted unless they are dependency updates or release-relevant maintenance.

When rewriting entries:

- Remove redundant conventional commit prefixes such as `feat:`, `fix:`, or `chore(deps):`.
- Keep the original technical meaning.
- Do not exaggerate impact.
- Do not invent features or fixes not supported by commits.
- If a commit is unclear, omit it rather than guessing.

Generate and self-review the notes, then continue without requesting user approval. Include supported deployment warnings and breaking changes. Explicit user instructions to pause or approve notes override this default.

### 5. Push the Release Commit and Tag

Push the release commit first, then the tag:

```bash
git push origin master
git push origin "$new_tag"
```

Stop if either push fails. Do not create the GitHub release if the commit or tag push did not succeed.

### 6. Create the Draft GitHub Release

Create the release in draft state:

```bash
gh release create "$new_tag" \
  --title "$new_tag" \
  --notes-file "$notes_file" \
  --draft \
  --prerelease \
  --latest=false
```

Then fetch the release URL:

```bash
release_url="$(gh release view "$new_tag" --json url --jq .url)"
```

### 7. Wait for CI and Verify Published Artifacts

After pushing, find the CI run for the release commit on `master`:

```bash
release_sha="$(git rev-parse HEAD)"
gh run list --workflow ci.yml --branch master --commit "$release_sha" --json databaseId,url,status --limit 1
```

Use the workflow file name (`ci.yml`), not the display name, because GitHub CLI can report ambiguous matches for workflow names.

Capture the returned run ID and URL as `ci_run_id` and `ci_url`. If no run is visible yet, retry discovery for up to two minutes; do not treat a workflow link as a successful run. If still absent, leave the release draft and report the blocker with this fallback link:

```bash
repo_url="$(gh repo view --json url --jq .url)"
ci_url="${repo_url}/actions/workflows/ci.yml?query=branch%3Amaster"
```

Always wait for the exact release commit's CI run to finish:

```bash
gh run watch "$ci_run_id" --exit-status
gh run view "$ci_run_id" --json headSha,status,conclusion,jobs
```

Do not cancel CI or stop at an in-progress report. Require the expected `headSha`, completed status, success conclusion, and successful Docker publishing step. If CI fails, is cancelled, or publication is skipped, keep the GitHub release draft and report the failure; never promote it.

After green CI, verify both published Docker references using the registry, not a local cached image:

```bash
docker manifest inspect "orangebuffalo/simple-accounting:${new_version}"
docker manifest inspect orangebuffalo/simple-accounting:latest
```

Require both references to resolve to the same published image (compare manifest digests, or config digests for single-platform manifests and platform/digest pairs for multi-platform indexes). If registry propagation delays visibility, retry for up to two minutes. On missing or mismatched images, leave the release draft and report the exact discrepancy rather than claiming publication. Never retag or overwrite images to bypass verification. If another release has superseded this one, stop rather than mark an older release latest.

### 8. Publish the Latest GitHub Release

Only after CI and Docker publication verification succeed, publish the draft as a stable release and mark it latest, without another confirmation:

```bash
gh release edit "$new_tag" --draft=false --prerelease=false --latest
gh release view "$new_tag" --json tagName,url,isDraft,isPrerelease
gh api "repos/$(gh repo view --json nameWithOwner --jq .nameWithOwner)/releases/latest" --jq '{tag_name,html_url}'
```

Verify that the release is neither draft nor prerelease and that GitHub's latest endpoint returns `new_tag`. Report any promotion or verification failure explicitly.

## Final Report

Report the release result with these fields:

- New version: `vX.Y.Z`
- Published latest stable GitHub release: verified release URL (or draft URL with the blocker if incomplete)
- Previous release: previous release tag and URL if available
- CI build: successful run URL and conclusion, or the actual failure/blocker
- Commit range: `previous_tag..HEAD`, or `first release` if there was no previous tag
- Commit stats: total non-merge commits, number of included release-note entries, number of omitted uncategorized commits, contributor count
- Docker publishing: confirm `orangebuffalo/simple-accounting:X.Y.Z` and `latest` are actually published and verified, including the matching digest evidence; never substitute a future-tense publishing promise

Keep the report concise and factual. If any step was skipped or required fallback behavior, include that explicitly.

## Failure Handling

- If GitHub CLI auth fails, stop and tell the user to run `gh auth login`.
- If the branch is not `master`, stop and tell the user releases are only supported from `master`.
- If the working copy is dirty, stop and list the changed files.
- If `releaseVersion` fails, report the Gradle output and do not retry with modified version numbers.
- If the remote tag already exists, stop and report the existing tag.
- If `git push origin master` succeeds but `git push origin "$new_tag"` fails, stop and tell the user the release commit was pushed but the tag was not.
- If GitHub release creation fails after pushing the tag, report the exact `gh` error and provide the `gh release create ...` command for retry.
- If CI, artifact verification, or GitHub promotion fails, report the exact failed stage and retain the draft where possible. Do not claim a completed/latest release or delete already-published artifacts.
- Never force-push tags or branches.
- Never delete or recreate a tag unless the user explicitly requests it.
