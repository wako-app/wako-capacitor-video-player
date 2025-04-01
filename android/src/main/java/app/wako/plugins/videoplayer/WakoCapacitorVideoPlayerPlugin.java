package app.wako.plugins.videoplayer;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;

import app.wako.plugins.videoplayer.Components.SubtitleItem;
import app.wako.plugins.videoplayer.Notifications.MyRunnable;
import app.wako.plugins.videoplayer.Notifications.NotificationCenter;
import app.wako.plugins.videoplayer.Utilities.FragmentUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

@CapacitorPlugin(
        name = "WakoCapacitorVideoPlayer",
        permissions = {
                @Permission(alias = "mediaVideo", strings = {Manifest.permission.READ_MEDIA_VIDEO}),
                @Permission(alias = "publicStorage", strings = {Manifest.permission.READ_EXTERNAL_STORAGE})
        }
)
public class WakoCapacitorVideoPlayerPlugin extends Plugin {

    private WakoCapacitorVideoPlayer implementation;
    private static final String TAG = "WakoCapacitorVideoPlayer";
    private final int frameLayoutViewId = 256;
    private final int pickerLayoutViewId = 257;

    private Context context;
    private String videoUrl;
    private JSONArray subtitles;
    private Boolean isTV;
    private String displayMode = "all";
    private FullscreenExoPlayerFragment fsFragment;
    private FragmentUtils fragmentUtils;
    private PluginCall call;
    private final Float[] rateList = {0.25f, 0.5f, 0.75f, 1f, 2f, 4f};
    private Float videoRate = 1f;
    private String title;
    private String smallTitle;
    private Boolean chromecast = true;
    private String artwork;
    private String url;
    private String preferredLocale = "";
    private JSObject subTitleOptions;
    private String subtitleTrackId;
    private String subtitleLocale;
    private String audioTrackId;
    private String audioLocale;
    private long startAtSec;
    private final JSObject ret = new JSObject();

    @Override
    public void load() {
        // Get context
        this.context = getContext();
        implementation = new WakoCapacitorVideoPlayer(this.context);
        this.fragmentUtils = new FragmentUtils(getBridge());
    }


    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", implementation.echo(value));
        call.resolve(ret);
    }

    @PluginMethod
    public void initPlayer(PluginCall call) {
        this.call = call;
        ret.put("method", "initPlayer");
        ret.put("result", false);
        // Check if running on a TV Device
        isTV = isTvDevice(context);
        Log.d(TAG, "**** isTvDevice " + isTV + " ****");

        videoRate = 1f;
        if (call.getData().has("rate")) {
            Float mRate = call.getFloat("rate");
            if (isInRate(rateList, mRate)) {
                videoRate = mRate;
            }
        }


        String _displayMode = "all";
        if (call.getData().has("displayMode")) {
            _displayMode = call.getString("displayMode");
        }
        displayMode = _displayMode;

        url = call.getString("url");
        if (url == null) {
            ret.put("message", "Must provide an url");
            call.resolve(ret);
            return;
        }

        // Reset subtitles for each new video
        subtitles = null;

        if (call.getData().has("subtitles")) {
            try {
                subtitles = call.getArray("subtitles");
            } catch (Exception e) {
                // Handle case where subtitles is not an array (for backward compatibility)
                String oldSubtitleUrl = call.getString("subtitles");
                if (oldSubtitleUrl != null && !oldSubtitleUrl.isEmpty()) {
                    subtitles = new JSONArray();
                    JSObject subtitleObj = new JSObject();
                    subtitleObj.put("url", oldSubtitleUrl);
                    subtitles.put(subtitleObj);
                }
            }
        }
        if (call.getData().has("preferredLocale")) {
            preferredLocale = call.getString("preferredLocale");
        }
        subTitleOptions = new JSObject();
        if (call.getData().has("subtitleOptions")) {
            subTitleOptions = call.getObject("subtitleOptions");
        }

        String _title = "";
        if (call.getData().has("title")) {
            _title = call.getString("title");
        }
        title = _title;
        String _smallTitle = "";
        if (call.getData().has("smallTitle")) {
            _smallTitle = call.getString("smallTitle");
        }
        smallTitle = _smallTitle;
        Boolean _chromecast = true;
        if (call.getData().has("chromecast")) {
            _chromecast = call.getBoolean("chromecast");
        }
        chromecast = _chromecast;
        String _artwork = "";
        if (call.getData().has("artwork")) {
            _artwork = call.getString("artwork");
        }
        artwork = _artwork;


        subtitleTrackId = "";
        if (call.getData().has("subtitleTrackId")) {
            subtitleTrackId = call.getString("subtitleTrackId");
        }

        subtitleLocale = "";
        if (call.getData().has("subtitleLocale")) {
            subtitleLocale = call.getString("subtitleLocale");
        }

        audioTrackId = "";
        if (call.getData().has("audioTrackId")) {
            audioTrackId = call.getString("audioTrackId");
        }

        audioLocale = "";
        if (call.getData().has("audioLocale")) {
            audioLocale = call.getString("audioLocale");
        }

        startAtSec = 0;
        if (call.getData().has("startAtSec")) {
            startAtSec = call.getInt("startAtSec", 0);
        }


        AddObserversToNotificationCenter();
        Log.v(TAG, "display url: " + url);
        Log.v(TAG, "display subtitles: " + subtitles);
        Log.v(TAG, "display preferredLocale: " + preferredLocale);
        Log.v(TAG, "title: " + title);
        Log.v(TAG, "smallTitle: " + smallTitle);
        Log.v(TAG, "chromecast: " + chromecast);
        Log.v(TAG, "artwork: " + artwork);

        _initPlayer(call);

    }

    @PluginMethod
    public void isPlaying(final PluginCall call) {
        this.call = call;
        JSObject ret = new JSObject();
        ret.put("method", "isPlaying");
        String playerId = call.getString("playerId");
        if (playerId == null) {
            ret.put("result", false);
            ret.put("message", "Must provide a PlayerId");
            call.resolve(ret);
            return;
        }
        bridge
                .getActivity()
                .runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                JSObject ret = new JSObject();
                                ret.put("method", "isPlaying");
                                if (fsFragment != null) {
                                    boolean playing = fsFragment.isPlaying();
                                    ret.put("result", true);
                                    ret.put("value", playing);
                                    call.resolve(ret);
                                } else {
                                    ret.put("result", false);
                                    ret.put("message", "Fullscreen fragment is not defined");
                                    call.resolve(ret);
                                }
                            }
                        }
                );

    }

    @PluginMethod
    public void play(final PluginCall call) {
        this.call = call;
        JSObject ret = new JSObject();
        ret.put("method", "play");

        try {
            bridge
                    .getActivity()
                    .runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        JSObject ret = new JSObject();
                                        ret.put("method", "play");
                                        
                                        // Check if fragment is still valid
                                        if (fsFragment != null) {
                                            // Synchronize to avoid any race conditions
                                            synchronized (fsFragment) {
                                                fsFragment.play();
                                                boolean playing = fsFragment.isPlaying();
                                            }
                                            ret.put("result", true);
                                            ret.put("value", true);
                                            call.resolve(ret);
                                        } else {
                                            ret.put("result", false);
                                            ret.put("message", "Fullscreen fragment is not defined");
                                            call.resolve(ret);
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error during play method execution", e);
                                        JSObject errorRet = new JSObject();
                                        errorRet.put("result", false);
                                        errorRet.put("message", "Error during play: " + e.getMessage());
                                        call.resolve(errorRet);
                                    }
                                }
                            }
                    );
        } catch (Exception e) {
            Log.e(TAG, "Error dispatching play to UI thread", e);
            ret.put("result", false);
            ret.put("message", "Error dispatching play: " + e.getMessage());
            call.resolve(ret);
        }
    }

    @PluginMethod
    public void pause(final PluginCall call) {
        this.call = call;
        JSObject ret = new JSObject();
        ret.put("method", "pause");

        try {
            bridge
                    .getActivity()
                    .runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        JSObject ret = new JSObject();
                                        ret.put("method", "pause");
                                        
                                        // Check if fragment is still valid
                                        if (fsFragment != null) {
                                            // Synchronize to avoid any race conditions
                                            synchronized (fsFragment) {
                                                fsFragment.pause();
                                            }
                                            ret.put("result", true);
                                            ret.put("value", true);
                                            call.resolve(ret);
                                        } else {
                                            ret.put("result", false);
                                            ret.put("message", "Fullscreen fragment is not defined");
                                            call.resolve(ret);
                                        }
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error during pause method execution", e);
                                        JSObject errorRet = new JSObject();
                                        errorRet.put("result", false);
                                        errorRet.put("message", "Error during pause: " + e.getMessage());
                                        call.resolve(errorRet);
                                    }
                                }
                            }
                    );
        } catch (Exception e) {
            Log.e(TAG, "Error dispatching pause to UI thread", e);
            ret.put("result", false);
            ret.put("message", "Error dispatching pause: " + e.getMessage());
            call.resolve(ret);
        }
    }

    @PluginMethod
    public void getDuration(final PluginCall call) {
        this.call = call;
        JSObject ret = new JSObject();
        ret.put("method", "getDuration");
        String playerId = call.getString("playerId");

        bridge
                .getActivity()
                .runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                JSObject ret = new JSObject();
                                ret.put("method", "getDuration");
                                if (fsFragment != null) {
                                    int duration = fsFragment.getDuration();
                                    ret.put("result", true);
                                    ret.put("value", duration);
                                    call.resolve(ret);
                                } else {
                                    ret.put("result", false);
                                    ret.put("message", "Fullscreen fragment is not defined");
                                    call.resolve(ret);
                                }
                            }
                        }
                );

    }

    @PluginMethod
    public void getCurrentTime(final PluginCall call) {
        this.call = call;
        JSObject ret = new JSObject();
        ret.put("method", "getCurrentTime");

        bridge
                .getActivity()
                .runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                JSObject ret = new JSObject();
                                ret.put("method", "getCurrentTime");
                                if (fsFragment != null) {
                                    int curTime = fsFragment.getCurrentTime();
                                    ret.put("result", true);
                                    ret.put("value", curTime);
                                    call.resolve(ret);
                                } else {
                                    ret.put("result", false);
                                    ret.put("message", "Fullscreen fragment is not defined");
                                    call.resolve(ret);
                                }
                            }
                        }
                );

    }

    @PluginMethod
    public void setCurrentTime(final PluginCall call) {
        this.call = call;
        JSObject ret = new JSObject();
        ret.put("method", "setCurrentTime");

        Double value = call.getDouble("seektime");
        if (value == null) {
            ret.put("result", false);
            ret.put("message", "Must provide a time in second");
            call.resolve(ret);
            return;
        }
        final int cTime = (int) Math.round(value);
        bridge
                .getActivity()
                .runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                JSObject ret = new JSObject();
                                ret.put("method", "setCurrentTime");
                                if (fsFragment != null) {
                                    fsFragment.setCurrentTime(cTime);
                                    ret.put("result", true);
                                    ret.put("value", cTime);
                                    call.resolve(ret);
                                } else {
                                    ret.put("result", false);
                                    ret.put("message", "Fullscreen fragment is not defined");
                                    call.resolve(ret);
                                }
                            }
                        }
                );

    }

    @PluginMethod
    public void getVolume(final PluginCall call) {
        this.call = call;
        JSObject ret = new JSObject();
        ret.put("method", "getVolume");

        bridge
                .getActivity()
                .runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                JSObject ret = new JSObject();
                                ret.put("method", "getVolume");
                                if (fsFragment != null) {
                                    Float volume = fsFragment.getVolume();
                                    ret.put("result", true);
                                    ret.put("value", volume);
                                    call.resolve(ret);
                                } else {
                                    ret.put("result", false);
                                    ret.put("message", "Fullscreen fragment is not defined");
                                    call.resolve(ret);
                                }
                            }
                        }
                );

    }

    @PluginMethod
    public void setVolume(final PluginCall call) {
        this.call = call;
        JSObject ret = new JSObject();
        ret.put("method", "setVolume");

        Float volume = call.getFloat("volume");
        if (volume == null) {
            ret.put("result", false);
            ret.put("method", "setVolume");
            ret.put("message", "Must provide a volume value");
            call.resolve(ret);
            return;
        }

        bridge
                .getActivity()
                .runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                JSObject ret = new JSObject();
                                ret.put("method", "setVolume");
                                if (fsFragment != null) {
                                    fsFragment.setVolume(volume);
                                    ret.put("result", true);
                                    ret.put("value", volume);
                                    call.resolve(ret);
                                } else {
                                    ret.put("result", false);
                                    ret.put("message", "Fullscreen fragment is not defined");
                                    call.resolve(ret);
                                }
                            }
                        }
                );

    }

    @PluginMethod
    public void getMuted(final PluginCall call) {
        this.call = call;
        JSObject ret = new JSObject();
        ret.put("method", "getMuted");

        bridge
                .getActivity()
                .runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                JSObject ret = new JSObject();
                                ret.put("method", "getMuted");
                                if (fsFragment != null) {
                                    boolean value = fsFragment.getMuted();
                                    ret.put("result", true);
                                    ret.put("value", value);
                                    call.resolve(ret);
                                } else {
                                    ret.put("result", false);
                                    ret.put("message", "Fullscreen fragment is not defined");
                                    call.resolve(ret);
                                }
                            }
                        }
                );

    }

    @PluginMethod
    public void setMuted(final PluginCall call) {
        this.call = call;
        JSObject ret = new JSObject();
        ret.put("method", "setMuted");

        Boolean value = call.getBoolean("muted");
        if (value == null) {
            ret.put("result", true);
            ret.put("message", "Must provide a boolean true/false");
            call.resolve(ret);
            return;
        }
        final boolean bValue = value;
        bridge
                .getActivity()
                .runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                JSObject ret = new JSObject();
                                ret.put("method", "setMuted");
                                if (fsFragment != null) {
                                    fsFragment.setMuted(bValue);
                                    ret.put("result", true);
                                    ret.put("value", bValue);
                                    call.resolve(ret);
                                } else {
                                    ret.put("result", false);
                                    ret.put("message", "Fullscreen fragment is not defined");
                                    call.resolve(ret);
                                }
                            }
                        }
                );

    }

    @PluginMethod
    public void getRate(final PluginCall call) {
        this.call = call;
        JSObject ret = new JSObject();
        ret.put("method", "getRate");
        String playerId = call.getString("playerId");

        bridge
                .getActivity()
                .runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                JSObject ret = new JSObject();
                                ret.put("method", "getRate");
                                if (fsFragment != null) {
                                    Float rate = fsFragment.getRate();
                                    ret.put("result", true);
                                    ret.put("value", rate);
                                    call.resolve(ret);
                                } else {
                                    ret.put("result", false);
                                    ret.put("message", "Fullscreen fragment is not defined");
                                    call.resolve(ret);
                                }
                            }
                        }
                );

    }

    @PluginMethod
    public void setRate(final PluginCall call) {
        this.call = call;
        JSObject ret = new JSObject();
        ret.put("method", "setRate");

        Float rate = call.getFloat("rate");
        if (rate == null) {
            ret.put("result", false);
            ret.put("method", "setRate");
            ret.put("message", "Must provide a volume value");
            call.resolve(ret);
            return;
        }
        if (isInRate(rateList, rate)) {
            videoRate = rate;
        } else {
            videoRate = 1f;
        }
        bridge
                .getActivity()
                .runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                JSObject ret = new JSObject();
                                ret.put("method", "setRate");
                                if (fsFragment != null) {
                                    fsFragment.setRate(videoRate);
                                    ret.put("result", true);
                                    ret.put("value", videoRate);
                                    call.resolve(ret);
                                } else {
                                    ret.put("result", false);
                                    ret.put("message", "Fullscreen fragment is not defined");
                                    call.resolve(ret);
                                }
                            }
                        }
                );

    }

    @PluginMethod
    public void stopAllPlayers(PluginCall call) {
        this.call = call;
        bridge
                .getActivity()
                .runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                JSObject ret = new JSObject();
                                ret.put("method", "stopAllPlayers");
                                if (fsFragment != null) {
                                    fsFragment.pause();
                                    ret.put("result", true);
                                    ret.put("value", true);
                                    call.resolve(ret);
                                } else {
                                    ret.put("result", false);
                                    ret.put("message", "Fullscreen fragment is not defined");
                                    call.resolve(ret);
                                }
                            }
                        }
                );
    }

    @PluginMethod
    public void isControllerIsFullyVisible(PluginCall call) {
        this.call = call;
        bridge
                .getActivity()
                .runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                JSObject ret = new JSObject();
                                ret.put("method", "isControllerIsFullyVisible");
                                if (fsFragment != null) {
                                    ret.put("result", true);
                                    ret.put("value", fsFragment.isControllerIsFullyVisible());
                                    call.resolve(ret);
                                } else {
                                    ret.put("result", false);
                                    ret.put("message", "Fullscreen fragment is not defined");
                                    call.resolve(ret);
                                }
                            }
                        }
                );
    }

    @PluginMethod
    public void showController(PluginCall call) {
        this.call = call;
        bridge
                .getActivity()
                .runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                JSObject ret = new JSObject();
                                ret.put("method", "showController");
                                if (fsFragment != null) {
                                    fsFragment.showController();
                                    ret.put("result", true);
                                    ret.put("value", true);
                                    call.resolve(ret);
                                } else {
                                    ret.put("result", false);
                                    ret.put("message", "Fullscreen fragment is not defined");
                                    call.resolve(ret);
                                }
                            }
                        }
                );
    }


    @PluginMethod
    public void exitPlayer(PluginCall call) {
        this.call = call;
        bridge
                .getActivity()
                .runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                JSObject ret = new JSObject();
                                ret.put("method", "exitPlayer");
                                if (fsFragment != null) {
                                    fsFragment.playerExit();
                                    ret.put("result", true);
                                    ret.put("value", true);
                                    call.resolve(ret);
                                } else {
                                    ret.put("result", false);
                                    ret.put("message", "Fullscreen fragment is not defined");
                                    call.resolve(ret);
                                }
                            }
                        }
                );
    }

    private boolean isTvDevice(Context context) {
        try {
            boolean isTelevision = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);

            if (isTelevision) {
                Log.d(TAG, "Running on a TV Device");
            } else {
                Log.d(TAG, "Running on a non-TV Device");
            }

            return isTelevision;
        } catch (Exception e) {
            Log.e(TAG, "Error checking if device is TV", e);
            return false;
        }
    }

    private void _initPlayer(PluginCall call) {
        // get the videoPath
        videoUrl = url;

        if (videoUrl == null) {
            Map<String, Object> info = new HashMap<String, Object>() {
                {
                    put("dismiss", "1");
                    put("currentTime", "0");
                }
            };
            NotificationCenter.defaultCenter().postNotification("playerFullscreenDismiss", info);
            ret.put("message", "initPlayer command failed: Video file not found");
            call.resolve(ret);
            return;
        }

        // get the subTitlePath if any
        ArrayList<SubtitleItem> subtitleItems = new ArrayList<>();
        if (subtitles != null && subtitles.length() > 0) {
            for (int i = 0; i < subtitles.length(); i++) {
                try {
                    // Create a new JSObject from the JSONObject data
                    JSONObject jsonObj = subtitles.getJSONObject(i);
                    JSObject subtitleObj = new JSObject();

                    // Copy the necessary properties
                    if (jsonObj.has("url")) {
                        subtitleObj.put("url", jsonObj.getString("url"));
                    }
                    if (jsonObj.has("name")) {
                        subtitleObj.put("name", jsonObj.getString("name"));
                    }
                    if (jsonObj.has("lang")) {
                        subtitleObj.put("lang", jsonObj.getString("lang"));
                    }

                    String url = subtitleObj.getString("url");
                    if (url != null && !url.isEmpty()) {
                        String name = subtitleObj.has("name") ? subtitleObj.getString("name") : null;
                        String lang = subtitleObj.has("lang") ? subtitleObj.getString("lang") : null;
                        subtitleItems.add(new SubtitleItem(url, name, lang));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing subtitle item: " + e.getMessage());
                }
            }
        }
        Log.v(TAG, "*** calculated videoPath: " + videoUrl);
        Log.v(TAG, "*** parsed " + subtitleItems.size() + " subtitles");

        fsFragment =
                implementation.createFullScreenFragment(
                        videoUrl,
                        displayMode,
                        subtitleItems,
                        preferredLocale,
                        subTitleOptions,
                        title,
                        smallTitle,
                        chromecast,
                        artwork,
                        isTV,
                        subtitleTrackId,
                        subtitleLocale,
                        audioTrackId,
                        audioLocale,
                        startAtSec
                );


        bridge
                .getActivity()
                .runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                JSObject ret = new JSObject();
                                ret.put("method", "initPlayer");
                                FrameLayout frameLayoutView = getBridge().getActivity().findViewById(frameLayoutViewId);
                                if (frameLayoutView != null) {
                                    ret.put("result", false);
                                    ret.put("message", "FrameLayout for ExoPlayer already exists");
                                } else {
                                    // Initialize a new FrameLayout as container for fragment
                                    frameLayoutView = new FrameLayout(getActivity().getApplicationContext());
                                    frameLayoutView.setId(frameLayoutViewId);
                                    FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                                            FrameLayout.LayoutParams.MATCH_PARENT,
                                            FrameLayout.LayoutParams.MATCH_PARENT
                                    );
                                    // Apply the Layout Parameters to frameLayout
                                    frameLayoutView.setLayoutParams(lp);

                                    ((ViewGroup) getBridge().getWebView().getParent()).addView(frameLayoutView);
                                    fragmentUtils.loadFragment(fsFragment, frameLayoutViewId);
                                    ret.put("result", true);
                                }
                                call.resolve(ret);
                            }
                        }
                );
    }



    private Boolean isInRate(Float[] arr, Float rate) {
        Boolean ret = false;
        for (Float el : arr) {
            if (el.equals(rate)) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    private void AddObserversToNotificationCenter() {
        NotificationCenter
                .defaultCenter()
                .addMethodForNotification(
                        "playerItemPlay",
                        new MyRunnable() {
                            @Override
                            public void run() {
                                JSObject data = new JSObject();
                                data.put("fromPlayerId", this.getInfo().get("fromPlayerId"));
                                data.put("currentTime", this.getInfo().get("currentTime"));
                                notifyListeners("playerPlay", data);
                            }
                        }
                );
        NotificationCenter
                .defaultCenter()
                .addMethodForNotification(
                        "playerItemPause",
                        new MyRunnable() {
                            @Override
                            public void run() {
                                JSObject data = new JSObject();
                                data.put("currentTime", this.getInfo().get("currentTime"));
                                notifyListeners("playerPause", data);
                            }
                        }
                );
        NotificationCenter
                .defaultCenter()
                .addMethodForNotification(
                        "playerStateReady",
                        new MyRunnable() {
                            @Override
                            public void run() {
                                JSObject data = new JSObject();
                                data.put("currentTime", this.getInfo().get("currentTime"));
                                notifyListeners("playerReady", data);
                            }
                        }
                );
        NotificationCenter
                .defaultCenter()
                .addMethodForNotification(
                        "playerStateEnd",
                        new MyRunnable() {
                            @Override
                            public void run() {
                                final JSObject data = new JSObject();
                                data.put("currentTime", this.getInfo().get("currentTime"));
                                bridge
                                        .getActivity()
                                        .runOnUiThread(
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        FrameLayout frameLayoutView = getBridge().getActivity().findViewById(frameLayoutViewId);

                                                        if (frameLayoutView != null) {
                                                            ((ViewGroup) getBridge().getWebView().getParent()).removeView(frameLayoutView);
                                                            fragmentUtils.removeFragment(fsFragment);
                                                        }
                                                        fsFragment = null;
                                                        NotificationCenter.defaultCenter().removeAllNotifications();
                                                        notifyListeners("playerEnded", data);
                                                    }
                                                }
                                        );
                            }
                        }
                );
        NotificationCenter
                .defaultCenter()
                .addMethodForNotification(
                        "playerFullscreenDismiss",
                        new MyRunnable() {
                            @Override
                            public void run() {
                                boolean ret = false;
                                final JSObject data = new JSObject();
                                if (Integer.valueOf((String) this.getInfo().get("dismiss")) == 1)
                                    ret = true;
                                data.put("dismiss", ret);
                                data.put("currentTime", this.getInfo().get("currentTime"));
                                bridge
                                        .getActivity()
                                        .runOnUiThread(
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        FrameLayout frameLayoutView = getBridge().getActivity().findViewById(frameLayoutViewId);

                                                        if (frameLayoutView != null) {
                                                            ((ViewGroup) getBridge().getWebView().getParent()).removeView(frameLayoutView);
                                                            fragmentUtils.removeFragment(fsFragment);
                                                        }
                                                        fsFragment = null;
                                                        NotificationCenter.defaultCenter().removeAllNotifications();
                                                        notifyListeners("playerExit", data);
                                                    }
                                                }
                                        );
                            }
                        }
                );

        NotificationCenter
                .defaultCenter()
                .addMethodForNotification(
                        "playerTracksChanged",
                        new MyRunnable() {
                            @Override
                            public void run() {
                                JSObject data = new JSObject();
                                Map<String, Object> info = this.getInfo();

                                data.put("fromPlayerId", info.get("fromPlayerId"));

                                if (info.containsKey("audioTrack")) {
                                    JSObject audioTrack = new JSObject();
                                    Map<String, Object> audioInfo = (Map<String, Object>) info.get("audioTrack");
                                    audioTrack.put("id", audioInfo.get("id"));
                                    audioTrack.put("language", audioInfo.get("language"));
                                    audioTrack.put("label", audioInfo.get("label"));
                                    audioTrack.put("codecs", audioInfo.get("codecs"));
                                    audioTrack.put("bitrate", audioInfo.get("bitrate"));
                                    audioTrack.put("channelCount", audioInfo.get("channelCount"));
                                    audioTrack.put("sampleRate", audioInfo.get("sampleRate"));
                                    data.put("audioTrack", audioTrack);
                                }

                                if (info.containsKey("subtitleTrack")) {
                                    JSObject subtitleTrack = new JSObject();
                                    Map<String, Object> subtitleInfo = (Map<String, Object>) info.get("subtitleTrack");
                                    subtitleTrack.put("id", subtitleInfo.get("id"));
                                    subtitleTrack.put("language", subtitleInfo.get("language"));
                                    subtitleTrack.put("label", subtitleInfo.get("label"));
                                    subtitleTrack.put("codecs", subtitleInfo.get("codecs"));
                                    subtitleTrack.put("containerMimeType", subtitleInfo.get("containerMimeType"));
                                    subtitleTrack.put("sampleMimeType", subtitleInfo.get("sampleMimeType"));
                                    data.put("subtitleTrack", subtitleTrack);
                                }

                                notifyListeners("playerTracksChanged", data);
                            }
                        }
                );
    }
}
