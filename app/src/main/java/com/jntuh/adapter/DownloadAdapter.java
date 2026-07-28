package com.jntuh.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jntuh.books.R;
import com.jntuh.books.databinding.RowDownloadBinding;
import com.jntuh.item.DownloadList;
import com.jntuh.util.AdInterstitialAds;
import com.jntuh.util.DatabaseHandler;
import com.jntuh.util.Method;
import com.jntuh.util.OnClick;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.ViewHolder> {

    Method method;
    Activity activity;
    DatabaseHandler db;
    List<DownloadList> downloadLists;
    OnClick onClick;
    int columnWidth;

    public DownloadAdapter(Activity activity, List<DownloadList> downloadLists) {
        this.activity = activity;
        this.downloadLists = downloadLists;
        db = new DatabaseHandler(activity);
        method = new Method(activity);
        Resources r = activity.getResources();
        float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, r.getDisplayMetrics());
        columnWidth = (int) ((method.getScreenWidth() - ((3 + 1) * padding)));
    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new ViewHolder(RowDownloadBinding.inflate(activity.getLayoutInflater()));

    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NotNull ViewHolder holder, final int position) {

        holder.rowDownloadBinding.llHomeBook.setLayoutParams(new LinearLayout.LayoutParams(columnWidth / 3, columnWidth / 2));
        holder.rowDownloadBinding.tvHomeConTitle.setText(downloadLists.get(position).getTitle());

        Glide.with(activity.getApplicationContext()).load("file://" + downloadLists.get(position).getImage())
                .placeholder(R.drawable.placeholder_portable)
                .into(holder.rowDownloadBinding.ivHomeCont);

        holder.rowDownloadBinding.rlFav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AdInterstitialAds.ShowInterstitialAds(activity, holder.getBindingAdapterPosition(), onClick);
            }
        });


        holder.rowDownloadBinding.cvDelete.setOnClickListener(v -> {
            Dialog dialog = new Dialog(activity, R.style.Theme_AppCompat_Translucent);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_rent_msg);
            dialog.setCancelable(false);
            if (method.isRtl()) {
                dialog.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            }
            dialog.getWindow().setLayout(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.MATCH_PARENT);
            TextView tvMsg = dialog.findViewById(R.id.txtPaidMsg);
            AppCompatButton btnBuy = dialog.findViewById(R.id.btnPaid);
            ImageView ivClose = dialog.findViewById(R.id.ivCloseReport);

            ivClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });

            tvMsg.setText(activity.getString(R.string.delete_msg));
            btnBuy.setText(activity.getString(R.string.lbl_delete));

            btnBuy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                        if (!db.checkIdDownloadBook(downloadLists.get(position).getId())) {
                            db.deleteDownloadBook(downloadLists.get(position).getId());
                            File file = new File(downloadLists.get(position).getUrl());
                            File file_image = new File(downloadLists.get(position).getImage());
                            file.delete();
                            file_image.delete();
                            downloadLists.remove(position);
                            notifyDataSetChanged();
                        } else {
                            Toast.makeText(activity, activity.getResources().getString(R.string.wrong), Toast.LENGTH_SHORT).show();
                        }
                }
            });

            dialog.show();
        });

    }

    @Override
    public int getItemCount() {
        return downloadLists.size();
    }

    public void setOnItemClickListener(OnClick clickListener) {
        this.onClick = clickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        RowDownloadBinding rowDownloadBinding;

        public ViewHolder(RowDownloadBinding rowDownloadBinding) {
            super(rowDownloadBinding.getRoot());
            this.rowDownloadBinding = rowDownloadBinding;
        }
    }
}
