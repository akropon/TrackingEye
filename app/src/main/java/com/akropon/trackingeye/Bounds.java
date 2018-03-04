package com.akropon.trackingeye;

import android.graphics.Rect;

/**
 * Created by akropon on 24.09.2017.
 */

public class Bounds {
    public float l;
    public float t;
    public float r;
    public float b;

    public Bounds(float l, float t, float r, float b) {
        this.l = l;
        this.t = t;
        this.r = r;
        this.b = b;
    }

    public Bounds(Bounds entity) {
        this.l = entity.l;
        this.t = entity.t;
        this.r = entity.r;
        this.b = entity.b;
    }

    public Bounds(Rect rect) {
        this.l = rect.left;
        this.t = rect.top;
        this.r = rect.right;
        this.b = rect.bottom;
    }

    public float getCenterX() {
        return (l + r) / 2;
    }

    public float getCenterY() {
        return (t + b) / 2;
    }

    public static Bounds getInterpolation(float degree, Bounds from, Bounds to) {
        return new Bounds(getInterpolation(degree, from.l, to.l),
                getInterpolation(degree, from.t, to.t),
                getInterpolation(degree, from.r, to.r),
                getInterpolation(degree, from.b, to.b));
    }

    public static float getInterpolation(float degree, float from, float to) {
        return from + (to - from) * degree;
    }


}
