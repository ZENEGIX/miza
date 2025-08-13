package ru.zenegix.miza.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.zenegix.miza.event.GroupMemberNameDecorateCallback;

@Mixin(PlayerEntity.class)
public abstract class PlayerMixin {

    @Shadow
    public abstract Text getName();

    @Inject(
            method = "getDisplayName",
            at = @At(
                    value = "RETURN"
            ),
            cancellable = true
    )
    public void miza$decorateGetName(CallbackInfoReturnable<Text> cir) {
        Text displayName = cir.getReturnValue();
        Text name = getName();
        var decoratedName = GroupMemberNameDecorateCallback.EVENT.invoker().decorate(
                name.getString(),
                displayName == null ? name : displayName
        );

        cir.setReturnValue(decoratedName);
    }
}
