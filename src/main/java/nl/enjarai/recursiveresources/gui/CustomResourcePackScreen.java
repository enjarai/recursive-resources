package nl.enjarai.recursiveresources.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.pack.PackListWidget;
import net.minecraft.client.gui.screen.pack.PackListWidget.ResourcePackEntry;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.pack.ResourcePackOrganizer;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.text.Text;
import nl.enjarai.recursiveresources.packs.ResourcePackFolderEntry;
import nl.enjarai.recursiveresources.packs.ResourcePackListProcessor;
import nl.enjarai.recursiveresources.repository.ResourcePackUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static nl.enjarai.recursiveresources.packs.ResourcePackFolderEntry.WIDGETS_TEXTURE;
import static nl.enjarai.recursiveresources.repository.ResourcePackUtils.wrap;

public class CustomResourcePackScreen extends PackScreen {
    private static final File ROOT_FOLDER = new File("");

    private static final Text OPEN_PACK_FOLDER = Text.translatable("pack.openFolder");
    private static final Text DONE = Text.translatable("gui.done");
    private static final Text SORT_AZ = Text.translatable("recursiveresources.sort.a-z");
    private static final Text SORT_ZA = Text.translatable("recursiveresources.sort.z-a");
    private static final Text VIEW_FOLDER = Text.translatable("recursiveresources.view.folder");
    private static final Text VIEW_FLAT = Text.translatable("recursiveresources.view.flat");

    private final MinecraftClient client = MinecraftClient.getInstance();

    private final ResourcePackListProcessor listProcessor = new ResourcePackListProcessor(this::refresh);
    private Comparator<ResourcePackEntry> currentSorter;

    private PackListWidget originalAvailablePacks;
    private PackListWidgetCustom customAvailablePacks;
    private TextFieldWidget searchField;

    private File currentFolder = ROOT_FOLDER;
    private boolean folderView = true;
    public final List<Path> roots;

    public CustomResourcePackScreen(ResourcePackManager packManager, Consumer<ResourcePackManager> applier, File mainRoot, Text title, List<Path> roots) {
        super(packManager, applier, mainRoot.toPath(), title);
        this.roots = roots;
    }

    // Components

    @Override
    protected void init() {
        super.init();

        findButton(OPEN_PACK_FOLDER).ifPresent(btn -> {
            btn.setX(width / 2 + 25);
            btn.setY(height - 48);
        });

        findButton(DONE).ifPresent(btn -> {
            btn.setX(width / 2 + 25);
            btn.setY(height - 26);
        });

        addDrawableChild(
                ButtonWidget.builder(SORT_AZ, btn -> {
                    listProcessor.setSorter(currentSorter = ResourcePackListProcessor.sortAZ);
                })
                .dimensions(width / 2 - 179, height - 26, 30, 20)
                .build()
        );

        addDrawableChild(
                ButtonWidget.builder(SORT_ZA, btn -> {
                    listProcessor.setSorter(currentSorter = ResourcePackListProcessor.sortZA);
                })
                .dimensions(width / 2 - 179 + 34, height - 26, 30, 20)
                .build()
        );

        addDrawableChild(
                ButtonWidget.builder(folderView ? VIEW_FOLDER : VIEW_FLAT, btn -> {
                    folderView = !folderView;
                    btn.setMessage(folderView ? VIEW_FOLDER : VIEW_FLAT);

                    refresh();
                    customAvailablePacks.setScrollAmount(0.0);
                })
                .dimensions(width / 2 - 179 + 68, height - 26, 86, 20)
                .build()
        );

        // Load all available packs button
        addDrawableChild(new SilentTexturedButtonWidget(width / 2 - 204, 0, 32, 32, 0, 0, WIDGETS_TEXTURE, btn -> {
            for (ResourcePackEntry entry : List.copyOf(availablePackList.children())) {
                if (entry.pack.canBeEnabled()) {
                    entry.pack.enable();
                }
            }
        }));

        // Unload all button
        addDrawableChild(new SilentTexturedButtonWidget(width / 2 + 204 - 32, 0, 32, 32, 32, 0, WIDGETS_TEXTURE, btn -> {
            for (ResourcePackEntry entry : List.copyOf(selectedPackList.children())) {
                if (entry.pack.canBeDisabled()) {
                    entry.pack.disable();
                }
            }
        }));

        searchField = addDrawableChild(new TextFieldWidget(
                textRenderer, width / 2 - 179, height - 46, 154, 16, searchField, Text.of("")));
        searchField.setFocusUnlocked(true);
        searchField.setChangedListener(listProcessor::setFilter);
        addDrawableChild(searchField);

        // Replacing the available pack list with our custom implementation
        originalAvailablePacks = availablePackList;
        remove(originalAvailablePacks);
        addSelectableChild(customAvailablePacks = new PackListWidgetCustom(originalAvailablePacks, this, 200, height, width / 2 - 204));
        availablePackList = customAvailablePacks;

        listProcessor.pauseCallback();
        listProcessor.setSorter(currentSorter == null ? (currentSorter = ResourcePackListProcessor.sortAZ) : currentSorter);
        listProcessor.setFilter(searchField.getText());
        listProcessor.resumeCallback();
    }

    private Optional<ClickableWidget> findButton(Text text) {
        return children.stream()
                .filter(ClickableWidget.class::isInstance)
                .map(ClickableWidget.class::cast)
                .filter(btn -> text.equals(btn.getMessage()))
                .findFirst();
    }

    @Override
    public void updatePackLists() {
        super.updatePackLists();
        if (customAvailablePacks != null) {
            onFiltersUpdated();
        }
    }

    // Processing

    private File getParentFileSafe(File file) {
        var parent = file.getParentFile();
        return parent == null ? ROOT_FOLDER : parent;
    }

    private boolean notInRoot() {
        return folderView && !currentFolder.equals(ROOT_FOLDER);
    }

    private void onFiltersUpdated() {
        List<ResourcePackEntry> folders = null;

        if (folderView) {
            folders = new ArrayList<>();

            // add a ".." entry when not in the root folder
            if (notInRoot()) {
                folders.add(new ResourcePackFolderEntry(client, customAvailablePacks,
                        this, getParentFileSafe(currentFolder), true));
            }

            // create entries for all the folders that aren't packs
            var createdFolders = new ArrayList<Path>();
            for (Path root : roots) {
                var absolute = root.resolve(currentFolder.toPath());

                for (File folder : wrap(absolute.toFile().listFiles(ResourcePackUtils::isFolderButNotFolderBasedPack))) {
                    var relative = root.relativize(folder.toPath().normalize());

                    if (createdFolders.contains(relative)) {
                        continue;
                    }

                    folders.add(new ResourcePackFolderEntry(client, customAvailablePacks,
                            this, relative.toFile()));
                    createdFolders.add(relative);
                }
            }
        }

        listProcessor.apply(customAvailablePacks.children().stream().toList(), folders, customAvailablePacks.children());

        // filter out all entries that aren't in the current folder
        if (folderView) {
            var filteredPacks = customAvailablePacks.children().stream().filter(entry -> {
                // if it's a folder, it's already relative, so we can check easily
                if (entry instanceof ResourcePackFolderEntry folder) {
                    return folder.isUp || currentFolder.equals(getParentFileSafe(folder.folder));
                }

                // if it's a pack, get the folder it's in and check that against all our roots
                File file = ResourcePackUtils.determinePackFolder(((ResourcePackOrganizer.AbstractPack) entry.pack).profile.createResourcePack());
                return file == null ? !notInRoot() : roots.stream().anyMatch((root) -> {
                    var absolute = root.resolve(currentFolder.toPath());
                    return absolute.toFile().equals(getParentFileSafe(file));
                });
            }).toList();

            customAvailablePacks.children().clear();
            customAvailablePacks.children().addAll(filteredPacks);
        }

        customAvailablePacks.setScrollAmount(customAvailablePacks.getScrollAmount());
    }

    public void moveToFolder(File folder) {
        currentFolder = folder;
        refresh();
        customAvailablePacks.setScrollAmount(0.0);
    }

    // UI Overrides

    @Override
    public void tick() {
        super.tick();
        searchField.tick();
    }

    @Override
    public void removed() {
        super.removed();
    }
}
