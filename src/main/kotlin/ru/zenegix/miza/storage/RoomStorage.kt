package ru.zenegix.miza.storage

import net.minecraft.text.Text
import ru.zenegix.miza.client.TelegraphClient
import ru.zenegix.miza.generated.TokenOuterClass
import ru.zenegix.miza.generated.telegraphV1RW
import ru.zenegix.miza.generated.token
import ru.zenegix.miza.model.Room
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class RoomStorage(
    private val tokenFilePath: Path
) {

    var token: String? = readToken()
        set(value) {
            Files.writeString(
                tokenFilePath,
                value ?: "",
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
            field = value
        }
    private val lock = ReentrantReadWriteLock()
    private var strategy: RoomStorageStrategy? = token?.let {
        try {
            parseToken(it)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @Volatile
    private var groupMemberData: Map<String, GroupMemberData> = emptyMap()

    @Volatile
    private var room: Room? = null

    fun getGroupMemberData(): Map<String, GroupMemberData> {
        return groupMemberData
    }

    fun loadFromFile() {
        strategy = token?.let {
            try {
                parseToken(it)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        loadRoom()
    }

    fun canWrite(): Boolean {
        return strategy?.canWrite() == true
    }

    fun exportToken(admin: Boolean): String? {
        return strategy?.exportToken(admin)
    }

    fun createRoom(name: String) {
        val room = Room(name, mutableListOf())
        val telegraphAccessToken = TelegraphClient.createAccount()
        val pageTitle = "Miza-${UUID.randomUUID()}"
        val pagePath = TelegraphClient.createOrEditPage(
            path = null,
            title = pageTitle,
            content = Base64.getEncoder().encodeToString(room.toModel().toByteArray()),
            accessToken = telegraphAccessToken,
        )
        val token = token {
            telegraphV1Rw = telegraphV1RW {
                this.pagePath = pagePath
                this.pageTitle = pageTitle
                this.accessToken = telegraphAccessToken
            }
        }

        updateToken(Base64.getEncoder().encodeToString(token.toByteArray()))
    }

    fun updateToken(token: String): String? {
        try {
            val parsedStrategy = parseToken(token)
            this.token = token

            lock.write {
                strategy = parsedStrategy
            }
        } catch (e: Exception) {
            return e.message
        } finally {
            loadRoom()
        }

        return null
    }

    fun getRoom(): Room? {
        return lock.read { room }
    }

    fun sync() {
        val localRoom = this.room ?: run {
            loadRoom()
            return
        }

        val remoteRoom = strategy?.readRoom() ?: run {
            updateGroupMemberData(null)
            return
        }
        val mergedRoom = localRoom.merge(remoteRoom)

        if (remoteRoom != mergedRoom) {
            strategy?.writeRoom(mergedRoom)
        }

        this.lock.write {
            this.room = mergedRoom
        }

        updateGroupMemberData(room)
    }

    private fun loadRoom() {
        val strategy = this.strategy

        if (strategy == null) {
            lock.write {
                room = null
            }

            updateGroupMemberData(null)
            return
        }

        val loadedRoom = strategy.readRoom()

        lock.write {
            this.room = loadedRoom
        }

        updateGroupMemberData(loadedRoom)
    }

    private fun readToken(): String? = try {
        Files.readString(tokenFilePath).takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }

    private fun updateGroupMemberData(room: Room?) {
        groupMemberData = room
            ?.groups
            ?.sortedByDescending { it.weight }
            ?.flatMap { group ->
                val groupData = GroupData(
                    prefix = group.prefix,
                    suffix = group.suffix,
                )
                group.members.map { member -> member.lowercase() to groupData }
            }
            ?.groupBy({ it.first }, { it.second })?.mapValues { (_, groupData) ->
                GroupMemberData(
                    prefixes = groupData.map { it.prefix },
                    suffixes = groupData.map { it.suffix },
                )
            }
            ?: emptyMap()
    }

    data class GroupData(
        val prefix: Text,
        val suffix: Text,
    )

    data class GroupMemberData(
        val prefixes: List<Text>,
        val suffixes: List<Text>,
    )

    companion object {
        private fun parseToken(givenToken: String): RoomStorageStrategy? {
            if (givenToken.isBlank()) {
                return null
            }

            val token = try {
                TokenOuterClass.Token.parseFrom(Base64.getDecoder().decode(givenToken))
            } catch (e: Exception) {
                error("Unable to parse token")
            }

            return when (token.payloadCase) {
                TokenOuterClass.Token.PayloadCase.TELEGRAPH_V1_RO -> TelegraphRoomStorageStrategy(
                    TelegraphRoomStorageStrategy.ReadOnlyV1TokenData(
                        pagePath = token.telegraphV1Ro.pagePath,
                    )
                )

                TokenOuterClass.Token.PayloadCase.TELEGRAPH_V1_RW -> TelegraphRoomStorageStrategy(
                    TelegraphRoomStorageStrategy.ReadWriteV1TokenData(
                        accessToken = token.telegraphV1Rw.accessToken,
                        pageTitle = token.telegraphV1Rw.pageTitle,
                        pagePath = token.telegraphV1Rw.pagePath,
                    )
                )

                else -> error(
                    "Current mod version does not support given token that was created " +
                            "on '${token.metadata.creatorModVersion}' mod version"
                )
            }
        }
    }
}
