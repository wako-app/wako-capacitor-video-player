package app.wako.plugins.videoplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Rational;
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

import androidx.annotation.RequiresApi;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.media3.cast.CastPlayer;
import androidx.media3.cast.SessionAvailabilityListener;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.RenderersFactory;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory;
import androidx.media3.extractor.ts.TsExtractor;
import androidx.media3.session.MediaSession;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.DefaultTimeBar;
import androidx.media3.ui.PlayerControlView;
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
import app.wako.plugins.videoplayer.Components.SubtitleManager;
import app.wako.plugins.videoplayer.Notifications.NotificationCenter;
import app.wako.plugins.videoplayer.Utilities.SubtitleUtils;

import java.io.File;
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

import org.json.JSONException;

/**
 * Fragment that handles full-screen video playback using ExoPlayer.
 * Provides functionality for video playback, subtitle display, picture-in-picture mode,
 * Chromecast integration, and various playback controls.
 */
@UnstableApi
public class FullscreenExoPlayerFragment extends Fragment {
    public String videoPath;
    public Float playbackRate;
    public String playerId;
    public static class SubtitleItem {
        public String url;
        public String name;
        public String lang;

        public SubtitleItem(String url, String name, String lang) {
            this.url = url;
            this.name = name;
            this.lang = lang;
        }
    }

    public ArrayList<SubtitleItem> subtitles;
    public String preferredLocale;
    public JSObject subTitleOptions;
    public JSObject requestHeaders;
    public Boolean isTvDevice;
    public Boolean isLocalFile;
    public Long videoId;
    public Boolean shouldExitOnEnd;
    public Boolean shouldLoopOnEnd;
    public Boolean isPipEnabled;
    public Boolean isBackgroundModeEnabled;
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
    private final List<String> supportedVideoFormats = Arrays.asList(
            "mp4",
            "webm",
            "ogv",
            "3gp",
            "flv",
            "dash",
            "mpd",
            "m3u8",
            "ism",
            "ytube",
            ""
    );
    private Player.Listener playerListener;
    private PlayerView playerView;
    private String videoType = null;
    private static ExoPlayer player;
    private boolean isFirstReady = true;
    private int currentMediaItemIndex = 0;
    private long playbackPosition = 0;
    private Uri videoUri = null;
    private ArrayList<Uri> subtitleUris = new ArrayList<>();
    private ProgressBar progressBar;
    private View fragmentView;
    private ImageButton closeButton;
    private ImageButton pipButton;
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
    private boolean isPictureInPicture = false;
    private DefaultTrackSelector trackSelector;
    // Current playback position (in milliseconds).
    private int lastKnownPosition;
    private int totalDuration;
    private static final int seekStep = 10000;
    private boolean isCasting = false;

    // Tag for the instance state bundle.
    private static final String PLAYBACK_TIME = "play_time";

    private MediaSession mediaSession;
    // private MediaSessionConnector mediaSessionConnector;
    private PlayerControlView.VisibilityListener visibilityListener;
    private PackageManager packageManager;
    private Boolean isPipModeEnabled = true;
    final Handler handler = new Handler();
    final Runnable pipCheckRunnable = new Runnable() {
        @RequiresApi(api = Build.VERSION_CODES.N)
        public void run() {
            checkPIPPermission();
        }
    };

    private Integer resizeStatus = AspectRatioFrameLayout.RESIZE_MODE_FIT;
    private MediaRouteButton mediaRouteButton;
    private CastContext castContext;
    private CastPlayer castPlayer;
    private MediaItem mediaItem;
    private MediaRouter mediaRouter;
    private final MediaRouter.Callback mediaRouterCallback = new EmptyCallback();
    private MediaRouteSelector mSelector;
    private CastStateListener castStateListener = null;
    private Boolean isPlayerReady = false;
    private SubtitleManager subtitleManager;


    /**
     * Creates and configures the fragment view with all necessary UI components.
     * Sets up player controls, event listeners, and initializes video playback.
     *
     * @param inflater The LayoutInflater object to inflate views
     * @param container The parent view that the fragment UI should be attached to
     * @param savedInstanceState Previous state of the fragment if being re-constructed
     * @return The View for the fragment's UI
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentContext = container.getContext();
        packageManager = fragmentContext.getPackageManager();
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
        pipButton = fragmentView.findViewById(R.id.exo_pip);


        videoProgressBar.setVisibility(View.GONE);
        currentTimeView.setVisibility(View.GONE);
        totalTimeView.setVisibility(View.GONE);
        timeSeparatorView.setVisibility(View.GONE);

        playerView.setShowPreviousButton(false);
        playerView.setShowNextButton(false);
        playerView.setShowFastForwardButton(false);
        playerView.setShowRewindButton(false);


        this.subtitleManager = new SubtitleManager(
                fragmentContext,
                fragmentView,
                subTitleOptions.has("foregroundColor") ? subTitleOptions.getString("foregroundColor") : "rgba(255,255,255,1)",
                subTitleOptions.has("backgroundColor") ? subTitleOptions.getString("backgroundColor") : "rgba(0,0,0,1)",
                subTitleOptions.has("fontSize") ? subTitleOptions.getInteger("fontSize") : 16,
                preferredLocale
        );

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

        //playerView.requestFocus();
        controlsContainer.setVisibility(View.INVISIBLE);
        playerView.setControllerShowTimeoutMs(3000);
        playerView.setControllerVisibilityListener(
                new PlayerView.ControllerVisibilityListener() {
                    @Override
                    public void onVisibilityChanged(int visibility) {
                        controlsContainer.setVisibility(visibility);
                    }
                }
        );


        Log.v(TAG, "display url: " + videoPath);
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
        File file = new File(videoPath);
        videoUri = Uri.parse(videoPath);
        Log.v(TAG, "display video url: " + videoUri);
        
        // Initialize subtitleUris
        subtitleUris.clear();
        if (subtitles != null && !subtitles.isEmpty()) {
            for (SubtitleItem subtitle : subtitles) {
                Uri subtitleUri = Uri.parse(subtitle.url);
                subtitleUris.add(subtitleUri);
                Log.v(TAG, "display subtitle uri: " + subtitleUri);
            }
        }

        // get video type
        videoType = getVideoType(videoUri);
        Log.v(TAG, "display videoType: " + videoType);

        if (videoUri != null || isLocalFile) {
            // go fullscreen
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            if (savedInstanceState != null) {
                lastKnownPosition = savedInstanceState.getInt(PLAYBACK_TIME);
            }

            getActivity()
                    .runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    // Set the onKey playerListener
                                    fragmentView.setFocusableInTouchMode(true);
                                    fragmentView.requestFocus();
                                    fragmentView.setOnKeyListener(
                                            new View.OnKeyListener() {
                                                @Override
                                                public boolean onKey(View v, int keyCode, KeyEvent event) {
                                                    if (event.getAction() == KeyEvent.ACTION_UP) {
                                                        long videoPosition = player.getCurrentPosition();
                                                        Log.v(TAG, "$$$$ onKey " + keyCode + " $$$$");
                                                        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) {
                                                            Log.v(TAG, "$$$$ Going to backpress $$$$");
                                                            backPressed();
                                                        } else if (isTvDevice) {
                                                            switch (keyCode) {
                                                                case KeyEvent.KEYCODE_DPAD_RIGHT:
                                                                    fastForward(videoPosition, 1);
                                                                    break;
                                                                case KeyEvent.KEYCODE_DPAD_LEFT:
                                                                    rewind(videoPosition, 1);
                                                                    break;
                                                                case KeyEvent.KEYCODE_DPAD_CENTER:
                                                                    play_pause();
                                                                    break;
                                                                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                                                                    fastForward(videoPosition, 2);
                                                                    break;
                                                                case KeyEvent.KEYCODE_MEDIA_REWIND:
                                                                    rewind(videoPosition, 2);
                                                                    break;
                                                            }
                                                        }
                                                        return true;
                                                    } else {
                                                        return false;
                                                    }
                                                }
                                            }
                                    );


                                    closeButton.setOnClickListener(
                                            new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    playerExit();
                                                }
                                            }
                                    );
                                    pipButton.setOnClickListener(
                                            new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    pictureInPictureMode();
                                                }
                                            }
                                    );
                                    resizeButton.setOnClickListener(
                                            new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    resizePressed();
                                                }
                                            }
                                    );
                                }
                            }
                    );
        } else {
            Log.d(TAG, "Video path wrong or type not supported");
            Toast.makeText(fragmentContext, "Video path wrong or type not supported", Toast.LENGTH_SHORT).show();
        }
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
       /* if (isCasting) {
            playerExit();
            return;
        }
        if (
                !isPictureInPicture &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) &&
                        isPipModeEnabled &&
                        isPipEnabled &&
                        isPlayerReady // <- isPlayerReady: this prevents a crash if the user presses back before the player is ready (when enters in pip mode and tries to get the aspect ratio)
        ) {
            pictureInPictureMode();
        } else {
            playerExit();
        }*/
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
        // We control if the user lock the screen when the player is in pip mode
        try {
            NotificationCenter.defaultCenter().postNotification("playerFullscreenDismiss", info);
        } catch (Exception e) {
            Log.e(TAG, "Error in posting notification");
        }
    }

    /**
     * Enters picture-in-picture mode if supported by the device.
     * Configures PIP parameters and adapts the UI for PIP display.
     */
    private void pictureInPictureMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            playerView.setUseController(false);
            playerView.setControllerAutoShow(false);
            controlsContainer.setVisibility(View.INVISIBLE);
            Log.v(TAG, "PIP break 1");
            // require android O or higher
            if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
            ) {
                PictureInPictureParams.Builder pictureInPictureParams = new PictureInPictureParams.Builder();
                // setup height and width of the PIP window
                Rational aspectRatio = new Rational(player.getVideoFormat().width, player.getVideoFormat().height);
                pictureInPictureParams.setAspectRatio(aspectRatio).build();
                getActivity().enterPictureInPictureMode(pictureInPictureParams.build());
                Log.v(TAG, "PIP break 2");
            } else {
                getActivity().enterPictureInPictureMode();
                Log.v(TAG, "PIP break 3");
            }
            isPictureInPicture = getActivity().isInPictureInPictureMode();
            if (subtitleUris.size() > 0) {
               // this.subtitleManager.setSubtitles(subtitleUris);
            }
            if (player != null) play();

            handler.postDelayed(pipCheckRunnable, 100);
            Log.v(TAG, "PIP break 4");
        } else {
            Log.v(TAG, "pictureInPictureMode: doesn't support PIP");
        }
    }

    /**
     * Checks if picture-in-picture mode is supported and enabled.
     * Called periodically to verify PIP permissions.
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void checkPIPPermission() {
        isPipModeEnabled = isPictureInPicture;
        if (!isPictureInPicture) {
            backPressed();
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
                if (player.getCurrentPosition() != 0) {
                    isFirstReady = false;
                    play();
                }
            } else {
                getActivity().finishAndRemoveTask();
            }
        }
    }

    /**
     * Lifecycle method called when the fragment is no longer visible to the user.
     * Releases resources but may keep the player active for background playback.
     */
    @Override
    public void onStop() {
        super.onStop();
        boolean isAppBackground = false;
        if (isBackgroundModeEnabled)
            isAppBackground = isApplicationSentToBackground(fragmentContext);
        if (isPictureInPicture) {
            controlsContainer.setVisibility(View.VISIBLE);
            playerExit();
            getActivity().finishAndRemoveTask();
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
    }

    /**
     * Lifecycle method called when the fragment is being paused.
     * Handles player state preservation and PIP mode transitions.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (isChromecastEnabled) castContext.removeCastStateListener(castStateListener);

        boolean isAppBackground = false;
        if (isBackgroundModeEnabled) {
            isAppBackground = isApplicationSentToBackground(fragmentContext);
        }

        if (!isPictureInPicture) {
            if (Util.SDK_INT < 24) {
                if (player != null) player.setPlayWhenReady(false);
                releasePlayer();
            } else {
                if (isAppBackground) {
                    if (player != null) {
                        if (player.isPlaying()) play();
                    }
                } else {
                    pause();
                }
            }
        } else {
            if (controlsContainer.getVisibility() == View.VISIBLE) {
                controlsContainer.setVisibility(View.INVISIBLE);
            }
            if ((isPictureInPicture || isAppBackground) && player != null) play();
        }
    }

    /**
     * Releases the player and associated resources.
     * Called during lifecycle events or when the player is no longer needed.
     */
    public void releasePlayer() {
        if (player != null) {
            playbackPosition = player.getCurrentPosition();
            currentMediaItemIndex = player.getCurrentWindowIndex();
            if (mediaSession != null) {
                mediaSession.release();
                mediaSession = null;
            }
            player.setRepeatMode(Player.REPEAT_MODE_OFF);
            player.removeListener(playerListener);
            player.release();
            player = null;
            showSystemUI();
            resetVariables();
            if (isChromecastEnabled && castPlayer != null) {
                castPlayer.release();
                castPlayer = null;
            }
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
        if (!isPictureInPicture) {
            hideSystemUi();
            if ((Util.SDK_INT < 24 || player == null)) {
                initializePlayer();
            }
        } else {
            isPictureInPicture = false;
            if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
            ) {
                playerView.setUseController(showControls);
            }
/*            if (subtitleUris.size() > 0) {
                this.subtitleManager.setSubtitles(subtitleUris);
            }*/
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
        // Enable audio libs
        DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory()
                .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS)
                .setTsExtractorTimestampSearchBytes(1500 * TsExtractor.TS_PACKET_SIZE);

        @SuppressLint("WrongConstant")
        RenderersFactory renderersFactory = new DefaultRenderersFactory(fragmentContext)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON);


        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter.Builder(fragmentContext).build();
        //  AdaptiveTrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory();

        DefaultTrackSelector trackSelector = new DefaultTrackSelector(fragmentContext);
// Configurer pour garder le renderer actif mais sans sélection par défaut
        DefaultTrackSelector.Parameters parameters = trackSelector.getParameters()
                .buildUpon()
                .setRendererDisabled(C.TRACK_TYPE_TEXT, false) // Renderer actif
                .clearOverridesOfType(C.TRACK_TYPE_TEXT) // Pas de piste sélectionnée
                .setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT | C.SELECTION_FLAG_FORCED) // Empêche toute sélection automatique
                .build();

        trackSelector.setParameters(parameters);

        this.subtitleManager.setTrackSelector(trackSelector);

        LoadControl loadControl = new DefaultLoadControl();

        playerListener = new PlayerListener();

        player =
                new ExoPlayer.Builder(fragmentContext, renderersFactory)
                        .setSeekBackIncrementMs(10000)
                        .setSeekForwardIncrementMs(10000)
                        .setTrackSelector(trackSelector)
                        .setLoadControl(loadControl)
                        .setBandwidthMeter(bandwidthMeter)
                        .setMediaSourceFactory(new DefaultMediaSourceFactory(fragmentContext, extractorsFactory))
                        .build();

        player.addListener(playerListener);

        this.subtitleManager.setPlayer(player);


        playerView.setPlayer(player);

        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder()
                .setUri(videoUri)
                .setMimeType(videoType);

        if (shouldLoopOnEnd) {
            player.setRepeatMode(Player.REPEAT_MODE_ONE);
        } else {
            player.setRepeatMode(Player.REPEAT_MODE_OFF);
        }

        if (!subtitleUris.isEmpty()) {

            List<MediaItem.SubtitleConfiguration> subtitleConfigurations = new ArrayList<>();
            for (int i = 0; i < subtitleUris.size(); i++) {
                Uri subtitleUri = subtitleUris.get(i);
                String subtitleName = null;
                String subtitleLang = null;
                
                // Retrieve subtitle name and language if available
                if (subtitles != null && i < subtitles.size()) {
                    SubtitleItem item = subtitles.get(i);
                    subtitleName = item.name;
                    subtitleLang = item.lang;
                }
                
                // Use the enhanced version of buildSubtitle with the language parameter
                MediaItem.SubtitleConfiguration subtitle = SubtitleUtils.buildSubtitle(
                    fragmentContext, 
                    subtitleUri, 
                    subtitleName,
                    subtitleLang,
                    true
                );
                
                subtitleConfigurations.add(subtitle);
            }
            mediaItemBuilder.setSubtitleConfigurations(subtitleConfigurations);
        }

        MediaItem mediaItem = mediaItemBuilder.build();

        player.setMediaItem(mediaItem, startAtSec > 0 ? startAtSec * 1000 : 0);

        player.prepare();

        playerView.showController();

        player.setPlayWhenReady(true);

        this.subtitleManager.updateCustomSubtitleButton();

        this.subtitleManager.setSubtitleStyle();


        Map<String, Object> info = new HashMap<String, Object>() {
            {
                put("fromPlayerId", playerId);
            }
        };

        NotificationCenter.defaultCenter().postNotification("initializePlayer", info);
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
            final long currentTime = player != null ?
                    (player.isCurrentMediaItemLive() ? 0 : player.getCurrentPosition() / 1000) : 0;

            Map<String, Object> info = new HashMap<String, Object>() {
                {
                    put("fromPlayerId", playerId);
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
                    isPlayerReady = true;
                    playerView.setUseController(showControls);
                    controlsContainer.setVisibility(View.INVISIBLE);
                    Log.v(TAG, "**** in ExoPlayer.STATE_READY isFirstReady " + isFirstReady);

                    subtitleManager.updateCustomSubtitleButton();

                    if (isFirstReady) {
                        isFirstReady = false;
                        NotificationCenter.defaultCenter().postNotification("playerItemReady", info);

                        play();
                        Log.v(TAG, "**** in ExoPlayer.STATE_READY isFirstReady player.isPlaying" + player.isPlaying());
                        player.seekTo(currentMediaItemIndex, playbackPosition);

                        selectTracks();

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
                        NotificationCenter.defaultCenter().postNotification("playerItemEnd", info);
                    }
                    break;
                default:
                    stateString = "UNKNOWN_STATE             -";
                    break;
            }
            Log.v(TAG, stateString + " currentTime: " + currentTime + " - isPlayerReady: " + isPlayerReady);
        }

        /**
         * Called when the player's play-when-ready state changes.
         * Handles play/pause state changes and sends appropriate notifications.
         *
         * @param playWhenReady Whether playback should proceed when ready
         * @param reason The reason for the change
         */
        @Override
        public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
            final long currentTime = player != null ?
                    (player.isCurrentMediaItemLive() ? 0 : player.getCurrentPosition() / 1000) : 0;

            Map<String, Object> info = new HashMap<String, Object>() {
                {
                    put("fromPlayerId", playerId);
                    put("currentTime", String.valueOf(currentTime));
                }
            };

            if (playWhenReady) {
                Log.v(TAG, "**** in onPlayWhenReadyChanged going to notify playerItemPlay ");
                NotificationCenter.defaultCenter().postNotification("playerItemPlay", info);
                resizeButton.setVisibility(View.VISIBLE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isPipEnabled) {
                    pipButton.setVisibility(View.VISIBLE);
                }
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

            Log.v(TAG, "**** icurrentSubtitleTrack: " + currentSubtitleTrack);

            // Create event data
            Map<String, Object> trackInfo = new HashMap<String, Object>();
            trackInfo.put("fromPlayerId", playerId);

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

            if (currentSubtitleTrack != null) {
                Format subtitleFormat = currentSubtitleTrack.getFormat(0);
                Map<String, Object> subtitleInfo = new HashMap<String, Object>();
                subtitleInfo.put("id", subtitleFormat.id);
                subtitleInfo.put("language", subtitleFormat.language);
                subtitleInfo.put("label", subtitleFormat.label);
                subtitleInfo.put("codecs", subtitleFormat.codecs);
                subtitleInfo.put("containerMimeType", subtitleFormat.containerMimeType);
                subtitleInfo.put("sampleMimeType", subtitleFormat.sampleMimeType);
                trackInfo.put("subtitleTrack", subtitleInfo);
            }

            NotificationCenter.defaultCenter().postNotification("playerTracksChanged", trackInfo);

            subtitleManager.updateCustomSubtitleButton();
        }
    }


    /**
     * Builds an HTTP media source for streaming content.
     * Configures HTTP headers and data source factory for network requests.
     *
     * @return The configured media source
     */
    private MediaSource buildHttpMediaSource() {
        MediaSource mediaSource = null;

        DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent("jeep-media3-plugin")
                .setConnectTimeoutMs(DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS)
                .setReadTimeoutMs(1800000)
                .setAllowCrossProtocolRedirects(true);

        // If requestHeaders is not null and has data we pass them to the HttpDataSourceFactory
        if (requestHeaders != null && requestHeaders.length() > 0) {
            // We map the requestHeaders(JSObject) to a Map<String, String>
            Map<String, String> headersMap = new HashMap<String, String>();
            for (int i = 0; i < requestHeaders.names().length(); i++) {
                try {
                    headersMap.put(requestHeaders.names().getString(i), requestHeaders.get(requestHeaders.names().getString(i)).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            httpDataSourceFactory.setDefaultRequestProperties(headersMap);
        }

        DataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(fragmentContext, httpDataSourceFactory);

        if (
                videoType.equals("mp4") ||
                        videoType.equals("webm") ||
                        videoType.equals("ogv") ||
                        videoType.equals("3gp") ||
                        videoType.equals("flv") ||
                        videoType.equals("")
        ) {
            mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(videoUri));
        } else if (videoType.equals("dash") || videoType.equals("mpd")) {
            /* adaptive streaming Dash stream */
            mediaSource = new DashMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(videoUri));
        } else if (videoType.equals("m3u8")) {
            mediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(videoUri));
        } else if (videoType.equals("ism")) {
            /* adaptive streaming SmoothStreaming stream */
            //  mediaSource = new SmoothStreamingMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(videoUri));
        }

        // Get the subtitles if any
        if (subtitleUris.size() > 0) {
            //   mediaSource = this.subtitleManager.getSubTitleFromUri(mediaSource, subtitleUri, dataSourceFactory);
        }
        return mediaSource;
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


    /**
     * Enables or disables subtitle display.
     *
     * @param enabled Whether subtitles should be displayed
     */
    public void enableSubtitles(boolean enabled) {
        this.subtitleManager.enableSubtitles(enabled);
    }


    /**
     * Fast-forwards the video by a specified amount.
     *
     * @param position The current position in milliseconds
     * @param times Multiplier for the seek step
     */
    private void fastForward(long position, int times) {
        if (position < totalDuration - seekStep) {
            if (player.isPlaying()) {
                player.setPlayWhenReady(false);
            }
            player.seekTo(position + (long) times * seekStep);
            play();
        }
    }

    /**
     * Rewinds the video by a specified amount.
     *
     * @param position The current position in milliseconds
     * @param times Multiplier for the seek step
     */
    private void rewind(long position, int times) {
        if (position > seekStep) {
            if (player.isPlaying()) {
                player.setPlayWhenReady(false);
            }
            player.seekTo(position - (long) times * seekStep);
            play();
        }
    }

    /**
     * Toggles between play and pause states.
     */
    private void play_pause() {
        player.setPlayWhenReady(!player.isPlaying());
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
          in the device and isChromecastEnabled at the same time. This is to avoid that behaviour.*/
        if (!isCasting) player.setPlayWhenReady(true);
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
        if (isPictureInPicture) {
            playerView.setUseController(false);
            controlsContainer.setVisibility(View.INVISIBLE);
        }
        long seekPosition = player.getCurrentPosition() == UNKNOWN_TIME
                ? 0
                : Math.min(Math.max(0, timeSecond * 1000), player.getDuration());
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

        TypedArray a = castContext.obtainStyledAttributes(
                null,
                androidx.mediarouter.R.styleable.MediaRouteButton,
                androidx.mediarouter.R.attr.mediaRouteButtonStyle,
                0
        );
        Drawable drawable = a.getDrawable(androidx.mediarouter.R.styleable.MediaRouteButton_externalRouteEnabledDrawable);
        a.recycle();
        DrawableCompat.setTint(drawable, getContext().getResources().getColor(R.color.white));
        drawable.setState(button.getDrawableState());
        button.setRemoteIndicatorDrawable(drawable);
    }

    /**
     * Determines the video type (MIME type) based on the URI.
     * Supports various streaming formats like HLS, DASH, and progressive download.
     *
     * @param uri The URI of the video
     * @return The identified MIME type
     */
    private String getVideoType(Uri uri) {
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

    /**
     * Resets all player state variables to their default values.
     * Called when preparing for a new playback session.
     */
    private void resetVariables() {
        videoType = null;
        playerView = null;
        isFirstReady = true;
        currentMediaItemIndex = 0;
        playbackPosition = 0;
        videoUri = null;
        subtitleUris.clear();
        isMuted = false;
        currentVolume = (float) 0.5;
        lastKnownPosition = 0;
    }

    /**
     * Checks if the application has been sent to the background.
     * Used to determine appropriate player behavior in background mode.
     *
     * @param context The Android context
     * @return True if app is in background, false otherwise
     */
    public boolean isApplicationSentToBackground(final Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> procInfos = am.getRunningAppProcesses();
        if (procInfos != null) {
            for (ActivityManager.RunningAppProcessInfo appProcess : procInfos) {
                if (appProcess.pid == pid) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Initializes the Chromecast service.
     * Sets up the CastContext and prepares for casting sessions.
     */
    private void initializeCastService() {
        Executor executor = Executors.newSingleThreadExecutor();
        Task<CastContext> task = CastContext.getSharedInstance(fragmentContext, executor);

        task.addOnCompleteListener(
                new OnCompleteListener<CastContext>() {
                    @Override
                    public void onComplete(Task<CastContext> task) {
                        if (task.isSuccessful()) {
                            castContext = task.getResult();
                            castPlayer = new CastPlayer(castContext);
                            mediaRouter = MediaRouter.getInstance(fragmentContext);
                            mSelector =
                                    new MediaRouteSelector.Builder()
                                            .addControlCategories(
                                                    Arrays.asList(MediaControlIntent.CATEGORY_LIVE_AUDIO, MediaControlIntent.CATEGORY_LIVE_VIDEO)
                                            )
                                            .build();

                            mediaRouteButtonColorWhite(mediaRouteButton);
                            if (
                                    castContext != null && castContext.getCastState() != CastState.NO_DEVICES_AVAILABLE
                            ) mediaRouteButton.setVisibility(View.VISIBLE);

                            castStateListener =
                                    state -> {
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
                                movieMetadata =
                                        new MediaMetadata.Builder()
                                                .setTitle(videoTitle)
                                                .setSubtitle(videoSubtitle)
                                                .setMediaType(MediaMetadata.MEDIA_TYPE_MOVIE)
                                                .setArtworkUri(Uri.parse(posterUrl))
                                                .build();
                                new setCastImage().execute();
                            } else {
                                movieMetadata = new MediaMetadata.Builder().setTitle(videoTitle).setSubtitle(videoSubtitle).build();
                            }
                            mediaItem =
                                    new MediaItem.Builder()
                                            .setUri(videoPath)
                                            .setMimeType(MimeTypes.VIDEO_UNKNOWN)
                                            .setMediaMetadata(movieMetadata)
                                            .build();

                            castPlayer.setSessionAvailabilityListener(
                                    new SessionAvailabilityListener() {
                                        @Override
                                        public void onCastSessionAvailable() {
                                            isCasting = true;
                                            final Long videoPosition = player.getCurrentPosition();
                                            if (isPipEnabled) {
                                                pipButton.setVisibility(View.GONE);
                                            }
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
                                            if (isPipEnabled) {
                                                pipButton.setVisibility(View.VISIBLE);
                                            }
                                            resizeButton.setVisibility(View.VISIBLE);
                                            castImage.setVisibility(View.GONE);
                                            playerView.setPlayer(player);
                                            player.setPlayWhenReady(true);
                                            player.seekTo(videoPosition);
                                            playerView.setControllerShowTimeoutMs(3000);
                                            playerView.setControllerHideOnTouch(true);
                                        }
                                    }
                            );

                            castPlayer.addListener(
                                    new Player.Listener() {
                                        @Override
                                        public void onPlayerStateChanged(boolean playWhenReady, int state) {
                                            Map<String, Object> info = new HashMap<String, Object>() {
                                                {
                                                    put("fromPlayerId", playerId);
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
                                    }
                            );

                            castContext.addCastStateListener(castStateListener);
                            mediaRouter.addCallback(mSelector, mediaRouterCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
                        } else {
                            Exception e = task.getException();
                            e.printStackTrace();
                        }
                    }
                }
        );
    }

    /**
     * Empty callback implementation for MediaRouter.
     * Used as a placeholder for the MediaRouter callback mechanism.
     */
    private final class EmptyCallback extends MediaRouter.Callback {
        // Empty implementation
    }

    /**
     * Selects audio and subtitle tracks based on user preferences.
     * Applies track selection parameters to the player based on the subtitleTrackId, 
     * subtitleLocale, audioTrackId, and audioLocale properties.
     */
    private void selectTracks() {
        if (player != null && trackSelector != null) {
            Tracks tracks = player.getCurrentTracks();

            boolean audioSelected = false;
            boolean subtitleSelected = false;

            // Select audio track
            if (audioTrackId != null || audioLocale != null) {
                Format selectedFormat = null;

                // First try to find by ID and check locale if specified
                if (audioTrackId != null) {
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

                if (selectedFormat != null && audioLocale != null && !selectedFormat.language.equals(audioLocale)) {
                    selectedFormat = null;
                }

                // If not found and locale specified, try by locale only
                if (selectedFormat == null && audioLocale != null) {
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
                    ((DefaultTrackSelector) trackSelector).setParameters(
                            ((DefaultTrackSelector) trackSelector).buildUponParameters().setPreferredAudioLanguage(selectedFormat.language)
                    );
                    audioSelected = true;
                    enableSubtitles(false);
                }
            }

            // Select subtitle track
            if (subtitleTrackId != null || subtitleLocale != null) {
                Format selectedFormat = null;

                // First try to find by ID and check locale if specified
                if (subtitleTrackId != null) {
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

                if (selectedFormat != null && subtitleLocale != null && !selectedFormat.language.equals(subtitleLocale)) {
                    selectedFormat = null;
                }

                // If not found and locale specified, try by locale only
                if (selectedFormat == null && subtitleLocale != null) {
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
                    ((DefaultTrackSelector) trackSelector).setParameters(
                            ((DefaultTrackSelector) trackSelector).buildUponParameters().setPreferredTextLanguage(selectedFormat.language)
                    );
                    subtitleSelected = true;
                }
            }

            if (!audioSelected && !subtitleSelected && preferredLocale != null) {
                Boolean trackFound = false;
                // First try to find the audio with the same language
                for (Tracks.Group trackGroup : tracks.getGroups()) {
                    if (trackGroup.getType() == C.TRACK_TYPE_AUDIO) {
                        Format format = trackGroup.getMediaTrackGroup().getFormat(0);
                        if (format.language != null && format.language.equals(preferredLocale)) {
                            ((DefaultTrackSelector) trackSelector).setParameters(
                                    ((DefaultTrackSelector) trackSelector).buildUponParameters()
                                            .setPreferredAudioLanguage(preferredLocale)
                            );
                            trackFound = true;
                            break;
                        }
                    }
                }

                if (!trackFound) {
                    // Then try to find the subtitle with the same language
                    for (Tracks.Group trackGroup : tracks.getGroups()) {
                        if (trackGroup.getType() == C.TRACK_TYPE_TEXT) {
                            Format format = trackGroup.getMediaTrackGroup().getFormat(0);
                            if (format.language != null && format.language.equals(preferredLocale)) {
                                ((DefaultTrackSelector) trackSelector).setParameters(
                                        ((DefaultTrackSelector) trackSelector).buildUponParameters()
                                                .setPreferredTextLanguage(preferredLocale)
                                );
                            }
                        }
                    }
                } else {
                    // Disable subtitles
                    enableSubtitles(false);
                }
            }
        }
    }


}
