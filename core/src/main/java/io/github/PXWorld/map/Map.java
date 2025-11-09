package io.github.PXWorld.map;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import java.util.ArrayList;
import java.util.List;

/*
    Pixel data:
    We can easily double it...
    1024*1024 = 1048576
    1048576 * 8 ~= 8.3MB
    XXXXXXXX XXXXXXXX XXXXXXXX XXXXXXXX
    TYPE     BEHAVIOR COLOR    COLOR

    MSBit can be also used to indicate that we updated the pixel already.
    This comes in handy when scanning is bottom-to-top and there will be smoke.
    It can be set when updating and unset when drawing.

    Map holds mapPixels 2D array object which stores information
    about every pixel on the map.
    It facilitates ways to modify it in a controlled manner.

    We iterate on every chunk - whenever there's update, we redraw only that part of the screen.
    Each chunk has its own Pixmap and texture. If it wants to update =>
    drawPixmap(Pixmap Worldpixmap, int x, int y)
    We can mark them as dirty.
    Only those who are dirty, will be drawn onto the world pixmap.
 */
public class Map {
    private static final int MAP_W = 1024;
    private static final int MAP_H = 1024;

    private static final int noOfChunks = 1024 / Chunk.chunkWidth; // chunk must be a rectangle
    private int[][] mapPixels = new int[MAP_H][MAP_W];
    private Pixmap pixmap; // maybe get rid of that
    private List<Chunk> chunks = new ArrayList<Chunk>(noOfChunks);

    private static final int FLAG_T_BACKGROUND = 0x00000000;
    private static final int FLAG_T_SAND       = 0x01000000;
    private static final int FLAG_T_FIRE       = 0x02000000;
    private static final int FLAG_T_WOOD       = 0x04000000;
    private static final int FLAG_T_WATER      = 0x08000000;
    private static final int FLAG_T_CONCRETE   = 0x10000000;
    /*
        The problem with these now is that these are not single bit values.
        It could be fine if behaviours were not meant to be logically added.
        We do not have to have 2^8 types. Let's have 2^5 = 32 types and see OR
        for now, do not do anything.
     */

    // TODO: Getters for these
    public static final int FLAG_B_STATIC     = 0x00000000; // do not block
    public static final int FLAG_B_FALLING    = 0x00010000;
    public static final int FLAG_B_BLOCKING   = 0x00010000 << 1;
    public static final int FLAG_B_FLUID      = 0x00010000 << 2;
    public static final int FLAG_B_SMOKE      = 0x00010000 << 3;
    public static final int FLAG_B_OBJECT     = 0x00010000 << 4; // maybe objects will have a class?

    public static final int COLOR_SKY_RGB565  = 0xB7FF;
    public static final int COLOR_SAND_RGB565 = 0xFF86;
    public static final int COLOR_CONCRETE_RGB565 = 0xB5B6;
    public static  final int COLOR_WATER_RGB565 = 0x235E;

    public static final int COLOR_SKY_RGB888 =  0x0099FFFF;

    public static final int CHUNK_WIDTH = 64;
    public static final int CHUNK_HEIGHT = 64;

    public Map() {
        this.initMap();
    }

    public static int convertFromRGB565ToRGB888(int c) {
        int r5 = (c >>> 11) & 0x1F;
        int g6 = (c >>> 5)  & 0x3F;
        int b5 =  c         & 0x1F;
        int r8 = (r5 * 527 + 23) >> 6;
        int g8 = (g6 * 259 + 33) >> 6;
        int b8 = (b5 * 527 + 23) >> 6;
        return Color.rgba8888(r8 / 255f, g8 / 255f, b8 / 255f, 1f);
    }

    private void initMap() {
        this.pixmap = new Pixmap(MAP_W, MAP_H, Pixmap.Format.RGB565);
        for (int y = 0; y < MAP_H; y++) {
            for (int x = 0; x < MAP_W; x++) {
                if ((y < 10 || y > MAP_H - 10) ||
                     x < 10 || x > MAP_W - 10) {
                    this.setPixel(x, y,
                        PixelType.PIXEL_TYPE_CONCRETE,
                        Map.FLAG_B_BLOCKING,
                        COLOR_CONCRETE_RGB565);
                } else {
                    this.mapPixels[y][x] = COLOR_SKY_RGB565;
                }
            }
        }
        System.out.println("Creating chunks...");
        for (int cIdy = 0; cIdy < MAP_W; cIdy+=CHUNK_HEIGHT) {
            for (int cIdx = 0; cIdx < MAP_H; cIdx+=CHUNK_WIDTH) {
                Chunk c = new Chunk(cIdx, cIdy, this.mapPixels);
                chunks.add(c);
            }
        }
    }

    public void setPixel(final int x,
                         final int y,
                         final PixelType pxType,
                         final int pixelBehaviourBitfield,
                         final int pixelColor) {
        final int type = getPixelType(pxType);
        this.mapPixels[y][x] = type | pixelBehaviourBitfield | (pixelColor & 0xFFFF);
    }

    // update chunk?

    private int getPixelType(final PixelType pxType) {
        switch (pxType) {
            case PIXEL_TYPE_SAND: {
                return FLAG_T_SAND;
            }
            case PIXEL_TYPE_BACKGROUND: {
                return FLAG_T_BACKGROUND;
            }
            case PIXEL_TYPE_FIRE: {
                return FLAG_T_FIRE;
            }
            case PIXEL_TYPE_WOOD: {
                return FLAG_T_WOOD;
            }
            case PIXEL_TYPE_WATER: {
                return FLAG_T_WATER;
            }
            case PIXEL_TYPE_CONCRETE: {
                return FLAG_T_CONCRETE;
            }
            default: {
                return 0;
            }
        }
    }

    private int getPixelBehaviour(final PixelBehaviour pxBehaviour) {
        switch (pxBehaviour) {
            case PIXEL_BEHAVIOUR_FLUID: {
                return FLAG_B_FLUID;
            }
            case PIXEL_BEHAVIOUR_SMOKE: {
                return FLAG_B_SMOKE;
            }
            case PIXEL_BEHAVIOUR_OBJECT: {
                return FLAG_B_OBJECT;
            }
            case PIXEL_BEHAVIOUR_STATIC: {
                return FLAG_B_STATIC;
            }
            case PIXEL_BEHAVIOUR_FALLING: {
                return FLAG_B_FALLING;
            }
            default: {
                return 0;
            }
        }
    }

    public static PixelBehaviour getPixelBehaviour(final int pixelData) {
        switch (pixelData & 0x00FF0000) {
            case FLAG_B_FALLING: {
                return PixelBehaviour.PIXEL_BEHAVIOUR_FALLING;
            }
            case FLAG_B_BLOCKING: {
                return PixelBehaviour.PIXEL_BEHAVIOUR_BLOCKING;
            }
            default: {
                return PixelBehaviour.PIXEL_BEHAVIOUR_STATIC;
            }
        }
    }

    public static PixelType getPixelType(final int pixelData) {
        switch (pixelData & 0xFF000000) {
            case FLAG_T_CONCRETE: {
                return PixelType.PIXEL_TYPE_CONCRETE;
            }
            case FLAG_T_SAND: {
                return PixelType.PIXEL_TYPE_SAND;
            }
            case FLAG_T_FIRE: {
                return PixelType.PIXEL_TYPE_FIRE;
            }
            case FLAG_T_WOOD: {
                return PixelType.PIXEL_TYPE_WOOD;
            }
            case FLAG_T_WATER: {
                return PixelType.PIXEL_TYPE_WATER;
            }
            default: {
                return PixelType.PIXEL_TYPE_BACKGROUND;
            }
        }
    }

    public List<Chunk> getAllChunks() {
        return this.chunks;
    }

    public Chunk getChunk(final int x, final int y) {
        return this.chunks.get(x % noOfChunks + y % noOfChunks);
    }

    public int getPixel(final int x, final int y) {
        return this.mapPixels[y][x];
    }

    // Now, pixmap should be disposed (deallocated) when drawn into
    // a texture.
    // Probably whenever there's update to the portion of the pixmap,
    // we will create the pixmap from mapPixels, blit onto the texture
    // and delete the pixmap
    // But - do not create, blit and delete for each chunk!
    // only create textures from pixmap updated pixmap
    // after iterating through each chunk, for chunks
    // marked as dirty!
    // Or Pixmap shouldn't be for ALL the pixels, but
    // only for the chunk pixels.
    public Pixmap getPixmap() {
        return this.pixmap;
    }

    public Texture getMapTexture() {
        Pixmap pixmap = new Pixmap(MAP_W, MAP_H, Pixmap.Format.RGB565);
        for (int y = 0; y < MAP_H; y++) {
            for (int x = 0; x < MAP_W; x++) {
                pixmap.drawPixel(x, y, convertFromRGB565ToRGB888(this.mapPixels[y][x] & 0x0000FFFF));
            }
        }
        Texture t = new Texture(pixmap);
        pixmap.dispose();
        return t;
    }

    public Texture getTextureFromChunk(final Chunk c) {
        Pixmap pixmap = c.getPixmap();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    public void drawChunkTextureOntoWorld(final Chunk c, Texture worldTexture) {
        // TODO
    }

    private void deletePixmap() {
        pixmap.dispose();
    }

    public static int getMapW() {
        return MAP_W;
    }

    public static int getMapH() {
        return MAP_H;
    }

//    public static short getPixelBehavior(int pixelData) {
//        // fourth byte
//        return (short)((pixelData >> 16) & 0xFF);
//    }

}
