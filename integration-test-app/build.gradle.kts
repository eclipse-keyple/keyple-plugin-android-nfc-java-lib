plugins {
  id("com.android.application")
  id("kotlin-android")
}

android {
  namespace = "org.eclipse.keyple.plugin.android.nfc.it"
  compileSdk = (project.findProperty("androidCompileSdk") as String).toInt()
  defaultConfig {
    applicationId = "org.eclipse.keyple.plugin.android.nfc.it"
    minSdk = (project.findProperty("androidMinSdk") as String).toInt()
    targetSdk = (project.findProperty("androidCompileSdk") as String).toInt()
    versionCode = 1
    versionName = "1.0"
  }
  buildFeatures { viewBinding = true }
  buildTypes {
    getByName("release") { isMinifyEnabled = false }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.toVersion(project.findProperty("javaSourceLevel") as String)
    targetCompatibility = JavaVersion.toVersion(project.findProperty("javaTargetLevel") as String)
  }
  kotlinOptions { jvmTarget = project.findProperty("javaTargetLevel") as String }
  sourceSets { getByName("main").java.srcDirs("src/main/kotlin") }
  lint { abortOnError = false }
  packagingOptions {
    resources.excludes += "META-INF/NOTICE.md"
    resources.excludes += "META-INF/LICENSE.md"
    resources.excludes += "META-INF/LICENSE.txt"
    resources.excludes += "META-INF/NOTICE.txt"
  }
}

dependencies {
  implementation(platform("org.eclipse.keyple:keyple-java-bom:2026.03.19"))
  implementation(project(":plugin"))
  implementation("org.eclipse.keyple:keyple-service-java-lib:4.0.0-SNAPSHOT") { isChanging = true }
  implementation("org.eclipse.keypop:keypop-reader-java-api")
  implementation("org.eclipse.keyple:keyple-common-java-api")
  implementation("org.eclipse.keyple:keyple-util-java-lib")
  implementation(
      "org.eclipse.keyple:keyple-plugin-java-api:3.0.0-SNAPSHOT") { isChanging = true }
  implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.20")
  implementation("androidx.appcompat:appcompat:1.6.1")
  implementation("com.google.android.material:material:1.9.0")
  implementation("org.slf4j:slf4j-api:1.7.36")
  runtimeOnly("org.slf4j:slf4j-nop:1.7.36")
}
