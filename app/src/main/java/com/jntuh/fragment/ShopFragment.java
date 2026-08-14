package com.jntuh.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jntuh.adapter.ShopCategoryAdapter;
import com.jntuh.adapter.ShopProductAdapter;
import com.jntuh.books.R;
import com.jntuh.books.ShopProductDetailActivity;
import com.jntuh.books.ShopWebActivity;
import com.jntuh.books.databinding.FragmentShopBinding;
import com.jntuh.item.ShopCategory;
import com.jntuh.item.ShopProduct;
import com.jntuh.response.ShopCategoryRP;
import com.jntuh.response.ShopLinksRP;
import com.jntuh.response.ShopProductRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Method;
import com.jntuh.util.StatusBarUtil;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Shop storefront: WooCommerce categories (chips) + product grid, fetched via
 * the JNTU Books backend proxy. Browsing only — tapping a product opens the
 * detail screen, and buying/tracking opens the website in a WebView.
 */
public class ShopFragment extends Fragment {

    private FragmentShopBinding binding;
    private Method method;

    private final List<ShopCategory> categories = new ArrayList<>();
    private final List<ShopProduct> products = new ArrayList<>();
    private ShopCategoryAdapter categoryAdapter;
    private ShopProductAdapter productAdapter;

    private String currentCategoryId = null; // null = All
    private String trackUrl = null;

    // Infinite scroll state.
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private static final int PAGE_SIZE = 20;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentShopBinding.inflate(inflater, container, false);
        StatusBarUtil.setStatusBar(requireActivity(), "");
        method = new Method(requireActivity());

        // Category chips (horizontal).
        categoryAdapter = new ShopCategoryAdapter(requireActivity(), categories, (category, position) -> {
            categoryAdapter.setSelected(position);
            currentCategoryId = category.getId(); // null for the "All" chip
            resetAndLoad();
        });
        binding.rvShopCategories.setLayoutManager(
                new LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvShopCategories.setAdapter(categoryAdapter);

        // Product grid (2-column staggered, like the feed).
        productAdapter = new ShopProductAdapter(requireActivity(), products, product -> {
            Intent i = new Intent(requireActivity(), ShopProductDetailActivity.class);
            i.putExtra("product_id", product.getId());
            i.putExtra("buy_url", product.getBuy_url());
            i.putExtra("category_id", currentCategoryId);
            startActivity(i);
        });
        StaggeredGridLayoutManager glm =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        glm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        binding.rvShopProducts.setLayoutManager(glm);
        binding.rvShopProducts.setAdapter(productAdapter);

        // Infinite scroll: load the next page as the user nears the end.
        binding.rvShopProducts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0 || isLoading || !hasMore || binding == null) return;
                StaggeredGridLayoutManager lm = (StaggeredGridLayoutManager) rv.getLayoutManager();
                if (lm == null) return;
                int total = lm.getItemCount();
                int[] lastPositions = lm.findLastVisibleItemPositions(null);
                int lastVisible = 0;
                for (int p : lastPositions) { lastVisible = Math.max(lastVisible, p); }
                if (lastVisible >= total - 4) {
                    loadProducts(false); // append next page
                }
            }
        });

        binding.swipeShop.setOnRefreshListener(() -> {
            loadCategories();
            resetAndLoad();
        });

        binding.btnTrackOrder.setOnClickListener(v -> openTrackOrder());

        loadShopLinks();
        loadCategories();
        resetAndLoad();

        return binding.getRoot();
    }

    /** Reset to page 1 (category change / refresh / first load). */
    private void resetAndLoad() {
        currentPage = 1;
        hasMore = true;
        loadProducts(true);
    }

    private String signedBody(JsonObject extra) {
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(requireActivity()));
        if (method.getIsLogin()) {
            jsObj.addProperty("user_id", method.getUserId());
        }
        if (extra != null) {
            for (String k : extra.keySet()) {
                jsObj.add(k, extra.get(k));
            }
        }
        return API.toBase64(jsObj.toString());
    }

    private void loadCategories() {
        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        Call<ShopCategoryRP> call = api.getShopCategories(signedBody(null));
        call.enqueue(new Callback<ShopCategoryRP>() {
            @Override
            public void onResponse(@NotNull Call<ShopCategoryRP> call, @NotNull Response<ShopCategoryRP> response) {
                if (getActivity() == null || binding == null) return;
                ShopCategoryRP body = response.body();
                if (body != null && "1".equals(body.getSuccess()) && body.getCategories() != null) {
                    categories.clear();
                    // Synthetic "All" chip first.
                    ShopCategory all = new Gson().fromJson("{\"id\":null,\"name\":\""
                            + getString(R.string.shop_all) + "\"}", ShopCategory.class);
                    categories.add(all);
                    categories.addAll(body.getCategories());
                    categoryAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(@NotNull Call<ShopCategoryRP> call, @NotNull Throwable t) {
                // Silent: product grid still works; categories just won't show.
            }
        });
    }

    private void loadProducts(final boolean reset) {
        if (binding == null || isLoading) return;
        isLoading = true;

        if (reset && products.isEmpty()) binding.progressShop.setVisibility(View.VISIBLE);
        binding.llShopEmpty.setVisibility(View.GONE);

        final int pageToLoad = reset ? 1 : currentPage + 1;

        JsonObject extra = new JsonObject();
        if (currentCategoryId != null && !currentCategoryId.isEmpty()) {
            extra.addProperty("category", currentCategoryId);
        }
        extra.addProperty("page", pageToLoad);

        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        Call<ShopProductRP> call = api.getShopProducts(signedBody(extra));
        call.enqueue(new Callback<ShopProductRP>() {
            @Override
            public void onResponse(@NotNull Call<ShopProductRP> call, @NotNull Response<ShopProductRP> response) {
                if (getActivity() == null || binding == null) return;
                isLoading = false;
                binding.progressShop.setVisibility(View.GONE);
                binding.swipeShop.setRefreshing(false);

                List<ShopProduct> incoming = null;
                ShopProductRP body = response.body();
                if (body != null && "1".equals(body.getSuccess())) {
                    incoming = body.getProducts();
                }

                if (reset) {
                    products.clear();
                }
                int added = 0;
                if (incoming != null && !incoming.isEmpty()) {
                    products.addAll(incoming);
                    added = incoming.size();
                    currentPage = pageToLoad;
                }
                // If we got a full page, assume there may be more.
                hasMore = (added >= PAGE_SIZE);

                productAdapter.notifyDataSetChanged();
                binding.llShopEmpty.setVisibility(products.isEmpty() ? View.VISIBLE : View.GONE);
                if (reset) binding.rvShopProducts.scrollToPosition(0);
            }

            @Override
            public void onFailure(@NotNull Call<ShopProductRP> call, @NotNull Throwable t) {
                if (getActivity() == null || binding == null) return;
                isLoading = false;
                binding.progressShop.setVisibility(View.GONE);
                binding.swipeShop.setRefreshing(false);
                binding.llShopEmpty.setVisibility(products.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void loadShopLinks() {
        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        Call<ShopLinksRP> call = api.getShopLinks(signedBody(null));
        call.enqueue(new Callback<ShopLinksRP>() {
            @Override
            public void onResponse(@NotNull Call<ShopLinksRP> call, @NotNull Response<ShopLinksRP> response) {
                ShopLinksRP body = response.body();
                if (body != null && body.getLinks() != null && !body.getLinks().isEmpty()) {
                    trackUrl = body.getLinks().get(0).getTrack_url();
                }
            }

            @Override
            public void onFailure(@NotNull Call<ShopLinksRP> call, @NotNull Throwable t) { }
        });
    }

    private void openTrackOrder() {
        String url = (trackUrl != null && !trackUrl.isEmpty())
                ? trackUrl : "https://madeforu.co.in/tracking-order/";
        Intent i = new Intent(requireActivity(), ShopWebActivity.class);
        i.putExtra("url", url);
        i.putExtra("title", getString(R.string.shop_track_order));
        startActivity(i);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
