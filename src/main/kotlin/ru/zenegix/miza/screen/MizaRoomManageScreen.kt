package ru.zenegix.miza.screen

import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.container.FlowLayout
import net.minecraft.client.gui.screen.Screen
import net.minecraft.util.Identifier
import ru.zenegix.miza.MizaClient
import ru.zenegix.miza.storage.RoomStorage
import ru.zenegix.miza.utils.childById

class MizaRoomManageScreen(
    private val previousScreen: Screen,
    private val roomStorage: RoomStorage,
) : BaseUIModelScreen<FlowLayout>(
    FlowLayout::class.java,
    DataSource.asset(Identifier.of(MizaClient.MOD_ID, "room-manage"))
) {

    override fun build(rootComponent: FlowLayout) {
        rootComponent.childById<ButtonComponent>("previous-screen-button").let {
            it.onPress {
                client?.setScreen(previousScreen)
            }
        }

        rootComponent.childById<ButtonComponent>("room-join-screen-button").let {
            it.onPress {
                client?.setScreen(MizaRoomJoinScreen(this, roomStorage))
            }
        }

        rootComponent.childById<ButtonComponent>("room-create-screen-button").let {
            it.onPress {
                client?.setScreen(MizaRoomCreateScreen(this, roomStorage))
            }
        }
    }
}
