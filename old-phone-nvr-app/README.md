# Old Phone NVR Cloud (Android)

MVP app to turn an old Android phone into:
- foreground NVR service
- segmented local recording (TODO hook in CameraX recorder)
- background upload worker (TODO Drive API upload)

## Build
1. Open project in Android Studio (Hedgehog+)
2. Let Gradle sync
3. Run on Android 8.0+ device
4. Click **Start NVR**

## Next tasks to finish APK production
- Implement CameraX recording in `NvrService`
- Implement Google Drive OAuth + upload in `UploadWorker`
- Add boot receiver auto-start
- Add motion detection + event-first upload queue

## Suggested Drive upload endpoint
`POST https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart`

Use OAuth scope:
`https://www.googleapis.com/auth/drive.file`
