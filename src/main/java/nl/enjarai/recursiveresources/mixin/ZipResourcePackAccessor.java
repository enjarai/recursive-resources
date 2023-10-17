package nl.enjarai.recursiveresources.mixin;

import net.minecraft.resource.ZipResourcePack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ZipResourcePack.class)
public interface ZipResourcePackAccessor {
    @Accessor("zipFile")
    ZipResourcePack.ZipFileWrapper getZipFileWrapper();
}
