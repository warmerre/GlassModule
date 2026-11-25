plugins { id("com.android.application") }
android {
    namespace = "dev.glass.lsp"
    compileSdk = 34
    defaultConfig {
        applicationId = "dev.glass.lsp"
        minSdk = 31
        targetSdk = 34
        versionCode = 2
        versionName = "0.2"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies { compileOnly("de.robv.android.xposed:api:82") }
