package com.avrapps.pdfviewer.viewer_fragment;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artifex.mupdf.fitz.PDFAnnotation;
import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.utils.DateTimeUtils;

import java.util.List;

public class AnnotationsDialog extends Dialog {

    public Activity activity;
    List<PDFAnnotation> annotations;
    MuPDFCore muPDFCore;
    OnChangeListener onChangeListener;
    boolean isChanged = false;
    boolean isEditable = false;

    public AnnotationsDialog(Activity activity, List<PDFAnnotation> annotations, MuPDFCore muPDFCore,
                             OnChangeListener listener, boolean isEditable) {
        super(activity, R.style.DialogSlideAnim);
        this.annotations = annotations;
        this.activity = activity;
        this.isEditable = isEditable;
        this.muPDFCore = muPDFCore;
        onChangeListener = listener;
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (isChanged) {
            try {
                muPDFCore.saveDocument(activity, returnValue -> onChangeListener.onChange());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCanceledOnTouchOutside(true);
        setDialogView(getWindow(), R.layout.dialog_layout);
        replaceContent(annotations);
    }

    private void setDialogView(Window window, int layout) {
        //Below lines enable touch outside widow. setCanceledOnTouchOutside is incompatible. https://stackoverflow.com/a/8384124/12643143
        //window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(0.7f); //0 for no dim to 1 for full dim
        window.setContentView(layout);
        window.setGravity(Gravity.BOTTOM);
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private String getDefaultComment(int type) {
        switch (type) {
            case PDFAnnotation.TYPE_TEXT:
                return "Text";
            case PDFAnnotation.TYPE_LINK:
                return "Link";
            case PDFAnnotation.TYPE_FREE_TEXT:
                return "Free Text";
            case PDFAnnotation.TYPE_LINE:
                return "Line";
            case PDFAnnotation.TYPE_SQUARE:
                return "Square";
            case PDFAnnotation.TYPE_CIRCLE:
                return "Circle";
            case PDFAnnotation.TYPE_POLYGON:
                return "Polygon";
            case PDFAnnotation.TYPE_POLY_LINE:
                return "Poly line";
            case PDFAnnotation.TYPE_HIGHLIGHT:
                return "Highlight";
            case PDFAnnotation.TYPE_UNDERLINE:
                return "Underline";
            case PDFAnnotation.TYPE_SQUIGGLY:
                return "Squiggly";
            case PDFAnnotation.TYPE_STRIKE_OUT:
                return "Strike Out";
            case PDFAnnotation.TYPE_REDACT:
                return "Redact";
            case PDFAnnotation.TYPE_STAMP:
                return "Stamp";
            case PDFAnnotation.TYPE_CARET:
                return "Caret";
            case PDFAnnotation.TYPE_INK:
                return "Ink";
            case PDFAnnotation.TYPE_POPUP:
                return "Popup";
            case PDFAnnotation.TYPE_FILE_ATTACHMENT:
                return "File Attachment";
            case PDFAnnotation.TYPE_SOUND:
                return "Sound";
            case PDFAnnotation.TYPE_MOVIE:
                return "Movie";
            case PDFAnnotation.TYPE_WIDGET:
                return "Widget";
            case PDFAnnotation.TYPE_SCREEN:
                return "Screen";
            case PDFAnnotation.TYPE_PRINTER_MARK:
                return "Mark";
            case PDFAnnotation.TYPE_TRAP_NET:
                return "Net";
            case PDFAnnotation.TYPE_WATERMARK:
                return "Watermark";
            case PDFAnnotation.TYPE_3D:
                return "3D";
        }
        return "";
    }

    public void replaceContent(List<PDFAnnotation> annotations) {
        if (annotations.size() != 0) {
            PDFAnnotationAdapter recyclerAdapter = new PDFAnnotationAdapter(annotations);
            RecyclerView recyclerView = findViewById(R.id.recyclerAnnotations);
            recyclerView.setLayoutManager(new LinearLayoutManager(activity));
            recyclerView.setAdapter(recyclerAdapter);
        }
    }

    public interface OnChangeListener {
        void onChange();
    }

    public class PDFAnnotationAdapter extends RecyclerView.Adapter<PDFAnnotationAdapter.ViewHolder> {

        private final List<PDFAnnotation> annotations;

        public PDFAnnotationAdapter(List<PDFAnnotation> annotations) {
            this.annotations = annotations;
        }

        @Override
        @NonNull
        public PDFAnnotationAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(activity).inflate(R.layout.row_annotation_item, parent, false);
            return new PDFAnnotationAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PDFAnnotationAdapter.ViewHolder holder, int position) {
            PDFAnnotation annotation = annotations.get(position);
            String annotationType = getDefaultComment(annotation.getType());
            String annotationComment = annotation.getContents();
            String comment = annotationComment == null || annotationComment.isEmpty() ? annotationType : annotationComment;
            String author = String.format(activity.getString(R.string.annotation_update_info), annotationType,
                    annotation.getAuthor(), DateTimeUtils.getTimeAgo(annotation.getModificationDate().getTime(), activity));
            holder.author.setText(author);
            holder.comment.setText(comment);
            if (isEditable) {
                int color = Color.argb(255, (int) (255 * annotation.getColor()[0]), (int) (255 * annotation.getColor()[1]), (int) (255 * annotation.getColor()[2]));
                holder.reply.setVisibility(getReplyAnnotation(annotation.getType()));
                holder.color.setColorFilter(color);
            }
        }

        private int getReplyAnnotation(int type) {
            return (type == PDFAnnotation.TYPE_UNDERLINE || type == PDFAnnotation.TYPE_STRIKE_OUT || type == PDFAnnotation.TYPE_HIGHLIGHT)
                    ? View.VISIBLE : View.GONE;
        }

        @Override
        public int getItemCount() {
            return annotations.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView author, comment;
            ImageView color, reply, delete;

            ViewHolder(View itemView) {
                super(itemView);
                author = itemView.findViewById(R.id.author);
                comment = itemView.findViewById(R.id.comment);
                if (isEditable) {
                    color = itemView.findViewById(R.id.color);
                    reply = itemView.findViewById(R.id.reply);
                    delete = itemView.findViewById(R.id.delete_item);
                    View.OnClickListener listener = v -> {
                        int position = getAdapterPosition();
                        switch (v.getId()) {
                            case R.id.reply:
                                showReplyTextDialog(position);
                                break;
                            case R.id.color:
                                showColorPickerDialog(position);
                                break;
                            case R.id.delete_item:
                                muPDFCore.deleteAnnotation(annotations.get(position));
                                isChanged = true;
                                annotations.remove(position);
                                notifyDataSetChanged();
                                if (annotations.size() == 0) {
                                    dismiss();
                                }
                                //onChangeListener.onChange();
                                break;
                        }
                    };
                    reply.setOnClickListener(listener);
                    delete.setOnClickListener(listener);
                    color.setOnClickListener(listener);
                } else {
                    View view = itemView.findViewById(R.id.editor_div);
                    view.setVisibility(View.INVISIBLE);
                }
            }

            private void showReplyTextDialog(int position) {
                Dialog dialog = new Dialog(getContext(), R.style.DialogSlideAnim);
                dialog.setCanceledOnTouchOutside(true);
                setDialogView(dialog.getWindow(), R.layout.dialogt_input_annotation_commen);
                Button reply = dialog.findViewById(R.id.replyButton);
                reply.setOnClickListener(v -> {
                    AutoCompleteTextView input = dialog.findViewById(R.id.input);
                    String replyComment = input.getText().toString();
                    muPDFCore.addComment(annotations.get(position), replyComment, activity);
                    isChanged = true;
                    notifyDataSetChanged();
                    dialog.dismiss();
                    //onChangeListener.onChange();
                });
                dialog.show();
            }

            private void showColorPickerDialog(int position) {
                PDFAnnotation pdfAnnotation = annotations.get(position);
                float[] mupdfColors = pdfAnnotation.getColor();
                int selectedColor = Color.argb(255, (int) (255 * mupdfColors[0]), (int) (255 * mupdfColors[1]), (int) (255 * mupdfColors[2]));
                ColorDialog dialog = new ColorDialog(activity, selectedColor, new ColorDialog.OnColorChangedListener() {
                    @Override
                    public void onCancel(ColorDialog dialog) {
                    }

                    @Override
                    public void onOk(ColorDialog dialog, int color) {
                        float[] cl = new float[]{(float) Color.red(color) / 255, (float) Color.green(color) / 255, (float) Color.blue(color) / 255};
                        muPDFCore.setAnnotationColor(pdfAnnotation, cl);
                        isChanged = true;
                        pdfAnnotation.setColor(cl);
                        annotations.remove(position);
                        annotations.add(position, pdfAnnotation);
                        notifyDataSetChanged();
                        //onChangeListener.onChange();
                    }
                });
                dialog.show();

            }

        }
    }
}