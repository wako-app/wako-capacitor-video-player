import { WebPlugin } from '@capacitor/core';

import type { WakoCapacitorVideoPlayerPlugin } from './definitions';

export class WakoCapacitorVideoPlayerWeb extends WebPlugin implements WakoCapacitorVideoPlayerPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
