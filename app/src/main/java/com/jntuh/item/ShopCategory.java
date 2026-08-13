package com.jntuh.item;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class ShopCategory implements Serializable {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("slug")
    private String slug;

    @SerializedName("count")
    private String count;

    @SerializedName("image")
    private String image;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getCount() { return count; }
    public String getImage() { return image; }
}
