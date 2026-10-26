package com.zeeesea.textureeditor.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import com.zeeesea.textureeditor.mixin.client.SpriteContentsAccessor;
import com.zeeesea.textureeditor.util.ImageColorCompat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TextureExtractor {
   public static List<TextureExtractor.BlockFaceTexture> extractAll(BlockState state, Direction face) {
      Minecraft client = Minecraft.getInstance();
      BakedModel model = client.getBlockRenderer().getBlockModel(state);
      List<TextureExtractor.BlockFaceTexture> results = new ArrayList();
      Set<ResourceLocation> seen = new HashSet();
      List<BakedQuad> quads = model.getQuads(state, face, RandomSource.create());
      List<BakedQuad> nullQuads = model.getQuads(state, null, RandomSource.create());
      List<BakedQuad> allQuads = new ArrayList(quads);
      allQuads.addAll(nullQuads);

      for (BakedQuad quad : allQuads) {
         TextureAtlasSprite sprite = quad.getSprite();
         if (!sprite.contents().name().getPath().equals("missingno")) {
            ResourceLocation spriteId = sprite.contents().name();
            ResourceLocation textureId = ResourceLocation.fromNamespaceAndPath(spriteId.getNamespace(), "textures/" + spriteId.getPath() + ".png");
            if (!seen.contains(textureId)) {
               seen.add(textureId);
               TextureExtractor.BlockFaceTexture tex = extractFromSprite(sprite);
               if (tex != null) {
                  results.add(tex);
                  System.out.println("[TextureEditor] extractAll: found " + textureId + " for face " + face);
               }
            }
         }
      }

      if (results.isEmpty()) {
         TextureAtlasSprite particle = model.getParticleIcon();
         if (particle != null && !particle.contents().name().getPath().equals("missingno")) {
            TextureExtractor.BlockFaceTexture tex = extractFromSprite(particle);
            if (tex != null) {
               results.add(tex);
            }
         }
      }

      return results;
   }

   public static TextureExtractor.BlockFaceTexture extract(BlockState state, Direction face) {
      List<TextureExtractor.BlockFaceTexture> all = extractAll(state, face);
      return all.isEmpty() ? null : (TextureExtractor.BlockFaceTexture)all.get(0);
   }

   private static TextureExtractor.BlockFaceTexture extractFromSprite(TextureAtlasSprite sprite) {
      SpriteContents contents = sprite.contents();
      int w = contents.width();
      int h = contents.height();
      NativeImage image = ((SpriteContentsAccessor)contents).getImage();
      int[][] pixels = new int[w][h];

      for (int x = 0; x < w; x++) {
         for (int y = 0; y < h; y++) {
            pixels[x][y] = ImageColorCompat.readArgb(image, x, y);
         }
      }

      ResourceLocation spriteId = contents.name();
      ResourceLocation textureId = ResourceLocation.fromNamespaceAndPath(spriteId.getNamespace(), "textures/" + spriteId.getPath() + ".png");
      return new TextureExtractor.BlockFaceTexture(textureId, pixels, w, h);
   }

   public record BlockFaceTexture(ResourceLocation textureId, int[][] pixels, int width, int height) {
   }
}
