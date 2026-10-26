package com.zeeesea.textureeditor.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zeeesea.textureeditor.mixin.client.SpriteContentsAccessor;
import com.zeeesea.textureeditor.util.ImageColorCompat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TextureManager {
   private static final TextureManager INSTANCE = new TextureManager();
   private final Map<ResourceLocation, int[][]> modifiedTextures = new HashMap();
   private final Map<ResourceLocation, int[]> textureDimensions = new HashMap();
   private final Map<ResourceLocation, int[][]> originalTextures = new HashMap();
   private boolean previewingOriginals = false;

   private TextureManager() {
   }

   public static TextureManager getInstance() {
      return INSTANCE;
   }

   public void putTexture(ResourceLocation textureId, int[][] pixels, int width, int height) {
      this.modifiedTextures.put(textureId, pixels);
      this.textureDimensions.put(textureId, new int[]{width, height});
   }

   public void storeOriginal(ResourceLocation textureId, int[][] pixels, int width, int height) {
      if (!this.originalTextures.containsKey(textureId)) {
         int[][] copy = new int[width][height];

         for (int x = 0; x < width; x++) {
            System.arraycopy(pixels[x], 0, copy[x], 0, height);
         }

         this.originalTextures.put(textureId, copy);
      }
   }

   public int[][] getOriginalPixels(ResourceLocation textureId) {
      return (int[][])this.originalTextures.get(textureId);
   }

   public boolean isPreviewingOriginals() {
      return this.previewingOriginals;
   }

   public void setPreviewingOriginals(boolean previewing) {
      if (previewing != this.previewingOriginals) {
         this.previewingOriginals = previewing;

         for (ResourceLocation textureId : this.modifiedTextures.keySet()) {
            int[] dims = (int[])this.textureDimensions.get(textureId);
            if (dims != null) {
               int w = dims[0];
               int h = dims[1];
               String path = textureId.getPath();
               if (path.startsWith("textures/") && path.endsWith(".png")) {
                  path = path.substring("textures/".length(), path.length() - ".png".length());
               }

               ResourceLocation spriteId = ResourceLocation.fromNamespaceAndPath(textureId.getNamespace(), path);
               int[][] pixels;
               if (previewing) {
                  pixels = (int[][])this.originalTextures.get(textureId);
                  if (pixels == null) {
                     continue;
                  }
               } else {
                  pixels = (int[][])this.modifiedTextures.get(textureId);
               }

               this.reuploadToAtlas(spriteId, pixels, w, h);
            }
         }
      }
   }

   private void reuploadToAtlas(ResourceLocation spriteId, int[][] pixels, int width, int height) {
      this.reuploadToAtlas(spriteId, pixels, width, height, true);
   }

   private void reuploadToAtlas(ResourceLocation spriteId, int[][] pixels, int width, int height, boolean allowRebake) {
      Minecraft client = Minecraft.getInstance();
      TextureAtlasSprite sprite = (TextureAtlasSprite)client.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(spriteId);
      if (sprite != null) {
         updateSpriteContents(sprite, pixels, width, height);
         int atlasX = sprite.getX();
         int atlasY = sprite.getY();
         RenderSystem.assertOnRenderThread();
         TextureAtlas atlas = (TextureAtlas)client.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
         atlas.bind();
         NativeImage img = new NativeImage(width, height, false);

         try {
            for (int x = 0; x < width; x++) {
               for (int y = 0; y < height; y++) {
                  ImageColorCompat.writeArgb(img, x, y, pixels[x][y]);
               }
            }

            img.upload(0, atlasX, atlasY, false);
         } catch (Throwable var15) {
            try {
               img.close();
            } catch (Throwable var14) {
               var15.addSuppressed(var14);
            }

            throw var15;
         }

         img.close();
         if (allowRebake && spriteId.getPath().startsWith("item/") && !ItemModelRebaker.shouldSuppressLiveRebake(spriteId)) {
            ItemModelRebaker.rebake(spriteId);
         }
      }
   }

   public void reapplyAllModifiedTextures() {
      for (ResourceLocation textureId : this.modifiedTextures.keySet()) {
         int[] dims = (int[])this.textureDimensions.get(textureId);
         int[][] pixels = (int[][])this.modifiedTextures.get(textureId);
         if (dims != null && pixels != null) {
            String path = textureId.getPath();
            if (path.startsWith("textures/") && path.endsWith(".png")) {
               path = path.substring("textures/".length(), path.length() - ".png".length());
            }

            ResourceLocation spriteId = ResourceLocation.fromNamespaceAndPath(textureId.getNamespace(), path);
            this.reuploadToAtlas(spriteId, pixels, dims[0], dims[1], false);
         }
      }
   }

   private static void updateSpriteContents(TextureAtlasSprite sprite, int[][] pixels, int width, int height) {
      SpriteContents contents = sprite.contents();
      NativeImage image = ((SpriteContentsAccessor)contents).getImage();
      if (image != null && image.getWidth() >= width && image.getHeight() >= height) {
         for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
               ImageColorCompat.writeArgb(image, x, y, pixels[x][y]);
            }
         }
      }
   }

   public Set<ResourceLocation> getModifiedTextureIds() {
      return this.modifiedTextures.keySet();
   }

   public int[][] getPixels(ResourceLocation textureId) {
      return (int[][])this.modifiedTextures.get(textureId);
   }

   public int[] getDimensions(ResourceLocation textureId) {
      return (int[])this.textureDimensions.get(textureId);
   }

   public boolean hasModifiedTextures() {
      return !this.modifiedTextures.isEmpty();
   }

   public void removeTexture(ResourceLocation textureId) {
      this.modifiedTextures.remove(textureId);
      this.textureDimensions.remove(textureId);
   }

   public void applyLive(ResourceLocation spriteId, int[][] pixels, int width, int height) {
      this.applyLive(spriteId, pixels, width, height, null);
   }

   public void applyLive(ResourceLocation spriteId, int[][] pixels, int width, int height, int[][] origPixels) {
      this.applyLive(spriteId, pixels, width, height, origPixels, true);
   }

   public void applyLiveTransient(ResourceLocation spriteId, int[][] pixels, int width, int height) {
      this.applyLive(spriteId, pixels, width, height, null, false);
   }

   private void applyLive(ResourceLocation spriteId, int[][] pixels, int width, int height, int[][] origPixels, boolean persist) {
      Minecraft client = Minecraft.getInstance();
      ResourceLocation textureId = ResourceLocation.fromNamespaceAndPath(spriteId.getNamespace(), "textures/" + spriteId.getPath() + ".png");
      if (persist) {
         if (origPixels != null) {
            this.storeOriginal(textureId, origPixels, width, height);
         }

         this.putTexture(textureId, pixels, width, height);
      }

      TextureAtlasSprite sprite = (TextureAtlasSprite)client.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(spriteId);
      if (sprite != null) {
         updateSpriteContents(sprite, pixels, width, height);
         int atlasX = sprite.getX();
         int atlasY = sprite.getY();
         RenderSystem.assertOnRenderThread();
         TextureAtlas atlas = (TextureAtlas)client.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
         atlas.bind();
         NativeImage img = new NativeImage(width, height, false);

         try {
            for (int x = 0; x < width; x++) {
               for (int y = 0; y < height; y++) {
                  ImageColorCompat.writeArgb(img, x, y, pixels[x][y]);
               }
            }

            img.upload(0, atlasX, atlasY, false);
         } catch (Throwable var27) {
            try {
               img.close();
            } catch (Throwable var25) {
               var27.addSuppressed(var25);
            }

            throw var27;
         }

         img.close();
         int var28 = width;
         int var29 = height;
         int[][] var30 = pixels;
         int level = 1;

         while (var28 > 1 && var29 > 1) {
            int newW = var28 / 2;
            int newH = var29 / 2;
            if (newW < 1 || newH < 1) {
               break;
            }

            int[][] mipPixels = new int[newW][newH];

            for (int x = 0; x < newW; x++) {
               for (int y = 0; y < newH; y++) {
                  int sx = x * 2;
                  int sy = y * 2;
                  mipPixels[x][y] = averageColors(
                     var30[sx][sy],
                     sx + 1 < var28 ? var30[sx + 1][sy] : var30[sx][sy],
                     sy + 1 < var29 ? var30[sx][sy + 1] : var30[sx][sy],
                     sx + 1 < var28 && sy + 1 < var29 ? var30[sx + 1][sy + 1] : var30[sx][sy]
                  );
               }
            }

            NativeImage mipImg = new NativeImage(newW, newH, false);

            try {
               for (int x = 0; x < newW; x++) {
                  for (int y = 0; y < newH; y++) {
                     ImageColorCompat.writeArgb(mipImg, x, y, mipPixels[x][y]);
                  }
               }

               mipImg.upload(level, atlasX >> level, atlasY >> level, false);
            } catch (Throwable var26) {
               try {
                  mipImg.close();
               } catch (Throwable var24) {
                  var26.addSuppressed(var24);
               }

               throw var26;
            }

            mipImg.close();
            var30 = mipPixels;
            var28 = newW;
            var29 = newH;
            if (++level > 4) {
               break;
            }
         }
      }
   }

   private static int averageColors(int c1, int c2, int c3, int c4) {
      int a = ((c1 >> 24 & 0xFF) + (c2 >> 24 & 0xFF) + (c3 >> 24 & 0xFF) + (c4 >> 24 & 0xFF)) / 4;
      int r = ((c1 >> 16 & 0xFF) + (c2 >> 16 & 0xFF) + (c3 >> 16 & 0xFF) + (c4 >> 16 & 0xFF)) / 4;
      int g = ((c1 >> 8 & 0xFF) + (c2 >> 8 & 0xFF) + (c3 >> 8 & 0xFF) + (c4 >> 8 & 0xFF)) / 4;
      int b = ((c1 & 0xFF) + (c2 & 0xFF) + (c3 & 0xFF) + (c4 & 0xFF)) / 4;
      return a << 24 | r << 16 | g << 8 | b;
   }

   public void clear() {
      this.modifiedTextures.clear();
      this.textureDimensions.clear();
      this.originalTextures.clear();
      this.previewingOriginals = false;
      ItemModelRebaker.invalidateCache();
   }
}
