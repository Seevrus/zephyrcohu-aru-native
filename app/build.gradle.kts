plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.ktlint)
  alias(libs.plugins.detekt)
}

android {
  namespace = "com.zephyr.boreal"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.zephyr.boreal"
    minSdk = 29
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

ktlint {
  android = true
  ignoreFailures = false
  reporters {
    reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
    reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
  }
}

detekt {
  buildUponDefaultConfig = true
  allRules = false
  config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  detektPlugins(libs.detekt.compose)
}
