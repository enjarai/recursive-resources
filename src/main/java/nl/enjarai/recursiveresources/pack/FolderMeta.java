package nl.enjarai.recursiveresources.pack;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import nl.enjarai.recursiveresources.RecursiveResources;
import nl.enjarai.recursiveresources.util.ResourcePackUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public record FolderMeta(Path icon, List<Path> packs, boolean hidden) {
    public static final Codec<FolderMeta> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(Path::of, Path::toString).fieldOf("icon").forGetter(FolderMeta::icon),
            Codec.STRING.xmap(Path::of, Path::toString).listOf().fieldOf("packs").forGetter(FolderMeta::packs),
            Codec.BOOL.fieldOf("hidden").forGetter(FolderMeta::hidden)
    ).apply(instance, FolderMeta::new));

    public static final FolderMeta DEFAULT = new FolderMeta(Path.of("icon.png"), List.of(), false);
    public static final String META_FILE_NAME = "folder.json";

    public static FolderMeta loadMetaFile(List<Path> roots, Path folder) {
        for (var root : roots) {
            var metaFile = root
                    .resolve(folder)
                    .resolve(FolderMeta.META_FILE_NAME);
            FolderMeta meta = null;

            if (Files.exists(metaFile)) {
                meta = FolderMeta.load(metaFile);
            }
            if (meta == null) meta = FolderMeta.DEFAULT;

            try (Stream<Path> packs = Files.list(metaFile.getParent())) {
                meta = meta.getRefreshed(packs
                        .filter(ResourcePackUtils::isPack)
                        .map(Path::normalize)
                        .map(root::relativize)
                        .toList()
                );
                meta.save(metaFile);
            } catch (Exception e) {
                RecursiveResources.LOGGER.error("Failed to process meta file for folder " + folder, e);
            }
        }
        return FolderMeta.DEFAULT;
    }

    public static FolderMeta load(Path metaFile) {
        try (var reader = Files.newBufferedReader(metaFile)) {
            var json = JsonParser.parseReader(reader);

            return CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, RecursiveResources.LOGGER::error);
        } catch (Exception e) {
            RecursiveResources.LOGGER.error("Failed to load folder meta file: " + metaFile, e);
            return DEFAULT;
        }
    }

    public void save(Path metaFile) {
        try (var writer = Files.newBufferedWriter(metaFile)) {
            var json = CODEC.encodeStart(JsonOps.INSTANCE, this).getOrThrow(false, RecursiveResources.LOGGER::error);

            writer.write(json.toString());
        } catch (Exception e) {
            RecursiveResources.LOGGER.error("Failed to save folder meta file: " + metaFile, e);
        }
    }

    public FolderMeta getRefreshed(List<Path> packsInFolder) {
        var packs = new ArrayList<>(packs());

        for (var pack : packsInFolder) {
            if (!packs.contains(pack)) {
                packs.add(pack);
            }
        }

        return new FolderMeta(icon, Collections.unmodifiableList(packs), hidden);
    }
}
