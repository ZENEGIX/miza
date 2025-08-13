package ru.zenegix.miza.mixin.owo;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.core.Size;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LabelComponent.class, remap = false)
public abstract class LabelComponentMixin extends BaseComponent {

    @Inject(
            method = "inflate",
            at = @At(value = "HEAD")
    )
    public void miza$callSuperInflateOnMethodHead(Size space, CallbackInfo ci) {
        super.inflate(space);
    }
}
