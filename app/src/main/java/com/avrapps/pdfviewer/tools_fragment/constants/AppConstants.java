package com.avrapps.pdfviewer.tools_fragment.constants;

import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.tools_fragment.data.Tool;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class AppConstants {
    public static final int TOOL_COMPRESS = 0;
    public static final int TOOL_PROTECT = 1;
    public static final int TOOL_UNLOCK_PDF = 2;
    public static final int TOOL_DELETE_PAGES = 3;
    public static final int TOOL_IMAGE_TO_PDF = 4;
    public static final int TOOL_REORDER_PAGES = 5;
    public static final int TOOL_WATERMARK_PDF = 6;
    public static final int TOOL_SPLIT_PDF = 7;
    public static final int TOOL_MERGE_PDF = 8;
    public static final int TOOL_PDF_TO_IMAGES = 9;
    public static final int TOOL_ROTATE_PAGES = 10;
    public static final int TOOL_REMOVE_WATERMARK = 11;
    public static final List<Tool> TOOLS = Arrays.asList(
            new Tool(R.string.compress_pdf, R.drawable.ic_compress_pdf, R.color.orange, 0),
            new Tool(R.string.protect_pdf, R.drawable.ic_add_password, R.color.light_green, 1),
            new Tool(R.string.remove_password, R.drawable.ic_remove_password, R.color.navy_blue, 2),
            new Tool(R.string.delete_pages, R.drawable.ic_delete_pages, R.color.violet, 3),
            new Tool(R.string.image_to_pdf, R.drawable.ic_add_image, R.color.light_orange, 4),
            // new Tool(R.string.reorder_pages, R.drawable.ic_reorder_pages, R.color.grey, 5),
            // new Tool(R.string.sign_pdf, R.drawable.ic_sign_pdf, R.color.parrot_green, 6),
            new Tool(R.string.split_pdf, R.drawable.ic_split_pdf, R.color.light_blue, 7),
            new Tool(R.string.merge_pdf, R.drawable.ic_merge_pdf, R.color.dark_pink, 8),
            new Tool(R.string.extract_pages, R.drawable.ic_merge_pdf, R.color.parrot_green, 9),
            new Tool(R.string.rotate_pages, R.drawable.ic_baseline_rotate_right_24, R.color.colorPrimaryDarkYellow, 10),
            new Tool(R.string.remove_watermark, R.drawable.remove_watermark, R.color.colorPrimaryDarkYellow, 11)
    );
    public static final HashMap<Integer, Integer> TOOL_NAMES = getToolNames();
    public static final LinkedHashMap<String, String> SUPPORTED_LANGUAGES = getSupportedLanguages();

    private static HashMap<Integer, Integer> getToolNames() {
        HashMap<Integer, Integer> toolNames = new HashMap<>();
        toolNames.put(TOOL_COMPRESS, R.string.compress_pdf);
        toolNames.put(TOOL_PROTECT, R.string.protect_pdf);
        toolNames.put(TOOL_UNLOCK_PDF, R.string.remove_password);
        toolNames.put(TOOL_DELETE_PAGES, R.string.delete_pages);
        toolNames.put(TOOL_IMAGE_TO_PDF, R.string.image_to_pdf);
        toolNames.put(TOOL_REORDER_PAGES, R.string.reorder_pages);
        toolNames.put(TOOL_WATERMARK_PDF, R.string.sign_pdf);
        toolNames.put(TOOL_SPLIT_PDF, R.string.split_pdf);
        toolNames.put(TOOL_MERGE_PDF, R.string.merge_pdf);
        toolNames.put(TOOL_PDF_TO_IMAGES, R.string.extract_pages);
        toolNames.put(TOOL_ROTATE_PAGES, R.string.rotate_pages);
        toolNames.put(TOOL_REMOVE_WATERMARK, R.string.remove_watermark);
        return toolNames;
    }

    private static LinkedHashMap<String, String> getSupportedLanguages() {
        LinkedHashMap<String , String> languageNameToLocale = new LinkedHashMap<>();
        languageNameToLocale.put("Device Default", "device");
        languageNameToLocale.put("English", "en_US");
        languageNameToLocale.put("Afrikaans", "af_ZA");
        languageNameToLocale.put("Arabic (KSA)", "ar_SA");
        languageNameToLocale.put("Catalan", "ca_ES");
        languageNameToLocale.put("Czech (Czezhia)", "cs_CZ");
        languageNameToLocale.put("Danish (Denmark)", "da_DK");
        languageNameToLocale.put("Dutch (Germany)", "de_DE");
        languageNameToLocale.put("Greek (Greece)", "el_GR");
        languageNameToLocale.put("Spanish (Spain)", "es_ES");
        languageNameToLocale.put("Finnish (Finland)", "fi_FL");
        languageNameToLocale.put("French (France)", "fr_FR");
        languageNameToLocale.put("Hindi (India)", "hi_IN");
        languageNameToLocale.put("Hungarian", "hu_HU");
        languageNameToLocale.put("Italian (Italy)", "it_IT");
        languageNameToLocale.put("Japanese (Japan)", "ja_JP");
        languageNameToLocale.put("Korean (Korea)", "ko_KR");
        languageNameToLocale.put("Dutch (Netherlands)", "nl_NL");
        languageNameToLocale.put("Norwegian (Norway)", "no_NO");
        languageNameToLocale.put("Polish (Poland)", "pl_PL");
        languageNameToLocale.put("Portuguese (Brazil)", "pt_BR");
        languageNameToLocale.put("Portuguese (Portugal)", "pt_PT");
        languageNameToLocale.put("Romanian (Romania)", "ro_RO");
        languageNameToLocale.put("Russian (Russia)", "ru_RU");
        languageNameToLocale.put("Swedish (Sweden)", "sv_SE");
        languageNameToLocale.put("Telugu (India)", "te_IN");
        languageNameToLocale.put("Turkish (Turkey)", "tr_TR");
        languageNameToLocale.put("Ukrainian (Ukraine)", "uk_UA");
        languageNameToLocale.put("Vietnamese (Vietnam)", "vi_VN");
        languageNameToLocale.put("Chinese Simplified (Mainland China)", "zh_CN");
        languageNameToLocale.put("Chinese (Taiwan)", "zh_TW");
        return languageNameToLocale;
    }
}
