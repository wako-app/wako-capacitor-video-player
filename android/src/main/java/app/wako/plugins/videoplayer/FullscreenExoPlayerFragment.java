package app.wako.plugins.videoplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.media3.cast.CastPlayer;
import androidx.media3.cast.SessionAvailabilityListener;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import app.wako.plugins.videoplayer.Components.SubtitleItem;
import app.wako.plugins.videoplayer.Components.SubtitleManager;
import app.wako.plugins.videoplayer.Notifications.NotificationCenter;
import app.wako.plugins.videoplayer.Utilities.HelperUtils;
import app.wako.plugins.videoplayer.Utilities.TrackUtils;

/**
 * Fragment that handles full-screen video playback using ExoPlayer.
 * Provides functionality for video playback, subtitle display, picture-in-picture mode,
 * Chromecast integration, and various playback controls.
 */
@UnstableApi
public class FullscreenExoPlayerFragment extends Fragment implements KeyEvent.Callback {
    public String videoUrl;
    public Float playbackRate;

    public ArrayList<SubtitleItem> subtitles;
    public String preferredLocale;
    public JSObject subTitleOptions;
    public Boolean isTvDevice;
    public Boolean shouldExitOnEnd;
    public Boolean shouldLoopOnEnd;
    public Boolean showControls;
    public String displayMode = "all";
    public String videoTitle;
    public String videoSubtitle;
    public String themeColor;
    public Boolean isChromecastEnabled;
    public String posterUrl;    // Track selection fields
    public String subtitleTrackId;
    public String subtitleLocale;
    public String audioTrackId;
    public String audioLocale;
    public long startAtSec;

    private static final String TAG = FullscreenExoPlayerFragment.class.getName();

    public static final long UNKNOWN_TIME = -1L;

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

    private boolean firstReadyCalled = false;

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

        View controlsBackground = fragmentView.findViewById(R.id.exo_controls_background);

        videoProgressBar.setVisibility(View.GONE);
        currentTimeView.setVisibility(View.GONE);
        totalTimeView.setVisibility(View.GONE);
        timeSeparatorView.setVisibility(View.GONE);

        playerView.setShowPreviousButton(false);
        playerView.setShowNextButton(false);
        playerView.setShowFastForwardButton(false);
        playerView.setShowRewindButton(false);

        playerView.setShowSubtitleButton(true);


        if (isTvDevice) {
            isChromecastEnabled = false;
            displayMode = "landscape";
            resizeButton.setVisibility(View.GONE);

            if (controlsBackground != null) {
                controlsBackground.setBackgroundResource(R.color.black_80);
            }
        } else {
            if (controlsBackground != null) {
                controlsBackground.setBackgroundResource(R.color.black_50);
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
        if (!Objects.equals(themeColor, "")) {
            progressBar.getIndeterminateDrawable().setColorFilter(Color.parseColor(themeColor), android.graphics.PorterDuff.Mode.MULTIPLY);
            videoProgressBar.setPlayedColor(Color.parseColor(themeColor));
            videoProgressBar.setScrubberColor(Color.parseColor(themeColor));
        }

        playerView.requestFocus();

        controlsContainer.setVisibility(View.INVISIBLE);

        playerView.setControllerShowTimeoutMs(3000);
        playerView.setControllerVisibilityListener(new PlayerView.ControllerVisibilityListener() {
            @Override
            public void onVisibilityChanged(int visibility) {
                controlsContainer.setVisibility(visibility);

                controllerVisible = visibility == View.VISIBLE;
                controllerVisibleFully = playerView.isControllerFullyVisible();
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
        if (player != null) {
            player.seekTo(0);
            player.setVolume(currentVolume);
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
        if (isChromecastEnabled) mediaRouter.removeCallback(mediaRouterCallback);
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
            if (subtitleManager != null) {
                subtitleManager.reset();
            }
            if (mediaSession != null) {
                mediaSession.release();
                mediaSession = null;
            }

            videoType = null;
            playerView = null;
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
        if (playerView != null) playerView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LOW_PROFILE |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        );
    }

    /**
     * Restores system UI elements when exiting fullscreen mode.
     * Clears fullscreen flags and makes status bar visible again.
     */
    private void showSystemUI() {
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getActivity().getWindow().getDecorView().setSystemUiVisibility(View.VISIBLE);
    }

    /**
     * Creates and configures the ExoPlayer instance.
     * Sets up media sources, track selectors, renderers, and control parameters.
     * Prepares the player for playback with the specified video and subtitle content.
     */
    private void initializePlayer() {
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

        mediaSession = new MediaSession.Builder(fragmentContext, player).build();

        player.addListener(playerListener);

        playerView.setPlayer(player);

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build();
        player.setAudioAttributes(audioAttributes, true);


        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder().setUri(videoUri).setMimeType(videoType);


        this.subtitleManager.setTrackSelector(trackSelector);
        this.subtitleManager.setPlayer(player);
        this.subtitleManager.loadExternalSubtitles(subtitles, mediaItemBuilder);

        if (shouldLoopOnEnd) {
            player.setRepeatMode(Player.REPEAT_MODE_ONE);
        } else {
            player.setRepeatMode(Player.REPEAT_MODE_OFF);
        }

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


        this.subtitleManager.updateCustomSubtitleButton();

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
                    Toast.makeText(fragmentContext, "Video Url not found", Toast.LENGTH_SHORT).show();
                    playerExit();
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    stateString = "ExoPlayer.STATE_BUFFERING -";
                    progressBar.setVisibility(View.VISIBLE);
                    break;
                case ExoPlayer.STATE_READY:
                    stateString = "ExoPlayer.STATE_READY     -";
                    progressBar.setVisibility(View.GONE);
                    playerView.setUseController(showControls);
                    controlsContainer.setVisibility(View.INVISIBLE);

                    if (!firstReadyCalled) {
                        firstReadyCalled = true;
                        NotificationCenter.defaultCenter().postNotification("playerStateReady", info);

                        TrackUtils.selectTracksOldWay(player, trackSelector, subtitleTrackId, subtitleLocale, audioTrackId, audioLocale, preferredLocale);
                    }

                    subtitleManager.updateCustomSubtitleButton();

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
                    player.setVolume(currentVolume);
                    player.setPlayWhenReady(false);
                    if (shouldExitOnEnd) {
                        releasePlayer();
                        NotificationCenter.defaultCenter().postNotification("playerStateEnd", info);
                    }
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
                Log.v(TAG, "**** in onPlayWhenReadyChanged going to notify playerItemPlay ");
                NotificationCenter.defaultCenter().postNotification("playerItemPlay", info);
                resizeButton.setVisibility(View.VISIBLE);

            } else {
                Log.v(TAG, "**** in onPlayWhenReadyChanged going to notify playerItemPause ");
                NotificationCenter.defaultCenter().postNotification("playerItemPause", info);
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
            TrackUtils.onTracksChanged(subtitleManager, tracks);
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
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_BUTTON_SELECT:
                if (player == null) break;
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                    player.pause();
                } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                    player.play();
                } else if (player.isPlaying()) {
                    player.pause();
                } else {
                    player.play();
                }
                return true;

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
                    if (player == null) break;
                    long pos = player.getCurrentPosition();

                    long seekTo = pos - 10_000;
                    if (seekTo < 0) seekTo = 0;
                    player.setSeekParameters(SeekParameters.PREVIOUS_SYNC);
                    player.seekTo(seekTo);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_BUTTON_R2:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                if (!controllerVisibleFully || keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
                    if (player == null) break;
                    long pos = player.getCurrentPosition();
                    long seekTo = pos + 10_000;
                    long seekMax = player.getDuration();
                    if (seekMax != C.TIME_UNSET && seekTo > seekMax) seekTo = seekMax;
                    player.setSeekParameters(SeekParameters.NEXT_SYNC);
                    player.seekTo(seekTo);
                    return true;
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
        PlaybackParameters param = new PlaybackParameters(playbackRate);
        player.setPlaybackParameters(param);

        /* If the user start the cast before the player is ready and playing, then the video will start
          in the device and chromecast at the same time. This is to avoid that behaviour.*/
        if (!isCasting) {
            player.setPlayWhenReady(true);

        }
    }

    /**
     * Pauses playback.
     */
    public void pause() {
        if (player != null) player.setPlayWhenReady(false);
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
        return player.getVolume();
    }

    /**
     * Sets the volume level.
     *
     * @param _volume Volume level between 0.0 and 1.0
     */
    public void setVolume(float _volume) {
        float volume = Math.min(Math.max(0, _volume), 1L);
        player.setVolume(volume);
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
        Executor executor = Executors.newSingleThreadExecutor();
        Task<CastContext> task = CastContext.getSharedInstance(fragmentContext, executor);

        task.addOnCompleteListener(new OnCompleteListener<CastContext>() {
            @Override
            public void onComplete(Task<CastContext> task) {
                if (task.isSuccessful()) {
                    castContext = task.getResult();
                    castPlayer = new CastPlayer(castContext);
                    mediaRouter = MediaRouter.getInstance(fragmentContext);
                    mSelector = new MediaRouteSelector.Builder().addControlCategories(Arrays.asList(MediaControlIntent.CATEGORY_LIVE_AUDIO, MediaControlIntent.CATEGORY_LIVE_VIDEO)).build();

                    mediaRouteButtonColorWhite(mediaRouteButton);
                    if (castContext != null && castContext.getCastState() != CastState.NO_DEVICES_AVAILABLE)
                        mediaRouteButton.setVisibility(View.VISIBLE);

                    castStateListener = state -> {
                        if (state == CastState.NO_DEVICES_AVAILABLE) {
                            mediaRouteButton.setVisibility(View.GONE);
                        } else {
                            if (mediaRouteButton.getVisibility() == View.GONE) {
                                mediaRouteButton.setVisibility(View.VISIBLE);
                            }
                        }
                    };
                    CastButtonFactory.setUpMediaRouteButton(fragmentContext, mediaRouteButton);

                    MediaMetadata movieMetadata;
                    if (posterUrl != "") {
                        movieMetadata = new MediaMetadata.Builder().setTitle(videoTitle).setSubtitle(videoSubtitle).setMediaType(MediaMetadata.MEDIA_TYPE_MOVIE).setArtworkUri(Uri.parse(posterUrl)).build();
                        new setCastImage().execute();
                    } else {
                        movieMetadata = new MediaMetadata.Builder().setTitle(videoTitle).setSubtitle(videoSubtitle).build();
                    }
                    mediaItem = new MediaItem.Builder().setUri(videoUrl).setMimeType(MimeTypes.VIDEO_UNKNOWN).setMediaMetadata(movieMetadata).build();

                    castPlayer.setSessionAvailabilityListener(new SessionAvailabilityListener() {
                        @Override
                        public void onCastSessionAvailable() {
                            isCasting = true;
                            final Long videoPosition = player.getCurrentPosition();

                            resizeButton.setVisibility(View.GONE);
                            player.setPlayWhenReady(false);
                            castImage.setVisibility(View.VISIBLE);
                            castPlayer.setMediaItem(mediaItem, videoPosition);
                            playerView.setPlayer(castPlayer);
                            playerView.setControllerShowTimeoutMs(0);
                            playerView.setControllerHideOnTouch(false);
                            //We perform a click because for some weird reason, the layout is black until the user clicks on it
                            playerView.performClick();
                        }

                        @Override
                        public void onCastSessionUnavailable() {
                            isCasting = false;
                            final Long videoPosition = castPlayer.getCurrentPosition();

                            resizeButton.setVisibility(View.VISIBLE);
                            castImage.setVisibility(View.GONE);
                            playerView.setPlayer(player);
                            player.setPlayWhenReady(true);
                            player.seekTo(videoPosition);
                            playerView.setControllerShowTimeoutMs(3000);
                            playerView.setControllerHideOnTouch(true);
                        }
                    });

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
                } else {
                    Exception e = task.getException();
                    e.printStackTrace();
                }
            }
        });
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

}
