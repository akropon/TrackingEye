package com.akropon.trackingeye;

import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.hardware.camera2.CaptureResult;
import android.os.Bundle;
import android.util.Log;

import com.akropon.trackingeye.eye.Eye;

import org.andengine.engine.Engine;
import org.andengine.engine.LimitedFPSEngine;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TextOptions;
import org.andengine.entity.util.FPSLogger;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.HorizontalAlign;
import org.andengine.util.color.Color;

public class LaunchActivity extends SimpleBaseGameActivity {
    // ===========================================================
    // Constants
    // ===========================================================

    // ===========================================================
    // Fields
    // ===========================================================

    private Font mFont;
    VertexBufferObjectManager vbom;

    public Text txt_x;
    public Text txt_y;

    Sprite s_target;
    TextureRegion tr_target;

    Rectangle rect;

    Eye eye;


    public Scene scene;

    CameraConnector cameraConnector;

    // ===========================================================
    // Constructors
    // ===========================================================

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    public CameraConnector getCameraConnector() {
        return cameraConnector;
    }


    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================

    @Override
    public EngineOptions onCreateEngineOptions() {
        final Camera camera = new Camera(0, 0,
                ScreenManager.getScreenWidth(), ScreenManager.getScreenHeight());

        return new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED,
                new RatioResolutionPolicy(ScreenManager.getScreenWidth(),
                        ScreenManager.getScreenHeight()), camera);
    }


    @Override
    public Engine onCreateEngine(EngineOptions pEngineOptions) {
        return new LimitedFPSEngine(pEngineOptions, Constants.INITIAL_FPS);
    }

    @Override
    public void onCreateResources() {
        this.mFont = FontFactory.create(this.getFontManager(), this.getTextureManager(),
                256, 256, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 32);
        this.mFont.load();


        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

        BitmapTextureAtlas ta_target = new BitmapTextureAtlas(
                getTextureManager(), 32, 32, TextureOptions.BILINEAR);
        tr_target = BitmapTextureAtlasTextureRegionFactory.createFromAsset(
                ta_target, this, "pict01.png", 0, 0);
        ta_target.load();

        eye.loadResources();
    }

    @Override
    public Scene onCreateScene() {
        vbom = mEngine.getVertexBufferObjectManager();
        this.mEngine.registerUpdateHandler(new FPSLogger());

        scene = new Scene();
        scene.setBackground(new Background(1.0f, 1.0f, 1.0f));

        txt_x = new Text(100, 40, this.mFont,
                "x = none",
                new TextOptions(HorizontalAlign.LEFT), vbom);
        txt_y = new Text(100, 100, this.mFont,
                "x = none",
                new TextOptions(HorizontalAlign.LEFT), vbom);

        s_target = new Sprite(0, 0, tr_target, vbom);

        rect = new Rectangle(0, 0, 10, 10, vbom);
        rect.setColor(Color.RED);

        eye.onCreatingScene();

        scene.attachChild(eye);
        //scene.attachChild(rect);
        //scene.attachChild(s_target);
        //scene.attachChild(txt_x);
        //scene.attachChild(txt_y);

        return scene;
    }

    @Deprecated
    public void setText_threadsafe(final String strTxt_x, final String strTxt_y) {
        mEngine.runOnUpdateThread(new Runnable() {
            @Override
            public void run() {
                txt_x.setText(strTxt_x);
                txt_y.setText(strTxt_y);
            }
        });
    }

    public void faceDetected_threadsafe(final float x, final float y) {
        mEngine.runOnUpdateThread(new Runnable() {
            @Override
            public void run() {
                txt_x.setText("x = " + x);
                txt_y.setText("y = " + y);

                float ratioX = cameraConnector.faceDetectionManager.getPosRatioByWidth(x);
                float ratioY = cameraConnector.faceDetectionManager.getPosRatioByHeight(y);
                float screenX = (1 - ratioX) * ScreenManager.getScreenWidth();
                float screenY = ratioY * ScreenManager.getScreenHeight();
                s_target.setPosition(screenX, screenY);

            }
        });
    }

    public void faceNotFound_threadsafe() {
        mEngine.runOnUpdateThread(new Runnable() {
            @Override
            public void run() {
                txt_x.setText("x = none");
                txt_y.setText("y = none");

            }
        });
    }

    public void detectedFaceBound_threadsafe(final int left, final int top,
                                             final int right, final int bottom) {
        mEngine.runOnUpdateThread(new Runnable() {
            @Override
            public void run() {


                float ratioL = cameraConnector.faceDetectionManager.getPosRatioByWidth(left);
                float ratioT = cameraConnector.faceDetectionManager.getPosRatioByHeight(top);
                float ratioR = cameraConnector.faceDetectionManager.getPosRatioByWidth(right);
                float ratioB = cameraConnector.faceDetectionManager.getPosRatioByHeight(bottom);
                float screenL = (1-ratioL)*ScreenManager.getScreenWidth();
                float screenT = ratioT*ScreenManager.getScreenHeight();
                float screenR = (1-ratioR)*ScreenManager.getScreenWidth();
                float screenB = ratioB*ScreenManager.getScreenHeight();

                rect.setPosition(screenL, screenT);
                rect.setWidth(screenR-screenL);
                rect.setHeight(screenB-screenT);
            }
        });
    }

    public void faceDetected(Bounds bounds, CaptureResult captureResult) {
        eye.setDetectedFaceBounds(bounds, captureResult);
    }

    public void faceLost() {
        eye.onFaceLost();
    }

    @Override
    protected void onCreate(Bundle pSavedInstanceState) {
        Log.d("akropon-tag", "launchActivity.onCreate()");

        ScreenManager.init(this);  // should be inited in the most beginning

        super.onCreate(pSavedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        eye = new Eye(this);


        cameraConnector = new CameraConnector(this);
        cameraConnector.onCreate();
    }



    @Override
    protected void onResume() {
        Log.d("akropon-tag", "launchActivity.onResume()");
        Log.d("akropon-tag", "launchActivity.onResume() - orientation = "
                +getResources().getConfiguration().orientation);

        super.onResume();
        cameraConnector.turnOn();
    }


    @Override
    protected void onPause() {
        Log.d("akropon-tag", "launchActivity.onPause()");
        cameraConnector.turnOff();
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d("akropon-tag", "launchActivity.onStop()");
        super.onStop();
        this.finish();
        this.finishAffinity();
        System.exit(0);
    }

    @Override
    protected void onDestroy() {
        Log.d("akropon-tag", "launchActivity.onDestroy()");
        super.onDestroy();
    }

    @Override
    protected void onRestart() {
        Log.d("akropon-tag", "launchActivity.onRestart()");
        super.onRestart();
    }

    // ===========================================================
    // Methods
    // ===========================================================

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
}