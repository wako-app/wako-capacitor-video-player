package app.wako.plugins.videoplayer.Utilities;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.CaptionStyleCompat;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.SubtitleView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for handling subtitle-related operations in the video player.
 * Provides methods for determining subtitle MIME types, extracting language information,
 * color conversion, and building subtitle configurations for ExoPlayer.
 */
public class SubtitleUtils {

    private static final String TAG = "SubtitleUtils";
    
    // Store the last selected subtitle track information
    private static Format lastSelectedSubtitleTrack = null;
    private static int lastSelectedGroupIndex = -1;
    private static int lastSelectedTrackIndex = -1;

    /**
     * Determines the MIME type of a subtitle file based on its URI.
     * Examines file extensions and path patterns to identify subtitle formats.
     *
     * @param uri The URI of the subtitle file
     * @return The MIME type string for the identified subtitle format (SSA, VTT, TTML, or SubRip)
     */
    public static String getSubtitleMime(Uri uri) {
        final String path = uri.getPath();
        // Clean the path from potential query parameters
        String cleanPath = path;

        // Handle paths with query parameters in the path (special case)
        if (path.contains("?")) {
            cleanPath = path.substring(0, path.indexOf("?"));
        }

        // Handle URLs like "sub.vtt/" where there is a trailing slash
        if (cleanPath.endsWith("/")) {
            cleanPath = cleanPath.substring(0, cleanPath.length() - 1);
        }

        // Also check the complete URL for extensions
        String fullUrl = uri.toString().toLowerCase();

        if (cleanPath.endsWith(".ssa") || cleanPath.endsWith(".ass") ||
                fullUrl.contains("/sub.ssa") || fullUrl.contains("/sub.ass")) {
            return MimeTypes.TEXT_SSA;
        } else if (cleanPath.endsWith(".vtt") ||
                fullUrl.contains("/sub.vtt")) {
            return MimeTypes.TEXT_VTT;
        } else if (cleanPath.endsWith(".ttml") || cleanPath.endsWith(".xml") ||
                cleanPath.endsWith(".dfxp") ||
                fullUrl.contains("/sub.ttml") || fullUrl.contains("/sub.xml") ||
                fullUrl.contains("/sub.dfxp")) {
            return MimeTypes.APPLICATION_TTML;
        } else {
            // Default to SubRip format if we can't determine the type
            return MimeTypes.APPLICATION_SUBRIP;
        }
    }

    /**
     * Attempts to extract language information from a subtitle URI.
     * Looks for a language code in the filename pattern (before the .srt extension).
     *
     * @param uri The URI of the subtitle file
     * @return The detected language code or null if not found
     */
    public static String getSubtitleLanguage(Uri uri) {
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
     * Converts an RGBA color string to an integer color value.
     * Parses color components from strings like "rgba(255,255,255,1)".
     *
     * @param rgbaColor The RGBA color string in format "rgba(r,g,b,a)"
     * @return The integer representation of the color
     */
    public static int getColorFromRGBA(String rgbaColor) {
        int ret = 0;
        String color = rgbaColor.substring(rgbaColor.indexOf("(") + 1, rgbaColor.indexOf(")"));
        List<String> colors = Arrays.asList(color.split(","));
        if (colors.size() == 4) {
            ret =
                    (Math.round(Float.parseFloat(colors.get(3).trim()) * 255) & 0xff) << 24 |
                            (Integer.parseInt(colors.get(0).trim()) & 0xff) << 16 |
                            (Integer.parseInt(colors.get(1).trim()) & 0xff) << 8 |
                            (Integer.parseInt(colors.get(2).trim()) & 0xff);
        }
        return ret;
    }

    /**
     * Extracts the filename from a URI, with special handling for content URIs.
     * Removes file extension if present.
     *
     * @param context The Android context
     * @param uri     The URI to extract the filename from
     * @return The extracted filename without extension
     */
    public static String getFileName(Context context, Uri uri) {
        String result = null;
        try {
            if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                try (Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        final int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (columnIndex > -1)
                            result = cursor.getString(columnIndex);
                    }
                }
            }
            if (result == null) {
                result = uri.getPath();
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
            if (result.indexOf(".") > 0)
                result = result.substring(0, result.lastIndexOf("."));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Builds a subtitle configuration for ExoPlayer.
     * Creates a fully configured subtitle based on URI, name, language, and selection preferences.
     *
     * @param context          The Android context
     * @param uri              The URI of the subtitle file
     * @param subtitleName     The name of the subtitle (can be null)
     * @param subtitleLanguage The language of the subtitle (can be null)
     * @return A subtitle configuration for ExoPlayer
     */
    public static MediaItem.SubtitleConfiguration buildSubtitle(
            Context context,
            Uri uri,
            String subtitleName,
            String subtitleLanguage) {

        final String subtitleMime = SubtitleUtils.getSubtitleMime(uri);
        // If no language is provided, try to detect it from the filename
        final String detectedLanguage = SubtitleUtils.getSubtitleLanguage(uri);
        // Priority to provided language, otherwise use detected language
        final String language = subtitleLanguage != null ? subtitleLanguage : detectedLanguage;

        // If no name is provided and no language is detected, use the filename
        if (language == null && subtitleName == null)
            subtitleName = getFileName(context, uri);

        MediaItem.SubtitleConfiguration.Builder subtitleConfigurationBuilder = new MediaItem.SubtitleConfiguration.Builder(uri)
                .setMimeType(subtitleMime)
                .setLanguage(language)
                .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
                .setId(uri.toString())
                .setLabel(subtitleName);


        return subtitleConfigurationBuilder.build();
    }

    @OptIn(markerClass = UnstableApi.class)
    public static void setSubtitleStyle(
            String subtitleForegroundColor,
            String subtitleBackgroundColor,
            Integer subtitleFontSize,
            PlayerView playerView
    ) {
        int foreground = Color.WHITE;
        int background = Color.TRANSPARENT;
        if (subtitleForegroundColor.length() > 4 && subtitleForegroundColor.startsWith("rgba")) {
            foreground = SubtitleUtils.getColorFromRGBA(subtitleForegroundColor);
        }
        if (subtitleBackgroundColor.length() > 4 && subtitleBackgroundColor.startsWith("rgba")) {
            background = SubtitleUtils.getColorFromRGBA(subtitleBackgroundColor);
        }

        CaptionStyleCompat style = new CaptionStyleCompat(
            foreground,
            background,
            Color.TRANSPARENT,
            CaptionStyleCompat.EDGE_TYPE_OUTLINE,
            Color.BLACK,
            null
        );

        playerView.getSubtitleView().setStyle(style);
        playerView.getSubtitleView().setFixedTextSize(TypedValue.COMPLEX_UNIT_DIP, subtitleFontSize);
        playerView.getSubtitleView().setApplyEmbeddedStyles(true);
        playerView.getSubtitleView().setApplyEmbeddedFontSizes(true);
        playerView.getSubtitleView().setBottomPaddingFraction(0.05f);

        playerView.getSubtitleView().setVisibility(View.VISIBLE);
    }

    /**
     * Enables subtitles and attempts to restore the previously selected subtitle track.
     * If no track was previously selected, selects the first available subtitle track.
     *
     * @param trackSelector The track selector to configure
     * @param player The ExoPlayer instance to get tracks from
     */
    @OptIn(markerClass = UnstableApi.class)
    public static void enableSubtitles(DefaultTrackSelector trackSelector, ExoPlayer player) {
        // Enable subtitles by not ignoring them in the selector
        trackSelector.setParameters(trackSelector.buildUponParameters()
                .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT));
        
        Tracks tracks = player.getCurrentTracks();
        boolean subtitleSelected = false;
        
        // Try to restore the last selected subtitle if we have one stored
        if (lastSelectedSubtitleTrack != null && lastSelectedGroupIndex >= 0 && lastSelectedTrackIndex >= 0) {
            int currentGroupIndex = 0;
            
            for (Tracks.Group trackGroup : tracks.getGroups()) {
                if (trackGroup.getType() == C.TRACK_TYPE_TEXT) {
                    if (currentGroupIndex == lastSelectedGroupIndex) {
                        TrackGroup group = trackGroup.getMediaTrackGroup();
                        // Check if the saved track index is valid for this group
                        if (lastSelectedTrackIndex < group.length) {
                            // Apply the selection manually by matching the track
                            Format format = group.getFormat(lastSelectedTrackIndex);
                            if (format.language != null) {
                                trackSelector.setParameters(
                                    trackSelector.buildUponParameters()
                                        .setPreferredTextLanguage(format.language)
                                );
                                Log.d(TAG, "Restored subtitle track with language: " + format.language);
                                subtitleSelected = true;
                                break;
                            }
                        }
                    }
                    currentGroupIndex++;
                }
            }
        }
        
        // If no subtitle was selected or restored, select the first available subtitle track
        if (!subtitleSelected) {
            for (Tracks.Group trackGroup : tracks.getGroups()) {
                if (trackGroup.getType() == C.TRACK_TYPE_TEXT) {
                    TrackGroup group = trackGroup.getMediaTrackGroup();
                    if (group.length > 0) {
                        Format format = group.getFormat(0);
                        if (format.language != null) {
                            trackSelector.setParameters(
                                trackSelector.buildUponParameters()
                                    .setPreferredTextLanguage(format.language)
                            );
                            Log.d(TAG, "Selected first available subtitle track with language: " + format.language);
                        } else {
                            // If no language info, still try to select it
                            trackSelector.setParameters(
                                trackSelector.buildUponParameters()
                                    .setPreferredTextLanguage("")  // Empty string to match any subtitle
                            );
                            Log.d(TAG, "Selected first available subtitle track (no language info)");
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Enables subtitles without restoring a specific track (backward compatibility)
     * 
     * @param trackSelector The track selector to configure
     */
    @OptIn(markerClass = UnstableApi.class)
    public static void enableSubtitles(DefaultTrackSelector trackSelector) {
        trackSelector.setParameters(trackSelector.buildUponParameters()
                .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT));
    }

    /**
     * Disables subtitles but stores the currently selected track for later restoration
     *
     * @param trackSelector The track selector to configure
     * @param player The ExoPlayer instance to get the current subtitle track from
     */
    @OptIn(markerClass = UnstableApi.class)
    public static void disableSubtitles(DefaultTrackSelector trackSelector, ExoPlayer player) {
        // Store the currently selected subtitle track before disabling
        storeCurrentSubtitleTrack(player);
        
        // Disable all subtitle tracks by setting a very high selection flag
        trackSelector.setParameters(trackSelector.buildUponParameters()
                .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT | C.SELECTION_FLAG_FORCED | C.SELECTION_FLAG_AUTOSELECT)
                .setPreferredTextLanguage(null)  // Clear any preferred language
                .setSelectUndeterminedTextLanguage(false));  // Don't auto-select any language
    }

    /**
     * Disables subtitles without storing the current track (backward compatibility)
     * 
     * @param trackSelector The track selector to configure
     */
    @OptIn(markerClass = UnstableApi.class)
    public static void disableSubtitles(DefaultTrackSelector trackSelector) {
        trackSelector.setParameters(trackSelector.buildUponParameters()
                .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT | C.SELECTION_FLAG_FORCED));
    }

    /**
     * Stores information about the currently selected subtitle track
     * 
     * @param player The ExoPlayer instance to get the current subtitle track from
     */
    @OptIn(markerClass = UnstableApi.class)
    private static void storeCurrentSubtitleTrack(ExoPlayer player) {
        Tracks tracks = player.getCurrentTracks();
        int groupIndex = 0;
        
        for (Tracks.Group trackGroup : tracks.getGroups()) {
            if (trackGroup.getType() == C.TRACK_TYPE_TEXT) {
                TrackGroup group = trackGroup.getMediaTrackGroup();
                for (int i = 0; i < group.length; i++) {
                    if (trackGroup.isTrackSelected(i)) {
                        lastSelectedSubtitleTrack = group.getFormat(i);
                        lastSelectedGroupIndex = groupIndex;
                        lastSelectedTrackIndex = i;
                        
                        String trackId = lastSelectedSubtitleTrack.id != null ? 
                            lastSelectedSubtitleTrack.id : "unknown";
                        String language = lastSelectedSubtitleTrack.language != null ? 
                            lastSelectedSubtitleTrack.language : "unknown";
                            
                        Log.d(TAG, "Stored subtitle track: " + trackId + 
                              " (" + language + ") at group " + 
                              groupIndex + ", track " + i);
                        return;
                    }
                }
                groupIndex++;
            }
        }
        
        // If no subtitle track was selected, clear the stored information
        lastSelectedSubtitleTrack = null;
        lastSelectedGroupIndex = -1;
        lastSelectedTrackIndex = -1;
    }

    /**
     * Gets the currently selected subtitle track
     *
     * @return Format of the active subtitle track, or null if none is active
     */
    @OptIn(markerClass = UnstableApi.class)
    public static Format getCurrentSubtitleTrack(ExoPlayer player) {
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
     * Sets the subtitle text size based on device type and orientation
     * @param playerView The PlayerView containing subtitles
     * @param isTvDevice Whether the device is a TV
     * @param subtitlesScale Scale factor for subtitle size
     */
     @OptIn(markerClass = UnstableApi.class)
     public static void setSubtitleTextSize(PlayerView playerView, boolean isTvDevice, float subtitlesScale, int orientation) {
        final SubtitleView subtitleView = playerView.getSubtitleView();
        if (subtitleView != null) {
            final float size;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // Landscape mode: base size * 1.3
                size = SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subtitlesScale * 1.1f;
            } else {
                // Portrait mode: larger size (2.0) for better readability
                // We don't use screen ratio to avoid inconsistencies
                size = SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subtitlesScale * 1.3f;
            }
            subtitleView.setFractionalTextSize(size);
        }
    }

    /**
     * Updates the subtitle style with custom colors and edge type
     * @param playerView The PlayerView containing subtitles
     * @param subTitleOptions JSON object containing subtitle style options
     */
    @OptIn(markerClass = UnstableApi.class)
    public static void updateSubtitleStyle(PlayerView playerView, JSONObject subTitleOptions, boolean isTvDevice, float subtitlesScale, int orientation) {
        final SubtitleView subtitleView = playerView.getSubtitleView();
        if (subtitleView != null) {
            CaptionStyleCompat style;
            
            // Create a custom style with your desired colors
            int foregroundColor = Color.WHITE;
            int backgroundColor = Color.TRANSPARENT;
            int edgeType = CaptionStyleCompat.EDGE_TYPE_OUTLINE;
            int edgeColor = Color.BLACK;
            
            if (subTitleOptions != null) {
                try {
                    if (subTitleOptions.has("foregroundColor")) {
                        foregroundColor = Color.parseColor(subTitleOptions.getString("foregroundColor"));
                    }
                    if (subTitleOptions.has("backgroundColor")) {
                        backgroundColor = Color.parseColor(subTitleOptions.getString("backgroundColor"));
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing subtitle options", e);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Invalid color format in subtitle options", e);
                }
            }
            
            style = new CaptionStyleCompat(
                foregroundColor,
                backgroundColor,
                Color.TRANSPARENT,
                edgeType,
                edgeColor,
                null
            );
            
            subtitleView.setStyle(style);
            setSubtitleTextSize(playerView, isTvDevice, subtitlesScale, orientation);
        }
    }
}


