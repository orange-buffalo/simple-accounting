# Full Stack Test Page Object DSL

This document describes the DSL pattern used in full stack tests for interacting with page objects.

## Overview

Full stack tests in this project use a consistent DSL pattern that allows for clean, structured test code. The pattern supports both fluent chaining and lambda-based scoped operations.

## Page Object DSL Pattern

### Lambda-Based Scoped Operations (Recommended)

Page objects can be invoked with a lambda that provides a scoped context for operations:

```kotlin
page.shouldBeEditUserPage {
    activationStatus {
        shouldBeVisible()
        input { shouldBeActivated() }
    }
    userName { input.fill("newName") }
    saveButton { click() }
}
```

This pattern:
- Provides clear scope and structure
- Groups related operations together
- Reduces verbosity
- Makes test intent clearer

### Fluent Chaining (Also Supported)

The traditional fluent chaining approach is also supported for backward compatibility:

```kotlin
page.shouldBeEditUserPage()
    .activationStatus { shouldBeVisible() }
    .userName { input.fill("newName") }
    .saveButton { click() }
```

Both patterns can be mixed as needed:

```kotlin
val editPage = page.shouldBeEditUserPage()
editPage.userName { input.fill("newName") }

page.shouldBeEditUserPage {
    saveButton { click() }
}
```

## Components DSL

Components follow the same pattern through the `UiComponent` base class:

```kotlin
activationStatus {
    shouldBeVisible()
    input { shouldBeActivated() }
}
```

Components use the `invoke` operator to accept a lambda with the component as receiver, executing the operations and returning to the parent context.

## Implementation Details

### For Page Objects

Each page object extension function has two overloads:

```kotlin
// Returns the page object for chaining
fun Page.shouldBeEditUserPage(): EditUserPage = EditUserPage(this).shouldBeOpen()

// Lambda-based version that executes the spec and returns Unit
fun Page.shouldBeEditUserPage(spec: EditUserPage.() -> Unit) {
    shouldBeEditUserPage().spec()
}
```

### For Components

Components inherit the DSL through `UiComponent`:

```kotlin
abstract class UiComponent<P : Any, T : UiComponent<P, T>>(
    protected val parent: P,
) {
    operator fun invoke(action: T.() -> Unit): P {
        self().action()
        return parent
    }
}
```

## Guidelines

1. **Prefer lambda-based syntax** for new tests when operations are logically grouped
2. **Use fluent chaining** when you need to store the page reference or when operations are sequential across different pages
3. **Both patterns work together** - choose what makes the test most readable
4. **Maintain consistency** within a single test method

## Examples

### Lambda Pattern - Grouped Operations

```kotlin
@Test
fun `should validate user input`(page: Page) {
    page.shouldBeCreateUserPage {
        userName { input.fill("") }
        saveButton { click() }
        shouldHaveNotifications { validationFailed() }
        userName { shouldHaveValidationError("This value is required") }
    }
}
```

### Chaining Pattern - Sequential Navigation

```kotlin
@Test
fun `should navigate through pages`(page: Page) {
    val overviewPage = page.shouldBeUsersOverviewPage()
    overviewPage.createUserButton.click()
    
    page.shouldBeCreateUserPage()
        .userName { input.fill("user") }
        .saveButton { click() }
        
    page.shouldBeEditUserPage()
}
```

### Mixed Pattern - Best of Both

```kotlin
@Test
fun `should update user with validation`(page: Page) {
    setupPreconditions(page)
    
    page.shouldBeEditUserPage {
        userName { input.fill("") }
        saveButton { click() }
    }
    
    // Perform external verification
    verifyDatabaseState()
    
    page.shouldBeEditUserPage {
        userName { 
            shouldHaveValidationError("Required")
            input.fill("valid-name")
        }
        saveButton { click() }
    }
}
```
