# Multi-Select Photos & Modern Gallery Grid Implementation Plan

Enable multi-photo selection, instant real-time gallery updates upon import, and a modern gallery grid layout matching the user's reference image (tight spacing, square thumbnails, gallery look).

## User Review Required

- **Multi-Photo Selection**: Uses `ActivityResultContracts.GetMultipleContents()` so users can select multiple photos at once from the gallery.
- **Instant Refresh**: Imported photos immediately appear in the gallery view in real-time without needing to reopen the folder.
- **Reference Gallery Layout**: Tight cell spacing (`3dp`) and square center-cropped thumbnails matching the reference screenshot layout.

## Proposed Changes

### [Multi-Select & Gallery UI]

#### [MODIFY] [dialog_gallery.xml](file:///C:/Users/sunrise/AndroidStudioProjects/calc/app/src/main/res/layout/dialog_gallery.xml)
- Adjust grid spacing to tight `3dp` (`verticalSpacing="3dp"` and `horizontalSpacing="3dp"`).

#### [MODIFY] [VaultActivity.kt](file:///C:/Users/sunrise/AndroidStudioProjects/calc/app/src/main/java/com/example/calc/VaultActivity.kt)
- Replace single photo picker with `GetMultipleContents()` launcher.
- Update photo import handler to process multiple URIs and instantly refresh the active gallery grid adapter.
- Ensure square thumbnail styling matching the gallery layout.

## Verification Plan

### Automated Tests
- Build verification (`app:assembleDebug`).

### Manual Verification
- Open Photos, click Add Photo, select multiple photos at once, verify they instantly appear in the gallery grid with clean gallery spacing.
