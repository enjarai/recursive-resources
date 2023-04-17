package nl.enjarai.recursiveresources.pack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.nio.file.Path;
import java.util.List;

public record FolderMeta(Path icon, List<String> packOrder) {
    public static final Codec<FolderMeta> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(Path::of, Path::toString).fieldOf("icon").forGetter(FolderMeta::icon),
            Codec.STRING.listOf().fieldOf("pack_order").forGetter(FolderMeta::packOrder)
    ).apply(instance, FolderMeta::new));
}
