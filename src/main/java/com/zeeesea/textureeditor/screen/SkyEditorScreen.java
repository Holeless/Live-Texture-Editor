package com.zeeesea.textureeditor.screen;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import com.zeeesea.textureeditor.editor.PixelCanvas;
import com.zeeesea.textureeditor.texture.TextureManager;
import com.zeeesea.textureeditor.util.ImageColorCompat;
import java.io.InputStream;
import java.util.Optional;

public class SkyEditorScreen extends AbstractEditorScreen {
   private final Screen parent;
   private SkyEditorScreen.SkyTexture currentSkyTexture = SkyEditorScreen.SkyTexture.SUN;

   public SkyEditorScreen(Screen parent) {
      super(Component.literal("Sky Editor"));
      this.parent = parent;
      this.zoom = 4;
   }

   @Override
   protected int getBackgroundColor() {
      return -16119266;
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
   protected Screen getBackScreen() {
      return this.parent;
   }

   @Override
   protected String getEditorTitle() {
      return "Sky Editor - " + this.currentSkyTexture.displayName;
   }

   @Override
   protected String getResetCurrentLabel() {
      return "Reset";
   }

   @Override
   protected void loadTexture() {
      this.textureId = this.currentSkyTexture.getTextureId();
      Minecraft client = Minecraft.getInstance();
      int[][] savedPixels = TextureManager.getInstance().getPixels(this.textureId);
      int[] savedDims = TextureManager.getInstance().getDimensions(this.textureId);

      try {
         Optional<Resource> optResource = client.getResourceManager().getResource(this.textureId);
         if (optResource.isPresent()) {
            InputStream stream = ((Resource)optResource.get()).open();
            NativeImage image = NativeImage.read(stream);
            int w = image.getWidth();
            int h = image.getHeight();
            this.originalPixels = new int[w][h];

            for (int x = 0; x < w; x++) {
               for (int y = 0; y < h; y++) {
                  this.originalPixels[x][y] = ImageColorCompat.readArgb(image, x, y);
               }
            }

            image.close();
            stream.close();
            if (savedPixels != null && savedDims != null && savedDims[0] == w && savedDims[1] == h) {
               this.canvas = new PixelCanvas(savedDims[0], savedDims[1], savedPixels);
            } else {
               this.canvas = new PixelCanvas(w, h, this.originalPixels);
            }
         }
      } catch (Exception e) {
         System.out.println("[TextureEditor] Failed to load sky texture: " + this.textureId + " - " + e.getMessage());
      }

      if (this.canvas == null) {
         this.canvas = new PixelCanvas(this.currentSkyTexture.defaultWidth, this.currentSkyTexture.defaultHeight);
         this.originalPixels = new int[this.currentSkyTexture.defaultWidth][this.currentSkyTexture.defaultHeight];
      }
   }

   @Override
   protected int addExtraButtons(int toolY) {
      int skyBtnX = this.width / 2 - 120;
      this.addRenderableWidget(
         Button.builder(Component.literal("Sun"), btn -> this.switchTexture(SkyEditorScreen.SkyTexture.SUN))
            .pos(skyBtnX, 5)
            .size(70, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Moon"), btn -> this.switchTexture(SkyEditorScreen.SkyTexture.MOON))
            .pos(skyBtnX + 74, 5)
            .size(70, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("End Sky"), btn -> this.switchTexture(SkyEditorScreen.SkyTexture.END_SKY))
            .pos(skyBtnX + 148, 5)
            .size(70, 20)
            .build()
      );
      return toolY;
   }

   @Override
   protected void renderExtra(GuiGraphics context, int mouseX, int mouseY) {
      int skyBtnX = this.width / 2 - 120;
      int tabIdx = this.currentSkyTexture.ordinal();
      int indicatorX = skyBtnX + tabIdx * 74;
      context.fill(indicatorX, 25, indicatorX + 70, 27, -256);
   }

   private void switchTexture(SkyEditorScreen.SkyTexture tex) {
      this.applyLive();
      this.currentSkyTexture = tex;
      this.panOffsetX = 0;
      this.panOffsetY = 0;
      this.canvas = null;
      this.clearWidgets();
      this.init();
   }

   @Override
   protected void applyLive() {
      if (this.textureId != null && this.canvas != null) {
         Minecraft client = Minecraft.getInstance();
         TextureManager.getInstance().putTexture(this.textureId, this.canvas.getPixels(), this.canvas.getWidth(), this.canvas.getHeight());
         client.execute(() -> {
            NativeImage img = new NativeImage(this.canvas.getWidth(), this.canvas.getHeight(), false);

            for (int x = 0; x < this.canvas.getWidth(); x++) {
               for (int y = 0; y < this.canvas.getHeight(); y++) {
                  ImageColorCompat.writeArgb(img, x, y, this.canvas.getPixels()[x][y]);
               }
            }

            DynamicTexture dynamicTex = new DynamicTexture(img);
            client.getTextureManager().register(this.textureId, dynamicTex);
         });
      }
   }

   @Override
   protected void resetCurrent() {
      if (this.originalPixels != null) {
         this.canvas.saveSnapshot();

         for (int x = 0; x < this.canvas.getWidth(); x++) {
            for (int y = 0; y < this.canvas.getHeight(); y++) {
               this.canvas.setPixel(x, y, this.originalPixels[x][y]);
            }
         }

         TextureManager.getInstance().removeTexture(this.textureId);
         this.applyLive();
      }
   }

   private enum SkyTexture {
      SUN("Sun", "environment/sun", 32, 32),
      MOON("Moon Phases", "environment/moon_phases", 128, 64),
      END_SKY("End Sky", "environment/end_sky", 128, 128);

      final String displayName;
      final String path;
      final int defaultWidth;
      final int defaultHeight;

      SkyTexture(String dn, String p, int dw, int dh) {
         this.displayName = dn;
         this.path = p;
         this.defaultWidth = dw;
         this.defaultHeight = dh;
      }

      ResourceLocation getTextureId() {
         return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/" + this.path + ".png");
      }
   }
}
