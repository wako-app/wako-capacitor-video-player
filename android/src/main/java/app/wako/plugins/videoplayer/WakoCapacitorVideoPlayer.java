package app.wako.plugins.videoplayer;

import android.content.Context;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;

import com.getcapacitor.JSObject;

import app.wako.plugins.videoplayer.Components.SubtitleItem;

import java.util.ArrayList;

public class WakoCapacitorVideoPlayer {

    WakoCapacitorVideoPlayer(Context context) {}

    public String echo(String value) {
        return value;
    }

    @OptIn(markerClass = UnstableApi.class)
    public FullscreenExoPlayerFragment createFullScreenFragment(
        String videoUrl,
        Float videoRate,
        Boolean exitOnEnd,
        Boolean loopOnEnd,
        Boolean showControls,
        String displayMode,
        ArrayList<SubtitleItem> subtitles,
        String preferredLocale,
        JSObject subTitleOptions,
        String title,
        String smallTitle,
        Boolean chromecast,
        String artwork,
        Boolean isTV,
        String subtitleTrackId,
        String subtitleLocale,
        String audioTrackId,
        String audioLocale,
        Long startAtSec
    ) {
        FullscreenExoPlayerFragment fsFragment = new FullscreenExoPlayerFragment();

        fsFragment.videoUrl = videoUrl;
        fsFragment.playbackRate = videoRate;
        fsFragment.shouldExitOnEnd = exitOnEnd;
        fsFragment.shouldLoopOnEnd = loopOnEnd;
        fsFragment.showControls = showControls;
        fsFragment.displayMode = displayMode;
        fsFragment.subtitles = subtitles;
        fsFragment.preferredLocale = preferredLocale;
        fsFragment.subTitleOptions = subTitleOptions;
        fsFragment.videoTitle = title;
        fsFragment.videoSubtitle = smallTitle;
        fsFragment.isChromecastEnabled = chromecast;
        fsFragment.posterUrl = artwork;
        fsFragment.isTvDevice = isTV;
        fsFragment.subtitleTrackId = subtitleTrackId;
        fsFragment.subtitleLocale = subtitleLocale;
        fsFragment.audioTrackId = audioTrackId;
        fsFragment.audioLocale = audioLocale;
        fsFragment.startAtSec = startAtSec;

        return fsFragment;
    }


    // public void setCurrentPlayerActivity(PlayerActivity activity) {
    //     this.currentPlayerActivity = activity;
    // }

    // public boolean isPlaying() {
    //     return currentPlayerActivity != null && currentPlayerActivity.isPlaying();
    // }

    // public void play() {
    //     if (currentPlayerActivity != null) {
    //         currentPlayerActivity.play();
    //     }
    // }

    // public void pause() {
    //     if (currentPlayerActivity != null) {
    //         currentPlayerActivity.pause();
    //     }
    // }

    // public int getDuration() {
    //     return currentPlayerActivity != null ? currentPlayerActivity.getDuration() : 0;
    // }

    // public int getCurrentTime() {
    //     return currentPlayerActivity != null ? currentPlayerActivity.getCurrentTime() : 0;
    // }

    // public void setCurrentTime(int time) {
    //     if (currentPlayerActivity != null) {
    //         currentPlayerActivity.setCurrentTime(time);
    //     }
    // }

    // public Float getVolume() {
    //     return currentPlayerActivity != null ? currentPlayerActivity.getVolume() : 1.0f;
    // }

    // public void setVolume(Float volume) {
    //     if (currentPlayerActivity != null) {
    //         currentPlayerActivity.setVolume(volume);
    //     }
    // }

    // public boolean getMuted() {
    //     return currentPlayerActivity != null && currentPlayerActivity.getMuted();
    // }

    // public void setMuted(boolean muted) {
    //     if (currentPlayerActivity != null) {
    //         currentPlayerActivity.setMuted(muted);
    //     }
    // }

    // public Float getRate() {
    //     return currentPlayerActivity != null ? currentPlayerActivity.getRate() : 1.0f;
    // }

    // public void setRate(Float rate) {
    //     if (currentPlayerActivity != null) {
    //         currentPlayerActivity.setRate(rate);
    //     }
    // }

    // public boolean isControllerIsFullyVisible() {
    //     return currentPlayerActivity != null && currentPlayerActivity.isControllerIsFullyVisible();
    // }

    // public void showController() {
    //     if (currentPlayerActivity != null) {
    //         currentPlayerActivity.showController();
    //     }
    // }

    // public void enableSubtitles(boolean enabled) {
    //     if (currentPlayerActivity != null) {
    //         currentPlayerActivity.enableSubtitles(enabled);
    //     }
    // }

    // public void exitPlayer() {
    //     if (currentPlayerActivity != null) {
    //         currentPlayerActivity.exitPlayer();
    //     }
    // }
}
