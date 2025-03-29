import Foundation
import Capacitor
import MobileVLCKit

@objc(WakoCapacitorVideoPlayerPlugin)
public class WakoCapacitorVideoPlayerPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "WakoCapacitorVideoPlayerPlugin"
    public let jsName = "WakoCapacitorVideoPlayer"
    private let implementation = WakoCapacitorVideoPlayer()
    
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "initPlayer", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isPlaying", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "play", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "pause", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getDuration", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getCurrentTime", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setCurrentTime", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getVolume", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setVolume", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getMuted", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setMuted", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "setRate", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getRate", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopAllPlayers", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "showController", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isControllerIsFullyVisible", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "exitPlayer", returnType: CAPPluginReturnPromise)
    ]
    
    override public func load() {
        print("[WakoCapacitorVideoPlayerPlugin] Plugin loaded")
        print("[WakoCapacitorVideoPlayerPlugin] Available methods: \(pluginMethods.map { $0.name })")
        
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handlePlayerExited),
            name: NSNotification.Name("WakoPlayerExited"),
            object: nil
        )
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handlePlayerReady),
            name: NSNotification.Name("WakoPlayerReady"),
            object: nil
        )
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(handleTracksChanged),
            name: NSNotification.Name("WakoPlayerTracksChanged"),
            object: nil
        )
    }

    @objc private func handlePlayerReady(_ notification: Notification) {
        print("[WakoCapacitorVideoPlayerPlugin] Player ready notification received")
        if let currentTime = notification.userInfo?["currentTime"] as? Double {
            notifyListeners("playerReady", data: [
                "currentTime": currentTime
            ])
        }
    }

    @objc private func handlePlayerExited() {
        print("[WakoCapacitorVideoPlayerPlugin] Player exited notification received")
        let currentTime = implementation.getCurrentTime()
        let success = implementation.exitPlayer()
        if success {
            print("[WakoCapacitorVideoPlayerPlugin] Player exited successfully from close button")
        } else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: Player not found during close")
        }
        notifyListeners("playerExit", data: [
            "dismiss": success,
            "currentTime": currentTime
        ])
    }

    @objc private func handleTracksChanged(_ notification: Notification) {
        print("[WakoCapacitorVideoPlayerPlugin] Tracks changed notification received")
        var data: [String: Any] = ["fromPlayerId": "player1"]
        if let audioTrack = notification.userInfo?["audioTrack"] as? [String: Any] {
            data["audioTrack"] = audioTrack
        }
        if let subtitleTrack = notification.userInfo?["subtitleTrack"] as? [String: Any] {
            data["subtitleTrack"] = subtitleTrack
        }
        notifyListeners("playerTracksChanged", data: data)
    }
    
    @objc func initPlayer(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] initPlayer called")
        guard let url = call.getString("url") else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: URL is required")
            call.reject("URL is required")
            return
        }
        
        // Extract options from the call
        let options: [String: Any] = [
            "title": call.getString("title") ?? "",
            "subtitleTrackId": call.getString("subtitleTrackId") ?? "",
            "subtitleLocale": call.getString("subtitleLocale") ?? "",
            "audioTrackId": call.getString("audioTrackId") ?? "",
            "audioLocale": call.getString("audioLocale") ?? "",
            "preferredLocale": call.getString("preferredLocale") ?? "",
            "exitOnEnd":  true,
            "loopOnEnd": false,
            "showControls": true,
            "displayMode": call.getString("displayMode") ?? "all",
            "startAtSec": call.getDouble("startAtSec") ?? 0
        ]
        
        implementation.createPlayer(options: options) { [weak self] success in
            guard let self = self else {
                call.reject("Plugin instance lost")
                return
            }
            print("[WakoCapacitorVideoPlayerPlugin] Player created: \(success)")
            
            if success {
                self.implementation.play(url: url) { playSuccess in
                    if playSuccess {
                        print("[WakoCapacitorVideoPlayerPlugin] Play started successfully")
                        call.resolve([
                            "result": true,
                            "method": "initPlayer"
                        ])
                    } else {
                        print("[WakoCapacitorVideoPlayerPlugin] Error: Failed to start playback")
                        call.reject("Failed to start playback")
                    }
                }
            } else {
                print("[WakoCapacitorVideoPlayerPlugin] Error: Failed to create player")
                call.reject("Failed to create player")
            }
        }
    }
    
    @objc func isPlaying(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] isPlaying called")
        let isPlaying = implementation.isPlaying()
        call.resolve([
            "result": true,
            "method": "isPlaying",
            "value": isPlaying
        ])
    }
    
    @objc func play(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] play called")
        guard let url = call.getString("url") else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: URL is required")
            call.reject("URL is required")
            return
        }
        
        print("[WakoCapacitorVideoPlayerPlugin] Playing URL: \(url)")
        print("[WakoCapacitorVideoPlayerPlugin] Implementation: \(implementation)")
        implementation.play(url: url) { success in
            print("[WakoCapacitorVideoPlayerPlugin] Play result: \(success)")
            if success {
                print("[WakoCapacitorVideoPlayerPlugin] Play started successfully")
                call.resolve([
                    "result": true,
                    "method": "play"
                ])
            } else {
                print("[WakoCapacitorVideoPlayerPlugin] Error: Player not found")
                call.reject("Player not found")
            }
        }
    }
    
    @objc func pause(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] pause called")
        let success = implementation.pause()
        if success {
            print("[WakoCapacitorVideoPlayerPlugin] Pause successful")
            call.resolve([
                "result": true,
                "method": "pause"
            ])
        } else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: Player not found")
            call.reject("Player not found")
        }
    }
    
    @objc func getDuration(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] getDuration called")
        let duration = implementation.getDuration()
        print("[WakoCapacitorVideoPlayerPlugin] Duration: \(duration)")
        call.resolve([
            "result": true,
            "method": "getDuration",
            "value": duration
        ])
    }
    
    @objc func getCurrentTime(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] getCurrentTime called")
        let time = implementation.getCurrentTime()
        print("[WakoCapacitorVideoPlayerPlugin] Current time: \(time)")
        call.resolve([
            "result": true,
            "method": "getCurrentTime",
            "value": time
        ])
    }
    
    @objc func setCurrentTime(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] setCurrentTime called")
        guard let seektime = call.getDouble("seektime") else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: seektime is required")
            call.reject("seektime is required")
            return
        }
        
        let success = implementation.seek(time: seektime)
        if success {
            print("[WakoCapacitorVideoPlayerPlugin] Seek successful")
            call.resolve([
                "result": true,
                "method": "setCurrentTime"
            ])
        } else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: Player not found")
            call.reject("Player not found")
        }
    }
    
    @objc func getVolume(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] getVolume called")
        let volume = implementation.getVolume()
        call.resolve([
            "result": true,
            "method": "getVolume",
            "value": volume
        ])
    }
    
    @objc func setVolume(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] setVolume called")
        guard let volume = call.getDouble("volume") else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: volume is required")
            call.reject("volume is required")
            return
        }
        
        let success = implementation.setVolume(volume: Int(volume * 100))
        if success {
            print("[WakoCapacitorVideoPlayerPlugin] Volume set successfully")
            call.resolve([
                "result": true,
                "method": "setVolume"
            ])
        } else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: Player not found or audio not available")
            call.reject("Player not found or audio not available")
        }
    }
    
    @objc func getMuted(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] getMuted called")
        let muted = implementation.getMuted()
        call.resolve([
            "result": true,
            "method": "getMuted",
            "value": muted
        ])
    }
    
    @objc func setMuted(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] setMuted called")
        guard let muted = call.getBool("muted") else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: muted is required")
            call.reject("muted is required")
            return
        }
        
        let success = implementation.setMuted(muted: muted)
        if success {
            print("[WakoCapacitorVideoPlayerPlugin] Muted set successfully")
            call.resolve([
                "result": true,
                "method": "setMuted"
            ])
        } else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: Player not found")
            call.reject("Player not found")
        }
    }
    
    @objc func setRate(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] setRate called")
        guard let rate = call.getDouble("rate") else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: rate is required")
            call.reject("rate is required")
            return
        }
        
        let success = implementation.setRate(rate: rate)
        if success {
            print("[WakoCapacitorVideoPlayerPlugin] Rate set successfully")
            call.resolve([
                "result": true,
                "method": "setRate"
            ])
        } else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: Player not found")
            call.reject("Player not found")
        }
    }
    
    @objc func getRate(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] getRate called")
        let rate = implementation.getRate()
        call.resolve([
            "result": true,
            "method": "getRate",
            "value": rate
        ])
    }
    
    @objc func stopAllPlayers(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] stopAllPlayers called")
        implementation.stopAllPlayers()
        call.resolve([
            "result": true,
            "method": "stopAllPlayers"
        ])
    }
    
    @objc func showController(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] showController called")
        let success = implementation.showController()
        if success {
            print("[WakoCapacitorVideoPlayerPlugin] Controller shown successfully")
            call.resolve([
                "result": true,
                "method": "showController"
            ])
        } else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: Player not found")
            call.reject("Player not found")
        }
    }
    
    @objc func isControllerIsFullyVisible(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] isControllerIsFullyVisible called")
        let isVisible = implementation.isControllerIsFullyVisible()
        call.resolve([
            "result": true,
            "method": "isControllerIsFullyVisible",
            "value": isVisible
        ])
    }
    
    @objc func exitPlayer(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] exitPlayer called")
        let success = implementation.exitPlayer()
        if success {
            print("[WakoCapacitorVideoPlayerPlugin] Player exited successfully")
            call.resolve([
                "result": true,
                "method": "exitPlayer"
            ])
        } else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: Player not found")
            call.reject("Player not found")
        }
    }
}
