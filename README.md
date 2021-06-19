# PDF Viewer

## FOR APP TRANSLATORS/ App Users

### Help us translate. 
* Signup and Login to website - https://crowdin.com
* Navigate to link - https://crowdin.com/project/pdf-viewer-lite/settings#translations
* Click on the language you want to translate (Ex:Chinese) 
* Select file strings.xml
* Then you will see the translation page. Translate and click save to store the strings

### Official Channels
Telegram : 
Facebook :
Twitter :

## FOR DEVELOPERS

### Instructions for adding new strings 
We use crowdin CLI to get the strings translated. If you have added a string please follow below guide.
* Setup crowdin CLI tool. Refer installation instructions.https://support.crowdin.com/configuration-file/#introduction
* Upload strings. ```crowdin upload sources```
* Reachout to dev owners via Telegram(Links in support channel). Devs will then translate strings in the website using machine translation
* Download the strings ```crowdin download```. That's all

### Screenshots for new features
If you feel you have added a new feath which worth of having a screen shot then u

### Contributions

Rama krishna Ayinumpudi (Developer AVR-Apps) - https://play.google.com/store/apps/dev?id=5324515993828938417

**Note: Before making use of the project, read below notice. We use & planned to use below libraries.**

#### 1. MuPDF
MuPDF is provided under the GNU Affero General Public License (AGPL), as well as under an Artifex commercial license.  If you are using it in closed source commercial project get licence from Artifex.
https://www.mupdf.com/license.html

#### 2. iText
MuPDF is provided under the GNU Affero General Public License (AGPL), as well as under an iText commercial license. It’s a legal violation to use iText Community and its open source add-ons in a non-AGPL environment. If you are using it in closed source commercial projects get licence from iText.
https://itextpdf.com/en/how-buy/agpl-license

#### Other Libraries
1. Sugar ORM - ORM tool for managing databases - https://satyan.github.io/sugar/
2. spongycastle (Dependency for itext)- https://github.com/rtyley/spongycastle
3. A few android libraries.

### Screenshots

### Testing

https://medium.com/androiddevelopers/local-development-and-testing-with-fakesplitinstallmanager-57083e1840a4
https://github.com/android/app-bundle-samples/blob/f35cfe96ae22cfb84e13057f44a2e54bba16abbd/DynamicFeatures/app/src/main/java/com/google/android/samples/dynamicfeatures/MainActivity.kt

./gradlew bundleDebug
java -jar ~/Downloads/bundletool-all-1.5.0.jar build-apks --overwrite --local-testing --bundle app/build/outputs/bundle/debug/app-debug.aab --output  app/build/outputs/apk/debug.apks
java -jar ~/Downloads/bundletool-all-1.5.0.jar install-apks --apks app/build/outputs/apk/debug.apks

