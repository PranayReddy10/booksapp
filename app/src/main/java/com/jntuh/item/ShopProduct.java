package com.jntuh.item;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class ShopProduct implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("price")
    private String price;

    @SerializedName("regular_price")
    private String regular_price;

    @SerializedName("sale_price")
    private String sale_price;

    @SerializedName("on_sale")
    private String on_sale;

    @SerializedName("discount")
    private String discount;

    @SerializedName("currency")
    private String currency;

    @SerializedName("thumb")
    private String thumb;

    @SerializedName("images")
    private List<String> images;

    @SerializedName("description")
    private String description;

    @SerializedName("short_desc")
    private String short_desc;

    @SerializedName("stock_status")
    private String stock_status;

    @SerializedName("permalink")
    private String permalink;

    @SerializedName("buy_url")
    private String buy_url;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPrice() { return price; }
    public String getRegular_price() { return regular_price; }
    public String getSale_price() { return sale_price; }
    public String getOn_sale() { return on_sale; }
    public String getDiscount() { return discount; }
    public String getCurrency() { return currency == null ? "\u20b9" : currency; }
    public String getThumb() { return thumb; }
    public List<String> getImages() { return images; }
    public String getDescription() { return description; }
    public String getShort_desc() { return short_desc; }
    public String getStock_status() { return stock_status; }
    public String getPermalink() { return permalink; }
    public String getBuy_url() { return buy_url; }

    public boolean isOnSale() { return "1".equals(on_sale); }
    public boolean isOutOfStock() { return stock_status != null && stock_status.equalsIgnoreCase("outofstock"); }
}
