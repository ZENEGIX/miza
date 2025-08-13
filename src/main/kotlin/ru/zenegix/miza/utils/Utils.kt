package ru.zenegix.miza.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.protobuf.ByteString
import com.mojang.serialization.JsonOps
import io.wispforest.owo.ui.component.ButtonComponent.Renderer
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.ParentComponent
import io.wispforest.owo.ui.core.Surface
import net.minecraft.text.Text
import net.minecraft.text.TextCodecs
import java.nio.ByteBuffer
import java.util.*

const val UUID_SIZE_IN_BYTES = 16

val GSON = GsonBuilder().setLenient().create()
val OBJECT_MAPPER = ObjectMapper().findAndRegisterModules()
val lineBottomUnderlineSurface = Surface { context, c ->
    context.fill(c.x() + 1, c.y() + c.height(), c.x() + c.width() - 1, c.y() + c.height() - 1, 0x19ffffff)
}

fun ByteString?.toUUID(): UUID? = this?.takeIf { !it.isEmpty }?.let {
    val byteBuffer = it.asReadOnlyByteBuffer()
    UUID(byteBuffer.long, byteBuffer.long)
}

fun UUID.toByteString(): ByteString {
    val uuidByteBuffer = ByteBuffer.wrap(ByteArray(UUID_SIZE_IN_BYTES))
    uuidByteBuffer.putLong(this.mostSignificantBits)
    uuidByteBuffer.putLong(this.leastSignificantBits)
    return ByteString.copyFrom(uuidByteBuffer.array())
}

inline fun <reified T : Component> ParentComponent.childById(id: String): T {
    return childById(T::class.java, id)
}

fun decodeText(input: String): Text {
    return try {
        TextCodecs.CODEC.decode(JsonOps.INSTANCE, GSON.fromJson(input, JsonElement::class.java)).orThrow.first
    } catch (e: Exception) {
        Text.of(input)
    }
}

fun encodeText(text: Text): String {
    return GSON.toJson(TextCodecs.CODEC.encodeStart(JsonOps.INSTANCE, text).getOrThrow())
}
