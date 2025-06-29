# General

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
