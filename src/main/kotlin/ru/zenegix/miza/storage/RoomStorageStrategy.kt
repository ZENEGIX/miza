package ru.zenegix.miza.storage

import ru.zenegix.miza.model.Room

interface RoomStorageStrategy {

    fun canWrite(): Boolean

    fun readRoom(): Room?

    fun writeRoom(room: Room)

    fun exportToken(admin: Boolean): String
}
