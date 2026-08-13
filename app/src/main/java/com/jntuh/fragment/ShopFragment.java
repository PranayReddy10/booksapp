package com.jntuh.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentShopBinding.inflate(inflater, container, false);
        method = new Method(requireActivity());

        // Category chips (horizontal).
        categoryAdapter = new ShopCategoryAdapter(requireActivity(), categories, (category, position) -> {
            categoryAdapter.setSelected(position);
            currentCategoryId = category.getId(); // null for the "All" chip
            loadProducts();
        });
        binding.rvShopCategories.setLayoutManager(
                new LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvShopCategories.setAdapter(categoryAdapter);

        // Product grid (2-column staggered, like the feed).
        productAdapter = new ShopProductAdapter(requireActivity(), products, product -> {
            Intent i = new Intent(requireActivity(), ShopProductDetailActivity.class);
            i.putExtra("product_id", product.getId());
            i.putExtra("buy_url", product.getBuy_url());
            startActivity(i);
        });
        StaggeredGridLayoutManager glm =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        glm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        binding.rvShopProducts.setLayoutManager(glm);
        binding.rvShopProducts.setAdapter(productAdapter);

        binding.swipeShop.setOnRefreshListener(() -> {
            loadCategories();
            loadProducts();
        });

        binding.btnTrackOrder.setOnClickListener(v -> openTrackOrder());

        loadShopLinks();
        loadCategories();
        loadProducts();

        return binding.getRoot();
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

    private void loadProducts() {
        if (binding == null) return;
        if (products.isEmpty()) binding.progressShop.setVisibility(View.VISIBLE);
        binding.llShopEmpty.setVisibility(View.GONE);

        JsonObject extra = new JsonObject();
        if (currentCategoryId != null && !currentCategoryId.isEmpty()) {
            extra.addProperty("category", currentCategoryId);
        }

        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        Call<ShopProductRP> call = api.getShopProducts(signedBody(extra));
        call.enqueue(new Callback<ShopProductRP>() {
            @Override
            public void onResponse(@NotNull Call<ShopProductRP> call, @NotNull Response<ShopProductRP> response) {
                if (getActivity() == null || binding == null) return;
                binding.progressShop.setVisibility(View.GONE);
                binding.swipeShop.setRefreshing(false);
                products.clear();
                ShopProductRP body = response.body();
                if (body != null && "1".equals(body.getSuccess()) && body.getProducts() != null) {
                    products.addAll(body.getProducts());
                }
                productAdapter.notifyDataSetChanged();
                binding.llShopEmpty.setVisibility(products.isEmpty() ? View.VISIBLE : View.GONE);
                binding.rvShopProducts.scrollToPosition(0);
            }

            @Override
            public void onFailure(@NotNull Call<ShopProductRP> call, @NotNull Throwable t) {
                if (getActivity() == null || binding == null) return;
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
