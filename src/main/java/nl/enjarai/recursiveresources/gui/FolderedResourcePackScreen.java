package nl.enjarai.recursiveresources.gui;

import com.google.common.collect.Lists;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.pack.PackListWidget;
import net.minecraft.client.gui.screen.pack.PackListWidget.ResourcePackEntry;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.text.Text;
import nl.enjarai.recursiveresources.RecursiveResources;
import nl.enjarai.recursiveresources.pack.FolderMeta;
import nl.enjarai.recursiveresources.pack.FolderPack;
import nl.enjarai.recursiveresources.util.ResourcePackListProcessor;
import nl.enjarai.recursiveresources.util.ResourcePackUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class FolderedResourcePackScreen extends PackScreen {
    private static final Path ROOT_FOLDER = Path.of("");

    private static final Text OPEN_PACK_FOLDER = Text.translatable("pack.openFolder");
    private static final Text DONE = Text.translatable("gui.done");
    private static final Text SORT_AZ = Text.translatable("recursiveresources.sort.a-z");
    private static final Text SORT_ZA = Text.translatable("recursiveresources.sort.z-a");
    private static final Text VIEW_FOLDER = Text.translatable("recursiveresources.view.folder");
    private static final Text VIEW_FLAT = Text.translatable("recursiveresources.view.flat");
    private static final Text AVAILABLE_PACKS_TITLE_HOVER = Text.translatable("recursiveresources.availablepacks.title.hover");
    private static final Text SELECTED_PACKS_TITLE_HOVER = Text.translatable("recursiveresources.selectedpacks.title.hover");

    protected final MinecraftClient client = MinecraftClient.getInstance();
    protected final Screen parent;

    private final ResourcePackListProcessor listProcessor = new ResourcePackListProcessor(this::refresh);
    private Comparator<ResourcePackEntry> currentSorter;

    private PackListWidget originalAvailablePacks;
    private PackListWidget customAvailablePacks;
    private PackListWidget originalSelectedPackList;
    private PackListWidget customSelectedPacks;
    private TextFieldWidget searchField;

    private Path currentFolder = ROOT_FOLDER;
    private FolderMeta currentFolderMeta;
    private boolean folderView = true;
    public final List<Path> roots;

    public FolderedResourcePackScreen(Screen parent, ResourcePackManager packManager, Consumer<ResourcePackManager> applier, File mainRoot, Text title, List<Path> roots) {
        super(packManager, applier, mainRoot.toPath(), title);
        this.parent = parent;
        this.roots = roots;
        this.currentFolderMeta = FolderMeta.loadMetaFile(roots, currentFolder);
        this.currentSorter = (pack1, pack2) -> Integer.compare(
                currentFolderMeta.sortEntry(pack1, currentFolder),
                currentFolderMeta.sortEntry(pack2, currentFolder)
        );
    }

    // Components

    @Override
    protected void init() {
        
        addDrawableChild(
            ButtonWidget.builder(folderView ? VIEW_FOLDER : VIEW_FLAT, btn -> {
                    folderView = !folderView;
                    btn.setMessage(folderView ? VIEW_FOLDER : VIEW_FLAT);
                    
                    refresh();
                    customAvailablePacks.setScrollY(0.0);
                })
                .dimensions(ButtonWidget.DEFAULT_WIDTH, ButtonWidget.DEFAULT_HEIGHT, ButtonWidget.DEFAULT_WIDTH, ButtonWidget.DEFAULT_HEIGHT)
                .build()
        );
        
        searchField = addDrawableChild(new TextFieldWidget(
            textRenderer, width / 2 - 179, height - 46, 150, 18, searchField, Text.of("")));
        searchField.setFocusUnlocked(true);
        searchField.setChangedListener(listProcessor::setFilter);
        
        super.init();

        // Replacing the available pack list with our custom implementation
        originalAvailablePacks = availablePackList;
        addDrawableChild(customAvailablePacks = new PackListWidget(client, this,
                originalAvailablePacks.getWidth(), originalAvailablePacks.getHeight(), availablePackList.title));
        customAvailablePacks.setPosition(originalAvailablePacks.getX(), originalAvailablePacks.getY());
        remove(originalAvailablePacks);
        // Make the title of the available packs selector clickable to load all packs
        ((FolderedPackListWidget) customAvailablePacks).recursiveresources$setTitleClickable(AVAILABLE_PACKS_TITLE_HOVER, null, () -> {
            for (ResourcePackEntry entry : Lists.reverse(List.copyOf(availablePackList.children()))) {
                if (entry.pack.canBeEnabled()) {
                    entry.pack.enable();
                }
            }
        });
        availablePackList = customAvailablePacks;

        originalSelectedPackList = selectedPackList;
        remove(originalSelectedPackList);
        addDrawableChild(customSelectedPacks = new PackListWidget(client, this,
                selectedPackList.getWidth(), selectedPackList.getHeight(), selectedPackList.title));
        customSelectedPacks.setPosition(selectedPackList.getX(), selectedPackList.getY());

        // Also make the selected packs title clickable to unload them
        ((FolderedPackListWidget) customSelectedPacks).recursiveresources$setTitleClickable(SELECTED_PACKS_TITLE_HOVER, null, () -> {
            for (ResourcePackEntry entry : Lists.reverse(List.copyOf(customSelectedPacks.children()))) {
                if ((this.currentFolderMeta.containsEntry(entry, this.currentFolder) || currentFolder.equals(ROOT_FOLDER)) && entry.pack.canBeDisabled()) {
                    entry.pack.disable();
                }
            }
        });
        selectedPackList = customSelectedPacks;

        listProcessor.pauseCallback();
        listProcessor.setSorter(currentSorter == null ? (currentSorter = ResourcePackListProcessor.sortAZ) : currentSorter);
        listProcessor.setFilter(searchField.getText());
        listProcessor.resumeCallback();
        
        this.refreshWidgetPositions();
    }
    
    @Override
    protected void refreshWidgetPositions() {
        super.refreshWidgetPositions();
        
        this.moveButtons();
        
        this.availablePackList.setDimensions(200, layout.getContentHeight() - 20);
        this.availablePackList.refreshScroll();
        this.selectedPackList.setDimensions(200, layout.getContentHeight() - 20);
        this.selectedPackList.refreshScroll();
    }
    
    public void moveButtons() {
        findButton(OPEN_PACK_FOLDER).ifPresent(btn -> {
            btn.setX(width / 2 + 35);
            btn.setY(height - 48);
        });
        
        findButton(DONE).ifPresent(btn -> {
            btn.setX(width / 2 + 35);
            btn.setY(height - 26);
            if (btn instanceof ButtonWidget button) {
                button.onPress = btn2 -> applyAndClose();
            }
        });
        
        findButton(VIEW_FOLDER).ifPresent(btn -> {
            btn.setX(width / 2 - 190);
            btn.setY(height - 26);
        });
        
        findButton(VIEW_FLAT).ifPresent(btn -> {
            btn.setX(width / 2 - 190);
            btn.setY(height - 26);
        });
        
        searchField.setX((width / 2 - 190));
        searchField.setY(height - 46);
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

    private Path getParentFileSafe(Path file) {
        var parent = file.getParent();
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
                var rootFolder = getParentFileSafe(currentFolder);
                var meta = FolderMeta.loadMetaFile(roots, currentFolder);
                folders.add(new ResourcePackFolderEntry(client, customAvailablePacks, customSelectedPacks,
                        this, currentFolder, rootFolder, true, meta));
            }

            // create entries for all the folders that aren't packs
            var createdFolders = new ArrayList<Path>();
            for (Path root : roots) {
                var absolute = root.resolve(currentFolder);

                try (var contents = Files.list(absolute)) {
                    for (Path folder : contents.filter(ResourcePackUtils::isFolderButNotFolderBasedPack).toList()) {
                        var relative = root.relativize(folder.normalize());

                        if (createdFolders.contains(relative)) {
                            continue;
                        }

                        var meta = FolderMeta.loadMetaFile(roots, relative);
                        var entry = new ResourcePackFolderEntry(client, customAvailablePacks, customSelectedPacks,
                                this, relative, null, false, meta);

                        if (((FolderPack) entry.pack).isVisible()) {
                            folders.add(entry);
                        }
                        createdFolders.add(relative);
                    }
                } catch (IOException e) {
                    RecursiveResources.LOGGER.error("Failed to read contents of " + absolute, e);
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

                // if it's a pack, we can use the foldermeta to check if it should be shown
                return currentFolderMeta.containsEntry(entry, currentFolder);
            }).toList();

            customAvailablePacks.children().clear();
            customAvailablePacks.children().addAll(filteredPacks);
        }

        customAvailablePacks.setScrollY(customAvailablePacks.getScrollY());
    }

    public void moveToFolder(Path folder) {
        currentFolder = folder;
        currentFolderMeta = FolderMeta.loadMetaFile(roots, currentFolder);
        refresh();
        customAvailablePacks.setScrollY(0.0);
    }

    // UI Overrides

    @Override
    public void tick() {
        super.tick();
    }

    protected void applyAndClose() {
        organizer.apply();
        closeDirectoryWatcher();
    }

    @Override
    public void close() {
        closeDirectoryWatcher();
        client.setScreen(parent);
        client.options.addResourcePackProfilesToManager(client.getResourcePackManager());
    }
}
