plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
}
android {
 namespace = "org.rodin.ytmlyrics"
 compileSdk = 35
 defaultConfig {
  applicationId = "org.rodin.ytmlyrics"
  minSdk = 28
  targetSdk = 35
  versionCode = 1
  versionName = "0.1.0-test"
 }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies { implementation("io.github.proify.lyricon:provider:0.1.70") }
