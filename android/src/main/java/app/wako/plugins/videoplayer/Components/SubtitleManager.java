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

import app.wako.plugins.videoplayer.R;
import app.wako.plugins.videoplayer.Utilities.CustomDefaultTrackNameProvider;
import app.wako.plugins.videoplayer.Utilities.SubtitleUtils;

import java.lang.reflect.Field;
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

    public void setSubtitleStyle() {
        int foreground = Color.WHITE;
        int background = Color.TRANSPARENT;
        if (subtitleForegroundColor.length() > 4 && subtitleForegroundColor.startsWith("rgba")) {
            foreground = SubtitleUtils.getColorFromRGBA(subtitleForegroundColor);
        }
        if (subtitleBackgroundColor.length() > 4 && subtitleBackgroundColor.startsWith("rgba")) {
            background = SubtitleUtils.getColorFromRGBA(subtitleBackgroundColor);
        }
        playerView
                .getSubtitleView()
                .setStyle(
                        new CaptionStyleCompat(foreground, background, Color.TRANSPARENT, CaptionStyleCompat.EDGE_TYPE_NONE, Color.WHITE, null)
                );
        playerView.getSubtitleView().setFixedTextSize(TypedValue.COMPLEX_UNIT_DIP, subtitleFontSize);

        if (player != null && trackSelector != null) {
            DefaultTrackSelector.Parameters parameters =
                    ((DefaultTrackSelector) trackSelector).getParameters().buildUpon().setSelectUndeterminedTextLanguage(true).build();
            trackSelector.setParameters(parameters);
        }

       playerView.getSubtitleView().setVisibility(View.VISIBLE);

    }

    /**
     * Updates the state of the custom subtitle button
     */
    public void updateCustomSubtitleButton() {
        // Check if subtitle tracks are available
        int totalSubtitles = 0;
        boolean hasSubtitleTracks = false;
        if (player != null) {
            Tracks tracks = player.getCurrentTracks();
            for (Tracks.Group trackGroup : tracks.getGroups()) {
                if (trackGroup.getType() == C.TRACK_TYPE_TEXT) {
                    totalSubtitles++;
                    hasSubtitleTracks = true;
                }
            }
        }

        this.playerView.setShowSubtitleButton(false);
        if(totalSubtitles > 1 || totalSubtitles == 0) {
            // Always use the native Android button
            this.playerView.setShowSubtitleButton(true);
            return;
        }
        // Update our custom button only if necessary
        ImageButton subtitleButton = fragmentView.findViewById(R.id.custom_subtitle_toggle);
        if (subtitleButton != null) {
            // Show button only if subtitles are available
            subtitleButton.setVisibility(hasSubtitleTracks ? View.VISIBLE : View.GONE);

            // Update icon based on subtitle state
            Format currentSubtitleTrack = getCurrentSubtitleTrack();
            if (currentSubtitleTrack != null) {
                subtitleButton.setImageResource(R.drawable.ic_subtitle_on);
            } else {
                subtitleButton.setImageResource(R.drawable.ic_subtitle_off);
            }

            // Configure context menu to display available subtitle tracks
            if (hasSubtitleTracks) {
                subtitleButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSubtitleTracksMenu(v);
                    }
                });
            }
        }
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

        // First check if text renderer is disabled
        if (trackSelector instanceof DefaultTrackSelector defaultTrackSelector) {
            DefaultTrackSelector.Parameters parameters = defaultTrackSelector.getParameters();
            if (parameters.getRendererDisabled(getTextRendererIndex())) {
                return null; // Renderer is disabled, so no subtitle is selected
            }
        }

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
     * @param enabled true to enable subtitles, false to disable
     */
    public void enableSubtitles(boolean enabled) {
        if (trackSelector == null || player == null) return;
        
        if (trackSelector instanceof DefaultTrackSelector defaultTrackSelector) {
            DefaultTrackSelector.Parameters.Builder parametersBuilder =
                    defaultTrackSelector.getParameters().buildUpon();

            if (enabled) {
                // Enable subtitles
                parametersBuilder
                    .setRendererDisabled(C.TRACK_TYPE_TEXT, false)
                    .setDisabledTextTrackSelectionFlags(0)
                    .setSelectUndeterminedTextLanguage(true)
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT);
                
                if (playerView != null && playerView.getSubtitleView() != null) {
                    playerView.getSubtitleView().setVisibility(View.VISIBLE);
                }
            } else {
                // Disable subtitles by selecting no tracks
                int textRendererIndex = getTextRendererIndex();
                if (textRendererIndex != -1) {
                    try {
                        // Get the mapped track info
                        androidx.media3.exoplayer.trackselection.MappingTrackSelector.MappedTrackInfo mappedTrackInfo =
                                defaultTrackSelector.getCurrentMappedTrackInfo();

                        if (mappedTrackInfo != null) {
                            // Get the text track groups
                            TrackGroupArray textTrackGroups = mappedTrackInfo.getTrackGroups(textRendererIndex);
                            
                            // Create an empty selection override to select no tracks
                            DefaultTrackSelector.SelectionOverride override =
                                    new DefaultTrackSelector.SelectionOverride(0, new int[0]);
                            
                            // Apply the override
                            parametersBuilder.setSelectionOverride(textRendererIndex, textTrackGroups, override);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error disabling subtitles: " + e.getMessage());
                    }
                }

                // Hide subtitle view
                if (playerView != null && playerView.getSubtitleView() != null) {
                    playerView.getSubtitleView().setVisibility(View.GONE);
                }
            }

            // Apply parameters
            defaultTrackSelector.setParameters(parametersBuilder.build());
            
            // Force UI update
            if (playerView != null) {
                playerView.invalidate();
            }
            
            // Update button state
            updateCustomSubtitleButton();
            
            Log.d(TAG, "Subtitles " + (enabled ? "enabled" : "disabled"));
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

    private static String getSubtitleLanguage(Uri uri) {
        final String path = uri.getPath().toLowerCase();

        if (path.endsWith(".srt")) {
            int last = path.lastIndexOf(".");
            int prev = last;

            for (int i = last; i >= 0; i--) {
                prev = path.indexOf(".", i);
                if (prev != last)
                    break;
            }

            int len = last - prev;

            if (len >= 2 && len <= 6) {
                // TODO: Validate lang
                return path.substring(prev + 1, last);
            }
        }

        return null;
    }


    /**
     * Gets all subtitle track groups
     *
     * @return Array of text track groups
     */
    private Tracks.Group[] getTextTrackGroups() {
        if (player == null) return new Tracks.Group[0];

        Tracks tracks = player.getCurrentTracks();
        List<Tracks.Group> textGroups = new java.util.ArrayList<>();

        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() == C.TRACK_TYPE_TEXT) {
                textGroups.add(group);
            }
        }

        return textGroups.toArray(new Tracks.Group[0]);
    }

    /**
     * Gets TrackGroupArray for a specific renderer
     *
     * @param rendererIndex The renderer index
     * @return The TrackGroupArray for this renderer, or null if not available
     */
    private TrackGroupArray getRendererTrackGroups(int rendererIndex) {
        if (player == null || trackSelector == null ||
                rendererIndex < 0 || rendererIndex >= player.getRendererCount()) {
            return null;
        }

        try {
            // Get mapped track info via track selector
            if (trackSelector instanceof DefaultTrackSelector) {
                DefaultTrackSelector defaultTrackSelector = (DefaultTrackSelector) trackSelector;
                androidx.media3.exoplayer.trackselection.MappingTrackSelector.MappedTrackInfo mappedTrackInfo =
                        defaultTrackSelector.getCurrentMappedTrackInfo();

                if (mappedTrackInfo != null) {
                    return mappedTrackInfo.getTrackGroups(rendererIndex);
                }
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting renderer track groups: " + e.getMessage());
            return null;
        }
    }

    /**
     * Converts a TrackGroup to TrackGroupArray for use with setSelectionOverride
     *
     * @param group The track group to convert
     * @return A TrackGroupArray containing the specified group
     */
    private TrackGroupArray getTrackGroupArrayFromGroup(TrackGroup group) {
        if (group == null) return null;

        // Create a new TrackGroupArray containing only this group
        return new TrackGroupArray(group);
    }
}
