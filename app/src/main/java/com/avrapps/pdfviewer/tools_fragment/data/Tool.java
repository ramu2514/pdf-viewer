package com.avrapps.pdfviewer.tools_fragment.data;

public class Tool {

    private int toolName;
    private int drawable;
    private int background;
    private int operationCode;

    public Tool(int toolName, int drawable, int background, int operationCode) {
        this.toolName = toolName;
        this.drawable = drawable;
        this.background = background;
        this.operationCode = operationCode;
    }

    public int getToolName() {
        return toolName;
    }

    public int getDrawable() {
        return drawable;
    }

    public void setDrawable(int drawable) {
        this.drawable = drawable;
    }

    public int getBackground() {
        return background;
    }

    public int getOperationCode() {
        return operationCode;
    }
}
