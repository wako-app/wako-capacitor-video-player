import { WakoCapacitorVideoPlayer } from 'wako-capacitor-video-player';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    WakoCapacitorVideoPlayer.echo({ value: inputValue })
}
