package ru.zenegix.miza.screen

import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.TextBoxComponent
import io.wispforest.owo.ui.container.FlowLayout
import net.minecraft.client.gui.screen.Screen
import net.minecraft.util.Identifier
import ru.zenegix.miza.MizaClient
import ru.zenegix.miza.storage.RoomStorage
import ru.zenegix.miza.utils.childById

class MizaRoomCreateScreen(
    private val previousScreen: Screen,
    private val roomStorage: RoomStorage,
) : BaseUIModelScreen<FlowLayout>(
    FlowLayout::class.java,
    DataSource.asset(Identifier.of(MizaClient.MOD_ID, "room-create"))
) {

    override fun build(rootComponent: FlowLayout) {
        val nameTextBoxComponent = rootComponent.childById<TextBoxComponent>("name-text-box")
        val joinButton = rootComponent.childById<ButtonComponent>("create-button")

        rootComponent.childById<ButtonComponent>("previous-screen-button").let {
            it.onPress {
                client?.setScreen(previousScreen)
            }
        }

        nameTextBoxComponent.setMaxLength(Integer.MAX_VALUE)
        nameTextBoxComponent.onChanged().subscribe {
            joinButton.active(it.isNotBlank())
        }

        joinButton.onPress {
            roomStorage.createRoom(nameTextBoxComponent.text)
            client?.setScreen(MizaRoomScreen(MizaGroupsScreen(roomStorage), roomStorage))
        }

        joinButton.active(false)
    }
}
