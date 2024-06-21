package nl.enjarai.recursiveresources.pack;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.ResourcePackCompatibility;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import nl.enjarai.recursiveresources.RecursiveResources;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

public class FolderPack implements ResourcePackOrganizer.Pack {
    private static final Identifier FOLDER_TEXTURE = RecursiveResources.id("textures/gui/folder.png");
    private static final Identifier OPEN_FOLDER_TEXTURE = RecursiveResources.id("textures/gui/folder_open.png");

    private static Identifier loadCustomIcon(Path icon, Path relativeFolder) {
        if (icon != null && Files.exists(icon)) {
            try (InputStream stream = Files.newInputStream(icon)) {
                // Get the path relative to the resourcepacks directory
                var relativePath = relativeFolder.toString();

                // Ensure the path only contains "a-z0-9_.-" characters
                relativePath = relativePath.toLowerCase().replaceAll("[^a-zA-Z0-9_.-]", "_");

                Identifier id = RecursiveResources.id("textures/gui/folder_icons/" + relativePath + "/" + icon.getFileName());
                MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(NativeImage.read(stream)));
                return id;
            } catch (Exception e) {
                RecursiveResources.LOGGER.warn("Error loading custom folder icon:");
                e.printStackTrace();
            }
        }
        return null;
    }

    private final Text displayName;
    private final Text description;
    @Nullable
    private Identifier icon = null;
    private final FolderMeta meta;

    private boolean hovered = false;

    public FolderPack(Text displayName, Text description, Function<Path, Path> iconFileResolver, Path relativeFolder, FolderMeta meta) {
        this.displayName = displayName;
        if (meta.description().equals("")) {
            this.description = description;
        } else {
            this.description = Text.of(meta.description());
        }
        this.icon = loadCustomIcon(iconFileResolver.apply(meta.icon()), relativeFolder);
        this.meta = meta;
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    @Override
    public Identifier getIconId() {
        return icon != null ? icon : (hovered ? OPEN_FOLDER_TEXTURE : FOLDER_TEXTURE);
    }

    @Override
    public ResourcePackCompatibility getCompatibility() {
        return ResourcePackCompatibility.COMPATIBLE;
    }

    @Override
    public String getName() {
        return displayName.getString();
    }

    @Override
    public Text getDisplayName() {
        return displayName;
    }

    @Override
    public Text getDescription() {
        return description;
    }

    @Override
    public ResourcePackSource getSource() {
        return ResourcePackSource.NONE;
    }

    @Override
    public boolean isPinned() {
        return true;
    }

    @Override
    public boolean isAlwaysEnabled() {
        return true;
    }

    @Override
    public void enable() {

    }

    @Override
    public void disable() {

    }

    @Override
    public void moveTowardStart() {

    }

    @Override
    public void moveTowardEnd() {

    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean canMoveTowardStart() {
        return false;
    }

    @Override
    public boolean canMoveTowardEnd() {
        return false;
    }

    public FolderMeta getMeta() {
        return meta;
    }

    public boolean isVisible() {
        return !meta.hidden();
    }
}
