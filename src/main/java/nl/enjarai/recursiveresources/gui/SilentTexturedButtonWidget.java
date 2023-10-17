package nl.enjarai.recursiveresources.gui;

import net.minecraft.client.gui.screen.ButtonTextures;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.util.Identifier;

public class SilentTexturedButtonWidget extends TexturedButtonWidget {
    public SilentTexturedButtonWidget(int x, int y, int width, int height, ButtonTextures textures, PressAction pressAction) {
        super(x, y, width, height, textures, pressAction);
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
        // Do nothing
    }
}
