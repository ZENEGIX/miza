package ru.zenegix.miza.model

import net.minecraft.text.Text
import ru.zenegix.miza.generated.RoomOuterClass
import ru.zenegix.miza.generated.room
import java.util.*

class Room(
    name: String,
    groups: MutableList<Group>
) {

    private val diffCollector = DiffCollector()
    var name: String by diffObservable(diffCollector, name)
    val groups: MutableList<Group> by diffObservable(diffCollector, groups)

    fun clearDiff() {
        diffCollector.clear()
    }

    fun createGroup(): Group {
        return Group(
            id = UUID.randomUUID(),
            displayName = "Новая группа",
            members = emptyList(),
            weight = groups.size,
            prefix = Text.literal(""),
            suffix = Text.literal(""),
        ).also {
            groups.add(it)
        }
    }

    fun removeGroup(group: Group) {
        groups.remove(group)
    }

    fun toModel(): RoomOuterClass.Room {
        return room {
            name = this@Room.name
            groups.addAll(this@Room.groups.map(Group::toModel))
        }
    }

    fun merge(remoteRoom: Room): Room {
        return Room(
            name = mergeName(remoteRoom),
            groups = mergeGroups(remoteRoom),
        )
    }

    private fun mergeName(remoteRoom: Room): String {
        val nameDiff = diffCollector.getDiff(Room::name)

        return if (nameDiff != null) {
            if (nameDiff is Diff.NewValue<String>) {
                nameDiff.newValue
            } else {
                error("Unknown diff")
            }
        } else {
            remoteRoom.name
        }
    }

    private fun mergeGroups(remoteRoom: Room): MutableList<Group> {
        val groupsDiff = diffCollector.getDiff(Room::groups)
        val localGroupsById = groups.associateBy { it.id }
        val remoteGroupsById = remoteRoom.groups.associateBy { it.id }.mapValues { (id, remoteGroup) ->
            val localGroup = localGroupsById[id] ?: return@mapValues remoteGroup

            localGroup.merge(remoteGroup)
        }.toMutableMap()

        val groups = if (groupsDiff != null) {
            if (groupsDiff is Diff.CollectionChange<*>) {
                val elementDiffList = groupsDiff.diffs

                elementDiffList.forEach {
                    if (it is Diff.RemoveCollectionElementChange<*>) {
                        val removedGroup = it.element as Group
                        remoteGroupsById.remove(removedGroup.id)
                    } else if (it is Diff.AddCollectionElementChange<*>) {
                        val addedGroup = it.element as Group
                        remoteGroupsById[addedGroup.id] = it.element
                    } else {
                        error("Unknown element diff")
                    }
                }

                remoteGroupsById.values
            } else {
                error("Unknown diff")
            }
        } else {
            remoteGroupsById.values
        }

        return groups.asSequence()
            .onEach { it.clearDiff() }
            .sortedBy { it.weight }
            .also { it.firstOrNull()?.let { it.weight = 0 } }
            .runningReduce { acc, group ->
                group.weight = acc.weight + 1
                group
            }.toMutableList()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (javaClass != other?.javaClass) {
            return false
        }

        other as Room
        return name == other.name && groups == other.groups
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + groups.hashCode()
        return result
    }

    override fun toString(): String {
        return "Room(name='$name', groups=$groups)"
    }

    companion object {
        fun read(model: RoomOuterClass.Room): Room {
            return Room(
                name = model.name,
                groups = model.groupsList.map(Group.Companion::read).toMutableList(),
            )
        }
    }
}
