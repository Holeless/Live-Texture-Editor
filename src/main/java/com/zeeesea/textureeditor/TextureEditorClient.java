package com.zeeesea.textureeditor;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import com.zeeesea.textureeditor.editor.ExternalEditorManager;
import com.zeeesea.textureeditor.screen.AbstractEditorScreen;
import com.zeeesea.textureeditor.screen.BrowseScreen;
import com.zeeesea.textureeditor.screen.EditorScreen;
import com.zeeesea.textureeditor.screen.ItemEditorScreen;
import com.zeeesea.textureeditor.screen.MobEditorScreen;
import com.zeeesea.textureeditor.texture.ItemModelRebaker;
import com.zeeesea.textureeditor.texture.ItemTextureExtractor;
import com.zeeesea.textureeditor.texture.MobTextureExtractor;
import com.zeeesea.textureeditor.texture.TextureExtractor;
import com.zeeesea.textureeditor.texture.TextureManager;
import com.zeeesea.textureeditor.util.BlockFilter;
import com.zeeesea.textureeditor.util.EntityMapper;

public class TextureEditorClient {
   private static boolean editorModeEnabled = false;
   private static KeyMapping toggleEditorKey;
   private static KeyMapping openEditorKey;
   private static KeyMapping previewOriginalKey;

   private TextureEditorClient() {
   }

   public static boolean isEditorModeEnabled() {
      return editorModeEnabled;
   }

   public static KeyMapping getOpenEditorKey() {
      return openEditorKey;
   }

   public static KeyMapping getPreviewOriginalKey() {
      return previewOriginalKey;
   }

   /** Mod-bus: registers the three key mappings. */
   public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
      toggleEditorKey = new KeyMapping("key.textureeditor.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.textureeditor");
      openEditorKey = new KeyMapping("key.textureeditor.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.textureeditor");
      previewOriginalKey = new KeyMapping("key.textureeditor.preview", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, "category.textureeditor");
      event.register(toggleEditorKey);
      event.register(openEditorKey);
      event.register(previewOriginalKey);
   }

   /** Mod-bus: invalidates the item re-bake cache whenever resources reload. */
   public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
      event.registerReloadListener((ResourceManagerReloadListener) manager -> ItemModelRebaker.invalidateCache());
   }

   /** Game-bus: runs every client tick (end of tick). */
   public static void onClientTick(ClientTickEvent.Post event) {
      Minecraft client = Minecraft.getInstance();
      if (toggleEditorKey == null) {
         return;
      }
      tick(client);
   }

   private static void tick(Minecraft client) {
      {
               while (toggleEditorKey.consumeClick()) {
                  editorModeEnabled = !editorModeEnabled;
                  if (client.player != null) {
                     client.player.displayClientMessage(Component.literal("§6[Texture Editor] §r" + (editorModeEnabled ? "§aEnabled" : "§cDisabled")), true);
                  }

                  if (editorModeEnabled) {
                     while (openEditorKey.consumeClick()) {
                     }
                  }
               }

               if (editorModeEnabled) {
                  if (client.screen instanceof AbstractEditorScreen) {
                     while (openEditorKey.consumeClick()) {
                        client.screen.onClose();
                     }

                     while (previewOriginalKey.consumeClick()) {
                     }

                     return;
                  }

                  boolean previewKeyHeld = InputConstants.isKeyDown(
                     client.getWindow().getWindow(), previewOriginalKey.getKey().getValue()
                  );
                  TextureManager tm = TextureManager.getInstance();
                  if (previewKeyHeld && !tm.isPreviewingOriginals() && tm.hasModifiedTextures()) {
                     client.execute(() -> tm.setPreviewingOriginals(true));
                  } else if (!previewKeyHeld && tm.isPreviewingOriginals()) {
                     client.execute(() -> tm.setPreviewingOriginals(false));
                  }

                  if (client.screen == null) {
                     while (openEditorKey.consumeClick()) {
                        if (client.player != null) {
                           ExternalEditorManager extMgr = ExternalEditorManager.getInstance();
                           boolean useExternal = extMgr.isExternalEditorEnabled();
                           ItemStack heldItem = client.player.getMainHandItem();
                           if (!heldItem.isEmpty() && heldItem.getItem() instanceof SpawnEggItem) {
                              Entity entity = EntityMapper.getEntityFromItem(heldItem, client.level);
                              if (entity != null) {
                                 if (useExternal) {
                                    client.execute(() -> openExternalForMob(entity));
                                 } else {
                                    client.execute(() -> client.setScreen(new MobEditorScreen(entity, null)));
                                 }
                              }
                           } else if (!heldItem.isEmpty()
                              && heldItem.getItem() instanceof BlockItem blockItem
                              && BlockFilter.isEditableBlock(blockItem.getBlock().defaultBlockState())) {
                              if (useExternal) {
                                 client.execute(() -> openExternalForBlock(blockItem.getBlock()));
                              } else {
                                 client.execute(() -> client.setScreen(new EditorScreen(blockItem.getBlock(), null)));
                              }
                           } else if (!heldItem.isEmpty()) {
                              if (useExternal) {
                                 client.execute(() -> openExternalForItem(heldItem));
                              } else {
                                 client.execute(() -> client.setScreen(new ItemEditorScreen(heldItem, null)));
                              }
                           } else if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.ENTITY) {
                              EntityHitResult entityHit = (EntityHitResult)client.hitResult;
                              Entity entity = entityHit.getEntity();
                              if (useExternal) {
                                 client.execute(() -> openExternalForMob(entity));
                              } else {
                                 client.execute(() -> client.setScreen(new MobEditorScreen(entity, null)));
                              }
                           } else {
                              if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK) {
                                 BlockHitResult hitResult = (BlockHitResult)client.hitResult;
                                 BlockState state = client.level != null ? client.level.getBlockState(hitResult.getBlockPos()) : null;
                                 if (state != null && BlockFilter.isEditableBlock(state)) {
                                    if (useExternal) {
                                       client.execute(() -> openExternalForBlockFace(state.getBlock(), hitResult.getDirection()));
                                    } else {
                                       client.execute(() -> client.setScreen(new EditorScreen(hitResult)));
                                    }
                                    continue;
                                 }
                              }

                              client.execute(() -> client.setScreen(new BrowseScreen()));
                           }
                        }
                     }
                  }
               }
      }
   }

   private static void openExternalForBlock(Block block) {
      openExternalForBlockFace(block, Direction.UP);
   }

   private static void openExternalForBlockFace(Block block, Direction face) {
      TextureExtractor.BlockFaceTexture tex = TextureExtractor.extract(block.defaultBlockState(), face);
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
         int[][] origCopy = new int[tex.width()][tex.height()];

         for (int x = 0; x < tex.width(); x++) {
            System.arraycopy(tex.pixels()[x], 0, origCopy[x], 0, tex.height());
         }

         ExternalEditorManager.getInstance().startAtlasSession(tex.textureId(), spriteId, tex.pixels(), origCopy, tex.width(), tex.height());
      }
   }

   private static void openExternalForItem(ItemStack stack) {
      ItemTextureExtractor.ItemTexture tex = ItemTextureExtractor.extract(stack);
      if (tex != null) {
         int[][] origCopy = new int[tex.width()][tex.height()];

         for (int x = 0; x < tex.width(); x++) {
            System.arraycopy(tex.pixels()[x], 0, origCopy[x], 0, tex.height());
         }

         if (tex.spriteId() != null && !tex.textureId().getPath().startsWith("textures/entity/")) {
            ExternalEditorManager.getInstance().startAtlasSession(tex.textureId(), tex.spriteId(), tex.pixels(), origCopy, tex.width(), tex.height());
         } else {
            ExternalEditorManager.getInstance().startEntitySession(tex.textureId(), tex.pixels(), origCopy, tex.width(), tex.height());
         }
      }
   }

   private static void openExternalForMob(Entity entity) {
      MobTextureExtractor.MobTexture tex = MobTextureExtractor.extract(entity);
      if (tex != null) {
         int[][] origCopy = new int[tex.width()][tex.height()];

         for (int x = 0; x < tex.width(); x++) {
            System.arraycopy(tex.pixels()[x], 0, origCopy[x], 0, tex.height());
         }

         ExternalEditorManager.getInstance().startEntitySession(tex.textureId(), tex.pixels(), origCopy, tex.width(), tex.height());
      }
   }
}
