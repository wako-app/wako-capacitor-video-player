package app.wako.plugins.videoplayer;

import android.content.Context;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;

import com.getcapacitor.JSObject;
import app.wako.plugins.videoplayer.PickerVideo.PickerVideoFragment;

import java.util.ArrayList;

public class WakoCapacitorVideoPlayer {

    WakoCapacitorVideoPlayer(Context context) {}

    public String echo(String value) {
        return value;
    }

    @OptIn(markerClass = UnstableApi.class)
    public FullscreenExoPlayerFragment createFullScreenFragment(
        String videoPath,
        Float videoRate,
        Boolean exitOnEnd,
        Boolean loopOnEnd,
        Boolean pipEnabled,
        Boolean bkModeEnabled,
        Boolean showControls,
        String displayMode,
        ArrayList<FullscreenExoPlayerFragment.SubtitleItem> subtitles,
        String preferredLanguage,
        JSObject subTitleOptions,
        JSObject headers,
        String title,
        String smallTitle,
        String accentColor,
        Boolean chromecast,
        String artwork,
        Boolean isTV,
        String playerId,
        String subtitleTrackId,
        String subtitleLocale,
        String audioTrackId,
        String audioLocale,
        Long startAtSec
    ) {
        FullscreenExoPlayerFragment fsFragment = new FullscreenExoPlayerFragment();

        fsFragment.videoPath = videoPath;
        fsFragment.playbackRate = videoRate;
        fsFragment.shouldExitOnEnd = exitOnEnd;
        fsFragment.shouldLoopOnEnd = loopOnEnd;
        fsFragment.isPipEnabled = pipEnabled;
        fsFragment.isBackgroundModeEnabled = bkModeEnabled;
        fsFragment.showControls = showControls;
        fsFragment.displayMode = displayMode;
        fsFragment.subtitles = subtitles;
        fsFragment.preferredLocale = preferredLanguage;
        fsFragment.subTitleOptions = subTitleOptions;
        fsFragment.requestHeaders = headers;
        fsFragment.videoTitle = title;
        fsFragment.videoSubtitle = smallTitle;
        fsFragment.themeColor = accentColor;
        fsFragment.isChromecastEnabled = chromecast;
        fsFragment.posterUrl = artwork;
        fsFragment.isTvDevice = isTV;
        fsFragment.playerId = playerId;
        fsFragment.isLocalFile = false;
        fsFragment.videoId = null;
        fsFragment.subtitleTrackId = subtitleTrackId;
        fsFragment.subtitleLocale = subtitleLocale;
        fsFragment.audioTrackId = audioTrackId;
        fsFragment.audioLocale = audioLocale;
        fsFragment.startAtSec = startAtSec;

        return fsFragment;
    }

    public PickerVideoFragment createPickerVideoFragment() {
        return new PickerVideoFragment();
    }
}
