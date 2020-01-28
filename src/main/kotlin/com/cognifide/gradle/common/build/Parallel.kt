package com.cognifide.gradle.common.build

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.*

@UseExperimental(ObsoleteCoroutinesApi::class)
object Parallel {

    fun <A, B : Any> map(iterable: Iterable<A>, mapper: CoroutineScope.(A) -> B): Collection<B> {
        return map(iterable, { true }, mapper)
    }

    fun <A, B : Any> map(iterable: Iterable<A>, filter: (A) -> Boolean, mapper: CoroutineScope.(A) -> B): List<B> {
        return map(Dispatchers.IO, iterable) {
            if (filter(it)) { mapper(it) } else { null }
        }
    }

    private fun <A, B : Any> map(context: CoroutineContext, iterable: Iterable<A>, mapper: CoroutineScope.(A) -> B?): List<B> {
        return runBlocking(context) {
            iterable.map { value -> async { if (value != null) mapper(value) else null } }.mapNotNull { it.await() }
        }
    }

    fun <A> each(iterable: Iterable<A>, callback: CoroutineScope.(A) -> Unit) {
        map(iterable) { callback(it); Unit }
    }

    fun <A> with(iterable: Iterable<A>, callback: A.() -> Unit) {
        map(iterable) { it.apply(callback); Unit }
    }

    fun <A, B : Any> poolMap(threads: Int, name: String, iterable: Iterable<A>, mapper: CoroutineScope.(A) -> B): List<B> {
        return map(newFixedThreadPoolContext(threads, name), iterable, mapper)
    }

    fun <A> poolEach(threads: Int, name: String, iterable: Iterable<A>, callback: CoroutineScope.(A) -> Unit) {
        poolMap(threads, name, iterable) { callback(it); Unit }
    }
}
