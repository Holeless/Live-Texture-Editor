package com.zeeesea.textureeditor.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import com.google.gson.JsonObject;
import com.zeeesea.textureeditor.util.ImageColorCompat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ResourcePackExporter {
   public static File export(String packName, String description, String author, int[][] iconPixels, int iconWidth, int iconHeight) {
      TextureManager manager = TextureManager.getInstance();
      if (!manager.hasModifiedTextures()) {
         return null;
      }

      File resourcePacksDir = new File(Minecraft.getInstance().gameDirectory, "resourcepacks");
      if (!resourcePacksDir.exists()) {
         resourcePacksDir.mkdirs();
      }

      String sanitized = packName.replaceAll("[^a-zA-Z0-9_\\- ]", "").trim();
      if (sanitized.isEmpty()) {
         sanitized = "TextureEditorPack";
      }

      File outputFile = new File(resourcePacksDir, sanitized + ".zip");

      for (int counter = 1; outputFile.exists(); counter++) {
         outputFile = new File(resourcePacksDir, sanitized + "_" + counter + ".zip");
      }

      try {
         FileOutputStream fos = new FileOutputStream(outputFile);

         File var37;
         try {
            ZipOutputStream zos = new ZipOutputStream(fos);

            try {
               JsonObject packMcmeta = new JsonObject();
               JsonObject pack = new JsonObject();
               pack.addProperty("pack_format", 34);
               pack.addProperty("min_format", 1);
               pack.addProperty("max_format", 9999);
               JsonObject supportedFormats = new JsonObject();
               supportedFormats.addProperty("min_inclusive", 1);
               supportedFormats.addProperty("max_inclusive", 9999);
               pack.add("supported_formats", supportedFormats);
               String desc = description != null && !description.isEmpty() ? description : packName;
               if (author != null && !author.isEmpty()) {
                  desc = desc + "\nBy " + author;
               }

               pack.addProperty("description", desc);
               packMcmeta.add("pack", pack);
               zos.putNextEntry(new ZipEntry("pack.mcmeta"));
               zos.write(packMcmeta.toString().getBytes(StandardCharsets.UTF_8));
               zos.closeEntry();
               if (iconPixels != null) {
                  NativeImage iconImg = new NativeImage(iconWidth, iconHeight, false);

                  try {
                     for (int x = 0; x < iconWidth; x++) {
                        for (int y = 0; y < iconHeight; y++) {
                           ImageColorCompat.writeArgb(iconImg, x, y, iconPixels[x][y]);
                        }
                     }

                     byte[] pngBytes = nativeImageToBytes(iconImg);
                     zos.putNextEntry(new ZipEntry("pack.png"));
                     zos.write(pngBytes);
                     zos.closeEntry();
                  } catch (Throwable var32) {
                     try {
                        iconImg.close();
                     } catch (Throwable var30) {
                        var32.addSuppressed(var30);
                     }

                     throw var32;
                  }

                  iconImg.close();
               }

               for (ResourceLocation textureId : manager.getModifiedTextureIds()) {
                  int[][] pixels = manager.getPixels(textureId);
                  int[] dims = manager.getDimensions(textureId);
                  if (pixels != null && dims != null) {
                     int w = dims[0];
                     int h = dims[1];
                     String path = "assets/" + textureId.getNamespace() + "/" + textureId.getPath();
                     NativeImage img = new NativeImage(w, h, false);

                     try {
                        for (int x = 0; x < w; x++) {
                           for (int y = 0; y < h; y++) {
                              ImageColorCompat.writeArgb(img, x, y, pixels[x][y]);
                           }
                        }

                        byte[] pngBytes = nativeImageToBytes(img);
                        zos.putNextEntry(new ZipEntry(path));
                        zos.write(pngBytes);
                        zos.closeEntry();
                     } catch (Throwable var31) {
                        try {
                           img.close();
                        } catch (Throwable var29) {
                           var31.addSuppressed(var29);
                        }

                        throw var31;
                     }

                     img.close();
                  }
               }

               zos.flush();
               var37 = outputFile;
            } catch (Throwable var33) {
               try {
                  zos.close();
               } catch (Throwable var28) {
                  var33.addSuppressed(var28);
               }

               throw var33;
            }

            zos.close();
         } catch (Throwable var34) {
            try {
               fos.close();
            } catch (Throwable var27) {
               var34.addSuppressed(var27);
            }

            throw var34;
         }

         fos.close();
         return var37;
      } catch (IOException e) {
         e.printStackTrace();
         return null;
      }
   }

   public static File export(String packName, int[][] iconPixels, int iconWidth, int iconHeight) {
      return export(packName, packName, "", iconPixels, iconWidth, iconHeight);
   }

   private static byte[] nativeImageToBytes(NativeImage image) throws IOException {
      Path tempFile = Files.createTempFile("textureeditor_", ".png", new FileAttribute[0]);

      try {
         image.writeToFile(tempFile);
         return Files.readAllBytes(tempFile);
      } finally {
         Files.deleteIfExists(tempFile);
      }
   }
}
