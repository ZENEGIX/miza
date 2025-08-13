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

class MizaRoomJoinScreen(
    private val previousScreen: Screen,
    private val roomStorage: RoomStorage,
) : BaseUIModelScreen<FlowLayout>(
    FlowLayout::class.java,
    DataSource.asset(Identifier.of(MizaClient.MOD_ID, "room-join"))
) {

    override fun build(rootComponent: FlowLayout) {
        val errorAreaComponent = rootComponent.childById<FlowLayout>("error-area")
        val tokenTextBoxComponent = rootComponent.childById<TextBoxComponent>("token-text-box")
        val joinButton = rootComponent.childById<ButtonComponent>("join-button")

        rootComponent.childById<ButtonComponent>("previous-screen-button").let {
            it.onPress {
                client?.setScreen(previousScreen)
            }
        }

        tokenTextBoxComponent.setMaxLength(Integer.MAX_VALUE)
        tokenTextBoxComponent.onChanged().subscribe {
            joinButton.active(it.isNotBlank())
        }

        joinButton.onPress {
            val error = roomStorage.updateToken(tokenTextBoxComponent.text)

            if (error == null) {
                client?.setScreen(MizaRoomScreen(MizaGroupsScreen(roomStorage), roomStorage))
            } else {
                errorAreaComponent.clearChildren()
                errorAreaComponent.child(
                    model.expandTemplate(
                        FlowLayout::class.java,
                        "error",
                        mapOf("error" to error)
                    )
                )
            }
        }

        joinButton.active(false)
    }
}
