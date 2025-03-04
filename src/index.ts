import { registerPlugin } from '@capacitor/core';

import type { WakoCapacitorVideoPlayerPlugin } from './definitions';

const WakoCapacitorVideoPlayer = registerPlugin<WakoCapacitorVideoPlayerPlugin>('WakoCapacitorVideoPlayer', {
  web: () => import('./web').then((m) => new m.CapacitorVideoPlayerWeb()),
});

export * from './definitions';
export { WakoCapacitorVideoPlayer };
