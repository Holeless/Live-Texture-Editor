package com.zeeesea.textureeditor.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.zeeesea.textureeditor.settings.ModSettings;

public class SettingsScreen extends Screen {
   private final Screen parent;

   public SettingsScreen(Screen parent) {
      super(Component.literal("Texture Editor Settings"));
      this.parent = parent;
   }

   protected void init() {
      ModSettings s = ModSettings.getInstance();
      int centerX = this.width / 2;
      int y = 40;
      this.addRenderableWidget(Button.builder(Component.literal("Auto Apply Live: " + (s.autoApplyLive ? "ON" : "OFF")), btn -> {
         s.autoApplyLive = !s.autoApplyLive;
         s.save();
         this.clearWidgets();
         this.init();
      }).pos(centerX - 100, y).size(200, 20).build());
      y += 28;
      this.addRenderableWidget(Button.builder(Component.literal("Grid Default: " + (s.gridOnByDefault ? "ON" : "OFF")), btn -> {
         s.gridOnByDefault = !s.gridOnByDefault;
         s.save();
         this.clearWidgets();
         this.init();
      }).pos(centerX - 100, y).size(200, 20).build());
      y += 28;
      this.addRenderableWidget(Button.builder(Component.literal("Tool Keybind Hints: " + (s.showToolHints ? "ON" : "OFF")), btn -> {
         s.showToolHints = !s.showToolHints;
         s.save();
         this.clearWidgets();
         this.init();
      }).pos(centerX - 100, y).size(200, 20).build());
      y += 28;
      this.addRenderableWidget(Button.builder(Component.literal("Max Undo Steps: " + s.maxUndoSteps), btn -> {
         s.maxUndoSteps = switch (s.maxUndoSteps) {
            case 10 -> 25;
            case 25 -> 50;
            case 50 -> 100;
            case 100 -> 200;
            default -> 10;
         };
         s.save();
         this.clearWidgets();
         this.init();
      }).pos(centerX - 100, y).size(200, 20).build());
      y += 28;
      this.addRenderableWidget(Button.builder(Component.literal("Color History Size: " + s.colorHistorySize), btn -> {
         s.colorHistorySize = switch (s.colorHistorySize) {
            case 5 -> 10;
            case 10 -> 15;
            case 15 -> 20;
            default -> 5;
         };
         s.save();
         this.clearWidgets();
         this.init();
      }).pos(centerX - 100, y).size(200, 20).build());
      y += 28;
      this.addRenderableWidget(Button.builder(Component.literal("Default Tool: " + s.defaultTool), btn -> {
         s.defaultTool = switch (s.defaultTool) {
            case "Pencil" -> "Brush";
            case "Brush" -> "Eraser";
            case "Eraser" -> "Fill";
            case "Fill" -> "Eyedropper";
            case "Eyedropper" -> "Line";
            default -> "Pencil";
         };
         s.save();
         this.clearWidgets();
         this.init();
      }).pos(centerX - 100, y).size(200, 20).build());
      y += 28;
      int variationPercent = Math.round(s.brushVariation * 100.0F);
      this.addRenderableWidget(Button.builder(Component.literal("Brush Variation: " + variationPercent + "%"), btn -> {
         s.brushVariation = switch (variationPercent) {
            case 5 -> 0.1F;
            case 10 -> 0.15F;
            case 15 -> 0.2F;
            case 20 -> 0.3F;
            case 30 -> 0.5F;
            default -> 0.05F;
         };
         s.save();
         this.clearWidgets();
         this.init();
      }).pos(centerX - 100, y).size(200, 20).build());
      y += 40;
      this.addRenderableWidget(
         Button.builder(
               Component.literal("§bExternal Editor... (Experimental)"), btn -> this.minecraft.setScreen(new ExternalEditorSettingsScreen(this))
            )
            .pos(centerX - 100, y)
            .size(200, 20)
            .build()
      );
      y += 28;
      this.addRenderableWidget(
         Button.builder(Component.literal("§eEditor Keybinds..."), btn -> this.minecraft.setScreen(new KeybindSettingsScreen(this)))
            .pos(centerX - 100, y)
            .size(200, 20)
            .build()
      );
      y += 40;
      this.addRenderableWidget(
         Button.builder(Component.literal("Done"), btn -> this.onClose())
            .pos(centerX - 50, y)
            .size(100, 20)
            .build()
      );
   }

   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      context.fill(0, 0, this.width, this.height, -15066578);
      context.drawCenteredString(this.font, "§l§6Texture Editor Settings", this.width / 2, 15, 16777215);
      super.render(context, mouseX, mouseY, delta);
   }

   public void renderBackground(GuiGraphics ctx, int mx, int my, float d) {
   }

   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   public boolean keyPressed(int kc, int sc, int m) {
      if (kc == 256) {
         this.onClose();
         return true;
      } else {
         return super.keyPressed(kc, sc, m);
      }
   }

   public boolean isPauseScreen() {
      return false;
   }
}
