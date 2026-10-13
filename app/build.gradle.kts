plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
}
android {
 namespace = "org.rodin.ytmlyrics"
 compileSdk = 36
 defaultConfig {
  applicationId = "org.rodin.ytmlyrics"
  minSdk = 28
  targetSdk = 35
  versionCode = 3
  versionName = "0.3.0-debug"
 }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }

}
kotlin { compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 } }
dependencies { implementation("io.github.proify.lyricon:provider:0.1.70") }
