# wako-capacitor-video-player

![Logo of wako](https://www.wako.app/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Ficon.23e1084e.png&w=640&q=75)

## Official video player for wako

This [Capacitor](https://capacitorjs.com/docs/plugins) plugin is the official video player for [wako](https://wako.app), the media tracking application that allows you to manage and watch your favorite movies and TV shows.

The plugin uses **MobileVLCKit** on iOS and **ExoPlayer (Media3)** on Android to provide a high-quality, feature-rich video playback experience.

## Installation

```bash
npm install wako-capacitor-video-player
npx cap sync
```

## Key Features

The wako-capacitor video player offers numerous advanced features:

- **Video playback** from remote URLs
- **Subtitle support** with customization options
- **Automatic selection of audio tracks and subtitles** based on preferred language
- **Playback controls** (play, pause, seek, volume)
- **Playback rate management** (speed)
- **Portrait and landscape mode support**
- **Chromecast integration** (Android)
- **Event listeners** to customize the viewing experience
- **Customizable user interface options**
- **Intuitive gesture controls** for seamless user interaction

## Usage Examples

### Basic Player Initialization

```typescript
import { WakoCapacitorVideoPlayer } from 'wako-capacitor-video-player';

// Simple initialization
WakoCapacitorVideoPlayer.initPlayer({
  url: "https://archive.org/download/big-buck-bunny-4k-60fps/BigBuckBunny4k60fps.mp4",
  title: "Big Buck Bunny MP4",
  smallTitle: "My small title",
  subtitles: [
    {
      url: "https://raw.githubusercontent.com/padraigfl/subtitle-ssa/refs/heads/master/test/dummySubs/3Lines.ssa",
      name: "My Sub FR",
    },
  ],
});
```

### Advanced Initialization with Complete Options

```typescript
import { WakoCapacitorVideoPlayer } from 'wako-capacitor-video-player';

// Initialization with advanced options
WakoCapacitorVideoPlayer.initPlayer({
  url: "https://example.com/video.mp4",
  title: "My video title",
  smallTitle: "Short description",
  subtitles: [
    {
      url: "https://example.com/subtitles-fr.vtt",
      name: "French",
      lang: "fr"
    },
    {
      url: "https://example.com/subtitles-en.vtt",
      name: "English",
      lang: "en"
    }
  ],
  preferredLocale: "fr",
  subtitleOptions: {
    foregroundColor: "rgba(255,255,255,1)",
    backgroundColor: "rgba(0,0,0,0.7)",
    fontSize: 18
  },
  displayMode: "all",
  chromecast: true,
  artwork: "https://example.com/poster.jpg",
  startAtSec: 30
});
```

### Playback Control

```typescript
// Play
WakoCapacitorVideoPlayer.play();

// Pause
WakoCapacitorVideoPlayer.pause();

// Navigation within the video
WakoCapacitorVideoPlayer.setCurrentTime({ seektime: 120 }); // Go to 2 minutes

// Volume control
WakoCapacitorVideoPlayer.setVolume({ volume: 0.5 }); // 50% volume

// Playback speed
WakoCapacitorVideoPlayer.setRate({ rate: 1.5 }); // 1.5x
```

### Gesture Controls

The wako-capacitor video player includes intuitive touch gesture controls for an enhanced user experience:

#### Double Tap Gestures
- **Double tap right side**: Fast-forward the video (typically 10 seconds)
- **Double tap left side**: Rewind the video (typically 10 seconds)

These double tap gestures provide a quick and intuitive way for users to navigate through the video without having to interact with the traditional seek bar.

#### Additional Gesture Controls
- **Single tap**: Show/hide the player controls
- **Swipe left/right**: Seek through the video
- **Swipe up/down** (right side): Adjust volume
- **Swipe up/down** (left side): Adjust brightness (on supported devices)
- **Pinch gesture**: Zoom in/out on the video content

Gesture controls automatically adapt to the device orientation and work seamlessly in both portrait and landscape modes, providing a consistent user experience across different viewing scenarios.

### Event Handling

```typescript
// Listen for play event
WakoCapacitorVideoPlayer.addListener('playerPlay', (info) => {
  console.log('Video is playing, current time:', info.currentTime);
});

// Listen for end of video event
WakoCapacitorVideoPlayer.addListener('playerEnded', (info) => {
  console.log('Playback has ended');
});

// Listen for player exit event
WakoCapacitorVideoPlayer.addListener('playerExit', (info) => {
  console.log('User exited the player');
  console.log('Current time:', info.currentTime);
  console.log('Exit requested by user:', info.dismiss);
});
```

## Advanced Use Cases

### Integration in a React Application

```typescript
import { useEffect, useState } from 'react';
import { WakoCapacitorVideoPlayer } from 'wako-capacitor-video-player';

const VideoPlayerComponent = () => {
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);

  useEffect(() => {
    // Player initialization
    const initializePlayer = async () => {
      await WakoCapacitorVideoPlayer.initPlayer({
        url: "https://example.com/video.mp4",
        title: "My video",
        smallTitle: "Description",
        subtitles: [
          { url: "https://example.com/subtitles-fr.vtt", name: "French", lang: "fr" }
        ],
        preferredLocale: "fr"
      });
    };

    // Event listeners
    const playListener = WakoCapacitorVideoPlayer.addListener('playerPlay', (info) => {
      setIsPlaying(true);
      setCurrentTime(info.currentTime);
    });

    const pauseListener = WakoCapacitorVideoPlayer.addListener('playerPause', (info) => {
      setIsPlaying(false);
      setCurrentTime(info.currentTime);
    });

    const exitListener = WakoCapacitorVideoPlayer.addListener('playerExit', (info) => {
      setIsPlaying(false);
      setCurrentTime(info.currentTime);
    });

    initializePlayer();

    // Cleanup listeners
    return () => {
      playListener.then(listener => listener.remove());
      pauseListener.then(listener => listener.remove());
      exitListener.then(listener => listener.remove());
    };
  }, []);

  return (
    <div>
      <div>Status: {isPlaying ? 'Playing' : 'Paused'}</div>
      <div>Current time: {currentTime} seconds</div>
      <button onClick={() => WakoCapacitorVideoPlayer.play()}>Play</button>
      <button onClick={() => WakoCapacitorVideoPlayer.pause()}>Pause</button>
    </div>
  );
};

export default VideoPlayerComponent;
```

### Managing Multiple Audio Tracks and Subtitles

```typescript
import { WakoCapacitorVideoPlayer } from 'wako-capacitor-video-player';

// Initialization with multiple audio track and subtitle options
WakoCapacitorVideoPlayer.initPlayer({
  url: "https://example.com/film.mp4",
  title: "Multi-language film",
  subtitles: [
    { url: "https://example.com/subtitles-fr.vtt", name: "French", lang: "fr" },
    { url: "https://example.com/subtitles-en.vtt", name: "English", lang: "en" },
    { url: "https://example.com/subtitles-es.vtt", name: "Spanish", lang: "es" },
    { url: "https://example.com/subtitles-de.vtt", name: "German", lang: "de" }
  ],
  preferredLocale: "fr", // Automatic selection of French track if available
});

// Listen for track changes
WakoCapacitorVideoPlayer.addListener('playerTracksChanged', (info) => {
  console.log('Current audio track:', info.audioTrack);
  console.log('Current subtitle track:', info.subtitleTrack);
});
```

## Error Handling

To handle potential errors when using the plugin, you can use the standard promise structure:

```typescript
try {
  const result = await WakoCapacitorVideoPlayer.initPlayer({
    url: "https://example.com/video.mp4",
    title: "My Video"
  });
  
  if (result.result) {
    console.log("Player initialized successfully");
  } else {
    console.error("Initialization error:", result.message);
  }
} catch (error) {
  console.error("Exception during player initialization:", error);
}
```

## User Interface Customization

### Custom Subtitle Colors

```typescript
WakoCapacitorVideoPlayer.initPlayer({
  url: "https://example.com/video.mp4",
  title: "Video with custom subtitles",
  subtitles: [
    { url: "https://example.com/subtitles.vtt", name: "English", lang: "en" }
  ],
  subtitleOptions: {
    foregroundColor: "rgba(255,255,0,1)", // Yellow text
    backgroundColor: "rgba(0,0,0,0.5)",   // Semi-transparent black background
    fontSize: 20                         // Larger font size
  }
});
```

### Specific Display Modes

```typescript
// Portrait mode only
WakoCapacitorVideoPlayer.initPlayer({
  url: "https://example.com/video-portrait.mp4",
  title: "Portrait Mode",
  displayMode: "portrait"
});

// Landscape mode only
WakoCapacitorVideoPlayer.initPlayer({
  url: "https://example.com/video-landscape.mp4",
  title: "Landscape Mode",
  displayMode: "landscape"
});
```

## Chromecast Integration (Android)

The Chromecast feature allows users to stream video content to a Chromecast-compatible device:

```typescript
WakoCapacitorVideoPlayer.initPlayer({
  url: "https://example.com/video-hd.mp4",
  title: "HD Movie",
  smallTitle: "HD Streaming",
  chromecast: true,
  artwork: "https://example.com/poster.jpg" // Image displayed on Chromecast screen
});
```

## Platform-Specific Capabilities

### Android
- Based on **ExoPlayer (Media3)** for optimized video playback
- Full Chromecast support
- Interface customization with title and subtitle
- Artwork for Chromecast sessions

### iOS
- Built with **MobileVLCKit** for powerful and flexible playback
- Native interface adapted to Apple standards
- Support for specific orientation modes
- Performance optimization for iOS devices

## Optimization Tips

1. **Subtitle preloading**: Ensure subtitle files are available before initializing the player
2. **Adaptive video format**: Use formats like HLS or DASH for network bandwidth adaptation
3. **Memory management**: Call `stopAllPlayers()` or `exitPlayer()` when you no longer need the player
4. **Local storage**: For frequently watched videos, consider caching them locally

## FAQ

**Q: How can I implement a playback resume mechanism?**
A: Use `addListener('playerExit')` to save the position with `getCurrentTime()`, then use `startAtSec` when reinitializing.

**Q: Can I customize the player controls?**
A: Controls are natively managed by the platform. You can show/hide them with `showControls` but not customize them.

**Q: How do I handle videos with DRM?**
A: The plugin supports URLs with standard DRM protection, but specific configurations may be necessary depending on the DRM system.

## Security and Permissions

The plugin requires certain permissions to function properly:

### Android
- `INTERNET` for online content playback
- `ACCESS_NETWORK_STATE` for connectivity detection

### iOS
- No specific permissions required beyond the standard application capabilities

---

The wako-capacitor video player is actively maintained and optimized to offer the best possible viewing experience, while seamlessly integrating into the [wako app](https://wako.app) ecosystem.

## API

<docgen-index>

* [`echo(...)`](#echo)
* [`initPlayer(...)`](#initplayer)
* [`isPlaying()`](#isplaying)
* [`play()`](#play)
* [`pause()`](#pause)
* [`getDuration()`](#getduration)
* [`getCurrentTime()`](#getcurrenttime)
* [`setCurrentTime(...)`](#setcurrenttime)
* [`getVolume()`](#getvolume)
* [`setVolume(...)`](#setvolume)
* [`getMuted()`](#getmuted)
* [`setMuted(...)`](#setmuted)
* [`setRate(...)`](#setrate)
* [`getRate()`](#getrate)
* [`stopAllPlayers()`](#stopallplayers)
* [`showController()`](#showcontroller)
* [`isControllerIsFullyVisible()`](#iscontrollerisfullyvisible)
* [`exitPlayer()`](#exitplayer)
* [`addListener('playerReady', ...)`](#addlistenerplayerready-)
* [`addListener('playerPlay', ...)`](#addlistenerplayerplay-)
* [`addListener('playerPause', ...)`](#addlistenerplayerpause-)
* [`addListener('playerEnded', ...)`](#addlistenerplayerended-)
* [`addListener('playerExit', ...)`](#addlistenerplayerexit-)
* [`addListener('playerTracksChanged', ...)`](#addlistenerplayertrackschanged-)
* [Interfaces](#interfaces)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### echo(...)

```typescript
echo(options: capEchoOptions) => Promise<capVideoPlayerResult>
```

Echo

| Param         | Type                                                      |
| ------------- | --------------------------------------------------------- |
| **`options`** | <code><a href="#capechooptions">capEchoOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### initPlayer(...)

```typescript
initPlayer(options: capVideoPlayerOptions) => Promise<capVideoPlayerResult>
```

Initialize a video player

| Param         | Type                                                                    |
| ------------- | ----------------------------------------------------------------------- |
| **`options`** | <code><a href="#capvideoplayeroptions">capVideoPlayerOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### isPlaying()

```typescript
isPlaying() => Promise<capVideoPlayerResult>
```

Return if the video is playing

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### play()

```typescript
play() => Promise<capVideoPlayerResult>
```

Play the current video

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### pause()

```typescript
pause() => Promise<capVideoPlayerResult>
```

Pause the current video

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### getDuration()

```typescript
getDuration() => Promise<capVideoPlayerResult>
```

Get the duration of the current video

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### getCurrentTime()

```typescript
getCurrentTime() => Promise<capVideoPlayerResult>
```

Get the current time of the current video

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### setCurrentTime(...)

```typescript
setCurrentTime(options: capVideoTimeOptions) => Promise<capVideoPlayerResult>
```

Set the current time to seek the current video to

| Param         | Type                                                                |
| ------------- | ------------------------------------------------------------------- |
| **`options`** | <code><a href="#capvideotimeoptions">capVideoTimeOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### getVolume()

```typescript
getVolume() => Promise<capVideoPlayerResult>
```

Get the volume of the current video

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### setVolume(...)

```typescript
setVolume(options: capVideoVolumeOptions) => Promise<capVideoPlayerResult>
```

Set the volume of the current video

| Param         | Type                                                                    |
| ------------- | ----------------------------------------------------------------------- |
| **`options`** | <code><a href="#capvideovolumeoptions">capVideoVolumeOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### getMuted()

```typescript
getMuted() => Promise<capVideoPlayerResult>
```

Get the muted state of the current video

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### setMuted(...)

```typescript
setMuted(options: capVideoMutedOptions) => Promise<capVideoPlayerResult>
```

Set the muted state of the current video

| Param         | Type                                                                  |
| ------------- | --------------------------------------------------------------------- |
| **`options`** | <code><a href="#capvideomutedoptions">capVideoMutedOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### setRate(...)

```typescript
setRate(options: capVideoRateOptions) => Promise<capVideoPlayerResult>
```

Set the rate of the current video

| Param         | Type                                                                |
| ------------- | ------------------------------------------------------------------- |
| **`options`** | <code><a href="#capvideorateoptions">capVideoRateOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### getRate()

```typescript
getRate() => Promise<capVideoPlayerResult>
```

Get the rate of the current video

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### stopAllPlayers()

```typescript
stopAllPlayers() => Promise<capVideoPlayerResult>
```

Stop all players playing

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### showController()

```typescript
showController() => Promise<capVideoPlayerResult>
```

Show controller

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### isControllerIsFullyVisible()

```typescript
isControllerIsFullyVisible() => Promise<capVideoPlayerResult>
```

isControllerIsFullyVisible

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### exitPlayer()

```typescript
exitPlayer() => Promise<capVideoPlayerResult>
```

Exit player

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### addListener('playerReady', ...)

```typescript
addListener(eventName: 'playerReady', listenerFunc: PlayerReady) => Promise<PluginListenerHandle>
```

Listen for changes in the App's active state (whether the app is in the foreground or background)

| Param              | Type                                                |
| ------------------ | --------------------------------------------------- |
| **`eventName`**    | <code>'playerReady'</code>                          |
| **`listenerFunc`** | <code><a href="#playerready">PlayerReady</a></code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

**Since:** 1.0.0

--------------------


### addListener('playerPlay', ...)

```typescript
addListener(eventName: 'playerPlay', listenerFunc: PlayerPlay) => Promise<PluginListenerHandle>
```

| Param              | Type                                              |
| ------------------ | ------------------------------------------------- |
| **`eventName`**    | <code>'playerPlay'</code>                         |
| **`listenerFunc`** | <code><a href="#playerplay">PlayerPlay</a></code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### addListener('playerPause', ...)

```typescript
addListener(eventName: 'playerPause', listenerFunc: PlayerPause) => Promise<PluginListenerHandle>
```

| Param              | Type                                                |
| ------------------ | --------------------------------------------------- |
| **`eventName`**    | <code>'playerPause'</code>                          |
| **`listenerFunc`** | <code><a href="#playerpause">PlayerPause</a></code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### addListener('playerEnded', ...)

```typescript
addListener(eventName: 'playerEnded', listenerFunc: PlayerEnded) => Promise<PluginListenerHandle>
```

| Param              | Type                                                |
| ------------------ | --------------------------------------------------- |
| **`eventName`**    | <code>'playerEnded'</code>                          |
| **`listenerFunc`** | <code><a href="#playerended">PlayerEnded</a></code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### addListener('playerExit', ...)

```typescript
addListener(eventName: 'playerExit', listenerFunc: PlayerExit) => Promise<PluginListenerHandle>
```

| Param              | Type                                              |
| ------------------ | ------------------------------------------------- |
| **`eventName`**    | <code>'playerExit'</code>                         |
| **`listenerFunc`** | <code><a href="#playerexit">PlayerExit</a></code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### addListener('playerTracksChanged', ...)

```typescript
addListener(eventName: 'playerTracksChanged', listenerFunc: PlayerTracksChanged) => Promise<PluginListenerHandle>
```

| Param              | Type                                                                |
| ------------------ | ------------------------------------------------------------------- |
| **`eventName`**    | <code>'playerTracksChanged'</code>                                  |
| **`listenerFunc`** | <code><a href="#playertrackschanged">PlayerTracksChanged</a></code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### Interfaces


#### capVideoPlayerResult

| Prop          | Type                 | Description                                   |
| ------------- | -------------------- | --------------------------------------------- |
| **`result`**  | <code>boolean</code> | result set to true when successful else false |
| **`method`**  | <code>string</code>  | method name                                   |
| **`value`**   | <code>any</code>     | value returned                                |
| **`message`** | <code>string</code>  | message string                                |


#### capEchoOptions

| Prop        | Type                | Description         |
| ----------- | ------------------- | ------------------- |
| **`value`** | <code>string</code> | String to be echoed |


#### capVideoPlayerOptions

| Prop                  | Type                                                          | Description                                                                                                                                                                                                                           |
| --------------------- | ------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **`url`**             | <code>string</code>                                           | The url of the video to play                                                                                                                                                                                                          |
| **`subtitles`**       | <code>{ url: string; name?: string; lang?: string; }[]</code> | The subtitle(s) associated with the video as an array of objects Each object must contain: - url: the url of the subtitle file (required) - name: the name of the subtitle (optional) - lang: the language of the subtitle (optional) |
| **`preferredLocale`** | <code>string</code>                                           | The default audio language to select, if not found will select the subtitle with the same language if available                                                                                                                       |
| **`subtitleOptions`** | <code><a href="#subtitleoptions">SubTitleOptions</a></code>   | SubTitle Options                                                                                                                                                                                                                      |
| **`displayMode`**     | <code>string</code>                                           | Display Mode ["all", "portrait", "landscape"] (iOS, Android) default: "all"                                                                                                                                                           |
| **`componentTag`**    | <code>string</code>                                           | Component Tag or DOM Element Tag (React app)                                                                                                                                                                                          |
| **`title`**           | <code>string</code>                                           | Title shown in the player (Android) by Manuel García Marín (https://github.com/PhantomPainX)                                                                                                                                          |
| **`smallTitle`**      | <code>string</code>                                           | Subtitle shown below the title in the player (Android) by Manuel García Marín (https://github.com/PhantomPainX)                                                                                                                       |
| **`chromecast`**      | <code>boolean</code>                                          | Chromecast enable/disable (Android) by Manuel García Marín (https://github.com/PhantomPainX) default: true                                                                                                                            |
| **`artwork`**         | <code>string</code>                                           | Artwork url to be shown in Chromecast player by Manuel García Marín (https://github.com/PhantomPainX) default: ""                                                                                                                     |
| **`subtitleTrackId`** | <code>string</code>                                           | ID of the subtitle track to select                                                                                                                                                                                                    |
| **`subtitleLocale`**  | <code>string</code>                                           | Locale of the subtitle track to select (if subtitleTrackId not found)                                                                                                                                                                 |
| **`audioTrackId`**    | <code>string</code>                                           | ID of the audio track to select                                                                                                                                                                                                       |
| **`audioLocale`**     | <code>string</code>                                           | Locale of the audio track to select (if audioTrackId not found)                                                                                                                                                                       |
| **`startAtSec`**      | <code>number</code>                                           | Start time of the video                                                                                                                                                                                                               |


#### SubTitleOptions

| Prop                  | Type                | Description                                           |
| --------------------- | ------------------- | ----------------------------------------------------- |
| **`foregroundColor`** | <code>string</code> | Foreground Color in RGBA (default rgba(255,255,255,1) |
| **`backgroundColor`** | <code>string</code> | Background Color in RGBA (default rgba(0,0,0,1)       |
| **`fontSize`**        | <code>number</code> | Font Size in pixels (default 16)                      |


#### capVideoTimeOptions

| Prop           | Type                | Description                          |
| -------------- | ------------------- | ------------------------------------ |
| **`seektime`** | <code>number</code> | Video time value you want to seek to |


#### capVideoVolumeOptions

| Prop         | Type                | Description                  |
| ------------ | ------------------- | ---------------------------- |
| **`volume`** | <code>number</code> | Volume value between [0 - 1] |


#### capVideoMutedOptions

| Prop        | Type                 | Description |
| ----------- | -------------------- | ----------- |
| **`muted`** | <code>boolean</code> | Muted value |


#### capVideoRateOptions

| Prop       | Type                | Description |
| ---------- | ------------------- | ----------- |
| **`rate`** | <code>number</code> | Rate value  |


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


#### capVideoListener

| Prop              | Type                | Description                                |
| ----------------- | ------------------- | ------------------------------------------ |
| **`currentTime`** | <code>number</code> | Video current time when listener trigerred |


#### capExitListener

| Prop              | Type                 | Description                                |
| ----------------- | -------------------- | ------------------------------------------ |
| **`dismiss`**     | <code>boolean</code> | Dismiss value true or false                |
| **`currentTime`** | <code>number</code>  | Video current time when listener trigerred |


#### TracksChangedInfo

| Prop                | Type                                            |
| ------------------- | ----------------------------------------------- |
| **`fromPlayerId`**  | <code>string</code>                             |
| **`audioTrack`**    | <code><a href="#trackinfo">TrackInfo</a></code> |
| **`subtitleTrack`** | <code><a href="#trackinfo">TrackInfo</a></code> |


#### TrackInfo

| Prop                    | Type                |
| ----------------------- | ------------------- |
| **`id`**                | <code>string</code> |
| **`language`**          | <code>string</code> |
| **`label`**             | <code>string</code> |
| **`codecs`**            | <code>string</code> |
| **`bitrate`**           | <code>number</code> |
| **`channelCount`**      | <code>number</code> |
| **`sampleRate`**        | <code>number</code> |
| **`containerMimeType`** | <code>string</code> |
| **`sampleMimeType`**    | <code>string</code> |


### Type Aliases


#### PlayerReady

<code>(event: <a href="#capvideolistener">capVideoListener</a>): void</code>


#### PlayerPlay

<code>(event: <a href="#capvideolistener">capVideoListener</a>): void</code>


#### PlayerPause

<code>(event: <a href="#capvideolistener">capVideoListener</a>): void</code>


#### PlayerEnded

<code>(event: <a href="#capvideolistener">capVideoListener</a>): void</code>


#### PlayerExit

<code>(event: <a href="#capexitlistener">capExitListener</a>): void</code>


#### PlayerTracksChanged

<code>(event: <a href="#trackschangedinfo">TracksChangedInfo</a>): void</code>

</docgen-api>
