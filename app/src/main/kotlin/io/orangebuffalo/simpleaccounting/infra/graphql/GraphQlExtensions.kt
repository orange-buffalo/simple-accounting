package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import io.orangebuffalo.simpleaccounting.business.security.runWithAuthentication
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory
import graphql.schema.DataFetchingEnvironment
import org.springframework.security.core.Authentication
import java.util.concurrent.CompletableFuture

/**
 * Creates a mapped [DataLoader] that executes the batch function on the dispatching thread.
 * Reduces boilerplate in [KotlinDataLoader] implementations by handling
 * [CompletableFuture] wrapping internally.
 */
fun <K : Any, V : Any> newMappedDataLoader(
    batchLoader: (Set<K>) -> Map<K, V>,
): DataLoader<K, V> = DataLoaderFactory.newMappedDataLoader { keys ->
    CompletableFuture.completedFuture(keys).thenApply(batchLoader)
}

fun <T> DataFetchingEnvironment.withRequestAuthentication(block: () -> T): T =
    runWithAuthentication(graphQlContext.get<Authentication?>(SUBSCRIPTION_AUTHENTICATION_KEY), block)
