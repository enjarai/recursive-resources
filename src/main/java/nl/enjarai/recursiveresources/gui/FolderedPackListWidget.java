package nl.enjarai.recursiveresources.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.pack.PackListWidget;
import net.minecraft.client.gui.screen.pack.PackScreen;

public class FolderedPackListWidget extends PackListWidget {
    public FolderedPackListWidget(PackListWidget original, PackScreen screen, int width, int height, int left) {
        super(MinecraftClient.getInstance(), screen, width, height, original.title);
        replaceEntries(original.children());
        setLeftPos(left);
    }
}
