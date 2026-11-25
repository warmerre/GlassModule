package dev.glass.lsp;

import android.graphics.drawable.Drawable;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    private static final String[] PROVIDERS = {
            "com.android.launcher3.icons.IconProvider",
            "com.android.launcher3.IconProvider"
    };

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!lp.packageName.equals("com.android.launcher3")
                && !lp.packageName.equals("com.google.android.apps.nexuslauncher")) return;

        for (String name : PROVIDERS) {
            Class<?> c = XposedHelpers.findClassIfExists(name, lp.classLoader);
            if (c == null) continue;
            XposedBridge.hookAllMethods(c, "getIcon", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    Object r = p.getResult();
                    if (r instanceof Drawable && !(r instanceof GlassIconDrawable)) {
                        try {
                            p.setResult(new GlassIconDrawable((Drawable) r));
                        } catch (Throwable t) {
                            XposedBridge.log("GlassModule: " + t);
                        }
                    }
                }
            });
            XposedBridge.log("GlassModule: hooked " + name + " in " + lp.packageName);
            return;
        }
        XposedBridge.log("GlassModule: IconProvider not found in " + lp.packageName);
    }
}
