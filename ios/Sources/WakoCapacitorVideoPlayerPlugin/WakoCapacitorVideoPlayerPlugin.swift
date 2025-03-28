import Foundation
import Capacitor
import MobileVLCKit

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(WakoCapacitorVideoPlayerPlugin)
public class WakoCapacitorVideoPlayerPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "WakoCapacitorVideoPlayerPlugin"
    public let jsName = "WakoCapacitorVideoPlayer"
    private let implementation = WakoCapacitorVideoPlayer()
    
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "echo", returnType: CAPPluginReturnPromise),
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
    }

    @objc func echo(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] echo called")
        let value = call.getString("value") ?? ""
        call.resolve([
            "value": value
        ])
    }
    
    @objc func initPlayer(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] initPlayer called")
        guard let url = call.getString("url") else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: URL is required")
            call.reject("URL is required")
            return
        }
        
        implementation.createPlayer()
        print("[WakoCapacitorVideoPlayerPlugin] Player created")
        
        let success = implementation.play(url: url)
        if success {
            print("[WakoCapacitorVideoPlayerPlugin] Play started successfully")
            call.resolve()
        } else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: Failed to start playback")
            call.reject("Failed to start playback")
        }
    }
    
    @objc func isPlaying(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] isPlaying called")
        let isPlaying = implementation.isPlaying()
        call.resolve([
            "isPlaying": isPlaying
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
        let success = implementation.play(url: url)
        print("[WakoCapacitorVideoPlayerPlugin] Play result: \(success)")
        if success {
            print("[WakoCapacitorVideoPlayerPlugin] Play started successfully")
            call.resolve()
        } else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: Player not found")
            call.reject("Player not found")
        }
    }
    
    @objc func pause(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] pause called")
        let success = implementation.pause()
        if success {
            print("[WakoCapacitorVideoPlayerPlugin] Pause successful")
            call.resolve()
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
            "duration": duration
        ])
    }
    
    @objc func getCurrentTime(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] getCurrentTime called")
        let time = implementation.getCurrentTime()
        print("[WakoCapacitorVideoPlayerPlugin] Current time: \(time)")
        call.resolve([
            "time": time
        ])
    }
    
    @objc func setCurrentTime(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] setCurrentTime called")
        guard let seektime = call.getDouble("seektime") else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: seektime is required")
            call.reject("seektime is required")
            return
        }
        
        let success = implementation.seek( time: seektime)
        if success {
            print("[WakoCapacitorVideoPlayerPlugin] Seek successful")
            call.resolve()
        } else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: Player not found")
            call.reject("Player not found")
        }
    }
    
    @objc func getVolume(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] getVolume called")
        let volume = implementation.getVolume()
        call.resolve([
            "volume": volume
        ])
    }
    
    @objc func setVolume(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] setVolume called")
        guard let volume = call.getDouble("volume") else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: volume is required")
            call.reject("volume is required")
            return
        }
        
        let success = implementation.setVolume( volume: Int(volume * 100))
        if success {
            print("[WakoCapacitorVideoPlayerPlugin] Volume set successfully")
            call.resolve()
        } else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: Player not found or audio not available")
            call.reject("Player not found or audio not available")
        }
    }
    
    @objc func getMuted(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] getMuted called")
        let muted = implementation.getMuted()
        call.resolve([
            "muted": muted
        ])
    }
    
    @objc func setMuted(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] setMuted called")
        guard let muted = call.getBool("muted") else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: muted is required")
            call.reject("muted is required")
            return
        }
        
        let success = implementation.setMuted( muted: muted)
        if success {
            print("[WakoCapacitorVideoPlayerPlugin] Muted set successfully")
            call.resolve()
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
        
        let success = implementation.setRate( rate: rate)
        if success {
            print("[WakoCapacitorVideoPlayerPlugin] Rate set successfully")
            call.resolve()
        } else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: Player not found")
            call.reject("Player not found")
        }
    }
    
    @objc func getRate(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] getRate called")
        let rate = implementation.getRate()
        call.resolve([
            "rate": rate
        ])
    }
    
    @objc func stopAllPlayers(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] stopAllPlayers called")
        implementation.stopAllPlayers()
        call.resolve()
    }
    
    @objc func showController(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] showController called")
        let success = implementation.showController()
        if success {
            print("[WakoCapacitorVideoPlayerPlugin] Controller shown successfully")
            call.resolve()
        } else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: Player not found")
            call.reject("Player not found")
        }
    }
    
    @objc func isControllerIsFullyVisible(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] isControllerIsFullyVisible called")
        let isVisible = implementation.isControllerIsFullyVisible()
        call.resolve([
            "isVisible": isVisible
        ])
    }
    
    @objc func exitPlayer(_ call: CAPPluginCall) {
        print("[WakoCapacitorVideoPlayerPlugin] exitPlayer called")
        let success = implementation.exitPlayer()
        if success {
            print("[WakoCapacitorVideoPlayerPlugin] Player exited successfully")
            call.resolve()
        } else {
            print("[WakoCapacitorVideoPlayerPlugin] Error: Player not found")
            call.reject("Player not found")
        }
    }
}
