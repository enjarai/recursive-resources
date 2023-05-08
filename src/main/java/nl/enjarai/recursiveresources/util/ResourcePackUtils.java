package nl.enjarai.recursiveresources.util;

import net.minecraft.client.resource.Format3ResourcePack;
import net.minecraft.client.resource.Format4ResourcePack;
import net.minecraft.resource.AbstractFileResourcePack;
import net.fabricmc.fabric.impl.resource.loader.ModNioResourcePack;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ZipResourcePack;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class ResourcePackUtils {
    private static final File[] EMPTY_FILE_ARRAY = new File[0];

    public static File[] wrap(File[] filesOrNull) {
        return filesOrNull == null ? EMPTY_FILE_ARRAY : filesOrNull;
    }

    public static boolean isFolderBasedPack(File folder) {
        return new File(folder, "pack.mcmeta").exists();
    }

    public static boolean isFolderBasedPack(Path folder) {
        return Files.exists(folder.resolve("pack.mcmeta"));
    }

    public static boolean isFolderButNotFolderBasedPack(File folder) {
        return folder.isDirectory() && !isFolderBasedPack(folder);
    }

    public static boolean isFolderButNotFolderBasedPack(Path folder) {
        return Files.isDirectory(folder) && !isFolderBasedPack(folder);
    }

    public static boolean isPack(Path fileOrFolder) {
        return Files.isDirectory(fileOrFolder) ? isFolderBasedPack(fileOrFolder) : fileOrFolder.toString().endsWith(".zip");
    }

    public static Path determinePackFolder(ResourcePack pack) {
        Class<? extends ResourcePack> cls = pack.getClass();

        if (cls == ZipResourcePack.class || cls == DirectoryResourcePack.class) {
            return ((AbstractFileResourcePack) pack).base.toPath();
        } else if (pack instanceof Format3ResourcePack compatPack) {
            return determinePackFolder(compatPack.parent);
        } else if (pack instanceof Format4ResourcePack compatPack) {
            return determinePackFolder(compatPack.parent);
        } else if (pack instanceof ModNioResourcePack modResourcePack) {
            return Path.of(modResourcePack.getName());
        } else {
            return null;
        }
    }

    public static boolean isChildOfFolder(Path folder, ResourcePack pack) {
        var packFolder = determinePackFolder(pack);
        return packFolder != null && packFolder.toAbsolutePath().startsWith(folder.toAbsolutePath());
    }
}
