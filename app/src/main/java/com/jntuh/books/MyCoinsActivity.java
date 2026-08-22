package com.jntuh.books;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jntuh.books.databinding.ActivityMyCoinsBinding;
import com.jntuh.item.CoinBookItem;
import com.jntuh.item.CoinCardItem;
import com.jntuh.item.CoinSummaryItem;
import com.jntuh.response.CoinCardRP;
import com.jntuh.response.CoinSummaryRP;
import com.jntuh.rest.ApiClient;
import com.jntuh.rest.ApiInterface;
import com.jntuh.util.API;
import com.jntuh.util.Method;
import com.jntuh.util.StatusBarUtil;

import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * My Coins: the balance, what earned it, and the gift cards it has been spent
 * on. Everything on the screen comes from coins_summary plus coins_cards, so a
 * refresh is two calls and no local bookkeeping.
 */
public class MyCoinsActivity extends AppCompatActivity {

    private ActivityMyCoinsBinding v;
    private Method method;
    private CoinSummaryItem summary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        v = ActivityMyCoinsBinding.inflate(getLayoutInflater());
        setContentView(v.getRoot());

        method = new Method(this);
        method.forceRTLIfSupported();
        StatusBarUtil.setStatusBar(this, "home");

        v.ivCoinsBack.setOnClickListener(view -> finish());
        v.btnRedeemCoins.setOnClickListener(view -> confirmRedeem());

        load();
    }

    private void load() {
        // Anything that goes wrong here shows a line on the screen. Closing the
        // activity instead just looked like the app bouncing straight back.
        if (!method.getIsLogin()) {
            showMessage(getString(R.string.result_login_needed));
            return;
        }
        if (!method.isNetworkAvailable()) {
            showMessage(getString(R.string.internet_connection));
            return;
        }

        v.progressCoins.setVisibility(View.VISIBLE);
        v.svCoins.setVisibility(View.GONE);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        jsObj.addProperty("user_id", method.getUserId());

        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        api.getCoinsSummary(API.toBase64(jsObj.toString())).enqueue(new Callback<CoinSummaryRP>() {
            @Override
            public void onResponse(@NotNull Call<CoinSummaryRP> call, @NotNull Response<CoinSummaryRP> resp) {
                v.progressCoins.setVisibility(View.GONE);
                try {
                    CoinSummaryRP body = resp.body();
                    if (body != null && body.getEbookApp() != null && !body.getEbookApp().isEmpty()) {
                        summary = body.getEbookApp().get(0);
                        if (summary.getEnabled() == 1) {
                            showContent();
                            render(summary);
                            loadCards();
                            return;
                        }
                        // Two different problems: the admin switched it off, or
                        // this server has not run the coins migration.
                        String fallback = "setup".equals(summary.getReason())
                                ? getString(R.string.msg_coins_setup_pending)
                                : getString(R.string.msg_coins_disabled);
                        showMessage(nn(summary.getMsg(), fallback));
                        return;
                    }
                } catch (Exception e) {
                    Log.d("coins_error", e.toString());
                }
                // No usable body: usually the server does not have the coins
                // endpoints yet, which is a deploy state, not a user error.
                showMessage(getString(R.string.msg_coins_disabled));
            }

            @Override
            public void onFailure(@NotNull Call<CoinSummaryRP> call, @NotNull Throwable t) {
                v.progressCoins.setVisibility(View.GONE);
                Log.e("coins_fail", t.toString());
                showMessage(getString(R.string.failed_try_again));
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void render(CoinSummaryItem s) {
        String currency = s.getCurrency() == null ? "" : s.getCurrency();

        v.tvCoinBalance.setText(NumberFormat.getInstance().format(s.getBalance()));
        v.tvCoinValue.setText(getString(R.string.msg_coin_worth, currency, nn(s.getBalance_value(), "0")));
        v.tvCoinRates.setText(getString(R.string.msg_coin_rates, s.getCoins_per_read(), s.getCoins_per_upload()));
        v.tvCoinStepSpend.setText(s.getCan_redeem() == 1
                ? getString(R.string.msg_coin_step_spend_ready)
                : getString(R.string.msg_coin_step_spend, s.getMin_redeem()));

        // Below the minimum the button would only ever fail, so say what is
        // missing instead of offering it.
        boolean canRedeem = s.getCan_redeem() == 1;
        v.btnRedeemCoins.setVisibility(canRedeem ? View.VISIBLE : View.GONE);
        v.tvRedeemHint.setVisibility(canRedeem ? View.GONE : View.VISIBLE);
        if (!canRedeem) {
            v.tvRedeemHint.setText(getString(R.string.msg_coin_need_more, s.getMin_redeem()));
        }

        v.llCoinBooks.removeAllViews();
        List<CoinBookItem> books = s.getBooks();
        boolean any = books != null && !books.isEmpty();
        v.tvCoinBooksEmpty.setVisibility(any ? View.GONE : View.VISIBLE);

        if (any) {
            LayoutInflater inflater = getLayoutInflater();
            for (CoinBookItem b : books) {
                View row = inflater.inflate(R.layout.row_coin_book, v.llCoinBooks, false);

                TextView title = row.findViewById(R.id.tvCoinBookTitle);
                TextView stats = row.findViewById(R.id.tvCoinBookStats);
                TextView coins = row.findViewById(R.id.tvCoinBookCoins);
                ImageView cover = row.findViewById(R.id.ivCoinBookCover);

                title.setText(nn(b.getTitle(), "—"));
                stats.setText(getString(R.string.msg_coin_stats,
                        NumberFormat.getInstance().format(b.getViews()), b.getReads()));
                coins.setText("+" + b.getCoins());

                if (b.getImage() != null && !b.getImage().trim().isEmpty()) {
                    Glide.with(this).load(b.getImage()).into(cover);
                }

                v.llCoinBooks.addView(row);
            }
        }
    }

    private void loadCards() {
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        jsObj.addProperty("user_id", method.getUserId());

        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        api.getCoinCards(API.toBase64(jsObj.toString())).enqueue(new Callback<CoinCardRP>() {
            @Override
            public void onResponse(@NotNull Call<CoinCardRP> call, @NotNull Response<CoinCardRP> resp) {
                try {
                    CoinCardRP body = resp.body();
                    if (body != null && body.getEbookApp() != null) {
                        renderCards(body.getEbookApp());
                    }
                } catch (Exception e) {
                    Log.d("cards_error", e.toString());
                }
            }

            @Override
            public void onFailure(@NotNull Call<CoinCardRP> call, @NotNull Throwable t) {
                Log.e("cards_fail", t.toString());   // the rest of the screen still stands
            }
        });
    }

    private void renderCards(List<CoinCardItem> cards) {
        v.llCoinCards.removeAllViews();

        boolean any = false;
        LayoutInflater inflater = getLayoutInflater();
        for (CoinCardItem c : cards) {
            // The "no gift cards yet" placeholder comes back as a row with no id.
            if (c.getRedemption_id() == null || c.getRedemption_id().trim().isEmpty()) continue;
            any = true;

            View row = inflater.inflate(R.layout.row_coin_card, v.llCoinCards, false);
            TextView code = row.findViewById(R.id.tvCardCode);
            TextView meta = row.findViewById(R.id.tvCardMeta);
            TextView copy = row.findViewById(R.id.tvCardCopy);

            boolean issued = c.getCode() != null && !c.getCode().trim().isEmpty();
            code.setText(issued ? c.getCode() : getString(R.string.msg_card_pending));
            meta.setText((summary != null && summary.getCurrency() != null ? summary.getCurrency() : "")
                    + nn(c.getAmount(), "0") + "  ·  " + c.getCoins() + "  ·  " + nn(c.getDate(), ""));

            copy.setVisibility(issued ? View.VISIBLE : View.GONE);
            copy.setOnClickListener(view -> copyCode(c.getCode()));

            v.llCoinCards.addView(row);
        }

        v.tvCoinCardsHeader.setVisibility(any ? View.VISIBLE : View.GONE);
    }

    private void copyCode(String code) {
        ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cb == null) return;
        cb.setPrimaryClip(ClipData.newPlainText("gift card", code));
        Toast.makeText(this, getString(R.string.msg_code_copied), Toast.LENGTH_SHORT).show();
    }

    /**
     * Redeeming spends the whole balance — a partial cash-out would need an
     * amount picker for no real gain, since coins keep accruing anyway.
     */
    private void confirmRedeem() {
        if (summary == null || summary.getCan_redeem() != 1) return;

        String currency = summary.getCurrency() == null ? "" : summary.getCurrency();
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.lbl_redeem_gift_card)
                .setMessage(getString(R.string.msg_redeem_confirm,
                        summary.getBalance(), currency, nn(summary.getBalance_value(), "0")))
                .setPositiveButton(R.string.lbl_redeem_now, (d, w) -> redeem(summary.getBalance()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void redeem(int coins) {
        if (!method.isNetworkAvailable()) {
            method.alertBox(getString(R.string.internet_connection));
            return;
        }
        v.progressCoins.setVisibility(View.VISIBLE);

        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(new API(this));
        jsObj.addProperty("user_id", method.getUserId());
        jsObj.addProperty("coins", coins);

        ApiInterface api = ApiClient.getClient().create(ApiInterface.class);
        api.redeemCoins(API.toBase64(jsObj.toString())).enqueue(new Callback<CoinCardRP>() {
            @Override
            public void onResponse(@NotNull Call<CoinCardRP> call, @NotNull Response<CoinCardRP> resp) {
                v.progressCoins.setVisibility(View.GONE);

                CoinCardItem item = null;
                CoinCardRP body = resp.body();
                if (body != null && body.getEbookApp() != null && !body.getEbookApp().isEmpty()) {
                    item = body.getEbookApp().get(0);
                }
                if (item == null) {
                    method.alertBox(getString(R.string.failed_try_again));
                    return;
                }

                method.alertBox(nn(item.getMsg(), getString(R.string.failed_try_again)));
                load();   // balance, books and cards all move together
            }

            @Override
            public void onFailure(@NotNull Call<CoinCardRP> call, @NotNull Throwable t) {
                v.progressCoins.setVisibility(View.GONE);
                Log.e("redeem_fail", t.toString());
                method.alertBox(getString(R.string.failed_try_again));
            }
        });
    }

    private void showMessage(String text) {
        v.svCoins.setVisibility(View.GONE);
        v.progressCoins.setVisibility(View.GONE);
        v.tvCoinsMessage.setText(text);
        v.tvCoinsMessage.setVisibility(View.VISIBLE);
    }

    private void showContent() {
        v.tvCoinsMessage.setVisibility(View.GONE);
        v.svCoins.setVisibility(View.VISIBLE);
    }

    private static String nn(String s, String fallback) {
        return (s == null || s.trim().isEmpty()) ? fallback : s.trim();
    }
}
