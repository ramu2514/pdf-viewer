package com.avrapps.pdfviewer.tools_fragment.constants;

import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.tools_fragment.data.Tool;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class AppConstants {
    public static final List<Tool> TOOLS = Arrays.asList(
            new Tool(R.string.compress_pdf, R.drawable.ic_compress_pdf, R.color.orange, 0),
            new Tool(R.string.protect_pdf, R.drawable.ic_add_password, R.color.light_green, 1),
            new Tool(R.string.remove_password, R.drawable.ic_remove_password, R.color.navy_blue, 2),
            new Tool(R.string.delete_pages, R.drawable.ic_delete_pages, R.color.violet, 3),
            new Tool(R.string.image_to_pdf, R.drawable.ic_add_image, R.color.light_orange, 4),
           // new Tool(R.string.reorder_pages, R.drawable.ic_reorder_pages, R.color.grey, 5),
           // new Tool(R.string.sign_pdf, R.drawable.ic_sign_pdf, R.color.parrot_green, 6),
           // new Tool(R.string.split_pdf, R.drawable.ic_split_pdf, R.color.light_blue, 7),
            new Tool(R.string.merge_pdf, R.drawable.ic_merge_pdf, R.color.dark_pink, 8)
    );
    public static final HashMap<Integer, Integer> TOOL_NAMES = getToolNames();

    private static HashMap<Integer, Integer> getToolNames() {
        HashMap<Integer, Integer> toolNames = new HashMap<>();
        toolNames.put(0, R.string.compress_pdf);
        toolNames.put(1, R.string.protect_pdf);
        toolNames.put(2, R.string.remove_password);
        toolNames.put(3, R.string.delete_pages);
        toolNames.put(4, R.string.image_to_pdf);
        toolNames.put(5, R.string.reorder_pages);
        toolNames.put(6, R.string.sign_pdf);
        toolNames.put(7, R.string.split_pdf);
        toolNames.put(8, R.string.merge_pdf);
        return toolNames;
    }
}
