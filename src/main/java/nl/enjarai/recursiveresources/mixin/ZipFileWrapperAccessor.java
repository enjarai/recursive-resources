package nl.enjarai.recursiveresources.mixin;

import net.minecraft.resource.ZipResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.io.File;

@Mixin(ZipResourcePack.ZipFileWrapper.class)
public interface ZipFileWrapperAccessor {
    @Accessor
    File getFile();
}
