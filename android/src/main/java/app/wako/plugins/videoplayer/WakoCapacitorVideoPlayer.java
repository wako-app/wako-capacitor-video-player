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

}
