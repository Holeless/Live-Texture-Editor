package com.zeeesea.textureeditor.editor;

import net.minecraft.client.gui.components.toasts.SystemToast;

import com.zeeesea.textureeditor.helper.NotificationHelper;
import com.zeeesea.textureeditor.settings.ModSettings;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

public class PixelCanvas {
   private final int width;
   private final int height;
   private LayerStack layerStack;
   private final Deque<PixelCanvas.LayerSnapshot> undoStack = new ArrayDeque();
   private final Deque<PixelCanvas.LayerSnapshot> redoStack = new ArrayDeque();
   private static int MAX_UNDO = 50;
   private boolean dirty = false;
   private int[][] flattenedCache;
   private boolean cacheValid = false;
   private static long lastLayerEmptyToast = 0L;
   private static final long TOAST_COOLDOWN_MS = 5000L;

   public PixelCanvas(int width, int height) {
      this.width = width;
      this.height = height;
      this.layerStack = new LayerStack(width, height);
      MAX_UNDO = ModSettings.getInstance().maxUndoSteps;
   }

   public PixelCanvas(int width, int height, int[][] initialPixels) {
      this.width = width;
      this.height = height;
      this.layerStack = new LayerStack(width, height, initialPixels);
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public boolean isDirty() {
      return this.dirty;
   }

   public void setDirty(boolean dirty) {
      this.dirty = dirty;
   }

   public LayerStack getLayerStack() {
      return this.layerStack;
   }

   public void setLayerStack(LayerStack layerStack) {
      this.layerStack = layerStack;
   }

   public int getPixel(int x, int y) {
      if (x >= 0 && x < this.width && y >= 0 && y < this.height) {
         if (!this.cacheValid) {
            this.flattenedCache = this.layerStack.getCompositeLayer().getPixels();
            this.cacheValid = true;
         }

         return this.flattenedCache[x][y];
      } else {
         return 0;
      }
   }

   public void setPixel(int x, int y, int color) {
      Layer active = this.layerStack.getActiveLayer();
      if (active != null && x >= 0 && x < this.width && y >= 0 && y < this.height) {
         active.setPixel(x, y, color);
         this.dirty = true;
         this.cacheValid = false;
      }
   }

   public int[][] getPixels() {
      if (!this.cacheValid) {
         this.flattenedCache = this.layerStack.flatten();
         this.cacheValid = true;
      }

      return this.flattenedCache;
   }

   public void saveSnapshot() {
      Layer active = this.layerStack.getActiveLayer();
      if (active != null) {
         this.undoStack.push(new PixelCanvas.LayerSnapshot(this.layerStack.getActiveIndex(), active.copyPixelsOut()));
         if (this.undoStack.size() > MAX_UNDO) {
            ((ArrayDeque)this.undoStack).removeLast();
         }

         this.redoStack.clear();
      }
   }

   public boolean canUndo() {
      return !this.undoStack.isEmpty();
   }

   public boolean canRedo() {
      return !this.redoStack.isEmpty();
   }

   public void undo() {
      if (!this.undoStack.isEmpty()) {
         PixelCanvas.LayerSnapshot snapshot = (PixelCanvas.LayerSnapshot)this.undoStack.pop();
         int idx = snapshot.layerIndex();
         if (idx >= 0 && idx < this.layerStack.getLayerCount()) {
            Layer layer = (Layer)this.layerStack.getLayers().get(idx);
            this.redoStack.push(new PixelCanvas.LayerSnapshot(idx, layer.copyPixelsOut()));
            layer.setPixels(snapshot.pixels());
         }

         this.dirty = true;
         this.cacheValid = false;
      }
   }

   public void redo() {
      if (!this.redoStack.isEmpty()) {
         PixelCanvas.LayerSnapshot snapshot = (PixelCanvas.LayerSnapshot)this.redoStack.pop();
         int idx = snapshot.layerIndex();
         if (idx >= 0 && idx < this.layerStack.getLayerCount()) {
            Layer layer = (Layer)this.layerStack.getLayers().get(idx);
            this.undoStack.push(new PixelCanvas.LayerSnapshot(idx, layer.copyPixelsOut()));
            layer.setPixels(snapshot.pixels());
         }

         this.dirty = true;
         this.cacheValid = false;
      }
   }

   public void drawPixel(int x, int y, int color) {
      this.setPixel(x, y, color);
   }

   public void drawBrushPixel(int x, int y, int color, float variation) {
      int a = color >> 24 & 0xFF;
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      float offset = (ThreadLocalRandom.current().nextFloat() * 2.0F - 1.0F) * variation;
      r = clamp((int)(r + r * offset), 0, 255);
      g = clamp((int)(g + g * offset), 0, 255);
      b = clamp((int)(b + b * offset), 0, 255);
      this.setPixel(x, y, a << 24 | r << 16 | g << 8 | b);
   }

   public void drawPixelArea(int cx, int cy, int size, int color) {
      int half = size / 2;

      for (int dx = -half; dx < size - half; dx++) {
         for (int dy = -half; dy < size - half; dy++) {
            this.drawPixel(cx + dx, cy + dy, color);
         }
      }
   }

   public void erasePixelArea(int cx, int cy, int size) {
      int half = size / 2;

      for (int dx = -half; dx < size - half; dx++) {
         for (int dy = -half; dy < size - half; dy++) {
            int x = cx + dx;
            int y = cy + dy;
            if (x >= 0 && x < this.width && y >= 0 && y < this.height) {
               this.setPixel(x, y, 0);
            }
         }
      }
   }

   public void drawBrushArea(int cx, int cy, int size, int color, float variation) {
      int half = size / 2;

      for (int dx = -half; dx < size - half; dx++) {
         for (int dy = -half; dy < size - half; dy++) {
            this.drawBrushPixel(cx + dx, cy + dy, color, variation);
         }
      }
   }

   private static int clamp(int val, int min, int max) {
      return Math.max(min, Math.min(max, val));
   }

   public void erasePixel(int x, int y) {
      Layer active = this.layerStack.getActiveLayer();
      if (active != null) {
         if (active.isEmpty()) {
            NotificationHelper.addToast(SystemToast.SystemToastId.PACK_LOAD_FAILURE, "Layer is already completely empty!");
         } else {
            this.setPixel(x, y, 0);
         }
      }
   }

   public void floodFill(int x, int y, int color) {
      Layer active = this.layerStack.getActiveLayer();
      if (active != null) {
         if (x >= 0 && x < this.width && y >= 0 && y < this.height) {
            int targetColor = active.getPixel(x, y);
            if (targetColor != color) {
               Queue<int[]> queue = new LinkedList();
               queue.add(new int[]{x, y});
               boolean[][] visited = new boolean[this.width][this.height];

               while (!queue.isEmpty()) {
                  int[] pos = (int[])queue.poll();
                  int px = pos[0];
                  int py = pos[1];
                  if (px >= 0 && px < this.width && py >= 0 && py < this.height && !visited[px][py] && active.getPixel(px, py) == targetColor) {
                     visited[px][py] = true;
                     active.setPixel(px, py, color);
                     queue.add(new int[]{px + 1, py});
                     queue.add(new int[]{px - 1, py});
                     queue.add(new int[]{px, py + 1});
                     queue.add(new int[]{px, py - 1});
                  }
               }

               this.dirty = true;
               this.cacheValid = false;
            }
         }
      }
   }

   public void drawLine(int x0, int y0, int x1, int y1, int color) {
      int dx = Math.abs(x1 - x0);
      int dy = Math.abs(y1 - y0);
      int sx = x0 < x1 ? 1 : -1;
      int sy = y0 < y1 ? 1 : -1;
      int err = dx - dy;

      while (true) {
         this.setPixel(x0, y0, color);
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

   public int pickColorOnActiveLayer(int x, int y) {
      Layer active = this.layerStack.getActiveLayer();
      return active == null ? this.getPixel(x, y) : active.getPixel(x, y);
   }

   public int pickColorComposited(int x, int y) {
      return this.getPixel(x, y);
   }

   public void invalidateCache() {
      this.cacheValid = false;
   }

   private record LayerSnapshot(int layerIndex, int[][] pixels) {
   }
}
