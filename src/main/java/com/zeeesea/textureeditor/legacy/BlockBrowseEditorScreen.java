package com.zeeesea.textureeditor.legacy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.zeeesea.textureeditor.editor.PixelCanvas;
import com.zeeesea.textureeditor.screen.AbstractEditorScreen;
import com.zeeesea.textureeditor.texture.TextureExtractor;
import com.zeeesea.textureeditor.texture.TextureManager;

public class BlockBrowseEditorScreen extends AbstractEditorScreen {
   private final Block block;
   private final BlockState blockState;
   private final Screen parent;
   private Direction currentFace = Direction.UP;

   public BlockBrowseEditorScreen(Block block, Screen parent) {
      super(Component.literal("Block Texture Editor"));
      this.block = block;
      this.blockState = block.defaultBlockState();
      this.parent = parent;
      Minecraft client = Minecraft.getInstance();
      if (client.level != null && client.player != null) {
         int color = client.getBlockColors().getColor(this.blockState, client.level, client.player.blockPosition(), 0);
         if (color != -1) {
            this.blockTint = color | 0xFF000000;
            this.isTinted = true;
         }
      }
   }

   @Override
   protected void loadTexture() {
      TextureExtractor.BlockFaceTexture tex = TextureExtractor.extract(this.blockState, this.currentFace);
      if (tex != null) {
         this.originalPixels = copyPixels(tex.pixels(), tex.width(), tex.height());
         this.textureId = tex.textureId();
         this.spriteId = ResourceLocation.fromNamespaceAndPath(tex.textureId().getNamespace(), tex.textureId().getPath().replace("textures/", "").replace(".png", ""));
         int[][] savedPixels = TextureManager.getInstance().getPixels(this.textureId);
         int[] savedDims = TextureManager.getInstance().getDimensions(this.textureId);
         if (savedPixels != null && savedDims != null && savedDims[0] == tex.width() && savedDims[1] == tex.height()) {
            this.canvas = new PixelCanvas(savedDims[0], savedDims[1], savedPixels);
         } else {
            this.canvas = new PixelCanvas(tex.width(), tex.height(), tex.pixels());
         }
      }
   }

   @Override
   protected String getEditorTitle() {
      String name = this.block.getName().getString();
      String tintLabel = this.isTinted ? " §a[Tinted]" : "";
      return "Block Editor - " + name + " (" + this.currentFace.getName() + ")" + tintLabel;
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
      this.addRenderableWidget(
         Button.builder(Component.literal("Top"), btn -> this.switchFace(Direction.UP))
            .pos(5, toolY)
            .size(48, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Bottom"), btn -> this.switchFace(Direction.DOWN))
            .pos(57, toolY)
            .size(48, 20)
            .build()
      );
      toolY += 24;
      this.addRenderableWidget(
         Button.builder(Component.literal("North"), btn -> this.switchFace(Direction.NORTH))
            .pos(5, toolY)
            .size(48, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("South"), btn -> this.switchFace(Direction.SOUTH))
            .pos(57, toolY)
            .size(48, 20)
            .build()
      );
      toolY += 24;
      this.addRenderableWidget(
         Button.builder(Component.literal("East"), btn -> this.switchFace(Direction.EAST))
            .pos(5, toolY)
            .size(48, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("West"), btn -> this.switchFace(Direction.WEST))
            .pos(57, toolY)
            .size(48, 20)
            .build()
      );
      toolY += 24;
      this.addRenderableWidget(
         Button.builder(Component.literal("Reset Block"), btn -> this.resetBlock())
            .pos(this.width - 115, this.height - 124)
            .size(110, 20)
            .build()
      );
      return toolY;
   }

   private void switchFace(Direction newFace) {
      this.applyLive();
      this.currentFace = newFace;
      this.panOffsetX = 0;
      this.panOffsetY = 0;
      this.canvas = null;
      this.clearWidgets();
      this.init();
   }

   @Override
   protected void applyLive() {
      if (this.spriteId != null && this.canvas != null) {
         Minecraft.getInstance()
            .execute(() -> TextureManager.getInstance().applyLive(this.spriteId, this.canvas.getPixels(), this.canvas.getWidth(), this.canvas.getHeight()));
      }
   }

   @Override
   protected void resetCurrent() {
      if (this.originalPixels != null && this.spriteId != null) {
         this.canvas.saveSnapshot();

         for (int x = 0; x < this.canvas.getWidth(); x++) {
            for (int y = 0; y < this.canvas.getHeight(); y++) {
               this.canvas.setPixel(x, y, this.originalPixels[x][y]);
            }
         }

         this.applyLive();
      }
   }

   private void resetBlock() {
      for (Direction dir : Direction.values()) {
         TextureExtractor.BlockFaceTexture tex = TextureExtractor.extract(this.blockState, dir);
         if (tex != null) {
            ResourceLocation sid = ResourceLocation.fromNamespaceAndPath(
               tex.textureId().getNamespace(), tex.textureId().getPath().replace("textures/", "").replace(".png", "")
            );
            TextureManager.getInstance().removeTexture(tex.textureId());
            Minecraft.getInstance().execute(() -> TextureManager.getInstance().applyLive(sid, tex.pixels(), tex.width(), tex.height()));
         }
      }

      if (this.originalPixels != null) {
         this.canvas.saveSnapshot();

         for (int x = 0; x < this.canvas.getWidth(); x++) {
            for (int y = 0; y < this.canvas.getHeight(); y++) {
               this.canvas.setPixel(x, y, this.originalPixels[x][y]);
            }
         }
      }
   }
}
