# Simple Accounting Development Environment

**ALWAYS follow these instructions first. Only search for additional information or use bash commands if these instructions are incomplete or found to be incorrect.**

## Critical Build Requirements

**NEVER CANCEL builds or long-running commands.** Build and test commands can take significant time:
- `./gradlew assemble`: ~2 minutes
- `./gradlew check`: ~3-4 minutes for unit/integration tests 
- Full test suites (with Playwright): ~5-10 minutes

## Environment Setup

Required dependencies (pre-installed in CI environment):
- **Java 21** (Temurin distribution)
- **Latest Bun** (package manager for frontend)  
- **Docker** - required for integration tests and demo mode

## Project Structure

Simple Accounting is a **Spring Boot + Vue.js** application:
- **Backend**: `/app` - Kotlin, Spring Boot 3, WebFlux, GraphQL, JOOQ, H2 database
- **Frontend**: `/frontend` - Vue 3, TypeScript, Vite, Element Plus, Urql GraphQL client
- **Build system**: Gradle multi-module with Bun for frontend dependencies

## Build Commands

### Bootstrap and Build (CRITICAL)
```bash
# Full build including frontend
./gradlew assemble --console=plain --build-cache
```

### Testing Commands
```bash
# Unit and integration tests
./gradlew check --console=plain --build-cache

# Frontend unit tests only (fast)
cd frontend && bun run test:unit

# Frontend linting
cd frontend && bun run lint

# GraphQL schema generation
./gradlew :app:updateGraphqlSchema --console=plain
```

### Development Commands
```bash
# Frontend development server with hot reload
cd frontend && bun dev
# Runs on Vite dev server, proxies API to Spring Boot on port 9393
```

## Validation Workflow

After making changes, ALWAYS run this validation sequence:

1. **Build and compile**:
   ```bash
   ./gradlew assemble --console=plain --build-cache
   ```

2. **Run unit tests**:
   ```bash
   ./gradlew check --console=plain --build-cache
   ```

## GraphQL API Development

**Code-first GraphQL** with schema generation:
- After GraphQL changes, run: `./gradlew :app:updateGraphqlSchema`
- Regenerates `app/src/test/resources/api-schema.graphqls`
- Frontend TypeScript types auto-generated via `bun graphql-codegen`

## File Locations

### Key Build Files
- Root build: `/build.gradle.kts` (multi-module setup)
- Backend build: `/app/build.gradle.kts` (Spring Boot, Kotlin, JOOQ)
- Frontend build: `/frontend/build.gradle.kts` (Bun tasks)
- Frontend deps: `/frontend/package.json` (Bun dependencies)

### Configuration Files  
- Main app: `/app/src/main/kotlin/io/orangebuffalo/simpleaccounting/SimpleAccountingApplication.kt`
- Test config template: `/app/src/test/.test-config.template.yaml`
- Frontend config: `/frontend/vite.config.ts`

### Documentation
- Main: `/README.md` 
- Development: `/docs/Development.md`
- Deployment: `/docs/Deployment.md`
- Contributing: `/docs/CONTRIBUTING.md`

## Common Development Tasks

### After changing GraphQL API:
```bash
./gradlew :app:updateGraphqlSchema
cd frontend && bun graphql-codegen
```

### Before committing changes:
```bash
./gradlew assemble check
```

## Architecture Notes

### Testing Strategy
- **Unit tests**: Spring Boot integration tests (API-focused)
- **Frontend tests**: Vitest for limited cases of complex logic 
- **Full stack tests**: Playwright

# General Coding Guidelines

1. Be concise and clear in your responses. Avoid extra details unless specifically requested.
2. Do not provide summaries of the executed actions unless there is a specific reason to do so.
3. Never add noisy comments or explanations to the code. Prefer self-explanatory code. Only add comments for non-trivial
   decisions where it is not clear why the code is written that way.
4. Take your time and think about the task before starting to write code.
5. Never hardcode any values to pass the tests. Only use constants when explicitly requested, or after confirmation that
   it is acceptable.

# Data Model Overview

The `PlatformUser` entity represents a user of the application.
A `PlatformUser` is the owner of one or more `Workspace`s.

The `Workspace` entity is the central point of the data model.
It acts as a container for all other business-related entities.

Most of the other entities in the data model are scoped to a `Workspace`.
This means that each record of these entities belongs to a specific workspace.

Here's a breakdown of the key relationships:

- **`Workspace` and `PlatformUser`**: A `PlatformUser` can own multiple `Workspace`s. The `ownerId` in the `Workspace`
  entity establishes this relationship.
- **Entities belonging to a `Workspace`**: The following entities have a direct many-to-one relationship with
  `Workspace`, identified by a `workspaceId` foreign key:
    - `Customer`: Represents a customer of the business.
    - `Category`: Used to categorize incomes and expenses.
    - `Expense`: Represents a business expense.
    - `Income`: Represents a business income.
    - `IncomeTaxPayment`: Records a payment of income tax.
    - `GeneralTax`: Defines general taxes like VAT or Sales Tax.
    - `Document`: Represents a file or a document, such as a receipt or an invoice.

# Migration from REST API
We are currently mirating from REST API (using standard Spring WebFlux controllers) to GraphQL API.
Key point about the GraphQL API setup:
1. In production code, we use `graphql-kotlin` library with their Spring Server integration. It means we have
  code-first approach: we define GraphQL schema using Kotlin classes and annotations.
2. We then generate the schema from the running Spring Boot application and store it in `app/src/test/resources/api-schema.graphqls` file. `GraphqlSchemaTest` is responsible for starting the
  application and generating the schema file.
3. We have a number of customizations / infrastructure code in `io.orangebuffalo.simpleaccounting.infra.graphql` package.
4. On the testing side, we use DGS Gradle plugin to generate Kotlin type-safe query builders, and then only
  generate the queries strings using DGS. The execution of the requests in standard Spring test client with
  customization on top (see `ApiTestClient` and `ApiTestUtils.kt` for the extensions).
5. We then generate TypeScript code from the schema as part of post-install step in `frontend` package. It is
  leveraging `graphql-codegen` tool for type-safe queries on the frontend side.
6. We use `uqrl` framework with its `graphql-codegen` and Vue 3 integration to call the API from the frontend.
  See `frontend/src/services/api/gql-api-client.ts` for the entry point.

## GraphQL API Implementation Guidelines
1. **Structure Pattern**: Follow the pattern established by `UserProfileApi.kt`:
   - Create a namespace class (e.g., `AuthenticationGqlApi`)
   - Create inner `@Component` classes for Query or Mutation that implement `Query` or `Mutation` interfaces
   - Use `@GraphQLDescription` annotations extensively for schema documentation
   - Use `@RequiredAuth` for access control

2. **Testing Mutations**: 
   - Use `DgsClient.buildMutation()` for generating mutation strings

3. **Schema Generation**:
   - After adding new GraphQL operations, run `GraphqlSchemaTest` to generate updated schema
   - Update `app/src/test/resources/api-schema.graphqls` manually if needed
   - Run `./gradlew :app:generateJava` to regenerate DGS client code for testing

4. **Exception Handling**:
   - Never catch generic exceptions (e.g., `Exception`, `RuntimeException`) in business code
   - Only catch specific exception types that you can meaningfully handle
   - Generic exception handling should only be used in cross-cutting concerns infrastructure code
   - Let the framework's generic exception handling deal with unexpected errors rather than silently suppressing them

# Testing

## Preconditions setup

To set up test preconditions, you should use either `preconditions` or `lazyPreconditions` method from the base class. The letter is particularly useful for preconditions that are shared across multiple test methods.

### Example

Here is an example of how to define and use preconditions in a test class:

```kotlin
class MyTest(
    // ... other dependencies
) : SaIntegrationTestBase() {

    private val preconditions by lazyPreconditions {
        object {
            val fry = fry()
            val workspace = workspace(owner = fry)
            val category = category(workspace = workspace)
        }
    }

    @Test
    fun `should do something`() {
        // Access the preconditions in your test
        val fryId = preconditions.fry.id
        val workspaceId = preconditions.workspace.id
        // ... your test logic
    }
}
```

In this example:

1. The test class extends `SaIntegrationTestBase` instead of using an annotation.
2. Dependencies are injected via constructor parameters where needed.
3. A `preconditions` property is defined using a lazy delegate provided by `lazyPreconditions` from the base class.
4. Inside the lambda, you can use the methods from `EntitiesFactory` to create the required entities for your tests
   (e.g., `fry()`, `workspace()`, `category()`).
5. In your test methods, you can access the created entities through the `preconditions` property.

## REST API Testing

REST API tests should follow a consistent structure to ensure comprehensive coverage and maintainability.

### Test Structure

Organize tests using nested classes that correspond to the API endpoints. Each endpoint should have its own inner class
named after the HTTP method and path pattern:

```kotlin
class MyApiTest(
    @Autowired private val client: ApiTestClient,
    // ... other dependencies  
) : SaIntegrationTestBase() {

    @Nested
    @DisplayName("GET /api/my-resource/{id}")
    inner class GetMyResource {
        // Tests for GET endpoint
    }

    @Nested
    @DisplayName("POST /api/my-resource")
    inner class CreateMyResource {
        // Tests for POST endpoint
    }
}
```

### Authentication and Authorization Tests

Every API endpoint should test access control scenarios:

1. **Anonymous access**: Use `.fromAnonymous()` to test unauthenticated requests
2. **Regular user access**: Use `.from(preconditions.regularUser)` for standard user authentication
3. **Admin access**: Use `.from(preconditions.adminUser)` for admin-level authentication
4. **Cross-user access**: Test that users cannot access resources of other users

### Test Infrastructure

- Use `ApiTestClient` instead of `WebTestClient` for JWT-based authentication
- Use `verifyOkAndJsonBodyEqualTo()`, `verifyNotFound()`, `verifyUnauthorized()` helper methods
- Use `sendJson {}` for request bodies with JSON DSL
- Validate responses with `expectThatJsonBodyEqualTo {}` using kotlinx.serialization DSL

### Common Test Cases

Cover these scenarios for each endpoint:

1. **Happy path**: Valid requests with expected responses
2. **Not found**: Test with non-existent resource IDs
3. **Validation errors**: Test with invalid request data
4. **Access control**: Authentication and authorization scenarios
5. **Edge cases**: Boundary conditions and special states (e.g., expired tokens, disabled features)

## Full Stack Testing

Full stack tests verify the complete integration between frontend and backend components using Playwright for browser
automation. These tests should follow the Page Object pattern for maintainable and reusable test code.

### Test Structure

Extend `SaFullStackTestBase` and structure tests around business scenarios:

```kotlin
class MyFeatureFullStackTest : SaFullStackTestBase() {

    @Test
    fun `should perform complete business workflow`(page: Page) {
        // Test implementation using page objects
    }

    private val preconditions by lazyPreconditions {
        object {
            val user = platformUser(userName = "testUser").withWorkspace()
        }
    }
}
```

### Page Object Pattern

#### Page Object Structure

Create page objects that encapsulate UI interactions and provide a clear API for tests:

```kotlin
class MyFeaturePage(page: Page) : SaPageBase<MyFeaturePage>(page) {
    private val header = components.pageHeader("My Feature")
    private val configSection = ConfigSection(components)

    fun shouldBeOpen(): MyFeaturePage = header.shouldBeVisible()

    fun shouldHaveConfigSectionVisible(spec: ConfigSection.() -> Unit): MyFeaturePage {
        configSection.shouldBeVisible()
        configSection.spec()
        return this
    }
}

fun Page.shouldBeMyFeaturePage(): MyFeaturePage = MyFeaturePage(this).shouldBeOpen()
```

#### Component Composition

Break down complex UI sections into reusable components:

```kotlin
@UiComponentMarker
class ConfigSection(components: ComponentsAccessors<MyFeaturePage>) {
    private val container = components.page.locator("#config-section")
    val toggle = components.switchByContainer(container)
    val settings = ConfigSettings(components, container)

    fun shouldBeVisible() {
        container.shouldBeVisible()
    }
}
```

### Responsibilities Split

#### Test Responsibilities

- **Business scenario orchestration**: Define the complete workflow being tested
- **Assertions on business outcomes**: Verify database state, integrations, and end-to-end behavior
- **Mock setup**: Configure external service mocks and preconditions
- **Error scenario testing**: Validate proper error handling and user feedback

#### Page Object Responsibilities

- **UI interaction encapsulation**: Provide methods for clicking, filling, and navigating
- **Element location**: Define selectors and element access patterns
- **UI state assertions**: Verify visibility, content, and component states
- **Component composition**: Organize complex pages into manageable sections

### Navigation and Authentication

Use helper functions for common operations:

```kotlin
private fun Page.onMyFeature(
    user: PlatformUser,
    spec: MyFeatureConfig.() -> Unit
) {
    loginAs(user)
    shouldHaveSideMenu().clickMyFeature()
    shouldBeMyFeaturePage().shouldHaveConfigSectionVisible {
        spec(this)
    }
}
```

### Testing Patterns

#### Async Operations

Use proper waiting strategies for operations that involve API calls:

```kotlin
page.withBlockedApiResponse(
    "**/api/endpoint",
    initiator = { button.click() },
    blockedRequestSpec = {
        status.shouldBeRegular("Processing...")
        reportRendering("feature.loading-state")
    }
)
```

#### Database Validation

Verify backend state changes with appropriate assertions:

```kotlin
withHint("Should update the database state") {
    aggregateTemplate.findSingle<MyEntity>(preconditions.entity.id!!)
        .property.shouldBe(expectedValue)
}
```

#### External Service Integration

Mock external services and verify interactions:

```kotlin
// Setup mocks
ExternalServiceMocks.mockOperation(expectedResponse)

// Verify interactions
assertExternalServiceRequests(expectedRequest1, expectedRequest2)
```

### Common Test Cases

1. **Happy Path Workflows**: Complete successful user journeys
2. **Configuration Changes**: Enable/disable features and verify persistence
3. **Error Scenarios**: Network failures, service errors, validation failures
4. **Authorization Flows**: OAuth, external service integrations
5. **State Persistence**: Verify changes are saved and restored correctly
6. **UI Feedback**: Loading states, success/error messages, proper status updates

# Commits and Pull Requests
1. We follow Conventional Commits specification for commit messages.
2. We prefer single-line commit messages with a reference to the issue at the end, e.g.
  `fix: Correct calculation of tax amounts (#123)`.
3. As we squash pull requests when merging, the title of the pull request should also follow the same convention.
