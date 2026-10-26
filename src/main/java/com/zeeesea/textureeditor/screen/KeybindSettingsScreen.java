package com.zeeesea.textureeditor.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.zeeesea.textureeditor.settings.ModSettings;

public class KeybindSettingsScreen extends Screen {
   private final Screen parent;
   private String waitingForKey = null;
   private static final String[][] ACTIONS = new String[][]{
      {"pencil", "Pencil Tool"},
      {"brush", "Brush Tool"},
      {"eraser", "Eraser Tool"},
      {"fill", "Fill Tool"},
      {"eyedropper", "Eyedropper Tool"},
      {"line", "Line Tool"},
      {"undo", "Undo"},
      {"redo", "Redo"},
      {"grid", "Toggle Grid"},
      {"browse", "Browse Tool"}
   };

   public KeybindSettingsScreen(Screen parent) {
      super(Component.literal("Editor Keybinds"));
      this.parent = parent;
   }

   protected void init() {
      ModSettings s = ModSettings.getInstance();
      int centerX = this.width / 2;
      int y = 50;

      for (String[] action : ACTIONS) {
         String actionId = action[0];
         String displayName = action[1];
         String keyName = s.getKeyName(actionId);
         boolean isWaiting = actionId.equals(this.waitingForKey);
         String btnText = isWaiting ? "§e> Press a key <" : displayName + ": [§b" + keyName + "§r]";
         String aid = actionId;
         this.addRenderableWidget(Button.builder(Component.literal(btnText), btn -> {
            this.waitingForKey = aid;
            this.clearWidgets();
            this.init();
         }).pos(centerX - 130, y).size(260, 20).build());
         y += 24;
      }

      y += 20;
      this.addRenderableWidget(Button.builder(Component.literal("§cReset to Defaults"), btn -> {
         ModSettings settings = ModSettings.getInstance();
         settings.keybinds.put("pencil", 66);
         settings.keybinds.put("brush", 86);
         settings.keybinds.put("eraser", 69);
         settings.keybinds.put("fill", 70);
         settings.keybinds.put("eyedropper", 73);
         settings.keybinds.put("line", 76);
         settings.keybinds.put("undo", 89);
         settings.keybinds.put("redo", 85);
         settings.keybinds.put("grid", 71);
         settings.keybinds.put("browse", 341);
         settings.save();
         this.waitingForKey = null;
         this.clearWidgets();
         this.init();
      }).pos(centerX - 80, y).size(160, 20).build());
      y += 30;
      this.addRenderableWidget(
         Button.builder(Component.literal("Done"), btn -> this.onClose())
            .pos(centerX - 50, y)
            .size(100, 20)
            .build()
      );
   }

   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      context.fill(0, 0, this.width, this.height, -15066578);
      context.drawCenteredString(this.font, "§l§eEditor Keybinds", this.width / 2, 15, 16777215);
      context.drawCenteredString(this.font, "§7Click a keybind to change it, then press the new key", this.width / 2, 30, 11184810);
      super.render(context, mouseX, mouseY, delta);
   }

   public void renderBackground(GuiGraphics ctx, int mx, int my, float d) {
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (this.waitingForKey != null) {
         if (keyCode == 256) {
            this.waitingForKey = null;
         } else {
            ModSettings.getInstance().setKeybind(this.waitingForKey, keyCode);
            this.waitingForKey = null;
         }

         this.clearWidgets();
         this.init();
         return true;
      } else if (keyCode == 256) {
         this.onClose();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   public boolean isPauseScreen() {
      return false;
   }
}
