package dev.glass.lsp;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Полупрозрачная стеклянная панель (док, папки, виджеты). */
public class GlassPanelDrawable extends Drawable {
    private final float radius;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    public GlassPanelDrawable(float radiusPx) { radius = radiusPx; }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        if (b.width() <= 0 || b.height() <= 0) return;
        r.set(b);
        float rad = Math.min(radius, Math.min(r.width(), r.height()) / 2f);

        p.reset(); p.setAntiAlias(true);
        p.setShader(new LinearGradient(0, r.top, 0, r.bottom, 0x553F4BD8, 0x551C1F6B, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, rad, rad, p);

        p.reset(); p.setAntiAlias(true);
        RectF top = new RectF(r.left, r.top, r.right, r.top + r.height() * 0.45f);
        p.setShader(new LinearGradient(0, top.top, 0, top.bottom, 0x40FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.save();
        Path clip = new Path();
        clip.addRoundRect(r, rad, rad, Path.Direction.CW);
        c.clipPath(clip);
        c.drawRect(top, p);
        c.restore();

        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1f, rad * 0.06f));
        p.setShader(new LinearGradient(r.left, r.top, r.right, r.bottom, 0xAAFFFFFF, 0x22FFFFFF, Shader.TileMode.CLAMP));
        c.drawRoundRect(r, rad, rad, p);
    }

    @Override public void setAlpha(int a) { }
    @Override public void setColorFilter(ColorFilter cf) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
