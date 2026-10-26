package com.zeeesea.textureeditor.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import com.zeeesea.textureeditor.util.ImageColorCompat;
import java.io.InputStream;
import java.util.Optional;

public final class TextureResourceLoader {
   private TextureResourceLoader() {
   }

   public static TextureResourceLoader.LoadedTexture loadTexture(ResourceLocation textureId) {
      Minecraft client = Minecraft.getInstance();
      if (client == null) {
         return null;
      }

      try {
         Optional<Resource> opt = client.getResourceManager().getResource(textureId);
         if (opt.isEmpty()) {
            return null;
         }

         InputStream stream = ((Resource)opt.get()).open();

         TextureResourceLoader.LoadedTexture var15;
         try {
            NativeImage image = NativeImage.read(stream);

            try {
               int width = image.getWidth();
               int height = image.getHeight();
               int[][] pixels = new int[width][height];

               for (int x = 0; x < width; x++) {
                  for (int y = 0; y < height; y++) {
                     pixels[x][y] = ImageColorCompat.readArgb(image, x, y);
                  }
               }

               var15 = new TextureResourceLoader.LoadedTexture(textureId, pixels, width, height);
            } catch (Throwable var12) {
               if (image != null) {
                  try {
                     image.close();
                  } catch (Throwable var11) {
                     var12.addSuppressed(var11);
                  }
               }

               throw var12;
            }

            if (image != null) {
               image.close();
            }
         } catch (Throwable var13) {
            if (stream != null) {
               try {
                  stream.close();
               } catch (Throwable var10) {
                  var13.addSuppressed(var10);
               }
            }

            throw var13;
         }

         if (stream != null) {
            stream.close();
         }

         return var15;
      } catch (Exception e) {
         System.out.println("[TextureEditor] Failed to load default texture " + textureId + ": " + e.getMessage());
         return null;
      }
   }

   public record LoadedTexture(ResourceLocation textureId, int[][] pixels, int width, int height) {
   }
}
