package nl.enjarai.recursiveresources.pack;

import net.minecraft.resource.*;
import net.minecraft.resource.ResourcePackProfile.InsertionPosition;
import net.minecraft.text.Text;
import nl.enjarai.recursiveresources.util.ResourcePackUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.file.Path;
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
        Path rootPath = root.toPath();
        Path filePath = rootPath.relativize(fileOrFolder.toPath());
        FolderedPackSource packSource = new FolderedPackSource(rootPath, filePath);

        if (fileOrFolder.isDirectory()) {
            info = ResourcePackProfile.create(
                    name, Text.literal(displayName), false,
                    new DirectoryResourcePack.DirectoryBackedFactory(fileOrFolder.toPath(), true),
                    ResourceType.CLIENT_RESOURCES, InsertionPosition.TOP, packSource
            );
        } else {
            info = ResourcePackProfile.create(
                    name, Text.literal(displayName), false,
                    new ZipResourcePack.ZipBackedFactory(fileOrFolder, true),
                    ResourceType.CLIENT_RESOURCES, InsertionPosition.TOP, packSource
            );
        }

        if (info != null) {
            profileAdder.accept(info);
        }
    }
}
