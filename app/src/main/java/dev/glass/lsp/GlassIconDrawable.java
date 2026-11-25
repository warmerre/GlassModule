package dev.glass.lsp;

import android.graphics.*;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;

/** Стеклянная плашка иконки: градиент, блики, двойная кромка, каустик снизу, глиф с тенью. */
public class GlassIconDrawable extends Drawable {
    private final Drawable glyph;
    private final boolean tint; // true: белый силуэт, false: оригинальная иконка без изменений цвета
    private final float scale;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path clip = new Path();

    public GlassIconDrawable(Drawable src) {
        Drawable g = src;
        boolean t = false;
        float s = 0.62f;
        if (src instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable a = (AdaptiveIconDrawable) src;
            Drawable mono = a.getMonochrome();
            g = mono != null ? mono : a.getForeground();
            t = true;
            s = 0.84f; // слой 108dp, видимая часть ~66dp
        }
        Drawable.ConstantState cs = g.getConstantState();
        glyph = cs != null ? cs.newDrawable().mutate() : g.mutate();
        tint = t;
        scale = s;
    }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        float w = b.width(), h = b.height();
        if (w <= 0 || h <= 0) return;
        rect.set(b.left + w * 0.03f, b.top + h * 0.03f, b.right - w * 0.03f, b.bottom - h * 0.03f);
        float r = Math.min(w, h) * 0.29f;

        // тело + тень
        p.reset(); p.setAntiAlias(true);
        p.setShader(new LinearGradient(0, rect.top, 0, rect.bottom, 0xE67078F0, 0xE62A2C98, Shader.TileMode.CLAMP));
        p.setShadowLayer(w * 0.06f, 0, w * 0.035f, 0x70000030);
        c.drawRoundRect(rect, r, r, p);

        c.save();
        clip.reset();
        clip.addRoundRect(rect, r, r, Path.Direction.CW);
        c.clipPath(clip);

        // верхний стеклянный лист
        p.reset(); p.setAntiAlias(true);
        RectF top = new RectF(rect.left, rect.top, rect.right, rect.top + h * 0.5f);
        p.setShader(new LinearGradient(0, top.top, 0, top.bottom, 0x44FFFFFF, 0x06FFFFFF, Shader.TileMode.CLAMP));
        c.drawRect(top, p);

        // зеркальный блик слева сверху
        p.reset(); p.setAntiAlias(true);
        p.setShader(new RadialGradient(rect.left + w * 0.28f, rect.top + h * 0.22f, w * 0.55f,
                0x99FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawRect(rect, p);

        // каустик снизу (свет проходит через стекло)
        p.reset(); p.setAntiAlias(true);
        p.setShader(new LinearGradient(0, rect.bottom, 0, rect.bottom - h * 0.32f,
                0x88B4CCFF, 0x00B4CCFF, Shader.TileMode.CLAMP));
        c.drawRect(rect, p);
        c.restore();

        // двойная кромка
        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1f, w * 0.022f));
        p.setShader(new LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
                0xF0FFFFFF, 0x33FFFFFF, Shader.TileMode.CLAMP));
        c.drawRoundRect(rect, r, r, p);
        RectF in = new RectF(rect);
        in.inset(w * 0.03f, h * 0.03f);
        p.setStrokeWidth(Math.max(1f, w * 0.012f));
        p.setShader(new LinearGradient(0, in.top, 0, in.bottom, 0x55FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawRoundRect(in, r * 0.85f, r * 0.85f, p);

        // глиф: тень, затем основной
        int gw = (int) (w * scale), gh = (int) (h * scale);
        int l = b.left + (b.width() - gw) / 2, t = b.top + (b.height() - gh) / 2;
        int off = Math.max(1, (int) (h * 0.02f));
        if (tint) {
            glyph.setColorFilter(new PorterDuffColorFilter(0x59000050, PorterDuff.Mode.SRC_IN));
            glyph.setBounds(l, t + off, l + gw, t + gh + off);
            glyph.draw(c);
            glyph.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        }
        glyph.setBounds(l, t, l + gw, t + gh);
        glyph.draw(c);
    }

    @Override public void setAlpha(int a) { glyph.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter cf) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    @Override public int getIntrinsicWidth() { int v = glyph.getIntrinsicWidth(); return v > 0 ? v : 192; }
    @Override public int getIntrinsicHeight() { int v = glyph.getIntrinsicHeight(); return v > 0 ? v : 192; }
}
