package com.zeeesea.textureeditor.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import com.zeeesea.textureeditor.editor.PixelCanvas;
import com.zeeesea.textureeditor.texture.ResourcePackExporter;
import java.io.File;

public class ExportScreen extends Screen {
   private final Screen parent;
   private EditBox packNameInput;
   private EditBox descriptionInput;
   private EditBox authorInput;
   private PixelCanvas iconCanvas;
   private String statusMessage = "";
   private int statusColor = 16777215;
   private static final int ICON_SIZE = 64;
   private int iconZoom = 4;
   private int iconScreenX;
   private int iconScreenY;
   private int currentColor = -65536;
   private boolean showGrid = true;
   private int lastIconDragX = -1;
   private int lastIconDragY = -1;
   private static final int[] PALETTE = new int[]{-16777216, -1, -65536, -16711936, -16776961, -256, -32768, -8388480, -16744320, -8355712};

   public ExportScreen(Screen parent) {
      super(Component.literal("Export Texture Pack"));
      this.parent = parent;
      this.iconCanvas = new PixelCanvas(64, 64);

      for (int x = 0; x < 64; x++) {
         for (int y = 0; y < 64; y++) {
            this.iconCanvas.setPixel(x, y, 0);
         }
      }
   }

   protected void init() {
      int centerX = this.width / 2;
      int fieldWidth = 200;
      this.packNameInput = new EditBox(this.font, centerX - fieldWidth / 2, 40, fieldWidth, 18, Component.literal("Pack Name"));
      this.packNameInput.setMaxLength(64);
      this.packNameInput.setValue("My Texture Pack");
      this.addRenderableWidget(this.packNameInput);
      this.descriptionInput = new EditBox(this.font, centerX - fieldWidth / 2, 75, fieldWidth, 18, Component.literal("Description"));
      this.descriptionInput.setMaxLength(128);
      this.descriptionInput.setValue("Created with Texture Editor Mod");
      this.addRenderableWidget(this.descriptionInput);
      this.authorInput = new EditBox(this.font, centerX - fieldWidth / 2, 110, fieldWidth, 18, Component.literal("Author"));
      this.authorInput.setMaxLength(64);
      this.authorInput.setValue("");
      this.addRenderableWidget(this.authorInput);
      int guiScale = (int)Minecraft.getInstance().getWindow().getGuiScale();
      this.iconZoom = guiScale >= 4 ? 2 : (guiScale >= 3 ? 3 : 4);
      this.iconScreenX = centerX - 64 * this.iconZoom / 2;
      this.iconScreenY = 145;
      this.addRenderableWidget(
         Button.builder(Component.literal("Grid"), btn -> this.showGrid = !this.showGrid)
            .pos(this.iconScreenX + 64 * this.iconZoom + 10, this.iconScreenY)
            .size(50, 20)
            .build()
      );
      int exportBackPosX = centerX - 60;
      int exportBackPosY = this.height - 50;
      if (this.getWindowHeight() < 1080 || this.getGuiScale() >= 3) {
         exportBackPosX = centerX + fieldWidth / 2 + 30;
         exportBackPosY = 40;
      }

      this.addRenderableWidget(
         Button.builder(Component.literal("§aExport"), btn -> this.doExport())
            .pos(exportBackPosX, exportBackPosY)
            .size(120, 20)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("Back"), btn -> this.minecraft.setScreen(this.parent))
            .pos(exportBackPosX, exportBackPosY + 24)
            .size(120, 20)
            .build()
      );
   }

   public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
      context.fill(0, 0, this.width, this.height, -15066578);
   }

   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      super.render(context, mouseX, mouseY, delta);
      context.drawCenteredString(this.font, "Export Texture Pack", this.width / 2, 10, 16777215);
      context.drawString(this.font, "Pack Name:", this.width / 2 - 100, 30, 13421772, false);
      context.drawString(this.font, "Description:", this.width / 2 - 100, 65, 13421772, false);
      context.drawString(this.font, "Author:", this.width / 2 - 100, 100, 13421772, false);
      context.drawString(this.font, "Pack Icon (draw below):", this.iconScreenX, 135, 13421772, false);
      this.drawIconCanvas(context, mouseX, mouseY);
      int palX = this.iconScreenX + 64 * this.iconZoom + 10;
      int palY = this.iconScreenY + 25;

      for (int i = 0; i < PALETTE.length; i++) {
         int px = palX;
         int py = palY + i * 18;
         context.fill(px, py, px + 16, py + 16, PALETTE[i]);
         if (PALETTE[i] == this.currentColor) {
            context.fill(px - 1, py - 1, px + 17, py, -256);
            context.fill(px - 1, py + 16, px + 17, py + 17, -256);
            context.fill(px - 1, py, px, py + 16, -256);
            context.fill(px + 16, py, px + 17, py + 16, -256);
         }
      }

      if (!this.statusMessage.isEmpty()) {
         context.drawCenteredString(this.font, this.statusMessage, this.width / 2, this.height - 65, this.statusColor);
      }
   }

   private void drawIconCanvas(GuiGraphics context, int mouseX, int mouseY) {
      for (int x = 0; x < 64; x++) {
         for (int y = 0; y < 64; y++) {
            int sx = this.iconScreenX + x * this.iconZoom;
            int sy = this.iconScreenY + y * this.iconZoom;
            int color = this.iconCanvas.getPixel(x, y);
            context.fill(sx, sy, sx + this.iconZoom, sy + this.iconZoom, color);
         }
      }

      if (this.showGrid) {
         for (int x = 0; x <= 64; x += 8) {
            int sx = this.iconScreenX + x * this.iconZoom;
            context.fill(sx, this.iconScreenY, sx + 1, this.iconScreenY + 64 * this.iconZoom, 1090519039);
         }

         for (int y = 0; y <= 64; y += 8) {
            int sy = this.iconScreenY + y * this.iconZoom;
            context.fill(this.iconScreenX, sy, this.iconScreenX + 64 * this.iconZoom, sy + 1, 1090519039);
         }
      }

      int endX = this.iconScreenX + 64 * this.iconZoom;
      int endY = this.iconScreenY + 64 * this.iconZoom;
      context.fill(this.iconScreenX - 1, this.iconScreenY - 1, endX + 1, this.iconScreenY, -1);
      context.fill(this.iconScreenX - 1, endY, endX + 1, endY + 1, -1);
      context.fill(this.iconScreenX - 1, this.iconScreenY, this.iconScreenX, endY, -1);
      context.fill(endX, this.iconScreenY, endX + 1, endY, -1);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      int palX = this.iconScreenX + 64 * this.iconZoom + 10;
      int palY = this.iconScreenY + 25;

      for (int i = 0; i < PALETTE.length; i++) {
         int py = palY + i * 18;
         if (mouseX >= palX && mouseX < palX + 16 && mouseY >= py && mouseY < py + 16) {
            this.currentColor = PALETTE[i];
            return true;
         }
      }

      if (this.handleIconCanvasClick(mouseX, mouseY)) {
         int px = (int)((mouseX - this.iconScreenX) / this.iconZoom);
         int py = (int)((mouseY - this.iconScreenY) / this.iconZoom);
         this.lastIconDragX = px;
         this.lastIconDragY = py;
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      this.lastIconDragX = -1;
      this.lastIconDragY = -1;
      return super.mouseReleased(mouseX, mouseY, button);
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      int px = (int)((mouseX - this.iconScreenX) / this.iconZoom);
      int py = (int)((mouseY - this.iconScreenY) / this.iconZoom);
      if (px >= 0 && px < 64 && py >= 0 && py < 64) {
         if (this.lastIconDragX < 0 || this.lastIconDragY < 0 || px == this.lastIconDragX && py == this.lastIconDragY) {
            this.iconCanvas.setPixel(px, py, this.currentColor);
         } else {
            this.drawIconLine(this.lastIconDragX, this.lastIconDragY, px, py);
         }

         this.lastIconDragX = px;
         this.lastIconDragY = py;
         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
      }
   }

   private void drawIconLine(int x0, int y0, int x1, int y1) {
      int dx = Math.abs(x1 - x0);
      int dy = Math.abs(y1 - y0);
      int sx = x0 < x1 ? 1 : -1;
      int sy = y0 < y1 ? 1 : -1;
      int err = dx - dy;
      int startX = x0;
      int startY = y0;

      while (true) {
         if ((x0 != startX || y0 != startY) && x0 >= 0 && x0 < 64 && y0 >= 0 && y0 < 64) {
            this.iconCanvas.setPixel(x0, y0, this.currentColor);
         }

         if (x0 == x1 && y0 == y1) {
            return;
         }

         int e2 = 2 * err;
         if (e2 > -dy) {
            err -= dy;
            x0 += sx;
         }

         if (e2 < dx) {
            err += dx;
            y0 += sy;
         }
      }
   }

   private boolean handleIconCanvasClick(double mouseX, double mouseY) {
      int px = (int)((mouseX - this.iconScreenX) / this.iconZoom);
      int py = (int)((mouseY - this.iconScreenY) / this.iconZoom);
      if (px >= 0 && px < 64 && py >= 0 && py < 64) {
         this.iconCanvas.setPixel(px, py, this.currentColor);
         return true;
      } else {
         return false;
      }
   }

   private void doExport() {
      String name = this.packNameInput.getValue().trim();
      if (name.isEmpty()) {
         this.statusMessage = "Please enter a pack name!";
         this.statusColor = 16733525;
      } else {
         String description = this.descriptionInput.getValue().trim();
         if (description.isEmpty()) {
            description = name;
         }

         String author = this.authorInput.getValue().trim();
         File result = ResourcePackExporter.export(name, description, author, this.iconCanvas.getPixels(), 64, 64);
         if (result != null) {
            this.statusMessage = "Exported to: " + result.getName();
            this.statusColor = 5635925;
         } else {
            this.statusMessage = "Export failed! No modified textures.";
            this.statusColor = 16733525;
         }
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 256) {
         this.minecraft.setScreen(this.parent);
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   private int getWindowHeight() {
      return Minecraft.getInstance().getWindow().getHeight();
   }

   private int getGuiScale() {
      return (int)Minecraft.getInstance().getWindow().getGuiScale();
   }

   public boolean isPauseScreen() {
      return false;
   }
}
