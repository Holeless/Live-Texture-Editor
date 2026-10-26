package com.zeeesea.textureeditor.screen;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.zeeesea.textureeditor.TextureEditorClient;
import com.zeeesea.textureeditor.editor.ColorHistory;
import com.zeeesea.textureeditor.editor.EditorTool;
import com.zeeesea.textureeditor.editor.Layer;
import com.zeeesea.textureeditor.editor.LayerStack;
import com.zeeesea.textureeditor.editor.PixelCanvas;
import com.zeeesea.textureeditor.helper.NotificationHelper;
import com.zeeesea.textureeditor.settings.ModSettings;
import com.zeeesea.textureeditor.texture.TextureManager;
import java.util.List;

public abstract class AbstractEditorScreen extends Screen {
   protected ResourceLocation spriteId;
   protected ResourceLocation textureId;
   protected PixelCanvas canvas;
   protected int[][] originalPixels;
   protected int blockTint = -1;
   protected boolean isTinted = false;
   protected EditorTool currentTool = EditorTool.PENCIL;
   protected int currentColor = -65536;
   protected int zoom = 12;
   protected boolean showGrid = true;
   protected int toolSize = 1;
   protected boolean previewingOriginal = false;
   protected int canvasBaseX;
   protected int canvasBaseY;
   protected int panOffsetX = 0;
   protected int panOffsetY = 0;
   protected int canvasScreenX;
   protected int canvasScreenY;
   protected boolean isPanning = false;
   private double panStartMouseX;
   private double panStartMouseY;
   private int panStartOffsetX;
   private int panStartOffsetY;
   private int lineStartX = -1;
   private int lineStartY = -1;
   private boolean lineFirstClick = false;
   private int lastDragPixelX = -1;
   private int lastDragPixelY = -1;
   protected AbstractEditorScreen.PanelType currentPanel = AbstractEditorScreen.PanelType.NONE;
   private float pickerHue = 0.0F;
   private float pickerSat = 1.0F;
   private float pickerVal = 1.0F;
   protected static final int[] PALETTE = new int[]{
      -16777216,
      -12566464,
      -8355712,
      -4144960,
      -1,
      -65536,
      -32768,
      -256,
      -8323328,
      -16711936,
      -16711808,
      -16711681,
      -16744193,
      -16776961,
      -8388353,
      -65281,
      -65408,
      -8388608,
      -8372224,
      -8355840,
      -12550144,
      -16744448,
      -16744384,
      -16744320,
      -16760704,
      -16777088,
      -12582784,
      -8388480,
      -8388544,
      -19533,
      -9805,
      -77,
      -4980813,
      -4980737
   };
   protected EditBox hexInput;

   protected AbstractEditorScreen(Component title) {
      super(title);
   }

   protected abstract void loadTexture();

   protected abstract void applyLive();

   protected abstract void resetCurrent();

   protected abstract String getEditorTitle();

   protected Screen getBackScreen() {
      return null;
   }

   protected int addExtraButtons(int toolY) {
      return toolY;
   }

   protected void renderExtra(GuiGraphics context, int mouseX, int mouseY) {
   }

   protected boolean handleExtraClick(double mx, double my, int btn) {
      return false;
   }

   protected boolean handleExtraRelease(double mx, double my, int btn) {
      return false;
   }

   protected boolean handleExtraDrag(double mx, double my, int btn, double dx, double dy) {
      return false;
   }

   protected boolean handleExtraScroll(double mx, double my, double ha, double va) {
      return false;
   }

   protected int getBackgroundColor() {
      return -15066578;
   }

   protected int getMaxZoom() {
      return 40;
   }

   protected int getMinZoom() {
      return 2;
   }

   protected int getZoomStep() {
      return 2;
   }

   protected String getResetCurrentLabel() {
      return "Reset";
   }

   protected boolean usesTint() {
      return this.isTinted;
   }

   protected int getGuiScale() {
      return (int)Minecraft.getInstance().getWindow().getGuiScale();
   }

   protected int getLeftSidebarWidth() {
      int scale = this.getGuiScale();
      if (scale >= 5) {
         return 70;
      } else {
         return scale >= 4 ? 85 : 110;
      }
   }

   protected int getRightSidebarWidth() {
      int scale = this.getGuiScale();
      if (scale >= 5) {
         return 80;
      } else {
         return scale >= 4 ? 95 : 120;
      }
   }

   protected int getToolButtonWidth() {
      int scale = this.getGuiScale();
      if (scale >= 5) {
         return 65;
      } else {
         return scale >= 4 ? 80 : 100;
      }
   }

   protected int getToolButtonHeight() {
      int scale = this.getGuiScale();
      return scale >= 4 ? 16 : 20;
   }

   /** Vertical gap between stacked buttons in the left column. */
   protected int getToolButtonGap() {
      return this.getGuiScale() >= 4 ? 2 : 4;
   }

   protected boolean showExtraButtons() {
      int scale = this.getGuiScale();
      if (scale >= 4) {
         return false;
      }

      int height = Minecraft.getInstance().getWindow().getHeight();
      return scale < 3 || height > 1080;
   }

   protected boolean showFaceButton() {
      return this.getGuiScale() <= 4;
   }

   private boolean showUndoRedo() {
      return this.getGuiScale() <= 4;
   }

   private boolean showColorHistory() {
      int scale = this.getGuiScale();
      int height = Minecraft.getInstance().getWindow().getHeight();
      return height <= 1080 && scale >= 3 ? false : scale <= 3;
   }

   protected int applyTint(int pixel) {
      if (!this.isTinted) {
         return pixel;
      }

      int a = pixel >> 24 & 0xFF;
      int r = pixel >> 16 & 0xFF;
      int g = pixel >> 8 & 0xFF;
      int b = pixel & 0xFF;
      int tr = this.blockTint >> 16 & 0xFF;
      int tg = this.blockTint >> 8 & 0xFF;
      int tb = this.blockTint & 0xFF;
      return a << 24 | r * tr / 255 << 16 | g * tg / 255 << 8 | b * tb / 255;
   }

   protected int removeTint(int color) {
      if (!this.isTinted) {
         return color;
      }

      int a = color >> 24 & 0xFF;
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      int tr = this.blockTint >> 16 & 0xFF;
      int tg = this.blockTint >> 8 & 0xFF;
      int tb = this.blockTint & 0xFF;
      r = tr > 0 ? Math.min(255, r * 255 / tr) : r;
      g = tg > 0 ? Math.min(255, g * 255 / tg) : g;
      b = tb > 0 ? Math.min(255, b * 255 / tb) : b;
      return a << 24 | r << 16 | g << 8 | b;
   }

   protected void init() {
      this.loadTexture();
      if (this.canvas == null) {
         this.canvas = new PixelCanvas(16, 16);
         this.originalPixels = new int[16][16];
      }

      int canvasPixelSize = Math.min(
         this.zoom,
         Math.min(
            (this.width - this.getLeftSidebarWidth() - this.getRightSidebarWidth() - 20) / this.canvas.getWidth(),
            (this.height - 80) / this.canvas.getHeight()
         )
      );
      if (canvasPixelSize < this.getMinZoom()) {
         canvasPixelSize = this.getMinZoom();
      }

      this.zoom = canvasPixelSize;
      int lsw = this.getLeftSidebarWidth();
      int rsw = this.getRightSidebarWidth();
      this.canvasBaseX = lsw + 10 + (this.width - lsw - rsw - 20 - this.canvas.getWidth() * this.zoom) / 2;
      this.canvasBaseY = 30 + (this.height - 80 - this.canvas.getHeight() * this.zoom) / 2;
      this.canvasScreenX = this.canvasBaseX + this.panOffsetX;
      this.canvasScreenY = this.canvasBaseY + this.panOffsetY;
      ModSettings settings = ModSettings.getInstance();
      this.currentTool = EditorTool.getToolByName(settings.defaultTool);
      ColorHistory.setMaxHistory(settings.colorHistorySize);
      this.showGrid = settings.gridOnByDefault;
      int toolY = 30;
      int tbw = this.getToolButtonWidth();
      int tbh = this.getToolButtonHeight();
      int gap = this.getToolButtonGap();

      for (EditorTool tool : EditorTool.values()) {
         EditorTool t = tool;
         String keyHint = settings.showToolHints ? this.getToolKeyHint(tool) : "";
         String label = keyHint.isEmpty() ? tool.getDisplayName() : tool.getDisplayName() + " (" + keyHint + ")";
         this.addRenderableWidget(
            Button.builder(Component.literal(label), btn -> this.currentTool = t).pos(5, toolY).size(tbw, tbh).build()
         );
         toolY += tbh + gap;
      }

      toolY += gap * 2;
      if (this.showUndoRedo()) {
         String undoHint = settings.showToolHints ? settings.getKeyName("undo") : "";
         String redoHint = settings.showToolHints ? settings.getKeyName("redo") : "";
         String undoText = undoHint.isEmpty() ? "Undo" : "Undo (" + undoHint + ")";
         String redoText = redoHint.isEmpty() ? "Redo" : "Redo (" + redoHint + ")";
         int halfBtnW = (tbw - 4) / 2;
         this.addRenderableWidget(
            Button.builder(Component.literal(undoText), btn -> this.canvas.undo())
               .pos(5, toolY)
               .size(halfBtnW, tbh)
               .build()
         );
         this.addRenderableWidget(
            Button.builder(Component.literal(redoText), btn -> this.canvas.redo())
               .pos(5 + halfBtnW + 4, toolY)
               .size(halfBtnW, tbh)
               .build()
         );
         toolY += tbh + gap;
      }

      if (this.showExtraButtons()) {
         String gridHint = settings.showToolHints ? settings.getKeyName("grid") : "";
         String gridText = gridHint.isEmpty() ? "Grid" : "Grid (" + gridHint + ")";
         this.addRenderableWidget(
            Button.builder(Component.literal(gridText), btn -> this.showGrid = !this.showGrid)
               .pos(5, toolY)
               .size(tbw, tbh)
               .build()
         );
         toolY += tbh + gap;
         int halfBtnW = (tbw - 4) / 2;
         this.addRenderableWidget(Button.builder(Component.literal("Zoom +"), btn -> {
            if (this.zoom < this.getMaxZoom()) {
               this.zoom = this.zoom + this.getZoomStep();
               this.recalcCanvasPos();
            }
         }).pos(5, toolY).size(halfBtnW, tbh).build());
         this.addRenderableWidget(Button.builder(Component.literal("Zoom -"), btn -> {
            if (this.zoom > this.getMinZoom()) {
               this.zoom = this.zoom - this.getZoomStep();
               this.recalcCanvasPos();
            }
         }).pos(5 + halfBtnW + 4, toolY).size(halfBtnW, tbh).build());
         toolY += tbh + 10;
      }

      this.addRenderableWidget(Button.builder(Component.literal("Size: " + this.toolSize + "px"), btn -> {
         this.toolSize = switch (this.toolSize) {
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 5;
            default -> 1;
         };
         btn.setMessage(Component.literal("Size: " + this.toolSize + "px"));
      }).pos(5, toolY).size(tbw, tbh).build());
      toolY += tbh + gap;
      toolY = this.addExtraButtons(toolY);
      int resetBtnW = rsw - 10;
      int resetX = this.width - rsw + 5;
      int resetBaseY = this.height - 100;
      this.addRenderableWidget(
         Button.builder(Component.literal(this.getResetCurrentLabel()), btn -> this.resetCurrent())
            .pos(resetX, resetBaseY)
            .size(resetBtnW, tbh)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("§cReset All"), btn -> this.doResetAll())
            .pos(resetX, resetBaseY + tbh + 4)
            .size(resetBtnW, tbh)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("§aApply Live"), btn -> this.applyLive())
            .pos(5, this.height - 4 - 3 * tbh - 2 * gap)
            .size(tbw, tbh)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(Component.literal("§6Export Pack"), btn -> Minecraft.getInstance().setScreen(new ExportScreen(this)))
            .pos(5, this.height - 4 - 2 * tbh - gap)
            .size(tbw, tbh)
            .build()
      );
      this.addRenderableWidget(Button.builder(Component.literal("§dBrowse"), btn -> {
         this.applyLive();
         Screen bs = this.getBackScreen();
         if (bs != null) {
            Minecraft.getInstance().setScreen(bs);
         } else {
            Minecraft.getInstance().setScreen(new BrowseScreen());
         }
      }).pos(5, this.height - 4 - tbh).size(tbw, tbh).build());
      this.addRenderableWidget(Button.builder(Component.literal("§cClose"), btn -> {
         if (settings.autoApplyLive) {
            this.applyLive();
         }

         this.onClose();
      }).pos(this.width - 65, 5).size(60, tbh).build());
      this.addRenderableWidget(
         Button.builder(
               Component.literal("§bPicker"),
               btn -> {
                  this.currentPanel = this.currentPanel == AbstractEditorScreen.PanelType.COLOR_PANEL
                     ? AbstractEditorScreen.PanelType.NONE
                     : AbstractEditorScreen.PanelType.COLOR_PANEL;
                  this.setColor(this.currentColor, false);
               }
            )
            .pos(this.width - 65, this.height - 26)
            .size(60, tbh)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(
               Component.literal("§eLayers"),
               btn -> this.currentPanel = this.currentPanel == AbstractEditorScreen.PanelType.LAYER_PANEL
                  ? AbstractEditorScreen.PanelType.NONE
                  : AbstractEditorScreen.PanelType.LAYER_PANEL
            )
            .pos(this.width - 130, this.height - 26)
            .size(60, tbh)
            .build()
      );
      int paletteEndY = this.getPaletteEndY() + 10;
      int hexW = Math.min(110, this.getRightSidebarWidth() - 30);
      this.hexInput = new EditBox(this.font, this.getPaletteX(), paletteEndY, hexW, 18, Component.literal("Hex"));
      this.hexInput.setMaxLength(9);
      this.hexInput.setValue(String.format("#%06X", new Object[]{this.currentColor & 16777215}));
      this.hexInput.setResponder(text -> {
         try {
            String hex = text.startsWith("#") ? text.substring(1) : text;
            if (hex.length() == 6 || hex.length() == 8) {
               this.currentColor = (int)Long.parseLong(hex.length() == 6 ? "FF" + hex : hex, 16);
            }
         } catch (NumberFormatException var3x) {
         }
      });
      this.addRenderableWidget(this.hexInput);
   }

   private String getToolKeyHint(EditorTool tool) {
      ModSettings s = ModSettings.getInstance();

      return switch (tool) {
         case PENCIL -> s.getKeyName("pencil");
         case BRUSH -> s.getKeyName("brush");
         case ERASER -> s.getKeyName("eraser");
         case FILL -> s.getKeyName("fill");
         case EYEDROPPER -> s.getKeyName("eyedropper");
         case LINE -> s.getKeyName("line");
      };
   }

   protected void recalcCanvasPos() {
      int lsw = this.getLeftSidebarWidth();
      int rsw = this.getRightSidebarWidth();
      this.canvasBaseX = lsw + 10 + (this.width - lsw - rsw - 20 - this.canvas.getWidth() * this.zoom) / 2;
      this.canvasBaseY = 30 + (this.height - 80 - this.canvas.getHeight() * this.zoom) / 2;
      this.canvasScreenX = this.canvasBaseX + this.panOffsetX;
      this.canvasScreenY = this.canvasBaseY + this.panOffsetY;
   }

   protected boolean isInUIRegion(double mx, double my) {
      return mx < this.getLeftSidebarWidth() || mx > this.width - this.getRightSidebarWidth() || my < 28.0;
   }

   protected boolean isInsideOpenPanel(double mx, double my) {
      if (this.currentPanel == AbstractEditorScreen.PanelType.COLOR_PANEL) {
         int lsw = this.getLeftSidebarWidth();
         int rsw = this.getRightSidebarWidth();
         int canvasAreaCenter = lsw + (this.width - lsw - rsw) / 2;
         int cpX = canvasAreaCenter - 80;
         int cpY = this.height - 90;
         if (mx >= cpX - 2 && mx <= cpX + 162 && my >= cpY - 14 && my <= cpY + 82) {
            return true;
         }
      }

      if (this.currentPanel == AbstractEditorScreen.PanelType.LAYER_PANEL && this.canvas != null) {
         LayerStack stack = this.canvas.getLayerStack();
         int panelW = 150;
         int rowH = 20;
         int panelH = 30 + stack.getLayerCount() * rowH + 30;
         int panelX = this.width / 2 - panelW / 2;
         int panelY = this.height - panelH - 30;
         if (mx >= panelX - 2 && mx <= panelX + panelW + 2 && my >= panelY - 2 && my <= panelY + panelH + 2) {
            return true;
         }
      }

      return false;
   }

   private void applyToolAtPixel(int px, int py) {
      float variation = ModSettings.getInstance().brushVariation;
      if (this.currentTool == EditorTool.PENCIL) {
         if (this.toolSize > 1) {
            this.canvas.drawPixelArea(px, py, this.toolSize, this.currentColor);
         } else {
            this.canvas.drawPixel(px, py, this.currentColor);
         }
      } else if (this.currentTool == EditorTool.BRUSH) {
         if (this.toolSize > 1) {
            this.canvas.drawBrushArea(px, py, this.toolSize, this.currentColor, variation);
         } else {
            this.canvas.drawBrushPixel(px, py, this.currentColor, variation);
         }
      } else if (this.currentTool == EditorTool.ERASER) {
         if (this.toolSize > 1) {
            this.canvas.erasePixelArea(px, py, this.toolSize);
         } else {
            this.canvas.erasePixel(px, py);
         }
      }
   }

   private void applyToolAlongLine(int x0, int y0, int x1, int y1) {
      int dx = Math.abs(x1 - x0);
      int dy = Math.abs(y1 - y0);
      int sx = x0 < x1 ? 1 : -1;
      int sy = y0 < y1 ? 1 : -1;
      int err = dx - dy;
      int startX = x0;
      int startY = y0;

      while (true) {
         if ((x0 != startX || y0 != startY) && x0 >= 0 && x0 < this.canvas.getWidth() && y0 >= 0 && y0 < this.canvas.getHeight()) {
            this.applyToolAtPixel(x0, y0);
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

   protected void doResetAll() {
      TextureManager.getInstance().clear();
      Minecraft.getInstance().reloadResourcePacks();
      if (this.originalPixels != null) {
         this.canvas.saveSnapshot();

         for (int x = 0; x < this.canvas.getWidth(); x++) {
            for (int y = 0; y < this.canvas.getHeight(); y++) {
               this.canvas.setPixel(x, y, this.originalPixels[x][y]);
            }
         }
      }
   }

   protected void setColor(int c, boolean addToHistory) {
      this.currentColor = c;
      if (addToHistory) {
         ColorHistory.getInstance().addColor(c);
      }

      if (this.currentPanel == AbstractEditorScreen.PanelType.COLOR_PANEL) {
         this.setColorPickerFromInt(c);
      }

      if (this.hexInput != null) {
         this.hexInput.setValue(String.format("#%06X", new Object[]{c & 16777215}));
      }
   }

   public void setColorPickerFromInt(int color) {
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      float[] hsv = new float[3];
      rgbToHsv(r, g, b, hsv);
      this.pickerHue = hsv[0];
      this.pickerSat = hsv[1];
      this.pickerVal = hsv[2];
   }

   private static void rgbToHsv(int r, int g, int b, float[] hsv) {
      float rf = r / 255.0F;
      float gf = g / 255.0F;
      float bf = b / 255.0F;
      float max = Math.max(rf, Math.max(gf, bf));
      float min = Math.min(rf, Math.min(gf, bf));
      float v = max;
      float d = max - min;
      float s = max == 0.0F ? 0.0F : d / max;
      float h;
      if (d == 0.0F) {
         h = 0.0F;
      } else if (max == rf) {
         h = ((gf - bf) / d + (gf < bf ? 6 : 0)) / 6.0F;
      } else if (max == gf) {
         h = ((bf - rf) / d + 2.0F) / 6.0F;
      } else {
         h = ((rf - gf) / d + 4.0F) / 6.0F;
      }

      hsv[0] = h;
      hsv[1] = s;
      hsv[2] = v;
   }

   public void renderBackground(GuiGraphics ctx, int mx, int my, float d) {
   }

   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      int bg = this.getBackgroundColor();
      int lsw = this.getLeftSidebarWidth();
      int rsw = this.getRightSidebarWidth();
      context.fill(0, 0, this.width, this.height, bg);
      this.drawCanvas(context, mouseX, mouseY);
      context.fill(0, 0, lsw, this.height, bg);
      context.fill(this.width - rsw, 0, this.width, this.height, bg);
      context.fill(lsw, 0, this.width - rsw, 28, bg);
      super.render(context, mouseX, mouseY, delta);
      context.drawString(this.font, this.getEditorTitle(), lsw + 5, 8, 16777215, true);
      context.drawString(
         this.font,
         "Tool: "
            + this.currentTool.getDisplayName()
            + " | Size: "
            + this.toolSize
            + "px"
            + (this.canvas != null ? "  |  " + this.canvas.getWidth() + "x" + this.canvas.getHeight() : ""),
         lsw + 5,
         20,
         13421772,
         false
      );
      this.drawPalette(context);
      int paletteX = this.getPaletteX();
      int paletteEndY = this.getPaletteEndY() + 10;
      int hexW = Math.min(110, this.getRightSidebarWidth() - 30);
      context.fill(paletteX + hexW + 4, paletteEndY, paletteX + hexW + 24, paletteEndY + 18, this.currentColor);
      this.drawRectOutline(context, paletteX + hexW + 4, paletteEndY, paletteX + hexW + 24, paletteEndY + 18, -1);
      if (this.showColorHistory()) {
         this.drawColorHistory(context);
      }

      if (this.currentPanel == AbstractEditorScreen.PanelType.COLOR_PANEL) {
         this.drawColorPicker(context);
      }

      if (this.currentPanel == AbstractEditorScreen.PanelType.LAYER_PANEL) {
         this.drawLayerPanel(context);
      }

      if (this.currentTool == EditorTool.LINE && this.lineFirstClick) {
         context.drawString(this.font, "Click endpoint...", this.canvasScreenX, this.canvasScreenY - 12, 16776960, false);
      }

      this.renderExtra(context, mouseX, mouseY);
   }

   private void drawCanvas(GuiGraphics ctx, int mx, int my) {
      if (this.canvas != null) {
         int w = this.canvas.getWidth();
         int h = this.canvas.getHeight();

         for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
               int sx = this.canvasScreenX + x * this.zoom;
               int sy = this.canvasScreenY + y * this.zoom;
               ctx.fill(sx, sy, sx + this.zoom, sy + this.zoom, (x + y) % 2 == 0 ? -8355712 : -6250336);
               int c;
               if (this.previewingOriginal && this.originalPixels != null && x < this.originalPixels.length && y < this.originalPixels[x].length) {
                  c = this.originalPixels[x][y];
               } else {
                  c = this.canvas.getPixel(x, y);
               }

               if ((c >> 24 & 0xFF) > 0) {
                  ctx.fill(sx, sy, sx + this.zoom, sy + this.zoom, this.usesTint() ? this.applyTint(c) : c);
               }
            }
         }

         if (this.showGrid && this.zoom >= 4) {
            for (int x = 0; x <= w; x++) {
               ctx.fill(
                  this.canvasScreenX + x * this.zoom,
                  this.canvasScreenY,
                  this.canvasScreenX + x * this.zoom + 1,
                  this.canvasScreenY + h * this.zoom,
                  1090519039
               );
            }

            for (int y = 0; y <= h; y++) {
               ctx.fill(
                  this.canvasScreenX,
                  this.canvasScreenY + y * this.zoom,
                  this.canvasScreenX + w * this.zoom,
                  this.canvasScreenY + y * this.zoom + 1,
                  1090519039
               );
            }
         }

         this.drawRectOutline(
            ctx, this.canvasScreenX - 1, this.canvasScreenY - 1, this.canvasScreenX + w * this.zoom + 1, this.canvasScreenY + h * this.zoom + 1, -1
         );
         int hx = (mx - this.canvasScreenX) / this.zoom;
         int hy = (my - this.canvasScreenY) / this.zoom;
         if (hx >= 0 && hx < w && hy >= 0 && hy < h) {
            int half = this.toolSize / 2;

            for (int dx = -half; dx < this.toolSize - half; dx++) {
               for (int dy = -half; dy < this.toolSize - half; dy++) {
                  int tx = hx + dx;
                  int ty = hy + dy;
                  if (tx >= 0 && tx < w && ty >= 0 && ty < h) {
                     int sx = this.canvasScreenX + tx * this.zoom;
                     int sy = this.canvasScreenY + ty * this.zoom;
                     this.drawRectOutline(ctx, sx, sy, sx + this.zoom, sy + this.zoom, -256);
                  }
               }
            }
         }

         if (this.previewingOriginal) {
            ctx.drawCenteredString(this.font, "§e§lPREVIEW (Original)", this.canvasScreenX + w * this.zoom / 2, this.canvasScreenY - 14, 16776960);
         }
      }
   }

   private void drawPalette(GuiGraphics ctx) {
      int cs = this.getPaletteCellSize();
      int cols = this.getPaletteColumns();
      int px0 = this.getPaletteX();
      int py0 = 30;

      for (int i = 0; i < PALETTE.length; i++) {
         int c = i % cols;
         int r = i / cols;
         int px = px0 + c * (cs + 2);
         int py = py0 + r * (cs + 2);
         ctx.fill(px, py, px + cs, py + cs, PALETTE[i]);
         if (PALETTE[i] == this.currentColor) {
            this.drawRectOutline(ctx, px - 1, py - 1, px + cs + 1, py + cs + 1, -256);
         } else {
            this.drawRectOutline(ctx, px, py, px + cs, py + cs, -13421773);
         }
      }
   }

   private void drawColorHistory(GuiGraphics ctx) {
      ColorHistory hist = ColorHistory.getInstance();
      if (hist.size() != 0) {
         int px0 = this.getPaletteX();
         int sy = this.getPaletteEndY() + 50;
         ctx.drawString(this.font, "History:", px0, sy, 10066329, false);
         sy += 12;
         int cols = this.getPaletteColumns();
         int cs = this.getPaletteCellSize();
         List<Integer> colors = hist.getColors();

         for (int i = 0; i < colors.size(); i++) {
            int c = i % cols;
            int r = i / cols;
            int px = px0 + c * (cs + 2);
            int py = sy + r * (cs + 2);
            ctx.fill(px, py, px + cs, py + cs, (Integer)colors.get(i));
            if ((Integer)colors.get(i) == this.currentColor) {
               this.drawRectOutline(ctx, px - 1, py - 1, px + cs + 1, py + cs + 1, -256);
            } else {
               this.drawRectOutline(ctx, px, py, px + cs, py + cs, -13421773);
            }
         }
      }
   }

   private void drawColorPicker(GuiGraphics ctx) {
      int lsw = this.getLeftSidebarWidth();
      int rsw = this.getRightSidebarWidth();
      int canvasAreaCenter = lsw + (this.width - lsw - rsw) / 2;
      int cpX = canvasAreaCenter - 80;
      int cpY = this.height - 90;
      int svW = 120;
      int svH = 80;
      ctx.fill(cpX - 2, cpY - 14, cpX + 162, cpY + 82, -14540220);
      this.drawRectOutline(ctx, cpX - 2, cpY - 14, cpX + 162, cpY + 82, -1);
      ctx.drawString(this.font, "Color Picker", cpX, cpY - 12, 16777215, false);

      for (int x = 0; x < svW; x++) {
         for (int y = 0; y < svH; y++) {
            float s = (float)x / (svW - 1);
            float v = 1.0F - (float)y / (svH - 1);
            ctx.fill(cpX + x, cpY + y, cpX + x + 1, cpY + y + 1, hsvToRgb(this.pickerHue, s, v));
         }
      }

      int scx = cpX + (int)(this.pickerSat * (svW - 1));
      int scy = cpY + (int)((1.0F - this.pickerVal) * (svH - 1));
      this.drawRectOutline(ctx, scx - 2, scy - 2, scx + 3, scy + 3, -1);
      int hueX = cpX + svW + 5;
      int hueW = 20;

      for (int y = 0; y < svH; y++) {
         float h = (float)y / (svH - 1);
         ctx.fill(hueX, cpY + y, hueX + hueW, cpY + y + 1, hsvToRgb(h, 1.0F, 1.0F));
      }

      int hcy = cpY + (int)(this.pickerHue * (svH - 1));
      ctx.fill(hueX - 1, hcy - 1, hueX + hueW + 1, hcy + 2, -1);
   }

   private void drawLayerPanel(GuiGraphics ctx) {
      if (this.canvas != null) {
         LayerStack stack = this.canvas.getLayerStack();
         int panelW = 150;
         int rowH = 20;
         int panelH = 30 + stack.getLayerCount() * rowH + 30;
         int panelX = this.width / 2 - panelW / 2;
         int panelY = this.height - panelH - 30;
         ctx.fill(panelX - 2, panelY - 2, panelX + panelW + 2, panelY + panelH + 2, -299752892);
         this.drawRectOutline(ctx, panelX - 2, panelY - 2, panelX + panelW + 2, panelY + panelH + 2, -1);
         ctx.drawString(this.font, "§eLayers", panelX + 4, panelY + 2, 16777215, false);
         int listY = panelY + 16;

         for (int i = stack.getLayerCount() - 1; i >= 0; i--) {
            Layer layer = (Layer)stack.getLayers().get(i);
            int rowY = listY + (stack.getLayerCount() - 1 - i) * rowH;
            int bg = i == stack.getActiveIndex() ? -12303224 : -13421739;
            ctx.fill(panelX, rowY, panelX + panelW, rowY + rowH - 1, bg);
            String eye = layer.isVisible() ? "§a●" : "§c○";
            ctx.drawString(this.font, eye, panelX + 3, rowY + 5, 16777215, false);
            String name = layer.getName();
            if (name.length() > 14) {
               name = name.substring(0, 14) + "..";
            }

            ctx.drawString(this.font, name, panelX + 16, rowY + 5, 14540253, false);
            if (i == stack.getActiveIndex()) {
               ctx.drawString(this.font, "▶", panelX + panelW - 12, rowY + 5, 16776960, false);
            }
         }

         int btnY = listY + stack.getLayerCount() * rowH + 4;
         ctx.fill(panelX, btnY, panelX + 34, btnY + 18, -13413069);
         ctx.drawString(this.font, "+ Add", panelX + 3, btnY + 4, 11206570, false);
         ctx.fill(panelX + 38, btnY, panelX + 76, btnY + 18, -11193549);
         ctx.drawString(this.font, "- Del", panelX + 41, btnY + 4, 16755370, false);
         ctx.fill(panelX + 80, btnY, panelX + 110, btnY + 18, -13421739);
         ctx.drawString(this.font, "▲ Up", panelX + 83, btnY + 4, 11184895, false);
         ctx.fill(panelX + 114, btnY, panelX + panelW, btnY + 18, -13421739);
         ctx.drawString(this.font, "▼ Dn", panelX + 117, btnY + 4, 11184895, false);
      }
   }

   public boolean mouseScrolled(double mx, double my, double ha, double va) {
      if (this.handleExtraScroll(mx, my, ha, va)) {
         return true;
      }

      int oldZoom = this.zoom;
      if (va > 0.0 && this.zoom < this.getMaxZoom()) {
         this.zoom = this.zoom + this.getZoomStep();
      } else if (va < 0.0 && this.zoom > this.getMinZoom()) {
         this.zoom = this.zoom - this.getZoomStep();
      }

      if (this.zoom != oldZoom) {
         double rx = (mx - this.canvasScreenX) / (this.canvas.getWidth() * oldZoom);
         double ry = (my - this.canvasScreenY) / (this.canvas.getHeight() * oldZoom);
         this.recalcCanvasPos();
         this.canvasScreenX = (int)(mx - rx * this.canvas.getWidth() * this.zoom);
         this.canvasScreenY = (int)(my - ry * this.canvas.getHeight() * this.zoom);
         this.panOffsetX = this.canvasScreenX - this.canvasBaseX;
         this.panOffsetY = this.canvasScreenY - this.canvasBaseY;
      }

      return true;
   }

   public boolean mouseClicked(double mx, double my, int btn) {
      if (super.mouseClicked(mx, my, btn)) {
         return true;
      }

      if (this.handleExtraClick(mx, my, btn)) {
         return true;
      }

      if (this.currentPanel == AbstractEditorScreen.PanelType.COLOR_PANEL && btn == 0 && this.handlePickerClick(mx, my)) {
         return true;
      }

      if (this.currentPanel == AbstractEditorScreen.PanelType.LAYER_PANEL && btn == 0 && this.handleLayerPanelClick(mx, my)) {
         return true;
      }

      if (this.currentPanel != AbstractEditorScreen.PanelType.NONE && btn == 0 && !this.isInsideOpenPanel(mx, my)) {
         this.currentPanel = AbstractEditorScreen.PanelType.NONE;
         return true;
      }

      if (btn == 0 && this.handleHistoryClick(mx, my)) {
         return true;
      }

      if (btn == 1) {
         this.isPanning = true;
         this.panStartMouseX = mx;
         this.panStartMouseY = my;
         this.panStartOffsetX = this.panOffsetX;
         this.panStartOffsetY = this.panOffsetY;
         return true;
      }

      if (btn == 0 && this.handlePaletteClick(mx, my)) {
         return true;
      }

      if (this.isInUIRegion(mx, my)) {
         return false;
      }

      if (btn == 0 && this.canvas != null) {
         int px = (int)((mx - this.canvasScreenX) / this.zoom);
         int py = (int)((my - this.canvasScreenY) / this.zoom);
         if (px >= 0 && px < this.canvas.getWidth() && py >= 0 && py < this.canvas.getHeight()) {
            this.handleCanvasClick(px, py, btn);
            this.lastDragPixelX = px;
            this.lastDragPixelY = py;
            return true;
         }
      }

      return false;
   }

   public boolean mouseReleased(double mx, double my, int btn) {
      if (this.handleExtraRelease(mx, my, btn)) {
         return true;
      }

      if (btn == 1) {
         this.isPanning = false;
         return true;
      }

      if (btn == 0) {
         this.lastDragPixelX = -1;
         this.lastDragPixelY = -1;
      }

      return super.mouseReleased(mx, my, btn);
   }

   public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
      if (btn == 1 && this.isPanning) {
         this.panOffsetX = this.panStartOffsetX + (int)(mx - this.panStartMouseX);
         this.panOffsetY = this.panStartOffsetY + (int)(my - this.panStartMouseY);
         this.canvasScreenX = this.canvasBaseX + this.panOffsetX;
         this.canvasScreenY = this.canvasBaseY + this.panOffsetY;
         return true;
      }

      if (this.handleExtraDrag(mx, my, btn, dx, dy)) {
         return true;
      }

      if (this.currentPanel == AbstractEditorScreen.PanelType.COLOR_PANEL && btn == 0 && this.handlePickerClick(mx, my)) {
         return true;
      }

      if (this.currentPanel != AbstractEditorScreen.PanelType.NONE && this.isInsideOpenPanel(mx, my)) {
         return true;
      }

      if (this.isInUIRegion(mx, my)) {
         return super.mouseDragged(mx, my, btn, dx, dy);
      }

      if (this.canvas != null && (this.currentTool == EditorTool.PENCIL || this.currentTool == EditorTool.BRUSH || this.currentTool == EditorTool.ERASER)) {
         int px = (int)((mx - this.canvasScreenX) / this.zoom);
         int py = (int)((my - this.canvasScreenY) / this.zoom);
         if (px >= 0 && px < this.canvas.getWidth() && py >= 0 && py < this.canvas.getHeight()) {
            if (this.lastDragPixelX < 0 || this.lastDragPixelY < 0 || px == this.lastDragPixelX && py == this.lastDragPixelY) {
               this.applyToolAtPixel(px, py);
            } else {
               this.applyToolAlongLine(this.lastDragPixelX, this.lastDragPixelY, px, py);
            }

            this.lastDragPixelX = px;
            this.lastDragPixelY = py;
            return true;
         }
      }

      return super.mouseDragged(mx, my, btn, dx, dy);
   }

   public boolean keyPressed(int kc, int sc, int m) {
      if (this.hexInput != null && this.hexInput.isFocused()) {
         return super.keyPressed(kc, sc, m);
      }

      KeyMapping previewKey = TextureEditorClient.getPreviewOriginalKey();
      if (previewKey != null && previewKey.matches(kc, sc)) {
         this.previewingOriginal = true;
         return true;
      }

      ModSettings s = ModSettings.getInstance();
      if (kc == s.getKeybind("undo")) {
         this.canvas.undo();
         return true;
      }

      if (kc == s.getKeybind("redo")) {
         this.canvas.redo();
         return true;
      }

      if (kc == s.getKeybind("grid")) {
         this.showGrid = !this.showGrid;
         return true;
      }

      if (kc == s.getKeybind("pencil")) {
         this.currentTool = EditorTool.PENCIL;
         return true;
      }

      if (kc == s.getKeybind("eraser")) {
         this.currentTool = EditorTool.ERASER;
         return true;
      }

      if (kc == s.getKeybind("fill")) {
         this.currentTool = EditorTool.FILL;
         return true;
      }

      if (kc == s.getKeybind("eyedropper")) {
         this.currentTool = EditorTool.EYEDROPPER;
         return true;
      }

      if (kc == s.getKeybind("line")) {
         this.currentTool = EditorTool.LINE;
         return true;
      }

      if (kc == s.getKeybind("brush")) {
         this.currentTool = EditorTool.BRUSH;
         return true;
      }

      if (kc == s.getKeybind("browse")) {
         this.applyLive();
         Minecraft.getInstance().setScreen(new BrowseScreen());
         return true;
      }

      KeyMapping openKey = TextureEditorClient.getOpenEditorKey();
      if (openKey != null && openKey.matches(kc, sc)) {
         if (s.autoApplyLive) {
            this.applyLive();
         }

         this.onClose();
         return true;
      } else if (kc == 256) {
         if (s.autoApplyLive) {
            this.applyLive();
         }

         this.onClose();
         return true;
      } else {
         return super.keyPressed(kc, sc, m);
      }
   }

   public boolean keyReleased(int kc, int sc, int m) {
      KeyMapping previewKey = TextureEditorClient.getPreviewOriginalKey();
      if (previewKey != null && previewKey.matches(kc, sc)) {
         this.previewingOriginal = false;
         return true;
      } else {
         return super.keyReleased(kc, sc, m);
      }
   }

   private boolean handlePickerClick(double mx, double my) {
      int lsw = this.getLeftSidebarWidth();
      int rsw = this.getRightSidebarWidth();
      int canvasAreaCenter = lsw + (this.width - lsw - rsw) / 2;
      int cpX = canvasAreaCenter - 80;
      int cpY = this.height - 90;
      int svW = 120;
      int svH = 80;
      int hueX = cpX + svW + 5;
      int hueW = 20;
      if (mx >= cpX && mx < cpX + svW && my >= cpY && my < cpY + svH) {
         this.pickerSat = Math.max(0.0F, Math.min(1.0F, (float)(mx - cpX) / (svW - 1)));
         this.pickerVal = Math.max(0.0F, Math.min(1.0F, 1.0F - (float)(my - cpY) / (svH - 1)));
         this.setColor(hsvToRgb(this.pickerHue, this.pickerSat, this.pickerVal), false);
         return true;
      } else if (mx >= hueX && mx < hueX + hueW && my >= cpY && my < cpY + svH) {
         this.pickerHue = Math.max(0.0F, Math.min(1.0F, (float)(my - cpY) / (svH - 1)));
         this.setColor(hsvToRgb(this.pickerHue, this.pickerSat, this.pickerVal), false);
         return true;
      } else {
         return false;
      }
   }

   private boolean handleHistoryClick(double mx, double my) {
      if (!this.showColorHistory()) {
         return false;
      }

      ColorHistory hist = ColorHistory.getInstance();
      if (hist.size() == 0) {
         return false;
      }

      int px0 = this.getPaletteX();
      int sy = this.getPaletteEndY() + 50 + 12;
      int cols = this.getPaletteColumns();
      int cs = this.getPaletteCellSize();
      List<Integer> colors = hist.getColors();

      for (int i = 0; i < colors.size(); i++) {
         int c = i % cols;
         int r = i / cols;
         int px = px0 + c * (cs + 2);
         int py = sy + r * (cs + 2);
         if (mx >= px && mx < px + cs && my >= py && my < py + cs) {
            this.currentColor = (Integer)colors.get(i);
            if (this.hexInput != null) {
               this.hexInput.setValue(String.format("#%06X", new Object[]{this.currentColor & 16777215}));
            }

            return true;
         }
      }

      return false;
   }

   private boolean handleLayerPanelClick(double mx, double my) {
      if (this.currentPanel == AbstractEditorScreen.PanelType.LAYER_PANEL && this.canvas != null) {
         LayerStack stack = this.canvas.getLayerStack();
         int panelW = 150;
         int rowH = 20;
         int panelH = 30 + stack.getLayerCount() * rowH + 30;
         int panelX = this.width / 2 - panelW / 2;
         int panelY = this.height - panelH - 30;
         if (!(mx < panelX - 2) && !(mx > panelX + panelW + 2) && !(my < panelY - 2) && !(my > panelY + panelH + 2)) {
            int listY = panelY + 16;

            for (int i = stack.getLayerCount() - 1; i >= 0; i--) {
               int rowY = listY + (stack.getLayerCount() - 1 - i) * rowH;
               if (my >= rowY && my < rowY + rowH - 1 && mx >= panelX && mx < panelX + panelW) {
                  if (mx < panelX + 14) {
                     ((Layer)stack.getLayers().get(i)).setVisible(!((Layer)stack.getLayers().get(i)).isVisible());
                     this.canvas.invalidateCache();
                  } else {
                     stack.setActiveIndex(i);
                  }

                  return true;
               }
            }

            int btnY = listY + stack.getLayerCount() * rowH + 4;
            if (my >= btnY && my < btnY + 18) {
               if (mx >= panelX && mx < panelX + 34) {
                  stack.addLayerAbove("Layer " + (stack.getLayerCount() - 1));
                  this.canvas.invalidateCache();
                  return true;
               }

               if (mx >= panelX + 38 && mx < panelX + 76) {
                  stack.removeLayer(stack.getActiveIndex());
                  this.canvas.invalidateCache();
                  return true;
               }

               if (mx >= panelX + 80 && mx < panelX + 110) {
                  int idx = stack.getActiveIndex();
                  if (idx < stack.getLayerCount() - 1) {
                     stack.moveLayerDown(idx);
                     this.canvas.invalidateCache();
                  }

                  return true;
               }

               if (mx >= panelX + 114 && mx < panelX + panelW) {
                  int idx = stack.getActiveIndex();
                  if (idx > 0) {
                     stack.moveLayerUp(idx);
                     this.canvas.invalidateCache();
                  }

                  return true;
               }
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   protected boolean handlePaletteClick(double mx, double my) {
      int cs = this.getPaletteCellSize();
      int cols = this.getPaletteColumns();
      int px0 = this.getPaletteX();
      int py0 = 30;

      for (int i = 0; i < PALETTE.length; i++) {
         int c = i % cols;
         int r = i / cols;
         int px = px0 + c * (cs + 2);
         int py = py0 + r * (cs + 2);
         if (mx >= px && mx < px + cs && my >= py && my < py + cs) {
            this.setColor(PALETTE[i], false);
            return true;
         }
      }

      return false;
   }

   protected void handleCanvasClick(int px, int py, int btn) {
      int storeColor = this.currentColor;
      float variation = ModSettings.getInstance().brushVariation;
      switch (this.currentTool) {
         case PENCIL:
            this.canvas.saveSnapshot();
            if (this.toolSize > 1) {
               this.canvas.drawPixelArea(px, py, this.toolSize, btn == 1 ? 0 : storeColor);
            } else {
               this.canvas.drawPixel(px, py, btn == 1 ? 0 : storeColor);
            }

            this.setColor(this.currentColor, true);
            break;
         case BRUSH:
            this.canvas.saveSnapshot();
            if (this.toolSize > 1) {
               this.canvas.drawBrushArea(px, py, this.toolSize, storeColor, variation);
            } else {
               this.canvas.drawBrushPixel(px, py, storeColor, variation);
            }

            this.setColor(this.currentColor, true);
            break;
         case ERASER:
            this.canvas.saveSnapshot();
            if (this.toolSize > 1) {
               this.canvas.erasePixelArea(px, py, this.toolSize);
            } else {
               this.canvas.erasePixel(px, py);
            }
            break;
         case FILL:
            this.canvas.saveSnapshot();
            this.canvas.floodFill(px, py, storeColor);
            this.setColor(this.currentColor, true);
            break;
         case EYEDROPPER:
            int raw = this.canvas.pickColorComposited(px, py);
            if (raw == 0) {
               NotificationHelper.addToast(SystemToast.SystemToastId.PACK_LOAD_FAILURE, "Layer is completely empty!");
               return;
            }

            this.currentColor = this.usesTint() ? this.applyTint(raw) : raw;
            this.setColor(this.currentColor, false);
            if (this.hexInput != null) {
               this.hexInput.setValue(String.format("#%08X", new Object[]{this.currentColor}));
            }
            break;
         case LINE:
            if (!this.lineFirstClick) {
               this.lineStartX = px;
               this.lineStartY = py;
               this.lineFirstClick = true;
            } else {
               this.canvas.saveSnapshot();
               this.canvas.drawLine(this.lineStartX, this.lineStartY, px, py, storeColor);
               this.lineFirstClick = false;
               this.setColor(this.currentColor, true);
            }
      }
   }

   protected void drawRectOutline(GuiGraphics ctx, int x1, int y1, int x2, int y2, int c) {
      ctx.fill(x1, y1, x2, y1 + 1, c);
      ctx.fill(x1, y2 - 1, x2, y2, c);
      ctx.fill(x1, y1, x1 + 1, y2, c);
      ctx.fill(x2 - 1, y1, x2, y2, c);
   }

   protected static int hsvToRgb(float h, float s, float v) {
      int i = (int)(h * 6.0F) % 6;
      float f = h * 6.0F - (int)(h * 6.0F);
      int p = (int)(255.0F * v * (1.0F - s));
      int q = (int)(255.0F * v * (1.0F - f * s));
      int t = (int)(255.0F * v * (1.0F - (1.0F - f) * s));
      int vi = (int)(255.0F * v);

      return switch (i) {
         case 0 -> 0xFF000000 | vi << 16 | t << 8 | p;
         case 1 -> 0xFF000000 | q << 16 | vi << 8 | p;
         case 2 -> 0xFF000000 | p << 16 | vi << 8 | t;
         case 3 -> 0xFF000000 | p << 16 | q << 8 | vi;
         case 4 -> 0xFF000000 | t << 16 | p << 8 | vi;
         default -> 0xFF000000 | vi << 16 | p << 8 | q;
      };
   }

   protected static int[][] copyPixels(int[][] src, int w, int h) {
      int[][] copy = new int[w][h];

      for (int x = 0; x < w; x++) {
         System.arraycopy(src[x], 0, copy[x], 0, h);
      }

      return copy;
   }

   private int getPaletteX() {
      return this.width - this.getRightSidebarWidth() + 5;
   }

   private int getPaletteColumns() {
      int scale = this.getGuiScale();
      if (scale >= 5) {
         return 5;
      } else {
         return scale >= 4 ? 5 : 5;
      }
   }

   private int getPaletteCellSize() {
      int scale = this.getGuiScale();
      if (scale >= 5) {
         return 10;
      } else {
         return scale >= 4 ? 12 : 20;
      }
   }

   private int getPaletteEndY() {
      int cs = this.getPaletteCellSize();
      int cols = this.getPaletteColumns();
      return 30 + (PALETTE.length + cols - 1) / cols * (cs + 2);
   }

   public boolean isPauseScreen() {
      return false;
   }

   protected enum PanelType {
      NONE,
      COLOR_PANEL,
      LAYER_PANEL;
   }
}
