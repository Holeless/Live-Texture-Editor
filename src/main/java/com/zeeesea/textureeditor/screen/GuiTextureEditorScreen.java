package com.zeeesea.textureeditor.screen;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.zeeesea.textureeditor.editor.PixelCanvas;
import com.zeeesea.textureeditor.texture.TextureManager;
import com.zeeesea.textureeditor.texture.TextureResourceLoader;
import com.zeeesea.textureeditor.util.ImageColorCompat;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

public class GuiTextureEditorScreen extends AbstractEditorScreen {
   private final ResourceLocation guiTextureId;
   private final String displayName;
   private final Screen parent;
   private ResourceLocation fullTextureId;
   private boolean isSpriteTexture = false;
   private ResourceLocation spriteAtlasId;
   private static final Map<String, String[]> ALTERNATIVE_PATHS = Map.of(
      "textures/entity/elytra.png",
      new String[]{"textures/entity/equipment/wings/elytra.png", "textures/entity/elytra/elytra.png", "textures/entity/equipment/elytra.png"}
   );

   public GuiTextureEditorScreen(ResourceLocation guiTextureId, String displayName, Screen parent) {
      super(Component.literal("GUI Texture Editor"));
      this.guiTextureId = guiTextureId;
      this.displayName = displayName;
      this.parent = parent;
      this.zoom = 4;
   }

   @Override
   protected void loadTexture() {
      System.out.println("[TextureEditor] Loading GUI texture: " + this.guiTextureId);
      if (this.guiTextureId.getPath().startsWith("textures/")) {
         this.fullTextureId = this.guiTextureId;
      } else {
         this.fullTextureId = ResourceLocation.fromNamespaceAndPath(this.guiTextureId.getNamespace(), "textures/" + this.guiTextureId.getPath() + ".png");
      }

      this.isSpriteTexture = this.guiTextureId.getPath().startsWith("gui/sprites/");
      if (this.isSpriteTexture) {
         this.spriteAtlasId = ResourceLocation.fromNamespaceAndPath(this.guiTextureId.getNamespace(), this.guiTextureId.getPath().replace("gui/sprites/", ""));
      }

      Minecraft client = Minecraft.getInstance();
      int[][] savedPixels = TextureManager.getInstance().getPixels(this.fullTextureId);
      int[] savedDims = TextureManager.getInstance().getDimensions(this.fullTextureId);
      Set<ResourceLocation> candidateSet = new LinkedHashSet();
      candidateSet.add(this.fullTextureId);
      String[] alts = (String[])ALTERNATIVE_PATHS.get(this.fullTextureId.getPath());
      if (alts != null) {
         for (String alt : alts) {
            candidateSet.add(ResourceLocation.fromNamespaceAndPath(this.fullTextureId.getNamespace(), alt));
         }
      }

      this.addArmorAliasCandidates(candidateSet, this.fullTextureId);
      if (this.fullTextureId.getPath().startsWith("textures/entity/") && this.fullTextureId.getPath().endsWith(".png")) {
         String filename = this.fullTextureId.getPath().replace("textures/entity/", "").replace(".png", "");
         if (!filename.contains("/")) {
            candidateSet.add(ResourceLocation.fromNamespaceAndPath(this.fullTextureId.getNamespace(), "textures/entity/" + filename + "/" + filename + ".png"));
            candidateSet.add(ResourceLocation.fromNamespaceAndPath(this.fullTextureId.getNamespace(), "textures/entity/equipment/wings/" + filename + ".png"));
            candidateSet.add(ResourceLocation.fromNamespaceAndPath(this.fullTextureId.getNamespace(), "textures/entity/equipment/" + filename + ".png"));
         }
      }

      List<ResourceLocation> candidates = new ArrayList(candidateSet);

      try {
         for (ResourceLocation candidateId : candidates) {
            System.out.println("[TextureEditor] Trying texture path: " + candidateId);
            Optional<Resource> optResource = client.getResourceManager().getResource(candidateId);
            if (optResource.isPresent()) {
               System.out.println("[TextureEditor] Resource FOUND: " + candidateId);
               this.fullTextureId = candidateId;
               TextureResourceLoader.LoadedTexture defaultTex = TextureResourceLoader.loadTexture(this.fullTextureId);
               int w;
               int h;
               int[][] basePixels;
               if (defaultTex != null) {
                  w = defaultTex.width();
                  h = defaultTex.height();
                  basePixels = defaultTex.pixels();
               } else {
                  InputStream stream = ((Resource)optResource.get()).open();
                  NativeImage image = NativeImage.read(stream);
                  w = image.getWidth();
                  h = image.getHeight();
                  basePixels = new int[w][h];

                  for (int x = 0; x < w; x++) {
                     for (int y = 0; y < h; y++) {
                        basePixels[x][y] = ImageColorCompat.readArgb(image, x, y);
                     }
                  }

                  image.close();
                  stream.close();
               }

               System.out.println("[TextureEditor] Image loaded: " + w + "x" + h);
               this.originalPixels = copyPixels(basePixels, w, h);
               savedPixels = TextureManager.getInstance().getPixels(this.fullTextureId);
               savedDims = TextureManager.getInstance().getDimensions(this.fullTextureId);
               if (savedPixels != null && savedDims != null && savedDims[0] == w && savedDims[1] == h) {
                  this.canvas = new PixelCanvas(savedDims[0], savedDims[1], savedPixels);
               } else {
                  this.canvas = new PixelCanvas(w, h, basePixels);
               }

               return;
            }
         }

         System.out.println("[TextureEditor] FAILED to find texture after trying all candidates for: " + this.guiTextureId);
      } catch (Exception e) {
         System.out.println("[TextureEditor] Failed to load GUI texture: " + this.fullTextureId + " - " + e.getMessage());
         e.printStackTrace();
      }
   }

   private void addArmorAliasCandidates(Set<ResourceLocation> candidates, ResourceLocation baseId) {
      String path = baseId.getPath();
      if (path.startsWith("textures/models/armor/") && path.endsWith(".png")) {
         String name = path.substring("textures/models/armor/".length(), path.length() - 4);
         String suffix = "";
         if (name.endsWith("_overlay")) {
            suffix = "_overlay";
            name = name.substring(0, name.length() - "_overlay".length());
         }

         if (name.endsWith("_layer_1")) {
            String material = this.normalizeArmorMaterial(name.substring(0, name.length() - "_layer_1".length()));
            candidates.add(ResourceLocation.fromNamespaceAndPath(baseId.getNamespace(), "textures/entity/equipment/humanoid/" + material + suffix + ".png"));
         } else if (name.endsWith("_layer_2")) {
            String material = this.normalizeArmorMaterial(name.substring(0, name.length() - "_layer_2".length()));
            candidates.add(ResourceLocation.fromNamespaceAndPath(baseId.getNamespace(), "textures/entity/equipment/humanoid_leggings/" + material + suffix + ".png"));
         } else {
            if (name.contains("_piglin_helmet")) {
               String material = this.normalizeArmorMaterial(name.replace("_piglin_helmet", ""));
               candidates.add(ResourceLocation.fromNamespaceAndPath(baseId.getNamespace(), "textures/entity/equipment/piglin_head/" + material + suffix + ".png"));
               candidates.add(ResourceLocation.fromNamespaceAndPath(baseId.getNamespace(), "textures/entity/equipment/humanoid/" + material + suffix + ".png"));
            }
         }
      }
   }

   private String normalizeArmorMaterial(String material) {
      String normalized = material;
      if (normalized.startsWith("piglin_")) {
         normalized = normalized.substring("piglin_".length());
      }

      if ("turtle".equals(normalized)) {
         normalized = "turtle_scute";
      }

      return normalized;
   }

   @Override
   protected String getEditorTitle() {
      return "GUI Editor - " + this.displayName;
   }

   @Override
   protected String getResetCurrentLabel() {
      return "Reset";
   }

   @Override
   protected Screen getBackScreen() {
      return this.parent;
   }

   @Override
   protected int getMaxZoom() {
      return 20;
   }

   @Override
   protected int getMinZoom() {
      return 1;
   }

   @Override
   protected int getZoomStep() {
      return 1;
   }

   @Override
   protected int addExtraButtons(int toolY) {
      if (this.guiTextureId.getPath().contains("elytra")) {
         int rsw = this.getRightSidebarWidth();
         int resetBtnW = rsw - 10;
         int tbh = this.getToolButtonHeight();
         this.addRenderableWidget(
            Button.builder(
                  Component.literal("Edit Item"),
                  btn -> Minecraft.getInstance().setScreen(new ItemEditorScreen(new ItemStack(Items.ELYTRA), this.parent))
               )
               .pos(this.width - rsw + 5, this.height - 124)
               .size(resetBtnW, tbh)
               .build()
         );
      }

      return toolY;
   }

   @Override
   protected void applyLive() {
      if (this.fullTextureId != null && this.canvas != null) {
         Minecraft client = Minecraft.getInstance();
         TextureManager.getInstance().putTexture(this.fullTextureId, this.canvas.getPixels(), this.canvas.getWidth(), this.canvas.getHeight());
         this.uploadCanvasToTarget(client);
      }
   }

   private void uploadCanvasToTarget(Minecraft client) {
      if (this.canvas != null) {
         client.execute(() -> {
            NativeImage img = new NativeImage(this.canvas.getWidth(), this.canvas.getHeight(), false);

            for (int x = 0; x < this.canvas.getWidth(); x++) {
               for (int y = 0; y < this.canvas.getHeight(); y++) {
                  ImageColorCompat.writeArgb(img, x, y, this.canvas.getPixels()[x][y]);
               }
            }

            ResourceLocation spriteId = this.guiTextureId;
            Pair<TextureAtlas, TextureAtlasSprite> atlasAndSprite = this.findSpriteInAtlases(client, spriteId);
            if (atlasAndSprite != null) {
               TextureAtlas atlas = (TextureAtlas)atlasAndSprite.getLeft();
               TextureAtlasSprite sprite = (TextureAtlasSprite)atlasAndSprite.getRight();
               System.out.println("[TextureEditor] Updating sprite in atlas: " + spriteId);
               atlas.bind();
               img.upload(0, sprite.getX(), sprite.getY(), false);
            } else {
               DynamicTexture dynamicTex = new DynamicTexture(img);
               client.getTextureManager().register(this.fullTextureId, dynamicTex);
               dynamicTex.bind();
               img.upload(0, 0, 0, false);
               System.out.println("[TextureEditor] Applied live GUI texture: " + this.fullTextureId);
            }
         });
      }
   }

   private Pair<TextureAtlas, TextureAtlasSprite> findSpriteInAtlases(Minecraft client, ResourceLocation id) {
      TextureAtlas blockAtlas = client.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
      TextureAtlasSprite sprite = blockAtlas.getSprite(id);
      if (sprite != null && !sprite.contents().name().getPath().equals("missingno") && sprite.contents().name().equals(id)) {
         return Pair.of(blockAtlas, sprite);
      }

      ResourceLocation guiAtlasId = ResourceLocation.parse("textures/atlas/gui.png");
      if (client.getTextureManager().getTexture(guiAtlasId) instanceof TextureAtlas guiAtlas) {
         sprite = guiAtlas.getSprite(id);
         if (sprite != null && !sprite.contents().name().getPath().equals("missingno") && sprite.contents().name().equals(id)) {
            return Pair.of(guiAtlas, sprite);
         }

         if (id.getPath().startsWith("gui/sprites/")) {
            String shortPath = id.getPath().substring("gui/sprites/".length());
            ResourceLocation shortId = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), shortPath);
            sprite = guiAtlas.getSprite(shortId);
            if (sprite != null && !sprite.contents().name().getPath().equals("missingno")) {
               return Pair.of(guiAtlas, sprite);
            }
         }
      }

      return null;
   }

   @Override
   protected void resetCurrent() {
      if (this.fullTextureId != null && this.canvas != null) {
         TextureResourceLoader.LoadedTexture defaultTex = TextureResourceLoader.loadTexture(this.fullTextureId);
         int[][] resetPixels = defaultTex != null ? defaultTex.pixels() : this.originalPixels;
         if (resetPixels != null) {
            if (defaultTex != null) {
               this.originalPixels = copyPixels(defaultTex.pixels(), defaultTex.width(), defaultTex.height());
            }

            this.canvas.saveSnapshot();
            if (this.canvas.getWidth() == resetPixels.length && this.canvas.getHeight() == resetPixels[0].length) {
               for (int x = 0; x < this.canvas.getWidth(); x++) {
                  for (int y = 0; y < this.canvas.getHeight(); y++) {
                     this.canvas.setPixel(x, y, resetPixels[x][y]);
                  }
               }
            } else {
               this.canvas = new PixelCanvas(resetPixels.length, resetPixels[0].length, resetPixels);
            }

            TextureManager.getInstance().removeTexture(this.fullTextureId);
            this.uploadCanvasToTarget(Minecraft.getInstance());
         }
      }
   }
}
