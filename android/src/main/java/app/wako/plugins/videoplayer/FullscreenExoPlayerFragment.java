package app.wako.plugins.videoplayer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.media3.cast.CastPlayer;
import androidx.media3.cast.SessionAvailabilityListener;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.RenderersFactory;
import androidx.media3.exoplayer.SeekParameters;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory;
import androidx.media3.extractor.ts.TsExtractor;
import androidx.media3.session.MediaSession;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.DefaultTimeBar;
import androidx.media3.ui.PlayerView;
import androidx.mediarouter.app.MediaRouteButton;
import androidx.mediarouter.media.MediaControlIntent;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;

import com.getcapacitor.JSObject;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.common.collect.ImmutableList;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import app.wako.plugins.videoplayer.Components.SubtitleItem;
import app.wako.plugins.videoplayer.Components.SubtitleManager;
import app.wako.plugins.videoplayer.Notifications.NotificationCenter;
import app.wako.plugins.videoplayer.Utilities.BrightnessControl;
import app.wako.plugins.videoplayer.Utilities.HelperUtils;
import app.wako.plugins.videoplayer.Utilities.SubtitleUtils;
import app.wako.plugins.videoplayer.Utilities.SystemUiHelper;
import app.wako.plugins.videoplayer.Utilities.TrackUtils;

/**
 * Fragment that handles full-screen video playback using ExoPlayer.
 * Provides functionality for video playback, subtitle display, picture-in-picture mode,
 * Chromecast integration, and various playback controls.
 */
@UnstableApi
public class FullscreenExoPlayerFragment extends Fragment {
    public String videoUrl;
    public Float playbackRate;
    public ArrayList<SubtitleItem> subtitles;
    public String preferredLocale;
    public JSObject subTitleOptions;
    public Boolean isTvDevice;
    public String displayMode = "all";
    public String videoTitle;
    public String videoSubtitle;
    public Boolean isChromecastEnabled;
    public String posterUrl;    // Track selection fields
    public String subtitleTrackId;
    public String subtitleLocale;
    public String audioTrackId;
    public String audioLocale;
    public long startAtSec;

    private static final String TAG = FullscreenExoPlayerFragment.class.getName();

    public static final long UNKNOWN_TIME = -1L;

    public static int volumeBoost = 0;

    private Player.Listener playerListener;
    private PlayerView playerView;
    private String videoType = null;
    private static ExoPlayer player;

    private Uri videoUri = null;
    private ProgressBar progressBar;
    private View fragmentView;
    private ImageButton closeButton;
    private ImageButton resizeButton;
    private LinearLayout controlsContainer;
    private static ImageView castImage;
    private DefaultTimeBar videoProgressBar;
    private TextView currentTimeView;
    private TextView totalTimeView;
    private TextView timeSeparatorView;
    private TextView liveText;
    private Context fragmentContext;
    private boolean isMuted = false;
    private float currentVolume = (float) 0.5;
    private DefaultTrackSelector trackSelector;

    private boolean isCasting = false;

    // Double tap gesture detector
    private GestureDetector gestureDetector;

    // Double tap indicators
    private TextView rewindIndicator;
    private TextView forwardIndicator;
    private Handler indicatorHandler = new Handler(Looper.getMainLooper());

    // Variables for gesture controls
    private float initialX;
    private float initialY;
    private float initialBrightness;
    private boolean isChangingVolume = false;
    private boolean isChangingBrightness = false;
    private boolean isChangingPosition = false;
    private boolean restorePlayState = false;
    private long initialPosition;
    private long totalDuration;
    private long lastMoveTime;
    private float lastX;
    private float lastMoveSpeed;
    private long accumulatedSeekMs = 0;
    private float lastY;

    // Thresholds for gesture detection
    private static final float SWIPE_THRESHOLD = 60f;
    // Fixed value for volume sensitivity (higher = less sensitive)
    private static final float VOLUME_CHANGE_FACTOR = 6000f;


    // Indicators for brightness, volume and seeking
    private TextView brightnessIndicator;
    private TextView volumeIndicator;
    private TextView seekIndicator;


    // BrightnessControl
    private BrightnessControl mBrightnessControl;
    private AudioManager mAudioManager;

    private boolean firstReadyCalled = false;

    // Tag for the instance state bundle.
    private static final String PLAYBACK_TIME = "play_time";

    private MediaSession mediaSession;


    private Integer resizeStatus = AspectRatioFrameLayout.RESIZE_MODE_FIT;
    private MediaRouteButton mediaRouteButton;
    private CastContext castContext;
    private CastPlayer castPlayer;
    private MediaItem mediaItem;
    private MediaRouter mediaRouter;
    private final MediaRouter.Callback mediaRouterCallback = new EmptyCallback();
    private MediaRouteSelector mSelector;
    private CastStateListener castStateListener = null;

    private SubtitleManager subtitleManager;
    public static boolean controllerVisible;
    public static boolean controllerVisibleFully;

    // Variables for volume management
    private float initialSystemVolume;
    private float systemMaxVolume;
    private static final String TAG_VOLUME = "VIDEO_VOLUME"; // More visible tag for logs
    private int lastSetVolume = -1; // To track the last set volume
    private static final int MAX_VOLUME = 100; // Maximum volume (100%)
    private int currentVolumePercent = 50; // Current volume level in percentage

    // Variables to detect if brightness is in auto mode
    private boolean isAutoBrightness = false;
    private boolean initialIsAutoBrightness = false;

    private float subtitlesScale = 1.0f;  // Add this variable at class level

    /**
     * Creates and configures the fragment view with all necessary UI components.
     * Sets up player controls, event listeners, and initializes video playback.
     *
     * @param inflater           The LayoutInflater object to inflate views
     * @param container          The parent view that the fragment UI should be attached to
     * @param savedInstanceState Previous state of the fragment if being re-constructed
     * @return The View for the fragment's UI
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentContext = container.getContext();
        fragmentView = inflater.inflate(R.layout.fragment_fs_exoplayer, container, false);


        // Initialization of AudioManager and save initial system volume
        mAudioManager = (AudioManager) fragmentContext.getSystemService(Context.AUDIO_SERVICE);

        if (!isTvDevice) {
            if (mAudioManager != null) {
                initialSystemVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                systemMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

                // Calculate initial volume percentage (rounded to the lower multiple of 10)
                float initialVolumeRatio = initialSystemVolume / systemMaxVolume;
                currentVolumePercent = Math.round(initialVolumeRatio * 100);
                if (currentVolumePercent <= MAX_VOLUME) {
                    currentVolumePercent = (currentVolumePercent / 10) * 10;
                }

                System.out.println("!!!! VIDEO_VOLUME: INITIAL system volume: " + initialSystemVolume + "/" + systemMaxVolume);
                Log.e(TAG_VOLUME, "INITIAL system volume: " + initialSystemVolume + "/" + systemMaxVolume);
            }

            // Initialization of brightness control and save initial brightness
            if (getActivity() != null) {
                mBrightnessControl = new BrightnessControl(getActivity());
                initialBrightness = mBrightnessControl.getScreenBrightness();

                // Detect if brightness is in auto mode
                initialIsAutoBrightness = (initialBrightness == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE);
                isAutoBrightness = initialIsAutoBrightness;
            }
        }

        // Initialize views
        controlsContainer = fragmentView.findViewById(R.id.linearLayout);
        playerView = fragmentView.findViewById(R.id.videoViewId);
        TextView titleView = fragmentView.findViewById(R.id.header_tv);
        TextView subtitleView = fragmentView.findViewById(R.id.header_below);
        progressBar = fragmentView.findViewById(R.id.indeterminateBar);
        videoProgressBar = fragmentView.findViewById(R.id.exo_progress);
        currentTimeView = fragmentView.findViewById(R.id.exo_position);
        totalTimeView = fragmentView.findViewById(R.id.exo_duration);
        timeSeparatorView = fragmentView.findViewById(R.id.exo_label_separation);
        liveText = fragmentView.findViewById(R.id.live_text);
        resizeButton = fragmentView.findViewById(R.id.exo_resize);
        castImage = fragmentView.findViewById(R.id.cast_image);
        mediaRouteButton = fragmentView.findViewById(R.id.media_route_button);
        closeButton = fragmentView.findViewById(R.id.exo_close);

        // Initialize double tap indicators
        rewindIndicator = fragmentView.findViewById(R.id.rewind_indicator);
        forwardIndicator = fragmentView.findViewById(R.id.forward_indicator);

        // Initialize gesture indicators
        brightnessIndicator = fragmentView.findViewById(R.id.brightness_indicator);
        volumeIndicator = fragmentView.findViewById(R.id.volume_indicator);
        seekIndicator = fragmentView.findViewById(R.id.seek_indicator);


        // Initialize the GestureDetector to detect double taps
        gestureDetector = new GestureDetector(fragmentContext, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (isCasting) {
                    // During casting, always show controls instead of toggling
                    playerView.showController();
                    return true;
                }

                // Check if tap is in the controls area (bottom of screen)
                float y = e.getY();
                int viewHeight = playerView.getHeight();
                int bottomExclusionZoneHeight = viewHeight / 7; // Bottom ~14% for progress bar area
                boolean inBottomControls = y > (viewHeight - bottomExclusionZoneHeight);

                // If tap is in controls area, let the native controller handle it
                if (inBottomControls) {
                    return false;
                }

                // Outside control areas, manually handle showing/hiding
                if (controllerVisibleFully) {
                    playerView.hideController();
                } else {
                    playerView.showController();
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                // Disable double taps during casting
                if (isCasting) {
                    return true;
                }

                if (player == null || player.isCurrentMediaItemLive()) {
                    return false;
                }

                float screenWidth = playerView.getWidth();
                float x = e.getX();

                if (x < screenWidth / 2) {
                    // Double tap on left side = rewind 10 seconds
                    long pos = player.getCurrentPosition();
                    long seekTo = pos - 10_000;
                    if (seekTo < 0) seekTo = 0;
                    player.setSeekParameters(SeekParameters.PREVIOUS_SYNC);
                    player.seekTo(seekTo);

                    // Show rewind indicator
                    showIndicator(rewindIndicator);
                } else {
                    // Double tap on right side = forward 10 seconds
                    long pos = player.getCurrentPosition();
                    long seekTo = pos + 10_000;
                    long seekMax = player.getDuration();
                    if (seekMax != C.TIME_UNSET && seekTo > seekMax) seekTo = seekMax;
                    player.setSeekParameters(SeekParameters.NEXT_SYNC);
                    player.seekTo(seekTo);

                    // Show forward indicator
                    showIndicator(forwardIndicator);
                }

                return true;
            }
        });

        // Now that playerView is initialized, we can attach the listener
        playerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Always pass the event to gestureDetector first for double tap
                boolean gestureResult = gestureDetector.onTouchEvent(event);

                // In casting mode, limit gestures
                if (isCasting) {
                    // Only allow simple taps that are handled by the gestureDetector
                    return true;
                }

                // If the gestureDetector has consumed the event (double tap), do nothing else
                if (gestureResult) {
                    return true;
                }

                // Get the height of the view for zone calculations
                int viewHeight = playerView.getHeight();
                int viewWidth = playerView.getWidth();

                // Define exclusion zones (approximation)
                int topExclusionZoneHeight = viewHeight / 10; // Top 10% for title area
                int bottomExclusionZoneHeight = viewHeight / 7; // Bottom ~14% for progress bar area

                // Check if touch is in exclusion zones
                float y = event.getY();
                boolean inTopExclusionZone = y < topExclusionZoneHeight;
                boolean inBottomExclusionZone = y > (viewHeight - bottomExclusionZoneHeight);

                // Ignore gesture controls in exclusion zones
                if (inTopExclusionZone || inBottomExclusionZone) {
                    // In exclusion zones, don't handle controller visibility
                    // as it conflicts with ExoPlayer native controls
                    return false;
                }

                // Handle other gestures
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = event.getX();
                        initialY = event.getY();
                        lastX = initialX;
                        lastMoveTime = System.currentTimeMillis();
                        lastMoveSpeed = 0;
                        accumulatedSeekMs = 0;
                        initialPosition = player != null ? player.getCurrentPosition() : 0;
                        totalDuration = player != null ? player.getDuration() : 0;
                        isChangingVolume = false;
                        isChangingBrightness = false;
                        isChangingPosition = false;
                        lastY = initialY;

                        // If in casting mode, disable advanced gestures
                        if (isCasting) {
                            return true;
                        }

                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // If in casting mode, avoid processing movements
                        if (isCasting) {
                            return true;
                        }

                        if (player != null && player.isCurrentMediaItemLive()) {
                            return true;
                        }

                        float deltaX = event.getX() - initialX;
                        float deltaY = event.getY() - initialY;
                        float currentY = event.getY();

                        // Determine gesture type (horizontal or vertical)
                        if (!isChangingVolume && !isChangingBrightness && !isChangingPosition) {
                            if (Math.abs(deltaX) > SWIPE_THRESHOLD && Math.abs(deltaX) > Math.abs(deltaY)) {
                                // Gesture horizontal - search
                                isChangingPosition = true;
                                if (player != null && player.isPlaying()) {
                                    player.pause();
                                    restorePlayState = true;
                                }
                            } else if (Math.abs(deltaY) > SWIPE_THRESHOLD) {
                                // Gesture vertical
                                float screenWidth = playerView.getWidth();
                                if (initialX < screenWidth / 2) {
                                    // Left side - Brightness
                                    isChangingBrightness = true;
                                    if (mBrightnessControl != null) {
                                        initialBrightness = mBrightnessControl.getScreenBrightness();
                                    }
                                } else {
                                    // Right side - Volume
                                    isChangingVolume = true;
                                }
                            }
                        }

                        // Handling position seeking
                        if (isChangingPosition && player != null) {
                            // Calculate movement speed (pixels per millisecond)
                            long currentTime = System.currentTimeMillis();
                            long timeDelta = currentTime - lastMoveTime;
                            float distance = event.getX() - lastX;

                            if (timeDelta > 0) {
                                lastMoveSpeed = Math.abs(distance) / timeDelta;
                            }

                            // Base seek amount for this movement (3000ms per 10% of screen width)
                            float baseSeekAmount = (Math.abs(distance) / playerView.getWidth()) * 30000;

                            // Speed multiplier: 1.0 for normal speed, up to 6.0 for very fast swipes
                            float speedMultiplier = Math.min(1 + (lastMoveSpeed * 3), 6.0f);

                            // Calculate seek change for this movement
                            long seekChangeForThisMove = (long) (baseSeekAmount * speedMultiplier);

                            // Add or subtract from accumulated seek based on direction
                            accumulatedSeekMs += distance > 0 ? seekChangeForThisMove : -seekChangeForThisMove;

                            // Calculate new position based on initial position and accumulated seek
                            long newPosition = Math.max(0, Math.min(totalDuration, initialPosition + accumulatedSeekMs));

                            // Calculate time difference from initial position
                            long timeDifference = newPosition - initialPosition;

                            // Update tracking variables
                            lastX = event.getX();
                            lastMoveTime = currentTime;

                            // Display indicator with rounded background
                            seekIndicator.setVisibility(View.VISIBLE);
                            seekIndicator.setBackgroundResource(R.drawable.rounded_black_background);

                            // Determine prefix (+ or -) based on direction
                            String prefix = timeDifference >= 0 ? "+" : "-";

                            // Format absolute time difference as "00:00"
                            String formattedDifference = formatTime(Math.abs(timeDifference));

                            // Display in "+00:00" or "-00:00" format
                            seekIndicator.setText(prefix + formattedDifference);

                            // Apply seek
                            player.seekTo(newPosition);
                        }

                        // Brightness management
                        if (isChangingBrightness && mBrightnessControl != null) {
                            // Calculate brightness adjustment based on vertical movement with reduced sensitivity
                            float brightnessChange = -deltaY / (playerView.getHeight()); // Reduced sensitivity
                            float newBrightness;

                            // If we are near the bottom of the screen and lowering brightness
                            if (initialBrightness <= 0.01f && brightnessChange < 0) {
                                // Switch to auto mode
                                newBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
                                isAutoBrightness = true;
                                brightnessIndicator.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_brightness_auto_24dp), null, null, null);
                                brightnessIndicator.setText("Auto");
                            } else {
                                // Otherwise, adjust brightness normally
                                isAutoBrightness = false;
                                newBrightness = Math.min(Math.max(initialBrightness + brightnessChange, 0.01f), 1f);
                                int brightnessPercentage = (int) (newBrightness * 100);
                                brightnessIndicator.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_brightness_medium_24), null, null, null);
                                brightnessIndicator.setText(brightnessPercentage + "%");
                            }

                            // Apply brightness
                            mBrightnessControl.setScreenBrightness(newBrightness);

                            // Apply rounded background and show indicator
                            brightnessIndicator.setBackgroundResource(R.drawable.rounded_black_background);
                            brightnessIndicator.setVisibility(View.VISIBLE);
                        }

                        // Handling volume
                        if (isChangingVolume) {
                            // Determine if volume is increasing or decreasing based on movement since last position
                            boolean isIncreasing = currentY < lastY;  // Moving up increases volume

                            // Calculate base volume change with fixed sensitivity
                            float baseVolumeChange = Math.abs(currentY - lastY) / VOLUME_CHANGE_FACTOR;

                            // Get current volume level
                            float systemVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                            float maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                            float currentVolumeNormalized = systemVolume / maxVolume;

                            // Apply progressive sensitivity reduction for high volumes
                            float volumeChange;
                            if (currentVolumeNormalized >= 0.9f) {
                                volumeChange = baseVolumeChange * 0.05f;  // 95% reduction
                            } else if (currentVolumeNormalized >= 0.8f) {
                                volumeChange = baseVolumeChange * 0.1f;   // 90% reduction
                            } else if (currentVolumeNormalized >= 0.7f) {
                                volumeChange = baseVolumeChange * 0.2f;   // 80% reduction
                            } else if (currentVolumeNormalized >= 0.5f) {
                                volumeChange = baseVolumeChange * 0.4f;   // 60% reduction
                            } else {
                                volumeChange = baseVolumeChange * 0.6f;   // 40% base reduction
                            }

                            // Apply the volume change in the correct direction
                            float newVolume = currentVolumeNormalized + (isIncreasing ? volumeChange : -volumeChange);

                            // Calculate and apply the new volume in steps
                            setVolumeLevel(newVolume, isIncreasing);

                            // Update last Y position
                            lastY = currentY;

                            // Display volume indicator
                            volumeIndicator.setVisibility(View.VISIBLE);
                            volumeIndicator.setBackgroundResource(R.drawable.rounded_black_background);

                            if (currentVolumePercent == 0) {
                                // Volume at 0%
                                volumeIndicator.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_volume_off_24dp), null, null, null);
                                volumeIndicator.setText("0%");
                            } else {
                                // Normal volume
                                volumeIndicator.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.ic_volume_up_24dp), null, null, null);
                                volumeIndicator.setText(currentVolumePercent + "%");
                            }
                        }

                        return true;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // If in casting mode, ignore the end of the gesture
                        if (isCasting) {
                            return true;
                        }

                        // Hide indicators after a short delay
                        if (isChangingBrightness || isChangingVolume || isChangingPosition) {
                            indicatorHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    hideAllIndicators();
                                }
                            }, 500);

                            // Restore playback if it was paused for seeking
                            if (isChangingPosition && restorePlayState && player != null) {
                                // Sauvegarde du volume actuel avant de reprendre la lecture
                                final float currentPlayerVolume = player.getVolume();
                                
                                player.play();
                                restorePlayState = false;
                                
                                // Assurons-nous que le volume ne change pas après le seeking
                                player.setVolume(currentPlayerVolume);
                            }

                            return true;
                        }

                        // For a simple tap (not a volume/brightness/position gesture),
                        // do nothing here and let the gestureDetector handle it via onSingleTapConfirmed
                        return false;
                }

                return false;
            }
        });


        videoProgressBar.setVisibility(View.GONE);
        currentTimeView.setVisibility(View.GONE);
        totalTimeView.setVisibility(View.GONE);
        timeSeparatorView.setVisibility(View.GONE);

        playerView.setShowPreviousButton(false);
        playerView.setShowNextButton(false);

     
        if (isTvDevice) {
            isChromecastEnabled = false;
            displayMode = "landscape";
            resizeButton.setVisibility(View.GONE);

             if(closeButton != null) {
                closeButton.setVisibility(View.GONE);
            }
        } else {
            FrameLayout topBar = fragmentView.findViewById(R.id.top_bar);
            if(topBar != null) {
                topBar.setBackgroundResource(R.color.transparent);
            }

            FrameLayout bottomBar = fragmentView.findViewById(R.id.exo_bottom_bar);
            if(bottomBar != null) {
                bottomBar.setBackgroundResource(R.color.transparent);
            }

            ConstraintLayout constraintLayout = fragmentView.findViewById(R.id.exo_constraint_layout);
            if (constraintLayout != null) {
                constraintLayout.setBackgroundResource(R.color.black_50);
            }
        }

        this.subtitleManager = new SubtitleManager(fragmentContext, fragmentView, subTitleOptions.has("foregroundColor") ? subTitleOptions.getString("foregroundColor") : "", subTitleOptions.has("backgroundColor") ? subTitleOptions.getString("backgroundColor") : "", subTitleOptions.has("fontSize") ? subTitleOptions.getInteger("fontSize") : 16, preferredLocale);

        Activity fragmentActivity = getActivity();
        if (displayMode.equals("landscape")) {
            assert fragmentActivity != null;
            fragmentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        if (displayMode.equals("portrait")) {
            assert fragmentActivity != null;
            fragmentActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        playerView.setUseController(true);


        Log.v(TAG, "isChromecastEnabled: " + isChromecastEnabled);
        if (!isChromecastEnabled) {
            mediaRouteButton.setVisibility(View.GONE);
        } else {
            initializeCastService();
        }

        Log.v(TAG, "title: " + videoTitle);
        if (!Objects.equals(videoTitle, "")) {
            titleView.setText(videoTitle);
        }
        Log.v(TAG, "videoSubtitle: " + videoSubtitle);
        if (!Objects.equals(videoSubtitle, "")) {
            subtitleView.setText(videoSubtitle);
        }


        playerView.requestFocus();

        controlsContainer.setVisibility(View.INVISIBLE);

        playerView.setControllerShowTimeoutMs(3000);
        playerView.setControllerHideOnTouch(false);
        playerView.setControllerVisibilityListener(new PlayerView.ControllerVisibilityListener() {
            @Override
            public void onVisibilityChanged(int visibility) {
                controlsContainer.setVisibility(visibility);

                controllerVisible = visibility == View.VISIBLE;
                controllerVisibleFully = playerView.isControllerFullyVisible();
                
                // // Adjust system bars visibility based on controller visibility
                // if (visibility == View.VISIBLE) {
                //     showSystemUI();
                // } else {
                //     hideSystemUi();
                // }
            }
        });


        Log.v(TAG, "display url: " + videoUrl);
        if (subtitles != null && !subtitles.isEmpty()) {
            for (SubtitleItem subtitle : subtitles) {
                Log.v(TAG, "display subtitle url: " + subtitle.url);
                if (subtitle.name != null) {
                    Log.v(TAG, "display subtitle name: " + subtitle.name);
                }
                if (subtitle.lang != null) {
                    Log.v(TAG, "display subtitle lang: " + subtitle.lang);
                }
            }
        }

        // check if the video file exists, if not leave
        videoUri = Uri.parse(videoUrl);
        Log.v(TAG, "display video url: " + videoUri);

        // get video type
        videoType = HelperUtils.getVideoType(videoUri);
        Log.v(TAG, "display videoType: " + videoType);

        if (videoUri == null) {
            Log.d(TAG, "Video path wrong or type not supported");
            Toast.makeText(fragmentContext, "Video path wrong or type not supported", Toast.LENGTH_SHORT).show();
            return fragmentView;
        }
        // go fullscreen
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        // Make PlayerView focusable and give it initial focus

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                closeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        playerExit();
                    }
                });

                resizeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        resizePressed();
                    }
                });
            }
        });

        return fragmentView;
    }


    /**
     * Inner class for asynchronously loading and setting the cast image.
     * Downloads the poster image from a URL and displays it in the cast image view.
     */
    private class setCastImage extends AsyncTask<Void, Void, Bitmap> {
        /**
         * Performs the image download operation in a background thread.
         *
         * @param params Not used in this implementation
         * @return The downloaded Bitmap or null if download failed
         */
        protected Bitmap doInBackground(Void... params) {
            final String image = posterUrl;
            if (image != "") {
                try {
                    URL url = new URL(image);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream input = connection.getInputStream();
                    Bitmap myBitmap = BitmapFactory.decodeStream(input);
                    return myBitmap;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                return null;
            }
        }

        /**
         * Updates the UI with the downloaded image.
         *
         * @param result The downloaded Bitmap
         */
        @Override
        protected void onPostExecute(Bitmap result) {
            castImage.setImageBitmap(result);
        }
    }

    /**
     * Forces the player controller to become visible.
     */
    public void showController() {
        playerView.showController();
    }

    /**
     * Checks if the player controller is fully visible.
     *
     * @return True if controller is fully visible, false otherwise
     */
    public boolean isControllerIsFullyVisible() {
        return playerView.isControllerFullyVisible();
    }


    /**
     * Handles the back button press event.
     * Exits the player or transitions to picture-in-picture mode based on configuration.
     */
    private void backPressed() {
        // If we are casting, stop the cast before exiting
        if (isCasting && castPlayer != null) {
            try {
                // Save current position to resume later if needed
                Long currentPosition = castPlayer.getCurrentPosition();

                // Stop the cast (this will trigger onCastSessionUnavailable)
                CastContext.getSharedInstance().getSessionManager().endCurrentSession(true);

                // If exiting video during casting, we may want to 
                // save position for future playback
                if (player != null) {
                    player.seekTo(currentPosition);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error ending cast session", e);
            }
        }

        playerExit();
    }

    /**
     * Cycles through video resize modes (fit, fill, zoom).
     * Updates the aspect ratio of the player view accordingly.
     */
    private void resizePressed() {
        if (resizeStatus == AspectRatioFrameLayout.RESIZE_MODE_FIT) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
            resizeStatus = AspectRatioFrameLayout.RESIZE_MODE_FILL;
            resizeButton.setImageResource(R.drawable.ic_zoom);
        } else if (resizeStatus == AspectRatioFrameLayout.RESIZE_MODE_FILL) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
            resizeStatus = AspectRatioFrameLayout.RESIZE_MODE_ZOOM;
            resizeButton.setImageResource(R.drawable.ic_fit);
        } else {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            resizeStatus = AspectRatioFrameLayout.RESIZE_MODE_FIT;
            resizeButton.setImageResource(R.drawable.ic_expand);
        }
    }

    /**
     * Exits the player and releases resources.
     * Notifies listeners about player exit and restores system UI.
     */
    public void playerExit() {
        Map<String, Object> info = new HashMap<String, Object>() {
            {
                put("dismiss", "1");
                put("currentTime", getCurrentTime());
            }
        };

        // No longer restore initial system volume
        // But keep brightness restoration
        if (mBrightnessControl != null && getActivity() != null) {
            if (initialIsAutoBrightness) {
                // If brightness was in auto mode, restore to auto
                mBrightnessControl.setScreenBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE);
            } else {
                // If player already exists, verify its volume is correct
                if (player != null) {
                    float currentPlayerVolume = player.getVolume();
                    System.out.println("!!!! VIDEO_VOLUME: STATE_ENDED - Current volume: " + currentPlayerVolume +
                            ", System volume: " + (initialSystemVolume / systemMaxVolume));
                    Log.e(TAG_VOLUME, "STATE_ENDED - Current volume: " + currentPlayerVolume +
                            ", System volume: " + (initialSystemVolume / systemMaxVolume));
                }

                // Set volume to system value
                if (mAudioManager != null) {
                    float normalizedVolume = initialSystemVolume / systemMaxVolume;
                    setVolumeLevel(normalizedVolume);
                }
            }
        }

        releasePlayer();
        /* 
    Activity mAct = getActivity();
    int mOrient = mAct.getRequestedOrientation();
    if (mOrient == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
      mAct.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
*/
        try {
            NotificationCenter.defaultCenter().postNotification("playerFullscreenDismiss", info);
        } catch (Exception e) {
            Log.e(TAG, "Error in posting notification");
        }
    }

    /**
     * Format time in milliseconds to MM:SS string
     */
    private String formatTime(long timeMs) {
        long totalSeconds = timeMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    /**
     * Lifecycle method called when the fragment becomes visible to the user.
     * Initializes the player if needed and handles Chromecast integration.
     */
    @Override
    public void onStart() {
        super.onStart();
        //if (isChromecastEnabled && castContext != null) mediaRouter.addCallback(mSelector, mediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);

        if (Util.SDK_INT >= 24) {
            if (playerView != null) {
                // If cast is playing then it doesn't start the local player once get backs from background
                if (castContext != null && isChromecastEnabled && castPlayer.isCastSessionAvailable())
                    return;

                if (player == null) {
                    initializePlayer();
                }

            } else {
                getActivity().finishAndRemoveTask();
            }
        }
    }


    /**
     * Lifecycle method called when the fragment is being destroyed.
     * Performs final cleanup and resource release.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        // Clean up casting resources
        if (isChromecastEnabled) {
            // Remove callbacks
            if (mediaRouter != null) {
                mediaRouter.removeCallback(mediaRouterCallback);
            }

            // Release CastPlayer
            if (castPlayer != null) {
                try {
                    castPlayer.setSessionAvailabilityListener(null);
                    castPlayer.removeListener(playerListener);
                    castPlayer.release();
                    castPlayer = null;
                } catch (Exception e) {
                    Log.e(TAG, "Error releasing CastPlayer", e);
                }
            }

            // Remove CastContext listeners
            if (castContext != null && castStateListener != null) {
                try {
                    castContext.removeCastStateListener(castStateListener);
                } catch (Exception e) {
                    Log.e(TAG, "Error removing cast state listener", e);
                }
            }
        }

        releasePlayer();
        NotificationCenter.defaultCenter().removeAllNotifications();
    }

    /**
     * Lifecycle method called when the fragment is being paused.
     * Handles player state preservation
     */
    @Override
    public void onPause() {
        super.onPause();
        if (isChromecastEnabled) castContext.removeCastStateListener(castStateListener);

        // Save the volume and brightness state if the application is paused
        if (mAudioManager != null) {
            initialSystemVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }

        if (mBrightnessControl != null) {
            initialBrightness = mBrightnessControl.getScreenBrightness();
        }

        if (Util.SDK_INT < 24) {
            if (player != null) player.setPlayWhenReady(false);
            releasePlayer();
        } else {
            pause();
        }
    }

    /**
     * Releases the player and associated resources.
     * Called during lifecycle events or when the player is no longer needed.
     */
    public void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
            trackSelector = null;

            if (mediaSession != null) {
                mediaSession.release();
                mediaSession = null;
            }

            videoType = null;
            videoUri = null;
            subtitles.clear();
            isMuted = false;
            currentVolume = (float) 0.5;

            showSystemUI();
        }
    }

    /**
     * Lifecycle method called when the fragment is resumed.
     * Restores player state and handles UI visibility.
     */
    @Override
    public void onResume() {
        super.onResume();
        //if (isChromecastEnabled && castContext != null) castContext.addCastStateListener(castStateListener);
        hideSystemUi();
        if ((Util.SDK_INT < 24 || player == null)) {
            initializePlayer();
        } else {
            // If player already exists, verify its volume is correct
            if (mAudioManager != null && player != null) {
                float currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                float maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                float normalizedVolume = currentVolume / maxVolume;
                setVolumeLevel(normalizedVolume);
                System.out.println("!!!! VIDEO_VOLUME: Volume set in onResume: " + normalizedVolume);
                Log.e(TAG_VOLUME, "Volume set in onResume: " + normalizedVolume);
            }
        }

        // Ensure PlayerView has focus
        if (playerView != null) {
            playerView.post(new Runnable() {
                @Override
                public void run() {
                    playerView.requestFocus();
                }
            });
        }
    }

    /**
     * Hides system UI elements for immersive fullscreen playback.
     * Sets appropriate flags for a clean viewing experience.
     */
    @SuppressLint("InlinedApi")
    private void hideSystemUi() {
        updateSystemUiVisibility(false);
    }

    /**
     * Restores system UI elements when exiting fullscreen mode.
     * Clears fullscreen flags and makes status bar visible again.
     */
    private void showSystemUI() {
        updateSystemUiVisibility(true);
    }

    /**
     * Creates and configures the ExoPlayer instance.
     * Sets up media sources, track selectors, renderers, and control parameters.
     * Prepares the player for playback with the specified video and subtitle content.
     */
    private void initializePlayer() {
        // Check if there is an active casting session and stop it
        if (isChromecastEnabled) {
            try {
                CastContext castCtx = CastContext.getSharedInstance();
                SessionManager sessionManager = castCtx.getSessionManager();
                if (sessionManager.getCurrentCastSession() != null) {
                    Log.d(TAG, "Detected active cast session, ending it for new video");
                    // Stop current session so new video takes over
                    sessionManager.endCurrentSession(true);
                    // Wait a short moment to ensure the session is ended
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Sleep interrupted", e);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking/ending cast session", e);
            }
        }

        if (player != null) {
            releasePlayer();
        }

        // Save the requested initial position
        long initialPosition = startAtSec > 0 ? startAtSec * 1000 : 0;
        Log.d(TAG, "Requested initial position: " + initialPosition + "ms (startAtSec=" + startAtSec + ")");

        // Enable audio libs
        DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory().setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS).setTsExtractorTimestampSearchBytes(1500 * TsExtractor.TS_PACKET_SIZE);

        @SuppressLint("WrongConstant") RenderersFactory renderersFactory = new DefaultRenderersFactory(fragmentContext).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);

        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter.Builder(fragmentContext).build();
        AdaptiveTrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory();

        trackSelector = new DefaultTrackSelector(fragmentContext, videoTrackSelectionFactory);

        trackSelector.setParameters(
                trackSelector.buildUponParameters()
                        .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT | C.SELECTION_FLAG_FORCED)
        );

        playerListener = new PlayerListener();

        LoadControl loadControl = new DefaultLoadControl();

        player = new ExoPlayer.Builder(fragmentContext, renderersFactory).setSeekBackIncrementMs(10000).setSeekForwardIncrementMs(10000).setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .setBandwidthMeter(bandwidthMeter)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(fragmentContext, extractorsFactory)).build();

        // Set volume immediately after creation, but only if not on TV
        if (!isTvDevice && mAudioManager != null) {
            float normalizedVolume = initialSystemVolume / systemMaxVolume;
            setVolumeLevel(normalizedVolume);
        }

        if (mediaSession != null) {
            mediaSession.release();
        }
        mediaSession = new MediaSession.Builder(fragmentContext, player).build();

        player.addListener(playerListener);

        playerView.setPlayer(player);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build();
        player.setAudioAttributes(audioAttributes, true);

        // Re-set volume after setting audio attributes, but only if not on TV
        if (!isTvDevice && mAudioManager != null) {
            float normalizedVolume = initialSystemVolume / systemMaxVolume;
            setVolumeLevel(normalizedVolume);
        }

        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder().setUri(videoUri).setMimeType(videoType);


        this.subtitleManager.setTrackSelector(trackSelector);
        this.subtitleManager.setPlayer(player);
        this.subtitleManager.loadExternalSubtitles(subtitles, mediaItemBuilder);

        player.setRepeatMode(Player.REPEAT_MODE_OFF);

        MediaItem mediaItem = mediaItemBuilder.build();

        // Use the saved initial position
        player.setMediaItem(mediaItem, initialPosition);

        player.prepare();

        player.setPlayWhenReady(true);

        ImmutableList<Tracks.Group> trackGroups = player.getCurrentTracks().getGroups();
        for (int i = 0; i < trackGroups.size(); i++) {
            Tracks.Group group = trackGroups.get(i);
            if (group.getType() == C.TRACK_TYPE_TEXT) {
                Log.d(TAG, "Subtitles: ${group.mediaTrackGroup.getFormat(0).language}, selected: ${group.isSelected}");
            }
        }

        playerView.showController();


        this.subtitleManager.refreshSubtitleButton();

        this.subtitleManager.setSubtitleStyle();

        NotificationCenter.defaultCenter().postNotification("initializePlayer", null);
    }


    /**
     * Inner class that listens to player events and handles appropriate responses.
     * Manages state transitions, track changes, and notifications to other components.
     */
    private class PlayerListener implements Player.Listener {
        /**
         * Called when the playback state changes.
         * Handles different states (idle, buffering, ready, ended) and updates UI accordingly.
         *
         * @param state The new playback state
         */
        @Override
        public void onPlaybackStateChanged(int state) {
            String stateString;
            final long currentTime = player != null ? (player.isCurrentMediaItemLive() ? 0 : player.getCurrentPosition() / 1000) : 0;

            Map<String, Object> info = new HashMap<String, Object>() {
                {
                    put("currentTime", String.valueOf(currentTime));
                }
            };

            switch (state) {
                case ExoPlayer.STATE_IDLE:
                    stateString = "ExoPlayer.STATE_IDLE      -";
                    Toast.makeText(fragmentContext, "Video URL cannot be played", Toast.LENGTH_SHORT).show();
                    playerExit();
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    stateString = "ExoPlayer.STATE_BUFFERING -";
                    progressBar.setVisibility(View.VISIBLE);
                    break;
                case ExoPlayer.STATE_READY:
                    stateString = "ExoPlayer.STATE_READY     -";
                    progressBar.setVisibility(View.GONE);
                    playerView.setUseController(true);
                    controlsContainer.setVisibility(View.INVISIBLE);
                    
                    // Set volume when player is ready, but only if not on TV and if it's the first time
                    if (!isTvDevice && mAudioManager != null && !firstReadyCalled) {
                        float systemVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                        float maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        float normalizedVolume = systemVolume / maxVolume;
                        
                        // Check current player volume
                        if (player != null) {
                            float currentPlayerVolume = player.getVolume();
                            System.out.println("!!!! VIDEO_VOLUME: STATE_READY - Current volume: " + currentPlayerVolume +
                                    ", System volume: " + normalizedVolume);
                            Log.e(TAG_VOLUME, "STATE_READY - Current volume: " + currentPlayerVolume +
                                    ", System volume: " + normalizedVolume);
                        }

                        // Set volume to system value only if it's the first time
                        setVolumeLevel(normalizedVolume);
                    }

                    if (!firstReadyCalled) {
                        firstReadyCalled = true;
                        NotificationCenter.defaultCenter().postNotification("playerStateReady", info);
                        TrackUtils.selectTracksOldWay(player, trackSelector, subtitleTrackId, subtitleLocale, audioTrackId, audioLocale, preferredLocale);

                        setSubtitleTextSize();
                    }

                    subtitleManager.refreshSubtitleButton();

                    // We show progress bar, position and duration only when the video is not live
                    if (!player.isCurrentMediaItemLive()) {
                        videoProgressBar.setVisibility(View.VISIBLE);
                        currentTimeView.setVisibility(View.VISIBLE);
                        totalTimeView.setVisibility(View.VISIBLE);
                        timeSeparatorView.setVisibility(View.VISIBLE);
                        playerView.setShowFastForwardButton(true);
                        playerView.setShowRewindButton(true);
                    } else {
                        liveText.setVisibility(View.VISIBLE);
                    }
                    break;
                case ExoPlayer.STATE_ENDED:
                    stateString = "ExoPlayer.STATE_ENDED     -";
                    Log.v(TAG, "**** in ExoPlayer.STATE_ENDED going to notify playerItemEnd ");

                    player.seekTo(0);
                    player.setPlayWhenReady(false);

                    releasePlayer();
                    NotificationCenter.defaultCenter().postNotification("playerStateEnd", info);

                    break;
                default:
                    stateString = "UNKNOWN_STATE             -";
                    break;
            }
            Log.v(TAG, stateString + " currentTime: " + currentTime);
        }

        /**
         * Called when the player's play-when-ready state changes.
         * Handles play/pause state changes and sends appropriate notifications.
         *
         * @param playWhenReady Whether playback should proceed when ready
         * @param reason        The reason for the change
         */
        @Override
        public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
            final long currentTime = player != null ? (player.isCurrentMediaItemLive() ? 0 : player.getCurrentPosition() / 1000) : 0;

            Map<String, Object> info = new HashMap<String, Object>() {
                {
                    put("currentTime", String.valueOf(currentTime));
                }
            };

            if (playWhenReady) {
                // Ensure volume is correctly set when playback starts, but only if not on TV
                // and only after the initial setup, not during seeking operations
                if (!isTvDevice && mAudioManager != null && firstReadyCalled && !isChangingPosition) {
                    float currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    float maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    float normalizedVolume = currentVolume / maxVolume;
                    setVolumeLevel(normalizedVolume);
                    System.out.println("!!!! VIDEO_VOLUME: Volume set in onPlayWhenReadyChanged: " + normalizedVolume);
                    Log.e(TAG_VOLUME, "Volume set in onPlayWhenReadyChanged: " + normalizedVolume);
                }

                Log.v(TAG, "**** in onPlayWhenReadyChanged going to notify playerItemPlay ");
                NotificationCenter.defaultCenter().postNotification("playerItemPlay", info);
                if (!isTvDevice) {
                    resizeButton.setVisibility(View.VISIBLE);
                }
            } else {
                Log.v(TAG, "**** in onPlayWhenReadyChanged going to notify playerItemPause ");
                NotificationCenter.defaultCenter().postNotification("playerItemPause", info);
            }

            int playbackState = player.getPlaybackState();
            if (playWhenReady && playbackState == Player.STATE_IDLE) {
                releasePlayer();
            }
        }


        /**
         * Called when the available or selected tracks change.
         * Updates track information and notifies listeners about track changes.
         *
         * @param tracks The available tracks information
         */
        @Override
        public void onTracksChanged(Tracks tracks) {
            // Restore TrackUtils call
            TrackUtils.onTracksChanged(subtitleManager, tracks);

            // Verify one last time that volume is correct
            if (mAudioManager != null && player != null) {
                float currentPlayerVolume = player.getVolume();
                float systemVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                float maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                float normalizedVolume = systemVolume / maxVolume;

                if (Math.abs(currentPlayerVolume - normalizedVolume) > 0.05f) {
                    setVolumeLevel(normalizedVolume);
                    System.out.println("!!!! VIDEO_VOLUME: Volume adjusted in onTracksChanged: " + normalizedVolume);
                    Log.e(TAG_VOLUME, "Volume adjusted in onTracksChanged: " + normalizedVolume);
                }
            }
        }
    }


    /**
     * Save instance state
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the current playback position (in milliseconds) to the
        // instance state bundle.
        if (player != null) {
            outState.putInt(PLAYBACK_TIME, (int) player.getCurrentPosition());
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        try {
            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_BUTTON_SELECT:
                    if (player == null) {
                        Log.w(TAG, "Media key event received but player is null: " + keyCode);
                        break;
                    }
                    try {
                        if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                            player.pause();
                            Log.d(TAG, "Media PAUSE key pressed - player paused");
                        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                            player.play();
                            Log.d(TAG, "Media PLAY key pressed - player started");
                        } else if (player.isPlaying()) {
                            player.pause();
                            Log.d(TAG, "Media PLAY/PAUSE key pressed - player was playing, now paused");
                        } else {
                            player.play();
                            Log.d(TAG, "Media PLAY/PAUSE key pressed - player was paused, now playing");
                        }
                        return true;
                    } catch (Exception e) {
                        Log.e(TAG, "Error handling media key: " + keyCode, e);
                        return false;
                    }

                case KeyEvent.KEYCODE_BUTTON_START:
                case KeyEvent.KEYCODE_BUTTON_A:
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_NUMPAD_ENTER:
                case KeyEvent.KEYCODE_SPACE:
                    if (player == null) break;
                    if (!controllerVisibleFully) {
                        if (player.isPlaying()) {
                            player.pause();
                        } else {
                            player.play();
                        }
                        return true;
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_BUTTON_L2:
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                    if (!controllerVisibleFully || keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
                        if (isCasting && castPlayer != null) {
                            // During casting, ensure consistent 10 second rewind
                            try {
                                long pos = castPlayer.getCurrentPosition();
                                long seekTo = Math.max(0, pos - 10_000);
                                castPlayer.seekTo(seekTo);
                            } catch (Exception e) {
                                Log.e(TAG, "Error seeking in cast player", e);
                            }
                            return true;
                        } else if (player != null) {
                            long pos = player.getCurrentPosition();
                            long seekTo = pos - 10_000;
                            if (seekTo < 0) seekTo = 0;
                            player.setSeekParameters(SeekParameters.PREVIOUS_SYNC);
                            player.seekTo(seekTo);
                            return true;
                        }
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_BUTTON_R2:
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                    if (!controllerVisibleFully || keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
                        if (isCasting && castPlayer != null) {
                            // During casting, ensure consistent 10 second forward
                            try {
                                long pos = castPlayer.getCurrentPosition();
                                long duration = castPlayer.getDuration();
                                long seekTo = Math.min(duration, pos + 10_000);
                                castPlayer.seekTo(seekTo);
                            } catch (Exception e) {
                                Log.e(TAG, "Error seeking in cast player", e);
                            }
                            return true;
                        } else if (player != null) {
                            long pos = player.getCurrentPosition();
                            long seekTo = pos + 10_000;
                            long seekMax = player.getDuration();
                            if (seekMax != C.TIME_UNSET && seekTo > seekMax) seekTo = seekMax;
                            player.setSeekParameters(SeekParameters.NEXT_SYNC);
                            player.seekTo(seekTo);
                            return true;
                        }
                    }
                    break;
                case KeyEvent.KEYCODE_BACK:
                    if (isTvDevice) {
                        if (controllerVisible && player != null && player.isPlaying()) {
                            playerView.hideController();
                        } else {
                            backPressed();
                        }
                    } else {
                        backPressed();
                    }
                    break;
                default:
                    if (!controllerVisibleFully) {
                        playerView.showController();
                        return true;
                    }
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onKeyDown handling keyCode: " + keyCode, e);
        }
        return false;
    }


    /**
     * Checks if the player is currently playing.
     *
     * @return True if the player is playing, false otherwise
     */
    public boolean isPlaying() {
        return player.isPlaying();
    }

    /**
     * Starts or resumes playback.
     */
    public void play() {
        try {
            if (player != null) {
                // Ensure volume is correctly set when playback starts
                if (mAudioManager != null) {
                    float currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    float maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    float normalizedVolume = currentVolume / maxVolume;
                    setVolumeLevel(normalizedVolume);
                    System.out.println("!!!! VIDEO_VOLUME: Volume set in play(): " + normalizedVolume);
                    Log.e(TAG_VOLUME, "Volume set in play(): " + normalizedVolume);
                }

                PlaybackParameters param = new PlaybackParameters(playbackRate);
                player.setPlaybackParameters(param);

                /* If the user start the cast before the player is ready and playing, then the video will start
                in the device and chromecast at the same time. This is to avoid that behaviour.*/
                if (!isCasting) {
                    player.setPlayWhenReady(true);
                    Log.v(TAG, "play() called - player started successfully");
                } else {
                    Log.v(TAG, "play() called during casting - ignoring for local player");
                }
            } else {
                Log.w(TAG, "play() called but player is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in play() method", e);
        }
    }

    /**
     * Pauses playback.
     */
    public void pause() {
        try {
            if (player != null) {
                player.setPlayWhenReady(false);
                Log.v(TAG, "pause() called - player paused successfully");
            } else {
                Log.w(TAG, "pause() called but player is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in pause() method", e);
        }
    }

    /**
     * Gets the total duration of the media in seconds.
     *
     * @return The duration in seconds
     */
    public int getDuration() {
        return player.getDuration() == UNKNOWN_TIME ? 0 : (int) (player.getDuration() / 1000);
    }

    /**
     * Gets the current playback position in seconds.
     *
     * @return The current position in seconds
     */
    public int getCurrentTime() {
        return player.getCurrentPosition() == UNKNOWN_TIME ? 0 : (int) (player.getCurrentPosition() / 1000);
    }

    /**
     * Sets the playback position to a specific time.
     *
     * @param timeSecond The target position in seconds
     */
    public void setCurrentTime(int timeSecond) {

        long seekPosition = player.getCurrentPosition() == UNKNOWN_TIME ? 0 : Math.min(Math.max(0, timeSecond * 1000), player.getDuration());
        player.seekTo(seekPosition);
    }

    /**
     * Gets the current volume level.
     *
     * @return Volume level between 0.0 and 1.0
     */
    public float getVolume() {
        if (mAudioManager != null) {
            int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            return (float) currentVolume / maxVolume;
        }
        return 0.5f;
    }

    /**
     * Sets the volume level for the player.
     *
     * @param _volume Volume level (0.0 to 1.0)
     */
    public void setVolume(float _volume) {
        setVolumeLevel(_volume);
    }

    /**
     * Gets the current playback rate.
     *
     * @return The playback rate multiplier (1.0 is normal speed)
     */
    public float getRate() {
        return playbackRate;
    }

    /**
     * Sets the playback rate.
     *
     * @param _rate The playback rate multiplier (1.0 is normal speed)
     */
    public void setRate(float _rate) {
        playbackRate = _rate;
        PlaybackParameters param = new PlaybackParameters(playbackRate);
        player.setPlaybackParameters(param);
    }

    /**
     * Mutes or unmutes the audio.
     *
     * @param _isMuted Whether audio should be muted
     */
    public void setMuted(boolean _isMuted) {
        isMuted = _isMuted;
        if (isMuted) {
            currentVolume = player.getVolume();
            player.setVolume(0L);
        } else {
            player.setVolume(currentVolume);
        }
    }

    /**
     * Checks if audio is currently muted.
     *
     * @return True if audio is muted, false otherwise
     */
    public boolean getMuted() {
        return isMuted;
    }

    /**
     * Sets the media route button color to white.
     * Used for Chromecast integration UI.
     *
     * @param button The MediaRouteButton to modify
     */
    private void mediaRouteButtonColorWhite(MediaRouteButton button) {
        if (button == null) return;
        Context castContext = new ContextThemeWrapper(getContext(), androidx.mediarouter.R.style.Theme_MediaRouter);

        TypedArray a = castContext.obtainStyledAttributes(null, androidx.mediarouter.R.styleable.MediaRouteButton, androidx.mediarouter.R.attr.mediaRouteButtonStyle, 0);
        Drawable drawable = a.getDrawable(androidx.mediarouter.R.styleable.MediaRouteButton_externalRouteEnabledDrawable);
        a.recycle();
        DrawableCompat.setTint(drawable, getContext().getResources().getColor(R.color.white));
        drawable.setState(button.getDrawableState());
        button.setRemoteIndicatorDrawable(drawable);
    }


    /**
     * Initializes the Chromecast service.
     * Sets up the CastContext and prepares for casting sessions.
     */
    private void initializeCastService() {
        if (fragmentContext == null) {
            Log.e(TAG, "Cannot initialize cast service: fragmentContext is null");
            return;
        }

        Executor executor = Executors.newSingleThreadExecutor();
        Task<CastContext> task = CastContext.getSharedInstance(fragmentContext, executor);

        task.addOnCompleteListener(new OnCompleteListener<CastContext>() {
            @Override
            public void onComplete(Task<CastContext> task) {
                if (task.isSuccessful()) {
                    try {
                        castContext = task.getResult();
                        castPlayer = new CastPlayer(castContext);
                        mediaRouter = MediaRouter.getInstance(fragmentContext);
                        mSelector = new MediaRouteSelector.Builder()
                                .addControlCategories(Arrays.asList(
                                        MediaControlIntent.CATEGORY_LIVE_AUDIO,
                                        MediaControlIntent.CATEGORY_LIVE_VIDEO))
                                .build();

                        // Check if mediaRouteButton is still valid (fragment not destroyed)
                        if (mediaRouteButton != null) {
                            mediaRouteButtonColorWhite(mediaRouteButton);

                            if (castContext != null && castContext.getCastState() != CastState.NO_DEVICES_AVAILABLE)
                                mediaRouteButton.setVisibility(View.VISIBLE);

                            CastButtonFactory.setUpMediaRouteButton(fragmentContext, mediaRouteButton);
                        } else {
                            Log.w(TAG, "mediaRouteButton is null, cannot set up cast button");
                        }

                        castStateListener = state -> {
                            if (mediaRouteButton == null) {
                                Log.w(TAG, "mediaRouteButton is null in castStateListener");
                                return;
                            }

                            if (state == CastState.NO_DEVICES_AVAILABLE) {
                                mediaRouteButton.setVisibility(View.GONE);
                            } else {
                                if (mediaRouteButton.getVisibility() == View.GONE) {
                                    mediaRouteButton.setVisibility(View.VISIBLE);
                                }
                            }
                        };

                        // Prepare metadata for casting
                        MediaMetadata movieMetadata;
                        if (posterUrl != null && !posterUrl.isEmpty()) {
                            movieMetadata = new MediaMetadata.Builder()
                                    .setTitle(videoTitle != null ? videoTitle : "")
                                    .setSubtitle(videoSubtitle != null ? videoSubtitle : "")
                                    .setMediaType(MediaMetadata.MEDIA_TYPE_MOVIE)
                                    .setArtworkUri(Uri.parse(posterUrl))
                                    .build();
                            new setCastImage().execute();
                        } else {
                            movieMetadata = new MediaMetadata.Builder()
                                    .setTitle(videoTitle != null ? videoTitle : "")
                                    .setSubtitle(videoSubtitle != null ? videoSubtitle : "")
                                    .build();
                        }

                        // Check if video URL is valid
                        if (videoUrl != null && !videoUrl.isEmpty()) {
                            mediaItem = new MediaItem.Builder()
                                    .setUri(videoUrl)
                                    .setMimeType(MimeTypes.VIDEO_UNKNOWN)
                                    .setMediaMetadata(movieMetadata)
                                    .build();
                        } else {
                            Log.e(TAG, "Cannot create mediaItem: videoUrl is null or empty");
                        }

                        // Set up session listeners
                        setupCastSessionListeners();

                    } catch (Exception e) {
                        Log.e(TAG, "Error initializing cast service", e);
                    }
                } else {
                    Exception e = task.getException();
                    Log.e(TAG, "Failed to get CastContext", e);
                }
            }
        });
    }

    /**
     * Sets up listeners for cast session events
     */
    private void setupCastSessionListeners() {
        if (castPlayer == null) {
            Log.e(TAG, "Cannot setup cast session listeners: castPlayer is null");
            return;
        }

        castPlayer.setSessionAvailabilityListener(new SessionAvailabilityListener() {
            @Override
            public void onCastSessionAvailable() {
                isCasting = true;

                // Check if playerView is still valid
                if (playerView == null) {
                    Log.e(TAG, "onCastSessionAvailable: playerView is null, fragment might have been destroyed");
                    return;
                }

                // Check if player is valid
                if (player == null) {
                    Log.e(TAG, "onCastSessionAvailable: player is null");
                    return;
                }

                final Long videoPosition = player.getCurrentPosition();

                // Hide resize button during casting
                if (resizeButton != null) {
                    resizeButton.setVisibility(View.GONE);
                }

                player.setPlayWhenReady(false);

                // Show casting indicator
                if (castImage != null) {
                    castImage.setVisibility(View.VISIBLE);
                }

                // Configure castPlayer and switch PlayerView
                if (mediaItem != null) {
                    // Set consistent seek increments (10 seconds for both rewind and forward)
                    try {
                        RemoteMediaClient remoteMediaClient = castContext.getSessionManager()
                                .getCurrentCastSession().getRemoteMediaClient();

                        if (remoteMediaClient != null) {
                            // Note: We cannot directly configure the skip time for RemoteMediaClient
                            // but we'll handle it in the UI interactions
                            Log.d(TAG, "Cast session established - media client ready");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error accessing cast remote media client", e);
                    }

                    castPlayer.setMediaItem(mediaItem, videoPosition);
                    playerView.setPlayer(castPlayer);
                    // Keep controller visible at all times in casting mode
                    playerView.setControllerShowTimeoutMs(0);
                    playerView.setControllerHideOnTouch(false);

                    // Show the controller immediately
                    playerView.showController();
                } else {
                    Log.e(TAG, "Cannot set media item: mediaItem is null");
                }
            }

            @Override
            public void onCastSessionUnavailable() {
                isCasting = false;

                // Check if castPlayer is still valid
                Long videoPosition = 0L;
                if (castPlayer != null) {
                    try {
                        videoPosition = castPlayer.getCurrentPosition();
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting cast player position", e);
                    }
                }

                // Check if playerView is still valid
                if (playerView == null) {
                    Log.e(TAG, "onCastSessionUnavailable: playerView is null, fragment might have been destroyed");
                    return;
                }

                // Check if player is still valid
                if (player == null) {
                    Log.e(TAG, "onCastSessionUnavailable: player is null, cannot continue");
                    return;
                }

                // Reset visual controls
                if (!isTvDevice) {
                    resizeButton.setVisibility(View.VISIBLE);
                }
                if (castImage != null) {
                    castImage.setVisibility(View.GONE);
                }

                // Make sure all indicators are hidden
                hideAllIndicators();

                // Restore the player configuration
                playerView.setPlayer(player);
                player.setPlayWhenReady(true);
                player.seekTo(videoPosition);
                playerView.setControllerShowTimeoutMs(3000);
                playerView.setControllerHideOnTouch(true);
            }
        });

        // Configure listener for castPlayer events
        castPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int state) {
                Map<String, Object> info = new HashMap<String, Object>() {
                    {
                        put("currentTime", String.valueOf(player.getCurrentPosition() / 1000));
                    }
                };
                switch (state) {
                    case CastPlayer.STATE_READY:
                        if (castPlayer.isPlaying()) {
                            NotificationCenter.defaultCenter().postNotification("playerItemPlay", info);
                        } else {
                            NotificationCenter.defaultCenter().postNotification("playerItemPause", info);
                        }
                        break;
                    default:
                        break;
                }
            }
        });

        castContext.addCastStateListener(castStateListener);
        mediaRouter.addCallback(mSelector, mediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }

    /**
     * Empty callback implementation for MediaRouter.
     * Used as a placeholder for the MediaRouter callback mechanism.
     */
    private final class EmptyCallback extends MediaRouter.Callback {
        // Empty implementation
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Add callback for back press
        requireActivity().getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isTvDevice) {
                    if (controllerVisible && player != null && player.isPlaying()) {
                        playerView.hideController();
                    } else {
                        backPressed();
                    }
                } else {
                    backPressed();
                }
            }
        });

        // Add key event interceptor
        requireActivity().getWindow().getDecorView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    return onKeyDown(keyCode, event);
                }
                return false;
            }
        });
    }

    /**
     * Displays an indicator for 800ms and then hides it.
     *
     * @param indicator The indicator to display
     */
    private void showIndicator(TextView indicator) {
        // Cancel any ongoing tasks to hide indicators
        indicatorHandler.removeCallbacksAndMessages(null);

        // Hide both indicators first
        rewindIndicator.setVisibility(View.GONE);
        forwardIndicator.setVisibility(View.GONE);

        // Show the requested indicator
        indicator.setAlpha(1.0f);
        indicator.setVisibility(View.VISIBLE);

        // Schedule the indicator to disappear after 800ms
        indicatorHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                indicator.animate()
                        .alpha(0.0f)
                        .setDuration(200)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                indicator.setVisibility(View.GONE);
                            }
                        });
            }
        }, 800);
    }

    private void updateSystemUiVisibility(boolean show) {
        // Use our helper to manage status bar visibility
        SystemUiHelper.toggleSystemUi(getActivity(), playerView, show);
    }

    /**
     * Utility method to set the volume level for both player and system audio
     *
     * @param volumeLevel  volume level between 0 and 1.0 (100%)
     * @param isIncreasing indicates if the volume is increasing (true) or decreasing (false)
     */
    private void setVolumeLevel(float volumeLevel, boolean isIncreasing) {
        // Do not manage volume on Android TV
        if (isTvDevice) {
            return;
        }

        if (mAudioManager == null) return;

        // Ensure volume is between 0 and 1.0 (100%)
        volumeLevel = Math.max(0f, Math.min(1.0f, volumeLevel));

        // Convert volume to percentage (0-100)
        int targetVolumePercent = Math.round(volumeLevel * 100);

        // Limit rate of change - adjust only by 1% at a time
        if (isIncreasing) {
            // If increasing, go up by 1%
            targetVolumePercent = Math.min(currentVolumePercent + 1, MAX_VOLUME);
        } else {
            // If decreasing, go down by 1%
            targetVolumePercent = Math.max(0, currentVolumePercent - 1);
        }

        // Store current value for future calls
        currentVolumePercent = targetVolumePercent;

        // Recalculate normalized value
        float normalizedVolume = targetVolumePercent / 100f;

        // Calculate system volume based on volumeLevel
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int targetVolume = Math.round(normalizedVolume * maxVolume);

        // Avoid setting the same volume multiple times
        if (lastSetVolume == targetVolume) {
            return;
        }

        // Set system volume
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0);
        lastSetVolume = targetVolume;

        // Also set player volume
        if (player != null) {
            player.setVolume(normalizedVolume);
        }

        System.out.println("!!!! VIDEO_VOLUME: Volume set to " + targetVolumePercent + "% (" + targetVolume + "/" + maxVolume + ")");
        Log.e(TAG_VOLUME, "Volume set to " + targetVolumePercent + "% (" + targetVolume + "/" + maxVolume + ")");
    }

    /**
     * Overload of setVolumeLevel without specifying direction
     */
    private void setVolumeLevel(float volumeLevel) {
        // By default, consider it as an increase
        setVolumeLevel(volumeLevel, volumeLevel > (currentVolumePercent / 100f));
    }


    private void setSubtitleTextSize() {
        final int orientation = getResources().getConfiguration().orientation;
        SubtitleUtils.setSubtitleTextSize(playerView, isTvDevice, subtitlesScale, orientation);
        SubtitleUtils.updateSubtitleStyle(playerView, subTitleOptions, isTvDevice, subtitlesScale, orientation);
    }


    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setSubtitleTextSize();
    }

    private void hideAllIndicators() {
        if (volumeIndicator != null) volumeIndicator.setVisibility(View.GONE);
        if (brightnessIndicator != null) brightnessIndicator.setVisibility(View.GONE);
        if (seekIndicator != null) seekIndicator.setVisibility(View.GONE);
        if (rewindIndicator != null) rewindIndicator.setVisibility(View.GONE);
        if (forwardIndicator != null) forwardIndicator.setVisibility(View.GONE);
    }

}

