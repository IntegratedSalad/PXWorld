package io.github.PXWorld.sim;

import io.github.PXWorld.map.Chunk;
import io.github.PXWorld.map.Map;

import java.util.List;

/*
    Simulation will notify the renderer
    that there's a chunk in which there
    was some simulation update.
    It modifies chunks.

 */
public class Simulation {

    private boolean isRunning = false;

    // this class should monitor which chunks need updating

    public Simulation() {
    }

    public void start() {
       this.isRunning = true;
    }

    // For now - let's iterate every chunk, no need for premature optimization
    // Only rendering will be selective
    // all return chunks needing updating
    public void step(List<Chunk> chunks) {
        for (Chunk chunk : chunks) {
            // TODO: Somehow omit the chunks which don't have anything going on in them.
            // Remember - some event modifies chunks (and demands update).
            // If I add something to the chunk, we can notify them.
            // If pixels fall out of the chunk onto another, that chunk can notify the other or add it to the list of
            // chunks to update
            // Maybe we can track how many pixels are there in the chunk.
            // I think the reason for the weird behaviour (updating in small clusters - chunks)
            // is because I am not marking the chunks, which pixels are getting transferred to, when they move out of
            // the current one
            boolean isDirtyChunk = false;
            chunk.setIsDirty(isDirtyChunk);
            final int sx = chunk.getStartX();
            final int sy = chunk.getStartY();
            for (int cy = sy + Chunk.chunkHeight - 1; cy >= sy; cy--) {
                for (int cx = sx; cx < sx + Chunk.chunkWidth; cx++) {
                    // simulate pixels
                    // 1. Get behavior
                    // TODO: Fix sand disappearing when on water that moves down
                    final int pixelBehaviourData = chunk.getPixelData(cx, cy) & 0x00FF0000;
                    if ((pixelBehaviourData & Map.FLAG_B_FALLING) == Map.FLAG_B_FALLING) {
                        isDirtyChunk |= simulateFalling(chunk, cx, cy); // TODO: flag other chunk if the pixel got into it
                    }
                    if ((pixelBehaviourData & Map.FLAG_B_FLUID) == Map.FLAG_B_FLUID) {
                        isDirtyChunk |= simulateFluid(chunk, cx, cy); // TODO: flag other chunk if the pixel got into it
                    }
                }
            }
            chunk.setIsDirty(isDirtyChunk);
        }
    }

    private boolean simulateFalling(Chunk c, final int posX, final int posY) {
        boolean didMove = false;
        final int posYDownFuture = posY + 1;
        final int posXLeftFuture = posX - 1;
        final int posXRightFuture = posX + 1;
        if ((c.getPixelData(posX, posYDownFuture) & Map.FLAG_B_BLOCKING) != Map.FLAG_B_BLOCKING) {
            c.movePixelDown(posX, posY);
            didMove = true;
        } else {
            if ((c.getPixelData(posXRightFuture, posYDownFuture) & Map.FLAG_B_BLOCKING) != Map.FLAG_B_BLOCKING) {
                c.movePixelRight(posX, posY);
                didMove = true;
                return didMove;
            }
            if ((c.getPixelData(posXLeftFuture, posYDownFuture) & Map.FLAG_B_BLOCKING) != Map.FLAG_B_BLOCKING) {
                c.movePixelLeft(posX, posY);
                didMove = true;
                return didMove;
            }
        }
        return didMove;
    }

    private boolean simulateFluid(Chunk c, final int posX, final int posY) {
        final int posXLeftFuture = posX - 1;
        final int posXRightFuture = posX + 1;
        final int posYDownFuture = posY + 1;
        if ((c.getPixelData(posX, posYDownFuture) & Map.FLAG_B_BLOCKING) == Map.FLAG_B_BLOCKING) {
            if ((c.getPixelData(posXRightFuture, posY) & Map.FLAG_B_BLOCKING) != Map.FLAG_B_BLOCKING) {
                c.movePixelRight(posX, posY); // try right and move if empty space
                return true;
            } else if ((c.getPixelData(posXLeftFuture, posY) & Map.FLAG_B_BLOCKING) != Map.FLAG_B_BLOCKING) {
                c.movePixelLeft(posX, posY); // try left and move if empty space
                return true;
            }
        }
        return false;
    }
}
