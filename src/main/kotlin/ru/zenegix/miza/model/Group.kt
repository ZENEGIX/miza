package ru.zenegix.miza.model

import net.minecraft.text.Text
import ru.zenegix.miza.generated.RoomOuterClass
import ru.zenegix.miza.generated.group
import ru.zenegix.miza.utils.decodeText
import ru.zenegix.miza.utils.encodeText
import ru.zenegix.miza.utils.toByteString
import ru.zenegix.miza.utils.toUUID
import java.text.Collator
import java.util.*

class Group(
    id: UUID,
    displayName: String,
    members: Collection<String>,
    weight: Int,
    prefix: Text,
    suffix: Text,
) {

    private val diffCollector = DiffCollector()
    var id: UUID by diffObservable(diffCollector, id)
    var displayName by diffObservable(diffCollector, displayName)
    var members by diffObservable(
        diffCollector,
        sortedSetOf(Comparator(Collator.getInstance()::compare), *members.toTypedArray())
    )
    var weight by diffObservable(diffCollector, weight)
    var prefix by diffObservable(diffCollector, prefix)
    var suffix by diffObservable(diffCollector, suffix)

    fun clearDiff() {
        diffCollector.clear()
    }

    fun toModel(): RoomOuterClass.Group {
        return group {
            id = this@Group.id.toByteString()
            displayName = this@Group.displayName
            members.addAll(this@Group.members)
            weight = this@Group.weight
            prefix = encodeText(this@Group.prefix)
            suffix = encodeText(this@Group.suffix)
        }
    }

    fun merge(remoteGroup: Group): Group {
        return Group(
            id = this.id,
            displayName = mergeDisplayName(remoteGroup),
            members = mergeMembers(remoteGroup),
            weight = mergeWeight(remoteGroup),
            prefix = mergePrefix(remoteGroup),
            suffix = mergeSuffix(remoteGroup),
        )
    }

    private fun mergeDisplayName(remoteGroup: Group): String {
        val nameDiff = diffCollector.getDiff(Group::displayName)

        return if (nameDiff != null) {
            if (nameDiff is Diff.NewValue<String>) {
                nameDiff.newValue
            } else {
                error("Unknown diff")
            }
        } else {
            remoteGroup.displayName
        }
    }

    private fun mergePrefix(remoteGroup: Group): Text {
        val nameDiff = diffCollector.getDiff(Group::prefix)

        return if (nameDiff != null) {
            if (nameDiff is Diff.NewValue<Text>) {
                nameDiff.newValue
            } else {
                error("Unknown diff")
            }
        } else {
            remoteGroup.prefix
        }
    }

    private fun mergeSuffix(remoteGroup: Group): Text {
        val nameDiff = diffCollector.getDiff(Group::suffix)

        return if (nameDiff != null) {
            if (nameDiff is Diff.NewValue<Text>) {
                nameDiff.newValue
            } else {
                error("Unknown diff")
            }
        } else {
            remoteGroup.suffix
        }
    }

    private fun mergeMembers(remoteGroup: Group): SortedSet<String> {
        val membersDiff = diffCollector.getDiff(Group::members)

        return if (membersDiff != null) {
            if (membersDiff is Diff.CollectionChange<*>) {
                val elementDiffList = membersDiff.diffs
                val remoteMembers = remoteGroup.members.toSortedSet()

                elementDiffList.forEach {
                    if (it is Diff.RemoveCollectionElementChange<*>) {
                        remoteMembers.remove(it.element as String)
                    } else if (it is Diff.AddCollectionElementChange<*>) {
                        remoteMembers.add(it.element as String)
                    } else {
                        error("Unknown element diff")
                    }
                }

                remoteMembers
            } else {
                error("Unknown diff")
            }
        } else {
            remoteGroup.members
        }
    }

    private fun mergeWeight(remoteGroup: Group): Int {
        val nameDiff = diffCollector.getDiff(Group::weight)

        return if (nameDiff != null) {
            if (nameDiff is Diff.NewValue<Int>) {
                nameDiff.newValue
            } else {
                error("Unknown diff")
            }
        } else {
            remoteGroup.weight
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (javaClass != other?.javaClass) {
            return false
        }

        other as Group

        return weight == other.weight &&
                id != other.id &&
                displayName != other.displayName &&
                members != other.members &&
                prefix != other.prefix &&
                suffix != other.suffix
    }

    override fun hashCode(): Int {
        var result = weight
        result = 31 * result + id.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + members.hashCode()
        result = 31 * result + prefix.hashCode()
        result = 31 * result + suffix.hashCode()
        return result
    }

    override fun toString(): String {
        return "Group(id=$id, displayName='$displayName', members=$members, weight=$weight, prefix='$prefix', suffix='$suffix')"
    }

    companion object {

        fun read(model: RoomOuterClass.Group): Group {
            return Group(
                id = model.id.toUUID() ?: error("group.id is not present"),
                displayName = model.displayName.takeIf { it.isNotEmpty() } ?: error("group.displayName is not present"),
                members = model.membersList ?: error("group.members is not present"),
                weight = model.weight,
                prefix = decodeText(model.prefix),
                suffix = decodeText(model.suffix),
            )
        }
    }
}
