package com.zeeesea.textureeditor.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.zeeesea.textureeditor.editor.LayerStack;
import com.zeeesea.textureeditor.editor.PixelCanvas;
import com.zeeesea.textureeditor.texture.TextureExtractor;
import com.zeeesea.textureeditor.texture.TextureManager;
import com.zeeesea.textureeditor.texture.TextureResourceLoader;
import java.util.ArrayList;
import java.util.List;

public class EditorScreen extends AbstractEditorScreen {
   private final Block block;
   private final BlockState blockState;
   private final BlockPos blockPos;
   private Direction face;
   private final Screen parent;
   private List<TextureExtractor.BlockFaceTexture> faceTextures = new ArrayList();
   private int faceTextureIndex = 0;

   public EditorScreen(BlockHitResult hitResult) {
      super(Component.literal("Texture Editor"));
      this.blockPos = hitResult.getBlockPos();
      this.face = hitResult.getDirection();
      Minecraft client = Minecraft.getInstance();
      this.blockState = client.level != null ? client.level.getBlockState(this.blockPos) : null;
      this.block = this.blockState != null ? this.blockState.getBlock() : null;
      this.parent = null;
      this.setTint();
   }

   public EditorScreen(Block block, Screen parent) {
      super(Component.literal("Block Texture Editor"));
      this.block = block;
      this.blockState = block.defaultBlockState();
      this.face = Direction.UP;
      this.blockPos = null;
      this.parent = parent;
      this.setTint();
   }

   private void setTint() {
      Minecraft client = Minecraft.getInstance();
      if (this.blockState != null && client.level != null) {
         int color = client.getBlockColors()
            .getColor(
               this.blockState,
               client.level,
               this.blockPos != null ? this.blockPos : (client.player != null ? client.player.blockPosition() : null),
               0
            );
         if (color != -1) {
            this.blockTint = color | 0xFF000000;
            this.isTinted = true;
         }
      }
   }

   @Override
   protected void loadTexture() {
      if (this.blockState != null) {
         this.faceTextures = TextureExtractor.extractAll(this.blockState, this.face);
         if (!this.faceTextures.isEmpty()) {
            if (this.faceTextureIndex >= this.faceTextures.size()) {
               this.faceTextureIndex = 0;
            }

            TextureExtractor.BlockFaceTexture tex = (TextureExtractor.BlockFaceTexture)this.faceTextures.get(this.faceTextureIndex);
            TextureResourceLoader.LoadedTexture defaultTex = TextureResourceLoader.loadTexture(tex.textureId());
            int baseW = defaultTex != null ? defaultTex.width() : tex.width();
            int baseH = defaultTex != null ? defaultTex.height() : tex.height();
            int[][] basePixels = defaultTex != null ? defaultTex.pixels() : tex.pixels();
            this.originalPixels = copyPixels(basePixels, baseW, baseH);
            this.textureId = tex.textureId();
            this.spriteId = ResourceLocation.fromNamespaceAndPath(tex.textureId().getNamespace(), tex.textureId().getPath().replace("textures/", "").replace(".png", ""));
            System.out
               .println(
                  "[TextureEditor] loadTexture: face="
                     + this.face
                     + " layer="
                     + (this.faceTextureIndex + 1)
                     + "/"
                     + this.faceTextures.size()
                     + " texture="
                     + this.textureId
               );
            int[][] savedPixels = TextureManager.getInstance().getPixels(this.textureId);
            int[] savedDims = TextureManager.getInstance().getDimensions(this.textureId);
            if (savedPixels != null && savedDims != null && savedDims[0] == baseW && savedDims[1] == baseH) {
               this.canvas = new PixelCanvas(savedDims[0], savedDims[1], savedPixels);
            } else {
               this.canvas = new PixelCanvas(baseW, baseH, basePixels);
            }
         }
      }
   }

   @Override
   protected String getEditorTitle() {
      String name = this.block != null
         ? this.block.getName().getString()
         : (this.blockState != null ? this.blockState.getBlock().getName().getString() : "Unknown");
      String tintLabel = this.isTinted ? " §a[Tinted]" : "";
      String layerLabel = "";
      if (this.faceTextures.size() > 1) {
         String spriteName = this.spriteId != null ? this.spriteId.getPath() : "";
         int lastSlash = spriteName.lastIndexOf(47);
         if (lastSlash >= 0) {
            spriteName = spriteName.substring(lastSlash + 1);
         }

         layerLabel = " §b[" + spriteName + " " + (this.faceTextureIndex + 1) + "/" + this.faceTextures.size() + "]";
      }

      return "Block Editor - " + name + " (" + this.face.getName() + ")" + tintLabel + layerLabel;
   }

   @Override
   protected String getResetCurrentLabel() {
      return "Reset Face";
   }

   @Override
   protected Screen getBackScreen() {
      return this.parent;
   }

   @Override
   protected int addExtraButtons(int toolY) {
      int tbw = this.getToolButtonWidth();
      int tbh = this.getToolButtonHeight();
      if (this.showFaceButton()) {
         this.addRenderableWidget(Button.builder(Component.literal("Face: " + this.face.getName().toUpperCase()), btn -> {
            Direction[] dirs = Direction.values();
            this.face = dirs[(this.face.ordinal() + 1) % dirs.length];
            btn.setMessage(Component.literal("Face: " + this.face.getName().toUpperCase()));
            this.faceTextureIndex = 0;
            this.switchFace(this.face);
         }).pos(5, toolY).size(tbw, tbh).build());
         toolY += tbh + 4;
      }

      if (this.faceTextures.size() > 1) {
         String currentLayerName = this.getCurrentLayerShortName();
         this.addRenderableWidget(
            Button.builder(
                  Component.literal("Tex: " + currentLayerName + " (" + (this.faceTextureIndex + 1) + "/" + this.faceTextures.size() + ")"), btn -> {
                     if (this.textureId != null && this.canvas != null) {
                        TextureManager.getInstance().putTexture(this.textureId, this.canvas.getPixels(), this.canvas.getWidth(), this.canvas.getHeight());
                     }

                     this.applyLive();
                     this.faceTextureIndex = (this.faceTextureIndex + 1) % this.faceTextures.size();
                     this.panOffsetX = 0;
                     this.panOffsetY = 0;
                     this.canvas = null;
                     this.clearWidgets();
                     this.init();
                  }
               )
               .pos(5, toolY)
               .size(tbw, tbh)
               .build()
         );
         toolY += tbh + 4;
      }

      int rsw = this.getRightSidebarWidth();
      int resetBtnW = rsw - 10;
      this.addRenderableWidget(
         Button.builder(Component.literal("Reset Block"), btn -> this.resetBlock())
            .pos(this.width - rsw + 5, this.height - 124)
            .size(resetBtnW, tbh)
            .build()
      );
      return toolY;
   }

   private String getCurrentLayerShortName() {
      if (this.spriteId == null) {
         return "?";
      }

      String path = this.spriteId.getPath();
      int lastSlash = path.lastIndexOf(47);
      return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
   }

   private void switchFace(Direction newFace) {
      if (this.textureId != null && this.canvas != null) {
         TextureManager.getInstance().putTexture(this.textureId, this.canvas.getPixels(), this.canvas.getWidth(), this.canvas.getHeight());
      }

      this.applyLive();
      this.face = newFace;
      this.panOffsetX = 0;
      this.panOffsetY = 0;
      this.canvas = null;
      this.clearWidgets();
      this.init();
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
      if (this.originalPixels != null && this.spriteId != null) {
         this.canvas.saveSnapshot();
         this.canvas.setLayerStack(new LayerStack(this.canvas.getWidth(), this.canvas.getHeight(), this.originalPixels));
         this.canvas.invalidateCache();
         this.applyLive();
      }
   }

   private void resetBlock() {
      if (this.blockState != null) {
         for (Direction dir : Direction.values()) {
            for (TextureExtractor.BlockFaceTexture tex : TextureExtractor.extractAll(this.blockState, dir)) {
               TextureResourceLoader.LoadedTexture defaultTex = TextureResourceLoader.loadTexture(tex.textureId());
               int[][] resetPixels = defaultTex != null ? defaultTex.pixels() : tex.pixels();
               int resetW = defaultTex != null ? defaultTex.width() : tex.width();
               int resetH = defaultTex != null ? defaultTex.height() : tex.height();
               ResourceLocation sid = ResourceLocation.fromNamespaceAndPath(
                  tex.textureId().getNamespace(), tex.textureId().getPath().replace("textures/", "").replace(".png", "")
               );
               TextureManager.getInstance().removeTexture(tex.textureId());
               Minecraft.getInstance().execute(() -> TextureManager.getInstance().applyLive(sid, resetPixels, resetW, resetH));
            }
         }

         this.resetCurrent();
      }
   }
}
