package app.wako.plugins.videoplayer.Utilities;

import android.content.Context;
import android.media.AudioManager;
import androidx.media3.exoplayer.ExoPlayer;

import app.wako.plugins.videoplayer.FullscreenExoPlayerFragment;

public class VolumeControl {

    private static final int MAX_VOLUME_BOOST = 30;

    public static boolean isVolumeMax(AudioManager audioManager) {
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) >= audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    public static void adjustVolume(Context context, AudioManager audioManager, ExoPlayer player, boolean increase, boolean canBoost) {
        if (increase) {
            if (isVolumeMax(audioManager) && canBoost) {
                // If volume is already at maximum, increase boost
                if (FullscreenExoPlayerFragment.volumeBoost < MAX_VOLUME_BOOST) {
                    FullscreenExoPlayerFragment.volumeBoost++;
                }
            } else {
                // Augmenter le volume normal
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
            }
        } else {
            if (FullscreenExoPlayerFragment.volumeBoost > 0) {
                // If boost is active, first reduce it
                FullscreenExoPlayerFragment.volumeBoost--;
            } else {
                // Sinon diminuer le volume normal
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
            }
        }
        
        // Apply boost if necessary
        if (player != null) {
            // Volume multiplier goes from 1.0 (no boost) to 2.0 (max boost)
            float multiplier = 1.0f + (FullscreenExoPlayerFragment.volumeBoost / (float)MAX_VOLUME_BOOST);
            player.setVolume(multiplier);
        }
    }

    public static void setVolume(Context context, AudioManager audioManager, ExoPlayer player, float volume) {
        if (audioManager != null && player != null) {
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int targetVolume = (int)(volume * maxVolume);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0);
            player.setVolume(volume);
        }
    }
} 