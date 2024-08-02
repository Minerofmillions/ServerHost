package minerofmillions.utils

import com.arkivanov.decompose.value.Value

fun <T> List<T>.filter(predicate: (T) -> Value<Boolean>) = FilteredValue(this, predicate)

class FilteredValue<T>(upstream: List<T>, predicate: (T) -> Value<Boolean>) :
    Value<List<T>>() {
    private val mapped = upstream.associateWith(predicate)
    private var lastValueSet: Set<T> = mapped.filterValues(Value<Boolean>::value).keys
    private var lastValue = lastValueSet.toList()
    private val observers = mutableMapOf<(List<T>) -> Unit, (Boolean) -> Unit>()

    override val value: List<T>
        get() = mapCached(mapped.filterValues(Value<Boolean>::value).keys)

    private fun mapCached(values: Set<T>): List<T> = Lock.synchronized {
        if (values != lastValueSet) {
            lastValueSet = values
            lastValue = lastValueSet.toList()
        }

        lastValue
    }

    @Deprecated(
        "Calling this method from Swift doesn't have any effect, because Kotlin wraps the function passed from Swift every time the method is called. Please use the new `observe` method which returns `Disposable`.",
        level = DeprecationLevel.WARNING
    )
    override fun unsubscribe(observer: (List<T>) -> Unit) {
        val upstreamObserver = Lock.synchronized { observers.remove(observer) } ?: return

        @Suppress("DEPRECATION") mapped.values.forEach { it.unsubscribe(upstreamObserver) }
    }

    @Deprecated(
        "Calling this method from Swift leaks the observer, because Kotlin wraps the function passed from Swift every time the method is called. Please use the new `observe` method which returns `Disposable`.",
        level = DeprecationLevel.WARNING
    )
    override fun subscribe(observer: (List<T>) -> Unit) {
        val upstreamObserver: (Boolean) -> Unit = { observer(mapCached(mapped.filterValues { it.value }.keys)) }

        Lock.synchronized {
            if (observer in observers) return

            observers[observer] = upstreamObserver
        }

        @Suppress("DEPRECATION") mapped.values.forEach { it.subscribe(upstreamObserver) }
    }

    private object Lock {
        inline fun <T> synchronized(block: () -> T): T = synchronized(this, block)
    }
}