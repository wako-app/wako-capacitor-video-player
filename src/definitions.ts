import type { PluginListenerHandle } from '@capacitor/core';

export interface WakoCapacitorVideoPlayerPlugin {
  /**
   * Echo
   *
   */
  echo(options: capEchoOptions): Promise<capVideoPlayerResult>;
  /**
   * Initialize a video player
   *
   */
  initPlayer(options: capVideoPlayerOptions): Promise<capVideoPlayerResult>;
  /**
   * Return if the video is playing
   *
   */
  isPlaying(): Promise<capVideoPlayerResult>;
  /**
   * Play the current video
   *
   */
  play(): Promise<capVideoPlayerResult>;
  /**
   * Pause the current video
   *
   */
  pause(): Promise<capVideoPlayerResult>;
  /**
   * Get the duration of the current video
   *
   */
  getDuration(): Promise<capVideoPlayerResult>;
  /**
   * Get the current time of the current video
   *
   */
  getCurrentTime(): Promise<capVideoPlayerResult>;
  /**
   * Set the current time to seek the current video to
   *
   */
  setCurrentTime(options: capVideoTimeOptions): Promise<capVideoPlayerResult>;
  /**
   * Get the volume of the current video
   *
   */
  getVolume(): Promise<capVideoPlayerResult>;
  /**
   * Set the volume of the current video
   *
   */
  setVolume(options: capVideoVolumeOptions): Promise<capVideoPlayerResult>;
  /**
   * Get the muted state of the current video
   *
   */
  getMuted(): Promise<capVideoPlayerResult>;
  /**
   * Set the muted state of the current video
   *
   */
  setMuted(options: capVideoMutedOptions): Promise<capVideoPlayerResult>;
  /**
   * Set the rate of the current video
   *
   */
  setRate(options: capVideoRateOptions): Promise<capVideoPlayerResult>;
  /**
   * Get the rate of the current video
   *
   */
  getRate(): Promise<capVideoPlayerResult>;
  /**
   * Stop all players playing
   *
   */
  stopAllPlayers(): Promise<capVideoPlayerResult>;
  /**
   * Show controller
   *
   */
  showController(): Promise<capVideoPlayerResult>;
  /**
   * isControllerIsFullyVisible
   *
   */
  isControllerIsFullyVisible(): Promise<capVideoPlayerResult>;
  /**
   * Exit player
   *
   */
  exitPlayer(): Promise<capVideoPlayerResult>;
  /**
   * Listen for changes in the App's active state (whether the app is in the foreground or background)
   *
   * @since 1.0.0
   */
  addListener(eventName: 'playerReady', listenerFunc: PlayerReady): Promise<PluginListenerHandle>;

  addListener(eventName: 'playerPlay', listenerFunc: PlayerPlay): Promise<PluginListenerHandle>;

  addListener(eventName: 'playerPause', listenerFunc: PlayerPause): Promise<PluginListenerHandle>;

  addListener(eventName: 'playerEnded', listenerFunc: PlayerEnded): Promise<PluginListenerHandle>;

  addListener(eventName: 'playerExit', listenerFunc: PlayerExit): Promise<PluginListenerHandle>;

  addListener(eventName: 'playerTracksChanged', listenerFunc: PlayerTracksChanged): Promise<PluginListenerHandle>;
}

export type PlayerReady = (event: capVideoListener) => void;
export type PlayerPlay = (event: capVideoListener) => void;
export type PlayerPause = (event: capVideoListener) => void;
export type PlayerEnded = (event: capVideoListener) => void;
export type PlayerExit = (event: capExitListener) => void;
export type PlayerTracksChanged = (event: TracksChangedInfo) => void;

export interface capEchoOptions {
  /**
   *  String to be echoed
   */

  value?: string;
}
export interface capVideoPlayerOptions {
  /**
   * The url of the video to play
   */
  url?: string;
  /**
   * The subtitle(s) associated with the video as an array of objects
   * Each object must contain:
   * - url: the url of the subtitle file (required)
   * - name: the name of the subtitle (optional)
   * - lang: the language of the subtitle (optional)
   */
  subtitles?: {
    url: string;
    name?: string;
    lang?: string;
  }[];
  /**
   * The default audio language to select, if not found will select the subtitle with the same language if available
   */
  preferredLocale?: string;
  /**
   * SubTitle Options
   */
  subtitleOptions?: SubTitleOptions;

  /**
   * Display Mode ["all", "portrait", "landscape"] (iOS, Android)
   * default: "all"
   */
  displayMode?: string;
  /**
   * Component Tag or DOM Element Tag (React app)
   */
  componentTag?: string;

  /**
   * Title shown in the player (Android)
   * by Manuel García Marín (https://github.com/PhantomPainX)
   */
  title?: string;
  /**
   * Subtitle shown below the title in the player (Android)
   * by Manuel García Marín (https://github.com/PhantomPainX)
   */
  smallTitle?: string;
  /**
   * Chromecast enable/disable (Android)
   * by Manuel García Marín (https://github.com/PhantomPainX)
   * default: true
   */
  chromecast?: boolean;
  /**
   * Artwork url to be shown in Chromecast player
   * by Manuel García Marín (https://github.com/PhantomPainX)
   * default: ""
   */
  artwork?: string;
  /**
   * ID of the subtitle track to select
   */
  subtitleTrackId?: string;

  /**
   * Locale of the subtitle track to select (if subtitleTrackId not found)
   */
  subtitleLocale?: string;

  /**
   * ID of the audio track to select
   */
  audioTrackId?: string;

  /**
   * Locale of the audio track to select (if audioTrackId not found)
   */
  audioLocale?: string;

  /**
   * Start time of the video
   */
  startAtSec?: number;
}

export interface capVideoRateOptions {
  /**
   * Rate value
   */
  rate?: number;
}
export interface capVideoVolumeOptions {
  /**
   * Volume value between [0 - 1]
   */
  volume?: number;
}
export interface capVideoTimeOptions {
  /**
   * Video time value you want to seek to
   */
  seektime?: number;
}
export interface capVideoMutedOptions {
  /**
   * Muted value
   */
  muted?: boolean;
}

export interface capVideoSubtitlesOptions {
  /**
   * Enable or disable subtitles
   */
  enabled?: boolean;
}

export interface capVideoListener {
  /**
   * Video current time when listener trigerred
   */
  currentTime?: number;
}
export interface capExitListener {
  /**
   * Dismiss value true or false
   */
  dismiss?: boolean;
  /**
   * Video current time when listener trigerred
   */
  currentTime?: number;
}
export interface capVideoPlayerResult {
  /**
   * result set to true when successful else false
   */
  result?: boolean;
  /**
   * method name
   */
  method?: string;
  /**
   * value returned
   */
  value?: any;
  /**
   * message string
   */
  message?: string;
}
export interface SubTitleOptions {
  /**
   * Foreground Color in RGBA (default rgba(255,255,255,1)
   */
  foregroundColor?: string;
  /**
   * Background Color in RGBA (default rgba(0,0,0,1)
   */
  backgroundColor?: string;
  /**
   * Font Size in pixels (default 16)
   */
  fontSize?: number;
}

export interface TrackInfo {
  id: string;
  language: string;
  label: string;
  codecs?: string;
  bitrate?: number;
  channelCount?: number;
  sampleRate?: number;
  containerMimeType?: string;
  sampleMimeType?: string;
}

export interface TracksChangedInfo {
  fromPlayerId: string;
  audioTrack?: TrackInfo;
  subtitleTrack?: TrackInfo;
}
