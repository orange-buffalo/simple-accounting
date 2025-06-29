package io.orangebuffalo.simpleaccounting.tests.infra.database

import org.junit.jupiter.api.extension.*
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.reflect.KProperty

private val namespace = ExtensionContext.Namespace.create(LegacyPreconditionsFactoryExtension::class.java)
private const val factoryKey = "preconditions-factory"

/**
 * Extension to manage [LegacyPreconditionsFactory].
 */
@Deprecated("Use Spring Bean instead")
class LegacyPreconditionsFactoryExtension : Extension, ParameterResolver, AfterEachCallback, BeforeEachCallback {
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean {
        return parameterContext.parameter.type == LegacyPreconditionsFactory::class.java
    }

    /**
     * [LegacyPreconditionsFactory] state is safe to be shared between tests, in generally between
     * different test classes as well. This resolver will create a new instance only once,
     * and then reuse it for all child contexts.
     */
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any {
        val existingFactory = extensionContext.getStore(namespace).get(factoryKey)
        if (existingFactory != null) {
            return existingFactory
        }
        val factory = LegacyPreconditionsFactory()
        extensionContext.getStore(namespace).put(factoryKey, factory)
        return factory
    }

    override fun afterEach(context: ExtensionContext) {
        val factory = context.getStore(namespace).get(factoryKey) as LegacyPreconditionsFactory?
        factory?.afterEach()
    }

    override fun beforeEach(context: ExtensionContext) {
        val factory = context.getStore(namespace).get(factoryKey) as LegacyPreconditionsFactory?
        factory?.beforeEach(context)
    }
}

/**
 * API for setting up preconditions for tests. Available as JUnit parameter via [LegacyPreconditionsFactoryExtension].
 *
 * The responsibility of this component is to ensure preconditions are created when they are expected by tests,
 * e.g. after database reset between tests, handling nested tests, etc.
 *
 * It is recommended to have single instance per test, injected into constructor of the test class.
 * Then, there are two flavours of using it:
 * 1. Lazy preconditions - typically shared between multiple tests, they are defined via delegate that produces a
 *   lazy-initialized value, that is reset between the tests, so each tests has preconditions guaranteed to be created.
 *
 *   This is usually achieved by having a tests class instance field:
 *   ```
 *   private val preconditions by preconditionsFactory {
 *       object {
 *           val myEntity = <entity factory call>
 *       }
 *   }
 *   ```
 *   And then in tests:
 *   ```
 *   preconditions.myEntity
 *   ```
 *
 * 2. Eager preconditions - created at the moment of execution:
 *   ```
 *   val preconditions = preconditionsFactory {
 *      object {
 *          val myEntity = <entity factory call>
 *      }
 *   }
 */
@Deprecated("Use Spring Bean instead")
class LegacyPreconditionsFactory {

    private val lazyPreconditions = mutableListOf<LegacyLazyRepeatablePreconditionsDelegate<*>>()
    private var currentTestContext: ExtensionContext? = null

    /**
     * Produces _a lazy_ delegate that will create preconditions when first accessed.
     * The delegate is reset between tests, so each test has preconditions guaranteed to be created.
     * Useful for shared preconditions.
     * See class-level docs for usage example.
     */
    operator fun <T> invoke(spec: EntitiesFactory.() -> T): LegacyLazyRepeatablePreconditionsDelegate<T> {
        val lazyPreconditionsInstance = LegacyLazyRepeatablePreconditionsDelegate(spec)
        lazyPreconditions.add(lazyPreconditionsInstance)
        return lazyPreconditionsInstance
    }

    /**
     * Produces preconditions eagerly, at the moment of execution.
     * Useful for dedicated preconditions for a single test.
     */
    fun <T> setup(spec: EntitiesFactory.() -> T): T {
        val entitiesFactory = entitiesFactory()
        return spec(entitiesFactory)
    }

    internal fun afterEach() {
        lazyPreconditions.forEach { it.reset() }
    }

    internal fun beforeEach(extensionContext: ExtensionContext) {
        this.currentTestContext = extensionContext
        val entitiesFactory = entitiesFactory()
        lazyPreconditions.forEach { it.updateEntitiesFactory(entitiesFactory) }
    }

    private fun entitiesFactory(): EntitiesFactory {
        check(currentTestContext != null) { "Current test context not set" }
        val springContext = SpringExtension.getApplicationContext(currentTestContext!!)
        val entitiesFactoryInfra = springContext.getBean(EntitiesFactoryInfra::class.java)
        return EntitiesFactory(entitiesFactoryInfra)
    }
}

/**
 * Delegate for lazy repeatable preconditions.
 * See [LegacyPreconditionsFactory.invoke] for details.
 */
@Deprecated("Use Spring Bean instead")
class LegacyLazyRepeatablePreconditionsDelegate<P>(private val preconditionsSpec: EntitiesFactory.() -> P) {
    private var preconditions: P? = null
    private var entitiesFactory: EntitiesFactory? = null

    /**
     * Resets the state between tests.
     */
    internal fun reset() {
        preconditions = null
    }

    /**
     * Sets current (for Spring context related to active test) entities factory.
     */
    internal fun updateEntitiesFactory(entitiesFactory: EntitiesFactory) {
        this.entitiesFactory = entitiesFactory
    }

    /**
     * Gets preconditions, creating them if necessary.
     */
    operator fun getValue(thisRef: Any?, property: KProperty<*>): P {
        if (preconditions == null) {
            check(entitiesFactory != null) { "Entities factory not set" }
            preconditions = preconditionsSpec(entitiesFactory!!)
        }
        return preconditions!!
    }
}
