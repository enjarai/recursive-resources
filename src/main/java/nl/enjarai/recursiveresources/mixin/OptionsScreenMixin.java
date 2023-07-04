package nl.enjarai.recursiveresources.mixin;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.text.Text;
import nl.enjarai.recursiveresources.gui.FolderedResourcePackScreen;
import nl.enjarai.shared_resources.api.DefaultGameResources;
import nl.enjarai.shared_resources.api.GameResourceHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.nio.file.Path;
import java.util.ArrayList;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
    protected OptionsScreenMixin(Text title) {
        super(title);
    }

    @Shadow
    protected abstract void refreshResourcePacks(ResourcePackManager resourcePackManager);

    /**
     * @author recursiveresources
     * @reason Replace the resource packs screen with a custom one.
     */
    @Overwrite
    private Screen method_47631() {
        var client = MinecraftClient.getInstance();
        var packRoots = new ArrayList<Path>();
        packRoots.add(client.getResourcePackDir());

        if (FabricLoader.getInstance().isModLoaded("shared-resources")) {
            var directory = GameResourceHelper.getPathFor(DefaultGameResources.RESOURCEPACKS);

            if (directory != null) {
                packRoots.add(directory);
            }
        }

        return new FolderedResourcePackScreen(
                this, client.getResourcePackManager(),
                this::refreshResourcePacks, client.getResourcePackDir().toFile(),
                Text.translatable("resourcePack.title"),
                packRoots
        );
    }
}
