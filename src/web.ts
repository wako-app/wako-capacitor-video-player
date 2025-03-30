import { WebPlugin } from '@capacitor/core';

import type {
  WakoCapacitorVideoPlayerPlugin,
  capVideoPlayerOptions,
  capVideoVolumeOptions,
  capVideoTimeOptions,
  capVideoMutedOptions,
  capVideoRateOptions,
  capVideoPlayerResult,
  capVideoSubtitlesOptions,
  capEchoOptions,
} from './definitions';
import { VideoPlayer } from './web-utils/videoplayer';

export interface IPlayerSize {
  height: number;
  width: number;
}

export class CapacitorVideoPlayerWeb extends WebPlugin implements WakoCapacitorVideoPlayerPlugin {
  private player: any | null = null;
  private videoContainer!: HTMLDivElement | null;
  private displayMode = '';

  constructor() {
    super();
    this.addListeners();
  }

  async echo(options: capEchoOptions): Promise<capVideoPlayerResult> {
    return Promise.resolve({ result: true, method: 'echo', value: options });
  }

  /**
   *  Player initialization
   *
   * @param options
   */
  async initPlayer(options: capVideoPlayerOptions): Promise<capVideoPlayerResult> {
    if (options == null) {
      return Promise.resolve({
        result: false,
        method: 'initPlayer',
        message: 'Must provide a capVideoPlayerOptions object',
      });
    }

    const url: string = options.url ? options.url : '';
    if (url == null || url.length === 0) {
      return Promise.resolve({
        result: false,
        method: 'initPlayer',
        message: 'Must provide a Video Url',
      });
    }
    if (!url.startsWith('http')) {
      return Promise.resolve({
        result: false,
        method: 'initPlayer',
        message: 'Only HTTP/HTTPS URLs are supported on Web Platform',
      });
    }
    const rate = 1.0;
    const exitOnEnd = true;
    const loopOnEnd = false;

    const componentTag: string = options.componentTag ? options.componentTag : '';
    if (componentTag == null || componentTag.length === 0) {
      return Promise.resolve({
        result: false,
        method: 'initPlayer',
        message: 'Must provide a Component Tag',
      });
    }

    const result = await this._initializeVideoPlayer(url, rate, exitOnEnd, loopOnEnd, componentTag);
    return Promise.resolve({ result: result });
  }
  /**
   * Return if a given playerId is playing
   */
  async isPlaying(): Promise<capVideoPlayerResult> {
    if (this.player) {
      const playing: boolean = this.player.isPlaying;
      return Promise.resolve({
        method: 'isPlaying',
        result: true,
        value: playing,
      });
    } else {
      return Promise.resolve({
        method: 'isPlaying',
        result: false,
        message: 'Player does not exist',
      });
    }
  }

  /**
   * Play the current video
   */
  async play(): Promise<capVideoPlayerResult> {
    if (this.player) {
      await this.player.videoEl.play();
      return Promise.resolve({ method: 'play', result: true, value: true });
    } else {
      return Promise.resolve({
        method: 'play',
        result: false,
        message: 'Player does not exist',
      });
    }
  }
  /**
   * Pause the current video
   */
  async pause(): Promise<capVideoPlayerResult> {
    if (this.player) {
      if (this.player.isPlaying) await this.player.videoEl.pause();
      return Promise.resolve({ method: 'pause', result: true, value: true });
    } else {
      return Promise.resolve({
        method: 'pause',
        result: false,
        message: 'Player does not exist',
      });
    }
  }
  /**
   * Get the duration of the current video
   */
  async getDuration(): Promise<capVideoPlayerResult> {
    if (this.player) {
      const duration: number = this.player.videoEl.duration;
      return Promise.resolve({
        method: 'getDuration',
        result: true,
        value: duration,
      });
    } else {
      return Promise.resolve({
        method: 'getDuration',
        result: false,
        message: 'Player does not exist',
      });
    }
  }
  /**
   * Set the rate of the current video
   *
   * @param options
   */
  async setRate(options: capVideoRateOptions): Promise<capVideoPlayerResult> {
    if (options == null) {
      return Promise.resolve({
        result: false,
        method: 'setRate',
        message: 'Must provide a capVideoRateOptions object',
      });
    }
    const rateList: number[] = [0.25, 0.5, 0.75, 1.0, 2.0, 4.0];
    const rate: number = options.rate && rateList.includes(options.rate) ? options.rate : 1.0;
    if (this.player) {
      this.player.videoEl.playbackRate = rate;
      return Promise.resolve({
        method: 'setRate',
        result: true,
        value: rate,
      });
    } else {
      return Promise.resolve({
        method: 'setRate',
        result: false,
        message: 'Player does not exist',
      });
    }
  }
  /**
   * Get the rate of the current video
   */
  async getRate(): Promise<capVideoPlayerResult> {
    if (this.player) {
      const rate: number = this.player.videoEl.playbackRate;
      return Promise.resolve({
        method: 'getRate',
        result: true,
        value: rate,
      });
    } else {
      return Promise.resolve({
        method: 'getRate',
        result: false,
        message: 'Player does not exist',
      });
    }
  }

  /**
   * Set the volume of the current video
   *
   * @param options
   */
  async setVolume(options: capVideoVolumeOptions): Promise<capVideoPlayerResult> {
    if (options == null) {
      return Promise.resolve({
        result: false,
        method: 'setVolume',
        message: 'Must provide a capVideoVolumeOptions object',
      });
    }
    const volume: number = options.volume ? options.volume : 0.5;
    if (this.player) {
      this.player.videoEl.volume = volume;
      return Promise.resolve({
        method: 'setVolume',
        result: true,
        value: volume,
      });
    } else {
      return Promise.resolve({
        method: 'setVolume',
        result: false,
        message: 'Player does not exist',
      });
    }
  }
  /**
   * Get the volume of the current video
   */
  async getVolume(): Promise<capVideoPlayerResult> {
    if (this.player) {
      const volume: number = this.player.videoEl.volume;
      return Promise.resolve({
        method: 'getVolume',
        result: true,
        value: volume,
      });
    } else {
      return Promise.resolve({
        method: 'getVolume',
        result: false,
        message: 'Player does not exist',
      });
    }
  }
  /**
   * Set the muted property of the current video
   *
   * @param options
   */
  async setMuted(options: capVideoMutedOptions): Promise<capVideoPlayerResult> {
    if (options == null) {
      return Promise.resolve({
        result: false,
        method: 'setMuted',
        message: 'Must provide a capVideoMutedOptions object',
      });
    }
    const muted: boolean = options.muted ? options.muted : false;
    if (this.player) {
      this.player.videoEl.muted = muted;
      return Promise.resolve({
        method: 'setMuted',
        result: true,
        value: muted,
      });
    } else {
      return Promise.resolve({
        method: 'setMuted',
        result: false,
        message: 'Player does not exist',
      });
    }
  }
  /**
   * Get the muted property of the current video
   */
  async getMuted(): Promise<capVideoPlayerResult> {
    if (this.player) {
      const muted: boolean = this.player.videoEl.muted;
      return Promise.resolve({
        method: 'getMuted',
        result: true,
        value: muted,
      });
    } else {
      return Promise.resolve({
        method: 'getMuted',
        result: false,
        message: 'Player does not exist',
      });
    }
  }
  /**
   * Set the current time of the current video
   *
   * @param options
   */
  async setCurrentTime(options: capVideoTimeOptions): Promise<capVideoPlayerResult> {
    if (options == null) {
      return Promise.resolve({
        result: false,
        method: 'setCurrentTime',
        message: 'Must provide a capVideoTimeOptions object',
      });
    }
    let seekTime: number = options.seektime ? options.seektime : 0;
    if (this.player) {
      const duration: number = this.player.videoEl.duration;
      seekTime = seekTime <= duration && seekTime >= 0 ? seekTime : duration / 2;
      this.player.videoEl.currentTime = seekTime;
      return Promise.resolve({
        method: 'setCurrentTime',
        result: true,
        value: seekTime,
      });
    } else {
      return Promise.resolve({
        method: 'setCurrentTime',
        result: false,
        message: 'Player does not exist',
      });
    }
  }
  /**
   * Get the current time of the current video
   */
  async getCurrentTime(): Promise<capVideoPlayerResult> {
    if (this.player) {
      const seekTime: number = this.player.videoEl.currentTime;
      return Promise.resolve({
        method: 'getCurrentTime',
        result: true,
        value: seekTime,
      });
    } else {
      return Promise.resolve({
        method: 'getCurrentTime',
        result: false,
        message: 'Player does not exist',
      });
    }
  }
  /**
   * Stop all players
   */
  async stopAllPlayers(): Promise<capVideoPlayerResult> {
    if (this.player) {
      if (this.player.pipMode) {
        const doc: any = document;
        if (doc.pictureInPictureElement) {
          await doc.exitPictureInPicture();
        }
      }
      if (!this.player.videoEl.paused) this.player.videoEl.pause();
    }
    return Promise.resolve({
      method: 'stopAllPlayers',
      result: true,
      value: true,
    });
  }
  /**
   * Show controller
   */
  async showController(): Promise<capVideoPlayerResult> {
    return Promise.resolve({
      method: 'showController',
      result: true,
      value: true,
    });
  }
  /**
   * isControllerIsFullyVisible
   */
  async isControllerIsFullyVisible(): Promise<capVideoPlayerResult> {
    return Promise.resolve({
      method: 'isControllerIsFullyVisible',
      result: true,
      value: true,
    });
  }
  /**
   * Exit the current player
   */
  async exitPlayer(): Promise<capVideoPlayerResult> {
    return Promise.resolve({
      method: 'exitPlayer',
      result: true,
      value: true,
    });
  }

  /**
   * Enable or disable subtitles
   *
   * @param options
   */
  async enableSubtitles(options: capVideoSubtitlesOptions): Promise<capVideoPlayerResult> {
    if (options == null) {
      return Promise.resolve({
        result: false,
        method: 'enableSubtitles',
        message: 'Must provide a capVideoSubtitlesOptions object',
      });
    }
    const enabled: boolean = options.enabled ? options.enabled : false;
    if (this.displayMode === 'fullscreen' || this.displayMode === 'embedded') {
      console.log(`enableSubtitles with value ${enabled}`);
      // Web implementation doesn't have direct subtitle control
      // You could implement custom subtitle handling here if needed
      return Promise.resolve({
        result: true,
        method: 'enableSubtitles',
        value: enabled,
        message: 'Subtitle control not fully implemented in web version',
      });
    } else {
      return Promise.resolve({
        result: false,
        method: 'enableSubtitles',
      });
    }
  }

  private async _initializeVideoPlayer(
    url: string,
    rate: number,
    exitOnEnd: boolean,
    loopOnEnd: boolean,
    componentTag: string,
  ): Promise<any> {
    const videoURL: string = url ? (url.indexOf('%2F') == -1 ? encodeURI(url) : url) : (null as any);
    if (videoURL === null || !videoURL.startsWith('http')) return Promise.resolve(false);

    this.videoContainer = await this._getContainerElement('fullscreen', componentTag);
    if (this.videoContainer === null)
      return Promise.resolve({
        method: 'initPlayer',
        result: false,
        message: 'componentTag or divContainerElement must be provided',
      });

    this.player = new VideoPlayer(
      'fullscreen',
      videoURL,
      'fullscreen',
      rate,
      exitOnEnd,
      loopOnEnd,
      this.videoContainer,
      99995,
    );
    await this.player.initialize();

    return Promise.resolve({ method: 'initPlayer', result: true, value: true });
  }
  private async _getContainerElement(playerId: string, componentTag: string): Promise<HTMLDivElement | null> {
    const videoContainer: HTMLDivElement = document.createElement('div');
    videoContainer.id = `vc_${playerId}`;
    if (componentTag != null && componentTag.length > 0) {
      const cmpTagEl: HTMLElement | null = document.querySelector(`${componentTag}`);
      if (cmpTagEl === null) return Promise.resolve(null);
      let container: HTMLDivElement | null = null;
      const shadowRoot = cmpTagEl.shadowRoot ? cmpTagEl.shadowRoot : null;
      if (shadowRoot != null) {
        container = shadowRoot.querySelector(`[id='${playerId}']`);
      } else {
        container = cmpTagEl.querySelector(`[id='${playerId}']`);
      }
      if (container != null) container.appendChild(videoContainer);
      return Promise.resolve(videoContainer);
    } else {
      return Promise.resolve(null);
    }
  }
  private handlePlayerPlay(data: any) {
    this.notifyListeners('playerPlay', data);
  }
  private handlePlayerPause(data: any) {
    this.notifyListeners('playerPause', data);
  }
  private handlePlayerEnded(data: any) {
    if (this.displayMode === 'fullscreen') {
      this.videoContainer?.remove();
    }
    this.removeListeners();
    this.notifyListeners('playerEnded', data);
  }
  private handlePlayerExit() {
    if (this.displayMode === 'fullscreen') {
      this.videoContainer?.remove();
    }
    const retData: any = { dismiss: true };
    this.removeListeners();
    this.notifyListeners('playerExit', retData);
  }
  private handlePlayerReady(data: any) {
    this.notifyListeners('playerReady', data);
  }
  private handlePlayerTracksChanged(data: any) {
    this.notifyListeners('playerTracksChanged', data);
  }

  private addListeners() {
    document.addEventListener(
      'videoPlayerPlay',
      (ev: any) => {
        this.handlePlayerPlay(ev.detail);
      },
      false,
    );
    document.addEventListener(
      'videoPlayerPause',
      (ev: any) => {
        this.handlePlayerPause(ev.detail);
      },
      false,
    );
    document.addEventListener(
      'videoPlayerEnded',
      (ev: any) => {
        this.handlePlayerEnded(ev.detail);
      },
      false,
    );
    document.addEventListener(
      'videoPlayerReady',
      (ev: any) => {
        this.handlePlayerReady(ev.detail);
      },
      false,
    );
    document.addEventListener(
      'videoPlayerExit',
      () => {
        this.handlePlayerExit();
      },
      false,
    );
    document.addEventListener(
      'videoPlayerTracksChanged',
      (ev: any) => {
        this.handlePlayerTracksChanged(ev.detail);
      },
      false,
    );
  }

  private removeListeners() {
    document.removeEventListener(
      'videoPlayerPlay',
      (ev: any) => {
        this.handlePlayerPlay(ev.detail);
      },
      false,
    );
    document.removeEventListener(
      'videoPlayerPause',
      (ev: any) => {
        this.handlePlayerPause(ev.detail);
      },
      false,
    );
    document.removeEventListener(
      'videoPlayerEnded',
      (ev: any) => {
        this.handlePlayerEnded(ev.detail);
      },
      false,
    );
    document.removeEventListener(
      'videoPlayerReady',
      (ev: any) => {
        this.handlePlayerReady(ev.detail);
      },
      false,
    );
    document.removeEventListener(
      'videoPlayerExit',
      () => {
        this.handlePlayerExit();
      },
      false,
    );
    document.removeEventListener(
      'videoPlayerTracksChanged',
      (ev: any) => {
        this.handlePlayerTracksChanged(ev.detail);
      },
      false,
    );
  }
}
