package com.avrapps.pdfviewer;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.ClassRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;
import tools.fastlane.screengrab.locale.LocaleTestRule;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ScreenShotGrabber {

    @ClassRule
    public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @BeforeEach
    public void init() {
        ActivityScenario.launch(MainActivity.class);
        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());
    }

    @org.junit.jupiter.api.Test
    public void test_DocumentScreen_takeScreenshot() throws InterruptedException {
        Thread.sleep(1000);
        //onView(withId(R.id.listView)).perform(actionOnItemAtPosition(0, click()));
        onView(allOf(withId(R.id.name), withText("Dart"))).perform(click());
        Thread.sleep(2000);
        Screengrab.screenshot("1_document_screen");
    }

    @org.junit.jupiter.api.Test
    public void test_SettingsScreen_takeScreenshot() throws InterruptedException {
        onView(withId(R.id.open_settings_button)).perform(click());
        Thread.sleep(500);
        Screengrab.screenshot("2_settings_screen");
    }

    @Test
    public void test_UtilsScreen_takeScreenshot() throws InterruptedException {
        onView(withId(R.id.open_tools_button)).perform(click());
        Thread.sleep(500);
        Screengrab.screenshot("3_tools_screen");
    }

    @org.junit.jupiter.api.Test
    public void test_FilesScreen_Recents_takeScreenshot() throws InterruptedException {
        Thread.sleep(2000);
        Screengrab.screenshot("4_main_screen_recents");
    }

    //manual screenshots.  5. Text Selection   6. Annotation dialog

    @org.junit.jupiter.api.Test
    public void test_DocumentScreen_TocBookmarks_takeScreenshot() throws InterruptedException {
        Thread.sleep(1000);
        onView(allOf(withId(R.id.name), withText("Dart"))).perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.outlineButton)).perform(click());
        Thread.sleep(500);
        Screengrab.screenshot("7_toc_bookmarks");
    }
    @Test
    public void test_DocumentScreen_DocMoreoptions_takeScreenshot() throws InterruptedException {
        Thread.sleep(1000);
        onView(allOf(withId(R.id.name), withText("Dart"))).perform(click());
        Thread.sleep(1000);
        onView(withId(R.id.moreOptions)).perform(click());
        Thread.sleep(500);
        Screengrab.screenshot("8_doc_more_options");
    }

}
