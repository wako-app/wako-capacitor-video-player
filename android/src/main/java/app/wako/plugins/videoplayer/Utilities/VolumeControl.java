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
                // Si le volume est déjà au maximum, augmenter le boost
                if (FullscreenExoPlayerFragment.volumeBoost < MAX_VOLUME_BOOST) {
                    FullscreenExoPlayerFragment.volumeBoost++;
                }
            } else {
                // Augmenter le volume normal
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0);
            }
        } else {
            if (FullscreenExoPlayerFragment.volumeBoost > 0) {
                // Si le boost est actif, d'abord le réduire
                FullscreenExoPlayerFragment.volumeBoost--;
            } else {
                // Sinon diminuer le volume normal
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, 0);
            }
        }
        
        // Appliquer le boost si nécessaire
        if (player != null) {
            // Le multiplicateur de volume va de 1.0 (pas de boost) à 2.0 (boost max)
            float multiplier = 1.0f + (FullscreenExoPlayerFragment.volumeBoost / (float)MAX_VOLUME_BOOST);
            player.setVolume(multiplier);
        }
    }
} 