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
}
