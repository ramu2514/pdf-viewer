fastlane documentation
================
# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```
xcode-select --install
```

Install _fastlane_ using
```
[sudo] gem install fastlane -NV
```
or alternatively using `brew install fastlane`

# Available Actions
## Android
### android capture_screen
```
fastlane android capture_screen
```
Capture Screen
### android upload_strings
```
fastlane android upload_strings
```
Upload Strings to Crowdin
### android download_strings
```
fastlane android download_strings
```
Download Strings from Crowdin
### android add_frames
```
fastlane android add_frames
```
Add Frames to Screenshots
### android release
```
fastlane android release
```
Submit a new Beta Build to Crashlytics Beta
### android deploy
```
fastlane android deploy
```
Deploy a new version to the Google Play

----

This README.md is auto-generated and will be re-generated every time [fastlane](https://fastlane.tools) is run.
More information about fastlane can be found on [fastlane.tools](https://fastlane.tools).
The documentation of fastlane can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
