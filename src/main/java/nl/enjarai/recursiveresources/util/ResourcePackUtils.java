package nl.enjarai.recursiveresources.util;

import net.fabricmc.fabric.impl.resource.loader.ModNioResourcePack;
import net.minecraft.resource.DirectoryResourcePack;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ZipResourcePack;
import nl.enjarai.recursiveresources.RecursiveResources;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class ResourcePackUtils {
    private static final File[] EMPTY_FILE_ARRAY = new File[0];
    // Catches all characters that are not allowed in a path on Windows and Linux
    private static final String UNSAFE_PATH_REGEX = "[<>:\"/\\\\|?*]";

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

    @SuppressWarnings("UnstableApiUsage")
    public static Path determinePackFolder(ResourcePack pack) {
        try {
            switch (pack) {
                case DirectoryResourcePack directoryResourcePack -> {
                    return Path.of(directoryResourcePack.getId().replaceFirst("file",""));
                }
                case ZipResourcePack zipResourcePack -> {
                    return Path.of(zipResourcePack.getId().replaceFirst("file",""));
                }
                case ModNioResourcePack modResourcePack -> {
                    return Path.of(modResourcePack.getId().replaceAll(UNSAFE_PATH_REGEX, "_"));
                }
                case null, default -> {
                    RecursiveResources.LOGGER.warn("Failed to determine source folder for pack: " + pack.getId() + ", unknown pack type: " + pack.getClass().getName());
                    return null;
                }
            }
        } catch (Exception e) {
            RecursiveResources.LOGGER.error("Error determining source folder for pack: " + pack.getId(), e);
            return null;
        }
    }

    public static boolean isChildOfFolder(Path folder, ResourcePack pack) {
        var packFolder = determinePackFolder(pack);
        return packFolder != null && packFolder.toAbsolutePath().startsWith(folder.toAbsolutePath());
    }
}
