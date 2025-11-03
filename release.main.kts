#!/usr/bin/env kotlin

import java.io.File

val RESET = "\u001B[0m"
val RED = "\u001B[31m"
val GREEN = "\u001B[32m"
val YELLOW = "\u001B[33m"
val BLUE = "\u001B[34m"

fun printColoured(message: String, colour: String) {
    println("$colour$message$RESET")
}

fun validateSemanticVersion(version: String): Boolean {
    val regex = Regex("""^\d+\.\d+\.\d+$""")
    return regex.matches(version)
}

fun getCurrentVersionInfo(buildFile: File): Pair<Int, String> {
    val content = buildFile.readText()
    val versionCodeRegex = Regex("""versionCode\s*=\s*(\d+)""")
    val versionNameRegex = Regex("""versionName\s*=\s*"([^"]+)"""")

    val versionCode = versionCodeRegex.find(content)?.groupValues?.get(1)?.toInt()
        ?: throw IllegalStateException("Could not find versionCode in build.gradle.kts")
    val versionName = versionNameRegex.find(content)?.groupValues?.get(1)
        ?: throw IllegalStateException("Could not find versionName in build.gradle.kts")

    return Pair(versionCode, versionName)
}

fun updateBuildFile(buildFile: File, newVersionCode: Int, newVersionName: String) {
    var content = buildFile.readText()

    // Update versionCode
    content = content.replace(
        Regex("""versionCode\s*=\s*\d+"""),
        "versionCode = $newVersionCode"
    )

    // Update versionName
    content = content.replace(
        Regex("""versionName\s*=\s*"[^"]+""""),
        """versionName = "$newVersionName""""
    )

    buildFile.writeText(content)
}

fun runCommand(command: String): Boolean {
    printColoured("\n→ Running: $command", BLUE)
    val process = ProcessBuilder(*command.split(" ").toTypedArray())
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()

    val exitCode = process.waitFor()
    return exitCode == 0
}

// Main script
try {
    printColoured("╔════════════════════════════════════════╗", GREEN)
    printColoured("║   WaifuVault Release Build Script     ║", GREEN)
    printColoured("╚════════════════════════════════════════╝", GREEN)

    // Check if build.gradle.kts exists
    val buildFile = File("app/build.gradle.kts")
    if (!buildFile.exists()) {
        printColoured("Error: app/build.gradle.kts not found!", RED)
        printColoured("Make sure you're running this script from the project root directory.", YELLOW)
        kotlin.system.exitProcess(1)
    }

    // Get current version info
    val (currentVersionCode, currentVersionName) = getCurrentVersionInfo(buildFile)
    printColoured("\nCurrent version: $currentVersionName (code: $currentVersionCode)", BLUE)

    // Prompt for new version
    print("\n${YELLOW}Enter new version (x.x.x format): $RESET")
    val newVersionName = readLine()?.trim() ?: ""

    if (newVersionName.isEmpty()) {
        printColoured("Error: Version cannot be empty!", RED)
        kotlin.system.exitProcess(1)
    }

    if (!validateSemanticVersion(newVersionName)) {
        printColoured("Error: Version must be in semantic versioning format (x.x.x)", RED)
        printColoured("Example: 1.2.0, 2.0.1, 1.1.3", YELLOW)
        kotlin.system.exitProcess(1)
    }

    val newVersionCode = currentVersionCode + 1

    // Confirm changes
    printColoured("\n┌─────────────────────────────────────┐", GREEN)
    printColoured("│ Version Changes:                    │", GREEN)
    printColoured("│ Version Name: $currentVersionName → $newVersionName${" ".repeat(17 - currentVersionName.length - newVersionName.length)}│", GREEN)
    printColoured("│ Version Code: $currentVersionCode → $newVersionCode${" ".repeat(19)}│", GREEN)
    printColoured("└─────────────────────────────────────┘", GREEN)

    print("\n${YELLOW}Proceed with release build? (y/n): $RESET")
    val confirm = readLine()?.trim()?.lowercase()

    if (confirm != "y" && confirm != "yes") {
        printColoured("Build cancelled.", YELLOW)
        kotlin.system.exitProcess(0)
    }

    // Update build.gradle.kts
    printColoured("\n[1/4] Updating build.gradle.kts...", BLUE)
    updateBuildFile(buildFile, newVersionCode, newVersionName)
    printColoured("✓ Version updated successfully", GREEN)

    // Run gradle clean
    printColoured("\n[2/4] Running Gradle clean...", BLUE)
    if (!runCommand("./gradlew clean")) {
        printColoured("✗ Gradle clean failed!", RED)
        kotlin.system.exitProcess(1)
    }
    printColoured("✓ Clean completed", GREEN)

    // Run gradle assembleRelease
    printColoured("\n[3/4] Running Gradle assembleRelease...", BLUE)
    if (!runCommand("./gradlew assembleRelease")) {
        printColoured("✗ Gradle assembleRelease failed!", RED)
        kotlin.system.exitProcess(1)
    }
    printColoured("✓ APK build completed", GREEN)

    // Run gradle bundleRelease
    printColoured("\n[4/4] Running Gradle bundleRelease...", BLUE)
    if (!runCommand("./gradlew bundleRelease")) {
        printColoured("✗ Gradle bundleRelease failed!", RED)
        kotlin.system.exitProcess(1)
    }
    printColoured("✓ AAB build completed", GREEN)

    // Success message
    printColoured("\n╔════════════════════════════════════════╗", GREEN)
    printColoured("║        BUILD SUCCESSFUL! ✓             ║", GREEN)
    printColoured("╚════════════════════════════════════════╝", GREEN)
    printColoured("\nAPK location: app/build/outputs/apk/release/waifuVault-v$newVersionName.apk", BLUE)
    printColoured("AAB location: app/build/outputs/bundle/release/app-release.aab", BLUE)

} catch (e: Exception) {
    printColoured("\n✗ Error: ${e.message}", RED)
    kotlin.system.exitProcess(1)
}
