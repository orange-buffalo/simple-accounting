# CI Timeout Investigation Setup

## Problem
CI builds occasionally hang indefinitely during the test phase, with the last log entries showing Spring Context and connection pool shutdown, suggesting a blocked thread preventing JVM shutdown.

## Solution Implemented

### 1. Test Timeout Configuration
Added a 20-minute timeout for all Gradle test tasks in `buildSrc/src/main/kotlin/Extensions.kt`:

```kotlin
timeout.set(java.time.Duration.ofMinutes(20))
```

This prevents indefinite hangs. Normal test execution takes ~3-4 minutes, so 20 minutes provides a safe buffer while ensuring the build doesn't run for hours.

### 2. Thread Dump Collection Script
Created `.github/scripts/collect-thread-dumps.sh` to periodically capture thread dumps during test execution:

- Runs in the background during test execution
- Collects thread dumps every 30 seconds
- Captures up to 40 dumps (covering the full 20-minute timeout window)
- Uses `jstack`, `jcmd`, or `kill -3` to capture dumps
- Saves dumps to `thread-dumps/` directory with timestamps and PIDs

### 3. CI Workflow Updates
Modified `.github/workflows/ci.yml`:

- Starts thread dump collection in the background before running tests
- Ensures thread dump collection is stopped after tests complete
- Preserves test exit code for proper CI status reporting
- Uploads thread dumps as artifacts for analysis (even on timeout/failure)

## Usage

### In CI
Thread dumps are automatically collected during all test steps:
- "Test" step → `thread-dumps/` directory
- "Test (screenshots)" step → `thread-dumps-screenshots/` directory  
- "Test (end-to-end)" step → `thread-dumps-e2e/` directory

All directories are uploaded as a single artifact named `thread-dumps`.

### Local Testing
To test thread dump collection locally:

```bash
# In one terminal, start a long-running test
./gradlew check

# In another terminal, collect thread dumps
.github/scripts/collect-thread-dumps.sh ./my-dumps 30 5
```

### Verifying the Configuration
The timeout configuration can be verified by examining the build scripts:
- `buildSrc/src/main/kotlin/Extensions.kt` for the standard test task
- `app/build.gradle.kts` for screenshotsTest and e2eTest tasks

All test tasks (`:app:test`, `:app:screenshotsTest`, `:app:e2eTest`) now have a 20-minute timeout configured.

You can verify the build configuration is valid by running:
```bash
./gradlew :app:help
```

## Analyzing Thread Dumps

When a timeout occurs:

1. Download the `thread-dumps` artifact from the failed CI run
2. Look for dumps collected near the timeout
3. Analyze thread states to identify blocked threads:
   - Look for threads in BLOCKED or WAITING state
   - Check for deadlocks (will be explicitly reported)
   - Identify which threads are holding locks
   - Examine stack traces of non-daemon threads that may prevent JVM shutdown

Common patterns to look for:
- Threads waiting on I/O operations
- Threads blocked on database connections
- Threads waiting on network operations
- Unclosed resources (database connections, HTTP clients, etc.)
- Non-daemon threads that haven't terminated
- Testcontainers not properly shut down
- Playwright browser processes not terminated
- Spring context cleanup issues

### Tools for Analysis

Use `jstack` analyzer or thread dump analysis tools like:
- FastThread (https://fastthread.io/)
- Thread Dump Analyzer (TDA)
- IntelliJ IDEA's built-in thread dump analysis

Look for:
- Multiple consecutive dumps showing the same threads stuck
- Growing number of threads over time
- Resource exhaustion patterns

## Next Steps

Once thread dumps are collected from a failing build:

1. Analyze the dumps to identify the root cause
2. Implement a fix for the identified issue
3. Consider if this diagnostic infrastructure should remain permanent or be removed after the issue is resolved

## Known Considerations

### Spring Context Caching
The test configuration uses Spring's context caching (see `app/build.gradle.kts`):
- Tests fork every 1000 tests to prevent context cache overgrowth
- Up to 5 parallel test forks are used
- Context cleanup issues could lead to resource leaks

### Resource Cleanup
Pay attention to:
- Database connection pools (H2)
- Testcontainers lifecycle
- Playwright browser instances
- HTTP clients and network connections
- Thread pools from Kotlin coroutines or reactive streams

## Related Files

- `buildSrc/src/main/kotlin/Extensions.kt` - Test timeout configuration for standard test task
- `app/build.gradle.kts` - Test timeout configuration for screenshotsTest and e2eTest tasks
- `.github/scripts/collect-thread-dumps.sh` - Thread dump collection script
- `.github/workflows/ci.yml` - CI workflow with thread dump collection for all test steps
- `.github/CI-TIMEOUT-INVESTIGATION.md` - This documentation file
