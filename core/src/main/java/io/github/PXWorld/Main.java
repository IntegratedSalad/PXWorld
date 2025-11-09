package io.github.PXWorld;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import io.github.PXWorld.map.Chunk;
import io.github.PXWorld.map.Map;
import io.github.PXWorld.map.PixelBehaviour;
import io.github.PXWorld.map.PixelType;
import io.github.PXWorld.sim.Simulation;

import java.util.List;

import static com.badlogic.gdx.math.MathUtils.clamp;


/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends Game implements InputProcessor {
    private SpriteBatch batch;
    private Texture worldTexture;
    private OrthographicCamera camera;
    private OrthographicCamera uiCamera;

    private ShapeRenderer shapeRenderer;

    private Map worldMap;
    private Simulation sim;

    private float accumulator = 0f;
    private final float STEP = 1f / 5f; // 5 updates per second

    private enum ElementPick {
        ELEMENT_PICK_NONE,
        ELEMENT_PICK_SAND,
        ELEMENT_PICK_WATER
    }

    private ElementPick elementPick = ElementPick.ELEMENT_PICK_NONE;
    private boolean isPickerActive = false;
    Vector2 pickerCenter = new Vector2();
    float pickerRadius = 50.0f;

    public void stepSimulation() {
        sim.step(worldMap.getAllChunks());
    }

    private void placePixels(final int n, final int wx, final int wy) {
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                switch (elementPick) {
                    case ELEMENT_PICK_SAND: {
                        worldMap.setPixel(
                            wx+i, wy+j, PixelType.PIXEL_TYPE_SAND, Map.FLAG_B_BLOCKING | Map.FLAG_B_FALLING,
                            Map.COLOR_SAND_RGB565);
                        break;
                    }
                    case ELEMENT_PICK_WATER: {
                        worldMap.setPixel(
                            wx+i, wy+j, PixelType.PIXEL_TYPE_WATER, Map.FLAG_B_FLUID | Map.FLAG_B_BLOCKING | Map.FLAG_B_FALLING,
                            Map.COLOR_WATER_RGB565);
                        break;
                    }
                    default: {
                    }
                }

            }
        }
    }

    @Override
    public void create() {
        Gdx.app.setLogLevel(Application.LOG_DEBUG);
        this.camera = new OrthographicCamera();
        this.camera.setToOrtho(false, Map.getMapW(), Map.getMapH());
        this.uiCamera = new OrthographicCamera();
        this.uiCamera.setToOrtho(true, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        this.shapeRenderer = new ShapeRenderer();
        this.worldMap = new Map();
        this.sim = new Simulation();
        worldTexture = this.worldMap.getMapTexture();
        worldTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        batch = new SpriteBatch();
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        stepSimulation();
        final List<Chunk> clist = worldMap.getAllChunks();
        for (final Chunk c : clist) { // TODO: to debug, print a red rectangle around chunks being rendered
            if (c.getIsDirty()) {
                Pixmap pxmap = c.getPixmap();
                worldTexture.draw(pxmap, c.getStartX(), c.getStartY());
                pxmap.dispose();
            }
        }
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(worldTexture, 0, 0); // we're still drawing the hole map each frame
        // maybe redraw and clear only chunks?
        batch.end();

        if (this.isPickerActive) {
            shapeRenderer.setProjectionMatrix(uiCamera.combined);

            // Base disc
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0, 0, 0, 0); // translucent background
            shapeRenderer.circle(pickerCenter.x, pickerCenter.y, pickerRadius);

            // Left half highlight (sand) if hovered
            if (elementPick == ElementPick.ELEMENT_PICK_SAND) {
                shapeRenderer.setColor(1f, 0.9f, 0.2f, 0);
                // draw a slightly smaller filled circle, then clip with a vertical rect
                shapeRenderer.circle(pickerCenter.x, pickerCenter.y, pickerRadius - 2);
                // mask right half by drawing a solid rect over it (same translucent bg)
                shapeRenderer.setColor(0, 0, 0, 0.35f);
                shapeRenderer.rect(pickerCenter.x, pickerCenter.y - pickerRadius,
                    pickerRadius, pickerRadius * 2);
            }

            // Right half highlight (water)
            if (elementPick == ElementPick.ELEMENT_PICK_WATER) {
                shapeRenderer.setColor(0.2f, 0.6f, 1f, 0.55f);
                shapeRenderer.circle(pickerCenter.x, pickerCenter.y, pickerRadius - 2);
                shapeRenderer.setColor(0, 0, 0, 0.35f);
                shapeRenderer.rect(pickerCenter.x - pickerRadius, pickerCenter.y - pickerRadius,
                    pickerRadius, pickerRadius * 2);
            }
            shapeRenderer.end();

            // Outline + labels
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(1,1,1,0.9f);
            shapeRenderer.circle(pickerCenter.x, pickerCenter.y, pickerRadius);

            // vertical splitter
            shapeRenderer.line(pickerCenter.x, pickerCenter.y - pickerRadius,
                pickerCenter.x, pickerCenter.y + pickerRadius);
            shapeRenderer.end();
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        worldTexture.dispose();
    }

    @Override
    public boolean keyDown(int i) {
        return false;
    }

    @Override
    public boolean keyUp(int i) {
        return false;
    }

    @Override
    public boolean keyTyped(char c) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (pointer > 0) return false;

        // TODO: Right click choose type - wheel

        Vector3 v = new Vector3(screenX, screenY, 0);
        camera.unproject(v);
        int wx = clamp((int)v.x, 0, Map.getMapW()-1);
        int wy = Map.getMapH() - 1 - clamp((int)v.y, 0, Map.getMapH()-1);

        System.out.println("wx = " + wx + ", wy = " + wy);
        if (button == Input.Buttons.LEFT) {
            this.isPickerActive = false;
            placePixels(30, wx, wy);
        } else if (button == Input.Buttons.RIGHT) {
            // spawn circle
            // choose either sand or water
            if (!this.isPickerActive) {
                this.isPickerActive = true;
                this.pickerCenter.set(screenX, screenY);
                this.elementPick = ElementPick.ELEMENT_PICK_NONE;
            } else {
                this.isPickerActive = false;
            }
        }
        return true;
    }

    @Override
    public boolean touchUp(int i, int i1, int i2, int i3) {
        return false;
    }

    @Override
    public boolean touchCancelled(int i, int i1, int i2, int i3) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (pointer > 0) return false;

        Vector3 v = new Vector3(screenX, screenY, 0);
        camera.unproject(v);
        int wx = clamp((int)v.x, 0, Map.getMapW()-1);
        int wy = Map.getMapH() - 1 - clamp((int)v.y, 0, Map.getMapH()-1);

        placePixels(30, wx, wy);

        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        float dx = screenX - pickerCenter.x;
        float dy = screenY - pickerCenter.y;
        float r2 = dx*dx + dy*dy;
        if (isPickerActive) {
            if (dx < 0) {
                this.elementPick = ElementPick.ELEMENT_PICK_SAND;
            } else if (dx >= 0) {
                this.elementPick = ElementPick.ELEMENT_PICK_WATER;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean scrolled(float v, float v1) {
        return false;
    }
}

/*
* Demo scope:
* 1. Sand [V]
* 2. Water
* 3. Rain
* 4. Wood
* 5. Fire
* 6. Soil
* 7. Mud (timers => after some time passed, wet soil turns into mud)
* 8. Clouds?
* 9. Gravity?
* */
// Create repo now.


