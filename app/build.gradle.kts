plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.ktlint)
  alias(libs.plugins.detekt)
  alias(libs.plugins.hilt)
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

    val baseUrlDev =
      project.findProperty("BASE_URL_DEV") as? String
        ?: "MISSING_BASE_URL_DEV"
    val baseUrlProd =
      project.findProperty("BASE_URL_PROD") as? String
        ?: "MISSING_BASE_URL_PROD"

    buildConfigField("String", "BASE_URL", "\"$baseUrlDev\"")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

      val baseUrlProd =
        project.findProperty("BASE_URL_PROD") as? String
          ?: "MISSING_BASE_URL_PROD"
      buildConfigField("String", "BASE_URL", "\"$baseUrlProd\"")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  testOptions {
    unitTests.isReturnDefaultValues = true
    unitTests.all {
      it.useJUnitPlatform()
    }
  }
}

ksp {
  arg("room.generateKotlin", "true")
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

  // Compose
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.hilt.navigation.compose)

  // DataStore
  implementation(libs.androidx.datastore.preferences)

  // Room
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // Hilt
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  // Retrofit & Networking
  implementation(libs.retrofit)
  implementation(libs.retrofit.converter.kotlinx)
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging)
  implementation(libs.kotlinx.serialization)

  testImplementation(libs.junit)
  testRuntimeOnly(libs.junit.launcher)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  detektPlugins(libs.detekt.compose)
}
