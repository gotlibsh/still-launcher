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
// SHA-256 of that zip. The build stops if the download doesn't match, so a swapped or
// corrupted file can't end up in the app. Update it together with the version.
val homeSdkSha256 = "9154ee88b3d93e7eafb85a1132043e281a14d1f60fc50913a4c230b34afafef4"
val homeSdkDir = file(".home-sdk/$homeSdkVersion")
if (!homeSdkDir.resolve("com/google/android/gms/play-services-home").exists()) {
    println("Downloading the Google Home APIs SDK ($homeSdkVersion)...")
    val zip = file(".home-sdk/home.android.sdk_$homeSdkVersion.zip")
    zip.parentFile.mkdirs()
    java.net.URI("https://storage.googleapis.com/home_sdk_android/home.android.sdk_$homeSdkVersion.zip")
        .toURL().openStream().use { input -> zip.outputStream().use { input.copyTo(it) } }
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    zip.inputStream().use { input ->
        val buffer = ByteArray(1 shl 16)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            digest.update(buffer, 0, n)
        }
    }
    val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
    if (actualSha256 != homeSdkSha256) {
        zip.delete()
        error("Google Home SDK download doesn't match its expected SHA-256 (got $actualSha256). Not using it.")
    }
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
