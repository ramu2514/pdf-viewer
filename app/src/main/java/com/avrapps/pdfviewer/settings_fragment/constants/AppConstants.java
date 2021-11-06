package com.avrapps.pdfviewer.settings_fragment.constants;

import com.avrapps.pdfviewer.R;

import java.util.Arrays;
import java.util.List;

public class AppConstants {
    public static final int PERMISSIONS_REQUEST_CODE = 12345;

    //Preferences related constants
    public static final List<Integer> APP_THEME_IDS = Arrays.asList(R.id.app_theme_green, R.id.app_theme_red, R.id.app_theme_blue, R.id.app_theme_black, R.id.app_theme_yellow);
    public static final List<Integer> PAGE_THEME_IDS = Arrays.asList(R.id.page_theme_white, R.id.page_theme_dark, R.id.page_theme_pink, R.id.page_theme_blue);

    public static final String READER_HORIZONTAL_SWIPE = "readerViewDirection";
    public static final String APP_THEME = "appTheme";
    public static final String DARK_THEME = "darkTheme";
    public static final String KEEP_SCREEN_ON = "keepScreenOn";
    public static final String VOLUME_BUTTON_SCROLL = "volumeScroll";
    public static final String AUTO_IMPORT_FILES = "autoImportFiles";
    public static final String PAGE_THEME = "pageTheme";
    public static final String IS_BOUGHT = "isBought";
    public static final String REMOVABLE_STORAGES = "removableStorages";
    public static final String PROVACY_POLICY = "<h3>Privacy Policy:</h2>\n" +
            "<h3>Definitions</h3><p>Below Definitions will help you further understand the privacy policy.<br><b>We:</b> Developers of this app<br><b>Our app:</b> PDF Viewer Lite App<br><b>You:</b> User of this app.</p>\n" +
            "<h3>Interpretation:</h3>\n" +
            "<p>We are requesting NETWORK ACCESS, starting version 3.9 to backup your files to your google drive.</p>\n" +
            "<p>You will be shown consent to upload the files to your drive when you are linking your account to backup your documents. Once you accept the consent, our app will sync your files in background to your google drive.</p>\n" +
            "<p>You will see android notification, when ever we are backing up the files. You can also see your files by going to the Google Drive that you have linked.</p>\n" +
            "<h3>Policy:</h3>\n" +
            "<p>We do not collect any of your personal/non personal information in any manner, infact we don't even have a server computer at all :)</p>\n" +
            "<p>This is an Open Source android app licensed under AGPL in accordance to the libraries that we consume(muPdf,iText) to deliver you this great app. You can see all this app code on <a href=\"https://github.com/ramu2514/pdf-viewer\">Github</a></p>\n" +
            "<p>We use google sign and google drive API just to upload your documents to your account. We will not access any of your files from your account and infact we can not access any of your files that were not created using this app. You can <a href=\"https://developers.google.com/drive/api/v3/about-auth#select_scopes_for_a_new_app\">for the details</a>.</p>\n" +
            "<p>(Note: This was added in accordance to google compliance policies.)</p>";

}
