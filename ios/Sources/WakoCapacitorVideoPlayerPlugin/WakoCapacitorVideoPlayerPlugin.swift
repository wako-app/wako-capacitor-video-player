import Foundation
import Capacitor

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(WakoCapacitorVideoPlayerPlugin)
public class WakoCapacitorVideoPlayerPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "WakoCapacitorVideoPlayerPlugin"
    public let jsName = "WakoCapacitorVideoPlayer"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "echo", returnType: CAPPluginReturnPromise)
    ]
    private let implementation = WakoCapacitorVideoPlayer()

    @objc func echo(_ call: CAPPluginCall) {
        let value = call.getString("value") ?? ""
        call.resolve([
            "value": implementation.echo(value)
        ])
    }
}
