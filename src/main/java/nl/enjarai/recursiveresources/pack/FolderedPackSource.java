package nl.enjarai.recursiveresources.pack;

import net.minecraft.resource.ResourcePackSource;
import net.minecraft.text.Text;

import java.nio.file.Path;

public record FolderedPackSource(Path root, Path file) implements ResourcePackSource {
    @Override
    public Text decorate(Text packName) {
        return packName;
    }
}
