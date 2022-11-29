# simple-accounting

## Development

### Screenshot Tests

1. Start Storybook:
    ```shell script
   npm run storybook:serve
    ```
1. Run the tests:
    ```shell script
   npm run test:screenshot
    ```

To accept the changes and override Git-manages screenshots:
```shell script
npm run test:screenshot -- -u
```

To run a particular story or kind:
```shell script
export STORYSHOTS_STORY_NAME="Initial Loading"
export STORYSHOTS_STORY_KIND="EditIncome"
npm run test:screenshot
```

### Load Tests

At this point we decided to not include load test into the CI pipeline. Load test are executed on demand locally.

To run the load tests, follow these steps:
1. Download the database snapshot with required data:
  * [part 1](https://github.com/orange-buffalo/simple-accounting-load-tests-data/raw/master/load-tests-db-snapshot.7z.001)
  * [part 2](https://github.com/orange-buffalo/simple-accounting-load-tests-data/raw/master/load-tests-db-snapshot.7z.002)
1. Unzip the archives.
1. Launch application with `load-tests` Spring Boot profile and `spring.datasource.url` set to 
`jdbc:h2:<path-to-database-file>`.
1. Start JMeter and open `load-tests/jmeter-load-tests.jmx` project.
1. Launch the tests in JMeter.

### Squashing Flyway migrations
1. Migrate target database to the latest version.
2. Delete old migrations from `<root>/backend/src/main/resources/db/migration`.
3. Get current schema:
   ```sql
   script nodata nopasswords nosettings
     to '<root>/backend/src/main/resources/db/migration/V0001__Baseline.sql'
     schema public;
   ```
4. Cleanup the script (remove users, remove sequences initialization, remove selectivity).
5. Drop Flyway table on target database:
   ```sql
   drop table "flyway_schema_history";
   ```                               
6. Enable `spring.flyway.baseline-on-migrate=true` in `application.yml`.
7. Once application started on the target database, set `spring.flyway.baseline-on-migrate` back to `false` in `application.yml`.
