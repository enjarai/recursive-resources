package nl.enjarai.recursiveresources.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.pack.PackListWidget;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import nl.enjarai.recursiveresources.gui.FolderedPackListWidget;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PackListWidget.class)
public abstract class PackListWidgetMixin extends EntryListWidgetMixin implements FolderedPackListWidget {
    @Shadow @Final PackScreen screen;
    @Unique
    @Nullable
    private Text titleHoverText;
    @Unique
    @Nullable
    private Text titleTooltip;
    @Unique
    @Nullable
    private Runnable titleClickEvent;

    public PackListWidgetMixin(int i, int j, int k, int l, Text text) {
        super(i, j, k, l, text);
    }

    @Override
    public void recursiveresources$setTitleClickable(Text hoverText, Text tooltip, Runnable clickEvent) {
        this.titleHoverText = hoverText;
        this.titleTooltip = tooltip;
        this.titleClickEvent = clickEvent;
    }

    @Override
    protected boolean recursiveresources$modifyHeaderRendering(EntryListWidget<?> thiz, DrawContext context, int x, int y,
                                                               @Share("mouseX") LocalIntRef mouseXRef, @Share("mouseY") LocalIntRef mouseYRef) {
        if (titleHoverText != null) {
            var text = Text.empty().append(titleHoverText).formatted(Formatting.UNDERLINE, Formatting.BOLD, Formatting.ITALIC);
            int textWidth = client.textRenderer.getWidth(text);

            int left = x + width / 2 - textWidth / 2;
            int top = Math.min(this.getY() + 3, y);
            int right = left + textWidth;
            int bottom = top + client.textRenderer.fontHeight;

            int mouseX = mouseXRef.get();
            int mouseY = mouseYRef.get();

            if (mouseX >= x && mouseX <= x + width && mouseY >= top && mouseY <= bottom) {
                context.drawText(client.textRenderer, text, left, top, 16777215, false);

                if (titleTooltip != null) {
                    screen.setTooltip(List.of(titleTooltip.asOrderedText()));
                }

                return false;
            }
        }

        return true;
    }

    @Override
    protected void recursiveresources$handleHeaderClick(int x, int y, CallbackInfoReturnable<Boolean> ci) {
        if (titleClickEvent != null && y <= client.textRenderer.fontHeight) {
            titleClickEvent.run();
        }
    }
}
