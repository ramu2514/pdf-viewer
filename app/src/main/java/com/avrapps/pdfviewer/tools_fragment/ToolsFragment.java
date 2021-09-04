package com.avrapps.pdfviewer.tools_fragment;

import static com.avrapps.pdfviewer.tools_fragment.constants.AppConstants.TOOLS;
import static com.avrapps.pdfviewer.tools_fragment.constants.AppConstants.TOOL_NAMES;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.avrapps.pdfviewer.MainActivity;
import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.tools_fragment.adapter.SquareBoxesAdapter;
import com.avrapps.pdfviewer.tools_fragment.data.PDFUtilsHistory;
import com.avrapps.pdfviewer.utils.DateTimeUtils;
import com.avrapps.pdfviewer.utils.MessagingUtility;
import com.avrapps.pdfviewer.utils.MiscUtils;
import com.avrapps.pdfviewer.utils.PathUtils;
import com.orm.query.Select;
import com.orm.util.NamingHelper;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ToolsFragment extends Fragment {

    List<PDFUtilsHistory> pdfUtilsHistories;
    HistoryViewAdapter listViewAdapter;
    View view;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.show_pdf_tools_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity activity = (MainActivity) getActivity();
        Toolbar toolbar = activity.findViewById(R.id.my_toolbar);
        toolbar.setTitle(R.string.tools);
        this.view = view;

        RecyclerView recyclerView = view.findViewById(R.id.toolsRecycleView);
        final int numberOfColumns = getResources().getInteger(R.integer.grid_columns);
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), numberOfColumns));
        SquareBoxesAdapter adapter = new SquareBoxesAdapter(getActivity(), TOOLS);
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(true);

        RecyclerView recyclerView2 = view.findViewById(R.id.recycler_view);
        recyclerView2.setLayoutManager(new LinearLayoutManager(getActivity()));
        refreshHistory();
        listViewAdapter = new HistoryViewAdapter(activity);
        recyclerView2.setAdapter(listViewAdapter);
        recyclerView2.setNestedScrollingEnabled(true);

        view.findViewById(R.id.clearAllImports).setOnClickListener(v -> {
            MessagingUtility.showPositiveMessageDialog(activity, getString(R.string.delete_all), getString(R.string.delete_tool_history_help), getString(R.string.clear), returnValue -> {
                File folder = new File(Environment.getExternalStorageDirectory(), "PDFViewerLite/Tools/");
                if (PathUtils.deleteDirectory(folder)) {
                    Log.e("Test", "Error deleting folder " + folder);
                }
                PDFUtilsHistory.deleteAll(PDFUtilsHistory.class);
                refreshRecyclerView();
            }, false);
        });
    }

    private void refreshHistory() {
        pdfUtilsHistories = Select.from(PDFUtilsHistory.class)
                .orderBy(NamingHelper.toSQLNameDefault("updatedDate") + " desc")
                .list();
        view.findViewById(R.id.history_help).setVisibility(pdfUtilsHistories.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void refreshRecyclerView() {
        refreshHistory();
        listViewAdapter.notifyDataSetChanged();
    }

    class HistoryViewAdapter extends RecyclerView.Adapter<HistoryViewAdapter.ViewHolder> {

        private final LayoutInflater mInflater;
        MainActivity activity;

        HistoryViewAdapter(MainActivity activity) {
            this.activity = activity;
            this.mInflater = LayoutInflater.from(activity);
        }

        @Override
        @NonNull
        public HistoryViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.history_item, parent, false);
            return new HistoryViewAdapter.ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull HistoryViewAdapter.ViewHolder holder, int position) {
            PDFUtilsHistory history = pdfUtilsHistories.get(position);
            holder.operationName.setText(TOOL_NAMES.get(history.getOperation()));
            holder.sourceFileName.setText(history.getPdfName());
            holder.age.setText(DateTimeUtils.getTimeAgo(history.getUpdatedDate(), activity));
        }

        @Override
        public int getItemCount() {
            return pdfUtilsHistories.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView operationName, age, sourceFileName;

            ViewHolder(View itemView) {
                super(itemView);
                operationName = itemView.findViewById(R.id.operation_name);
                age = itemView.findViewById(R.id.age);
                sourceFileName = itemView.findViewById(R.id.file_name);
                itemView.findViewById(R.id.share_file).setOnClickListener(v -> {
                    PDFUtilsHistory history = pdfUtilsHistories.get(getAdapterPosition());
                    if (history.getOperation() == 9) {
                        MiscUtils.shareMultipleFiles(activity, new File(history.getDestinationFile()), "image/*");
                    } else if (history.getOperation() == 7) {
                        MiscUtils.shareMultipleFiles(activity, new File(history.getDestinationFile()), "application/pdf");
                    } else {
                        MiscUtils.shareFile(activity, new File(history.getDestinationFile()));
                    }
                });
                itemView.findViewById(R.id.open_file).setOnClickListener(v -> {
                    PDFUtilsHistory history = pdfUtilsHistories.get(getAdapterPosition());
                    if (history.getOperation() == 9) {
                        MiscUtils.viewMultipleFiles(activity, new File(history.getDestinationFile()));
                    } else if (history.getOperation() == 7) {
                        MiscUtils.viewMultipleFiles(activity, new File(history.getDestinationFile()));
                    } else {
                        MiscUtils.openFile(activity, new File(history.getDestinationFile()));
                    }
                });
                itemView.findViewById(R.id.download).setOnClickListener(v -> {
                    PDFUtilsHistory history = pdfUtilsHistories.get(getAdapterPosition());
                    try {
                        File f = new File(history.getDestinationFile());
                        MiscUtils.downloadFile(activity, f);
                        String dest = "";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            dest = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString();
                        } else {
                            dest = Environment.getExternalStorageDirectory().getAbsolutePath();
                        }
                        String destination = dest + "/" + f.getName();
                        Toast.makeText(activity, getString(R.string.file_saved,destination), Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(activity, R.string.exception_saving_doc, Toast.LENGTH_LONG).show();
                    }
                });
                itemView.findViewById(R.id.delete).setOnClickListener(v -> {
                    PDFUtilsHistory history = pdfUtilsHistories.get(getAdapterPosition());
                    if (history.getOperation() == 9 || history.getOperation() == 7) {
                        PathUtils.deleteDirectory(new File(history.getDestinationFile()));
                    } else {
                        new File(history.getDestinationFile()).delete();
                    }
                    history.delete();
                    refreshRecyclerView();
                });
            }
        }
    }
}
