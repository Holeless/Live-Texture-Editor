package com.zeeesea.textureeditor.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import com.zeeesea.textureeditor.mixin.client.SpriteContentsAccessor;
import com.zeeesea.textureeditor.util.ImageColorCompat;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public class ItemTextureExtractor {
   public static ItemTextureExtractor.ItemTexture extract(ItemStack stack) {
      Minecraft client = Minecraft.getInstance();
      ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
      System.out.println("[TextureEditor] Extracting item texture for: " + itemId);
      ResourceLocation directSpriteId = ResourceLocation.fromNamespaceAndPath(itemId.getNamespace(), "item/" + itemId.getPath());
      TextureAtlasSprite directSprite = (TextureAtlasSprite)client.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(directSpriteId);
      if (directSprite != null && !directSprite.contents().name().getPath().equals("missingno")) {
         System.out.println("[TextureEditor] Found sprite directly in atlas: " + directSprite.contents().name());
         return extractFromSprite(directSprite);
      }

      System.out.println("[TextureEditor] Direct sprite not found, trying BakedModel approach...");
      Block block = Block.byItem(stack.getItem());
      if (block != Blocks.AIR) {
         BakedModel model = client.getBlockRenderer().getBlockModel(block.defaultBlockState());
         if (model != null) {
            List<BakedQuad> quads = model.getQuads(null, null, RandomSource.create());
            if (quads.isEmpty()) {
               for (Direction dir : Direction.values()) {
                  quads = model.getQuads(block.defaultBlockState(), dir, RandomSource.create());
                  if (!quads.isEmpty()) {
                     break;
                  }
               }
            }

            if (!quads.isEmpty()) {
               TextureAtlasSprite sprite = ((BakedQuad)quads.getFirst()).getSprite();
               if (!sprite.contents().name().getPath().equals("missingno")) {
                  System.out.println("[TextureEditor] Found sprite via block model: " + sprite.contents().name());
                  return extractFromSprite(sprite);
               }
            }
         }
      }

      // Vanilla keys item models by "<item id>#inventory" (Fabric API's getModel(ResourceLocation) does not exist here).
      BakedModel model = client.getModelManager().getModel(ModelResourceLocation.inventory(itemId));
      if (model != null) {
         List<BakedQuad> quads = model.getQuads(null, null, RandomSource.create());
         if (quads.isEmpty()) {
            for (Direction dir : Direction.values()) {
               quads = model.getQuads(null, dir, RandomSource.create());
               if (!quads.isEmpty()) {
                  break;
               }
            }
         }

         if (!quads.isEmpty()) {
            TextureAtlasSprite sprite = ((BakedQuad)quads.getFirst()).getSprite();
            if (!sprite.contents().name().getPath().equals("missingno")) {
               System.out.println("[TextureEditor] Found sprite via model manager: " + sprite.contents().name());
               return extractFromSprite(sprite);
            }
         }
      }

      if (block != Blocks.AIR) {
         BakedModel blockModel = client.getBlockRenderer().getBlockModel(block.defaultBlockState());
         if (blockModel != null) {
            TextureAtlasSprite particle = blockModel.getParticleIcon();
            if (particle != null && !particle.contents().name().getPath().equals("missingno")) {
               System.out.println("[TextureEditor] Found sprite via particle texture: " + particle.contents().name());
               return extractFromSprite(particle);
            }
         }
      }

      System.out.println("[TextureEditor] FAILED to find valid sprite for item: " + itemId);
      ResourceLocation entityTexture = getSpecialItemTexturePath(itemId);
      if (entityTexture != null) {
         try {
            Optional<Resource> optResource = client.getResourceManager().getResource(entityTexture);
            if (optResource.isPresent()) {
               InputStream stream = ((Resource)optResource.get()).open();
               NativeImage image = NativeImage.read(stream);
               int w = image.getWidth();
               int h = image.getHeight();
               int[][] pixels = new int[w][h];

               for (int x = 0; x < w; x++) {
                  for (int y = 0; y < h; y++) {
                     pixels[x][y] = ImageColorCompat.readArgb(image, x, y);
                  }
               }

               image.close();
               stream.close();
               ResourceLocation spriteId = ResourceLocation.fromNamespaceAndPath(
                  entityTexture.getNamespace(), entityTexture.getPath().replace("textures/", "").replace(".png", "")
               );
               System.out.println("[TextureEditor] Found special item texture: " + entityTexture);
               return new ItemTextureExtractor.ItemTexture(entityTexture, spriteId, pixels, w, h);
            }
         } catch (Exception e) {
            System.out.println("[TextureEditor] Failed to load special texture: " + entityTexture + " - " + e.getMessage());
         }
      }

      return null;
   }

   private static ResourceLocation getSpecialItemTexturePath(ResourceLocation itemId) {
      String path = itemId.getPath();
      String ns = itemId.getNamespace();

      return switch (path) {
         case "elytra" -> ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/elytra.png");
         case "shield" -> ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/shield/shield_base.png");
         case "trident" -> ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/trident.png");
         case "decorated_pot" -> ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/decorated_pot/decorated_pot_base.png");
         case "conduit" -> ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/conduit/base.png");
         case "bell" -> ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/bell/bell_body.png");
         default -> {
            if (path.endsWith("_sign") || path.endsWith("_hanging_sign")) {
               String wood = path.replace("_hanging_sign", "").replace("_wall_sign", "").replace("_sign", "");
               yield path.contains("hanging")
                  ? ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/signs/hanging/" + wood + ".png")
                  : ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/signs/" + wood + ".png");
            } else if (path.endsWith("_banner")) {
               yield ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/banner_base.png");
            } else if (path.endsWith("_bed")) {
               String color = path.replace("_bed", "");
               yield ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/bed/" + color + ".png");
            } else if (path.equals("chest") || path.equals("trapped_chest")) {
               yield ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/chest/normal.png");
            } else if (path.equals("ender_chest")) {
               yield ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/chest/ender.png");
            } else if (path.contains("boat")) {
               String wood = path.replace("_chest_boat", "").replace("_boat", "");
               yield path.contains("chest")
                  ? ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/chest_boat/" + wood + ".png")
                  : ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/boat/" + wood + ".png");
            } else if (path.contains("shulker_box")) {
               if (path.equals("shulker_box")) {
                  yield ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/shulker/shulker.png");
               } else {
                  String color = path.replace("_shulker_box", "");
                  yield ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/shulker/shulker_" + color + ".png");
               }
            } else {
               yield null;
            }
         }
      };
   }

   private static ItemTextureExtractor.ItemTexture extractFromSprite(TextureAtlasSprite sprite) {
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
      System.out.println("[TextureEditor] Extracted: textureId=" + textureId + " spriteId=" + spriteId + " size=" + w + "x" + h);
      return new ItemTextureExtractor.ItemTexture(textureId, spriteId, pixels, w, h);
   }

   public record ItemTexture(ResourceLocation textureId, ResourceLocation spriteId, int[][] pixels, int width, int height) {
   }
}
