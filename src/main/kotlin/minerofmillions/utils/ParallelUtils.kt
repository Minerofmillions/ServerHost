package minerofmillions.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun <E> Collection<E>.forEachParallel(action: suspend (E) -> Unit) = runBlocking(Dispatchers.IO) {
    forEach { launch { action(it) } }
}