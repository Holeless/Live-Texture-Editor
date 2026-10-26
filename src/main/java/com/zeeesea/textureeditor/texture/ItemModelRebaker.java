package com.zeeesea.textureeditor.texture;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.CompletableFuture;

public final class ItemModelRebaker {
   private static final long RELOAD_COOLDOWN_MS = 400L;
   private static long lastReloadMs = 0L;
   private static boolean reloadInProgress = false;
   private static boolean reloadQueued = false;

   private ItemModelRebaker() {
   }

   public static void rebake(ResourceLocation spriteId) {
      if (spriteId != null && spriteId.getPath().startsWith("item/")) {
         Minecraft client = Minecraft.getInstance();
         if (client != null) {
            boolean startReload;
            synchronized (ItemModelRebaker.class) {
               long now = System.currentTimeMillis();
               if (reloadInProgress) {
                  reloadQueued = true;
                  return;
               }

               if (now - lastReloadMs < 400L) {
                  reloadQueued = true;
                  return;
               }

               reloadInProgress = true;
               lastReloadMs = now;
               startReload = true;
            }

            if (startReload) {
               CompletableFuture<Void> future = client.reloadResourcePacks();
               future.whenComplete((ignored, throwable) -> {
                  boolean shouldReloadAgain;
                  synchronized (ItemModelRebaker.class) {
                     reloadInProgress = false;
                     shouldReloadAgain = reloadQueued;
                     reloadQueued = false;
                     lastReloadMs = System.currentTimeMillis();
                  }

                  if (throwable != null) {
                     System.out.println("[TextureEditor] ItemModelRebaker: resource reload failed: " + throwable.getMessage());
                  } else {
                     client.execute(() -> TextureManager.getInstance().reapplyAllModifiedTextures());
                     if (shouldReloadAgain) {
                        rebake(spriteId);
                     }
                  }
               });
            }
         }
      }
   }

   public static boolean shouldSuppressLiveRebake(ResourceLocation spriteId) {
      if (spriteId != null && spriteId.getPath().startsWith("item/")) {
         synchronized (ItemModelRebaker.class) {
            if (reloadInProgress) {
               reloadQueued = true;
               return true;
            } else {
               long now = System.currentTimeMillis();
               if (now - lastReloadMs < 400L) {
                  reloadQueued = true;
                  return true;
               } else {
                  return false;
               }
            }
         }
      } else {
         return false;
      }
   }

   public static void invalidateCache() {
      synchronized (ItemModelRebaker.class) {
         reloadQueued = false;
      }
   }
}
