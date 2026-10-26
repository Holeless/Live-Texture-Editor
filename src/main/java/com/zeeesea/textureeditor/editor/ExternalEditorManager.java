package com.zeeesea.textureeditor.editor;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;

import com.zeeesea.textureeditor.helper.NotificationHelper;
import com.zeeesea.textureeditor.settings.ModSettings;
import com.zeeesea.textureeditor.texture.TextureManager;
import com.zeeesea.textureeditor.util.ImageColorCompat;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class ExternalEditorManager {
   private static final ExternalEditorManager INSTANCE = new ExternalEditorManager();
   private final ConcurrentHashMap<String, ExternalEditorSession> sessions = new ConcurrentHashMap();

   private ExternalEditorManager() {
   }

   public static ExternalEditorManager getInstance() {
      return INSTANCE;
   }

   public boolean isExternalEditorEnabled() {
      ModSettings s = ModSettings.getInstance();
      if (!s.useExternalEditor) {
         return false;
      }

      String path = this.getEditorPath();
      return path != null && !path.isEmpty() ? new File(path).exists() || !path.contains(File.separator) : false;
   }

   public String getEditorPath() {
      ModSettings s = ModSettings.getInstance();
      if (s.externalEditorCustomPath != null && !s.externalEditorCustomPath.isEmpty() && new File(s.externalEditorCustomPath).exists()) {
         return s.externalEditorCustomPath;
      }

      if (s.selectedEditorName != null && !s.selectedEditorName.isEmpty()) {
         for (ExternalEditorDetector.DetectedEditor editor : ExternalEditorDetector.detectEditors()) {
            if (editor.name().equals(s.selectedEditorName)) {
               return editor.executablePath();
            }
         }
      }

      ExternalEditorDetector.DetectedEditor def = ExternalEditorDetector.getDefault();
      return def != null ? def.executablePath() : null;
   }

   public void startAtlasSession(ResourceLocation textureId, ResourceLocation spriteId, int[][] pixels, int[][] originalPixels, int width, int height) {
      String key = textureId.toString();
      String editorPath = this.getEditorPath();
      if (editorPath == null) {
         this.warnNoEditor();
      } else {
         int[][] currentPixels = TextureManager.getInstance().getPixels(textureId);
         if (currentPixels != null) {
            pixels = currentPixels;
         }

         ExternalEditorSession existing = (ExternalEditorSession)this.sessions.get(key);
         if (existing != null && existing.isActive()) {
            this.syncTempFile(existing, pixels);
         } else {
            if (existing != null) {
               existing.stop();
            }

            ExternalEditorSession session = new ExternalEditorSession(
               textureId, spriteId, pixels, originalPixels, width, height, ExternalEditorSession.TextureType.BLOCK_ATLAS, editorPath, true, null
            );
            this.sessions.put(key, session);
         }
      }
   }

   public void startEntitySession(ResourceLocation textureId, int[][] pixels, int[][] originalPixels, int width, int height) {
      String key = textureId.toString();
      String editorPath = this.getEditorPath();
      if (editorPath == null) {
         this.warnNoEditor();
      } else {
         int[][] currentPixels = TextureManager.getInstance().getPixels(textureId);
         if (currentPixels != null) {
            pixels = currentPixels;
         }

         ExternalEditorSession existing = (ExternalEditorSession)this.sessions.get(key);
         if (existing != null && existing.isActive()) {
            this.syncTempFile(existing, pixels);
         } else {
            if (existing != null) {
               existing.stop();
            }

            ExternalEditorSession session = new ExternalEditorSession(
               textureId, null, pixels, originalPixels, width, height, ExternalEditorSession.TextureType.ENTITY_TEXTURE, editorPath, true, null
            );
            this.sessions.put(key, session);
         }
      }
   }

   public void startGuiSession(ResourceLocation textureId, ResourceLocation guiSpriteId, int[][] pixels, int[][] originalPixels, int width, int height) {
      String key = textureId.toString();
      String editorPath = this.getEditorPath();
      if (editorPath == null) {
         this.warnNoEditor();
      } else {
         int[][] currentPixels = TextureManager.getInstance().getPixels(textureId);
         if (currentPixels != null) {
            pixels = currentPixels;
         }

         ExternalEditorSession existing = (ExternalEditorSession)this.sessions.get(key);
         if (existing != null && existing.isActive()) {
            this.syncTempFile(existing, pixels);
         } else {
            if (existing != null) {
               existing.stop();
            }

            ExternalEditorSession session = new ExternalEditorSession(
               textureId, null, pixels, originalPixels, width, height, ExternalEditorSession.TextureType.GUI_SPRITE, editorPath, true, guiSpriteId
            );
            this.sessions.put(key, session);
         }
      }
   }

   private void syncTempFile(ExternalEditorSession session, int[][] pixels) {
      Minecraft.getInstance().execute(() -> NotificationHelper.addToast(SystemToast.SystemToastId.PERIODIC_NOTIFICATION, "Already Open", "Texture is already open in external editor."));
   }

   public void syncAllTempFiles() {
      for (Entry<String, ExternalEditorSession> entry : this.sessions.entrySet()) {
         ExternalEditorSession session = (ExternalEditorSession)entry.getValue();
         ResourceLocation texId = session.getTextureId();
         int[][] currentPixels = TextureManager.getInstance().getPixels(texId);
         if (currentPixels != null) {
            session.forceExportPixels(currentPixels);
         } else {
            int[][] orig = session.getOriginalPixels();
            if (orig != null) {
               session.forceExportPixels(orig);
            }
         }
      }
   }

   public void resetTexture(ResourceLocation textureId) {
      String key = textureId.toString();
      ExternalEditorSession session = (ExternalEditorSession)this.sessions.get(key);
      int[][] origPixels = null;
      if (session != null) {
         origPixels = session.getOriginalPixels();
      }

      TextureManager.getInstance().removeTexture(textureId);
      if (origPixels != null) {
         if (session != null) {
            session.forceExportPixels(origPixels);
         }

         if (session != null && session.getTextureType() == ExternalEditorSession.TextureType.BLOCK_ATLAS && session.getSpriteId() != null) {
            TextureManager.getInstance().applyLive(session.getSpriteId(), origPixels, session.getWidth(), session.getHeight());
         } else if (session != null) {
            this.uploadEntityTexture(textureId, origPixels, session.getWidth(), session.getHeight());
         }
      }

      Minecraft.getInstance().execute(() -> NotificationHelper.addToast(SystemToast.SystemToastId.PERIODIC_NOTIFICATION, "Texture Reset", "Texture reset to default."));
   }

   public static void resetTextureStatic(ResourceLocation textureId, ResourceLocation spriteId, int[][] originalPixels, int width, int height) {
      TextureManager tm = TextureManager.getInstance();
      tm.removeTexture(textureId);
      String safeName = textureId.toString().replaceAll("[^a-zA-Z0-9._-]", "_") + ".png";
      Path tempFile = new File(ExternalEditorSession.getTempDir(), safeName).toPath();

      try {
         Files.deleteIfExists(tempFile);
      } catch (Exception var9) {
      }

      if (spriteId != null) {
         tm.applyLive(spriteId, originalPixels, width, height);
      } else {
         INSTANCE.uploadEntityTexture(textureId, originalPixels, width, height);
      }

      INSTANCE.sessions.remove(textureId.toString());
      Minecraft.getInstance().execute(() -> NotificationHelper.addToast(SystemToast.SystemToastId.PERIODIC_NOTIFICATION, "Texture Reset", "Texture reset to default."));
   }

   private void uploadEntityTexture(ResourceLocation textureId, int[][] pixels, int w, int h) {
      Minecraft client = Minecraft.getInstance();
      NativeImage img = new NativeImage(w, h, false);

      try {
         for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
               ImageColorCompat.writeArgb(img, x, y, pixels[x][y]);
            }
         }

         AbstractTexture tex = client.getTextureManager().getTexture(textureId);
         if (tex != null) {
            tex.bind();
            img.upload(0, 0, 0, false);
         }
      } catch (Throwable var10) {
         try {
            img.close();
         } catch (Throwable var9) {
            var10.addSuppressed(var9);
         }

         throw var10;
      }

      img.close();
   }

   public void stopSession(ResourceLocation textureId) {
      ExternalEditorSession session = (ExternalEditorSession)this.sessions.remove(textureId.toString());
      if (session != null) {
         session.stop();
      }
   }

   public void stopAll() {
      for (ExternalEditorSession session : this.sessions.values()) {
         session.stopAndCleanup();
      }

      this.sessions.clear();
   }

   public static void deleteCache() {
      File tempDir = ExternalEditorSession.getTempDir();
      if (tempDir.exists() && tempDir.isDirectory()) {
         File[] files = tempDir.listFiles();
         if (files != null) {
            for (File f : files) {
               f.delete();
            }
         }
      }

      INSTANCE.stopAll();
      System.out.println("[TextureEditor] Temp cache cleared.");
   }

   public boolean hasSession(ResourceLocation textureId) {
      ExternalEditorSession session = (ExternalEditorSession)this.sessions.get(textureId.toString());
      return session != null && session.isActive();
   }

   public int getActiveSessionCount() {
      return (int)this.sessions.values().stream().filter(ExternalEditorSession::isActive).count();
   }

   public Set<String> getActiveSessionKeys() {
      return this.sessions.keySet();
   }

   private void warnNoEditor() {
      System.out.println("[TextureEditor] No external editor configured!");
      Minecraft.getInstance()
         .execute(() -> NotificationHelper.addToast(SystemToast.SystemToastId.PACK_LOAD_FAILURE, "No Editor", "Configure an external editor in settings first."));
   }
}
