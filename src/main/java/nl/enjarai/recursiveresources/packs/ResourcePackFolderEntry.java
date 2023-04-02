package nl.enjarai.recursiveresources.packs;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.pack.PackListWidget;
import net.minecraft.client.gui.screen.pack.PackListWidget.ResourcePackEntry;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import nl.enjarai.recursiveresources.RecursiveResources;
import nl.enjarai.recursiveresources.gui.CustomResourcePackScreen;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static nl.enjarai.recursiveresources.repository.ResourcePackUtils.isChildOfFolder;

public class ResourcePackFolderEntry extends ResourcePackEntry {
    public static final Identifier WIDGETS_TEXTURE = RecursiveResources.id("textures/gui/widgets.png");
    public static final String UP_TEXT = "..";

    private static final Text BACK_DESCRIPTION = Text.translatable("recursiveresources.folder.back");
    private static final Text FOLDER_DESCRIPTION = Text.translatable("recursiveresources.folder.folder");

    private final CustomResourcePackScreen ownerScreen;
    public final File folder;
    public final boolean isUp;
    public final List<ResourcePackEntry> children;

    private static File findIconFile(List<Path> roots, File folder) {
        for (var root : roots) {
            var iconFile = root
                    .resolve(folder.toPath())
                    .resolve("icon.png")
                    .toFile();

            if (iconFile.exists()) return iconFile;
        }
        return null;
    }

    public ResourcePackFolderEntry(MinecraftClient client, PackListWidget list, CustomResourcePackScreen ownerScreen, File folder, boolean isUp) {
        super(client, list,
                new FolderPack(
                        Text.of(isUp ? UP_TEXT : folder.getName()),
                        isUp ? BACK_DESCRIPTION : FOLDER_DESCRIPTION,
                        findIconFile(ownerScreen.roots, folder),
                        folder
                )
        );
        this.ownerScreen = ownerScreen;
        this.folder = folder;
        this.isUp = isUp;
        this.children = isUp ? List.of() : resolveChildren();
    }

    public ResourcePackFolderEntry(MinecraftClient client, PackListWidget list, CustomResourcePackScreen ownerScreen, File folder) {
        this(client, list, ownerScreen, folder, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double relativeMouseX = mouseX - (double) widget.getRowLeft();
        if (getChildren().size() > 0 && relativeMouseX <= 32.0D) {
            enableChildren();
            return true;
        }

        ownerScreen.moveToFolder(folder);
        return true;
    }

    @Override
    public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        if (pack instanceof FolderPack folderPack) {
            folderPack.setHovered(hovered);
        }

        super.render(matrices, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta);

        if (hovered) {
            DrawableHelper.fill(matrices, x, y, x + 32, y + 32, 0xa0909090);
            RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);

            int relativeMouseX = mouseX - x;

            if (getChildren().size() > 0) {
                if (relativeMouseX < 32) {
                    DrawableHelper.drawTexture(matrices, x, y, 0.0F, 32.0F, 32, 32, 256, 256);
                } else {
                    DrawableHelper.drawTexture(matrices, x, y, 0.0F, 0.0F, 32, 32, 256, 256);
                }
            }
        }
    }

    public void enableChildren() {
        for (ResourcePackEntry entry : getChildren()) {
            if (entry.pack.canBeEnabled()) {
                entry.pack.enable();
            }
        }
    }

    public List<ResourcePackEntry> getChildren() {
        return children;
    }

    private List<ResourcePackEntry> resolveChildren() {
        return widget.children().stream()
                .filter(entry -> !(entry instanceof ResourcePackFolderEntry))
                .filter(entry -> {
                    var resourcePack = ((ResourcePackOrganizer.AbstractPack) entry.pack).profile.createResourcePack();
                    return ownerScreen.roots.stream().anyMatch((root) ->
                            isChildOfFolder(root.resolve(folder.toPath()).toFile(), resourcePack)
                    );
                })
                .toList();
    }
}
