package app.wako.plugins.videoplayer.Components;

import android.content.Context;
import android.media.Image;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelector;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.PlayerControlView;

import app.wako.plugins.videoplayer.R;
import app.wako.plugins.videoplayer.Utilities.CustomDefaultTrackNameProvider;
import app.wako.plugins.videoplayer.Utilities.SubtitleUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

@UnstableApi
public class SubtitleManager {

    private final Context fragmentContext;
    private final View fragmentView;
    private final PlayerView playerView;
    private final ImageButton toggleButton;
    private DefaultTrackSelector trackSelector;
    private ExoPlayer player;
    private String subtitleForegroundColor = "";
    private String subtitleBackgroundColor = "";
    private Integer subtitleFontSize = 16;
    public String preferredLocale;
    private CustomDefaultTrackNameProvider customDefaultTrackNameProvider;

    private static final String TAG = SubtitleManager.class.getName();

    public SubtitleManager(Context fragmentContext, View fragmentView, String subtitleForegroundColor, String subtitleBackgroundColor, Integer subtitleFontSize, String preferredLocale) {
        this.fragmentContext = fragmentContext;
        this.fragmentView = fragmentView;
        this.subtitleForegroundColor = subtitleForegroundColor;
        this.subtitleBackgroundColor = subtitleBackgroundColor;
        this.subtitleFontSize = subtitleFontSize;
        this.preferredLocale = preferredLocale;

        this.playerView = fragmentView.findViewById(R.id.videoViewId);

        this.toggleButton = fragmentView.findViewById(R.id.subtitle_toggle);

        if (this.toggleButton != null) {
            this.toggleButton.setVisibility(View.GONE);

            this.toggleButton.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (trackSelector == null || player == null) {
                                return;
                            }
                            Format currentSubtitleTrack = SubtitleUtils.getCurrentSubtitleTrack(player);
                            if (currentSubtitleTrack != null) {
                                SubtitleUtils.disableSubtitles(trackSelector, player);
                            } else {
                                SubtitleUtils.enableSubtitles(trackSelector, player);
                            }
                            refreshSubtitleButton();
                        }
                    }
            );
        }

        this.customDefaultTrackNameProvider = new CustomDefaultTrackNameProvider(fragmentContext.getResources());
        try {
            // First, access the controller via reflection
            Field controllerField = PlayerView.class.getDeclaredField("controller");
            controllerField.setAccessible(true);
            PlayerControlView controlView = (PlayerControlView) controllerField.get(playerView);

            // Then set the trackNameProvider
            final Field field = PlayerControlView.class.getDeclaredField("trackNameProvider");
            field.setAccessible(true);
            field.set(controlView, customDefaultTrackNameProvider);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Error setting custom track name provider: " + e.getMessage());
            e.printStackTrace();
        }


        refreshSubtitleButton();

    }

    public void setPlayer(ExoPlayer player) {
        this.player = player;
    }

    public void setTrackSelector(DefaultTrackSelector trackSelector) {
        this.trackSelector = trackSelector;
    }

    public void loadExternalSubtitles(ArrayList<SubtitleItem> subtitles, MediaItem.Builder mediaItemBuilder) {
        if (!subtitles.isEmpty()) {

            List<MediaItem.SubtitleConfiguration> subtitleConfigurations = new ArrayList<>();
            for (int i = 0; i < subtitles.size(); i++) {
                SubtitleItem subtitleItem = subtitles.get(i);


                MediaItem.SubtitleConfiguration subtitle = SubtitleUtils.buildSubtitle(
                        fragmentContext,
                        Uri.parse(subtitleItem.url),
                        subtitleItem.name,
                        subtitleItem.lang
                );

                subtitleConfigurations.add(subtitle);

            }
            mediaItemBuilder.setSubtitleConfigurations(subtitleConfigurations);
        }
    }

    public void setSubtitleStyle() {
        SubtitleUtils.setSubtitleStyle(
                subtitleForegroundColor,
                subtitleBackgroundColor,
                subtitleFontSize,
                playerView
        );

    }

    public void refreshSubtitleButton() {
        if(trackSelector == null || player == null) {
            return ;
        }
        // Check if subtitle tracks are available
        boolean hasSubtitles = false;
        if (player != null) {
            Tracks tracks = player.getCurrentTracks();
            for (Tracks.Group trackGroup : tracks.getGroups()) {
                if (trackGroup.getType() == C.TRACK_TYPE_TEXT) {
                    hasSubtitles = true;
                    break;
                }
            }
        }


        this.playerView.setShowSubtitleButton(hasSubtitles);

        if (hasSubtitles) {
            toggleButton.setVisibility(View.VISIBLE);
        } else {
            toggleButton.setVisibility(View.GONE);
        }

        // Update icon based on subtitle state
        Format currentSubtitleTrack = SubtitleUtils.getCurrentSubtitleTrack(player);
        if (currentSubtitleTrack != null) {
            toggleButton.setImageResource(R.drawable.ic_subtitle_on);
        } else {
            toggleButton.setImageResource(R.drawable.ic_subtitle_off);
        }

    }


}
