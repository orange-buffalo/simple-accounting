import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createHash } from 'node:crypto';

// Each original feedback instruction has an explicit destination, including superseded policies.
export const groups = [
  ['independent-expectations', 'testing', [1, 9, 10], 'Production mappings and third-party reference data', 'Mirroring implementation can reproduce its defect; exhaustive external fixtures add maintenance.', 'Use independent representative expected values, including an excluded value for filtered data. Do not reproduce production registry/filtering logic or maintain exhaustive provider lists unless exhaustive membership is the contract.'],
  ['api-test-structure', 'testing', [2, 35], 'Adding GraphQL API tests', 'Consistent boundaries expose missing business and authorization scenarios.'],
  ['nullable-wire-input', 'testing', [3], 'Testing optional API input', 'Null, empty and omitted wire fields have different semantics.'],
  ['component-test-ownership', 'testing', [4, 6, 11], 'Testing reusable component behavior', 'Duplicated component cases make consuming page tests expensive and brittle.', 'Keep reusable interaction-state coverage in the component-specific Kotlin full-stack test and page happy paths focused on workflow and persistence. A direction to move one component\'s coverage is not a repository-wide prohibition on other test frameworks.'],
  ['transient-notifications', 'testing', [5], 'Submissions with transient notifications and persistent field errors', 'Checking persistent state first can miss a transient notification.'],
  ['dependent-input-transitions', 'frontend', [7], 'Input changes that invalidate dependent values and start async work', 'Late responses or old selections can overwrite current input state.'],
  ['form-component-binding', 'frontend', [8], 'Implementing form components', 'Standard binding preserves the form contract and type safety.'],
  ['selector-rationale', 'testing', [12], 'Explaining a questioned test-selector change', 'A report explanation and a source comment serve different purposes.'],
  ['explicit-commit-precedence', 'workflow', [13, 17, 27], 'Committing implementation or feedback corrections', 'Workflow defaults must respect explicit user control.', 'Commit completed relevant turns separately by default, including prior progress before feedback. Explicit no-commit instructions override that default and are not a workflow defect. Scoped authorization covers only that scope, not later corrections or publication.'],
  ['feedback-review-turns', 'workflow', [14, 20, 51], 'Resuming review after human feedback', 'Distinct feedback needs current review without evading limits through duplicate requests.', 'Each distinct feedback turn has up to two review rounds with fresh round, repair, time and cost limits. Refresh knowledge and review cumulative changes against the original base, including analogous earlier-turn issues. Preserve cumulative usage and prior state. Whitespace-equivalent duplicate feedback and ordinary continuation never reset limits.'],
  ['feedback-completeness', 'workflow', [15, 16, 18], 'Responding to human corrections or preferences', 'Partial handling leaves known mistakes unresolved.', 'Investigate every item, including new preferences. Preserve exact comments and stable refs across follow-ups; label summaries and carry unresolved items forward. Ask with evidence and options about genuine conflicts rather than dismissing feedback. Report ref, file:line, initial comment, correction and active rule/remedy or unresolved status.'],
  ['precise-evidence', 'workflow', [19], 'Explaining behavior or learning from corrections', 'Unsupported diagnoses produce unreliable prevention.'],
  ['active-prevention', 'learning', [21], 'Starting implementation or publishing learned guidance', 'Prevention before coding is stronger than catching the same defect during review.', 'Apply the complete active catalog before coding and identify relevant constraints in the implementation approach. Inject all distilled rules; never silently truncate or leave accepted preventive instructions undeployed. Use the same active projection for validation and size checks. Feedback reports identify active rule IDs and the catalog path; verification is not measured improvement.'],
  ['server-cross-field-invariants', 'backend', [22], 'API writes whose values depend on another field, including residency/currency', 'Direct API callers bypass UI filtering.', 'Enforce cross-field constraints on the server and add negative API tests proving rejected input leaves persistence unchanged. Preserve authorization and optimistic-version precedence before edit validation and permit repairs of legacy inconsistent records.'],
  ['typed-provider-config', 'backend', [23, 34], 'Defining application or integration configuration', 'Typed domain-owned configuration keeps settings consistent.', 'Use documented typed @ConfigurationProperties rooted under sa, with provider settings under sa.integrations.<provider>. Do not add ad-hoc @Value properties.'],
  ['provider-api-versions', 'backend', [24], 'Providers with multiple API versions', 'A version embedded in the origin prevents mixed-version endpoints.'],
  ['workspace-integrations-api', 'backend', [25, 29, 30, 32, 46], 'Exposing workspace-owned integration metadata in GraphQL', 'Workspace ownership and extensible naming prevent flat first-provider-specific APIs.', 'Nest metadata under Workspace.integrations.<provider>.<data>, not standalone status/provider root queries. Name the API and root DTO documentation after the provider-neutral integrations domain; reserve provider names for provider-scoped types. Describe third-party IDs as opaque unless clients should interpret them.'],
  ['business-error-mapping', 'backend', [26], 'Mapping expected business failures', 'Broad catches hide unexpected defects.'],
  ['review-exhaustion-delivery', 'workflow', [28], 'Exhausting automated review rounds', 'A review budget caps automation, not delivery of required fixes.'],
  ['integration-service-boundary', 'backend', [31, 33], 'Implementing provider clients and configuration', 'The owning service prevents provider details leaking into APIs.'],
  ['page-navigation-tests', 'testing', [36, 37], 'Designing page and menu full-stack tests', 'Repeated navigation obscures the page business workflow.'],
  ['static-rendering', 'testing', [38], 'Validating static integration cards and instructional content', 'Rendering review avoids assertions duplicating static copy.'],
  ['test-helper-placement', 'testing', [39, 42, 43], 'Adding stubs, page objects and utilities', 'Clear ownership prevents parallel utility hierarchies.'],
  ['remove-unnecessary-tests', 'testing', [40], 'User-directed removal of unnecessary tests', 'Removing redundant coverage does not revoke production requirements.'],
  ['provider-service-tests', 'testing', [41], 'Testing internal provider clients', 'Service boundaries cover observable integration behavior.'],
  ['server-access-boundary', 'frontend', [44], 'Adding client-side access gates', 'UI gating is not authorization.'],
  ['page-owned-components', 'frontend', [45, 50, 55], 'Non-reused page behavior such as an integration wizard', 'Pass-through layers and scattered styles make behavior harder to follow.', 'Keep single-use behavior and styles in the owning Vue page/component; do not retain a pass-through wrapper unless it has reusable or independent value. Use shared styles only when genuinely shared.'],
  ['lazy-graphql-fields', 'backend', [48], 'GraphQL fields requiring extra computation or database access', 'Eager namespace construction spends resources for unselected fields.'],
  ['workspace-page-lifecycle', 'frontend', [49], 'Implementing fixed-workspace pages', 'Redundant subscriptions introduce stale-state paths.'],
  ['unmerged-migrations', 'backend', [52], 'Considering follow-up database migrations', 'Unreleased migrations can often be corrected directly.'],
  ['real-preconditions', 'testing', [53, 56, 57, 58], 'Setting up backend and full-stack tests', 'Realistic setup exercises application boundaries instead of implementation details.', 'Use real services and persistence-backed preconditions/lazyPreconditions factories; never access aggregateTemplate directly for setup. Use named arguments when supplying multiple parameters and defaults for unrelated values. Mock only unavoidable external boundaries; prefer persistence to repository mocks/spies. On distinct feedback turns audit cumulative branch changes for analogous issues; missing audit prevents a clean receipt.'],
  ['graphql-laziness-tests', 'testing', [54], 'Testing GraphQL lazy fields', 'Framework-mechanics tests add coupling without application coverage.'],
  ['exact-ui-collections', 'testing', [59, 60], 'Asserting and interacting with dynamic repeated UI items', 'Partial assertions and first-match selection hide unexpected or duplicate items.'],
  ['frontend-error-ownership', 'frontend', [61], 'Handling GraphQL/API errors in pages', 'Duplicate handlers create inconsistent or suppressed feedback.'],
  ['setup-persistence', 'testing', [62, 63], 'Testing successful credential-backed setup', 'UI success can hide incomplete persistence; overlapping journeys waste validation time.'],
  ['ordinary-vue-async', 'frontend', [64], 'Ordinary page-local Vue async flows', 'Speculative lifecycle machinery adds complexity; dependent-input races remain a distinct concern.'],
  ['semantic-wizard-steps', 'frontend', [65], 'Representing multi-step UI state', 'Named steps improve readability without changing library behavior.'],
  ['credential-safe-diagnostics', 'backend', [66], 'Diagnosing credential-backed provider setup', 'Useful diagnostics must locate failures without disclosing credentials.'],
  ['no-log-assertions', 'testing', [67], 'Testing integration diagnostics', 'Tests should cover service behavior rather than diagnostic emission.'],
  ['wizard-status-layout', 'frontend', [68], 'Changing Wise setup status presentation', 'Loading and completed states need visual consistency and accessible announcements.'],
];

export function migrate(lessons) {
  const entries = lessons.flatMap((lesson) => lesson.evidence.dispositions);
  const refs = groups.flatMap((group) => group[2].map((ref) => `F${ref}`));
  if (new Set(refs).size !== refs.length || entries.length !== refs.length
    || entries.some((entry) => !refs.includes(entry.ref))) throw new Error('Migration must cover every original instruction exactly once');
  return { version: 2, rules: groups.map(([id, category, numbers, appliesWhen, reason, replacement]) => ({
    id, category, appliesWhen, instruction: replacement ?? numbers.map((number) => entries.find((entry) => entry.ref === `F${number}`).lesson).join(' '), reason,
  })) };
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  if (!process.argv[2]) throw new Error('Provide the path to a pre-migration checkout; the migrated catalog is printed to stdout');
  const root = path.resolve(process.argv[2]);
  const registry = JSON.parse(await readFile(path.join(root, '.harness/knowledge.json'), 'utf8'));
  const lessons = await Promise.all(registry.lessons.map(async ({ id, sha256 }) => {
    const raw = await readFile(path.join(root, '.harness/lessons', `${id}.json`), 'utf8');
    if (createHash('sha256').update(raw).digest('hex') !== sha256) throw new Error(`Integrity failure: ${id}`);
    return JSON.parse(raw);
  }));
  console.log(JSON.stringify(migrate(lessons), null, 2));
}
