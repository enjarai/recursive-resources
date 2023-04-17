package nl.enjarai.recursiveresources.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.DefaultClientResourcePackProvider;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProvider;
import nl.enjarai.recursiveresources.compat.shared_resources.ExternalNestedFolderPackProvider;
import nl.enjarai.recursiveresources.pack.NestedFolderPackProvider;
import nl.enjarai.shared_resources.api.DefaultGameResources;
import nl.enjarai.shared_resources.api.GameResourceHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Mixin(ResourcePackManager.class)
public abstract class ResourcePackManagerMixin {
    @Shadow
    @Final
    @Mutable
    private Set<ResourcePackProvider> providers;

    @Inject(
            method = "<init>([Lnet/minecraft/resource/ResourcePackProvider;)V",
            at = @At("RETURN")
    )
    private void recursiveresources$onInit(CallbackInfo ci) {
        // Only add our own provider if this is the manager of client
        // resource packs, we wouldn't want to mess with datapacks
        if (providers.stream().anyMatch(DefaultClientResourcePackProvider.class::isInstance)) {
            var client = MinecraftClient.getInstance();

            providers = new HashSet<>(providers);
            providers.add(new NestedFolderPackProvider(client.getResourcePackDir().toFile()));

            // Load shared resources compat if present
            if (FabricLoader.getInstance().isModLoaded("shared-resources")) {
                providers.add(new ExternalNestedFolderPackProvider(() -> GameResourceHelper.getPathFor(DefaultGameResources.RESOURCEPACKS)));
            }
        }
    }
}
