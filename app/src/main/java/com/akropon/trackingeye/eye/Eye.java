package com.akropon.trackingeye.eye;

import android.hardware.camera2.CaptureResult;

import com.akropon.trackingeye.Bounds;
import com.akropon.trackingeye.LaunchActivity;
import com.akropon.trackingeye.ScreenManager;

import org.andengine.engine.camera.Camera;
import org.andengine.entity.Entity;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.sprite.Sprite;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.util.GLState;
import org.andengine.util.color.Color;

import java.util.Random;

/**
 * Created by akropon on 24.09.2017.
 */

public class Eye extends Entity {
    LaunchActivity launchActivity;

    Random random;

    TextureRegion tr_rainbowka;
    TextureRegion tr_zrachok;

    Sprite s_rainbowka;
    Sprite s_zrachok;
    Eyelids eyelids;

    Bounds detectedRatioBounds;
    Bounds prevStepRatioBounds;
    Bounds targetRatioBounds;
    Bounds centerScreenRatioBounds;

    public Eye(LaunchActivity launchActivity) {
        this.launchActivity = launchActivity;
        random = new Random(System.currentTimeMillis());

        detectedRatioBounds = null;
        prevStepRatioBounds = new Bounds(0, 0, 1, 1);
        centerScreenRatioBounds = new Bounds(0.5f, 0.5f, 0.5f, 0.5f);
        targetRatioBounds = centerScreenRatioBounds;
        prevStepRatioBounds = centerScreenRatioBounds;
    }

    public void loadResources() {


        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

        BitmapTextureAtlas ta_raiwbowka = new BitmapTextureAtlas(
                launchActivity.getTextureManager(), 1080, 1080, TextureOptions.BILINEAR);
        tr_rainbowka = BitmapTextureAtlasTextureRegionFactory.createFromAsset(
                ta_raiwbowka, launchActivity, "rainbowka.png", 0, 0);
        ta_raiwbowka.load();

        BitmapTextureAtlas ta_zrachok = new BitmapTextureAtlas(
                launchActivity.getTextureManager(), 1080, 1080, TextureOptions.BILINEAR);
        tr_zrachok = BitmapTextureAtlasTextureRegionFactory.createFromAsset(
                ta_zrachok, launchActivity, "zrachok.png", 0, 0);
        ta_zrachok.load();


    }

    public void onCreatingScene() {

        s_rainbowka = new Sprite(0, 0, tr_rainbowka, launchActivity.getVertexBufferObjectManager()) {
            protected void preDraw(GLState pGLState, Camera pCamera) {
                pGLState.enableDither();
                super.preDraw(pGLState, pCamera);
            }

            protected void postDraw(GLState pGLState, Camera pCamera) {
                pGLState.disableDither();
                super.postDraw(pGLState, pCamera);
            }
        };
        s_rainbowka.setScale(ScreenManager.getScaleMultiplier(
                s_rainbowka.getHeight(), 0.85f, ScreenManager.getScreenHeight()));


        s_zrachok = new Sprite(0, 0, tr_zrachok, launchActivity.getVertexBufferObjectManager());
        s_zrachok.setScale(ScreenManager.getScaleMultiplier(
                s_zrachok.getHeight(), 0.50f, ScreenManager.getScreenHeight()));


        eyelids = new Eyelids(launchActivity.getVertexBufferObjectManager());

        Rectangle toucheLayout = new Rectangle(0, 0, ScreenManager.getScreenWidth(),
                ScreenManager.getScreenHeight(), launchActivity.getVertexBufferObjectManager()) {
            @Override
            public boolean onAreaTouched(TouchEvent pSceneTouchEvent, float pTouchAreaLocalX, float pTouchAreaLocalY) {
                if (pSceneTouchEvent.getAction() == TouchEvent.ACTION_DOWN)
                    Eye.this.eyelids.onTouched();
                return false;
            }
        };
        toucheLayout.setZIndex(200);


        this.attachChild(s_rainbowka);
        this.attachChild(s_zrachok);
        this.attachChild(eyelids);
        launchActivity.scene.registerTouchArea(toucheLayout);
    }

    public void setDetectedFaceBounds(Bounds ratioBounds, CaptureResult captureResult) {
        float correctingOffset = launchActivity.getCameraConnector().faceDetectionManager
                .getCameraToScreenImageCenterRatioOffset(captureResult, ratioBounds);
        Bounds correctedRatioBounds = new Bounds(
                ratioBounds.l + correctingOffset,
                ratioBounds.t,
                ratioBounds.r + correctingOffset,
                ratioBounds.b);

        this.detectedRatioBounds = correctedRatioBounds;
    }

    public void onFaceLost() {
        this.detectedRatioBounds = null;
    }

    @Override
    protected void onManagedUpdate(float pSecondsElapsed) {
        super.onManagedUpdate(pSecondsElapsed);

        targetRatioBounds = detectedRatioBounds;

        if (targetRatioBounds == null) {
            targetRatioBounds = centerScreenRatioBounds;
        }

        prevStepRatioBounds = Bounds.getInterpolation(0.05f, prevStepRatioBounds, targetRatioBounds);

        float screenX = (1 - prevStepRatioBounds.getCenterX()) * ScreenManager.getScreenWidth();
        float screenY = prevStepRatioBounds.getCenterY() * ScreenManager.getScreenHeight();
        setSpriteByItsCenter(screenX, screenY, s_rainbowka);
        setSpriteByItsCenter(screenX, screenY, s_zrachok);

        /*eyelid.getVertexBufferObject().setDirtyOnHardware();  //одноразовая перезапись дата буфферов в память для отрисовки
        for (int i=0; i<eyelid.getBufferData().length/6; i++) {
            eyelid.getBufferData()[i*6+1] += 1f;
        }*/

        if (detectedRatioBounds == null) {
            if (eyelids.isActionHandlerExists() == false)
                eyelids.action_changeOpeness(0.4f, 1.0f);
            s_rainbowka.setColor(Color.BLUE);

        } else {
            if (eyelids.isActionHandlerExists() == false)
                eyelids.action_changeOpeness(1.0f, 0.1f);
            s_rainbowka.setColor(Color.RED);
        }

        if (random.nextFloat() < 0.005f)
            if (eyelids.isActionHandlerExists() == false)
                eyelids.action_blink();


    }

    private void setSpriteByItsCenter(float x, float y, Sprite sprite) {
        sprite.setPosition(x - sprite.getWidth() / 2, y - sprite.getHeight() / 2);
    }

    private void setSpriteByItsCenterRatioByScreen(float ratioX, float ratioY, Sprite sprite) {
        sprite.setPosition(ratioX - sprite.getWidth() / 2, ratioY - sprite.getHeight() / 2);
    }
}
