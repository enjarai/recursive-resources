package nl.enjarai.recursiveresources.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.io.File;

@Mixin(targets = "net/minecraft/resource/ZipResourcePack$ZipFileWrapper")
public interface ZipFileWrapperAccessor {
    @Accessor
    File getFile();
}
