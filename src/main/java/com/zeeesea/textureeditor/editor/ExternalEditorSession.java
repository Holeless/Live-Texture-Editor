package com.zeeesea.textureeditor.editor;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import com.zeeesea.textureeditor.helper.NotificationHelper;
import com.zeeesea.textureeditor.texture.TextureManager;
import com.zeeesea.textureeditor.util.ImageColorCompat;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;

public class ExternalEditorSession {
   private final ResourceLocation textureId;
   private final ResourceLocation spriteId;
   private final ResourceLocation guiSpriteId;
   private final int[][] originalPixels;
   private final int width;
   private final int height;
   private final ExternalEditorSession.TextureType textureType;
   private final Path tempFile;
   private Thread watchThread;
   private volatile boolean active = true;
   private WatchService watchService;

   public ExternalEditorSession(
      ResourceLocation textureId,
      ResourceLocation spriteId,
      int[][] pixels,
      int[][] originalPixels,
      int width,
      int height,
      ExternalEditorSession.TextureType textureType,
      String editorPath,
      boolean launchEditor,
      ResourceLocation guiSpriteId
   ) {
      this.textureId = textureId;
      this.spriteId = spriteId;
      this.guiSpriteId = guiSpriteId;
      this.originalPixels = originalPixels;
      this.width = width;
      this.height = height;
      this.textureType = textureType;
      File tempDir = getTempDir();
      if (!tempDir.exists()) {
         tempDir.mkdirs();
      }

      String safeName = textureId.toString().replaceAll("[^a-zA-Z0-9._-]", "_") + ".png";
      this.tempFile = new File(tempDir, safeName).toPath();
      if (!Files.exists(this.tempFile, new LinkOption[0])) {
         this.exportPixels(pixels);
      }

      if (launchEditor) {
         this.doLaunchEditor(editorPath);
      }

      this.startWatcher();
      System.out.println("[TextureEditor] External editor session started for: " + textureId);
      Minecraft.getInstance()
         .execute(() -> NotificationHelper.addToast(SystemToast.SystemToastId.PERIODIC_NOTIFICATION, "External Editor", "Opened in external editor. Save to apply changes live."));
   }

   public ExternalEditorSession(
      ResourceLocation textureId,
      ResourceLocation spriteId,
      int[][] pixels,
      int[][] originalPixels,
      int width,
      int height,
      ExternalEditorSession.TextureType textureType,
      String editorPath
   ) {
      this(textureId, spriteId, pixels, originalPixels, width, height, textureType, editorPath, true, null);
   }

   private void startWatcher() {
      String safeName = this.tempFile.getFileName().toString();
      this.watchThread = new Thread(this::watchLoop, "TextureEditor-FileWatcher-" + safeName);
      this.watchThread.setDaemon(true);
      this.watchThread.start();
   }

   public static File getTempDir() {
      return new File(Minecraft.getInstance().gameDirectory, "textureeditor_temp");
   }

   private void exportPixels(int[][] pixels) {
      try {
         int w = pixels.length;
         int h = w > 0 ? pixels[0].length : 0;
         BufferedImage image = new BufferedImage(w, h, 2);

         for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
               image.setRGB(x, y, pixels[x][y]);
            }
         }

         ImageIO.write(image, "png", this.tempFile.toFile());
         System.out.println("[TextureEditor] Exported temp texture: " + this.tempFile);
      } catch (IOException e) {
         System.out.println("[TextureEditor] Failed to export temp texture: " + e.getMessage());
      }
   }

   public void forceExportPixels(int[][] pixels) {
      this.exportPixels(pixels);
   }

   private int[][] readPixels() {
      try {
         BufferedImage image = ImageIO.read(this.tempFile.toFile());
         if (image == null) {
            return null;
         }

         int w = image.getWidth();
         int h = image.getHeight();
         int[][] pixels = new int[w][h];

         for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
               pixels[x][y] = image.getRGB(x, y);
            }
         }

         return pixels;
      } catch (IOException e) {
         System.out.println("[TextureEditor] Failed to read temp texture: " + e.getMessage());
         return null;
      }
   }

   private void doLaunchEditor(String editorPath) {
      try {
         ProcessBuilder pb = new ProcessBuilder(new String[]{editorPath, this.tempFile.toString()});
         pb.redirectErrorStream(true);
         pb.start();
         System.out.println("[TextureEditor] Launched external editor: " + editorPath);
      } catch (IOException e) {
         System.out.println("[TextureEditor] Failed to launch editor: " + e.getMessage());
         Minecraft.getInstance().execute(() -> NotificationHelper.addToast(SystemToast.SystemToastId.PACK_LOAD_FAILURE, "Editor Failed", "Could not launch: " + editorPath));
      }
   }

   private void watchLoop() {
      try {
         this.watchService = FileSystems.getDefault().newWatchService();
         this.tempFile.getParent().register(this.watchService, new Kind[]{StandardWatchEventKinds.ENTRY_MODIFY});
         long lastApply = 0L;
         String fileName = this.tempFile.getFileName().toString();

         while (this.active) {
            WatchKey key;
            try {
               key = this.watchService.poll(500L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
               break;
            }

            if (key != null) {
               boolean fileChanged = false;

               for (WatchEvent<?> event : key.pollEvents()) {
                  Path changed = (Path)event.context();
                  if (changed != null && changed.toString().equals(fileName)) {
                     fileChanged = true;
                  }
               }

               key.reset();
               if (fileChanged) {
                  long now = System.currentTimeMillis();
                  if (now - lastApply >= 600L) {
                     Thread.sleep(600L);
                     lastApply = System.currentTimeMillis();
                     int[][] newPixels = this.readPixels();
                     if (newPixels != null) {
                        System.out.println("[TextureEditor] Detected external change, applying live: " + this.textureId);
                        Minecraft.getInstance().execute(() -> {
                           this.applyChanges(newPixels);
                           NotificationHelper.addToast(SystemToast.SystemToastId.PERIODIC_NOTIFICATION, "Texture Updated", "External changes applied live!");
                        });
                     }
                  }
               }
            }
         }
      } catch (Exception e) {
         if (this.active) {
            System.out.println("[TextureEditor] File watcher error: " + e.getMessage());
         }
      }
   }

   private void applyChanges(int[][] newPixels) {
      int newW = newPixels.length;
      int newH = newW > 0 ? newPixels[0].length : 0;
      TextureManager tm = TextureManager.getInstance();
      if (this.originalPixels != null) {
         tm.storeOriginal(this.textureId, this.originalPixels, this.width, this.height);
      }

      if (this.textureType == ExternalEditorSession.TextureType.BLOCK_ATLAS && this.spriteId != null) {
         tm.applyLive(this.spriteId, newPixels, newW, newH, this.originalPixels);
      } else if (this.textureType == ExternalEditorSession.TextureType.GUI_SPRITE) {
         tm.putTexture(this.textureId, newPixels, newW, newH);
         Minecraft client = Minecraft.getInstance();
         NativeImage img = new NativeImage(newW, newH, false);

         try {
            for (int x = 0; x < newW; x++) {
               for (int y = 0; y < newH; y++) {
                  ImageColorCompat.writeArgb(img, x, y, newPixels[x][y]);
               }
            }

            boolean uploaded = false;
            ResourceLocation lookupId = this.guiSpriteId != null ? this.guiSpriteId : this.spriteId;
            if (lookupId != null) {
               uploaded = this.tryUploadToAtlas(client, img, lookupId);
            }

            if (!uploaded && this.textureId != null) {
               String path = this.textureId.getPath();
               if (path.startsWith("textures/") && path.endsWith(".png")) {
                  path = path.substring("textures/".length(), path.length() - ".png".length());
               }

               ResourceLocation derivedId = ResourceLocation.fromNamespaceAndPath(this.textureId.getNamespace(), path);
               uploaded = this.tryUploadToAtlas(client, img, derivedId);
            }

            if (!uploaded) {
               DynamicTexture dynamicTex = new DynamicTexture(img);
               client.getTextureManager().register(this.textureId, dynamicTex);
               dynamicTex.bind();
               img.upload(0, 0, 0, false);
               System.out.println("[TextureEditor] Applied GUI texture as dynamic: " + this.textureId);
            }
         } catch (Throwable var14) {
            try {
               img.close();
            } catch (Throwable var12) {
               var14.addSuppressed(var12);
            }

            throw var14;
         }

         img.close();
      } else {
         tm.putTexture(this.textureId, newPixels, newW, newH);
         Minecraft client = Minecraft.getInstance();
         NativeImage img = new NativeImage(newW, newH, false);

         try {
            for (int x = 0; x < newW; x++) {
               for (int y = 0; y < newH; y++) {
                  ImageColorCompat.writeArgb(img, x, y, newPixels[x][y]);
               }
            }

            AbstractTexture tex = client.getTextureManager().getTexture(this.textureId);
            if (tex != null) {
               tex.bind();
               img.upload(0, 0, 0, false);
            } else {
               DynamicTexture dynamicTex = new DynamicTexture(new NativeImage(newW, newH, false));
               client.getTextureManager().register(this.textureId, dynamicTex);
               dynamicTex.bind();
               img.upload(0, 0, 0, false);
            }
         } catch (Throwable var13) {
            try {
               img.close();
            } catch (Throwable var11) {
               var13.addSuppressed(var11);
            }

            throw var13;
         }

         img.close();
      }
   }

   private boolean tryUploadToAtlas(Minecraft client, NativeImage img, ResourceLocation id) {
      TextureAtlas blockAtlas = client.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
      TextureAtlasSprite sprite = blockAtlas.getSprite(id);
      if (sprite != null && !sprite.contents().name().getPath().equals("missingno") && sprite.contents().name().equals(id)) {
         blockAtlas.bind();
         img.upload(0, sprite.getX(), sprite.getY(), false);
         System.out.println("[TextureEditor] Uploaded GUI sprite to block atlas: " + id);
         return true;
      }

      ResourceLocation guiAtlasId = ResourceLocation.parse("textures/atlas/gui.png");
      if (client.getTextureManager().getTexture(guiAtlasId) instanceof TextureAtlas guiAtlas) {
         sprite = guiAtlas.getSprite(id);
         if (sprite != null && !sprite.contents().name().getPath().equals("missingno") && sprite.contents().name().equals(id)) {
            guiAtlas.bind();
            img.upload(0, sprite.getX(), sprite.getY(), false);
            System.out.println("[TextureEditor] Uploaded GUI sprite to GUI atlas: " + id);
            return true;
         }

         if (id.getPath().startsWith("gui/sprites/")) {
            String shortPath = id.getPath().substring("gui/sprites/".length());
            ResourceLocation shortId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), shortPath);
            sprite = guiAtlas.getSprite(shortId);
            if (sprite != null && !sprite.contents().name().getPath().equals("missingno")) {
               guiAtlas.bind();
               img.upload(0, sprite.getX(), sprite.getY(), false);
               System.out.println("[TextureEditor] Uploaded GUI sprite to GUI atlas (short ID): " + shortId);
               return true;
            }
         }
      }

      return false;
   }

   public void stop() {
      this.active = false;

      try {
         if (this.watchService != null) {
            this.watchService.close();
         }
      } catch (IOException var2) {
      }

      if (this.watchThread != null) {
         this.watchThread.interrupt();
      }

      System.out.println("[TextureEditor] External editor session stopped for: " + this.textureId);
   }

   public void stopAndCleanup() {
      this.stop();

      try {
         Files.deleteIfExists(this.tempFile);
      } catch (IOException var2) {
      }
   }

   public ResourceLocation getTextureId() {
      return this.textureId;
   }

   public ResourceLocation getSpriteId() {
      return this.spriteId;
   }

   public ResourceLocation getGuiSpriteId() {
      return this.guiSpriteId;
   }

   public int[][] getOriginalPixels() {
      return this.originalPixels;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public ExternalEditorSession.TextureType getTextureType() {
      return this.textureType;
   }

   public boolean isActive() {
      return this.active;
   }

   public Path getTempFile() {
      return this.tempFile;
   }

   public enum TextureType {
      BLOCK_ATLAS,
      ENTITY_TEXTURE,
      GUI_SPRITE;
   }
}
