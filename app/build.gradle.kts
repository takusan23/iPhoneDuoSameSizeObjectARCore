/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "io.github.takusan23.iphoneduosamesizeobjectarcore"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        applicationId = "io.github.takusan23.iphoneduosamesizeobjectarcore"

        // AR Optional apps must declare minSdkVersion >= 14.
        // AR Required apps must declare minSdkVersion >= 24.
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildTypes {
        release {
            optimization {
                enable = true
            }
        }
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ARCore (Google Play Services for AR) library.
    implementation("com.google.ar:core:1.56.0")

    // Obj - a simple Wavefront OBJ file loader
    // https://github.com/javagl/Obj
    implementation("de.javagl:obj:0.4.0")

    implementation("androidx.appcompat:appcompat:1.1.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.2.0")

    implementation("com.google.android.material:material:1.1.0")

    // jetpack compose
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
}
