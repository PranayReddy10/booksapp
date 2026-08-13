package com.jntuh.books;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jntuh.adapter.ShopImageAdapter;
import com.jntuh.books.databinding.ActivityShopProductDetailBinding;
import com.jntuh.item.ShopProduct;
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
 * Native product detail: image carousel, price, description. The "Buy /
 * Personalise" button hands off to the website (ShopWebActivity) where the
 * existing photo-personalisation + cart + checkout + payment flow runs.
 */
public class ShopProductDetailActivity extends AppCompatActivity {

    private ActivityShopProductDetailBinding binding;
    private Method method;

    private String productId;
    private String buyUrl;
    private final List<String> images = new ArrayList<>();

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityShopProductDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        method = new Method(this);

        productId = getIntent().getStringExtra("product_id");
        buyUrl = getIntent().getStringExtra("buy_url");

        binding.toolbarMain.tvToolbarTitle.setText(getString(R.string.shop_title));
        binding.toolbarMain.ivSearch.setVisibility(View.GONE);
        binding.toolbarMain.imageFilter.setVisibility(View.GONE);
        binding.toolbarMain.imageArrowBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        binding.btnBuyNow.setOnClickListener(v -> openBuy());

        binding.vpImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateCounter(position);
            }
        });

        loadDetail();
    }

    private void updateCounter(int position) {
        if (images.size() > 1) {
            binding.tvImageCounter.setVisibility(View.VISIBLE);
            binding.tvImageCounter.setText((position + 1) + "/" + images.size());
        } else {
            binding.tvImageCounter.setVisibility(View.GONE);
        }
    }

    @SuppressLint("SetTextI18n")
    private void bind(ShopProduct p) {
        binding.tvDetailName.setText(p.getName());
        String cur = p.getCurrency();
        binding.tvDetailPrice.setText(cur + (p.getPrice() == null ? "" : p.getPrice()));

        if (p.isOnSale() && p.getRegular_price() != null && !p.getRegular_price().isEmpty()
                && !p.getRegular_price().equals(p.getPrice())) {
            binding.tvDetailRegular.setVisibility(View.VISIBLE);
            binding.tvDetailRegular.setText(cur + p.getRegular_price());
            binding.tvDetailRegular.setPaintFlags(
                    binding.tvDetailRegular.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            binding.tvDetailRegular.setVisibility(View.GONE);
        }

        if (p.getDiscount() != null && !p.getDiscount().isEmpty()) {
            binding.tvDetailDiscount.setVisibility(View.VISIBLE);
            binding.tvDetailDiscount.setText(p.getDiscount());
        } else {
            binding.tvDetailDiscount.setVisibility(View.GONE);
        }

        binding.tvDetailStock.setVisibility(p.isOutOfStock() ? View.VISIBLE : View.GONE);

        String desc = p.getDescription();
        if (desc == null || desc.trim().isEmpty()) desc = p.getShort_desc();
        binding.tvDetailDesc.setText(desc == null ? "" : desc.trim());

        images.clear();
        if (p.getImages() != null && !p.getImages().isEmpty()) {
            images.addAll(p.getImages());
        } else if (p.getThumb() != null) {
            images.add(p.getThumb());
        }
        binding.vpImages.setAdapter(new ShopImageAdapter(this, images));
        updateCounter(0);

        if (p.getBuy_url() != null && !p.getBuy_url().isEmpty()) {
            buyUrl = p.getBuy_url();
        }
    }

    private void loadDetail() {
        binding.progressDetail.setVisibility(View.VISIBLE);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        if (method.getIsLogin()) jsObj.addProperty("user_id", method.getUserId());
        jsObj.addProperty("product_id", productId);

        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        Call<ShopProductRP> call = api.getShopProductDetail(API.toBase64(jsObj.toString()));
        call.enqueue(new Callback<ShopProductRP>() {
            @Override
            public void onResponse(@NotNull Call<ShopProductRP> call, @NotNull Response<ShopProductRP> response) {
                if (isFinishing() || binding == null) return;
                binding.progressDetail.setVisibility(View.GONE);
                ShopProductRP body = response.body();
                if (body != null && "1".equals(body.getSuccess())
                        && body.getProducts() != null && !body.getProducts().isEmpty()) {
                    bind(body.getProducts().get(0));
                }
            }

            @Override
            public void onFailure(@NotNull Call<ShopProductRP> call, @NotNull Throwable t) {
                if (isFinishing() || binding == null) return;
                binding.progressDetail.setVisibility(View.GONE);
            }
        });
    }

    private void openBuy() {
        String url = (buyUrl != null && !buyUrl.isEmpty()) ? buyUrl : "https://madeforu.co.in/shop/";
        Intent i = new Intent(this, ShopWebActivity.class);
        i.putExtra("url", url);
        i.putExtra("title", getString(R.string.shop_buy_now));
        startActivity(i);
    }
}
