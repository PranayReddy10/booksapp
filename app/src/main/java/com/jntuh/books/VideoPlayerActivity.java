package com.jntuh.books;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

/**
 * Plays a YouTube video IN-APP (no redirect to the YouTube app). Full-screen activity
 * with a back toolbar, matching the reader-style chrome. The player's own controls
 * include a fullscreen button.
 *
 * Extras: "video_id" (required), "title" (optional).
 */
public class VideoPlayerActivity extends AppCompatActivity {

    private YouTubePlayerView playerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final String videoId = getIntent().getStringExtra("video_id");
        String title = getIntent().getStringExtra("title");

        ((TextView) findViewById(R.id.tvTitle)).setText(title != null ? title : "");
        ((ImageView) findViewById(R.id.ivBack)).setOnClickListener(v -> onBackPressed());

        playerView = findViewById(R.id.youtube_player_view);
        getLifecycle().addObserver(playerView);

        playerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                youTubePlayer.loadVideo(videoId != null ? videoId : "", 0f);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (playerView != null) playerView.release();
        super.onDestroy();
    }
}
