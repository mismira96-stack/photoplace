package com.example.gallerysorter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;
import java.util.Locale;

/**
 * In-app inline video player for MemoryPhotoViewer.
 * Keeps poster preview fast while swiping, plays inline on tap, and guarantees decoder/audio release
 * on page change, viewer close, or window detachment.
 */
final class MemoryViewerVideoPlayer {
    private final Context context;
    private final ImageView posterView;
    private final FrameLayout videoContainer;
    private final VideoView videoView;
    private final LinearLayout centerPlayBadge;
    private final LinearLayout controlBar;
    private final ImageView playPauseButton;
    private final TextView timeLabel;
    private final SeekBar seekBar;
    private final ImageView muteButton;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private MemoryPhotoItem currentItem;
    private MediaPlayer activePlayer;
    private boolean prepared;
    private boolean playing;
    private boolean muted;
    private boolean userSeeking;

    private final Runnable progressTicker = new Runnable() {
        @Override
        public void run() {
            if (!prepared || !playing) {
                return;
            }
            try {
                int pos = Math.max(0, videoView.getCurrentPosition());
                int dur = Math.max(pos, videoView.getDuration());
                if (!userSeeking) {
                    seekBar.setMax(Math.max(1, dur));
                    seekBar.setProgress(pos);
                }
                timeLabel.setText(formatDuration(pos) + " / " + formatDuration(dur));
            } catch (Exception ignored) {
            }
            handler.postDelayed(this, 250L);
        }
    };

    MemoryViewerVideoPlayer(Context context, ImageView posterView, int FunctionDpUnit) {
        this.context = context;
        this.posterView = posterView;

        videoContainer = new FrameLayout(context);
        videoContainer.setVisibility(View.GONE);
        videoView = new VideoView(context);
        FrameLayout.LayoutParams videoParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER);
        videoContainer.addView(videoView, videoParams);

        centerPlayBadge = new LinearLayout(context);
        centerPlayBadge.setOrientation(LinearLayout.VERTICAL);
        centerPlayBadge.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(Color.argb(185, 18, 22, 32));
        circle.setStroke(dp(2), Color.argb(190, 255, 255, 255));
        centerPlayBadge.setBackground(circle);
        ImageView playIcon = new ImageView(context);
        playIcon.setScaleType(ImageView.ScaleType.CENTER);
        playIcon.setImageDrawable(MediaVectorIconDrawable.play(Color.WHITE, dp(26)));
        centerPlayBadge.addView(playIcon, new LinearLayout.LayoutParams(dp(64), dp(64)));
        centerPlayBadge.setContentDescription("동영상 인앱 재생");
        centerPlayBadge.setVisibility(View.GONE);

        controlBar = new LinearLayout(context);
        controlBar.setOrientation(LinearLayout.HORIZONTAL);
        controlBar.setGravity(Gravity.CENTER_VERTICAL);
        controlBar.setPadding(dp(12), dp(6), dp(12), dp(6));
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(Color.argb(185, 24, 28, 40));
        barBg.setCornerRadius(dp(16));
        barBg.setStroke(dp(1), Color.argb(55, 255, 255, 255));
        controlBar.setBackground(barBg);
        controlBar.setVisibility(View.GONE);

        playPauseButton = new ImageView(context);
        playPauseButton.setScaleType(ImageView.ScaleType.CENTER);
        playPauseButton.setImageDrawable(MediaVectorIconDrawable.play(Color.WHITE, dp(18)));
        playPauseButton.setContentDescription("동영상 재생 또는 일시정지");
        controlBar.addView(playPauseButton, new LinearLayout.LayoutParams(dp(36), dp(36)));

        seekBar = new SeekBar(context);
        seekBar.setProgressTintList(ColorStateList.valueOf(Color.rgb(154, 128, 255)));
        seekBar.setThumbTintList(ColorStateList.valueOf(Color.WHITE));
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(0, FrameLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        seekParams.setMargins(dp(4), 0, dp(4), 0);
        controlBar.addView(seekBar, seekParams);

        timeLabel = new TextView(context);
        timeLabel.setText("00:00 / 00:00");
        timeLabel.setTextColor(Color.rgb(220, 226, 238));
        timeLabel.setTextSize(11.5f);
        timeLabel.setSingleLine(true);
        controlBar.addView(timeLabel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        muteButton = new ImageView(context);
        muteButton.setScaleType(ImageView.ScaleType.CENTER);
        muteButton.setImageDrawable(MediaVectorIconDrawable.volumeOn(Color.WHITE, dp(18)));
        muteButton.setContentDescription("음소거 전환");
        LinearLayout.LayoutParams muteParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        muteParams.leftMargin = dp(4);
        controlBar.addView(muteButton, muteParams);

        muteButton.setOnClickListener(v -> {
            muted = !muted;
            updateMuteIcon();
            applyVolume();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser && prepared) {
                    int dur = Math.max(progress, videoView.getDuration());
                    timeLabel.setText(formatDuration(progress) + " / " + formatDuration(dur));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
                userSeeking = true;
                ViewParent parent = bar.getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(true);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                userSeeking = false;
                if (prepared) {
                    try {
                        videoView.seekTo(bar.getProgress());
                    } catch (Exception ignored) {
                    }
                }
            }
        });
    }

    View videoContainer() {
        return videoContainer;
    }

    View centerPlayBadge() {
        return centerPlayBadge;
    }

    View controlBar() {
        return controlBar;
    }

    boolean isPlaying() {
        return playing;
    }

    void bind(MemoryPhotoItem item, Runnable onFallbackExternal) {
        stopAndRelease();
        this.currentItem = item;
        boolean isVideo = item != null && item.mediaKind == MediaKind.VIDEO && !item.sourceUri.isEmpty();
        centerPlayBadge.setVisibility(isVideo ? View.VISIBLE : View.GONE);
        controlBar.setVisibility(isVideo ? View.VISIBLE : View.GONE);
        timeLabel.setText("00:00 / 00:00");
        seekBar.setMax(100);
        seekBar.setProgress(0);
        updatePlayPauseIcon(false);
        if (!isVideo) {
            centerPlayBadge.setOnClickListener(null);
            playPauseButton.setOnClickListener(null);
            return;
        }
        View.OnClickListener toggleAction = v -> togglePlayPause(onFallbackExternal);
        centerPlayBadge.setOnClickListener(toggleAction);
        playPauseButton.setOnClickListener(toggleAction);
    }

    void togglePlayPause(Runnable onFallbackExternal) {
        if (currentItem == null || currentItem.mediaKind != MediaKind.VIDEO || currentItem.sourceUri.isEmpty()) {
            return;
        }
        if (prepared) {
            try {
                if (playing) {
                    videoView.pause();
                    playing = false;
                    handler.removeCallbacks(progressTicker);
                    updatePlayPauseIcon(false);
                    centerPlayBadge.setVisibility(View.VISIBLE);
                } else {
                    videoView.start();
                    playing = true;
                    updatePlayPauseIcon(true);
                    centerPlayBadge.setVisibility(View.GONE);
                    handler.removeCallbacks(progressTicker);
                    handler.post(progressTicker);
                }
                return;
            } catch (Exception ignored) {
            }
        }
        startPlayback(onFallbackExternal);
    }

    private void startPlayback(Runnable onFallbackExternal) {
        try {
            videoContainer.setVisibility(View.VISIBLE);
            centerPlayBadge.setVisibility(View.GONE);
            updatePlayPauseIcon(true);
            videoView.setOnPreparedListener(mp -> {
                activePlayer = mp;
                prepared = true;
                playing = true;
                applyVolume();
                int duration = Math.max(1, videoView.getDuration());
                seekBar.setMax(duration);
                seekBar.setProgress(0);
                timeLabel.setText("00:00 / " + formatDuration(duration));
                posterView.setVisibility(View.INVISIBLE);
                videoView.start();
                handler.removeCallbacks(progressTicker);
                handler.post(progressTicker);
            });
            videoView.setOnCompletionListener(mp -> {
                playing = false;
                handler.removeCallbacks(progressTicker);
                updatePlayPauseIcon(false);
                centerPlayBadge.setVisibility(View.VISIBLE);
                try {
                    int dur = Math.max(0, videoView.getDuration());
                    seekBar.setProgress(dur);
                    timeLabel.setText(formatDuration(dur) + " / " + formatDuration(dur));
                    videoView.seekTo(0);
                } catch (Exception ignored) {
                }
            });
            videoView.setOnErrorListener((mp, what, extra) -> {
                stopAndRelease();
                if (currentItem != null) {
                    centerPlayBadge.setVisibility(View.VISIBLE);
                }
                if (onFallbackExternal != null) {
                    onFallbackExternal.run();
                }
                return true;
            });
            videoView.setVideoURI(Uri.parse(currentItem.sourceUri));
            videoView.requestFocus();
        } catch (Exception e) {
            stopAndRelease();
            if (onFallbackExternal != null) {
                onFallbackExternal.run();
            }
        }
    }

    void stopAndRelease() {
        handler.removeCallbacks(progressTicker);
        boolean wasActive = prepared || playing || videoContainer.getVisibility() == View.VISIBLE;
        prepared = false;
        playing = false;
        activePlayer = null;
        if (wasActive) {
            try {
                videoView.stopPlayback();
            } catch (Exception ignored) {
            }
        }
        videoContainer.setVisibility(View.GONE);
        posterView.setVisibility(View.VISIBLE);
        updatePlayPauseIcon(false);
        if (currentItem != null && currentItem.mediaKind == MediaKind.VIDEO) {
            centerPlayBadge.setVisibility(View.VISIBLE);
        } else {
            centerPlayBadge.setVisibility(View.GONE);
        }
    }

    private void updatePlayPauseIcon(boolean isPlayingNow) {
        playPauseButton.setImageDrawable(isPlayingNow
                ? MediaVectorIconDrawable.pause(Color.WHITE, dp(18))
                : MediaVectorIconDrawable.play(Color.WHITE, dp(18)));
    }

    private void updateMuteIcon() {
        muteButton.setImageDrawable(muted
                ? MediaVectorIconDrawable.volumeOff(Color.WHITE, dp(18))
                : MediaVectorIconDrawable.volumeOn(Color.WHITE, dp(18)));
    }

    private void applyVolume() {
        if (activePlayer != null) {
            try {
                float v = muted ? 0.0f : 1.0f;
                activePlayer.setVolume(v, v);
            } catch (Exception ignored) {
            }
        }
    }

    static String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
