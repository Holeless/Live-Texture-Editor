package com.zeeesea.textureeditor.editor;



public class Layer {
   private final int width;
   private final int height;
   private int[][] pixels;
   private boolean visible = true;
   private String name;

   public Layer(int width, int height, String name) {
      this.width = width;
      this.height = height;
      this.name = name;
      this.pixels = new int[width][height];
   }

   public Layer(int width, int height, String name, int[][] initialPixels) {
      this.width = width;
      this.height = height;
      this.name = name;
      this.pixels = copyPixels(initialPixels, width, height);
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public boolean isVisible() {
      return this.visible;
   }

   public void setVisible(boolean visible) {
      this.visible = visible;
   }

   public int getPixel(int x, int y) {
      return x >= 0 && x < this.width && y >= 0 && y < this.height ? this.pixels[x][y] : 0;
   }

   public void setPixel(int x, int y, int color) {
      if (x >= 0 && x < this.width && y >= 0 && y < this.height) {
         this.pixels[x][y] = color;
      }
   }

   public int[][] getPixels() {
      return this.pixels;
   }

   public void setPixels(int[][] newPixels) {
      this.pixels = copyPixels(newPixels, this.width, this.height);
   }

   public int[][] copyPixelsOut() {
      return copyPixels(this.pixels, this.width, this.height);
   }

   public boolean isEmpty() {
      for (int x = 0; x < this.width; x++) {
         for (int y = 0; y < this.height; y++) {
            if ((this.pixels[x][y] & 0xFF000000) != 0) {
               return false;
            }
         }
      }

      return true;
   }

   private static int[][] copyPixels(int[][] src, int w, int h) {
      int[][] copy = new int[w][h];

      for (int x = 0; x < w; x++) {
         System.arraycopy(src[x], 0, copy[x], 0, h);
      }

      return copy;
   }
}
