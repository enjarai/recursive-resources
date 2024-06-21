package nl.enjarai.recursiveresources.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.gui.screen.pack.PackListWidget;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;
import net.minecraft.resource.ResourcePackSource;
import nl.enjarai.recursiveresources.RecursiveResources;
import nl.enjarai.recursiveresources.gui.ResourcePackFolderEntry;
import nl.enjarai.recursiveresources.util.ResourcePackUtils;
import org.slf4j.Marker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public record FolderMeta(Path icon, String description, List<Path> packs, boolean hidden, boolean errored) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DUMMY_ROOT_PATH = Path.of("/");
    private static final Path EMPTY_PATH = Path.of("");
    public static final Codec<FolderMeta> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(Path::of, Path::toString).fieldOf("icon").forGetter(FolderMeta::icon),
            Codec.STRING.fieldOf("description").forGetter(FolderMeta::description),
            Codec.STRING.xmap(Path::of, Path::toString).listOf().fieldOf("packs").forGetter(FolderMeta::packs),
            Codec.BOOL.fieldOf("hidden").forGetter(FolderMeta::hidden)
    ).apply(instance, FolderMeta::new));

    public static final FolderMeta DEFAULT = new FolderMeta(Path.of("icon.png"), "", List.of(), false);
    public static final FolderMeta ERRORED = new FolderMeta(Path.of("icon.png"), "", List.of(), false, true);
    public static final String META_FILE_NAME = "folder.json";

    public FolderMeta(Path icon, String description, List<Path> packs, boolean hidden) {
        this(icon, description, packs, hidden, false);
    }

    public static FolderMeta loadMetaFile(List<Path> roots, Path folder) {
        for (var root : roots) {
            var rootedFolder = root.resolve(folder);
            var metaFile = rootedFolder.resolve(FolderMeta.META_FILE_NAME);

            if (Files.exists(rootedFolder) && Files.isDirectory(rootedFolder)) {
                FolderMeta meta = FolderMeta.DEFAULT;

                if (Files.exists(metaFile)) {
                    meta = FolderMeta.load(metaFile);

                    //if contents of folder have changed, refresh meta file
                    if (meta.packs().stream().anyMatch(pack -> !Files.exists(rootedFolder.resolve(pack)))) {
                        meta = FolderMeta.DEFAULT;

                        try (Stream<Path> packs = Files.list(rootedFolder)) {
                            meta = meta.getRefreshed(packs
                                    .filter(ResourcePackUtils::isPack)
                                    .map(Path::normalize)
                                    .map(rootedFolder::relativize)
                                    .toList()
                            );
                            meta.save(metaFile);
                        } catch (Exception e) {
                            RecursiveResources.LOGGER.error("Failed to process meta file for folder " + folder, e);
                        }
                    }
                }

                if (!meta.errored()) {
                    try (Stream<Path> packs = Files.list(rootedFolder)) {
                        meta = meta.getRefreshed(packs
                                .filter(ResourcePackUtils::isPack)
                                .map(Path::normalize)
                                .map(rootedFolder::relativize)
                                .toList()
                        );
                        meta.save(metaFile);
                    } catch (Exception e) {
                        RecursiveResources.LOGGER.error("Failed to process meta file for folder " + folder, e);
                    }
                }

                return meta;
            }
        }
        return FolderMeta.DEFAULT;
    }

    private static Path relativiseRelativePath(Path folder, Path path) {
        var packPath = DUMMY_ROOT_PATH.resolve(path);
        var folderPath = DUMMY_ROOT_PATH.resolve(folder);
        return folderPath.relativize(packPath);
    }

    public static FolderMeta load(Path metaFile) {
        try (var reader = Files.newBufferedReader(metaFile)) {
            var json = JsonParser.parseReader(reader);

            return CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        } catch (Exception e) {
            RecursiveResources.LOGGER.error("Failed to load folder meta file: " + metaFile, e);
            return ERRORED;
        }
    }

    public void save(Path metaFile) {
        if (!errored) {
            try (var writer = Files.newBufferedWriter(metaFile)) {
                var json = CODEC.encodeStart(JsonOps.INSTANCE, this).getOrThrow();

                writer.write(GSON.toJson(json));
            } catch (Exception e) {
                RecursiveResources.LOGGER.error("Failed to save folder meta file: " + metaFile, e);
            }
        } else {
            RecursiveResources.LOGGER.warn("Skipped overwriting meta file due to previous error: " + metaFile);
        }
    }

    public FolderMeta getRefreshed(List<Path> packsInFolder) {
        var packs = new ArrayList<>(packs());

        for (var pack : packsInFolder) {
            if (!packs.contains(pack)) {
                packs.add(pack);
            }
        }

        return new FolderMeta(icon, description, Collections.unmodifiableList(packs), hidden, errored);
    }

    public int sortEntry(PackListWidget.ResourcePackEntry entry, Path folder) {
        if (entry.pack.getSource() instanceof FolderedPackSource folderedPackSource) {
            var packIndex = packs().indexOf(relativiseRelativePath(folder, folderedPackSource.file()));
            if (packIndex != -1) return packIndex;
        }

        if (entry instanceof ResourcePackFolderEntry) return Integer.MIN_VALUE;

        return Integer.MAX_VALUE;
    }

    public boolean containsEntry(PackListWidget.ResourcePackEntry entry, Path folder) {
        Path pack;
        Path packParent = EMPTY_PATH;

        if (entry.pack.getSource() instanceof FolderedPackSource folderedPackSource) {
            pack = folderedPackSource.file();
        } else if (entry.pack.getSource() == ResourcePackSource.BUILTIN) {
            pack = EMPTY_PATH.resolve(entry.getName());

            if (folder.equals(EMPTY_PATH)) return true;
        } else {
            Path fsPath = ResourcePackUtils.determinePackFolder(((ResourcePackOrganizer.AbstractPack) entry.pack).profile.createResourcePack());

            if (fsPath == null) return false;

            pack = EMPTY_PATH.resolve(fsPath.getFileName());
            packParent = fsPath.getParent() != null ? fsPath.getParent() : EMPTY_PATH;
        }

        Path relativePath = relativiseRelativePath(folder, pack);
        return packs().contains(relativePath) || packParent.endsWith(folder);
    }
}
