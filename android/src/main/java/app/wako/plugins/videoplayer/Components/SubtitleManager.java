package app.wako.plugins.videoplayer.Components;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelector;
import androidx.media3.ui.CaptionStyleCompat;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.PlayerControlView;

import app.wako.plugins.videoplayer.FullscreenExoPlayerFragment;
import app.wako.plugins.videoplayer.R;
import app.wako.plugins.videoplayer.Utilities.CustomDefaultTrackNameProvider;
import app.wako.plugins.videoplayer.Utilities.SubtitleUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@UnstableApi
public class SubtitleManager {

    private final Context fragmentContext;
    private final View fragmentView;
    private final PlayerView playerView;
    private TrackSelector trackSelector;
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


        ImageButton customSubtitleToggle = fragmentView.findViewById(R.id.custom_subtitle_toggle);

        if (customSubtitleToggle != null) {
            customSubtitleToggle.setVisibility(View.GONE);
        }

        updateCustomSubtitleButton();

    }

    public void setPlayer(ExoPlayer player) {
        this.player = player;
    }

    public void setTrackSelector(TrackSelector trackSelector) {
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

    /**
     * Updates the state of the custom subtitle button
     */
    public void updateCustomSubtitleButton() {
        boolean useCustomButtonAndTrackMenu = false;

        // Check if subtitle tracks are available
        int totalSubtitles = 0;
        if (player != null) {
            Tracks tracks = player.getCurrentTracks();
            for (Tracks.Group trackGroup : tracks.getGroups()) {
                if (trackGroup.getType() == C.TRACK_TYPE_TEXT) {
                    Format format = trackGroup.getMediaTrackGroup().getFormat(0);
                    Log.d(TAG, "Subtitle track found - Language: " + format.language +
                            ", Label: " + format.label +
                            ", ID: " + format.id +
                            ", Type: " + trackGroup.getType() +
                            ", Selected: " + trackGroup.isSelected());
                    totalSubtitles++;
                }
            }
        }

        useCustomButtonAndTrackMenu = totalSubtitles == 1;

        this.playerView.setShowSubtitleButton(!useCustomButtonAndTrackMenu);

        ImageButton subtitleButton = fragmentView.findViewById(R.id.custom_subtitle_toggle);
        if (subtitleButton == null) {
            return;
        }
        if (!useCustomButtonAndTrackMenu) {
            subtitleButton.setVisibility(View.GONE);
            return;
        }

        // Show button only if subtitles are available
        subtitleButton.setVisibility(View.VISIBLE);

        // Update icon based on subtitle state
        Format currentSubtitleTrack = getCurrentSubtitleTrack();
        if (currentSubtitleTrack != null) {
            subtitleButton.setImageResource(R.drawable.ic_subtitle_on);
        } else {
            subtitleButton.setImageResource(R.drawable.ic_subtitle_off);
        }

        // Configure context menu to display available subtitle tracks
        subtitleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSubtitleTracksMenu(v);
            }
        });

    }

    /**
     * Displays a context menu with all available subtitle tracks
     *
     * @param anchorView View to anchor the menu to
     */
    private void showSubtitleTracksMenu(View anchorView) {
        if (player == null || trackSelector == null) return;

        // Create a context menu (PopupMenu) with custom style for opacity
        Context wrapper = new ContextThemeWrapper(fragmentContext, R.style.PopupMenuStyle);
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(wrapper, anchorView);
        android.view.Menu menu = popup.getMenu();

        // Configure icon display in the menu
        setupPopupMenuIcons(menu);

        // Add option to disable subtitles
        android.view.MenuItem disableItem = menu.add(0, -1, 0, getTranslatedString("disable_subtitles", "Disable"));

        // Get available subtitle tracks
        Tracks tracks = player.getCurrentTracks();
        int trackNumber = 0;

        // Get currently selected subtitle track
        Format currentSubtitleFormat = getCurrentSubtitleTrack();

        // If no subtitle is selected, mark the "Disable" option as active
        if (currentSubtitleFormat == null) {
            disableItem.setChecked(true);
            disableItem.setIcon(R.drawable.ic_check);
        }

        // Loop through all subtitle tracks and add them to the menu
        for (Tracks.Group trackGroup : tracks.getGroups()) {
            if (trackGroup.getType() == C.TRACK_TYPE_TEXT) {
                TrackGroup group = trackGroup.getMediaTrackGroup();
                for (int i = 0; i < group.length; i++) {
                    Format format = group.getFormat(i);

                    // Always start with the language name
                    String trackName = customDefaultTrackNameProvider.getTrackName(format);

                    // Add track to menu
                    android.view.MenuItem item = menu.add(0, trackNumber, i + 1, trackName);

                    // Mark active track with a checkmark
                    boolean isSelected = false;
                    if (currentSubtitleFormat != null) {
                        // Check by ID if available
                        if (format.id != null && currentSubtitleFormat.id != null &&
                                format.id.equals(currentSubtitleFormat.id)) {
                            isSelected = true;
                        }
                    }

                    if (isSelected) {
                        item.setChecked(true);
                        item.setIcon(R.drawable.ic_check);
                    }

                    trackNumber++;
                }
            }
        }

        // Configure playerListener to handle selections
        popup.setOnMenuItemClickListener(new androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(android.view.MenuItem item) {
                int id = item.getItemId();

                if (id == -1) {
                    // Disable subtitles
                    enableSubtitles(false);
                    return true;
                }

                int trackNumber = 0;
                for (Tracks.Group trackGroup : tracks.getGroups()) {
                    if (trackGroup.getType() == C.TRACK_TYPE_TEXT) {
                        TrackGroup group = trackGroup.getMediaTrackGroup();
                        for (int i = 0; i < group.length; i++) {
                            Format format = group.getFormat(i);

                            if (trackNumber == id) {
                                selectSubtitleTrack(format.id);

                                return true;
                            }
                            trackNumber++;
                        }
                    }
                }


                return false;

            }
        });

        // Show menu
        popup.show();
    }


    /**
     * Gets a translated string
     * First uses Android system strings if available
     *
     * @param key          String identifier
     * @param defaultValue Default value if translation is not available
     * @return Translated string
     */
    private String getTranslatedString(String key, String defaultValue) {
        // Specific cases for common system strings
        if (key.equals("disable_subtitles")) {
            return fragmentContext.getResources().getString(android.R.string.no);
        } else if (key.equals("track")) {
            return fragmentContext.getResources().getString(android.R.string.untitled);
        } else if (key.equals("unknown_language")) {
            return "Unknown language"; // No equivalent system string
        } else if (key.equals("subtitles_on")) {
            return "Subtitles enabled"; // No equivalent system string
        } else if (key.equals("subtitles_off")) {
            return "Subtitles disabled"; // No equivalent system string
        }

        return defaultValue;
    }


    /**
     * Gets the currently selected subtitle track
     *
     * @return Format of the active subtitle track, or null if none is active
     */
    private Format getCurrentSubtitleTrack() {
        if (player == null) return null;

        Tracks tracks = player.getCurrentTracks();
        for (Tracks.Group trackGroup : tracks.getGroups()) {
            if (trackGroup.getType() == C.TRACK_TYPE_TEXT && trackGroup.isSelected()) {
                TrackGroup group = trackGroup.getMediaTrackGroup();
                for (int i = 0; i < group.length; i++) {
                    if (trackGroup.isTrackSelected(i)) {
                        return group.getFormat(i);
                    }
                }
            }
        }
        return null;
    }


    /**
     * Selects a specific subtitle track
     *
     * @param trackId ID of the track to select
     */
    private void selectSubtitleTrack(String trackId) {
        if (player == null || trackSelector == null) return;

        // Enable subtitles first
        enableSubtitles(true);

        if (trackSelector instanceof DefaultTrackSelector defaultTrackSelector) {
            DefaultTrackSelector.Parameters.Builder parametersBuilder =
                    defaultTrackSelector.getParameters().buildUpon();

            // First reset all previous selections
            parametersBuilder
                    .clearSelectionOverrides()
                    .setRendererDisabled(C.TRACK_TYPE_TEXT, false);

            // Find text renderer index
            int textRendererIndex = getTextRendererIndex();
            if (textRendererIndex == -1) {
                Log.e(TAG, "No text renderer found");
                return;
            }

            try {
                // Get mapped track info for the player
                androidx.media3.exoplayer.trackselection.MappingTrackSelector.MappedTrackInfo mappedTrackInfo =
                        defaultTrackSelector.getCurrentMappedTrackInfo();

                if (mappedTrackInfo == null) {
                    Log.e(TAG, "No mapped track info available");
                    return;
                }

                // Get track groups for text renderer
                TrackGroupArray textTrackGroups = mappedTrackInfo.getTrackGroups(textRendererIndex);

                // Search for track with specified ID
                for (int groupIndex = 0; groupIndex < textTrackGroups.length; groupIndex++) {
                    TrackGroup group = textTrackGroups.get(groupIndex);

                    for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                        Format format = group.getFormat(trackIndex);

                        if (Objects.equals(format.id, trackId)) {
                            Log.d(TAG, "Found subtitle track with ID: " + trackId);

                            // Create an override to select only this track
                            int[] selectedTracks = new int[]{trackIndex};
                            DefaultTrackSelector.SelectionOverride override =
                                    new DefaultTrackSelector.SelectionOverride(groupIndex, selectedTracks);

                            // Apply the override
                            parametersBuilder.setSelectionOverride(textRendererIndex, textTrackGroups, override);
                            defaultTrackSelector.setParameters(parametersBuilder.build());

                            // Force UI update
                            playerView.invalidate();
                            this.updateCustomSubtitleButton();

                            return;
                        }
                    }
                }

                Log.w(TAG, "No subtitle track found with ID: " + trackId);
            } catch (Exception e) {
                Log.e(TAG, "Error selecting subtitle track: " + e.getMessage());
            }
        }
    }

    /**
     * Enable or disable subtitles
     *
     * @param enabled Whether subtitles should be enabled
     */
    public void enableSubtitles(boolean enabled) {
        if (trackSelector == null || player == null) return;

        if (trackSelector instanceof DefaultTrackSelector defaultTrackSelector) {
            DefaultTrackSelector.Parameters.Builder parametersBuilder =
                    defaultTrackSelector.getParameters().buildUpon();

            int textRendererIndex = getTextRendererIndex();
            if (textRendererIndex == -1) return;

            try {
                // Get mapped track info for the player
                androidx.media3.exoplayer.trackselection.MappingTrackSelector.MappedTrackInfo mappedTrackInfo =
                        defaultTrackSelector.getCurrentMappedTrackInfo();

                if (mappedTrackInfo == null) {
                    Log.e(TAG, "No mapped track info available");
                    return;
                }

                // Get track groups for text renderer
                TrackGroupArray textTrackGroups = mappedTrackInfo.getTrackGroups(textRendererIndex);

                if (!enabled) {
                    // Disable subtitles by clearing all selections and overrides
                    parametersBuilder.clearSelectionOverrides(textRendererIndex)
                            .setSelectionOverride(textRendererIndex, textTrackGroups, null);
                } else {
                    // Enable subtitles (let ExoPlayer choose automatically)
                    parametersBuilder.clearSelectionOverrides(textRendererIndex)
                            .setRendererDisabled(textRendererIndex, false);
                }

                // Apply parameters
                defaultTrackSelector.setParameters(parametersBuilder.build());

                // Force UI update
                playerView.invalidate();

                // Update button state
                updateCustomSubtitleButton();

                Log.d(TAG, "Subtitles " + (enabled ? "enabled" : "disabled"));
            } catch (Exception e) {
                Log.e(TAG, "Error managing subtitles: " + e.getMessage());
            }
        }
    }


    /**
     * Configure icon display in the popup menu
     *
     * @param menu Menu to configure
     */
    private void setupPopupMenuIcons(android.view.Menu menu) {
        try {
            // Use reflection to access setOptionalIconsVisible method
            Class<?> menuClass = Class.forName("androidx.appcompat.view.menu.MenuBuilder");
            if (menu.getClass().equals(menuClass)) {
                java.lang.reflect.Method setOptionalIconsVisible =
                        menuClass.getDeclaredMethod("setOptionalIconsVisible", boolean.class);
                setOptionalIconsVisible.setAccessible(true);
                setOptionalIconsVisible.invoke(menu, true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting popup menu icons visible: " + e.getMessage());
        }
    }


    /**
     * Gets the text/subtitle renderer index
     *
     * @return the text renderer index, or -1 if not found
     */
    private int getTextRendererIndex() {
        if (player == null) return -1;

        for (int i = 0; i < player.getRendererCount(); i++) {
            if (player.getRendererType(i) == C.TRACK_TYPE_TEXT) {
                return i;
            }
        }
        return -1;
    }


    public void reset() {
        this.player = null;
        this.trackSelector = null;
    }
}
