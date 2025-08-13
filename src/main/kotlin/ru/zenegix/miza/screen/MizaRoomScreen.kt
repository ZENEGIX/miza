package ru.zenegix.miza.screen

import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.component.TextBoxComponent
import io.wispforest.owo.ui.container.FlowLayout
import net.minecraft.client.gui.screen.Screen
import net.minecraft.util.Identifier
import ru.zenegix.miza.MizaClient
import ru.zenegix.miza.storage.RoomStorage
import ru.zenegix.miza.utils.childById

class MizaRoomScreen(
    private val previousScreen: Screen,
    private val roomStorage: RoomStorage,
) : BaseUIModelScreen<FlowLayout>(
    FlowLayout::class.java,
    DataSource.asset(Identifier.of(MizaClient.MOD_ID, "room"))
) {

    override fun build(rootComponent: FlowLayout) {
        rootComponent.childById<ButtonComponent>("previous-screen-button").let {
            it.onPress {
                client?.setScreen(previousScreen)
            }
        }

        val workingArea = rootComponent.childById<FlowLayout>("working-area")

        workingArea.child(
            model.expandTemplate(LabelComponent::class.java, "title-label", mapOf())
        )

        if (roomStorage.canWrite()) {
            workingArea.child(buildNameChangeComponent())
            workingArea.child(buildCopyTokenButton(CopyTokenType.USER))
            workingArea.child(buildCopyTokenButton(CopyTokenType.ADMIN))
        } else {
            workingArea.child(buildCopyTokenButton(CopyTokenType.COMMON))
        }

        workingArea.child(buildLeaveButton())
    }

    private fun buildNameChangeComponent(): FlowLayout {
        val room = roomStorage.getRoom() ?: error("Room not found")
        val nameChangeComponent = model.expandTemplate(FlowLayout::class.java, "name-change", mapOf())
        val nameChangeTextBox = nameChangeComponent.childById<TextBoxComponent>("name-change-text-box")

        nameChangeTextBox.setMaxLength(Integer.MAX_VALUE)
        nameChangeTextBox.text(room.name)
        nameChangeTextBox.onChanged().subscribe {
            room.name = it
        }

        return nameChangeComponent
    }

    private fun buildCopyTokenButton(type: CopyTokenType): ButtonComponent {
        val buttonComponent = model.expandTemplate(ButtonComponent::class.java, type.templateName, mapOf())

        buttonComponent.onPress {
            roomStorage.exportToken(type == CopyTokenType.ADMIN)?.let {
                client?.keyboard?.clipboard = it
            }
        }

        return buttonComponent
    }

    private fun buildLeaveButton(): ButtonComponent {
        val buttonComponent = model.expandTemplate(ButtonComponent::class.java, "leave-room-button", mapOf())

        buttonComponent.onPress {
            roomStorage.updateToken("")
            client?.setScreen(MizaRoomManageScreen(MizaGroupsScreen(roomStorage), roomStorage))
        }

        return buttonComponent
    }

    private enum class CopyTokenType(
        val templateName: String,
    ) {
        COMMON("copy-token-button"),
        USER("copy-user-token-button"),
        ADMIN("copy-admin-token-button");
    }
}
