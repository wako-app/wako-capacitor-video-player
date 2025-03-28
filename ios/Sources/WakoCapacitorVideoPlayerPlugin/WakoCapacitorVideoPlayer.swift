import Foundation
import Capacitor
import MobileVLCKit
import UIKit

@objc public class WakoCapacitorVideoPlayer: NSObject, VLCMediaPlayerDelegate {
    private var player: VLCMediaPlayer?
    private var videoView: UIView?
    
    @objc public func createPlayer() -> String {
        print("[WakoCapacitorVideoPlayer] Creating player")
        if player != nil {
            print("[WakoCapacitorVideoPlayer] Player already exists, stopping it")
            player?.stop()
        }
        
        // Créer la vue pour le player
        if videoView == nil {
            videoView = UIView(frame: UIScreen.main.bounds)
            videoView?.backgroundColor = .black
        }
        
        // Créer le player
        player = VLCMediaPlayer()
        player?.delegate = self
        
        // Attacher la vue au player
        if let videoView = videoView {
            player?.drawable = videoView
            print("[WakoCapacitorVideoPlayer] Video view attached to player")
            
            // Ajouter la vue à la fenêtre principale
            DispatchQueue.main.async {
                if let window = UIApplication.shared.windows.first {
                    window.addSubview(videoView)
                    print("[WakoCapacitorVideoPlayer] Video view added to window")
                }
            }
        }
        
        print("[WakoCapacitorVideoPlayer] Player created with delegate: \(String(describing: player))")
        return "default"
    }
    
    @objc public func play(url: String) -> Bool {
        print("[WakoCapacitorVideoPlayer] Attempting to play URL: \(url)")
        print("[WakoCapacitorVideoPlayer] Current player: \(String(describing: player))")
        
        guard let player = player else {
            print("[WakoCapacitorVideoPlayer] Error: Player is nil")
            return false
        }
        
        guard let mediaUrl = URL(string: url) else {
            print("[WakoCapacitorVideoPlayer] Error: Invalid URL")
            return false
        }
        
        print("[WakoCapacitorVideoPlayer] Creating media with URL: \(mediaUrl)")
        let media = VLCMedia(url: mediaUrl)
        print("[WakoCapacitorVideoPlayer] Media created: \(String(describing: media))")
        
        print("[WakoCapacitorVideoPlayer] Setting media to player")
        player.media = media
        print("[WakoCapacitorVideoPlayer] Media set to player")
        
        print("[WakoCapacitorVideoPlayer] Starting playback")
        player.play()
        print("[WakoCapacitorVideoPlayer] Play command sent")
        return true
    }
    
    @objc public func pause() -> Bool {
        guard let player = player else { return false }
        player.pause()
        return true
    }
    
    @objc public func seek(time: Double) -> Bool {
        guard let player = player else { return false }
        player.time = VLCTime(int: Int32(time * 1000))
        return true
    }
    
    @objc public func setVolume(volume: Int) -> Bool {
        guard let player = player,
              let audio = player.audio else { return false }
        audio.volume = Int32(volume)
        return true
    }
    
    @objc public func getCurrentTime() -> Double {
        guard let player = player else { return 0 }
        return Double(player.time.intValue) / 1000.0
    }
    
    @objc public func getDuration() -> Double {
        guard let player = player,
              let media = player.media else { return 0 }
        return Double(media.length.intValue) / 1000.0
    }
    
    @objc public func isPlaying() -> Bool {
        guard let player = player else { return false }
        return player.isPlaying
    }
    
    @objc public func getVolume() -> Double {
        guard let player = player,
              let audio = player.audio else { return 0 }
        return Double(audio.volume) / 100.0
    }
    
    @objc public func getMuted() -> Bool {
        guard let player = player,
              let audio = player.audio else { return false }
        return audio.isMuted
    }
    
    @objc public func setMuted(muted: Bool) -> Bool {
        guard let player = player,
              let audio = player.audio else { return false }
        audio.isMuted = muted
        return true
    }
    
    @objc public func setRate(rate: Double) -> Bool {
        guard let player = player else { return false }
        player.rate = Float(rate)
        return true
    }
    
    @objc public func getRate() -> Double {
        guard let player = player else { return 1.0 }
        return Double(player.rate)
    }
    
    @objc public func stopAllPlayers() {
        player?.stop()
        player = nil
    }
    
    @objc public func showController() -> Bool {
        guard let player = player else { return false }
        // VLCKit ne gère pas directement l'affichage des contrôles
        // On retourne true car c'est géré par le système
        return true
    }
    
    @objc public func isControllerIsFullyVisible() -> Bool {
        guard let player = player else { return false }
        // VLCKit ne gère pas directement la visibilité des contrôles
        // On retourne true car c'est géré par le système
        return true
    }
    
    @objc public func exitPlayer() -> Bool {
        guard player != nil else { return false }
        
        player?.stop()
        
        // Retirer la vue
        DispatchQueue.main.async {
            self.videoView?.removeFromSuperview()
            self.videoView = nil
        }
        
        self.player = nil
        return true
    }
    
    // MARK: - VLCMediaPlayerDelegate
    
    public func mediaPlayerStateChanged(_ aNotification: Notification) {
        guard let player = aNotification.object as? VLCMediaPlayer else { return }
        print("[WakoCapacitorVideoPlayer] State changed: \(player.state.rawValue)")
        
        switch player.state {
        case .opening:
            print("[WakoCapacitorVideoPlayer] Opening media")
        case .buffering:
            print("[WakoCapacitorVideoPlayer] Buffering")
        case .playing:
            print("[WakoCapacitorVideoPlayer] Playing")
            if let media = player.media {
                print("[WakoCapacitorVideoPlayer] Media length: \(media.length.intValue / 1000) seconds")
            }
        case .paused:
            print("[WakoCapacitorVideoPlayer] Paused")
        case .stopped:
            print("[WakoCapacitorVideoPlayer] Stopped")
        case .error:
            print("[WakoCapacitorVideoPlayer] Error occurred")
            print("[WakoCapacitorVideoPlayer] Current state: \(player.state.rawValue)")
        default:
            print("[WakoCapacitorVideoPlayer] Unknown state")
        }
    }
    
    public func mediaPlayerTimeChanged(_ aNotification: Notification) {
        guard let player = aNotification.object as? VLCMediaPlayer else { return }
        print("[WakoCapacitorVideoPlayer] Time changed: \(player.time.intValue / 1000) seconds")
    }
    
    public func mediaPlayerTitleChanged(_ aNotification: Notification) {
        guard let player = aNotification.object as? VLCMediaPlayer else { return }
        print("[WakoCapacitorVideoPlayer] Title changed")
        if let media = player.media {
            print("[WakoCapacitorVideoPlayer] Media URL: \(media.url)")
        }
    }
} 