package ru.zenegix.miza

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.MinecraftClient
import net.minecraft.client.option.KeyBinding
import org.lwjgl.glfw.GLFW
import ru.zenegix.miza.event.GroupMemberNameDecorateCallback
import ru.zenegix.miza.screen.MizaGroupsScreen
import ru.zenegix.miza.storage.RoomStorage
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MizaClient : ClientModInitializer {

    companion object {
        const val MOD_ID = "miza"

        private val GROUPS_KEY = KeyBinding("key.miza.groups", GLFW.GLFW_KEY_H, "key.categories.misc")
    }

    override fun onInitializeClient() {
        val modConfigFolder = File("./config/miza").also { it.mkdirs() }
        val tokenFile = modConfigFolder.resolve("token").also {
            if (!it.exists()) {
                it.createNewFile()
            }
        }
        val roomStorage = RoomStorage(tokenFile.toPath())
        roomStorage.loadFromFile()
        KeyBindingHelper.registerKeyBinding(GROUPS_KEY)

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client: MinecraftClient ->
            if (GROUPS_KEY.wasPressed()) {
                client.setScreen(MizaGroupsScreen(roomStorage))
            }
        })

        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay({
            roomStorage.sync()
        }, 10, 300, TimeUnit.SECONDS)

        GroupMemberNameDecorateCallback.EVENT.register(GroupMemberNameDecorateCallback { target, name ->
            val groupMemberData = roomStorage.getGroupMemberData()
            val memberData = groupMemberData[target] ?: return@GroupMemberNameDecorateCallback name
            var result = name

            result = memberData.prefixes.fold(result) { acc, prefix ->
                prefix.copy().append(acc)
            }

            result = memberData.suffixes.fold(result) { acc, suffix ->
                acc.copy().append(suffix)
            }

            return@GroupMemberNameDecorateCallback result
        })
    }
}
