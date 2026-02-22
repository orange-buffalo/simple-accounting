# Developing Simple Accounting

Generally speaking, it is just a Spring Boot application built with Gradle. Importing it into your
IDE should be straightforward.

You can then build it with `./gradlew assemble` and then run `SimpleAccountingApplication` main class.

> **Caveat**
>
> To speedup local development (e.g. in IntelliJ Idea), we disable automatic build of frontend code as a dependency
> to the backend build unless `CI` environment variable is set to `true`. If you make the frontend changes
> and like to run the integrated application, you need to run `./gradlew assemble` to build the frontend code.
>
> See other sections for more efficient development workflows.
 
## Requirements

* Temurin JDK 21
* Latest [Bun](https://bun.sh/docs/installation), which should be available on `PATH`.
* Docker

## Development Flow

We recommend developing new functionality using testing facilities, as it allows to prepare very specific
preconditions and system state that might be hard to achieve running the application in regular mode. It makes sense to
run the production build of the application for testing integrations with external systems, for exploratory testing
and final checks of the new developments.

### Regular Spring Boot with Vite dev server

You can start Vite dev server by running `bun dev` in `frontend` module. It will proxy all API requests to
the Spring Boot application running on `9393` port (unless test configs states the different, more on that below).

With Vite dev server, your frontend code will hot-reload on changes.

### Storybook

We are using Storybook for developing generic components that are aimed to be reused in multiple places. We then
also test the Storybook pages with screenshot testing (see below) to ensure visual stability e.g. on library updates.

Start the Storybook with `bun storybook` in `frontend` module.

### Testing

#### General approach

We believe in what many people call integration testing, although we see it as unit testing, where a unit being some
functionality with stable API. We highly discourage using mocks except for things like external systems, generators,
time. Our goal is to have tests that do not change when we do refactoring of the underlying functionality, including
moving classes between the packages, moving methods between the classes, merging and splitting the code. We see a good
test as such which changes only when we change the business logic.

For this reason, most of our tests are Spring Boot integration tests which run against API endpoint and UI, which
are the public stable APIs of our system.

Having said that, we still use some low-level tests (e.g. for a class) where those units are small and make sense to 
be covered separately.

#### Frontend

For covering the most complex frontend logic (non-UI), we use Vitest. Run `bun test:unit` or `bun test:unit:watch`
in `frontend` directory to run the tests or develop new ones.

#### API tests

All our APIs are covered with tests, including security and data access restrictions. This is done via regular
Spring Boot tests in `app` module. See tests extending `SaIntegrationTestBase`.

#### Tests config

For other tests, we have some support for more efficient development workflow. It can be enabled on-demand via
a configuration file `app/src/test/.test-config.yaml`. You can copy `app/src/test/.test-config.template.yaml` to
`.test-config.yaml` file. It will then be picked up by the tests and different environment will be created or different
behavior will be followed based on the configs.

#### Full stack tests

We test UIs via so-called full stack tests, which use Playwright to interact with a browser for steps and assertions.
See tests extending `SaFullStackTestBase`.

By default, the tests will run against a Testcontainers-managed browser and serve compiled (built by `frontend` Gradle 
module) frontend code.

We can use `.test-config.yaml` to switch to other modes:
* Provide `fullStackTestsConfig.useLocalBrowser: true` in order to use locally running Playwright browser. Useful to
  for better visibility on what is happening when test is executed.
* Provide additionally `fullStackTestsConfig.useViteDevServer: true` and the tests will start using Vite dev server instead
  of compiled frontend, which allows to have hot-reload of frontend code in tests (be sure to restart Vite dev server
  after changing this value). This is extremely useful for developing UI, when one can set a break point in the test
  after setting the preconditions and opening the page, and then make the necessary UI changes.

You can run the full suite with `./gradlew test`. Just note that `.test-config.yaml` will take effect here as well,
and your frontend code will not be built automatically unless `CI=true` is set in environment variables.

#### Faster tests development with JBR

We leverage [JBR](https://github.com/JetBrains/JetBrainsRuntime) and its support for advanced
class redefinitions in order to provide even faster feedback loop during the tests' development.

The general idea is simple - run the test in a loop, change the code (both test and subject) and 
compile it without restarting Spring Context. JBR will reload almost all code changes. Obviously,
changes to Spring Context (like new beans or changes to aspects) will not be applied until the 
context restart.

To enable this mode:
* Ensure JBR is available in Gradle [toolchain directory](https://docs.gradle.org/current/userguide/toolchains.html#sec:auto_detection) as auto-provisioning is not yet supported.
* In the tests config (`.tests-config.yaml`, see above), add `hotReloadEnabled: true`.
* Add JUnit's `@RepeatedTest(100)` on your test.
* Ensure your IDE is delegating executions to Gradle.
* Put a break point in the test.
* Debug the test.

Once the test is started, changing any code and compiling the project will reload the changes. In most cases,
they will be picked up on the next test loop iteration.

#### Screenshot tests

As mentioned above, we use Storybook for commonly used components and test their appearance stability with
screenshot tests. Similarly to full stack tests, we use Playwright for this. See `UiComponentsScreenshotsIT`.

By default, the tests will run against compiled Storybook and will just fail on broken comparison. However, this
can be customized via `.test-config.yaml` file:
* Provide `screenshots.replaceCommittedFiles: true` to override committed screenshots with the new ones. The tests will
  still fail if the new screenshots are different from the committed ones.
* Provide `screenshots.useCompliedStorybook: false` to use local Storybook instead of compiled one. This is useful
  for developing UI components to save time on rebuilding the Storybook.

You can run the screenshot tests with `./gradlew screenshotsTest`. `.test-config.yaml` will take effect here as well,
and your Storybook code will not be built automatically unless `CI=true` is set in environment variables.

### GraphQL Schema Management

Simple Accounting uses `graphql-kotlin` with a code-first approach to define the GraphQL API. The schema is automatically
generated from the Kotlin classes and stored as `app/src/test/resources/api-schema.graphqls` for version control.

#### Updating the GraphQL Schema

When you make changes to the GraphQL API (add new queries, mutations, or modify types), you need to update the
committed schema file. Use the following Gradle task:

```bash
./gradlew :app:updateGraphqlSchema
```

This task:
* Runs `GraphqlSchemaTest` with schema override enabled
* Updates `app/src/test/resources/api-schema.graphqls` with the current schema
* Should be run before running tests (for DGS builders regeneration) and before regenerating frontend TypeScript types

**Important**: Always run this task after making GraphQL API changes to keep the schema file in sync.

### REST API (OpenAPI) Schema Management

Simple Accounting uses SpringDoc OpenAPI to automatically generate an OpenAPI specification from REST API controllers.
The specification is stored as `app/src/test/resources/api-spec.yaml` for version control, and TypeScript client code
is generated from it into `frontend/src/services/api/generated/`.

#### Updating the OpenAPI Schema and TypeScript Client

When you make changes to REST API endpoints (add new endpoints, modify parameters, change request/response types),
you need to update both the committed OpenAPI spec and the generated TypeScript client. Use the `ApiSpecTest` test
with auto-update enabled:

```bash
OVERRIDE_COMMITTED_FILES=true ./gradlew :app:test --tests "ApiSpecTest"
```

This command:
* Runs the Spring Boot application to generate the current OpenAPI specification
* Compares it with the committed `app/src/test/resources/api-spec.yaml`
* Updates the spec file if different
* Generates TypeScript client code using OpenAPI Generator
* Updates files in `frontend/src/services/api/generated/` if different

**Important**: 
* Always run this after making REST API changes to keep the spec and TypeScript client in sync
* The test will fail if there are differences, prompting you to run it with `OVERRIDE_COMMITTED_FILES=true`
* Commit both the updated `api-spec.yaml` and the generated TypeScript files
* Do not manually edit files in `frontend/src/services/api/generated/` as they will be overwritten

#### Load Tests

At this point we decided to not include load test into the CI pipeline. Load test are executed on demand locally.

To run the load tests, follow these steps:

1. Download the database snapshot with required data:

* [part 1](https://github.com/orange-buffalo/simple-accounting-load-tests-data/raw/master/load-tests-db-snapshot.7z.001)
* [part 2](https://github.com/orange-buffalo/simple-accounting-load-tests-data/raw/master/load-tests-db-snapshot.7z.002)

1. Unzip the archives.
2. Launch application with `load-tests` Spring Boot profile and `spring.datasource.url` set to
   `jdbc:h2:<path-to-database-file>`.
3. Start JMeter and open `load-tests/jmeter-load-tests.jmx` project.
4. Launch the tests in JMeter.

## Squashing Flyway migrations

Occasionally we want to squash the Flyway migrations to reduce the number of files and make it easier to maintain.
Follow these steps:

1. Migrate target database to the latest version.
2. Delete old migrations from `<root>/app/src/main/resources/db/migration`.
3. Get current schema:
   ```sql
   script nodata nopasswords nosettings
     to '<root>/app/src/main/resources/db/migration/V0001__Baseline.sql'
     schema public;
   ```
4. Cleanup the script (remove users, remove sequences initialization, remove selectivity).
5. Drop Flyway table on target database:
   ```sql
   drop table "flyway_schema_history";
   ```
6. Enable `spring.flyway.baseline-on-migrate=true` in `application.yml`.
7. Once application started on the target database, set `spring.flyway.baseline-on-migrate` back to `false`
   in `application.yml`.
