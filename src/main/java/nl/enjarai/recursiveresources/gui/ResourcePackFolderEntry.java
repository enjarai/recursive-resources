package nl.enjarai.recursiveresources.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.pack.PackListWidget;
import net.minecraft.client.gui.screen.pack.PackListWidget.ResourcePackEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import nl.enjarai.recursiveresources.RecursiveResources;
import nl.enjarai.recursiveresources.pack.FolderMeta;
import nl.enjarai.recursiveresources.pack.FolderPack;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class ResourcePackFolderEntry extends ResourcePackEntry {
    public static final Identifier WIDGETS_TEXTURE = RecursiveResources.id("textures/gui/widgets.png");
    public static final String UP_TEXT = "..";

    private static final Text BACK_DESCRIPTION = Text.translatable("recursiveresources.folder.back");
    private static final Text FOLDER_DESCRIPTION = Text.translatable("recursiveresources.folder.folder");
    private static final Text ERRORED_NAME = Text.translatable("recursiveresources.folder.errored").formatted(Formatting.DARK_RED);
    private static final Text ERRORED_DESCRIPTION = Text.translatable("recursiveresources.folder.errored_description").formatted(Formatting.RED);

    private final FolderedResourcePackScreen ownerScreen;
    public final Path folder;
    @Nullable
    public final Path rootFolder;
    public final boolean isUp;
    public final List<ResourcePackEntry> children;
    public final FolderMeta meta;
    private final PackListWidget selectedList;

    private static Function<Path, Path> getIconFileResolver(List<Path> roots, Path folder) {
        return iconPath -> {
            if (iconPath.isAbsolute()) {
                return iconPath;
            } else {
                for (var root : roots) {
                    var iconFile = root
                            .resolve(folder)
                            .resolve(iconPath);

                    if (Files.exists(iconFile)) return iconFile;
                }
            }
            return null;
        };
    }

    public ResourcePackFolderEntry(MinecraftClient client, PackListWidget availablePacks, PackListWidget selectedList, FolderedResourcePackScreen ownerScreen, Path folder, @Nullable Path rootFolder, boolean isUp, FolderMeta meta) {
        super(
                client, availablePacks,
                new FolderPack(
                        meta.errored() ? ERRORED_NAME : Text.of(isUp ? UP_TEXT : String.valueOf(folder.getFileName())),
                        isUp ? BACK_DESCRIPTION : meta.errored() ? ERRORED_DESCRIPTION : FOLDER_DESCRIPTION,
                        getIconFileResolver(ownerScreen.roots, folder),
                        folder, meta
                )
        );
        this.selectedList = selectedList;
        this.ownerScreen = ownerScreen;
        this.folder = folder;
        this.rootFolder = rootFolder;
        this.isUp = isUp;
        this.meta = meta;
        this.children = resolveChildren();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double relativeMouseX = mouseX - (double) widget.getRowLeft();
        if (relativeMouseX <= 32.0D) {
            if(getChildren().isEmpty()) {
                disableChildren();
            } else {
                enableChildren();
            }
            return true;
        }

        ownerScreen.moveToFolder(this.isUp ? this.rootFolder : this.folder);
        return true;
    }

    @Override
    public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        if (pack instanceof FolderPack folderPack) {
            folderPack.setHovered(hovered);
        }

        super.render(context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta);

        if (hovered) {
            context.fill(x, y, x + 32, y + 32, 0xa0909090);

            int relativeMouseX = mouseX - x;

            context.drawTexture(WIDGETS_TEXTURE, x, y, getChildren().isEmpty() ? 32.0F : 0.0F, relativeMouseX < 32 ? 32.0F : 0.0F, 32, 32, 256, 256);
        }
    }

    public void enableChildren() {
        for (ResourcePackEntry entry : getChildren()) {
            if (entry.pack.canBeEnabled()) {
                entry.pack.enable();
            }
        }
    }

    public void disableChildren() {
        for (ResourcePackEntry entry : List.copyOf(this.selectedList.children())) {
            if (this.meta.containsEntry(entry, this.folder) && entry.pack.canBeDisabled()) {
                entry.pack.disable();
            }
        }
    }

    public List<ResourcePackEntry> getChildren() {
        return children;
    }

    private List<ResourcePackEntry> resolveChildren() {
        return widget.children().stream()
                .filter(entry -> !(entry instanceof ResourcePackFolderEntry))
                .filter(entry -> meta.containsEntry(entry, folder))
                .sorted(Comparator.comparingInt(entry -> meta.sortEntry((ResourcePackEntry) entry, folder)).reversed())
                .toList();
    }
}
