package nl.enjarai.recursiveresources.repository;

import net.minecraft.resource.*;
import net.minecraft.resource.ResourcePackProfile.Factory;
import net.minecraft.resource.ResourcePackProfile.InsertionPosition;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.function.Consumer;

public class NestedFolderPackFinder implements ResourcePackProvider {
    protected File root;
    protected int rootLength;

    public NestedFolderPackFinder(File root) {
        this.root = root;
        this.rootLength = root.getAbsolutePath().length();
    }

    @Override
    public void register(Consumer<ResourcePackProfile> profileAdder, Factory factory) {
        File[] folders = root.listFiles(ResourcePackUtils::isFolderButNotFolderBasedPack);

        for (File folder : ResourcePackUtils.wrap(folders)) {
            processFolder(folder, profileAdder, factory);
        }
    }

    public void processFolder(File folder, Consumer<ResourcePackProfile> profileAdder, Factory factory) {
        if (ResourcePackUtils.isFolderBasedPack(folder)) {
            addPack(folder, profileAdder, factory);
            return;
        }

        File[] zipFiles = folder.listFiles(file -> file.isFile() && file.getName().endsWith(".zip"));

        for (File zipFile : ResourcePackUtils.wrap(zipFiles)) {
            addPack(zipFile, profileAdder, factory);
        }

        File[] nestedFolders = folder.listFiles(File::isDirectory);

        for (File nestedFolder : ResourcePackUtils.wrap(nestedFolders)) {
            processFolder(nestedFolder, profileAdder, factory);
        }
    }

    public void addPack(File fileOrFolder, Consumer<ResourcePackProfile> profileAdder) {
        String displayName = fileOrFolder.getName();
        String name = "file/" + StringUtils.removeStart(
                fileOrFolder.getAbsolutePath().substring(rootLength).replace('\\', '/'), "/");
        ResourcePackProfile info;

        if (fileOrFolder.isDirectory()) {
            info = ResourcePackProfile.of(
                    name, false, () -> new DirectoryResourcePack(fileOrFolder),
                    factory, InsertionPosition.TOP, ResourcePackSource.PACK_SOURCE_NONE
            );
        } else {
            info = ResourcePackProfile.of(
                    name, false, () -> new ZipResourcePack(fileOrFolder),
                    factory, InsertionPosition.TOP, ResourcePackSource.PACK_SOURCE_NONE
            );
        }

        if (info != null) {
            profileAdder.accept(info);
        }
    }
}
