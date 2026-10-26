package com.zeeesea.textureeditor.screen;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import com.zeeesea.textureeditor.editor.LayerStack;
import com.zeeesea.textureeditor.editor.PixelCanvas;
import com.zeeesea.textureeditor.texture.MobTextureExtractor;
import com.zeeesea.textureeditor.texture.TextureManager;
import com.zeeesea.textureeditor.texture.TextureResourceLoader;
import com.zeeesea.textureeditor.util.EntityMapper;
import com.zeeesea.textureeditor.util.ImageColorCompat;

public class MobEditorScreen extends AbstractEditorScreen {
   private final Entity entity;
   private final String entityName;
   private final Screen parent;
   private MobPreviewWidget mobPreview;
   private boolean mobPreviewActive = false;

   public MobEditorScreen(Entity entity, Screen parent) {
      super(Component.literal("Mob Texture Editor"));
      this.entity = entity;
      this.entityName = entity.getType().getDescription().getString();
      this.parent = parent;
      this.zoom = 6;
   }

   @Override
   protected void loadTexture() {
      MobTextureExtractor.MobTexture tex = MobTextureExtractor.extract(this.entity);
      if (tex != null) {
         this.textureId = tex.textureId();
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
            // If another mod (e.g. Steve Everything) has replaced this mob's texture with a dynamic one, edit that
            // one so the mob shows up with its Steve look instead of the vanilla texture.
            int[][] live = readLiveTexture(this.textureId, baseW, baseH);
            this.canvas = new PixelCanvas(baseW, baseH, live != null ? live : basePixels);
         }
      }

      if (this.canvas == null) {
         this.canvas = new PixelCanvas(64, 64);
         this.originalPixels = new int[64][64];
      }
   }

   /** Pixels of a runtime-registered (DynamicTexture) replacement for this texture, or null if it is the stock one. */
   private static int[][] readLiveTexture(net.minecraft.resources.ResourceLocation id, int w, int h) {
      try {
         net.minecraft.client.renderer.texture.AbstractTexture t = Minecraft.getInstance().getTextureManager().getTexture(id);
         if (t instanceof net.minecraft.client.renderer.texture.DynamicTexture dt && dt.getPixels() != null) {
            NativeImage img = dt.getPixels();
            if (img.getWidth() != w || img.getHeight() != h) {
               return null;
            }
            int[][] px = new int[w][h];
            for (int x = 0; x < w; x++) {
               for (int y = 0; y < h; y++) {
                  px[x][y] = ImageColorCompat.readArgb(img, x, y);
               }
            }
            return px;
         }
      } catch (Throwable ignored) {
      }
      return null;
   }

   @Override
   protected String getEditorTitle() {
      return "Mob Editor - " + this.entityName;
   }

   @Override
   protected String getResetCurrentLabel() {
      return "Reset Mob";
   }

   @Override
   protected Screen getBackScreen() {
      return this.parent;
   }

   @Override
   protected int getMaxZoom() {
      return 20;
   }

   @Override
   protected int getMinZoom() {
      return 1;
   }

   @Override
   protected int getZoomStep() {
      return 1;
   }

   @Override
   protected int addExtraButtons(int toolY) {
      this.mobPreview = new MobPreviewWidget(this.entity);
      this.mobPreview.setPosition(115, 30, 140, 160);
      this.addRenderableWidget(Button.builder(Component.literal("§d3D"), btn -> {
         this.mobPreview.toggleVisible();
         this.mobPreviewActive = this.mobPreview.isVisible();
      }).pos(this.width - 195, this.height - 26).size(60, 20).build());
      if (EntityMapper.hasItemMode(this.entity)) {
         int rsw = this.getRightSidebarWidth();
         int resetBtnW = rsw - 10;
         int tbh = this.getToolButtonHeight();
         this.addRenderableWidget(Button.builder(Component.literal("Edit Item"), btn -> {
            ItemStack itemStack = EntityMapper.getItemFromEntity(this.entity);
            if (itemStack != null) {
               Minecraft.getInstance().setScreen(new ItemEditorScreen(itemStack, this.parent));
            }
         }).pos(this.width - rsw + 5, this.height - 148).size(resetBtnW, tbh).build());
      }

      return toolY;
   }

   @Override
   protected void renderExtra(GuiGraphics context, int mouseX, int mouseY) {
      if (this.mobPreview != null) {
         this.mobPreview.render(context, mouseX, mouseY);
      }
   }

   @Override
   protected boolean handleExtraClick(double mx, double my, int btn) {
      return this.mobPreview != null && this.mobPreview.mouseClicked(mx, my, btn);
   }

   @Override
   protected boolean handleExtraRelease(double mx, double my, int btn) {
      return this.mobPreview != null && this.mobPreview.mouseReleased(mx, my, btn);
   }

   @Override
   protected boolean handleExtraDrag(double mx, double my, int btn, double dx, double dy) {
      return this.mobPreview != null && this.mobPreview.mouseDragged(mx, my, btn, dx, dy);
   }

   @Override
   protected boolean handleExtraScroll(double mx, double my, double ha, double va) {
      return this.mobPreview != null && this.mobPreview.mouseScrolled(mx, my, ha, va);
   }

   @Override
   protected void handleCanvasClick(int px, int py, int btn) {
      super.handleCanvasClick(px, py, btn);
      if (this.mobPreviewActive) {
         this.applyLive();
      }
   }

   @Override
   public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
      boolean b = super.mouseDragged(mx, my, btn, dx, dy);
      if (this.mobPreviewActive) {
         this.applyLive();
      }

      return b;
   }

   @Override
   protected void applyLive() {
      if (this.textureId != null && this.canvas != null) {
         Minecraft client = Minecraft.getInstance();
         if (this.originalPixels != null) {
            TextureManager.getInstance().storeOriginal(this.textureId, this.originalPixels, this.canvas.getWidth(), this.canvas.getHeight());
         }

         TextureManager.getInstance().putTexture(this.textureId, this.canvas.getPixels(), this.canvas.getWidth(), this.canvas.getHeight());
         client.execute(() -> {
            NativeImage img = new NativeImage(this.canvas.getWidth(), this.canvas.getHeight(), false);

            try {
               for (int x = 0; x < this.canvas.getWidth(); x++) {
                  for (int y = 0; y < this.canvas.getHeight(); y++) {
                     ImageColorCompat.writeArgb(img, x, y, this.canvas.getPixels()[x][y]);
                  }
               }

               AbstractTexture tex = client.getTextureManager().getTexture(this.textureId);
               if (tex != null) {
                  tex.bind();
                  img.upload(0, 0, 0, false);
               }
            } catch (Throwable var6) {
               try {
                  img.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }

               throw var6;
            }

            img.close();
         });
      }
   }

   @Override
   protected void resetCurrent() {
      if (this.originalPixels != null && this.textureId != null) {
         TextureResourceLoader.LoadedTexture defaultTex = TextureResourceLoader.loadTexture(this.textureId);
         int[][] resetPixels = defaultTex != null ? defaultTex.pixels() : this.originalPixels;
         if (defaultTex != null) {
            this.originalPixels = copyPixels(defaultTex.pixels(), defaultTex.width(), defaultTex.height());
         }

         this.canvas.saveSnapshot();
         this.canvas.setLayerStack(new LayerStack(this.canvas.getWidth(), this.canvas.getHeight(), resetPixels));
         this.canvas.invalidateCache();
         this.applyLive();
      }
   }
}
