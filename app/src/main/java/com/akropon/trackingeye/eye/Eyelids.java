package com.akropon.trackingeye.eye;

import android.util.Log;

import com.akropon.trackingeye.Constants;
import com.akropon.trackingeye.ScreenManager;

import org.andengine.entity.Entity;
import org.andengine.entity.primitive.DrawMode;
import org.andengine.entity.primitive.Mesh;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.util.color.Color;

import java.util.Random;

/**
 * Created by akropon on 25.09.2017.
 */

public class Eyelids extends Entity {
    public static final int UNUSED = 0;


    private VertexBufferObjectManager vbom;
    Random random;

    private Mesh upperEyelid;
    private Mesh lowerEyelid;
    private float currentOpennessLevel;

    private EyelidsDynamicHandler currentEyelidsDynamicHandler;
    private EyelidsDynamicHandler nextEyelidsDynamicHandler;


    public Eyelids(VertexBufferObjectManager vbom) {
        currentOpennessLevel = 0;
        random = new Random(System.currentTimeMillis());
        currentEyelidsDynamicHandler = null;
        nextEyelidsDynamicHandler = null;

        float yBottomLevel = ScreenManager.getScreenHeight();
        float yMiddleLevel = yBottomLevel / 2;
        int xStepsAmount = 100;
        float xStep = ScreenManager.getScreenWidth() / (float) (xStepsAmount - 1);
        float[] bufferDataUpperEyelid = new float[xStepsAmount * 3 * 2]; //2 точки по тре координаты (x,y,z) на каждый шаг
        float[] bufferDataLowerEyelid = new float[xStepsAmount * 3 * 2];


        for (int i = 0; i < xStepsAmount; i++) {
            bufferDataUpperEyelid[i * 6 + 0] = xStep * i;
            bufferDataUpperEyelid[i * 6 + 1] = yMiddleLevel;
            bufferDataUpperEyelid[i * 6 + 2] = UNUSED;

            bufferDataUpperEyelid[i * 6 + 3] = xStep * i;
            bufferDataUpperEyelid[i * 6 + 4] = 0;
            bufferDataUpperEyelid[i * 6 + 5] = UNUSED;


            bufferDataLowerEyelid[i * 6 + 0] = xStep * i;
            bufferDataLowerEyelid[i * 6 + 1] = yMiddleLevel;
            bufferDataLowerEyelid[i * 6 + 2] = UNUSED;

            bufferDataLowerEyelid[i * 6 + 3] = xStep * i;
            bufferDataLowerEyelid[i * 6 + 4] = yBottomLevel;
            bufferDataLowerEyelid[i * 6 + 5] = UNUSED;
        }

        upperEyelid = new Mesh(0, 0, bufferDataUpperEyelid, xStepsAmount * 2,
                DrawMode.TRIANGLE_STRIP, vbom);
        lowerEyelid = new Mesh(0, 0, bufferDataLowerEyelid, xStepsAmount * 2,
                DrawMode.TRIANGLE_STRIP, vbom);
        upperEyelid.setColor(Color.BLACK);
        lowerEyelid.setColor(Color.BLACK);

        this.attachChild(upperEyelid);
        this.attachChild(lowerEyelid);
    }


    public void onTouched() {
        nextEyelidsDynamicHandler = new EyelidsDynamicHandler(currentOpennessLevel,
                currentOpennessLevel, this) {
            @Override
            public void onManageUpdate() {
                if (iterator <= 20) {
                    if (iterator <= 5) {
                        newOpenessLevel = (1.0f - iterator / 5.0f) * startOpeness;
                    } else if (iterator > 15) {
                        newOpenessLevel = ((iterator - 15) / 5.0f) * finalOpeness;
                    }
                    iterator++;
                    Log.d("akropon-tag", "opLvl=" + newOpenessLevel);
                } else {
                    isDead = true;
                }
            }
        };
    }

    public void action_blink() {
        nextEyelidsDynamicHandler = new EyelidsDynamicHandler(currentOpennessLevel,
                currentOpennessLevel, this) {
            @Override
            public void onManageUpdate() {
                if (iterator <= 10) {
                    if (iterator <= 5) {
                        newOpenessLevel = (1.0f - iterator / 5.0f) * startOpeness;
                    } else if (iterator > 5) {
                        newOpenessLevel = ((iterator - 5) / 5.0f) * finalOpeness;
                    }
                    iterator++;
                } else {
                    isDead = true;
                }
            }
        };
    }

    public void action_changeOpeness(final float finalOpeness, final float seconds) {

        if (currentEyelidsDynamicHandler == null) {
            if (currentOpennessLevel == finalOpeness) {
                return;
            }
        } else {
            if (currentEyelidsDynamicHandler.finalOpeness == finalOpeness) {
                return;
            }
        }

        if (nextEyelidsDynamicHandler != null)
            if (nextEyelidsDynamicHandler.finalOpeness == finalOpeness)
                return;


        nextEyelidsDynamicHandler = new EyelidsDynamicHandler(
                currentOpennessLevel, finalOpeness, this) {
            @Override
            public void onManageUpdate() {
                if (iterator <= seconds * Constants.INITIAL_FPS) {
                    newOpenessLevel = startOpeness + (finalOpeness - startOpeness)
                            * ((float) iterator / (seconds * Constants.INITIAL_FPS));
                    iterator++;
                } else {
                    isDead = true;
                }
            }
        };


    }


    public boolean isActionHandlerExists() {
        return (currentEyelidsDynamicHandler != null);
    }

    @Override
    protected void onManagedUpdate(float pSecondsElapsed) {
        /*if (random.nextFloat() < 0.005f) {
            currentOpennessLevel = 0;
            recountBuffersByNewOpenessLevel(currentOpennessLevel);
        } else {
            if (currentOpennessLevel > 0.95f) return;
            recountBuffersByNewOpenessLevel(currentOpennessLevel);
            currentOpennessLevel = Bounds.getInterpolation(0.02f, currentOpennessLevel, 1);
        }*/


        if (nextEyelidsDynamicHandler != currentEyelidsDynamicHandler) {
            currentEyelidsDynamicHandler = nextEyelidsDynamicHandler;
        } else {
            if (currentEyelidsDynamicHandler != null) {
                if (currentEyelidsDynamicHandler.isDead()) {
                    currentEyelidsDynamicHandler = null;
                } else {

                    if (currentEyelidsDynamicHandler.getNewOpenessLevel() != currentOpennessLevel) {
                        currentOpennessLevel = currentEyelidsDynamicHandler.getNewOpenessLevel();
                        recountBuffersByNewOpenessLevel();
                    }
                    currentEyelidsDynamicHandler.onManageUpdate();
                }
            }
        }


    }

    private double sqr(double value) {
        return value * value;
    }

    private void recountBuffersByNewOpenessLevel() {
        double openness = currentOpennessLevel * ScreenManager.getScreenHeight() / 2;
        double gyp = Math.sqrt(sqr(openness) + sqr(ScreenManager.getScreenWidth() / 2));
        double cosAlpha = openness / gyp;
        double radius = (gyp / 2) / cosAlpha;
        double centerY = ScreenManager.getScreenHeight() / 2 - openness + radius;

        int pointSteps = upperEyelid.getBufferData().length / 6;
        float[] bufferDataUpperEyelid = upperEyelid.getBufferData();
        float[] bufferDataLowerEyelid = lowerEyelid.getBufferData();
        float xStep = ScreenManager.getScreenWidth() / (float) (pointSteps - 1);
        float currentX;
        double sinRotate;
        double cosRotate;
        float currentY;

        upperEyelid.getVertexBufferObject().setDirtyOnHardware();
        lowerEyelid.getVertexBufferObject().setDirtyOnHardware();
        for (int i = 0; i < pointSteps; i++) {
            currentX = i * xStep;
            cosRotate = (currentX - ScreenManager.getScreenWidth() / 2) / radius;
            sinRotate = Math.sqrt(1 - sqr(cosRotate));
            currentY = (float) (centerY - sinRotate * radius);

            bufferDataUpperEyelid[i * 6 + 1] = currentY;
            bufferDataLowerEyelid[i * 6 + 1] = ScreenManager.getScreenHeight() - currentY;
        }
    }


    public static abstract class EyelidsDynamicHandler {
        float startOpeness;
        float finalOpeness;
        Eyelids eyelids;
        int iterator;
        float newOpenessLevel;
        boolean isDead;

        public EyelidsDynamicHandler(float startOpeness, float finalOpeness, Eyelids eyelids) {
            this.startOpeness = startOpeness;
            this.finalOpeness = finalOpeness;
            this.eyelids = eyelids;
            iterator = 0;
            newOpenessLevel = startOpeness;
            isDead = false;
        }

        public float getNewOpenessLevel() {
            return newOpenessLevel;
        }

        public boolean isDead() {
            return isDead;
        }

        abstract public void onManageUpdate();
    }
}
