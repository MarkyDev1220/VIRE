# Fix Firebase Initialization Crash in ProfileActivity

The application is crashing with an `IllegalStateException` because Firebase is not initialized. This is due to the `google-services` plugin being disabled in the Gradle configuration and the `google-services.json` file being missing from the project.

## User Review Required

> [!IMPORTANT]
> The `google-services.json` file is missing from the `VireApp` module. You must download this file from your Firebase Console and place it in the `VireApp/` directory.

> [!WARNING]
> The `google-services` plugin was commented out in `build.gradle.kts`. Re-enabling it without the `google-services.json` file will cause the project to fail to build.

## Proposed Changes

### Build Configuration

#### [MODIFY] [root build.gradle.kts](file:///Users/markdjones/StudioProjects/VIRE/build.gradle.kts)
Re-enable the `googleServices` plugin alias.

#### [MODIFY] [VireApp build.gradle.kts](file:///Users/markdjones/StudioProjects/VIRE/VireApp/build.gradle.kts)
Re-enable the `google-services` plugin application.

### Code Improvements (Safety Checks)

#### [MODIFY] [ProfileActivity.kt](file:///Users/markdjones/StudioProjects/VIRE/VireApp/src/main/java/com/vire/android/android/ProfileActivity.kt)
Add safety checks when accessing Firebase instances to prevent crashes if the configuration is still missing, and show a helpful Toast instead of crashing.

#### [MODIFY] [GameNightManager.kt](file:///Users/markdjones/StudioProjects/VIRE/VireApp/src/main/java/com/vire/android/android/GameNightManager.kt)
Change Firebase instances to `lazy` initialization to avoid crashing as soon as the `object` is accessed if Firebase is not yet initialized.

## Verification Plan

### Manual Verification
1.  Verify that the project builds after re-enabling the plugin (requires `google-services.json`).
2.  Launch the app and navigate to the Profile screen.
3.  Ensure the app does not crash even if Firebase fails to initialize (it should show a Toast or handle the error gracefully).
