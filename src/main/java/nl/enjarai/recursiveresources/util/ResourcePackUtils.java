package nl.enjarai.recursiveresources.util;

import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ZipResourcePack;

import java.io.File;

public class ResourcePackUtils {
    private static final File[] EMPTY_FILE_ARRAY = new File[0];

    public static File[] wrap(File[] filesOrNull) {
        return filesOrNull == null ? EMPTY_FILE_ARRAY : filesOrNull;
    }

    public static boolean isFolderBasedPack(File folder) {
        return new File(folder, "pack.mcmeta").exists();
    }

    public static boolean isFolderButNotFolderBasedPack(File folder) {
        return folder.isDirectory() && !isFolderBasedPack(folder);
    }

    public static File determinePackFolder(ResourcePack pack) {
        if (pack instanceof DirectoryResourcePack directoryResourcePack) {
            return directoryResourcePack.root.toFile();
        } else if (pack instanceof ZipResourcePack zipResourcePack) {
            return zipResourcePack.backingZipFile;
        } else {
            return null;
        }
    }

    public static boolean isChildOfFolder(File folder, ResourcePack pack) {
        File packFolder = determinePackFolder(pack);
        return packFolder != null && packFolder.getAbsolutePath().startsWith(folder.getAbsolutePath());
    }
}
