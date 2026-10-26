package com.zeeesea.textureeditor.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.zeeesea.textureeditor.editor.ExternalEditorDetector;
import com.zeeesea.textureeditor.editor.ExternalEditorManager;
import com.zeeesea.textureeditor.helper.NotificationHelper;
import com.zeeesea.textureeditor.settings.ModSettings;
import com.zeeesea.textureeditor.texture.TextureManager;
import java.io.File;
import java.util.List;

public class ExternalEditorSettingsScreen extends Screen {
   private final Screen parent;
   private EditBox customPathField;
   private int detectedEditorsEndY;

   public ExternalEditorSettingsScreen(Screen parent) {
      super(Component.literal("External Editor Settings"));
      this.parent = parent;
   }

   protected void init() {
      ModSettings s = ModSettings.getInstance();
      int centerX = this.width / 2;
      int y = 35;
      this.addRenderableWidget(Button.builder(Component.literal("Use External Editor: " + (s.useExternalEditor ? "§aON" : "§cOFF")), btn -> {
         s.useExternalEditor = !s.useExternalEditor;
         s.save();
         this.clearWidgets();
         this.init();
      }).pos(centerX - 120, y).size(240, 20).build());
      y += 30;
      List<ExternalEditorDetector.DetectedEditor> detected = ExternalEditorDetector.detectEditors();
      if (!detected.isEmpty()) {
         y += 14;

         for (ExternalEditorDetector.DetectedEditor editor : detected) {
            boolean isSelected = editor.name().equals(s.selectedEditorName);
            String label = (isSelected ? "§a✔ " : "§7  ") + editor.name();
            this.addRenderableWidget(Button.builder(Component.literal(label), btn -> {
               s.selectedEditorName = editor.name();
               s.save();
               this.clearWidgets();
               this.init();
            }).pos(centerX - 120, y).size(240, 20).build());
            y += 22;
         }
      } else {
         y += 18;
      }

      this.detectedEditorsEndY = y;
      y += 8;
      y += 12;
      this.customPathField = new EditBox(this.font, centerX - 120, y, 240, 18, Component.literal("Custom Path"));
      this.customPathField.setMaxLength(500);
      this.customPathField.setValue(s.externalEditorCustomPath != null ? s.externalEditorCustomPath : "");
      this.customPathField.setResponder(text -> {
         s.externalEditorCustomPath = text.trim();
         s.save();
      });
      this.addRenderableWidget(this.customPathField);
      y += 28;
      this.addRenderableWidget(Button.builder(Component.literal("§eRefresh Detection"), btn -> {
         ExternalEditorDetector.clearCache();
         this.clearWidgets();
         this.init();
      }).pos(centerX - 120, y).size(115, 20).build());
      this.addRenderableWidget(Button.builder(Component.literal("§cDelete Cache"), btn -> {
         ExternalEditorManager.deleteCache();
         Minecraft.getInstance().execute(() -> NotificationHelper.addToast(SystemToast.SystemToastId.PERIODIC_NOTIFICATION, "Cache Cleared", "All temp files deleted."));
      }).pos(centerX + 5, y).size(115, 20).build());
      y += 24;
      this.addRenderableWidget(Button.builder(Component.literal("§cReset All Textures"), btn -> {
         TextureManager.getInstance().clear();
         ExternalEditorManager.deleteCache();
         Minecraft.getInstance().reloadResourcePacks();
      }).pos(centerX - 120, y).size(240, 20).build());
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
      context.drawCenteredString(this.font, "§l§6External Editor Settings", this.width / 2, 10, 16777215);
      int centerX = this.width / 2;
      List<ExternalEditorDetector.DetectedEditor> detected = ExternalEditorDetector.detectEditors();
      int labelY = 65;
      if (!detected.isEmpty()) {
         context.drawString(this.font, "§eDetected Editors:", centerX - 118, labelY, 16777215, false);
      } else {
         context.drawString(this.font, "§cNo editors auto-detected", centerX - 118, labelY + 4, 16733525, false);
      }

      int customLabelY = this.detectedEditorsEndY + 8;
      context.drawString(this.font, "Custom Editor Path (overrides auto):", centerX - 118, customLabelY, 11184810, false);
      ModSettings s = ModSettings.getInstance();
      String editorPath = this.getResolvedPath(s);
      int statusY = this.height - 20;
      if (editorPath != null && !editorPath.isEmpty()) {
         boolean exists = new File(editorPath).exists() || editorPath.equals("mspaint");
         String status = exists ? "§aCurrent: " + this.shortenPath(editorPath) : "§cNot found: " + this.shortenPath(editorPath);
         context.drawCenteredString(this.font, status, centerX, statusY, 16777215);
      } else {
         context.drawCenteredString(this.font, "§7No editor configured", centerX, statusY, 16777215);
      }

      super.render(context, mouseX, mouseY, delta);
   }

   private String getResolvedPath(ModSettings s) {
      if (s.externalEditorCustomPath != null && !s.externalEditorCustomPath.isEmpty()) {
         return s.externalEditorCustomPath;
      }

      if (s.selectedEditorName != null && !s.selectedEditorName.isEmpty()) {
         for (ExternalEditorDetector.DetectedEditor editor : ExternalEditorDetector.detectEditors()) {
            if (editor.name().equals(s.selectedEditorName)) {
               return editor.executablePath();
            }
         }
      }

      ExternalEditorDetector.DetectedEditor def = ExternalEditorDetector.getDefault();
      return def != null ? def.executablePath() : null;
   }

   private String shortenPath(String path) {
      return path.length() > 60 ? "..." + path.substring(path.length() - 57) : path;
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
