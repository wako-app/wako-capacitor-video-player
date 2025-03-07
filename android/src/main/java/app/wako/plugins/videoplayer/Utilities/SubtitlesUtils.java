package app.wako.plugins.videoplayer.Utilities;

import android.content.res.Configuration;
import android.graphics.Color;
import android.util.Log;

import androidx.media3.ui.CaptionStyleCompat;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.SubtitleView;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility class for managing subtitles in video playback
 */
public class SubtitlesUtils {
    private static final String TAG = "SubtitlesUtils";

    /**
     * Sets the subtitle text size based on device type and orientation
     * @param playerView The PlayerView containing subtitles
     * @param isTvDevice Whether the device is a TV
     * @param subtitlesScale Scale factor for subtitle size
     */
    public static void setSubtitleTextSize(PlayerView playerView, boolean isTvDevice, float subtitlesScale, int orientation) {
        final SubtitleView subtitleView = playerView.getSubtitleView();
        if (subtitleView != null) {
            final float size;
            if (isTvDevice) {
                // TV devices: use a larger base size regardless of orientation
                size = SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subtitlesScale * 2.5f;
            } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                // Landscape mode: base size * 1.3
                size = SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subtitlesScale * 1.3f;
            } else {
                // Portrait mode: larger size (2.0) for better readability
                // We don't use screen ratio to avoid inconsistencies
                size = SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * subtitlesScale * 2.0f;
            }
            subtitleView.setFractionalTextSize(size);
        }
    }

    /**
     * Updates the subtitle style with custom colors and edge type
     * @param playerView The PlayerView containing subtitles
     * @param subTitleOptions JSON object containing subtitle style options
     */
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