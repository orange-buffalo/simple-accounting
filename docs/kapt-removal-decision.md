# KAPT Removal Decision

## Background
KAPT (Kotlin Annotation Processing Tool) has been deprecated by JetBrains in favor of KSP (Kotlin Symbol Processing).

## Investigation Results

### Previous Usage
The project was using kapt for a single dependency:
- `org.springframework.boot:spring-boot-configuration-processor`

### Purpose of Configuration Processor
The Spring Boot Configuration Processor is an **optional** annotation processor that generates metadata for `@ConfigurationProperties` classes. This metadata provides:
1. IDE autocomplete in application.properties/application.yml files
2. Configuration properties documentation
3. Type hints for configuration values

### KSP Migration Assessment
As of Spring Boot 3.5.7, the `spring-boot-configuration-processor` **does not support KSP**. The processor only works with Java annotation processing (APT/KAPT).

According to the Spring Boot team, KSP support for the configuration processor is being considered but is not yet available.

### Decision
**KAPT has been completely removed from the project.**

The `spring-boot-configuration-processor` dependency has also been removed because:
1. It is **optional** - the application functions correctly without it
2. It only provides developer experience enhancements (IDE autocomplete)
3. It does not support KSP, so we cannot migrate it
4. The configuration properties are simple and well-documented in code
5. Loss of IDE autocomplete is acceptable given the small number of configuration properties

### Impact
- ✅ No runtime impact - application works correctly
- ✅ No build impact - compilation and tests pass
- ⚠️ Minor developer experience impact - loss of IDE autocomplete for custom configuration properties
- ✅ No impact on standard Spring Boot properties (those still have autocomplete from Spring Boot itself)

### Custom Configuration Properties
The following custom properties will no longer have IDE autocomplete (but still work correctly):
- `simpleaccounting.documents.storage.local-fs.*`
- `simpleaccounting.user-management.*`
- `simpleaccounting.backup.*`

These are documented in their respective `@ConfigurationProperties` classes:
- `LocalFileSystemDocumentsStorageProperties`
- `UserManagementProperties`
- `BackupProperties`

## Future Considerations
If Spring Boot adds KSP support for the configuration processor in the future, we can:
1. Add the KSP plugin to the build
2. Add the configuration processor as a `ksp` dependency
3. Regenerate the metadata

Until then, the current approach (no annotation processing) is the recommended solution.

## References
- [Spring Boot Configuration Processor Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html#appendix.configuration-metadata.annotation-processor)
- [KAPT Deprecation](https://kotlinlang.org/docs/kapt.html)
- [KSP Documentation](https://kotlinlang.org/docs/ksp-overview.html)
