package app.wako.plugins.videoplayer.Utilities;

import android.content.res.Resources;
import android.text.TextUtils;

import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.DefaultTrackNameProvider;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@UnstableApi
public class CustomDefaultTrackNameProvider extends DefaultTrackNameProvider {
    
    // Map to convert 3-letter language codes to 2-letter codes
    private static final Map<String, String> LANGUAGE_CODE_MAP = new HashMap<>();
    
    static {
        // Initialize common language code mappings
        LANGUAGE_CODE_MAP.put("eng", "GB"); // English
        LANGUAGE_CODE_MAP.put("fra", "FR"); // French
        LANGUAGE_CODE_MAP.put("deu", "DE"); // German
        LANGUAGE_CODE_MAP.put("ita", "IT"); // Italian
        LANGUAGE_CODE_MAP.put("spa", "ES"); // Spanish
        LANGUAGE_CODE_MAP.put("jpn", "JP"); // Japanese
        LANGUAGE_CODE_MAP.put("kor", "KR"); // Korean
        LANGUAGE_CODE_MAP.put("zho", "CN"); // Chinese
        LANGUAGE_CODE_MAP.put("rus", "RU"); // Russian
        LANGUAGE_CODE_MAP.put("ara", "SA"); // Arabic
        LANGUAGE_CODE_MAP.put("por", "PT"); // Portuguese
        LANGUAGE_CODE_MAP.put("nld", "NL"); // Dutch
        LANGUAGE_CODE_MAP.put("swe", "SE"); // Swedish
        LANGUAGE_CODE_MAP.put("hin", "IN"); // Hindi
        // Add more mappings as needed
    }
    
    public CustomDefaultTrackNameProvider(Resources resources) {
        super(resources);
    }

    @Override
    public String getTrackName(Format format) {
        String trackName = super.getTrackName(format);
        
        // Add language flag emoji if language is available
        if (!TextUtils.isEmpty(format.language)) {
            try {
                // Get country code for the flag emoji
                String countryCode = getCountryCodeFromLanguage(format.language);
                if (!TextUtils.isEmpty(countryCode)) {
                    String flagEmoji = countryCodeToEmojiFlag(countryCode);
                    trackName = flagEmoji + " " + trackName;
                } else {
                    // Fallback to language code display if no mapping found
                    trackName = "[" + format.language.toUpperCase() + "] " + trackName;
                }
            } catch (Exception e) {
                // Continue without adding flag in case of error
            }
        }
        
        if (format.sampleMimeType != null) {
            String sampleFormat = formatNameFromMime(format.sampleMimeType);
            if (sampleFormat == null) {
                sampleFormat = formatNameFromMime(format.codecs);
            }
            if (sampleFormat == null) {
                sampleFormat = format.sampleMimeType;
            }
            if (sampleFormat != null) {
                trackName += " (" + sampleFormat + ")";
            }
        }
        if (format.label != null) {
            if (!trackName.startsWith(format.label)) { // HACK
                trackName += " - " + format.label;
            }
        }
        return trackName;
    }
    
    /**
     * Gets appropriate country code for flag emoji from language code
     * Handles both 2-letter and 3-letter language codes
     * 
     * @param languageCode Language code from the track
     * @return 2-letter country code suitable for flag emoji
     */
    private String getCountryCodeFromLanguage(String languageCode) {
        if (TextUtils.isEmpty(languageCode)) {
            return "";
        }
        
        String langCode = languageCode.toLowerCase();
        
        // Check if it's a 3-letter code that needs mapping
        if (langCode.length() == 3 && LANGUAGE_CODE_MAP.containsKey(langCode)) {
            return LANGUAGE_CODE_MAP.get(langCode);
        }
        
        // Special case for English
        if (langCode.equals("en")) {
            return "GB"; // Use UK flag for English
        }
        
        // For 2-letter codes or unknown 3-letter codes, use first two letters
        // This works for most 2-letter language codes that match country codes
        if (langCode.length() >= 2) {
            return langCode.substring(0, 2).toUpperCase();
        }
        
        return "";
    }
    
    /**
     * Converts a two-letter ISO country code to an emoji flag
     * @param countryCode Two-letter ISO country code (e.g. "US", "FR")
     * @return Emoji flag for the country
     */
    private String countryCodeToEmojiFlag(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) {
            return "";
        }
        
        // Convert each letter to regional indicator symbol
        // Regional indicator symbols range: U+1F1E6 (A) to U+1F1FF (Z)
        int firstLetter = Character.codePointAt(countryCode, 0) - 'A' + 0x1F1E6;
        int secondLetter = Character.codePointAt(countryCode, 1) - 'A' + 0x1F1E6;
        
        // Combine the two regional indicator symbols to form the flag
        return new String(Character.toChars(firstLetter)) + new String(Character.toChars(secondLetter));
    }

    private String formatNameFromMime(final String mimeType) {
        if (mimeType == null) {
            return null;
        }
        switch (mimeType) {
            case MimeTypes.AUDIO_DTS:
                return "DTS";
            case MimeTypes.AUDIO_DTS_HD:
                return "DTS-HD";
            case MimeTypes.AUDIO_DTS_EXPRESS:
                return "DTS Express";
            case MimeTypes.AUDIO_TRUEHD:
                return "TrueHD";
            case MimeTypes.AUDIO_AC3:
                return "AC-3";
            case MimeTypes.AUDIO_E_AC3:
                return "E-AC-3";
            case MimeTypes.AUDIO_E_AC3_JOC:
                return "E-AC-3-JOC";
            case MimeTypes.AUDIO_AC4:
                return "AC-4";
            case MimeTypes.AUDIO_AAC:
                return "AAC";
            case MimeTypes.AUDIO_MPEG:
                return "MP3";
            case MimeTypes.AUDIO_MPEG_L2:
                return "MP2";
            case MimeTypes.AUDIO_VORBIS:
                return "Vorbis";
            case MimeTypes.AUDIO_OPUS:
                return "Opus";
            case MimeTypes.AUDIO_FLAC:
                return "FLAC";
            case MimeTypes.AUDIO_ALAC:
                return "ALAC";
            case MimeTypes.AUDIO_WAV:
                return "WAV";
            case MimeTypes.AUDIO_AMR:
                return "AMR";
            case MimeTypes.AUDIO_AMR_NB:
                return "AMR-NB";
            case MimeTypes.AUDIO_AMR_WB:
                return "AMR-WB";
            case MimeTypes.AUDIO_IAMF:
                return "IAMF";
            case MimeTypes.AUDIO_MPEGH_MHA1:
            case MimeTypes.AUDIO_MPEGH_MHM1:
                return "MPEG-H";

            case MimeTypes.APPLICATION_PGS:
                return "PGS";
            case MimeTypes.APPLICATION_SUBRIP:
                return "SRT";
            case MimeTypes.TEXT_SSA:
                return "SSA";
            case MimeTypes.TEXT_VTT:
                return "VTT";
            case MimeTypes.APPLICATION_TTML:
                return "TTML";
            case MimeTypes.APPLICATION_TX3G:
                return "TX3G";
            case MimeTypes.APPLICATION_DVBSUBS:
                return "DVB";
        }
        return null;
    }
}
