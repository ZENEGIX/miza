package ru.zenegix.miza.storage

import ru.zenegix.miza.client.TelegraphClient
import ru.zenegix.miza.generated.RoomOuterClass
import ru.zenegix.miza.generated.telegraphV1RO
import ru.zenegix.miza.generated.telegraphV1RW
import ru.zenegix.miza.generated.token
import ru.zenegix.miza.model.Room
import java.util.*

class TelegraphRoomStorageStrategy(
    private val token: TokenData
) : RoomStorageStrategy {

    override fun canWrite(): Boolean {
        return token is ReadWriteV1TokenData
    }

    override fun readRoom(): Room? {
        return TelegraphClient.readPageContent(token.pagePath)?.let {
            Room.read(RoomOuterClass.Room.parseFrom(Base64.getDecoder().decode(it)))
        }
    }

    override fun writeRoom(room: Room) {
        if (token is ReadWriteV1TokenData) {
            TelegraphClient.createOrEditPage(
                path = token.pagePath,
                title = token.pageTitle,
                content = Base64.getEncoder().encodeToString(room.toModel().toByteArray()),
                accessToken = token.accessToken
            )
        }
    }

    override fun exportToken(admin: Boolean): String {
        val tokenData = if (admin) {
            if (token is ReadWriteV1TokenData) {
                token {
                    telegraphV1Rw = telegraphV1RW {
                        this.pagePath = token.pagePath
                        this.pageTitle = token.pageTitle
                        this.accessToken = token.accessToken
                    }
                }
            } else {
                error("Cant export admin token because you use user token")
            }
        } else {
            token {
                telegraphV1Ro = telegraphV1RO {
                    this.pagePath = token.pagePath
                }
            }
        }

        return Base64.getEncoder().encodeToString(tokenData.toByteArray())
    }

    sealed interface TokenData {
        val pagePath: String
    }

    data class ReadOnlyV1TokenData(
        override val pagePath: String,
    ) : TokenData

    data class ReadWriteV1TokenData(
        val accessToken: String,
        val pageTitle: String,
        override val pagePath: String,
    ) : TokenData
}
