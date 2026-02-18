package io.orangebuffalo.simpleaccounting.infra.graphql

import com.expediagroup.graphql.dataloader.KotlinDataLoader
import graphql.schema.DataFetchingEnvironment
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

/**
 * Loads a value from the data loader registered for this [KotlinDataLoader] type.
 * Simplifies the caller-side invocation by encapsulating the data loader name lookup.
 */
fun <K, V> KotlinDataLoader<K, V>.load(
    env: DataFetchingEnvironment,
    key: K,
): CompletableFuture<V> = env.getDataLoader<K, V>(dataLoaderName)!!.load(key)
