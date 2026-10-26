package com.zeeesea.textureeditor.editor;


import java.util.ArrayList;
import java.util.List;

public class LayerStack {
   private final int width;
   private final int height;
   private final List<Layer> layers = new ArrayList();
   private int activeIndex = 1;

   public LayerStack(int width, int height) {
      this.width = width;
      this.height = height;
      this.layers.add(new Layer(width, height, "Base"));
      this.layers.add(new Layer(width, height, "Layer 0"));
   }

   public LayerStack(int width, int height, int[][] basePixels) {
      this.width = width;
      this.height = height;
      this.layers.add(new Layer(width, height, "Base", basePixels));
      this.layers.add(new Layer(width, height, "Layer 0"));
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public List<Layer> getLayers() {
      return this.layers;
   }

   public int getLayerCount() {
      return this.layers.size();
   }

   public int getActiveIndex() {
      return this.activeIndex;
   }

   public void setActiveIndex(int index) {
      if (index >= 0 && index < this.layers.size()) {
         this.activeIndex = index;
      }
   }

   public Layer getActiveLayer() {
      return this.activeIndex >= 0 && this.activeIndex < this.layers.size() ? (Layer)this.layers.get(this.activeIndex) : null;
   }

   public void addLayer(String name) {
      Layer layer = new Layer(this.width, this.height, name);
      this.layers.add(layer);
      this.activeIndex = this.layers.size() - 1;
   }

   public void addLayerAbove(String name) {
      Layer layer = new Layer(this.width, this.height, name);
      int insertAt = Math.min(this.activeIndex + 1, this.layers.size());
      this.layers.add(insertAt, layer);
      this.activeIndex = insertAt;
   }

   public void removeLayer(int index) {
      if (this.layers.size() > 1) {
         if (index >= 0 && index < this.layers.size()) {
            this.layers.remove(index);
            if (this.activeIndex >= this.layers.size()) {
               this.activeIndex = this.layers.size() - 1;
            }
         }
      }
   }

   public void moveLayerUp(int index) {
      if (index > 0 && index < this.layers.size()) {
         Layer l = (Layer)this.layers.remove(index);
         this.layers.add(index - 1, l);
         if (this.activeIndex == index) {
            this.activeIndex = index - 1;
         } else if (this.activeIndex == index - 1) {
            this.activeIndex = index;
         }
      }
   }

   public void moveLayerDown(int index) {
      if (index >= 0 && index < this.layers.size() - 1) {
         Layer l = (Layer)this.layers.remove(index);
         this.layers.add(index + 1, l);
         if (this.activeIndex == index) {
            this.activeIndex = index + 1;
         } else if (this.activeIndex == index + 1) {
            this.activeIndex = index;
         }
      }
   }

   public int[][] flatten() {
      int[][] result = new int[this.width][this.height];

      for (int x = 0; x < this.width; x++) {
         for (int y = 0; y < this.height; y++) {
            result[x][y] = 0;
         }
      }

      for (Layer layer : this.layers) {
         if (layer.isVisible()) {
            for (int x = 0; x < this.width; x++) {
               for (int y = 0; y < this.height; y++) {
                  int src = layer.getPixel(x, y);
                  int dst = result[x][y];
                  result[x][y] = alphaBlend(src, dst);
               }
            }
         }
      }

      return result;
   }

   public Layer getCompositeLayer() {
      int[][] composite = new int[this.width][this.height];

      for (int x = 0; x < this.width; x++) {
         for (int y = 0; y < this.height; y++) {
            composite[x][y] = 0;
         }
      }

      for (int i = 0; i < this.layers.size(); i++) {
         Layer layer = (Layer)this.layers.get(i);
         if (layer.isVisible()) {
            for (int x = 0; x < this.width; x++) {
               for (int y = 0; y < this.height; y++) {
                  int color = layer.getPixel(x, y);
                  if ((color & 0xFF000000) != 0) {
                     composite[x][y] = color;
                  }
               }
            }
         }
      }

      return new Layer(this.width, this.height, "Composite", composite);
   }

   private static int alphaBlend(int src, int dst) {
      int srcA = src >> 24 & 0xFF;
      if (srcA == 0) {
         return dst;
      }

      if (srcA == 255) {
         return src;
      }

      int srcR = src >> 16 & 0xFF;
      int srcG = src >> 8 & 0xFF;
      int srcB = src & 0xFF;
      int dstA = dst >> 24 & 0xFF;
      int dstR = dst >> 16 & 0xFF;
      int dstG = dst >> 8 & 0xFF;
      int dstB = dst & 0xFF;
      int outA = srcA + dstA * (255 - srcA) / 255;
      if (outA == 0) {
         return 0;
      }

      int outR = (srcR * srcA + dstR * dstA * (255 - srcA) / 255) / outA;
      int outG = (srcG * srcA + dstG * dstA * (255 - srcA) / 255) / outA;
      int outB = (srcB * srcA + dstB * dstA * (255 - srcA) / 255) / outA;
      return outA << 24 | Math.min(255, outR) << 16 | Math.min(255, outG) << 8 | Math.min(255, outB);
   }
}
