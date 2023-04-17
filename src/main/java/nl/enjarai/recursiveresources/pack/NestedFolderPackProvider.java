package nl.enjarai.recursiveresources.pack;

import net.minecraft.resource.*;
import net.minecraft.resource.ResourcePackProfile.InsertionPosition;
import net.minecraft.text.Text;
import nl.enjarai.recursiveresources.util.ResourcePackUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.function.Consumer;

public class NestedFolderPackProvider implements ResourcePackProvider {
    protected File root;
    protected int rootLength;

    public NestedFolderPackProvider(File root) {
        this.root = root;
        this.rootLength = root.getAbsolutePath().length();
    }

    @Override
    public void register(Consumer<ResourcePackProfile> profileAdder) {
        File[] folders = root.listFiles(ResourcePackUtils::isFolderButNotFolderBasedPack);

        for (File folder : ResourcePackUtils.wrap(folders)) {
            processFolder(folder, profileAdder);
        }
    }

    public void processFolder(File folder, Consumer<ResourcePackProfile> profileAdder) {
        if (ResourcePackUtils.isFolderBasedPack(folder)) {
            addPack(folder, profileAdder);
            return;
        }

        File[] zipFiles = folder.listFiles(file -> file.isFile() && file.getName().endsWith(".zip"));

        for (File zipFile : ResourcePackUtils.wrap(zipFiles)) {
            addPack(zipFile, profileAdder);
        }

        File[] nestedFolders = folder.listFiles(File::isDirectory);

        for (File nestedFolder : ResourcePackUtils.wrap(nestedFolders)) {
            processFolder(nestedFolder, profileAdder);
        }
    }

    public void addPack(File fileOrFolder, Consumer<ResourcePackProfile> profileAdder) {
        String displayName = fileOrFolder.getName();
        String name = "file/" + StringUtils.removeStart(
                fileOrFolder.getAbsolutePath().substring(rootLength).replace('\\', '/'), "/");
        ResourcePackProfile info;

        if (fileOrFolder.isDirectory()) {
            info = ResourcePackProfile.create(
                    name, Text.literal(displayName), false,
                    (packName) -> new DirectoryResourcePack(packName, fileOrFolder.toPath(), true),
                    ResourceType.CLIENT_RESOURCES, InsertionPosition.TOP, ResourcePackSource.NONE
            );
        } else {
            info = ResourcePackProfile.create(
                    name, Text.literal(displayName), false,
                    (packName) -> new ZipResourcePack(packName, fileOrFolder, true),
                    ResourceType.CLIENT_RESOURCES, InsertionPosition.TOP, ResourcePackSource.NONE
            );
        }

        if (info != null) {
            profileAdder.accept(info);
        }
    }
}
