package com.zeeesea.textureeditor.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import com.zeeesea.textureeditor.util.ImageColorCompat;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MobTextureExtractor {
   public static MobTextureExtractor.MobTexture extract(Entity entity) {
      Minecraft client = Minecraft.getInstance();
      EntityType<?> type = entity.getType();
      ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
      String name = type.getDescription().getString();
      System.out.println("[TextureEditor] Extracting mob texture for: " + entityId);
      ResourceLocation variantTexture = tryGetVariantTexture(entity, entityId);
      if (variantTexture != null) {
         System.out.println("[TextureEditor] Variant texture detected: " + variantTexture);
         Optional<Resource> optResource = client.getResourceManager().getResource(variantTexture);
         if (optResource.isPresent()) {
            try {
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
               System.out.println("[TextureEditor] Found mob texture: " + variantTexture + " size=" + w + "x" + h);
               return new MobTextureExtractor.MobTexture(variantTexture, pixels, w, h, name);
            } catch (IOException e) {
               System.out.println("[TextureEditor] Failed to read variant texture: " + variantTexture + " - " + e.getMessage());
            }
         }
      }

      for (ResourceLocation textureId : buildTexturePaths(entityId)) {
         System.out.println("[TextureEditor] Trying texture path: " + textureId);
         Optional<Resource> optResource = client.getResourceManager().getResource(textureId);
         if (optResource.isPresent()) {
            try {
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
               System.out.println("[TextureEditor] Found mob texture: " + textureId + " size=" + w + "x" + h);
               return new MobTextureExtractor.MobTexture(textureId, pixels, w, h, name);
            } catch (IOException e) {
               System.out.println("[TextureEditor] Failed to read texture: " + textureId + " - " + e.getMessage());
            }
         }
      }

      System.out.println("[TextureEditor] FAILED to find texture for entity: " + entityId);
      return null;
   }

   private static ResourceLocation tryGetVariantTexture(Entity entity, ResourceLocation entityId) {
      try {
         String entityPath = entityId.getPath();
         Method getTextureIdMethod = findMethod(entity.getClass(), "getTextureId");
         if (getTextureIdMethod != null) {
            getTextureIdMethod.setAccessible(true);
            if (getTextureIdMethod.invoke(entity, new Object[0]) instanceof ResourceLocation id) {
               System.out.println("[TextureEditor] Wolf-style getTextureId(): " + id);
               return id;
            }
         }

         if (entityPath.equals("horse")) {
            ResourceLocation horseTex = tryHorseVariant(entity);
            if (horseTex != null) {
               return horseTex;
            }
         }

         if (entityPath.equals("llama") || entityPath.equals("trader_llama")) {
            ResourceLocation llamaTex = tryEnumVariantTexture(entity, "textures/entity/llama/", null);
            if (llamaTex != null) {
               return llamaTex;
            }
         }

         if (entityPath.equals("rabbit")) {
            ResourceLocation rabbitTex = tryRabbitVariant(entity);
            if (rabbitTex != null) {
               return rabbitTex;
            }
         }

         if (entityPath.equals("fox")) {
            ResourceLocation foxTex = tryFoxVariant(entity);
            if (foxTex != null) {
               return foxTex;
            }
         }

         if (entityPath.equals("parrot")) {
            ResourceLocation parrotTex = tryParrotVariant(entity);
            if (parrotTex != null) {
               return parrotTex;
            }
         }

         Method getVariantMethod = findMethod(entity.getClass(), "getVariant");
         if (getVariantMethod != null) {
            getVariantMethod.setAccessible(true);
            Object variantRef = getVariantMethod.invoke(entity, new Object[0]);
            System.out.println("[TextureEditor] getVariant() returned: " + (variantRef != null ? variantRef.getClass().getSimpleName() : "null"));
            if (variantRef != null) {
               Object variantValue = variantRef;
               Method valueMethod = findMethod(variantRef.getClass(), "value");
               if (valueMethod != null) {
                  valueMethod.setAccessible(true);
                  variantValue = valueMethod.invoke(variantRef, new Object[0]);
                  System.out.println("[TextureEditor] Variant value: " + (variantValue != null ? variantValue.getClass().getSimpleName() : "null"));
               }

               if (variantValue != null) {
                  ResourceLocation texId = extractTextureFromVariant(variantValue);
                  if (texId != null) {
                     return texId;
                  }
               }
            }
         }
      } catch (Exception e) {
         System.out.println("[TextureEditor] Variant detection failed: " + e.getMessage());
         e.printStackTrace();
      }

      return null;
   }

   private static ResourceLocation extractTextureFromVariant(Object variant) {
      if (variant == null) {
         return null;
      }

      try {
         Method matMethod = findMethod(variant.getClass(), "modelAndTexture");
         if (matMethod != null) {
            matMethod.setAccessible(true);
            Object mat = matMethod.invoke(variant, new Object[0]);
            if (mat != null) {
               Method assetMethod = findMethod(mat.getClass(), "asset");
               if (assetMethod != null) {
                  assetMethod.setAccessible(true);
                  Object asset = assetMethod.invoke(mat, new Object[0]);
                  if (asset != null) {
                     ResourceLocation texId = callTexturePath(asset);
                     if (texId != null) {
                        System.out.println("[TextureEditor] Found via modelAndTexture().asset().texturePath(): " + texId);
                        return texId;
                     }
                  }
               }

               ResourceLocation texId = callTexturePath(mat);
               if (texId != null) {
                  System.out.println("[TextureEditor] Found via modelAndTexture().texturePath(): " + texId);
                  return texId;
               }
            }
         }

         Method assetInfoMethod = findMethod(variant.getClass(), "assetInfo");
         if (assetInfoMethod != null) {
            assetInfoMethod.setAccessible(true);
            Object assetInfo = assetInfoMethod.invoke(variant, new Object[0]);
            if (assetInfo != null) {
               ResourceLocation texId = callTexturePath(assetInfo);
               if (texId != null) {
                  System.out.println("[TextureEditor] Found via assetInfo().texturePath(): " + texId);
                  return texId;
               }
            }
         }

         ResourceLocation texId = callTexturePath(variant);
         if (texId != null) {
            System.out.println("[TextureEditor] Found via direct texturePath(): " + texId);
            return texId;
         }

         Method textureMethod = findMethod(variant.getClass(), "texture");
         if (textureMethod != null) {
            textureMethod.setAccessible(true);
            Object result = textureMethod.invoke(variant, new Object[0]);
            texId = resolveIdentifier(result);
            if (texId != null) {
               System.out.println("[TextureEditor] Found via direct texture(): " + texId);
               return texId;
            }
         }
      } catch (Exception e) {
         System.out.println("[TextureEditor] extractTextureFromVariant error: " + e.getMessage());
      }

      return null;
   }

   private static ResourceLocation callTexturePath(Object obj) {
      try {
         Method method = findMethod(obj.getClass(), "texturePath");
         if (method != null) {
            method.setAccessible(true);
            Object result = method.invoke(obj, new Object[0]);
            return resolveIdentifier(result);
         }
      } catch (Exception var3) {
      }

      return null;
   }

   private static ResourceLocation resolveIdentifier(Object obj) {
      if (obj == null) {
         return null;
      } else if (obj instanceof ResourceLocation id) {
         return id.getPath().startsWith("textures/") && id.getPath().endsWith(".png")
            ? id
            : ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/" + id.getPath() + ".png");
      } else {
         String str = obj.toString();
         if (str.contains(":")) {
            try {
               ResourceLocation id = ResourceLocation.parse(str);
               if (id.getPath().startsWith("textures/") && id.getPath().endsWith(".png")) {
                  return id;
               }

               return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/" + id.getPath() + ".png");
            } catch (Exception var3) {
            }
         }

         return null;
      }
   }

   private static ResourceLocation tryHorseVariant(Entity entity) {
      try {
         for (Method m : entity.getClass().getMethods()) {
            if (m.getParameterCount() == 0) {
               Class<?> ret = m.getReturnType();
               if (ret.isEnum() && (ret.getSimpleName().contains("HorseColor") || ret.getSimpleName().equals("Variant"))) {
                  m.setAccessible(true);
                  Object color = m.invoke(entity, new Object[0]);
                  if (color != null) {
                     String colorName = color.toString().toLowerCase();
                     System.out.println("[TextureEditor] Horse color (via " + m.getName() + "): " + colorName);

                     String texName = switch (colorName) {
                        case "white" -> "horse_white";
                        case "creamy" -> "horse_creamy";
                        case "chestnut" -> "horse_chestnut";
                        case "brown" -> "horse_brown";
                        case "black" -> "horse_black";
                        case "gray" -> "horse_gray";
                        case "dark_brown" -> "horse_darkbrown";
                        default -> "horse_" + colorName;
                     };
                     return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/horse/" + texName + ".png");
                  }
               }
            }
         }
      } catch (Exception e) {
         System.out.println("[TextureEditor] tryHorseVariant error: " + e.getMessage());
      }

      return null;
   }

   private static ResourceLocation tryEnumVariantTexture(Entity entity, String prefix, Map<String, String> nameMap) {
      try {
         Method getVariantMethod = findMethod(entity.getClass(), "getVariant");
         if (getVariantMethod != null) {
            getVariantMethod.setAccessible(true);
            Object variant = getVariantMethod.invoke(entity, new Object[0]);
            if (variant != null && variant.getClass().isEnum()) {
               String varName = variant.toString().toLowerCase();
               if (nameMap != null && nameMap.containsKey(varName)) {
                  varName = (String)nameMap.get(varName);
               }

               System.out.println("[TextureEditor] Enum variant: " + varName);
               return ResourceLocation.fromNamespaceAndPath("minecraft", prefix + varName + ".png");
            }
         }
      } catch (Exception e) {
         System.out.println("[TextureEditor] tryEnumVariantTexture error: " + e.getMessage());
      }

      return null;
   }

   private static ResourceLocation tryRabbitVariant(Entity entity) {
      try {
         for (String name : new String[]{"getRabbitType", "getVariant", "getRabbitVariant"}) {
            Method m = findMethod(entity.getClass(), name);
            if (m != null) {
               m.setAccessible(true);
               Object result = m.invoke(entity, new Object[0]);
               if (result instanceof Integer typeInt) {
                  return rabbitTypeToTexture(typeInt);
               }

               if (result != null && result.getClass().isEnum()) {
                  int ordinal = ((Enum)result).ordinal();
                  return rabbitTypeToTexture(ordinal);
               }
            }
         }

         for (Method m : entity.getClass().getDeclaredMethods()) {
            if (m.getParameterCount() == 0 && m.getReturnType() == int.class) {
               String mName = m.getName().toLowerCase();
               if (mName.contains("rabbit") || mName.contains("type") || mName.contains("variant")) {
                  m.setAccessible(true);
                  int typeInt = (Integer)m.invoke(entity, new Object[0]);
                  System.out.println("[TextureEditor] Rabbit type (via " + m.getName() + "): " + typeInt);
                  return rabbitTypeToTexture(typeInt);
               }
            }
         }
      } catch (Exception e) {
         System.out.println("[TextureEditor] tryRabbitVariant error: " + e.getMessage());
      }

      return null;
   }

   private static ResourceLocation rabbitTypeToTexture(int typeInt) {
      String texName = switch (typeInt) {
         case 0 -> "brown";
         case 1 -> "white";
         case 2 -> "black";
         case 3 -> "white_splotched";
         case 4 -> "gold";
         case 5 -> "salt";
         case 99 -> "caerbannog";
         default -> "brown";
      };
      System.out.println("[TextureEditor] Rabbit type: " + typeInt + " -> " + texName);
      return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/rabbit/" + texName + ".png");
   }

   private static ResourceLocation tryFoxVariant(Entity entity) {
      try {
         for (String name : new String[]{"getFoxType", "getVariant", "getFoxVariant"}) {
            Method m = findMethod(entity.getClass(), name);
            if (m != null && m.getReturnType().isEnum()) {
               m.setAccessible(true);
               Object foxType = m.invoke(entity, new Object[0]);
               if (foxType != null) {
                  String typeName = foxType.toString().toLowerCase();
                  System.out.println("[TextureEditor] Fox type (via " + m.getName() + "): " + typeName);
                  String texName = typeName.equals("snow") ? "snow_fox" : "fox";
                  return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/fox/" + texName + ".png");
               }
            }
         }

         for (Method m : entity.getClass().getDeclaredMethods()) {
            if (m.getParameterCount() == 0 && m.getReturnType().isEnum()) {
               String retName = m.getReturnType().getSimpleName();
               if (retName.contains("Type") || retName.contains("Fox")) {
                  m.setAccessible(true);
                  Object foxType = m.invoke(entity, new Object[0]);
                  if (foxType != null) {
                     String typeName = foxType.toString().toLowerCase();
                     System.out.println("[TextureEditor] Fox type (via scan " + m.getName() + "): " + typeName);
                     String texName = typeName.equals("snow") ? "snow_fox" : "fox";
                     return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/fox/" + texName + ".png");
                  }
               }
            }
         }
      } catch (Exception e) {
         System.out.println("[TextureEditor] tryFoxVariant error: " + e.getMessage());
      }

      return null;
   }

   private static ResourceLocation tryParrotVariant(Entity entity) {
      try {
         for (String name : new String[]{"getVariant", "getParrotVariant"}) {
            Method m = findMethod(entity.getClass(), name);
            if (m != null && m.getReturnType().isEnum()) {
               m.setAccessible(true);
               Object variant = m.invoke(entity, new Object[0]);
               if (variant != null) {
                  String varName = variant.toString().toLowerCase();
                  System.out.println("[TextureEditor] Parrot variant (via " + m.getName() + "): " + varName);

                  String texName = switch (varName) {
                     case "red_blue" -> "parrot_red_blue";
                     case "blue" -> "parrot_blue";
                     case "green" -> "parrot_green";
                     case "yellow_blue" -> "parrot_yellow_blue";
                     case "gray" -> "parrot_grey";
                     default -> "parrot_" + varName;
                  };
                  return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/parrot/" + texName + ".png");
               }
            }
         }
      } catch (Exception e) {
         System.out.println("[TextureEditor] tryParrotVariant error: " + e.getMessage());
      }

      return null;
   }

   private static Method findMethod(Class<?> clazz, String name) {
      for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
         for (Method m : c.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == 0) {
               return m;
            }
         }

         for (Class<?> iface : c.getInterfaces()) {
            for (Method m : iface.getDeclaredMethods()) {
               if (m.getName().equals(name) && m.getParameterCount() == 0) {
                  return m;
               }
            }
         }
      }

      return null;
   }

   private static List<ResourceLocation> buildTexturePaths(ResourceLocation entityId) {
      List<ResourceLocation> paths = new ArrayList();
      String ns = entityId.getNamespace();
      String path = entityId.getPath();
      paths.add(ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/" + path + "/" + path + ".png"));
      paths.add(ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/" + path + ".png"));
      switch (path) {
         case "iron_golem":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/iron_golem/iron_golem.png"));
            break;
         case "snow_golem":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/snow_golem.png"));
            break;
         case "armor_stand":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/armor_stand/wood.png"));
            break;
         case "ender_dragon":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/enderdragon/dragon.png"));
            break;
         case "wither":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/wither/wither.png"));
            break;
         case "guardian":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/guardian.png"));
            break;
         case "elder_guardian":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/guardian_elder.png"));
            break;
         case "horse":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/horse/horse_white.png"));
            break;
         case "donkey":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/horse/donkey.png"));
            break;
         case "mule":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/horse/mule.png"));
            break;
         case "skeleton_horse":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/horse/horse_skeleton.png"));
            break;
         case "zombie_horse":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/horse/horse_zombie.png"));
            break;
         case "llama":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/llama/creamy.png"));
            break;
         case "trader_llama":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/llama/creamy.png"));
            break;
         case "cat":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/cat/tabby.png"));
            break;
         case "wolf":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/wolf/wolf.png"));
            break;
         case "fox":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/fox/fox.png"));
            break;
         case "parrot":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/parrot/parrot_red_blue.png"));
            break;
         case "rabbit":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/rabbit/brown.png"));
            break;
         case "villager":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/villager/villager.png"));
            break;
         case "zombie_villager":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/zombie_villager/zombie_villager.png"));
            break;
         case "wandering_trader":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/wandering_trader.png"));
            break;
         case "witch":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/witch.png"));
            break;
         case "shulker":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/shulker/shulker.png"));
            break;
         case "slime":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/slime/slime.png"));
            break;
         case "magma_cube":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/slime/magmacube.png"));
            break;
         case "ghast":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/ghast/ghast.png"));
            break;
         case "blaze":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/blaze.png"));
            break;
         case "enderman":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/enderman/enderman.png"));
            break;
         case "endermite":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/endermite.png"));
            break;
         case "silverfish":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/silverfish.png"));
            break;
         case "bat":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/bat.png"));
            break;
         case "phantom":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/phantom.png"));
            break;
         case "panda":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/panda/panda.png"));
            break;
         case "bee":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/bee/bee.png"));
            break;
         case "piglin":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/piglin/piglin.png"));
            break;
         case "piglin_brute":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/piglin/piglin_brute.png"));
            break;
         case "zombified_piglin":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/piglin/zombified_piglin.png"));
            break;
         case "hoglin":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/hoglin/hoglin.png"));
            break;
         case "zoglin":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/hoglin/zoglin.png"));
            break;
         case "strider":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/strider/strider.png"));
            break;
         case "ravager":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/illager/ravager.png"));
            break;
         case "pillager":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/illager/pillager.png"));
            break;
         case "vindicator":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/illager/vindicator.png"));
            break;
         case "evoker":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/illager/evoker.png"));
            break;
         case "illusioner":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/illager/illusioner.png"));
            break;
         case "vex":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/illager/vex.png"));
            break;
         case "warden":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/warden/warden.png"));
            break;
         case "allay":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/allay/allay.png"));
            break;
         case "frog":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/frog/temperate_frog.png"));
            break;
         case "tadpole":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/tadpole/tadpole.png"));
            break;
         case "camel":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/camel/camel.png"));
            break;
         case "sniffer":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/sniffer/sniffer.png"));
            break;
         case "breeze":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/breeze/breeze.png"));
            break;
         case "bogged":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/skeleton/bogged.png"));
            break;
         case "axolotl":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/axolotl/axolotl_lucy.png"));
            break;
         case "glow_squid":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/squid/glow_squid.png"));
            break;
         case "squid":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/squid/squid.png"));
            break;
         case "goat":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/goat/goat.png"));
            break;
         case "tropical_fish":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/fish/tropical_a.png"));
            break;
         case "salmon":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/fish/salmon.png"));
            break;
         case "cod":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/fish/cod.png"));
            break;
         case "pufferfish":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/fish/pufferfish.png"));
            break;
         case "dolphin":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/dolphin.png"));
            break;
         case "turtle":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/turtle/big_sea_turtle.png"));
            break;
         case "ocelot":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/cat/ocelot.png"));
            break;
         case "mooshroom":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/cow/red_mooshroom.png"));
            break;
         case "cow":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/cow/cow.png"));
            break;
         case "pig":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/pig/pig.png"));
            break;
         case "sheep":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/sheep/sheep.png"));
            break;
         case "chicken":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/chicken/chicken.png"));
            break;
         case "creeper":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/creeper/creeper.png"));
            break;
         case "skeleton":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/skeleton/skeleton.png"));
            break;
         case "wither_skeleton":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/skeleton/wither_skeleton.png"));
            break;
         case "stray":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/skeleton/stray.png"));
            break;
         case "zombie":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/zombie/zombie.png"));
            break;
         case "husk":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/zombie/husk.png"));
            break;
         case "drowned":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/zombie/drowned.png"));
            break;
         case "spider":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/spider/spider.png"));
            break;
         case "cave_spider":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/spider/cave_spider.png"));
            break;
         case "boat":
         case "oak_boat":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/boat/oak.png"));
            break;
         case "chest_boat":
         case "oak_chest_boat":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/chest_boat/oak.png"));
            break;
         case "minecart":
            paths.add(0, ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/minecart.png"));
      }

      paths.add(ResourceLocation.fromNamespaceAndPath(ns, "textures/entity/" + path + "/default.png"));
      return paths;
   }

   public record MobTexture(ResourceLocation textureId, int[][] pixels, int width, int height, String entityName) {
   }
}
