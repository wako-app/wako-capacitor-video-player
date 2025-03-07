# wako-capacitor-video-player

A capacitor video player

## Install

```bash
npm install wako-capacitor-video-player
npx cap sync
```

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

Return if a given playerId is playing

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### play()

```typescript
play() => Promise<capVideoPlayerResult>
```

Play the current video from a given playerId

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### pause()

```typescript
pause() => Promise<capVideoPlayerResult>
```

Pause the current video from a given playerId

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### getDuration()

```typescript
getDuration() => Promise<capVideoPlayerResult>
```

Get the duration of the current video from a given playerId

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### getCurrentTime()

```typescript
getCurrentTime() => Promise<capVideoPlayerResult>
```

Get the current time of the current video from a given playerId

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### setCurrentTime(...)

```typescript
setCurrentTime(options: capVideoTimeOptions) => Promise<capVideoPlayerResult>
```

Set the current time to seek the current video to from a given playerId

| Param         | Type                                                                |
| ------------- | ------------------------------------------------------------------- |
| **`options`** | <code><a href="#capvideotimeoptions">capVideoTimeOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### getVolume()

```typescript
getVolume() => Promise<capVideoPlayerResult>
```

Get the volume of the current video from a given playerId

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### setVolume(...)

```typescript
setVolume(options: capVideoVolumeOptions) => Promise<capVideoPlayerResult>
```

Set the volume of the current video to from a given playerId

| Param         | Type                                                                    |
| ------------- | ----------------------------------------------------------------------- |
| **`options`** | <code><a href="#capvideovolumeoptions">capVideoVolumeOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### getMuted()

```typescript
getMuted() => Promise<capVideoPlayerResult>
```

Get the muted of the current video from a given playerId

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### setMuted(...)

```typescript
setMuted(options: capVideoMutedOptions) => Promise<capVideoPlayerResult>
```

Set the muted of the current video to from a given playerId

| Param         | Type                                                                  |
| ------------- | --------------------------------------------------------------------- |
| **`options`** | <code><a href="#capvideomutedoptions">capVideoMutedOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### setRate(...)

```typescript
setRate(options: capVideoRateOptions) => Promise<capVideoPlayerResult>
```

Set the rate of the current video from a given playerId

| Param         | Type                                                                |
| ------------- | ------------------------------------------------------------------- |
| **`options`** | <code><a href="#capvideorateoptions">capVideoRateOptions</a></code> |

**Returns:** <code>Promise&lt;<a href="#capvideoplayerresult">capVideoPlayerResult</a>&gt;</code>

--------------------


### getRate()

```typescript
getRate() => Promise<capVideoPlayerResult>
```

Get the rate of the current video from a given playerId

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
| **`rate`**            | <code>number</code>                                           | Initial playing rate                                                                                                                                                                                                                  |
| **`exitOnEnd`**       | <code>boolean</code>                                          | Exit on VideoEnd (iOS, Android) default: true                                                                                                                                                                                         |
| **`loopOnEnd`**       | <code>boolean</code>                                          | Loop on VideoEnd when exitOnEnd false (iOS, Android) default: false                                                                                                                                                                   |
| **`showControls`**    | <code>boolean</code>                                          | Show Controls Enable (iOS, Android) default: true                                                                                                                                                                                     |
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

| Prop           | Type                 | Description |
| -------------- | -------------------- | ----------- |
| **`playerId`** | <code>string</code>  | Player Id   |
| **`muted`**    | <code>boolean</code> | Muted value |


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
