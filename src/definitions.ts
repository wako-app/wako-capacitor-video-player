export interface WakoCapacitorVideoPlayerPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
