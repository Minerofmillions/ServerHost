package minerofmillions.utils

import com.arkivanov.decompose.value.Value

fun List<Value<Boolean>>.any(): Value<Boolean> = ListReducedValue(this) { it.any(::identity) }

class ListReducedValue<T : Any, out R : Any>(private val upstream: List<Value<T>>, private val mapper: (List<T>) -> R) :
    Value<R>() {
    private var lastUpstreamValues: List<T> = upstream.map(Value<T>::value)
    private var lastMappedValue: R = mapper(lastUpstreamValues)
    private val observers = mutableMapOf<(R) -> Unit, (T) -> Unit>()

    override val value: R get() = mapCached(upstream.map(Value<T>::value))
    private fun mapCached(values: List<T>): R = Lock.synchronized {
        if (values != lastUpstreamValues) {
            lastUpstreamValues = values
            lastMappedValue = mapper(values)
        }

        lastMappedValue
    }

    @Deprecated(
        "Calling this method from Swift leaks the observer, because Kotlin wraps the function passed from Swift every time the method is called. Please use the new `observe` method which returns `Disposable`.",
        level = DeprecationLevel.WARNING,
    )
    override fun subscribe(observer: (R) -> Unit) {
        val upstreamObserver: (T) -> Unit = { _ -> observer(mapCached(upstream.map(Value<T>::value))) }

        Lock.synchronized {
            if (observer in observers) {
                return
            }

            observers[observer] = upstreamObserver
        }

        @Suppress("DEPRECATION") upstream.forEach { it.subscribe(upstreamObserver) }
    }

    @Deprecated(
        "Calling this method from Swift doesn't have any effect, because Kotlin wraps the function passed from Swift every time the method is called. Please use the new `observe` method which returns `Disposable`.",
        level = DeprecationLevel.WARNING,
    )
    override fun unsubscribe(observer: (R) -> Unit) {
        val upstreamObserver = Lock.synchronized { observers.remove(observer) } ?: return

        @Suppress("DEPRECATION") upstream.forEach { it.unsubscribe(upstreamObserver) }
    }

    private object Lock {
        inline fun <T> synchronized(block: () -> T): T = synchronized(this, block)
    }
}