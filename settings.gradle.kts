pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
// The Google Home APIs SDK isn't published to a Maven server. Google hosts it as a zip
// in a public bucket, so download it once into .home-sdk/ (git-ignored) and use it as a
// local Maven repository. Bump the version here and in libs.versions.toml together.
val homeSdkVersion = "1_11_1"
val homeSdkDir = file(".home-sdk/$homeSdkVersion")
if (!homeSdkDir.resolve("com/google/android/gms/play-services-home").exists()) {
    println("Downloading the Google Home APIs SDK ($homeSdkVersion)...")
    val zip = file(".home-sdk/home.android.sdk_$homeSdkVersion.zip")
    zip.parentFile.mkdirs()
    java.net.URI("https://storage.googleapis.com/home_sdk_android/home.android.sdk_$homeSdkVersion.zip")
        .toURL().openStream().use { input -> zip.outputStream().use { input.copyTo(it) } }
    java.util.zip.ZipFile(zip).use { z ->
        z.entries().asSequence()
            .filter { !it.isDirectory && !it.name.contains("javadoc") }
            .forEach { entry ->
                val out = homeSdkDir.resolve(entry.name)
                require(out.canonicalPath.startsWith(homeSdkDir.canonicalPath)) { "Bad zip entry ${entry.name}" }
                out.parentFile.mkdirs()
                z.getInputStream(entry).use { input -> out.outputStream().use { input.copyTo(it) } }
            }
    }
    zip.delete()
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = homeSdkDir.toURI()
            content { includeModule("com.google.android.gms", "play-services-home"); includeModule("com.google.android.gms", "play-services-home-types") }
        }
    }
}
rootProject.name = "Still"
include(":app")
