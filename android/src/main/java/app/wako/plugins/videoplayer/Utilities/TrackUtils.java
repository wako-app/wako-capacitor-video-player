package app.wako.plugins.videoplayer.Utilities;

import android.content.Context;

import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import app.wako.plugins.videoplayer.Components.SubtitleManager;
import app.wako.plugins.videoplayer.Notifications.NotificationCenter;


public class TrackUtils {
    public static final String DISABLED_TRACK = "#disabled";

    @OptIn(markerClass = UnstableApi.class)
    public static void selectTracksOldWay(
            ExoPlayer player,
            DefaultTrackSelector trackSelector,
            String subtitleTrackId,
            String subtitleLocale,
            String audioTrackId,
            String audioLocale,
            String preferredLocale
    ) {
        if (player == null || trackSelector == null) {
            return;
        }
        Tracks tracks = player.getCurrentTracks();

        boolean audioSelected = false;
        boolean subtitleSelected = false;

        // Select audio track
        if (!audioTrackId.isEmpty() || !audioLocale.isEmpty()) {
            Format selectedFormat = null;

            // First try to find by ID and check locale if specified
            if (!audioTrackId.isEmpty()) {
                for (Tracks.Group trackGroup : tracks.getGroups()) {
                    if (trackGroup.getType() == C.TRACK_TYPE_AUDIO) {
                        Format format = trackGroup.getMediaTrackGroup().getFormat(0);
                        if (format.id != null && format.id.equals(audioTrackId)) {
                            selectedFormat = format;
                            break;
                        }
                    }
                }
            }

            if (selectedFormat != null && !audioLocale.isEmpty() && !selectedFormat.language.equals(audioLocale)) {
                selectedFormat = null;
            }

            // If not found and locale specified, try by locale only
            if (selectedFormat == null && !audioLocale.isEmpty()) {
                for (Tracks.Group trackGroup : tracks.getGroups()) {
                    if (trackGroup.getType() == C.TRACK_TYPE_AUDIO) {
                        Format format = trackGroup.getMediaTrackGroup().getFormat(0);
                        if (format.language != null && format.language.equals(audioLocale)) {
                            selectedFormat = format;
                            break;
                        }
                    }
                }
            }

            // Apply the selected format if found
            if (selectedFormat != null) {
                trackSelector.setParameters(trackSelector.buildUponParameters().setPreferredAudioLanguage(selectedFormat.language));
                audioSelected = true;
                SubtitleUtils.disableSubtitles(trackSelector);
            }
        }

        // Select subtitle track
        if (!subtitleTrackId.isEmpty() || !subtitleLocale.isEmpty()) {
            Format selectedFormat = null;

            // First try to find by ID and check locale if specified
            if (!subtitleTrackId.isEmpty()) {

                for (Tracks.Group trackGroup : tracks.getGroups()) {
                    if (trackGroup.getType() == C.TRACK_TYPE_TEXT) {
                        Format format = trackGroup.getMediaTrackGroup().getFormat(0);
                        if (format.id != null && format.id.equals(subtitleTrackId)) {
                            selectedFormat = format;
                            break;
                        }
                    }
                }
            }

            if (selectedFormat != null && !subtitleLocale.isEmpty() && !selectedFormat.language.equals(subtitleLocale)) {
                selectedFormat = null;
            }

            // If not found and locale specified, try by locale only
            if (selectedFormat == null && !subtitleLocale.isEmpty()) {
                for (Tracks.Group trackGroup : tracks.getGroups()) {
                    if (trackGroup.getType() == C.TRACK_TYPE_TEXT) {
                        Format format = trackGroup.getMediaTrackGroup().getFormat(0);
                        if (format.language != null && format.language.equals(subtitleLocale)) {
                            selectedFormat = format;
                            break;
                        }
                    }
                }
            }

            // Apply the selected format if found
            if (selectedFormat != null) {
                trackSelector.setParameters((trackSelector).buildUponParameters().setPreferredTextLanguage(selectedFormat.language));
                subtitleSelected = true;
            }
        }

        if (subtitleTrackId.equals(DISABLED_TRACK)) {
            // Disable subtitles
            SubtitleUtils.disableSubtitles(trackSelector);
        }

        if (!audioSelected && !subtitleSelected && !preferredLocale.isEmpty()) {
            // First try to find the audio with the same language
            for (Tracks.Group trackGroup : tracks.getGroups()) {
                if (trackGroup.getType() == C.TRACK_TYPE_AUDIO) {
                    Format format = trackGroup.getMediaTrackGroup().getFormat(0);
                    if (format.language != null && format.language.equals(preferredLocale)) {

                        // Audio track found, select it and disable subtitles
                        trackSelector.setParameters(trackSelector.buildUponParameters().setPreferredAudioLanguage(preferredLocale));
                        SubtitleUtils.disableSubtitles(trackSelector);
                        return;
                    }
                }
            }

            // Then try to find the subtitle with the same language
            for (Tracks.Group trackGroup : tracks.getGroups()) {
                if (trackGroup.getType() == C.TRACK_TYPE_TEXT) {
                    Format format = trackGroup.getMediaTrackGroup().getFormat(0);
                    if (format.language != null && format.language.equals(preferredLocale)) {
                        trackSelector.setParameters(trackSelector.buildUponParameters().setPreferredTextLanguage(preferredLocale));
                    }
                }
            }
        }

    }

    /**
     * Selects audio and subtitle tracks based on user preferences.
     * Applies track selection parameters to the player based on the subtitleTrackId,
     * subtitleLocale, audioTrackId, and audioLocale properties.
     */
    @OptIn(markerClass = UnstableApi.class)
    private void selectTracksJustPlayerWay(
            Context fragmentContext,
            ExoPlayer player, DefaultTrackSelector trackSelector, String subtitleTrackId, String subtitleLocale, String audioTrackId, String audioLocale, String preferredLocale
    ) {
        if (player != null && trackSelector != null) {
            Tracks tracks = player.getCurrentTracks();
            TrackSelectionParameters.Builder overridesBuilder = new TrackSelectionParameters.Builder(fragmentContext);

            TrackSelectionOverride trackSelectionOverride = null;

            final List<Integer> newTracks = new ArrayList<>();
            newTracks.add(0);

            TrackGroup audioGroup = null;
            TrackGroup subtitleGroup = null;


            // Select audio track
            if (!audioTrackId.isEmpty() || !audioLocale.isEmpty()) {
                Format selectedFormat = null;

                // First try to find by ID and check locale if specified
                if (!audioTrackId.isEmpty()) {
                    audioGroup = getTrackGroupFromFormatId(C.TRACK_TYPE_AUDIO, audioTrackId, player);
                    if (audioGroup != null) {
                        selectedFormat = audioGroup.getFormat(0);
                    }
                }

                if (selectedFormat != null && !audioLocale.isEmpty() && !selectedFormat.language.equals(audioLocale)) {
                    audioGroup = null;
                    selectedFormat = null;
                }

                // If not found and locale specified, try by locale only
                if (audioGroup == null && !audioLocale.isEmpty()) {
                    audioGroup = getTrackGroupFromFormatLanguage(C.TRACK_TYPE_AUDIO, audioLocale, player);
                }
            }

            // Select subtitle track
            if (!subtitleTrackId.isEmpty() || !subtitleLocale.isEmpty()) {
                Format selectedFormat = null;

                // First try to find by ID and check locale if specified
                if (!subtitleTrackId.isEmpty()) {
                    subtitleGroup = getTrackGroupFromFormatId(C.TRACK_TYPE_TEXT, subtitleTrackId, player);
                    if (subtitleGroup != null) {
                        selectedFormat = subtitleGroup.getFormat(0);
                    }
                }

                if (selectedFormat != null && !subtitleLocale.isEmpty() && !selectedFormat.language.equals(subtitleLocale)) {
                    subtitleGroup = null;
                    selectedFormat = null;
                }

                // If not found and locale specified, try by locale only
                if (subtitleGroup == null && !subtitleLocale.isEmpty()) {
                    subtitleGroup = getTrackGroupFromFormatLanguage(C.TRACK_TYPE_TEXT, subtitleLocale, player);
                }
            }


            if (audioGroup == null && subtitleGroup == null && !preferredLocale.isEmpty()) {
                // First try to find the audio with the same language
                audioGroup = getTrackGroupFromFormatLanguage(C.TRACK_TYPE_AUDIO, preferredLocale, player);

                // Then try to find the subtitle with the same language
                if (audioGroup == null) {
                    subtitleGroup = getTrackGroupFromFormatLanguage(C.TRACK_TYPE_TEXT, preferredLocale, player);
                }
            }
            if (subtitleTrackId.equals(DISABLED_TRACK)) {
                subtitleGroup = null;
                // Disable subtitles
                SubtitleUtils.disableSubtitles(trackSelector);
            }

            if (subtitleGroup != null) {
                trackSelectionOverride = new TrackSelectionOverride(subtitleGroup, newTracks);
                overridesBuilder.addOverride(trackSelectionOverride);
            }
            if (audioGroup != null) {
                trackSelectionOverride = new TrackSelectionOverride(audioGroup, newTracks);
                overridesBuilder.addOverride(trackSelectionOverride);
            }
            if (player != null) {
                TrackSelectionParameters.Builder trackSelectionParametersBuilder = player.getTrackSelectionParameters().buildUpon();
                if (trackSelectionOverride != null) {
                    trackSelectionParametersBuilder.setOverrideForType(trackSelectionOverride);
                }
                player.setTrackSelectionParameters(trackSelectionParametersBuilder.build());
            }

        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private TrackGroup getTrackGroupFromFormatId(int trackType, String id, ExoPlayer player) {
        if ((id == null && trackType == C.TRACK_TYPE_AUDIO) || player == null) {
            return null;
        }
        for (Tracks.Group group : player.getCurrentTracks().getGroups()) {
            if (group.getType() == trackType) {
                final TrackGroup trackGroup = group.getMediaTrackGroup();
                final Format format = trackGroup.getFormat(0);
                if (Objects.equals(id, format.id)) {
                    return trackGroup;
                }
            }
        }
        return null;
    }

    @OptIn(markerClass = UnstableApi.class)
    private TrackGroup getTrackGroupFromFormatLanguage(int trackType, String locale, ExoPlayer player) {
        if ((locale == null && trackType == C.TRACK_TYPE_AUDIO) || player == null) {
            return null;
        }
        for (Tracks.Group group : player.getCurrentTracks().getGroups()) {
            if (group.getType() == trackType) {
                final TrackGroup trackGroup = group.getMediaTrackGroup();
                final Format format = trackGroup.getFormat(0);
                if (Objects.equals(locale, format.language)) {
                    return trackGroup;
                }
            }
        }
        return null;
    }

    @OptIn(markerClass = UnstableApi.class)
    public static void onTracksChanged(SubtitleManager subtitleManager, Tracks tracks) {
        // Log current audio track
        TrackGroup currentAudioTrack = null;
        for (Tracks.Group trackGroup : tracks.getGroups()) {
            if (trackGroup.isSelected() && trackGroup.getType() == C.TRACK_TYPE_AUDIO) {
                currentAudioTrack = trackGroup.getMediaTrackGroup();
                break;
            }
        }

        // Log current subtitle track
        TrackGroup currentSubtitleTrack = null;
        for (Tracks.Group trackGroup : tracks.getGroups()) {
            if (trackGroup.isSelected() && trackGroup.getType() == C.TRACK_TYPE_TEXT) {
                currentSubtitleTrack = trackGroup.getMediaTrackGroup();
                break;
            }
        }


        // Create event data
        Map<String, Object> trackInfo = new HashMap<String, Object>();

        if (currentAudioTrack != null) {
            Format audioFormat = currentAudioTrack.getFormat(0);
            Map<String, Object> audioInfo = new HashMap<String, Object>();
            audioInfo.put("id", audioFormat.id);
            audioInfo.put("language", audioFormat.language);
            audioInfo.put("label", audioFormat.label);
            audioInfo.put("codecs", audioFormat.codecs);
            audioInfo.put("bitrate", audioFormat.bitrate);
            audioInfo.put("channelCount", audioFormat.channelCount);
            audioInfo.put("sampleRate", audioFormat.sampleRate);
            trackInfo.put("audioTrack", audioInfo);
        }

        Map<String, Object> subtitleInfo = new HashMap<String, Object>();
        if (currentSubtitleTrack != null) {
            Format subtitleFormat = currentSubtitleTrack.getFormat(0);
            subtitleInfo.put("id", subtitleFormat.id);
            subtitleInfo.put("language", subtitleFormat.language);
            subtitleInfo.put("label", subtitleFormat.label);
            subtitleInfo.put("codecs", subtitleFormat.codecs);
            subtitleInfo.put("containerMimeType", subtitleFormat.containerMimeType);
            subtitleInfo.put("sampleMimeType", subtitleFormat.sampleMimeType);

        } else {
            subtitleInfo.put("id", TrackUtils.DISABLED_TRACK);
        }
        trackInfo.put("subtitleTrack", subtitleInfo);

        NotificationCenter.defaultCenter().postNotification("playerTracksChanged", trackInfo);

        subtitleManager.refreshSubtitleButton();
    }
}
