package com.avrapps.pdfviewer.tools_fragment.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.avrapps.pdfviewer.MainActivity;
import com.avrapps.pdfviewer.R;
import com.avrapps.pdfviewer.tools_fragment.data.Tool;

import java.util.List;

public class SquareBoxesAdapter extends RecyclerView.Adapter<SquareBoxesAdapter.ViewHolder> {

    private final LayoutInflater mInflater;
    Activity activity;
    List<Tool> tools;

    public SquareBoxesAdapter(FragmentActivity activity, List<Tool> tools) {
        this.activity = activity;
        this.mInflater = LayoutInflater.from(activity);
        this.tools = tools;
    }

    private void clickHandler(int position) {
        ((MainActivity) activity).performOperation(tools.get(position).getOperationCode());
    }


    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_toolname, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each cell
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(tools.get(position).getToolName());
        holder.imageView.setImageResource(tools.get(position).getDrawable());
        holder.itemView.setBackgroundResource(tools.get(position).getBackground());
    }

    @Override
    public int getItemCount() {
        return tools.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ImageView imageView;
        View itemView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView);
            imageView = itemView.findViewById(R.id.imageView);
            this.itemView = itemView;
            itemView.setOnClickListener(v -> clickHandler(getAdapterPosition()));
        }
    }
}