package dev.glass.lsp;

import android.graphics.*;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;

/** Рисует "стеклянную" плашку и белый глиф приложения поверх неё. */
public class GlassIconDrawable extends Drawable {
    private static final int TOP = 0xE07F86F0;     // светлее сверху
    private static final int BOTTOM = 0xE03B3FAE;  // темнее снизу

    private final Drawable glyph;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint inner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path clip = new Path();

    public GlassIconDrawable(Drawable src) {
        Drawable g = src;
        if (src instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable a = (AdaptiveIconDrawable) src;
            Drawable mono = a.getMonochrome();          // API 33+
            g = mono != null ? mono : a.getForeground();
        }
        glyph = g.mutate().getConstantState() != null
                ? g.getConstantState().newDrawable().mutate() : g.mutate();
        glyph.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        stroke.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        float w = b.width(), h = b.height();
        float r = Math.min(w, h) * 0.30f;
        rect.set(b);
        rect.inset(w * 0.02f, h * 0.02f);

        // тело стекла
        fill.setShader(new LinearGradient(0, rect.top, 0, rect.bottom, TOP, BOTTOM, Shader.TileMode.CLAMP));
        fill.setShadowLayer(w * 0.05f, 0, w * 0.03f, 0x66000020);
        c.drawRoundRect(rect, r, r, fill);
        fill.clearShadowLayer();

        c.save();
        clip.reset();
        clip.addRoundRect(rect, r, r, Path.Direction.CW);
        c.clipPath(clip);

        // внутреннее свечение снизу (преломление)
        inner.setShader(new RadialGradient(rect.centerX(), rect.bottom, w * 0.7f,
                0x558FB4FF, 0x00000000, Shader.TileMode.CLAMP));
        c.drawRect(rect, inner);

        // блик сверху
        RectF s = new RectF(rect.left + w * 0.08f, rect.top + h * 0.04f,
                rect.right - w * 0.08f, rect.top + h * 0.48f);
        shine.setShader(new LinearGradient(0, s.top, 0, s.bottom, 0x77FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        c.drawRoundRect(s, r * 0.8f, r * 0.8f, shine);
        c.restore();

        // кромка стекла
        stroke.setStrokeWidth(Math.max(1f, w * 0.018f));
        stroke.setShader(new LinearGradient(rect.left, rect.top, rect.right, rect.bottom,
                0xCCFFFFFF, 0x22FFFFFF, Shader.TileMode.CLAMP));
        c.drawRoundRect(rect, r, r, stroke);

        // глиф
        int gw = (int) (w * 0.60f), gh = (int) (h * 0.60f);
        int l = b.left + (b.width() - gw) / 2, t = b.top + (b.height() - gh) / 2;
        glyph.setBounds(l, t, l + gw, t + gh);
        glyph.draw(c);
    }

    @Override public void setAlpha(int a) { glyph.setAlpha(a); }
    @Override public void setColorFilter(ColorFilter cf) { /* игнор: цвет задаём сами */ }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    @Override public int getIntrinsicWidth() { return glyph.getIntrinsicWidth() > 0 ? glyph.getIntrinsicWidth() : 192; }
    @Override public int getIntrinsicHeight() { return glyph.getIntrinsicHeight() > 0 ? glyph.getIntrinsicHeight() : 192; }
}
