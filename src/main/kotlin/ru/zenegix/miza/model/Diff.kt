package ru.zenegix.miza.model

import java.util.*
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

class DiffCollector {

    private val diffMap = mutableMapOf<KProperty<*>, Diff.Change<*>>()

    fun <V> addDiff(property: KProperty<V>, change: Diff.Change<V>) {
        diffMap[property] = change
    }

    fun <V> getDiff(property: KProperty<V>): Diff.Change<V>? {
        return diffMap[property] as? Diff.Change<V>
    }

    fun clear() {
        diffMap.clear()
    }

    override fun toString(): String {
        return "DiffCollector(diffMap=$diffMap)"
    }
}

private class DiffMutableList<E>(
    diffCollector: DiffCollector,
    property: KProperty<*>,
    private val target: MutableList<E>,
) : MutableList<E> by target {

    private val handle = DiffMutableCollectionHandle<SortedSet<E>, E>(diffCollector, property, target)

    override fun add(element: E): Boolean {
        return handle.add(element)
    }

    override fun remove(element: E): Boolean {
        return handle.remove(element)
    }

    override fun hashCode(): Int {
        return handle.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return handle == other
    }

    override fun toString(): String {
        return handle.toString()
    }
}

private class DiffSortedSet<E>(
    diffCollector: DiffCollector,
    property: KProperty<*>,
    private val target: SortedSet<E>,
) : SortedSet<E> by target {

    private val handle = DiffMutableCollectionHandle<SortedSet<E>, E>(diffCollector, property, target)

    override fun add(element: E): Boolean {
        return handle.add(element)
    }

    override fun remove(element: E): Boolean {
        return handle.remove(element)
    }

    override fun hashCode(): Int {
        return handle.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return handle == other
    }

    override fun toString(): String {
        return handle.toString()
    }
}

private class DiffMutableCollectionHandle<C : MutableCollection<E>, E>(
    private val diffCollector: DiffCollector,
    private val property: KProperty<*>,
    private val target: MutableCollection<E>,
) {

    val listDiff by lazy {
        Diff.CollectionChange<E>(mutableListOf()).also {
            diffCollector.addDiff(property as KProperty<C>, it as Diff.Change<C>)
        }
    }

    fun add(element: E): Boolean {
        listDiff.diffs.add(Diff.AddCollectionElementChange(element))
        return target.add(element)
    }

    fun remove(element: E): Boolean {
        listDiff.diffs.add(Diff.RemoveCollectionElementChange(element))
        return target.remove(element)
    }

    override fun hashCode(): Int {
        return target.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return target == other
    }

    override fun toString(): String {
        return target.toString()
    }
}

class Diff<T, V>(
    val property: KProperty1<T, V>,
    val change: Change<V>
) {

    sealed interface Change<V>

    sealed interface ValueChange<V> : Change<V>

    sealed interface CollectionElementChange<E> : Change<E>

    data class NewValue<V>(
        val oldValue: V,
        val newValue: V,
    ) : ValueChange<V>

    data class AddCollectionElementChange<E>(
        val element: E,
    ) : CollectionElementChange<E>

    data class RemoveCollectionElementChange<E>(
        val element: E,
    ) : CollectionElementChange<E>

    data class CollectionChange<E>(
        val diffs: MutableList<CollectionElementChange<E>>
    ) : Change<List<E>>
}

class DiffObservable<T, V>(
    private val diffCollector: DiffCollector,
    private var value: V,
) {

    operator fun getValue(room: T, property: KProperty<*>): V {
        return value
    }

    operator fun setValue(room: T, property: KProperty<*>, s: V) {
        diffCollector.addDiff(property, Diff.NewValue(value, s))
        value = s
    }
}

class DiffCollectionObservable<T, C : MutableCollection<E>, E>(
    private val diffCollector: DiffCollector,
    private val initialValue: C,
    private val valueFactory: (DiffCollector, KProperty<*>, C) -> C
) {

    private var value: C? = null

    operator fun getValue(room: T, property: KProperty<*>): C {
        return getValue(property)
    }

    operator fun setValue(room: T, property: KProperty<*>, s: C) {
        diffCollector.addDiff(property, Diff.NewValue(value, s))
        value = s
    }

    private fun getValue(property: KProperty<*>): C {
        if (value == null) {
            value = valueFactory(diffCollector, property, initialValue)
        }

        return value!!
    }
}

fun <T, V> diffObservable(diffCollector: DiffCollector, initialValue: V): DiffObservable<T, V> {
    return DiffObservable(diffCollector, initialValue)
}

fun <T, E> diffObservable(
    diffCollector: DiffCollector,
    initialValue: MutableList<E>
): DiffCollectionObservable<T, MutableList<E>, E> {
    return DiffCollectionObservable(diffCollector, initialValue, ::DiffMutableList)
}

fun <T, E> diffObservable(
    diffCollector: DiffCollector,
    initialValue: SortedSet<E>
): DiffCollectionObservable<T, SortedSet<E>, E> {
    return DiffCollectionObservable(diffCollector, initialValue, ::DiffSortedSet)
}
