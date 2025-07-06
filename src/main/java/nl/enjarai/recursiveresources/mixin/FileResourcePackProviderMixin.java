package nl.enjarai.recursiveresources.mixin;

import net.minecraft.resource.FileResourcePackProvider;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FileResourcePackProvider.class)
public abstract class FileResourcePackProviderMixin {
    @Redirect(
            method = "forEachProfile",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;)V"
            ),
            remap = false
    )
    private static void recursiveresources$skipUnnessecaryLogging(Logger instance, String s, Object o) {
        // Don't log anything
    }
}
