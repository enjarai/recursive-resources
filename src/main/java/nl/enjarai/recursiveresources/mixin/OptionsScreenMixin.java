package nl.enjarai.recursiveresources.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.text.Text;
import nl.enjarai.recursiveresources.gui.CustomResourcePackScreen;
import nl.enjarai.shared_resources.api.DefaultGameResources;
import nl.enjarai.shared_resources.api.GameResourceHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Consumer;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin {

    @Redirect(method = "method_19824", at = @At(value = "NEW", target = "Lnet/minecraft/client/gui/screen/pack/PackScreen;", remap = true), remap = false)
    private PackScreen replacePackScreen(Screen parent, ResourcePackManager packManager, Consumer<ResourcePackManager> applier, File resourcePackDir, Text title) {
        var packRoots = new ArrayList<Path>();
        packRoots.add(resourcePackDir.toPath());

        if (FabricLoader.getInstance().isModLoaded("shared-resources")) {
            var directory = GameResourceHelper.getPathFor(DefaultGameResources.RESOURCEPACKS);

            if (directory != null) {
                packRoots.add(directory);
            }
        }
        return new CustomResourcePackScreen(parent, packManager, applier, resourcePackDir, title, packRoots);
    }
}