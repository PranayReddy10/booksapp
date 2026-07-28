package com.jntuh.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.jntuh.books.LoginActivity;
import com.jntuh.books.R;
import com.jntuh.books.databinding.RowRateReviewBinding;
import com.jntuh.item.RateReviewList;
import com.jntuh.response.PostRateRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Method;
import com.jntuh.util.OnClick;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class RateReviewListAdapter extends RecyclerView.Adapter<RateReviewListAdapter.ViewHolder> {

    Activity activity;
    List<RateReviewList> rateReviewLists;
    OnClick onClick;
    Method method;
    String postId;
    ProgressDialog progressDialog;

    public RateReviewListAdapter(Activity activity, String postId) {
        this.activity = activity;
        method = new Method(activity);
        this.postId = postId;
        progressDialog = new ProgressDialog(activity, R.style.MyAlertDialogStyle);
    }

    public void setList(List<RateReviewList> rateReviewLists) {
        this.rateReviewLists = rateReviewLists;
        notifyDataSetChanged();

    }

    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new ViewHolder(RowRateReviewBinding.inflate(activity.getLayoutInflater()));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        RateReviewList rateReviewList = rateReviewLists.get(position);
        Glide.with(activity.getApplicationContext()).load(rateReviewList.getUser_image().isEmpty()
                        ? R.drawable.user_profile : rateReviewList.getUser_image())
                .placeholder(R.drawable.user_profile)
                .into(holder.rowRateReviewBinding.ivUser);

        holder.rowRateReviewBinding.tvUserName.setText(rateReviewList.getUser_name());
        holder.rowRateReviewBinding.tvTime.setText(rateReviewList.getDate());
        holder.rowRateReviewBinding.ratingView.setRating(Float.parseFloat(rateReviewList.getRate()));
        holder.rowRateReviewBinding.tvReviewMsg.setText(rateReviewList.getReview_text());

        holder.rowRateReviewBinding.layOverFlow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moreMenuDialog(rateReviewList);
            }
        });

    }

    @Override
    public int getItemCount() {
        return rateReviewLists.size();
    }

    public void setOnItemClickListener(OnClick clickListener) {
        this.onClick = clickListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        RowRateReviewBinding rowRateReviewBinding;

        public ViewHolder(RowRateReviewBinding rowRateReviewBinding) {
            super(rowRateReviewBinding.getRoot());
            this.rowRateReviewBinding = rowRateReviewBinding;
        }
    }

    public void deleteCmtDialog(String userId, String reviewId) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        dialog.setContentView(R.layout.layout_delete_comment);
        if (method.isRtl()) {
            dialog.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }

        ImageView ivClose = dialog.findViewById(R.id.ivCloseReport);
        AppCompatButton buttonYes = dialog.findViewById(R.id.btnSubmit);

        assert ivClose != null;
        ivClose.setOnClickListener(v -> dialog.dismiss());

        assert buttonYes != null;
        buttonYes.setOnClickListener(v -> {
            dialog.dismiss();
            sendCommentDeleteData(userId, reviewId);
        });

        dialog.show();
    }

    private void sendCommentDeleteData(String userId, String reviewId) {

        progressDialog.show();
        progressDialog.setMessage(activity.getResources().getString(R.string.loading));
        progressDialog.setCancelable(false);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(activity));
        jsObj.addProperty("user_id", userId);
        jsObj.addProperty("post_type", "Book");
        jsObj.addProperty("review_id", reviewId);
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<PostRateRP> call = apiService.getDeleteCommentData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<PostRateRP>() {
            @Override
            public void onResponse(@NotNull Call<PostRateRP> call, @NotNull Response<PostRateRP> response) {
                try {

                    PostRateRP postRateRP = response.body();
                    if (postRateRP !=null && postRateRP.getSuccess().equals("1")) {
                        if (postRateRP.getPostRateLists().size() != 0) {
                            method.alertBox(postRateRP.getPostRateLists().get(0).getMsg());
                        } else {
                            progressDialog.dismiss();
                            method.alertBox(activity.getResources().getString(R.string.failed_try_again));
                        }
                    } else {
                        progressDialog.dismiss();
                        method.alertBox(activity.getResources().getString(R.string.failed_try_again));
                    }

                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                    method.alertBox(activity.getResources().getString(R.string.failed_try_again));
                }
                progressDialog.dismiss();
            }


            @Override
            public void onFailure(@NotNull Call<PostRateRP> call, @NotNull Throwable t) {
                // Log error here since request failed
                Log.e("fail", t.toString());
                progressDialog.dismiss();
                method.alertBox(activity.getResources().getString(R.string.failed_try_again));
            }
        });

    }

    public void reportCmtDialog(String userId, String postId, String reviewId) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        dialog.setContentView(R.layout.layout_report_comment);
        if (method.isRtl()) {
            dialog.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }

        ImageView ivClose = dialog.findViewById(R.id.ivCloseReport);
        AppCompatButton buttonYes = dialog.findViewById(R.id.btnSubmit);
        EditText editTextReport = dialog.findViewById(R.id.etYourReviewText);

        ivClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        buttonYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editTextReport.getText().toString().isEmpty()) {
                    Toast.makeText(activity, activity.getString(R.string.lbl_detail_report_msg), Toast.LENGTH_SHORT).show();
                } else {
                    dialog.dismiss();
                    sendCommentReportData(userId, postId, "Book", reviewId, editTextReport.getText().toString());
                }
            }
        });

        dialog.show();
    }

    private void sendCommentReportData(String userId, String postId, String postType, String reviewId, String reportMsg) {

        progressDialog.show();
        progressDialog.setMessage(activity.getResources().getString(R.string.loading));
        progressDialog.setCancelable(false);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(activity));
        jsObj.addProperty("post_id", postId);
        jsObj.addProperty("user_id", userId);
        jsObj.addProperty("post_type", postType);
        jsObj.addProperty("review_id", reviewId);
        jsObj.addProperty("message", reportMsg);
        ApiInterface apiService = ApiClient.getClient().create(ApiInterface.class);
        Call<PostRateRP> call = apiService.getReportCommentData(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<PostRateRP>() {
            @Override
            public void onResponse(@NotNull Call<PostRateRP> call, @NotNull Response<PostRateRP> response) {
                try {

                    PostRateRP postRateRP = response.body();

                    if (postRateRP !=null && postRateRP.getSuccess().equals("1")) {
                        if (postRateRP.getPostRateLists().size() != 0) {
                            method.alertBox(postRateRP.getPostRateLists().get(0).getMsg());
                        } else {
                            progressDialog.dismiss();
                            method.alertBox(activity.getResources().getString(R.string.failed_try_again));
                        }
                    } else {
                        progressDialog.dismiss();
                        method.alertBox(activity.getResources().getString(R.string.failed_try_again));
                    }

                } catch (Exception e) {
                    Log.d("exception_error", e.toString());
                    method.alertBox(activity.getResources().getString(R.string.failed_try_again));
                }
                progressDialog.dismiss();
            }


            @Override
            public void onFailure(@NotNull Call<PostRateRP> call, @NotNull Throwable t) {
                // Log error here since request failed
                Log.e("fail", t.toString());
                progressDialog.dismiss();
                method.alertBox(activity.getResources().getString(R.string.failed_try_again));
            }
        });

    }

    public void moreMenuDialog(RateReviewList rateReviewList) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        dialog.setContentView(R.layout.layout_bottom_more_review);
        if (method.isRtl()) {
            dialog.getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }

        RelativeLayout layReport = dialog.findViewById(R.id.layReport);
        RelativeLayout layDelete = dialog.findViewById(R.id.layDelete);

        assert layReport != null;
        assert layDelete != null;

        if (method.getUserId().equals(rateReviewList.getUser_id())) {
            layDelete.setVisibility(View.VISIBLE);
            layReport.setVisibility(View.GONE);
        } else {
            layDelete.setVisibility(View.GONE);
            layReport.setVisibility(View.VISIBLE);
        }


        layDelete.setOnClickListener(v -> {
            dialog.dismiss();
            if (method.getIsLogin()) {
                deleteCmtDialog(method.getUserId(), rateReviewList.getReview_id());
            } else {
                Toast.makeText(activity, activity.getString(R.string.login_require), Toast.LENGTH_SHORT).show();
                Intent intentLogin = new Intent(activity, LoginActivity.class);
                intentLogin.putExtra("isFromDetail", true);
                activity.startActivity(intentLogin);
            }
        });

        layReport.setOnClickListener(v -> {
            dialog.dismiss();
            if (method.getIsLogin()) {
                reportCmtDialog(method.getUserId(), postId, rateReviewList.getReview_id());
            } else {
                Toast.makeText(activity, activity.getString(R.string.login_require), Toast.LENGTH_SHORT).show();
                Intent intentLogin = new Intent(activity, LoginActivity.class);
                intentLogin.putExtra("isFromDetail", true);
                activity.startActivity(intentLogin);
            }
        });

        dialog.show();
    }
}
