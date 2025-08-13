package ru.zenegix.miza.screen

import io.wispforest.owo.ui.base.BaseUIModelScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.component.TextBoxComponent
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.ScrollContainer
import io.wispforest.owo.ui.core.Component
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import ru.zenegix.miza.MizaClient
import ru.zenegix.miza.model.Group
import ru.zenegix.miza.storage.RoomStorage
import ru.zenegix.miza.utils.childById
import ru.zenegix.miza.utils.decodeText
import ru.zenegix.miza.utils.encodeText
import ru.zenegix.miza.utils.lineBottomUnderlineSurface
import java.util.concurrent.atomic.AtomicReference

class MizaGroupsScreen(
    private val roomStorage: RoomStorage,
) : BaseUIModelScreen<FlowLayout>(
    FlowLayout::class.java,
    DataSource.asset(Identifier.of(MizaClient.MOD_ID, "groups"))
) {

    private var activeGroupButton: AtomicReference<ButtonComponent?> = AtomicReference()

    private val selectedGroupFloatingChild = FloatingChild()
    private val selectedGroupWorkingAreaFloatingChild = FloatingChild()
    private var activeButton: AtomicReference<ButtonComponent?> = AtomicReference()

    private var toAddCandidate: String? = null

    private val commonButtonRenderer = ButtonComponent.Renderer.flat(0, 0x59101010, 0)
    private val activeButtonRenderer = ButtonComponent.Renderer.flat(0x59101010, 0x59101010, 0)

    override fun build(rootComponent: FlowLayout) {
        val room = roomStorage.getRoom()
        val workingAreaComponent = rootComponent.childById<FlowLayout>("working-area")
        val groupListComponent = rootComponent.childById<FlowLayout>("groups-list")
        val groupsListScrollComponent = rootComponent.childById<ScrollContainer<*>>("groups-list-scroll")
        rootComponent.childById<ButtonComponent>("room-screen-button").onPress {
            if (roomStorage.getRoom() == null) {
                client?.setScreen(MizaRoomManageScreen(this, roomStorage))
            } else {
                client?.setScreen(MizaRoomScreen(this, roomStorage))
            }
        }
        rootComponent.childById<ButtonComponent>("room-sync-button").onPress {
            roomStorage.sync()
            client?.setScreen(MizaGroupsScreen(roomStorage))
        }

        if (roomStorage.canWrite()) {
            val groupsHeaderComponent = workingAreaComponent.childById<FlowLayout>("groups-header")
            val groupAddButton = model.expandTemplate(ButtonComponent::class.java, "groups-header-add-button", mapOf())

            groupAddButton.onPress {
                val group = room?.createGroup() ?: return@onPress
                val (groupEntryComponent, groupButtonComponent) = buildGroupListEntry(groupListComponent, group)

                updateButtonActiveRenderer(activeGroupButton, groupButtonComponent)
                groupListComponent.child(groupEntryComponent)
                groupsListScrollComponent.scrollTo(groupEntryComponent)
                selectedGroupFloatingChild.applyNewChild(configureSelectedGroupComponent(group, groupButtonComponent))
            }.renderer(commonButtonRenderer)

            groupsHeaderComponent.child(groupAddButton)
        }

        selectedGroupFloatingChild.applyFor(workingAreaComponent)

        room?.groups?.forEach { group ->
            groupListComponent.child(buildGroupListEntry(groupListComponent, group).first)
        }
    }

    private fun configureSelectedGroupComponent(
        group: Group,
        groupButtonComponent: ButtonComponent,
    ): FlowLayout {
        val isAdminToken = roomStorage.canWrite()
        val selectedGroupComponent = model.expandTemplate(FlowLayout::class.java, "selected-group", mapOf())
        val groupNameLabel = selectedGroupComponent.childById<LabelComponent>("selected-group-header-name-label")
        val selectedGroupHeaderComponent = selectedGroupComponent.childById<FlowLayout>("selected-group-header")
        val playersButton = model.expandTemplate(
            ButtonComponent::class.java,
            "selected-group-header-players-button",
            mapOf(
                "members-size" to "${group.members.size}",
                "horizontal-sizing" to if (isAdminToken) "40" else "70"
            )
        )
        val workingAreaComponent = selectedGroupComponent.childById<FlowLayout>("selected-group-working-area")
        val selectedGroupPlayersComponent = buildSelectedGroupPlayersComponent(group)
        groupNameLabel.text(Text.literal(group.displayName))

        activeButton.set(playersButton)

        playersButton.onPress(wrapButtonClick(activeButton) {
            selectedGroupWorkingAreaFloatingChild.applyNewChild(selectedGroupPlayersComponent)
        })

        selectedGroupWorkingAreaFloatingChild.clearParents()
        selectedGroupWorkingAreaFloatingChild.applyFor(workingAreaComponent)
        selectedGroupWorkingAreaFloatingChild.applyNewChild(selectedGroupPlayersComponent)

        playersButton.renderer(activeButtonRenderer)
        selectedGroupHeaderComponent.child(playersButton)

        if (isAdminToken) {
            val settingsButton = model.expandTemplate(
                ButtonComponent::class.java,
                "selected-group-header-settings-button",
                mapOf()
            )
            val selectedGroupSettingsComponent = buildSelectedGroupSettingsComponent(
                group,
                groupButtonComponent,
                groupNameLabel
            )
            settingsButton.onPress(wrapButtonClick(activeButton) {
                selectedGroupWorkingAreaFloatingChild.applyNewChild(selectedGroupSettingsComponent)
            })
            settingsButton.renderer(commonButtonRenderer)
            selectedGroupHeaderComponent.child(settingsButton)
        }

        return selectedGroupComponent
    }

    private fun buildSelectedGroupSettingsComponent(
        group: Group,
        groupButtonComponent: ButtonComponent,
        groupNameLabel: LabelComponent
    ): FlowLayout {
        val settingsComponent = model.expandTemplate(FlowLayout::class.java, "selected-group-settings", mapOf())
        val nameTextBoxComponent = settingsComponent.childById<TextBoxComponent>(
            "selected-group-settings-name-text-box"
        )
        val prefixTextBoxComponent = settingsComponent.childById<TextBoxComponent>(
            "selected-group-settings-prefix-text-box"
        )
        val suffixTextBoxComponent = settingsComponent.childById<TextBoxComponent>(
            "selected-group-settings-suffix-text-box"
        )
        nameTextBoxComponent.setMaxLength(Integer.MAX_VALUE)
        prefixTextBoxComponent.setMaxLength(Integer.MAX_VALUE)
        suffixTextBoxComponent.setMaxLength(Integer.MAX_VALUE)
        nameTextBoxComponent.text(group.displayName)
        prefixTextBoxComponent.text(encodeText(group.prefix))
        suffixTextBoxComponent.text(encodeText(group.suffix))

        nameTextBoxComponent.onChanged().subscribe {
            group.displayName = it
            groupNameLabel.text(Text.literal(it))
            groupButtonComponent.message = Text.literal(it)
        }

        prefixTextBoxComponent.onChanged().subscribe {
            group.prefix = decodeText(it)
        }

        suffixTextBoxComponent.onChanged().subscribe {
            group.suffix = decodeText(it)
        }

        return settingsComponent
    }

    private fun buildSelectedGroupPlayersComponent(
        group: Group
    ): FlowLayout {
        val selectedGroupPlayersComponent = model.expandTemplate(
            FlowLayout::class.java,
            "selected-group-players",
            mapOf()
        )
        val selectedGroupPlayersListScroll = model.expandTemplate(
            ScrollContainer::class.java,
            "selected-group-players-list-scroll",
            mapOf()
        )
        val playerList = selectedGroupPlayersListScroll.childById<FlowLayout>("selected-group-players-list")

        if (roomStorage.canWrite()) {
            val selectedGroupPlayersAddComponent = model.expandTemplate(
                FlowLayout::class.java,
                "selected-group-players-add",
                mapOf()
            )
            val playersAddTextBoxComponent = selectedGroupPlayersAddComponent.childById<TextBoxComponent>(
                "selected-group-players-add-text-box"
            )
            val playersAddButtonComponent = selectedGroupPlayersAddComponent.childById<ButtonComponent>(
                "selected-group-players-add-button"
            )

            playersAddTextBoxComponent.onChanged().subscribe {
                toAddCandidate = it.takeIf { it.isNotBlank() }
            }

            playersAddButtonComponent.onPress {
                toAddCandidate?.let { candidate ->
                    val members = group.members

                    if (members.none { it.equals(candidate, true) }) {
                        members.add(candidate)
                        playerList.child(
                            buildSelectedGroupPlayersEntry(group, playerList, candidate)
                        )
                    }
                }
                toAddCandidate = null
                playersAddTextBoxComponent.text("")
            }

            playersAddButtonComponent.renderer(commonButtonRenderer)
            selectedGroupPlayersComponent.child(selectedGroupPlayersAddComponent)
        }

        group.members.forEach { member ->
            playerList.child(buildSelectedGroupPlayersEntry(group, playerList, member))
        }

        selectedGroupPlayersComponent.child(selectedGroupPlayersListScroll)

        return selectedGroupPlayersComponent
    }

    private fun buildSelectedGroupPlayersEntry(
        group: Group,
        playerListComponent: FlowLayout,
        member: String
    ): FlowLayout {
        val isAdminToken = roomStorage.canWrite()
        val component = model.expandTemplate(
            FlowLayout::class.java,
            "selected-group-players-entry",
            mapOf(
                "member-name" to member,
                "horizontal-sizing" to if (isAdminToken) "95" else "100"
            )
        )
        component.surface(lineBottomUnderlineSurface)

        if (isAdminToken) {
            val deleteButtonComponent = model.expandTemplate(
                ButtonComponent::class.java,
                "selected-group-players-entry-delete-button",
                mapOf()
            )
            deleteButtonComponent.renderer(commonButtonRenderer)
            deleteButtonComponent.onPress {
                playerListComponent.removeChild(component)
                group.members.remove(member)
            }
            component.child(deleteButtonComponent)
        }

        return component
    }

    private fun buildGroupListEntry(groupListComponent: FlowLayout, group: Group): Pair<FlowLayout, ButtonComponent> {
        val isAdminToken = roomStorage.canWrite()
        val entryComponent = this.model.expandTemplate(
            FlowLayout::class.java,
            "groups-list-entry",
            mapOf(
                "group-name" to group.displayName,
                "horizontal-sizing" to if (isAdminToken) "90" else "100"
            )
        )
        val groupButtonComponent = entryComponent.childById<ButtonComponent>("groups-list-entry-button")

        if (isAdminToken) {
            val deleteButton =
                model.expandTemplate(ButtonComponent::class.java, "groups-list-entry-delete-button", mapOf())

            deleteButton.onPress {
                val currentActiveGroupButton = activeGroupButton.get()

                roomStorage.getRoom()?.removeGroup(group)

                if (currentActiveGroupButton == groupButtonComponent) {
                    activeGroupButton.set(null)
                    selectedGroupFloatingChild.unapply()
                    selectedGroupWorkingAreaFloatingChild.unapply()
                }

                groupListComponent.removeChild(entryComponent)
            }.renderer(commonButtonRenderer)

            entryComponent.child(deleteButton)
        }

        entryComponent.surface(lineBottomUnderlineSurface)

        groupButtonComponent
            .renderer(commonButtonRenderer)
            .onPress(wrapButtonClick(activeGroupButton) {
                selectedGroupFloatingChild.applyNewChild(configureSelectedGroupComponent(group, groupButtonComponent))
            })

        return entryComponent to groupButtonComponent
    }

    private fun wrapButtonClick(
        activeButton: AtomicReference<ButtonComponent?>,
        action: (ButtonComponent) -> Unit = {},
    ): (ButtonComponent) -> Unit = { button ->
        updateButtonActiveRenderer(activeButton, button)
        action(button)
    }

    private fun updateButtonActiveRenderer(
        activeButton: AtomicReference<ButtonComponent?>,
        button: ButtonComponent
    ) {
        activeButton.get()?.renderer(commonButtonRenderer)
        activeButton.set(button)
        button.renderer(activeButtonRenderer)
    }

    class FloatingChild {

        private val parents: MutableList<FlowLayout> = mutableListOf()
        private var child: Component? = null

        fun applyNewChild(newChild: Component?) {
            if (child == newChild) {
                return
            }

            unapply()
            child = newChild

            parents.forEach { parent ->
                parent.child(newChild)
            }
        }

        fun clearParents() {
            unapply()
            parents.clear()
        }

        fun applyFor(parentComponent: FlowLayout) {
            parents.add(parentComponent)
            child?.let(parentComponent::child)
        }

        fun unapply() {
            child?.let {
                parents.forEach { parent ->
                    parent.removeChild(it)
                }
            }
        }
    }
}
