package com.zeeesea.textureeditor.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.zeeesea.textureeditor.editor.PixelCanvas;
import com.zeeesea.textureeditor.texture.ItemTextureExtractor;
import com.zeeesea.textureeditor.texture.TextureManager;
import com.zeeesea.textureeditor.texture.TextureResourceLoader;
import com.zeeesea.textureeditor.util.EntityMapper;

public class ItemEditorScreen extends AbstractEditorScreen {
   private final ItemStack itemStack;
   private final String itemName;
   private final Screen parent;

   public ItemEditorScreen(ItemStack itemStack, Screen parent) {
      super(Component.literal("Item Texture Editor"));
      this.itemStack = itemStack;
      this.itemName = itemStack.getHoverName().getString();
      this.parent = parent;
   }

   @Override
   protected void loadTexture() {
      ItemTextureExtractor.ItemTexture tex = ItemTextureExtractor.extract(this.itemStack);
      if (tex != null) {
         this.textureId = tex.textureId();
         this.spriteId = tex.spriteId();
         TextureResourceLoader.LoadedTexture defaultTex = TextureResourceLoader.loadTexture(this.textureId);
         int baseW = defaultTex != null ? defaultTex.width() : tex.width();
         int baseH = defaultTex != null ? defaultTex.height() : tex.height();
         int[][] basePixels = defaultTex != null ? defaultTex.pixels() : tex.pixels();
         this.originalPixels = copyPixels(basePixels, baseW, baseH);
         int[][] savedPixels = TextureManager.getInstance().getPixels(this.textureId);
         int[] savedDims = TextureManager.getInstance().getDimensions(this.textureId);
         if (savedPixels != null && savedDims != null && savedDims[0] == baseW && savedDims[1] == baseH) {
            this.canvas = new PixelCanvas(savedDims[0], savedDims[1], savedPixels);
         } else {
            this.canvas = new PixelCanvas(baseW, baseH, basePixels);
         }
      }
   }

   @Override
   protected String getEditorTitle() {
      return "Item Editor - " + this.itemName;
   }

   @Override
   protected String getResetCurrentLabel() {
      return "Reset Item";
   }

   @Override
   protected Screen getBackScreen() {
      return this.parent;
   }

   @Override
   protected int addExtraButtons(int toolY) {
      int rsw = this.getRightSidebarWidth();
      int resetBtnW = rsw - 10;
      int tbh = this.getToolButtonHeight();
      if (EntityMapper.hasEntityMode(this.itemStack)) {
         this.addRenderableWidget(Button.builder(Component.literal("Edit Mob/Entity"), btn -> {
            Entity entity = EntityMapper.getEntityFromItem(this.itemStack, Minecraft.getInstance().level);
            if (entity != null) {
               Minecraft.getInstance().setScreen(new MobEditorScreen(entity, this.parent));
            }
         }).pos(this.width - rsw + 5, this.height - 124).size(resetBtnW, tbh).build());
      }

      if (this.itemStack.getItem() == Items.ELYTRA) {
         this.addRenderableWidget(Button.builder(Component.literal("Edit Wing Tex"), btn -> {
            ResourceLocation elytraTexId = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/elytra.png");
            Minecraft.getInstance().setScreen(new GuiTextureEditorScreen(elytraTexId, "Elytra Wings", this.parent != null ? this.parent : this));
         }).pos(this.width - rsw + 5, this.height - 148).size(resetBtnW, tbh).build());
      }

      return toolY;
   }

   @Override
   protected void applyLive() {
      if (this.spriteId != null && this.canvas != null) {
         int[][] origCopy = this.originalPixels;
         Minecraft.getInstance()
            .execute(
               () -> TextureManager.getInstance().applyLive(this.spriteId, this.canvas.getPixels(), this.canvas.getWidth(), this.canvas.getHeight(), origCopy)
            );
      }
   }

   @Override
   protected void resetCurrent() {
      if (this.canvas != null && this.textureId != null) {
         TextureResourceLoader.LoadedTexture defaultTex = TextureResourceLoader.loadTexture(this.textureId);
         int[][] resetPixels = defaultTex != null ? defaultTex.pixels() : this.originalPixels;
         if (resetPixels != null) {
            if (defaultTex != null) {
               this.originalPixels = copyPixels(defaultTex.pixels(), defaultTex.width(), defaultTex.height());
            }

            this.canvas.saveSnapshot();
            if (this.canvas.getWidth() == resetPixels.length && this.canvas.getHeight() == resetPixels[0].length) {
               for (int x = 0; x < this.canvas.getWidth(); x++) {
                  for (int y = 0; y < this.canvas.getHeight(); y++) {
                     this.canvas.setPixel(x, y, resetPixels[x][y]);
                  }
               }
            } else {
               this.canvas = new PixelCanvas(resetPixels.length, resetPixels[0].length, resetPixels);
            }

            TextureManager.getInstance().removeTexture(this.textureId);
            if (this.spriteId != null) {
               TextureManager.getInstance().applyLiveTransient(this.spriteId, this.canvas.getPixels(), this.canvas.getWidth(), this.canvas.getHeight());
            }
         }
      }
   }
}
