package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import org.dataloader.DataLoader
import org.dataloader.DataLoaderFactory
import java.util.concurrent.CompletableFuture

/**
 * Creates a mapped [DataLoader] that executes the batch function asynchronously.
 * Reduces boilerplate in [KotlinDataLoader] implementations by handling
 * [CompletableFuture] wrapping internally.
 */
fun <K : Any, V> newAsyncMappedDataLoader(
    batchLoader: (Set<K>) -> Map<K, V>,
): DataLoader<K, V> = DataLoaderFactory.newMappedDataLoader { keys ->
    CompletableFuture.supplyAsync { batchLoader(keys) }
}
