package app.wako.plugins.videoplayer.Utilities;

import android.net.Uri;

import java.util.Arrays;
import java.util.List;

public class HelperUtils {
    private static final List<String> supportedVideoFormats = Arrays.asList("mp4", "webm", "ogv", "3gp", "flv", "dash", "mpd", "m3u8", "ism", "ytube", "");

    /**
     * Determines the video type (MIME type) based on the URI.
     * Supports various streaming formats like HLS, DASH, and progressive download.
     *
     * @param uri The URI of the video
     * @return The identified MIME type
     */
    public static String getVideoType(Uri uri) {
        String ret = null;
        Object obj = uri.getLastPathSegment();
        String lastSegment = (obj == null) ? "" : uri.getLastPathSegment();
        for (String type : supportedVideoFormats) {
            if (ret != null) break;
            if (lastSegment.length() > 0 && lastSegment.contains(type)) ret = type;
            if (ret == null) {
                List<String> segments = uri.getPathSegments();
                if (segments.size() > 0) {
                    String segment;
                    if (segments.get(segments.size() - 1).equals("manifest")) {
                        segment = segments.get(segments.size() - 2);
                    } else {
                        segment = segments.get(segments.size() - 1);
                    }
                    for (String sType : supportedVideoFormats) {
                        if (segment.contains(sType)) {
                            ret = sType;
                            break;
                        }
                    }
                }
            }
        }
        ret = (ret != null) ? ret : "";
        return ret;
    }
}
