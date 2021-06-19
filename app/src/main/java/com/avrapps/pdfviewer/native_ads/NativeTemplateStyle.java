package com.avrapps.pdfviewer.native_ads;

import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;

/**
 * A class containing the optional styling options for the Native Template. *
 */
@SuppressWarnings("ALL")
public class NativeTemplateStyle {

    // Call to action typeface.
    private Typeface callToActionTextTypeface;

    // Size of call to action text.
    private float callToActionTextSize;

    // Call to action typeface color in the form 0xAARRGGBB.
    private int callToActionTypefaceColor;

    // Call to action background color.
    private ColorDrawable callToActionBackgroundColor;

    // All templates have a primary text area which is populated by the native ad's headline.

    // Primary text typeface.
    private Typeface primaryTextTypeface;

    // Size of primary text.
    private float primaryTextSize;

    // Primary text typeface color in the form 0xAARRGGBB.
    private int primaryTextTypefaceColor;

    // Primary text background color.
    private ColorDrawable primaryTextBackgroundColor;

    // The typeface, typeface color, and background color for the second row of text in the template.
    // All templates have a secondary text area which is populated either by the body of the ad or
    // by the rating of the app.

    // Secondary text typeface.
    private Typeface secondaryTextTypeface;

    // Size of secondary text.
    private float secondaryTextSize;

    // Secondary text typeface color in the form 0xAARRGGBB.
    private int secondaryTextTypefaceColor;

    // Secondary text background color.
    private ColorDrawable secondaryTextBackgroundColor;

    // The typeface, typeface color, and background color for the third row of text in the template.
    // The third row is used to display store name or the default tertiary text.

    // Tertiary text typeface.
    private Typeface tertiaryTextTypeface;

    // Size of tertiary text.
    private float tertiaryTextSize;

    // Tertiary text typeface color in the form 0xAARRGGBB.
    private int tertiaryTextTypefaceColor;

    // Tertiary text background color.
    private ColorDrawable tertiaryTextBackgroundColor;

    // The background color for the bulk of the ad.
    private ColorDrawable mainBackgroundColor;

    public Typeface getCallToActionTextTypeface() {
        return callToActionTextTypeface;
    }

    public float getCallToActionTextSize() {
        return callToActionTextSize;
    }

    public int getCallToActionTypefaceColor() {
        return callToActionTypefaceColor;
    }

    public ColorDrawable getCallToActionBackgroundColor() {
        return callToActionBackgroundColor;
    }

    public Typeface getPrimaryTextTypeface() {
        return primaryTextTypeface;
    }

    public float getPrimaryTextSize() {
        return primaryTextSize;
    }

    public int getPrimaryTextTypefaceColor() {
        return primaryTextTypefaceColor;
    }

    public ColorDrawable getPrimaryTextBackgroundColor() {
        return primaryTextBackgroundColor;
    }

    public Typeface getSecondaryTextTypeface() {
        return secondaryTextTypeface;
    }

    public float getSecondaryTextSize() {
        return secondaryTextSize;
    }

    public int getSecondaryTextTypefaceColor() {
        return secondaryTextTypefaceColor;
    }

    public ColorDrawable getSecondaryTextBackgroundColor() {
        return secondaryTextBackgroundColor;
    }

    public Typeface getTertiaryTextTypeface() {
        return tertiaryTextTypeface;
    }

    public float getTertiaryTextSize() {
        return tertiaryTextSize;
    }

    public int getTertiaryTextTypefaceColor() {
        return tertiaryTextTypefaceColor;
    }

    public ColorDrawable getTertiaryTextBackgroundColor() {
        return tertiaryTextBackgroundColor;
    }

    public ColorDrawable getMainBackgroundColor() {
        return mainBackgroundColor;
    }

    /**
     * A class that provides helper methods to build a style object. *
     */
    public static class Builder {

        private final NativeTemplateStyle styles;

        public Builder() {
            this.styles = new NativeTemplateStyle();
        }

        public NativeTemplateStyle build() {
            return styles;
        }
    }
}
