package ru.zenegix.miza.mixin.journeymap;

import com.llamalad7.mixinextras.sugar.Local;
import journeymap.client.render.draw.DrawEntityStep;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import ru.zenegix.miza.event.GroupMemberNameDecorateCallback;

@Mixin(DrawEntityStep.class)
public abstract class DrawEntityStepMixin {

    @ModifyArg(
            method = "drawPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Ljourneymap/client/render/draw/DrawUtil;drawBatchLabel(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/text/Text;Ljourneymap/client/render/draw/DrawStep$Pass;Lnet/minecraft/client/render/VertexConsumerProvider;DDLjourneymap/client/render/draw/DrawUtil$HAlign;Ljourneymap/client/render/draw/DrawUtil$VAlign;IFIFDZD)V",
                    ordinal = 0
            ),
            index = 1
    )
    public Text miza$renderPlayerTeamName(
            Text text,
            @Local(ordinal = 0) LivingEntity livingEntity
    ) {
        return decorateName(text, livingEntity);
    }

    @ModifyArg(
            method = "drawPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Ljourneymap/client/render/draw/DrawUtil;drawBatchLabel(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/text/Text;Ljourneymap/client/render/draw/DrawStep$Pass;Lnet/minecraft/client/render/VertexConsumerProvider;DDLjourneymap/client/render/draw/DrawUtil$HAlign;Ljourneymap/client/render/draw/DrawUtil$VAlign;IFIFDZD)V",
                    ordinal = 1
            ),
            index = 1
    )
    public Text miza$renderPlayerName(
            Text text,
            @Local(ordinal = 0) LivingEntity livingEntity
    ) {
        return decorateName(text, livingEntity);
    }

    @Unique
    private static Text decorateName(Text text, LivingEntity livingEntity) {
        Text name = livingEntity.getName();
        return GroupMemberNameDecorateCallback.EVENT.invoker().decorate(
                name.getString(),
                text == null ? name : text
        );
    }
}
