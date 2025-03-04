package app.wako.plugins.videoplayer.Utilities;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import androidx.documentfile.provider.DocumentFile;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;

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
     * @param uri The URI to extract the filename from
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
     * @param selected         Whether the subtitle should be selected by default
     * @return A subtitle configuration for ExoPlayer
     */
    public static MediaItem.SubtitleConfiguration buildSubtitle(
            Context context,
            Uri uri,
            String subtitleName,
            String subtitleLanguage,
            boolean selected) {

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
                .setLabel(subtitleName);
        if (selected) {
            subtitleConfigurationBuilder.setSelectionFlags(C.SELECTION_FLAG_DEFAULT);
        }
        return subtitleConfigurationBuilder.build();
    }

}
