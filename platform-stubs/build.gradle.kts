// Stubs de APIs internas de la plataforma y flags (aconfig) que Launcher3 espera encontrar en AOSP.
plugins { alias(libs.plugins.android.library) }
android {
    namespace = "com.qtekfun.platformstubs"
    compileSdk { version = release(37) { minorApiLevel = 0 } }
    defaultConfig { minSdk = 31 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_21; targetCompatibility = JavaVersion.VERSION_21 }
}
dependencies { implementation(libs.androidx.core.ktx) }
