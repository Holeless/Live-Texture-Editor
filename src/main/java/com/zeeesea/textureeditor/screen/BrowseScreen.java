package com.zeeesea.textureeditor.screen;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;

import com.zeeesea.textureeditor.editor.ExternalEditorManager;
import com.zeeesea.textureeditor.editor.ExternalEditorSession;
import com.zeeesea.textureeditor.texture.ItemTextureExtractor;
import com.zeeesea.textureeditor.texture.MobTextureExtractor;
import com.zeeesea.textureeditor.texture.TextureExtractor;
import com.zeeesea.textureeditor.texture.TextureManager;
import com.zeeesea.textureeditor.texture.TextureResourceLoader;
import com.zeeesea.textureeditor.util.BlockFilter;
import com.zeeesea.textureeditor.util.EntityMapper;
import com.zeeesea.textureeditor.util.ImageColorCompat;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class BrowseScreen extends Screen {
   private BrowseScreen.Tab currentTab = BrowseScreen.Tab.ALL;
   private EditBox searchField;
   private String searchQuery = "";
   private List<BrowseScreen.BrowseEntry> allEntries;
   private List<BrowseScreen.BrowseEntry> filteredEntries = new ArrayList();
   private static final int CELL_SIZE = 36;
   private static final int CELL_PADDING = 4;
   private static final int GRID_TOP = 60;
   private static final int GRID_SIDE_MARGIN = 10;
   private int columns;
   private int visibleRows;
   private int scrollOffset = 0;
   private int maxScroll = 0;
   private Button tabAll;
   private Button tabBlocks;
   private Button tabItems;
   private Button tabMobs;
   private Button tabGui;
   private Button tabEntity;
   private Button tabSky;

   public BrowseScreen() {
      super(Component.literal("Texture Browser"));
   }

   protected void init() {
      if (this.allEntries == null) {
         this.allEntries = this.buildEntries();
      }

      int tabY = 5;
      int guiScale = (int)Minecraft.getInstance().getWindow().getGuiScale();
      if (guiScale >= 5) {
         this.addRenderableWidget(Button.builder(Component.literal("Tab: " + this.currentTab.name()), btn -> {
            BrowseScreen.Tab[] tabs = BrowseScreen.Tab.values();
            BrowseScreen.Tab next = tabs[(this.currentTab.ordinal() + 1) % (tabs.length - 1)];
            this.switchTab(next);
            btn.setMessage(Component.literal("Tab: " + next.name()));
         }).pos(10, tabY).size(80, 20).build());
         this.addRenderableWidget(
            Button.builder(Component.literal("Sky"), btn -> this.minecraft.setScreen(new SkyEditorScreen(this)))
               .pos(94, tabY)
               .size(40, 20)
               .build()
         );
         this.tabAll = this.tabBlocks = this.tabItems = this.tabMobs = this.tabGui = this.tabEntity = null;
         this.tabSky = null;
      } else {
         int tabX = 10;
         int tabW = 50;
         this.tabAll = (Button)this.addRenderableWidget(
            Button.builder(Component.literal("All"), btn -> this.switchTab(BrowseScreen.Tab.ALL))
               .pos(tabX, tabY)
               .size(tabW, 20)
               .build()
         );
         tabX += tabW + 4;
         this.tabBlocks = (Button)this.addRenderableWidget(
            Button.builder(Component.literal("Blocks"), btn -> this.switchTab(BrowseScreen.Tab.BLOCKS))
               .pos(tabX, tabY)
               .size(tabW, 20)
               .build()
         );
         tabX += tabW + 4;
         this.tabItems = (Button)this.addRenderableWidget(
            Button.builder(Component.literal("Items"), btn -> this.switchTab(BrowseScreen.Tab.ITEMS))
               .pos(tabX, tabY)
               .size(tabW, 20)
               .build()
         );
         tabX += tabW + 4;
         this.tabMobs = (Button)this.addRenderableWidget(
            Button.builder(Component.literal("Mobs"), btn -> this.switchTab(BrowseScreen.Tab.MOBS))
               .pos(tabX, tabY)
               .size(tabW, 20)
               .build()
         );
         tabX += tabW + 4;
         this.tabGui = (Button)this.addRenderableWidget(
            Button.builder(Component.literal("GUI"), btn -> this.switchTab(BrowseScreen.Tab.GUI))
               .pos(tabX, tabY)
               .size(tabW, 20)
               .build()
         );
         tabX += tabW + 4;
         this.tabEntity = (Button)this.addRenderableWidget(
            Button.builder(Component.literal("Entities"), btn -> this.switchTab(BrowseScreen.Tab.ENTITY))
               .pos(tabX, tabY)
               .size(tabW, 20)
               .build()
         );
         tabX += tabW + 4;
         this.tabSky = (Button)this.addRenderableWidget(
            Button.builder(Component.literal("Sky"), btn -> this.minecraft.setScreen(new SkyEditorScreen(this)))
               .pos(tabX, tabY)
               .size(tabW, 20)
               .build()
         );
      }

      this.searchField = new EditBox(this.font, this.width / 2 - 80, 32, 160, 18, Component.literal("Search"));
      this.searchField.setMaxLength(64);
      this.searchField.setValue(this.searchQuery);
      this.searchField.setResponder(text -> {
         this.searchQuery = text.toLowerCase();
         this.applyFilter();
         this.scrollOffset = 0;
      });
      this.addRenderableWidget(this.searchField);
      this.addRenderableWidget(
         Button.builder(Component.literal("§cClose"), btn -> this.onClose())
            .pos(this.width - 65, 5)
            .size(60, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("§6Export"), btn -> this.minecraft.setScreen(new ExportScreen(this)))
            .pos(this.width - 130, 5)
            .size(60, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Settings"), btn -> this.minecraft.setScreen(new SettingsScreen(this)))
            .pos(this.width - 195, 5)
            .size(60, 20)
            .build()
      );
      this.columns = Math.max(1, (this.width - 20) / 40);
      this.visibleRows = (this.height - 60 - 10) / 40;
      this.applyFilter();
   }

   private void switchTab(BrowseScreen.Tab tab) {
      this.currentTab = tab;
      this.scrollOffset = 0;
      this.applyFilter();
   }

   private void applyFilter() {
      this.filteredEntries = (List<BrowseScreen.BrowseEntry>)this.allEntries
         .stream()
         .filter(
            e -> {
               if (this.currentTab == BrowseScreen.Tab.BLOCKS && e.type != BrowseScreen.EntryType.BLOCK) {
                  return false;
               } else if (this.currentTab == BrowseScreen.Tab.ITEMS && e.type != BrowseScreen.EntryType.ITEM) {
                  return false;
               } else if (this.currentTab == BrowseScreen.Tab.MOBS && e.type != BrowseScreen.EntryType.MOB) {
                  return false;
               } else if (this.currentTab == BrowseScreen.Tab.GUI && e.type != BrowseScreen.EntryType.GUI) {
                  return false;
               } else if (this.currentTab == BrowseScreen.Tab.ENTITY && e.type != BrowseScreen.EntryType.ENTITY) {
                  return false;
               } else {
                  return this.currentTab == BrowseScreen.Tab.ALL && e.type == BrowseScreen.EntryType.GUI
                     ? false
                     : this.searchQuery.isEmpty()
                        || e.name.toLowerCase().contains(this.searchQuery)
                        || e.id.toString().toLowerCase().contains(this.searchQuery);
               }
            }
         )
         .collect(Collectors.toList());
      int totalRows = (this.filteredEntries.size() + this.columns - 1) / this.columns;
      this.maxScroll = Math.max(0, totalRows - this.visibleRows);
   }

   private List<BrowseScreen.BrowseEntry> buildEntries() {
      List<BrowseScreen.BrowseEntry> entries = new ArrayList();
      Set<ResourceLocation> addedItems = new HashSet();

      for (Block block : BuiltInRegistries.BLOCK) {
         ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
         if (!blockId.getPath().equals("air") && BlockFilter.isEditableBlock(block.defaultBlockState())) {
            ItemStack stack = new ItemStack(block);
            if (!stack.isEmpty()) {
               entries.add(new BrowseScreen.BrowseEntry(blockId, block.getName().getString(), BrowseScreen.EntryType.BLOCK, stack));
               addedItems.add(BuiltInRegistries.ITEM.getKey(block.asItem()));
            }
         }
      }

      for (Item item : BuiltInRegistries.ITEM) {
         ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
         if (!addedItems.contains(itemId) && !(item instanceof BlockItem)) {
            ItemStack stack = new ItemStack(item);
            BrowseScreen.EntryType type = item instanceof SpawnEggItem ? BrowseScreen.EntryType.MOB : BrowseScreen.EntryType.ITEM;
            entries.add(new BrowseScreen.BrowseEntry(itemId, item.getDescription().getString(), type, stack));
         }
      }

      entries.addAll(this.buildGuiEntries());
      entries.addAll(this.buildNotNormalEntries());
      entries.addAll(this.buildEntities());
      return entries;
   }

   private List<BrowseScreen.BrowseEntry> buildEntities() {
      List<BrowseScreen.BrowseEntry> entries = new ArrayList();
      this.addEntityArmorEntry(entries, "chainmail_layer_1");
      this.addEntityArmorEntry(entries, "chainmail_layer_2");
      this.addEntityArmorEntry(entries, "chainmail_piglin_helmet");
      this.addEntityArmorEntry(entries, "diamond_layer_1");
      this.addEntityArmorEntry(entries, "diamond_layer_2");
      this.addEntityArmorEntry(entries, "diamond_piglin_helmet");
      this.addEntityArmorEntry(entries, "gold_layer_1");
      this.addEntityArmorEntry(entries, "gold_layer_2");
      this.addEntityArmorEntry(entries, "gold_piglin_helmet");
      this.addEntityArmorEntry(entries, "iron_layer_1");
      this.addEntityArmorEntry(entries, "iron_layer_2");
      this.addEntityArmorEntry(entries, "iron_piglin_helmet");
      this.addEntityArmorEntry(entries, "leather_layer_1");
      this.addEntityArmorEntry(entries, "leather_layer_1_overlay");
      this.addEntityArmorEntry(entries, "leather_layer_2");
      this.addEntityArmorEntry(entries, "leather_layer_2_overlay");
      this.addEntityArmorEntry(entries, "leather_piglin_helmet");
      this.addEntityArmorEntry(entries, "leather_piglin_helmet_overlay");
      this.addEntityArmorEntry(entries, "netherite_layer_1");
      this.addEntityArmorEntry(entries, "netherite_layer_2");
      this.addEntityArmorEntry(entries, "netherite_piglin_helmet");
      this.addEntityArmorEntry(entries, "piglin_leather_layer_1");
      this.addEntityArmorEntry(entries, "piglin_leather_layer_1_overlay");
      this.addEntityArmorEntry(entries, "turtle_layer_1");
      entries.sort(Comparator.comparing(e -> e.name));
      return entries;
   }

   private void addEntityArmorEntry(List<BrowseScreen.BrowseEntry> entries, String armorFileName) {
      ResourceLocation id = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/models/armor/" + armorFileName + ".png");
      entries.add(new BrowseScreen.BrowseEntry(id, this.formatArmorDisplayName(armorFileName), BrowseScreen.EntryType.ENTITY, null));
   }

   private String formatArmorDisplayName(String armorFileName) {
      boolean isOverlay = armorFileName.endsWith("_overlay");
      String base = isOverlay ? armorFileName.substring(0, armorFileName.length() - "_overlay".length()) : armorFileName;
      String[] words = base.split("_");
      StringBuilder sb = new StringBuilder();
      boolean previousWasLayer = false;

      for (String word : words) {
         if (!sb.isEmpty()) {
            sb.append(' ');
         }

         if (word.matches("\\d+")) {
            if (previousWasLayer) {
               sb.append(word);
            } else {
               sb.append("Layer ").append(word);
            }

            previousWasLayer = false;
         } else {
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            previousWasLayer = "layer".equals(word);
         }
      }

      if (isOverlay) {
         sb.append(" Overlay");
      }

      sb.append(" Armor");
      return sb.toString();
   }

   private List<BrowseScreen.BrowseEntry> buildGuiEntries() {
      List<BrowseScreen.BrowseEntry> entries = new ArrayList();
      this.addGUIEntry(entries, "gui/sprites/hud/hotbar", "Hotbar");
      this.addGUIEntry(entries, "gui/sprites/hud/hotbar_selection", "Hotbar Selection");
      this.addGUIEntry(entries, "gui/sprites/hud/crosshair", "Crosshair");
      this.addGUIEntry(entries, "gui/sprites/hud/experience_bar_background", "XP Bar Background");
      this.addGUIEntry(entries, "gui/sprites/hud/experience_bar_progress", "XP Bar Progress");
      this.addGUIEntry(entries, "gui/sprites/hud/armor_empty", "Armor Empty");
      this.addGUIEntry(entries, "gui/sprites/hud/armor_half", "Armor Half");
      this.addGUIEntry(entries, "gui/sprites/hud/armor_full", "Armor Full");
      this.addGUIEntry(entries, "gui/sprites/hud/heart/full", "Heart Full");
      this.addGUIEntry(entries, "gui/sprites/hud/heart/half", "Heart Half");
      this.addGUIEntry(entries, "gui/sprites/hud/heart/container", "Heart Container");
      this.addGUIEntry(entries, "gui/sprites/hud/food_empty", "Food Empty");
      this.addGUIEntry(entries, "gui/sprites/hud/food_half", "Food Half");
      this.addGUIEntry(entries, "gui/sprites/hud/food_full", "Food Full");
      this.addGUIEntry(entries, "gui/sprites/hud/air", "Air Bubble");
      this.addGUIEntry(entries, "gui/sprites/hud/air_bursting", "Air Bubble Bursting");
      this.addGUIEntry(entries, "gui/container/inventory", "Inventory");
      this.addGUIEntry(entries, "gui/container/crafting_table", "Crafting Table");
      this.addGUIEntry(entries, "gui/container/furnace", "Furnace");
      this.addGUIEntry(entries, "gui/container/blast_furnace", "Blast Furnace");
      this.addGUIEntry(entries, "gui/container/smoker", "Smoker");
      this.addGUIEntry(entries, "gui/container/generic_54", "Large Chest");
      this.addGUIEntry(entries, "gui/container/shulker_box", "Shulker Box UI");
      this.addGUIEntry(entries, "gui/container/dispenser", "Dispenser/Dropper");
      this.addGUIEntry(entries, "gui/container/hopper", "Hopper");
      this.addGUIEntry(entries, "gui/container/brewing_stand", "Brewing Stand");
      this.addGUIEntry(entries, "gui/container/enchanting_table", "Enchanting Table");
      this.addGUIEntry(entries, "gui/container/anvil", "Anvil");
      this.addGUIEntry(entries, "gui/container/beacon", "Beacon");
      this.addGUIEntry(entries, "gui/container/villager", "Villager Trading");
      this.addGUIEntry(entries, "gui/container/grindstone", "Grindstone");
      this.addGUIEntry(entries, "gui/container/loom", "Loom");
      this.addGUIEntry(entries, "gui/container/cartography_table", "Cartography Table");
      this.addGUIEntry(entries, "gui/container/stonecutter", "Stonecutter");
      this.addGUIEntry(entries, "gui/container/smithing", "Smithing Table");
      this.addGUIEntry(entries, "gui/container/creative_inventory/tab_inventory", "Creative Inventory BG");
      this.addGUIEntry(entries, "gui/container/creative_inventory/tab_items", "Creative Items BG");
      this.addGUIEntry(entries, "gui/sprites/widget/button", "Button");
      this.addGUIEntry(entries, "gui/sprites/widget/button_highlighted", "Button Highlighted");
      this.addGUIEntry(entries, "gui/sprites/widget/button_disabled", "Button Disabled");
      this.addGUIEntry(entries, "gui/sprites/widget/slider", "Slider");
      this.addGUIEntry(entries, "gui/sprites/widget/slider_highlighted", "Slider Highlighted");
      this.addGUIEntry(entries, "gui/sprites/widget/slider_handle", "Slider Handle");
      this.addGUIEntry(entries, "gui/sprites/widget/slider_handle_highlighted", "Slider Handle HL");
      this.addGUIEntry(entries, "gui/sprites/widget/text_field", "Text Field");
      this.addGUIEntry(entries, "gui/sprites/widget/text_field_highlighted", "Text Field HL");
      this.addGUIEntry(entries, "gui/title/minecraft", "Title Logo");
      this.addGUIEntry(entries, "gui/title/edition", "Edition Badge");
      return entries;
   }

   private List<BrowseScreen.BrowseEntry> buildNotNormalEntries() {
      List<BrowseScreen.BrowseEntry> entries = new ArrayList();
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/boat/oak.png"), "Oak Boat", BrowseScreen.EntryType.MOB, new ItemStack(Items.OAK_BOAT)
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/boat/spruce.png"),
            "Spruce Boat",
            BrowseScreen.EntryType.MOB,
            new ItemStack(Items.SPRUCE_BOAT)
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/boat/birch.png"),
            "Birch Boat",
            BrowseScreen.EntryType.MOB,
            new ItemStack(Items.BIRCH_BOAT)
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/boat/jungle.png"),
            "Jungle Boat",
            BrowseScreen.EntryType.MOB,
            new ItemStack(Items.JUNGLE_BOAT)
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/boat/acacia.png"),
            "Acacia Boat",
            BrowseScreen.EntryType.MOB,
            new ItemStack(Items.ACACIA_BOAT)
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/boat/dark_oak.png"),
            "Dark Oak Boat",
            BrowseScreen.EntryType.MOB,
            new ItemStack(Items.DARK_OAK_BOAT)
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/boat/mangrove.png"),
            "Mangrove Boat",
            BrowseScreen.EntryType.MOB,
            new ItemStack(Items.MANGROVE_BOAT)
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/boat/cherry.png"),
            "Cherry Boat",
            BrowseScreen.EntryType.MOB,
            new ItemStack(Items.CHERRY_BOAT)
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/boat/bamboo.png"),
            "Bamboo Raft",
            BrowseScreen.EntryType.MOB,
            new ItemStack(Items.BAMBOO_RAFT)
         )
      );
      BuiltInRegistries.ITEM
         .getOptional(ResourceLocation.withDefaultNamespace("pale_oak_boat"))
         .ifPresent(
            paleOakBoat -> entries.add(
               new BrowseScreen.BrowseEntry(
                  ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/boat/pale_oak.png"),
                  "Pale Oak Boat",
                  BrowseScreen.EntryType.MOB,
                  new ItemStack(paleOakBoat)
               )
            )
         );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/minecart.png"), "Minecart", BrowseScreen.EntryType.MOB, new ItemStack(Items.MINECART)
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/elytra.png"), "Elytra", BrowseScreen.EntryType.MOB, new ItemStack(Items.ELYTRA)
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/sheep/sheep_fur.png"), "Sheep Fur", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(new BrowseScreen.BrowseEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/wolf/wolf.png"), "Wolf", BrowseScreen.EntryType.MOB, null));
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/wolf/wolf_angry.png"), "Wolf (Angry)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/wolf/wolf_tame.png"), "Wolf (Tame)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/cat/tabby.png"), "Cat (Tabby)", BrowseScreen.EntryType.MOB, null)
      );
      entries.add(
         new BrowseScreen.BrowseEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/cat/black.png"), "Cat (Black)", BrowseScreen.EntryType.MOB, null)
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/cat/british_shorthair.png"), "Cat (British Shorthair)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/cat/calico.png"), "Cat (Calico)", BrowseScreen.EntryType.MOB, null)
      );
      entries.add(
         new BrowseScreen.BrowseEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/cat/jellie.png"), "Cat (Jellie)", BrowseScreen.EntryType.MOB, null)
      );
      entries.add(
         new BrowseScreen.BrowseEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/cat/ocelot.png"), "Cat (Ocelot)", BrowseScreen.EntryType.MOB, null)
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/cat/persian.png"), "Cat (Persian)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/cat/ragdoll.png"), "Cat (Ragdoll)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/cat/red.png"), "Cat (Red)", BrowseScreen.EntryType.MOB, null)
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/cat/siamese.png"), "Cat (Siamese)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/cat/white.png"), "Cat (White)", BrowseScreen.EntryType.MOB, null)
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/horse/horse_white.png"), "Horse (White)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/horse/horse_creamy.png"), "Horse (Creamy)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/horse/horse_chestnut.png"), "Horse (Chestnut)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/horse/horse_brown.png"), "Horse (Brown)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/horse/horse_black.png"), "Horse (Black)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/horse/horse_gray.png"), "Horse (Gray)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/horse/horse_darkbrown.png"), "Horse (Dark Brown)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/horse/horse_markings_white.png"), "Horse Markings (White)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/horse/horse_markings_whitefield.png"),
            "Horse Markings (Whitefield)",
            BrowseScreen.EntryType.MOB,
            null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/horse/horse_markings_whitedots.png"),
            "Horse Markings (White Dots)",
            BrowseScreen.EntryType.MOB,
            null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/horse/horse_markings_blackdots.png"),
            "Horse Markings (Black Dots)",
            BrowseScreen.EntryType.MOB,
            null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/villager/villager.png"), "Villager", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/villager/profession/armorer.png"), "Villager (Armorer)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/villager/profession/butcher.png"), "Villager (Butcher)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/villager/profession/cartographer.png"),
            "Villager (Cartographer)",
            BrowseScreen.EntryType.MOB,
            null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/villager/profession/cleric.png"), "Villager (Cleric)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/villager/profession/farmer.png"), "Villager (Farmer)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/villager/profession/fisherman.png"), "Villager (Fisherman)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/villager/profession/fletcher.png"), "Villager (Fletcher)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/villager/profession/leatherworker.png"),
            "Villager (Leatherworker)",
            BrowseScreen.EntryType.MOB,
            null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/villager/profession/librarian.png"), "Villager (Librarian)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/villager/profession/mason.png"), "Villager (Mason)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/villager/profession/nitwit.png"), "Villager (Nitwit)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/villager/profession/shepherd.png"), "Villager (Shepherd)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/villager/profession/toolsmith.png"), "Villager (Toolsmith)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/villager/profession/weaponsmith.png"),
            "Villager (Weaponsmith)",
            BrowseScreen.EntryType.MOB,
            null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie_villager/zombie_villager.png"), "Zombie Villager", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie_villager/profession/armorer.png"),
            "Zombie Villager (Armorer)",
            BrowseScreen.EntryType.MOB,
            null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie_villager/profession/butcher.png"),
            "Zombie Villager (Butcher)",
            BrowseScreen.EntryType.MOB,
            null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie_villager/profession/cartographer.png"),
            "Zombie Villager (Cartographer)",
            BrowseScreen.EntryType.MOB,
            null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie_villager/profession/cleric.png"),
            "Zombie Villager (Cleric)",
            BrowseScreen.EntryType.MOB,
            null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie_villager/profession/farmer.png"),
            "Zombie Villager (Farmer)",
            BrowseScreen.EntryType.MOB,
            null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie_villager/profession/fisherman.png"),
            "Zombie Villager (Fisherman)",
            BrowseScreen.EntryType.MOB,
            null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie_villager/profession/fletcher.png"),
            "Zombie Villager (Fletcher)",
            BrowseScreen.EntryType.MOB,
            null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie_villager/profession/leatherworker.png"),
            "Zombie Villager (Leatherworker)",
            BrowseScreen.EntryType.MOB,
            null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie_villager/profession/librarian.png"),
            "Zombie Villager (Librarian)",
            BrowseScreen.EntryType.MOB,
            null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie_villager/profession/mason.png"),
            "Zombie Villager (Mason)",
            BrowseScreen.EntryType.MOB,
            null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie_villager/profession/nitwit.png"),
            "Zombie Villager (Nitwit)",
            BrowseScreen.EntryType.MOB,
            null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie_villager/profession/shepherd.png"),
            "Zombie Villager (Shepherd)",
            BrowseScreen.EntryType.MOB,
            null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie_villager/profession/toolsmith.png"),
            "Zombie Villager (Toolsmith)",
            BrowseScreen.EntryType.MOB,
            null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/zombie_villager/profession/weaponsmith.png"),
            "Zombie Villager (Weaponsmith)",
            BrowseScreen.EntryType.MOB,
            null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/shulker/shulker.png"), "Shulker", BrowseScreen.EntryType.MOB, null)
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/shulker/shulker_white.png"), "Shulker (White)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/shulker/shulker_orange.png"), "Shulker (Orange)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/shulker/shulker_magenta.png"), "Shulker (Magenta)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/shulker/shulker_light_blue.png"), "Shulker (Light Blue)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/shulker/shulker_yellow.png"), "Shulker (Yellow)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/shulker/shulker_lime.png"), "Shulker (Lime)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/shulker/shulker_pink.png"), "Shulker (Pink)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/shulker/shulker_gray.png"), "Shulker (Gray)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/shulker/shulker_light_gray.png"), "Shulker (Light Gray)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/shulker/shulker_cyan.png"), "Shulker (Cyan)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/shulker/shulker_purple.png"), "Shulker (Purple)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/shulker/shulker_blue.png"), "Shulker (Blue)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/shulker/shulker_brown.png"), "Shulker (Brown)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/shulker/shulker_green.png"), "Shulker (Green)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/shulker/shulker_red.png"), "Shulker (Red)", BrowseScreen.EntryType.MOB, null
         )
      );
      entries.add(
         new BrowseScreen.BrowseEntry(
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/shulker/shulker_black.png"), "Shulker (Black)", BrowseScreen.EntryType.MOB, null
         )
      );
      return entries;
   }

   private void addGUIEntry(List<BrowseScreen.BrowseEntry> entries, String texturePath, String displayName) {
      ResourceLocation id = ResourceLocation.fromNamespaceAndPath("minecraft", texturePath);
      entries.add(new BrowseScreen.BrowseEntry(id, displayName, BrowseScreen.EntryType.GUI, null));
   }

   public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
   }

   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      context.fill(0, 0, this.width, this.height, -15066578);
      context.fill(0, 0, this.width, 28, -15326914);
      if (this.tabAll != null) {
         Button activeTab = switch (this.currentTab) {
            case ALL -> this.tabAll;
            case BLOCKS -> this.tabBlocks;
            case ITEMS -> this.tabItems;
            case MOBS -> this.tabMobs;
            case GUI -> this.tabGui;
            case ENTITY -> this.tabEntity;
            case SKY -> this.tabSky;
         };
         if (activeTab != null) {
            context.fill(
               activeTab.getX(),
               activeTab.getY() + 20,
               activeTab.getX() + activeTab.getWidth(),
               activeTab.getY() + 22,
               -256
            );
         }
      }

      super.render(context, mouseX, mouseY, delta);
      String entriesText = this.filteredEntries.size() + " entries";
      context.drawString(this.font, entriesText, this.width - this.font.width(entriesText) - 5, 36, 10066329, false);
      this.renderGrid(context, mouseX, mouseY);
   }

   private void renderGrid(GuiGraphics context, int mouseX, int mouseY) {
      int startIdx = this.scrollOffset * this.columns;
      int gridX = 10;
      int gridY = 60;
      String tooltipText = null;

      for (int i = startIdx; i < this.filteredEntries.size(); i++) {
         int localIdx = i - startIdx;
         int col = localIdx % this.columns;
         int row = localIdx / this.columns;
         if (row >= this.visibleRows + 1) {
            break;
         }

         int x = gridX + col * 40;
         int y = gridY + row * 40;
         if (y + 36 > this.height) {
            break;
         }

         BrowseScreen.BrowseEntry entry = (BrowseScreen.BrowseEntry)this.filteredEntries.get(i);
         boolean hovered = mouseX >= x && mouseX < x + 36 && mouseY >= y && mouseY < y + 36;
         int bgColor = hovered ? -12961186 : -14013874;
         context.fill(x, y, x + 36, y + 36, bgColor);
         boolean modified = this.isModified(entry);
         if (modified) {
            this.drawRectOutline(context, x, y, x + 36, y + 36, -16711936);
         } else {
            this.drawRectOutline(context, x, y, x + 36, y + 36, -13421739);
         }

         if (entry.stack != null && !entry.stack.isEmpty()) {
            int iconX = x + 10;
            int iconY = y + 10;
            context.renderItem(entry.stack, iconX, iconY);
         } else {
            String label = entry.name.length() > 5 ? entry.name.substring(0, 5) : entry.name;
            int lx = x + (36 - this.font.width(label)) / 2;
            int ly = y + 14;
            context.drawString(this.font, label, lx, ly, -3355444, false);
         }

         if (hovered) {
            tooltipText = entry.name + " (" + entry.type.displayName + ")";
            if (modified) {
               tooltipText = tooltipText + " §a[Modified] §7- Right-click to reset";
            }
         }
      }

      if (tooltipText != null) {
         int tw = this.font.width(tooltipText) + 8;
         int tx = Math.min(mouseX + 10, this.width - tw - 5);
         int ty = mouseY - 18;
         if (ty < 0) {
            ty = mouseY + 15;
         }

         context.pose().pushPose();
         context.pose().translate(0.0F, 0.0F, 300.0F);
         context.fill(tx - 2, ty - 2, tx + tw, ty + 12, -299752892);
         this.drawRectOutline(context, tx - 2, ty - 2, tx + tw, ty + 12, -5592406);
         context.drawString(this.font, tooltipText, tx + 2, ty, 16777215, true);
         context.pose().popPose();
      }

      if (this.maxScroll > 0) {
         int sbX = this.width - 8;
         int sbY = 60;
         int sbH = this.height - 60 - 10;
         context.fill(sbX, sbY, sbX + 6, sbY + sbH, -13421739);
         int thumbH = Math.max(20, sbH * this.visibleRows / ((this.filteredEntries.size() + this.columns - 1) / this.columns));
         int thumbY = sbY + (int)((float)this.scrollOffset / this.maxScroll * (sbH - thumbH));
         context.fill(sbX, thumbY, sbX + 6, thumbY + thumbH, -7829300);
      }
   }

   private boolean isModified(BrowseScreen.BrowseEntry entry) {
      if (!TextureManager.getInstance().hasModifiedTextures()) {
         return false;
      }

      if (entry.type == BrowseScreen.EntryType.BLOCK) {
         Block block = (Block)BuiltInRegistries.BLOCK.get(entry.id);

         for (Direction dir : Direction.values()) {
            TextureExtractor.BlockFaceTexture tex = TextureExtractor.extract(block.defaultBlockState(), dir);
            if (tex != null && TextureManager.getInstance().getPixels(tex.textureId()) != null) {
               return true;
            }
         }
      } else if (entry.type == BrowseScreen.EntryType.ITEM && entry.stack != null) {
         ItemTextureExtractor.ItemTexture tex = ItemTextureExtractor.extract(entry.stack);
         if (tex != null && TextureManager.getInstance().getPixels(tex.textureId()) != null) {
            return true;
         }
      } else if (entry.type == BrowseScreen.EntryType.MOB || entry.type == BrowseScreen.EntryType.GUI || entry.type == BrowseScreen.EntryType.ENTITY) {
         ResourceLocation fullId = this.asFullTextureId(entry.id);
         if (TextureManager.getInstance().getPixels(fullId) != null) {
            return true;
         }
      }

      return false;
   }

   private void drawRectOutline(GuiGraphics ctx, int x1, int y1, int x2, int y2, int c) {
      ctx.fill(x1, y1, x2, y1 + 1, c);
      ctx.fill(x1, y2 - 1, x2, y2, c);
      ctx.fill(x1, y1, x1 + 1, y2, c);
      ctx.fill(x2 - 1, y1, x2, y2, c);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (super.mouseClicked(mouseX, mouseY, button)) {
         return true;
      }

      if (button == 0 || button == 1) {
         int startIdx = this.scrollOffset * this.columns;
         int gridX = 10;
         int gridY = 60;

         for (int i = startIdx; i < this.filteredEntries.size(); i++) {
            int localIdx = i - startIdx;
            int col = localIdx % this.columns;
            int row = localIdx / this.columns;
            if (row >= this.visibleRows + 1) {
               break;
            }

            int x = gridX + col * 40;
            int y = gridY + row * 40;
            if (y + 36 > this.height) {
               break;
            }

            if (mouseX >= x && mouseX < x + 36 && mouseY >= y && mouseY < y + 36) {
               if (button == 0) {
                  this.openEditor((BrowseScreen.BrowseEntry)this.filteredEntries.get(i));
               } else {
                  this.resetEntry((BrowseScreen.BrowseEntry)this.filteredEntries.get(i));
               }

               return true;
            }
         }
      }

      return false;
   }

   private void openEditor(BrowseScreen.BrowseEntry entry) {
      ExternalEditorManager extMgr = ExternalEditorManager.getInstance();
      boolean useExternal = extMgr.isExternalEditorEnabled();
      if (entry.type == BrowseScreen.EntryType.BLOCK) {
         Block block = (Block)BuiltInRegistries.BLOCK.get(entry.id);
         if (useExternal) {
            this.openExternalForBlock(block);
         } else {
            this.minecraft.setScreen(new EditorScreen(block, this));
         }
      } else if (entry.type == BrowseScreen.EntryType.MOB) {
         if (entry.id.getPath().startsWith("textures/")) {
            if (useExternal) {
               if (!this.openExternalForDirectTexture(entry.id, entry.name)) {
                  this.minecraft.setScreen(new GuiTextureEditorScreen(entry.id, entry.name, this));
               }
            } else {
               this.minecraft.setScreen(new GuiTextureEditorScreen(entry.id, entry.name, this));
            }
         } else if (entry.stack == null) {
            if (useExternal) {
               if (!this.openExternalForDirectTexture(entry.id, entry.name)) {
                  this.minecraft.setScreen(new GuiTextureEditorScreen(entry.id, entry.name, this));
               }
            } else {
               this.minecraft.setScreen(new GuiTextureEditorScreen(entry.id, entry.name, this));
            }
         } else if (this.minecraft.level != null) {
            Entity entity = EntityMapper.getEntityFromItem(entry.stack, this.minecraft.level);
            if (entity != null) {
               if (useExternal) {
                  this.openExternalForMob(entity);
               } else {
                  this.minecraft.setScreen(new MobEditorScreen(entity, this));
               }
            } else if (useExternal) {
               this.openExternalForItem(entry.stack);
            } else {
               this.minecraft.setScreen(new ItemEditorScreen(entry.stack, this));
            }
         }
      } else if (entry.type == BrowseScreen.EntryType.ITEM) {
         if (entry.stack != null) {
            if (useExternal) {
               this.openExternalForItem(entry.stack);
            } else {
               this.minecraft.setScreen(new ItemEditorScreen(entry.stack, this));
            }
         }
      } else if (entry.type == BrowseScreen.EntryType.GUI) {
         if (useExternal) {
            if (!this.openExternalForDirectTexture(entry.id, entry.name)) {
               this.minecraft.setScreen(new GuiTextureEditorScreen(entry.id, entry.name, this));
            }
         } else {
            this.minecraft.setScreen(new GuiTextureEditorScreen(entry.id, entry.name, this));
         }
      } else if (entry.type == BrowseScreen.EntryType.ENTITY) {
         if (useExternal) {
            if (!this.openExternalForDirectTexture(entry.id, entry.name)) {
               this.minecraft.setScreen(new GuiTextureEditorScreen(entry.id, entry.name, this));
            }
         } else {
            this.minecraft.setScreen(new GuiTextureEditorScreen(entry.id, entry.name, this));
         }
      }
   }

   private void resetEntry(BrowseScreen.BrowseEntry entry) {
      if (this.isModified(entry)) {
         if (entry.type == BrowseScreen.EntryType.BLOCK) {
            Block block = (Block)BuiltInRegistries.BLOCK.get(entry.id);

            for (Direction dir : Direction.values()) {
               TextureExtractor.BlockFaceTexture tex = TextureExtractor.extract(block.defaultBlockState(), dir);
               if (tex != null && TextureManager.getInstance().getPixels(tex.textureId()) != null) {
                  TextureResourceLoader.LoadedTexture defaultTex = TextureResourceLoader.loadTexture(tex.textureId());
                  int[][] resetPixels = defaultTex != null ? defaultTex.pixels() : tex.pixels();
                  int resetW = defaultTex != null ? defaultTex.width() : tex.width();
                  int resetH = defaultTex != null ? defaultTex.height() : tex.height();
                  ResourceLocation spriteId = ResourceLocation.fromNamespaceAndPath(
                     tex.textureId().getNamespace(), tex.textureId().getPath().replace("textures/", "").replace(".png", "")
                  );
                  ExternalEditorManager.resetTextureStatic(tex.textureId(), spriteId, resetPixels, resetW, resetH);
               }
            }
         } else if (entry.type == BrowseScreen.EntryType.ITEM && entry.stack != null) {
            ItemTextureExtractor.ItemTexture tex = ItemTextureExtractor.extract(entry.stack);
            if (tex != null && TextureManager.getInstance().getPixels(tex.textureId()) != null) {
               TextureResourceLoader.LoadedTexture defaultTex = TextureResourceLoader.loadTexture(tex.textureId());
               int[][] resetPixels = defaultTex != null ? defaultTex.pixels() : tex.pixels();
               int resetW = defaultTex != null ? defaultTex.width() : tex.width();
               int resetH = defaultTex != null ? defaultTex.height() : tex.height();
               ExternalEditorManager.resetTextureStatic(tex.textureId(), tex.spriteId(), resetPixels, resetW, resetH);
            }
         } else if (entry.type == BrowseScreen.EntryType.MOB || entry.type == BrowseScreen.EntryType.GUI || entry.type == BrowseScreen.EntryType.ENTITY) {
            ResourceLocation fullId = this.asFullTextureId(entry.id);
            if (TextureManager.getInstance().getPixels(fullId) != null) {
               TextureManager.getInstance().removeTexture(fullId);
               String safeName = fullId.toString().replaceAll("[^a-zA-Z0-9._-]", "_") + ".png";
               File tempFile = new File(ExternalEditorSession.getTempDir(), safeName);
               if (tempFile.exists()) {
                  tempFile.delete();
               }

               if (this.minecraft != null) {
                  this.minecraft.reloadResourcePacks();
               }
            }
         }
      }
   }

   private ResourceLocation asFullTextureId(ResourceLocation id) {
      return id.getPath().startsWith("textures/") ? id : ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/" + id.getPath() + ".png");
   }

   private void openExternalForBlock(Block block) {
      TextureExtractor.BlockFaceTexture tex = TextureExtractor.extract(block.defaultBlockState(), Direction.UP);
      if (tex == null) {
         for (Direction dir : Direction.values()) {
            tex = TextureExtractor.extract(block.defaultBlockState(), dir);
            if (tex != null) {
               break;
            }
         }
      }

      if (tex != null) {
         ResourceLocation spriteId = ResourceLocation.fromNamespaceAndPath(
            tex.textureId().getNamespace(), tex.textureId().getPath().replace("textures/", "").replace(".png", "")
         );
         int[][] origCopy = copyPixels(tex.pixels(), tex.width(), tex.height());
         ExternalEditorManager.getInstance().startAtlasSession(tex.textureId(), spriteId, tex.pixels(), origCopy, tex.width(), tex.height());
      }
   }

   private void openExternalForItem(ItemStack stack) {
      ItemTextureExtractor.ItemTexture tex = ItemTextureExtractor.extract(stack);
      if (tex != null) {
         int[][] origCopy = copyPixels(tex.pixels(), tex.width(), tex.height());
         if (tex.spriteId() != null && !tex.textureId().getPath().startsWith("textures/entity/")) {
            ExternalEditorManager.getInstance().startAtlasSession(tex.textureId(), tex.spriteId(), tex.pixels(), origCopy, tex.width(), tex.height());
         } else {
            ExternalEditorManager.getInstance().startEntitySession(tex.textureId(), tex.pixels(), origCopy, tex.width(), tex.height());
         }
      }
   }

   private void openExternalForMob(Entity entity) {
      MobTextureExtractor.MobTexture tex = MobTextureExtractor.extract(entity);
      if (tex != null) {
         int[][] origCopy = copyPixels(tex.pixels(), tex.width(), tex.height());
         ExternalEditorManager.getInstance().startEntitySession(tex.textureId(), tex.pixels(), origCopy, tex.width(), tex.height());
      }
   }

   private boolean openExternalForDirectTexture(ResourceLocation textureId, String name) {
      boolean isGuiSprite = textureId.getPath().startsWith("gui/sprites/")
         || textureId.getPath().startsWith("gui/container/")
         || textureId.getPath().startsWith("gui/");
      ResourceLocation fullId = textureId.getPath().startsWith("textures/")
         ? textureId
         : ResourceLocation.fromNamespaceAndPath(textureId.getNamespace(), "textures/" + textureId.getPath() + ".png");

      try {
         Optional<Resource> optResource = this.minecraft.getResourceManager().getResource(fullId);
         if (optResource.isPresent()) {
            InputStream stream = ((Resource)optResource.get()).open();
            NativeImage image = NativeImage.read(stream);
            int w = image.getWidth();
            int h = image.getHeight();
            int[][] pixels = new int[w][h];

            for (int x = 0; x < w; x++) {
               for (int y = 0; y < h; y++) {
                  pixels[x][y] = ImageColorCompat.readArgb(image, x, y);
               }
            }

            image.close();
            stream.close();
            int[][] origCopy = copyPixels(pixels, w, h);
            if (isGuiSprite) {
               ExternalEditorManager.getInstance().startGuiSession(fullId, textureId, pixels, origCopy, w, h);
            } else {
               ExternalEditorManager.getInstance().startEntitySession(fullId, pixels, origCopy, w, h);
            }

            return true;
         }
      } catch (Exception e) {
         System.out.println("[TextureEditor] Failed to load texture for external editor: " + fullId + " - " + e.getMessage());
      }

      System.out.println("[TextureEditor] Direct texture not found for " + name + ": " + fullId);
      return false;
   }

   private static int[][] copyPixels(int[][] src, int w, int h) {
      int[][] copy = new int[w][h];

      for (int x = 0; x < w; x++) {
         System.arraycopy(src[x], 0, copy[x], 0, h);
      }

      return copy;
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (verticalAmount > 0.0) {
         this.scrollOffset = Math.max(0, this.scrollOffset - 3);
      } else if (verticalAmount < 0.0) {
         this.scrollOffset = Math.min(this.maxScroll, this.scrollOffset + 3);
      }

      return true;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         this.onClose();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }

   private static class BrowseEntry {
      final ResourceLocation id;
      final String name;
      final BrowseScreen.EntryType type;
      final ItemStack stack;

      BrowseEntry(ResourceLocation id, String name, BrowseScreen.EntryType type, ItemStack stack) {
         this.id = id;
         this.name = name;
         this.type = type;
         this.stack = stack;
      }
   }

   private enum EntryType {
      BLOCK("Block"),
      ITEM("Item"),
      MOB("Mob"),
      GUI("GUI"),
      ENTITY("Entity");

      final String displayName;

      EntryType(String displayName) {
         this.displayName = displayName;
      }
   }

   private enum Tab {
      ALL,
      BLOCKS,
      ITEMS,
      MOBS,
      GUI,
      ENTITY,
      SKY;
   }
}
