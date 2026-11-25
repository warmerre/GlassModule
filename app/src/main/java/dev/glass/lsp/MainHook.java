package dev.glass.lsp;

import android.app.Dialog;
import android.appwidget.AppWidgetHostView;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookZygoteInit, IXposedHookLoadPackage {

    private static void log(String s) { XposedBridge.log("GlassModule: " + s); }

    /* ---------- любые приложения, включённые в scope: размытие за диалогами ---------- */
    @Override
    public void initZygote(StartupParam startupParam) {
        try {
            XposedBridge.hookAllMethods(Dialog.class, "onStart", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    try {
                        Window w = ((Dialog) p.thisObject).getWindow();
                        if (w == null) return;
                        WindowManager.LayoutParams a = w.getAttributes();
                        if (a.width == ViewGroup.LayoutParams.MATCH_PARENT
                                && a.height == ViewGroup.LayoutParams.MATCH_PARENT) return;
                        w.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                        a.setBlurBehindRadius(40);
                        a.dimAmount = 0.2f;
                        w.setAttributes(a);
                    } catch (Throwable t) { log("dialog: " + t); }
                }
            });
        } catch (Throwable t) { log("zygote: " + t); }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        switch (lp.packageName) {
            case "com.android.launcher3":
            case "com.google.android.apps.nexuslauncher":
                hookLauncher(lp);
                break;
            case "com.android.systemui":
                hookSystemUi(lp);
                break;
        }
    }

    /* ---------- лаунчер: иконки, док, папки, виджеты ---------- */
    private void hookLauncher(XC_LoadPackage.LoadPackageParam lp) {
        // иконки
        for (String name : new String[]{"com.android.launcher3.icons.IconProvider",
                "com.android.launcher3.IconProvider"}) {
            Class<?> c = XposedHelpers.findClassIfExists(name, lp.classLoader);
            if (c == null) continue;
            XposedBridge.hookAllMethods(c, "getIcon", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    Object r = p.getResult();
                    if (r instanceof Drawable && !(r instanceof GlassIconDrawable)) {
                        try { p.setResult(new GlassIconDrawable((Drawable) r)); }
                        catch (Throwable t) { log("icon: " + t); }
                    }
                }
            });
            log("hooked " + name);
            break;
        }

        // док и папки
        glassBackground(lp, "com.android.launcher3.Hotseat", 28);
        glassBackground(lp, "com.android.launcher3.folder.Folder", 32);

        // виджеты
        try {
            XposedBridge.hookAllConstructors(AppWidgetHostView.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    try {
                        final View v = (View) p.thisObject;
                        final float rad = 26 * v.getResources().getDisplayMetrics().density;
                        v.setBackground(new GlassPanelDrawable(rad));
                        v.setOutlineProvider(new ViewOutlineProvider() {
                            @Override public void getOutline(View view, Outline o) {
                                o.setRoundRect(0, 0, view.getWidth(), view.getHeight(), rad);
                            }
                        });
                        v.setClipToOutline(true);
                    } catch (Throwable t) { log("widget: " + t); }
                }
            });
        } catch (Throwable t) { log("widgets: " + t); }
    }

    private void glassBackground(XC_LoadPackage.LoadPackageParam lp, String cls, final int radiusDp) {
        Class<?> c = XposedHelpers.findClassIfExists(cls, lp.classLoader);
        if (c == null) { log("not found " + cls); return; }
        XposedBridge.hookAllMethods(c, "onFinishInflate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam p) {
                try {
                    View v = (View) p.thisObject;
                    v.setBackground(new GlassPanelDrawable(radiusDp * v.getResources().getDisplayMetrics().density));
                } catch (Throwable t) { log("panel: " + t); }
            }
        });
        log("hooked " + cls);
    }

    /* ---------- SystemUI: включаем размытие шторки ---------- */
    private void hookSystemUi(XC_LoadPackage.LoadPackageParam lp) {
        Class<?> c = XposedHelpers.findClassIfExists("com.android.systemui.statusbar.BlurUtils", lp.classLoader);
        if (c == null) { log("BlurUtils not found"); return; }
        XposedBridge.hookAllMethods(c, "supportsBlursOnWindows", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam p) { p.setResult(true); }
        });
        log("hooked BlurUtils");
    }
}
