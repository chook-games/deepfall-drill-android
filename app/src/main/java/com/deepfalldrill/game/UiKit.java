package com.deepfalldrill.game;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

/** Tiny immediate-mode drawing kit. Sizes are in design units (390-wide). */
public final class UiKit {
    public final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    public final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    public final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

    public final Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);
    public final Typeface bold = Typeface.create("sans-serif", Typeface.BOLD);
    public final Typeface mono = Typeface.create("monospace", Typeface.BOLD);

    public float u = 1f;
    public int w = 1;
    public int h = 1;

    public void setViewport(int w, int h) {
        this.w = w;
        this.h = h;
        this.u = w / Config.UI_UNIT_REFERENCE;
    }

    public float px(float designUnits) {
        return designUnits * u;
    }

    public void txt(Canvas c, String s, float x, float y, float size, int color, Paint.Align align, boolean isBold) {
        text.setTypeface(isBold ? bold : regular);
        text.setTextSize(px(size));
        text.setColor(color);
        text.setTextAlign(align);
        text.setLetterSpacing(0f);
        text.setShadowLayer(0f, 0f, 0f, 0);
        c.drawText(s, x, y, text);
    }

    public void spaced(Canvas c, String s, float x, float y, float size, int color,
                       Paint.Align align, boolean isBold, float em) {
        text.setTypeface(isBold ? bold : regular);
        text.setTextSize(px(size));
        text.setColor(color);
        text.setTextAlign(align);
        text.setLetterSpacing(em);
        text.setShadowLayer(0f, 0f, 0f, 0);
        c.drawText(s, x, y, text);
        text.setLetterSpacing(0f);
    }

    public void shadowText(Canvas c, String s, float x, float y, float size, int color,
                           Paint.Align align, boolean isBold, float blur, int shadowColor) {
        text.setTypeface(isBold ? bold : regular);
        text.setTextSize(px(size));
        text.setColor(color);
        text.setTextAlign(align);
        text.setLetterSpacing(0f);
        text.setShadowLayer(px(blur), 0f, px(blur * 0.35f), shadowColor);
        c.drawText(s, x, y, text);
        text.setShadowLayer(0f, 0f, 0f, 0);
    }

    public void rect(Canvas c, float l, float t, float r, float b, int color, float radius) {
        fill.setColor(color);
        fill.setStyle(Paint.Style.FILL);
        if (radius <= 0f) c.drawRect(l, t, r, b, fill);
        else c.drawRoundRect(l, t, r, b, px(radius), px(radius), fill);
    }

    public void outline(Canvas c, float l, float t, float r, float b, int color, float width, float radius) {
        line.setColor(color);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(px(width));
        if (radius <= 0f) c.drawRect(l, t, r, b, line);
        else c.drawRoundRect(l, t, r, b, px(radius), px(radius), line);
    }

    public void panel(Canvas c, float l, float t, float r, float b) {
        rect(c, l, t + px(2), r, b + px(4), 0x66000000, 18);
        rect(c, l, t, r, b, 0xF2101319, 18);
        outline(c, l, t, r, b, 0x18FFFFFF, 1f, 18);
    }

    public void dim(Canvas c) {
        fill.setColor(0xE6040508);
        fill.setStyle(Paint.Style.FILL);
        c.drawRect(0, 0, w, h, fill);
    }

    public void bar(Canvas c, float l, float t, float r, float b, float frac, int color) {
        float radius = (b - t) / 2f;
        rect(c, l, t, r, b, 0x22FFFFFF, radius);
        float rr = l + (r - l) * Config.clamp(frac, 0f, 1f);
        if (rr > l + radius) rect(c, l, t, rr, b, color, radius);
        else if (rr > l) rect(c, l, t, l + radius, b, color, radius);
    }
}
