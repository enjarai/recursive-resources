package nl.enjarai.recursiveresources.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ContainerWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntryListWidget.class)
public abstract class EntryListWidgetMixin extends ContainerWidget {
    @Shadow @Final protected MinecraftClient client;

    public EntryListWidgetMixin(int i, int j, int k, int l, Text text) {
        super(i, j, k, l, text);
    }

    @Inject(
            method = "renderWidget",
            at = @At("HEAD")
    )
    private void recursiveresources$captureParams(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci,
                                                  @Share("mouseX") LocalIntRef mouseXRef, @Share("mouseY") LocalIntRef mouseYRef) {
        mouseXRef.set(mouseX);
        mouseYRef.set(mouseY);
    }

    @WrapWithCondition(
            method = "renderWidget",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/widget/EntryListWidget;renderHeader(Lnet/minecraft/client/gui/DrawContext;II)V"
            )
    )
    protected boolean recursiveresources$modifyHeaderRendering(EntryListWidget<?> thiz, DrawContext context, int x, int y,
                                                               @Share("mouseX") LocalIntRef mouseXRef, @Share("mouseY") LocalIntRef mouseYRef) {
        return true;
    }
}
