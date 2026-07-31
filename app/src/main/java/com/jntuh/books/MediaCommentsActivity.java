package com.jntuh.books;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.jntuh.adapter.MediaCommentAdapter;
import com.jntuh.books.databinding.ActivityMediaCommentsBinding;
import com.jntuh.item.MediaCommentItem;
import com.jntuh.response.MediaCommentListRP;
import com.jntuh.response.MediaCommentRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Method;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MediaCommentsActivity extends AppCompatActivity
        implements MediaCommentAdapter.OnCommentLongClickListener {

    public static final String EXTRA_POST_ID = "post_id";
    public static final String EXTRA_ALLOW_COMMENTS = "allow_comments";

    private ActivityMediaCommentsBinding binding;
    private Method method;

    private String postId;
    private boolean commentsAllowed = true;

    private final List<MediaCommentItem> comments = new ArrayList<>();
    private MediaCommentAdapter adapter;
    private LinearLayoutManager layoutManager;

    private int pageIndex = 1;
    private int totalPages = 1;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMediaCommentsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        method = new Method(this);
        method.forceRTLIfSupported();

        postId = getIntent().getStringExtra(EXTRA_POST_ID);
        commentsAllowed = !"0".equals(getIntent().getStringExtra(EXTRA_ALLOW_COMMENTS));

        binding.toolbarMain.tvToolbarTitle.setText(getString(R.string.lbl_comments));
        binding.toolbarMain.ivSearch.setVisibility(View.GONE);
        binding.toolbarMain.imageFilter.setVisibility(View.GONE);
        binding.toolbarMain.imageArrowBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        layoutManager = new LinearLayoutManager(this);
        binding.rvComments.setLayoutManager(layoutManager);
        adapter = new MediaCommentAdapter(this, comments, method.getUserId(), this);
        binding.rvComments.setAdapter(adapter);

        // Hide the input bar entirely if comments are disabled for this post.
        if (!commentsAllowed) {
            binding.llInputBar.setVisibility(View.GONE);
        }

        binding.btnSendComment.setOnClickListener(v -> submitComment());

        binding.rvComments.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NotNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (dy <= 0) return;
                int last = layoutManager.findLastVisibleItemPosition();
                if (!isLoading && pageIndex < totalPages && last >= comments.size() - 3) {
                    pageIndex++;
                    loadComments(false);
                }
            }
        });

        if (postId != null && method.isNetworkAvailable()) {
            loadComments(true);
        } else if (postId == null) {
            finish();
        } else {
            method.alertBox(getString(R.string.internet_connection));
        }
    }

    private void loadComments(boolean first) {
        if (isLoading) return;
        isLoading = true;
        if (first) binding.progressComments.setVisibility(View.VISIBLE);
        binding.tvNoComments.setVisibility(View.GONE);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        jsObj.addProperty("post_id", postId);
        jsObj.addProperty("page", pageIndex);

        ApiClient.getClient().create(ApiInterface.class)
                .getMediaCommentListData(API.toBase64(jsObj.toString()))
                .enqueue(new Callback<MediaCommentListRP>() {
                    @Override
                    public void onResponse(@NotNull Call<MediaCommentListRP> call, @NotNull Response<MediaCommentListRP> response) {
                        isLoading = false;
                        binding.progressComments.setVisibility(View.GONE);
                        try {
                            MediaCommentListRP body = response.body();
                            if (body != null && "1".equals(body.getSuccess())) {
                                totalPages = Math.max(body.getTotal_pages(), 1);
                                int start = comments.size();
                                if (body.getCommentItems() != null) comments.addAll(body.getCommentItems());
                                if (start == 0) adapter.notifyDataSetChanged();
                                else adapter.notifyItemRangeInserted(start, comments.size() - start);
                            }
                            binding.tvNoComments.setVisibility(comments.isEmpty() ? View.VISIBLE : View.GONE);
                        } catch (Exception e) {
                            Log.d("exception_error", e.toString());
                            binding.tvNoComments.setVisibility(comments.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<MediaCommentListRP> call, @NotNull Throwable t) {
                        Log.e("fail", t.toString());
                        isLoading = false;
                        binding.progressComments.setVisibility(View.GONE);
                        binding.tvNoComments.setVisibility(comments.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                });
    }

    private void submitComment() {
        if (!method.getIsLogin()) {
            startActivity(new android.content.Intent(this, LoginActivity.class));
            return;
        }
        String text = binding.edtComment.getText().toString().trim();
        if (text.isEmpty()) return;
        if (!method.isNetworkAvailable()) {
            method.alertBox(getString(R.string.internet_connection));
            return;
        }

        binding.btnSendComment.setEnabled(false);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        jsObj.addProperty("user_id", method.getUserId());
        jsObj.addProperty("post_id", postId);
        jsObj.addProperty("comment", text);

        ApiClient.getClient().create(ApiInterface.class)
                .getMediaCommentAddData(API.toBase64(jsObj.toString()))
                .enqueue(new Callback<MediaCommentRP>() {
                    @Override
                    public void onResponse(@NotNull Call<MediaCommentRP> call, @NotNull Response<MediaCommentRP> response) {
                        binding.btnSendComment.setEnabled(true);
                        MediaCommentRP body = response.body();
                        if (body != null && body.getItemCommentList() != null && !body.getItemCommentList().isEmpty()) {
                            MediaCommentRP.ItemComment r = body.getItemCommentList().get(0);
                            if ("1".equals(r.getSuccess())) {
                                // Prepend the new comment (newest first).
                                MediaCommentItem newItem = new MediaCommentItem();
                                newItem.setComment_id(r.getComment_id());
                                newItem.setUser_id(method.getUserId());
                                newItem.setUser_name(method.getUserName());
                                newItem.setUser_image(method.getUserImage());
                                newItem.setComment(text);
                                newItem.setCreated_at(nowStamp());
                                comments.add(0, newItem);
                                adapter.notifyItemInserted(0);
                                binding.rvComments.scrollToPosition(0);
                                binding.edtComment.setText("");
                                hideKeyboard();
                                binding.tvNoComments.setVisibility(View.GONE);
                            } else {
                                Toast.makeText(MediaCommentsActivity.this,
                                        r.getMsg() != null ? r.getMsg() : getString(R.string.comments_disabled),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<MediaCommentRP> call, @NotNull Throwable t) {
                        Log.e("fail", t.toString());
                        binding.btnSendComment.setEnabled(true);
                        method.alertBox(getString(R.string.failed_try_again));
                    }
                });
    }

    @Override
    public void onLongClick(MediaCommentItem item, int position) {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.confirm_delete_comment))
                .setPositiveButton(android.R.string.ok, (d, w) -> deleteComment(item, position))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void deleteComment(MediaCommentItem item, int position) {
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        jsObj.addProperty("user_id", method.getUserId());
        jsObj.addProperty("comment_id", item.getComment_id());

        ApiClient.getClient().create(ApiInterface.class)
                .getMediaCommentDeleteData(API.toBase64(jsObj.toString()))
                .enqueue(new Callback<MediaCommentRP>() {
                    @Override
                    public void onResponse(@NotNull Call<MediaCommentRP> call, @NotNull Response<MediaCommentRP> response) {
                        MediaCommentRP body = response.body();
                        boolean ok = body != null && body.getItemCommentList() != null
                                && !body.getItemCommentList().isEmpty()
                                && "1".equals(body.getItemCommentList().get(0).getSuccess());
                        if (ok && position >= 0 && position < comments.size()) {
                            comments.remove(position);
                            adapter.notifyItemRemoved(position);
                            adapter.notifyItemRangeChanged(position, comments.size());
                            binding.tvNoComments.setVisibility(comments.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(@NotNull Call<MediaCommentRP> call, @NotNull Throwable t) {
                        Log.e("fail", t.toString());
                        method.alertBox(getString(R.string.failed_try_again));
                    }
                });
    }

    private String nowStamp() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                .format(new java.util.Date());
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null && getCurrentFocus() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        } catch (Exception ignored) {
        }
    }
}
