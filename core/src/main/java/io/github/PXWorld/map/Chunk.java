package io.github.PXWorld.map;

import com.badlogic.gdx.graphics.Pixmap;

public class Chunk {

    private final int startX;
    private final int startY;

    public static final int chunkWidth = 32;
    public static final int chunkHeight = 32;

    // Reference to worldPixels
    private int[][] chunkPixels;

    private boolean isDirty = false;

//    Pixmap pxmap;

    public Chunk(final int startX, final int startY, int[][] worldPixels) {
        this.startX = startX;
        this.startY = startY;
        this.chunkPixels = worldPixels;
//        pxmap = new Pixmap();
    }

    public int getStartX() {
        return this.startX;
    }

    public int getStartY() {
        return this.startY;
    }

    public boolean getIsDirty() {
        return this.isDirty;
    }

    public void setIsDirty(final boolean isDirty) {
        this.isDirty = isDirty;
    }

    public int[][] getChunkPixels() {
        return this.chunkPixels;
    }

    public int getPixelData(final int x, final int y) {
        return this.chunkPixels[y][x];
    }

    public void setChunkPixel(final int x, final int y, final int pixelData) {
        this.chunkPixels[y][x] = pixelData;
    }

    // Get pixmap which can be drawn onto the worldMap texture at chunk x,y
    public Pixmap getPixmap() {
        Pixmap pxMap = new Pixmap(Chunk.chunkWidth, chunkHeight, Pixmap.Format.RGB565);
        pxMap.setBlending(Pixmap.Blending.None);
        int px = 0;
        int py = 0;
        for (int cy = startY; cy < startY + chunkHeight; cy++) {
            for (int cx = startX; cx < startX + chunkWidth; cx++) {
                pxMap.drawPixel(px, py, Map.convertFromRGB565ToRGB888(chunkPixels[cy][cx] & 0x0000FFFF));
                px++;
            }
            py++;
            px = 0;
        }
        return pxMap;
    }

    // probably checking the behavior should not be here
    // x,y should be in bound of the chunkPixels here
    public void movePixelUp(final int x, final int y) {
        final int pixelData = this.chunkPixels[y][x];
        this.chunkPixels[y-1][x] = pixelData;
        this.chunkPixels[y][x] = 0;
    }

    public void movePixelDown(final int x, final int y) {
        final int pixelData = this.chunkPixels[y][x];
        final int previousPixelData = 0xB7FF; // TODO: Get background / set background as member variable
        this.chunkPixels[y+1][x] = pixelData;
        this.chunkPixels[y][x] = previousPixelData;
    }


    public void movePixelLeft(final int x, final int y) {
        final int pixelData = this.chunkPixels[y][x];
        final int previousPixelData = 0xB7FF; // TODO: Get background / set background as member variable
        this.chunkPixels[y][x-1] = pixelData;
        this.chunkPixels[y][x] = previousPixelData;
    }

    public void movePixelRight(final int x, final int y) {
        final int pixelData = this.chunkPixels[y][x];
        final int previousPixelData = 0xB7FF; // TODO: Get background / set background as member variable
        this.chunkPixels[y][x+1] = pixelData;
        this.chunkPixels[y][x] = previousPixelData;
    }

    public void destroyPixel(final int x, final int y) {
        this.chunkPixels[y][x] = 0;
    }
}
