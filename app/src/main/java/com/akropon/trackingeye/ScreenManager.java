package com.akropon.trackingeye;

import android.util.DisplayMetrics;

/**
 * Created by akropon on 12.09.2017.
 */

public class ScreenManager {

    private static LaunchActivity launchActivity;

    private static int realPhysicalScreenWidth;
    private static int realPhysicalScreenHeight;
    private static int screenWidth;
    private static int screenHeight;

    private static float miningScreenHeight;
    private static float eventScreenHeight;


    public static void init(LaunchActivity launchActivity) {
        ScreenManager.launchActivity = launchActivity;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        launchActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        realPhysicalScreenWidth = displayMetrics.widthPixels;
        realPhysicalScreenHeight = displayMetrics.heightPixels;

        screenHeight = realPhysicalScreenHeight;
        screenWidth = (int) (realPhysicalScreenHeight / Constants.HEIGHT_TO_WIDTH_RATIO);
        if (screenWidth > displayMetrics.widthPixels) {
            screenWidth = realPhysicalScreenWidth;
            screenHeight = (int) (realPhysicalScreenWidth * Constants.HEIGHT_TO_WIDTH_RATIO);
        }

        miningScreenHeight = screenHeight * Constants.MINING_HEIGHT_TO_SCREEN_RATIO;
        eventScreenHeight = screenHeight - 2 * miningScreenHeight;
    }


    public static LaunchActivity getLaunchActivity() {
        return launchActivity;
    }

    public static int getRealPhysicalScreenWidth() {
        return realPhysicalScreenWidth;
    }

    public static int getRealPhysicalScreenHeight() {
        return realPhysicalScreenHeight;
    }

    public static int getScreenWidth() {
        return screenWidth;
    }

    public static int getScreenHeight() {
        return screenHeight;
    }


    public static float getScaleMultiplier(float px_asIs, float px_toBe) {
        return px_toBe / px_asIs;
    }

    public static float getScaleMultiplier(float px_asIs, float r_partFromLength, float px_Length) {
        return px_Length * r_partFromLength / px_asIs;
    }
}
