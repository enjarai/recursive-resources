package nl.enjarai.recursiveresources.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.pack.PackListWidget;

public class FolderedPackListWidget extends PackListWidget {
    public FolderedPackListWidget(PackListWidget original, int width, int height, int left) {
        super(MinecraftClient.getInstance(), width, height, original.title);
        replaceEntries(original.children());
        setLeftPos(left);
    }
}
