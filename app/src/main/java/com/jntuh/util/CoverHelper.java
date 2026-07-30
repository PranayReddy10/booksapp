package com.jntuh.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.jntuh.books.R;

/**
 * Renders book covers. Real covers (a URL from the API) load via Glide. Books with
 * no real image get a locally-drawn cover: a gradient from the category color to a
 * darker shade, with the wrapped title centered in bold white — matching the web look.
 *
 * cover_color is only returned by books_details today. For list/grid screens that
 * don't have it, pass null and a neutral default color is used.
 */
public class CoverHelper {

    private static final String DEFAULT_COLOR = "#5C6BC0"; // neutral indigo fallback

    // Treat these as "no real image".
    public static boolean isPlaceholder(String url) {
        if (url == null) return true;
        String u = url.trim();
        if (u.isEmpty()) return true;
        String lower = u.toLowerCase();
        return lower.endsWith("book_placeholder.jpg")
                || lower.endsWith("book_placeholder.png")
                || lower.contains("placeholder");
    }

    /**
     * Bind a cover into an ImageView.
     *
     * @param imageView   target
     * @param imageUrl    post_image from the API (may be empty/placeholder)
     * @param title       book title (drawn on generated covers)
     * @param coverColor  hex color from cover_color (may be null on list screens)
     */
    public static void bind(ImageView imageView, String imageUrl, String title, String coverColor) {
        if (isPlaceholder(imageUrl)) {
            int w = imageView.getWidth() > 0 ? imageView.getWidth() : 400;
            int h = imageView.getHeight() > 0 ? imageView.getHeight() : 560;
            Bitmap bmp = generateCover(w, h, title, coverColor);
            imageView.setImageBitmap(bmp);
        } else {
            Glide.with(imageView.getContext().getApplicationContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_portable)
                    .into(imageView);
        }
    }

    public static Bitmap generateCover(int width, int height, String title, String coverColor) {
        if (width <= 0) width = 400;
        if (height <= 0) height = 560;

        int base = parseColor(coverColor, DEFAULT_COLOR);
        int darker = darken(base, 0.65f);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Gradient background (top base -> bottom darker).
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setShader(new LinearGradient(0, 0, 0, height, base, darker, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, width, height, bgPaint);

        // Title text, wrapped, centered.
        String text = title == null ? "" : title.trim();
        if (!text.isEmpty()) {
            TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            tp.setColor(Color.WHITE);
            tp.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            tp.setTextSize(width * 0.11f);
            tp.setShadowLayer(4f, 0f, 2f, Color.argb(120, 0, 0, 0));

            int textWidth = (int) (width * 0.8f);
            StaticLayout layout = StaticLayout.Builder
                    .obtain(text, 0, text.length(), tp, textWidth)
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(0f, 1.1f)
                    .setMaxLines(5)
                    .setEllipsize(android.text.TextUtils.TruncateAt.END)
                    .build();

            canvas.save();
            float x = (width - textWidth) / 2f;
            float y = (height - layout.getHeight()) / 2f;
            canvas.translate(x, y);
            layout.draw(canvas);
            canvas.restore();
        }

        return bitmap;
    }

    private static int parseColor(String hex, String fallback) {
        try {
            if (hex != null && !hex.trim().isEmpty()) {
                String h = hex.trim();
                if (!h.startsWith("#")) h = "#" + h;
                return Color.parseColor(h);
            }
        } catch (Exception ignored) {
        }
        return Color.parseColor(fallback);
    }

    private static int darken(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a,
                Math.min(r, 255), Math.min(g, 255), Math.min(b, 255));
    }
}
