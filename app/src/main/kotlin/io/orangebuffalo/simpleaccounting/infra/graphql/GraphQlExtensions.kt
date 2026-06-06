package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import io.orangebuffalo.simpleaccounting.infra.supplyAsyncWithDbContext
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory

/**
 * Creates a mapped [DataLoader] that executes the batch function asynchronously.
 * Reduces boilerplate in [KotlinDataLoader] implementations by handling
 * [CompletableFuture] wrapping internally.
 */
fun <K : Any, V : Any> newAsyncMappedDataLoader(
    batchLoader: (Set<K>) -> Map<K, V>,
): DataLoader<K, V> = DataLoaderFactory.newMappedDataLoader { keys ->
    supplyAsyncWithDbContext { batchLoader(keys) }
}
