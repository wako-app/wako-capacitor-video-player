import Foundation
import MobileVLCKit
import UIKit
import AVKit

public class WakoCapacitorVideoPlayer {
    private var mediaPlayer: VLCMediaPlayer?
    private var playerViewController: PlayerViewController?

    public func createPlayer(options: [String: Any], completion: @escaping (Bool) -> Void) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else {
                completion(false)
                return
            }
            
            print("[WakoCapacitorVideoPlayer] createPlayer called with options: \(options)")
            
            if let subtitles = options["subtitles"] as? [[String: Any]] {
                print("[WakoCapacitorVideoPlayer] Found \(subtitles.count) external subtitles:")
                for (index, subtitle) in subtitles.enumerated() {
                    print("[WakoCapacitorVideoPlayer] Subtitle \(index + 1):")
                    if let url = subtitle["url"] as? String {
                        print("   - URL: \(url)")
                    }
                    if let name = subtitle["name"] as? String {
                        print("   - Name: \(name)")
                    }
                    if let lang = subtitle["lang"] as? String {
                        print("   - Language: \(lang)")
                    }
                }
            } else {
                print("[WakoCapacitorVideoPlayer] No external subtitles found in options")
            }
            
            let player = VLCMediaPlayer()
            player.drawable = nil
            
            self.mediaPlayer = player
            self.playerViewController = PlayerViewController(mediaPlayer: player, options: options)
            completion(true)
        }
    }

    public func play(url: String, completion: @escaping (Bool) -> Void) {
        guard let mediaPlayer = mediaPlayer, let playerViewController = playerViewController else {
            print("[WakoCapacitorVideoPlayer] Play failed: mediaPlayer or playerViewController is nil")
            completion(false)
            return
        }
        guard let mediaURL = URL(string: url) else {
            print("[WakoCapacitorVideoPlayer] Play failed: Invalid URL - \(url)")
            completion(false)
            return
        }
        print("[WakoCapacitorVideoPlayer] Attempting to play URL: \(url)")
        
        // Create the media
        let media = VLCMedia(url: mediaURL)
        
        // Configure options for the media
        var options: [String: Any] = [:]
        
        // If startTime is defined, configure options for VLC
        let startTime = playerViewController.getStartAtSec()
        if startTime > 0 {
            // The "start-time" option is in seconds
            print("[WakoCapacitorVideoPlayer] Setting start-time option: \(startTime) seconds")
            options["start-time"] = NSNumber(value: Double(startTime))
        }
        
        // Configure subtitle appearance
        options["sub-text-scale"] = NSNumber(value: 0.6) // 60% of default size
        
        // Start with subtitles disabled until the user selects one
        options["sub-track"] = NSNumber(value: -1)
        
        // Network caching options
        options["network-caching"] = NSNumber(value: 2000) // 2 seconds
        options["file-caching"] = NSNumber(value: 1500)
        options["live-caching"] = NSNumber(value: 1500)
        
        // Log external subtitles information
        if let externalSubtitles = playerViewController.getExternalSubtitles(), !externalSubtitles.isEmpty {
            print("[WakoCapacitorVideoPlayer] Found \(externalSubtitles.count) external subtitles to add after playback starts")
            
            for (index, subtitle) in externalSubtitles.enumerated() {
                if let urlString = subtitle["url"] as? String {
                    let name = subtitle["name"] as? String ?? "Subtitle \(index + 1)"
                    let lang = subtitle["lang"] as? String ?? ""
                    print("[WakoCapacitorVideoPlayer] Subtitle \(index + 1): \(name) [\(lang)] - \(urlString)")
                }
            }
        } else {
            print("[WakoCapacitorVideoPlayer] No external subtitles to add")
        }
        
        // Apply all options to the media
        media.addOptions(options)
        
        // Set the media to the player
        mediaPlayer.media = media
        
        DispatchQueue.main.async {
            if let rootViewController = UIApplication.shared.windows.first?.rootViewController {
                playerViewController.modalPresentationStyle = .fullScreen
                rootViewController.present(playerViewController, animated: true) {
                    // Start playback
                    mediaPlayer.play()
                    print("[WakoCapacitorVideoPlayer] Playback started")
                    completion(true)
                }
            } else {
                print("[WakoCapacitorVideoPlayer] No root view controller found")
                completion(false)
            }
        }
    }

    public func isPlaying() -> Bool {
        return mediaPlayer?.isPlaying ?? false
    }

    public func pause() -> Bool {
        guard let mediaPlayer = mediaPlayer else { return false }
        DispatchQueue.main.async {
            mediaPlayer.pause()
        }
        return true
    }

    public func getDuration() -> Double {
        guard let mediaPlayer = mediaPlayer, let media = mediaPlayer.media else { return 0.0 }
        return Double(media.length.value?.intValue ?? 0) / 1000
    }

    public func getCurrentTime() -> Double {
        guard let mediaPlayer = mediaPlayer else { return 0.0 }
        return Double(mediaPlayer.time.value?.intValue ?? 0) / 1000
    }

    public func seek(time: Double) -> Bool {
        guard let mediaPlayer = mediaPlayer else { return false }
        DispatchQueue.main.async {
            mediaPlayer.time = VLCTime(int: Int32(time * 1000))
        }
        return true
    }

    public func getVolume() -> Int {
        guard let mediaPlayer = mediaPlayer, let audio = mediaPlayer.audio else { return 0 }
        return Int(audio.volume)
    }

    public func setVolume(volume: Int) -> Bool {
        guard let mediaPlayer = mediaPlayer, let audio = mediaPlayer.audio else { return false }
        DispatchQueue.main.async {
            audio.volume = Int32(volume)
        }
        return true
    }

    public func getMuted() -> Bool {
        guard let mediaPlayer = mediaPlayer, let audio = mediaPlayer.audio else { return false }
        return audio.isMuted
    }

    public func setMuted(muted: Bool) -> Bool {
        guard let mediaPlayer = mediaPlayer, let audio = mediaPlayer.audio else { return false }
        DispatchQueue.main.async {
            audio.isMuted = muted
        }
        return true
    }

    public func setRate(rate: Double) -> Bool {
        guard let mediaPlayer = mediaPlayer else { return false }
        DispatchQueue.main.async {
            mediaPlayer.rate = Float(rate)
        }
        return true
    }

    public func getRate() -> Double {
        return Double(mediaPlayer?.rate ?? 1.0)
    }

    public func stopAllPlayers() {
        DispatchQueue.main.async { [weak self] in
            self?.mediaPlayer?.stop()
            self?.playerViewController?.dismiss(animated: true)
            self?.playerViewController = nil
            self?.mediaPlayer = nil
        }
    }

    public func showController() -> Bool {
        return playerViewController != nil
    }

    public func isControllerIsFullyVisible() -> Bool {
        return playerViewController?.isBeingPresented ?? false
    }

    public func exitPlayer() -> Bool {
        guard let playerViewController = playerViewController else { return false }
        DispatchQueue.main.async { [weak self] in
            self?.mediaPlayer?.stop()
            playerViewController.dismiss(animated: true)
            self?.playerViewController = nil
            self?.mediaPlayer = nil
        }
        return true
    }
}

class PlayerViewController: UIViewController {
    private var mediaPlayer: VLCMediaPlayer
    private var videoView = UIView()
    private var controlsView = UIView()
    private var titleLabel = UILabel()
    private var playPauseButton = UIButton(type: .system)
    private var closeButton = UIButton(type: .system)
    private var seekBar = UISlider()
    private var subtitleButton = UIButton(type: .system)
    private var audioTrackButton = UIButton(type: .system)
    private var loadingIndicator = UIActivityIndicatorView(style: .large)
    private var currentTimeLabel = UILabel()
    private var totalTimeLabel = UILabel()
    private var castButton = UIButton(type: .system)
    private var controlsTimer: Timer?
    private var isControlsVisible = true
    private var hasStartedPlaying = false
    
    private var rewindIndicator: UIView?
    private var forwardIndicator: UIView?
    private let seekDuration: Float = 10.0

    private var videoTitle: String?
    private var subtitleTrackId: String?
    private var subtitleLocale: String?
    private var audioTrackId: String?
    private var audioLocale: String?
    private var preferredLocale: String?
    private var exitOnEnd: Bool = true
    private var loopOnEnd: Bool = false
    private var showControls: Bool = true
    private var displayMode: String = "all"
    private var startAtSec: Double = 0
    private var isAirPlayAvailable = false
    private var airplayButton: UIButton?
    
    private var externalSubtitles: [[String: Any]]?

    init(mediaPlayer: VLCMediaPlayer, options: [String: Any]) {
        print("[WakoCapacitorVideoPlayer] Initializing player with options: \(options)")
        self.mediaPlayer = mediaPlayer
        self.videoTitle = options["title"] as? String
        self.subtitleTrackId = options["subtitleTrackId"] as? String
        self.subtitleLocale = options["subtitleLocale"] as? String
        self.audioTrackId = options["audioTrackId"] as? String
        self.audioLocale = options["audioLocale"] as? String
        self.preferredLocale = options["preferredLocale"] as? String
        self.exitOnEnd = options["exitOnEnd"] as? Bool ?? true
        self.loopOnEnd = options["loopOnEnd"] as? Bool ?? false
        self.showControls = options["showControls"] as? Bool ?? true
        self.displayMode = options["displayMode"] as? String ?? "all"
        self.startAtSec = options["startAtSec"] as? Double ?? 0
        
        self.externalSubtitles = options["subtitles"] as? [[String: Any]]
        if let subs = self.externalSubtitles {
            print("[WakoCapacitorVideoPlayer] External subtitles provided: \(subs.count)")
        }
        
        super.init(nibName: nil, bundle: nil)
        self.rewindIndicator = nil
        self.forwardIndicator = nil
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        
        setupUI()
        setupMediaPlayer()
        
        controlsView.isHidden = false
        closeButton.isHidden = false
        titleLabel.isHidden = false
        castButton.isHidden = !isAirPlayAvailable
        
        controlsView.alpha = 1.0
        closeButton.alpha = 1.0
        titleLabel.alpha = 1.0
        castButton.alpha = isAirPlayAvailable ? 1.0 : 0.0
        
        resetControlsTimer()
        
        checkAirPlayAvailability()
        
        // Enable keyboard support
        becomeFirstResponder()
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        modalPresentationStyle = .fullScreen
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        
        // Disable keyboard support
        resignFirstResponder()
    }

    private func setupUI() {
        view.backgroundColor = .black
        
        view.addSubview(videoView)
        videoView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            videoView.topAnchor.constraint(equalTo: view.topAnchor),
            videoView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            videoView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            videoView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
        
        setupDoubleTapIndicators()
        setupDoubleTapGestureRecognizers()

        controlsView.backgroundColor = .black.withAlphaComponent(0.7)
        view.addSubview(controlsView)
        controlsView.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            controlsView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            controlsView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            controlsView.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),
            controlsView.heightAnchor.constraint(equalToConstant: 100)
        ])

        titleLabel.text = videoTitle
        titleLabel.textColor = .white
        titleLabel.font = .systemFont(ofSize: 16, weight: .medium)
        titleLabel.textAlignment = .center
        titleLabel.numberOfLines = 1
        titleLabel.lineBreakMode = .byTruncatingTail
        view.addSubview(titleLabel)
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            titleLabel.leadingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.leadingAnchor, constant: 40),
            titleLabel.trailingAnchor.constraint(equalTo: view.safeAreaLayoutGuide.trailingAnchor, constant: -40),
            titleLabel.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 10),
            titleLabel.heightAnchor.constraint(equalToConstant: 30)
        ])

        playPauseButton.setImage(UIImage(systemName: "play.fill"), for: .normal)
        playPauseButton.tintColor = .white
        playPauseButton.addTarget(self, action: #selector(togglePlayPause), for: .touchUpInside)
        controlsView.addSubview(playPauseButton)
        playPauseButton.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            playPauseButton.centerXAnchor.constraint(equalTo: controlsView.centerXAnchor),
            playPauseButton.centerYAnchor.constraint(equalTo: controlsView.centerYAnchor),
            playPauseButton.widthAnchor.constraint(equalToConstant: 50),
            playPauseButton.heightAnchor.constraint(equalToConstant: 50)
        ])

        subtitleButton.setImage(UIImage(systemName: "captions.bubble.fill"), for: .normal)
        subtitleButton.tintColor = .white
        subtitleButton.addTarget(self, action: #selector(showSubtitles), for: .touchUpInside)
        controlsView.addSubview(subtitleButton)
        subtitleButton.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            subtitleButton.leadingAnchor.constraint(equalTo: playPauseButton.trailingAnchor, constant: 30),
            subtitleButton.centerYAnchor.constraint(equalTo: controlsView.centerYAnchor),
            subtitleButton.widthAnchor.constraint(equalToConstant: 50),
            subtitleButton.heightAnchor.constraint(equalToConstant: 50)
        ])

        audioTrackButton.setImage(UIImage(systemName: "ear"), for: .normal)
        audioTrackButton.tintColor = .white
        audioTrackButton.addTarget(self, action: #selector(showAudioTracks), for: .touchUpInside)
        controlsView.addSubview(audioTrackButton)
        audioTrackButton.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            audioTrackButton.trailingAnchor.constraint(equalTo: playPauseButton.leadingAnchor, constant: -30),
            audioTrackButton.centerYAnchor.constraint(equalTo: controlsView.centerYAnchor),
            audioTrackButton.widthAnchor.constraint(equalToConstant: 50),
            audioTrackButton.heightAnchor.constraint(equalToConstant: 50)
        ])

        closeButton.setImage(UIImage(systemName: "xmark"), for: .normal)
        closeButton.tintColor = .white
        closeButton.addTarget(self, action: #selector(closePlayer), for: .touchUpInside)
        view.addSubview(closeButton)
        closeButton.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            closeButton.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 10),
            closeButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -10),
            closeButton.widthAnchor.constraint(equalToConstant: 30),
            closeButton.heightAnchor.constraint(equalToConstant: 30)
        ])

        seekBar.minimumValue = 0
        seekBar.isContinuous = true
        seekBar.addTarget(self, action: #selector(seekBarValueChanged), for: .valueChanged)
        controlsView.addSubview(seekBar)
        seekBar.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            seekBar.leadingAnchor.constraint(equalTo: controlsView.leadingAnchor, constant: 10),
            seekBar.trailingAnchor.constraint(equalTo: controlsView.trailingAnchor, constant: -10),
            seekBar.bottomAnchor.constraint(equalTo: playPauseButton.topAnchor, constant: -10)
        ])

        let seekBarTapGesture = UITapGestureRecognizer(target: self, action: #selector(seekBarTapped(_:)))
        seekBar.addGestureRecognizer(seekBarTapGesture)
        seekBar.isUserInteractionEnabled = true

        currentTimeLabel.textColor = .white
        currentTimeLabel.font = .systemFont(ofSize: 12)
        currentTimeLabel.text = "00:00"
        controlsView.addSubview(currentTimeLabel)
        currentTimeLabel.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            currentTimeLabel.leadingAnchor.constraint(equalTo: seekBar.leadingAnchor),
            currentTimeLabel.topAnchor.constraint(equalTo: seekBar.bottomAnchor, constant: 2)
        ])

        totalTimeLabel.textColor = .white
        totalTimeLabel.font = .systemFont(ofSize: 12)
        totalTimeLabel.text = "00:00"
        controlsView.addSubview(totalTimeLabel)
        totalTimeLabel.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            totalTimeLabel.trailingAnchor.constraint(equalTo: seekBar.trailingAnchor),
            totalTimeLabel.topAnchor.constraint(equalTo: seekBar.bottomAnchor, constant: 2)
        ])

        loadingIndicator.color = .white
        view.addSubview(loadingIndicator)
        loadingIndicator.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            loadingIndicator.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            loadingIndicator.centerYAnchor.constraint(equalTo: view.centerYAnchor)
        ])

        castButton.setImage(UIImage(systemName: "airplayvideo"), for: .normal)
        castButton.tintColor = .white
        castButton.addTarget(self, action: #selector(showCastOptions), for: .touchUpInside)
        controlsView.addSubview(castButton)
        castButton.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            castButton.leadingAnchor.constraint(equalTo: subtitleButton.trailingAnchor, constant: 30),
            castButton.centerYAnchor.constraint(equalTo: controlsView.centerYAnchor),
            castButton.widthAnchor.constraint(equalToConstant: 50),
            castButton.heightAnchor.constraint(equalToConstant: 50)
        ])
        
        subtitleButton.isEnabled = false
    }

    private func checkAirPlayAvailability() {
        let tempPlayer = AVPlayerViewController()
        tempPlayer.view.frame = CGRect(x: 0, y: 0, width: 1, height: 1)
        view.addSubview(tempPlayer.view)
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { [weak self] in
            guard let self = self else { return }
            
            if let airplayButton = self.findAirPlayButton(in: tempPlayer.view) {
                self.isAirPlayAvailable = true
                self.airplayButton = airplayButton
                print("[WakoCapacitorVideoPlayer] AirPlay is available")
            } else {
                self.isAirPlayAvailable = false
                print("[WakoCapacitorVideoPlayer] AirPlay is not available")
            }
            
            tempPlayer.view.removeFromSuperview()
        }
    }

    private func findAirPlayButton(in view: UIView) -> UIButton? {
        for subview in view.subviews {
            if let button = subview as? UIButton,
               button.image(for: .normal)?.accessibilityIdentifier == "airplay" {
                return button
            }
            if let button = findAirPlayButton(in: subview) {
                return button
            }
        }
        return nil
    }

    @objc private func togglePlayPause() {
        if mediaPlayer.isPlaying {
            mediaPlayer.pause()
        } else {
            mediaPlayer.play()
        }
        updatePlayPauseButton()
        resetControlsTimer()
    }

    private func updatePlayPauseButton() {
        let imageName = mediaPlayer.isPlaying ? "pause.fill" : "play.fill"
        playPauseButton.setImage(UIImage(systemName: imageName), for: .normal)
    }

    @objc private func closePlayer() {
        mediaPlayer.stop()
        dismiss(animated: true) {
            NotificationCenter.default.post(name: NSNotification.Name("WakoPlayerExited"), object: nil)
        }
    }

    @objc private func seekBarValueChanged() {
        let time = VLCTime(int: Int32(seekBar.value * 1000))
        mediaPlayer.time = time
        resetControlsTimer()
    }

    @objc private func seekBarTapped(_ gesture: UITapGestureRecognizer) {
        let location = gesture.location(in: seekBar)
        let percentage = location.x / seekBar.bounds.width
        let newValue = seekBar.minimumValue + (seekBar.maximumValue - seekBar.minimumValue) * Float(percentage)
        seekBar.value = newValue
        seekBarValueChanged()
    }

    @objc private func showSubtitles() {
        guard let subtitleTracks = mediaPlayer.videoSubTitlesIndexes as? [NSNumber],
              let subtitleNames = mediaPlayer.videoSubTitlesNames as? [String] else {
            print("[WakoCapacitorVideoPlayer] No subtitles available or mismatch in data")
            return
        }
        
        // Log all available subtitle tracks for debugging
        print("[WakoCapacitorVideoPlayer] Available subtitle tracks: \(subtitleTracks.count)")
        for (i, track) in subtitleTracks.enumerated() {
            if i < subtitleNames.count {
                print("[WakoCapacitorVideoPlayer] Track \(i): ID=\(track), Name='\(subtitleNames[i])'")
            }
        }
        
        // Create alert controller
        let alert = UIAlertController(title: "Select Subtitle", message: nil, preferredStyle: .actionSheet)
        
        // Configure popover for iPad
        if UIDevice.current.userInterfaceIdiom == .pad {
            if let popover = alert.popoverPresentationController {
                popover.sourceView = subtitleButton
                popover.sourceRect = subtitleButton.bounds
            }
        }
        
        // Add the Disable option first
        let disableAction = UIAlertAction(title: "Disable", style: .default) { _ in
            print("[WakoCapacitorVideoPlayer] Disabling subtitles")
            self.mediaPlayer.currentVideoSubTitleIndex = -1
            self.resetControlsTimer()
            
            let subtitleInfo = self.trackInfo(forTrackIndex: -1, name: "Disabled", type: "subtitle")
            let audioInfo = self.getCurrentAudioTrackInfo()
            
            NotificationCenter.default.post(
                name: NSNotification.Name("WakoPlayerTracksChanged"),
                object: nil,
                userInfo: [
                    "subtitleTrack": subtitleInfo,
                    "audioTrack": audioInfo
                ]
            )
        }
        disableAction.setValue(mediaPlayer.currentVideoSubTitleIndex == -1, forKey: "checked")
        alert.addAction(disableAction)
        
        // Create a dictionary to track added internal subtitle tracks by their name
        var addedSubtitleNames = Set<String>()
        
        // PHASE 1: Add all internal subtitle tracks first
        let processedTracks = min(subtitleTracks.count, subtitleNames.count)
        for index in 0..<processedTracks {
            let trackIndex = subtitleTracks[index]
            let originalName = subtitleNames[index]
            
            // Skip any "Disable" internal options, since we added our own already
            if originalName.lowercased() == "disable" || originalName.lowercased() == "disabled" || originalName == "Off" {
                continue
            }
            
            // Track this subtitle name to avoid duplicates later
            addedSubtitleNames.insert(originalName)
            
            let action = UIAlertAction(title: originalName, style: .default) { _ in
                print("[WakoCapacitorVideoPlayer] Selecting subtitle track: \(originalName) at index: \(trackIndex)")
                self.mediaPlayer.currentVideoSubTitleIndex = trackIndex.int32Value
                self.resetControlsTimer()
                
                let subtitleInfo = self.trackInfo(forTrackIndex: trackIndex.int32Value, name: originalName, type: "subtitle")
                let audioInfo = self.getCurrentAudioTrackInfo()
                
                NotificationCenter.default.post(
                    name: NSNotification.Name("WakoPlayerTracksChanged"),
                    object: nil,
                    userInfo: [
                        "subtitleTrack": subtitleInfo,
                        "audioTrack": audioInfo
                    ]
                )
            }
            action.setValue(mediaPlayer.currentVideoSubTitleIndex == trackIndex.int32Value, forKey: "checked")
            alert.addAction(action)
        }
        
        // PHASE 2: Add external subtitles explicitly if they are not already visible 
        // in the internal subtitle tracks
        if let externalSubs = externalSubtitles, !externalSubs.isEmpty {
            print("[WakoCapacitorVideoPlayer] Adding external subtitles to menu: \(externalSubs.count)")
            
            // Track seen URLs to avoid exact duplicates
            var addedUrls = Set<String>()
            
            for (index, subtitle) in externalSubs.enumerated() {
                if let urlString = subtitle["url"] as? String, 
                   !urlString.isEmpty,
                   !addedUrls.contains(urlString) {
                   
                    let name = subtitle["name"] as? String ?? "Subtitle \(index + 1)"
                    let lang = subtitle["lang"] as? String ?? ""
                    let displayName = !lang.isEmpty ? "\(name) [\(lang)]" : name
                    
                    // Check if we already have this specific name in the list
                    if addedSubtitleNames.contains(displayName) {
                        print("[WakoCapacitorVideoPlayer] Skipping external subtitle - name already in list: \(displayName)")
                        continue
                    }
                    
                    // Check if this URL appears as a subtitle name (for VLC-added subtitles)
                    var skipThisSubtitle = false
                    for subtitleName in subtitleNames {
                        if subtitleName.contains(urlString) {
                            skipThisSubtitle = true
                            print("[WakoCapacitorVideoPlayer] Skipping external subtitle as its URL appears in internal list: \(urlString)")
                            break
                        }
                    }
                    
                    if skipThisSubtitle {
                        continue
                    }
                    
                    print("[WakoCapacitorVideoPlayer] Adding external subtitle to menu: \(displayName)")
                    
                    // Add URL to our tracking set 
                    addedUrls.insert(urlString)
                    
                    // Create action for this subtitle - note we'll need to add it first
                    let action = UIAlertAction(title: displayName, style: .default) { _ in
                        print("[WakoCapacitorVideoPlayer] Selected external subtitle: \(displayName)")
                        
                        // Create URL from string
                        guard let url = URL(string: urlString) else {
                            print("[WakoCapacitorVideoPlayer] Invalid URL for external subtitle: \(urlString)")
                            return
                        }
                        
                        // Add subtitle as media slave
                        let result = self.mediaPlayer.addPlaybackSlave(url, type: .subtitle, enforce: true)
                        let success = result != 0
                        
                        print("[WakoCapacitorVideoPlayer] External subtitle added on demand: \(success ? "SUCCESS" : "FAILED") (result=\(result))")
                        
                        if success {
                            // After a delay to let VLC process the subtitle
                            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                                // Check if subtitle was added and select it
                                if let tracks = self.mediaPlayer.videoSubTitlesIndexes as? [NSNumber], 
                                   let names = self.mediaPlayer.videoSubTitlesNames as? [String] {
                                    // Find the newly added subtitle
                                    for (i, trackIdx) in tracks.enumerated() {
                                        if i < names.count {
                                            let trackName = names[i]
                                            // This is a bit of a guess - the last added subtitle might be the one
                                            // Or a track containing the URL or matching our display name
                                            if trackName.contains(urlString) || trackName == displayName || i == tracks.count - 1 {
                                                print("[WakoCapacitorVideoPlayer] Selecting newly added subtitle track: \(trackName)")
                                                self.mediaPlayer.currentVideoSubTitleIndex = trackIdx.int32Value
                                                
                                                let subtitleInfo = self.trackInfo(forTrackIndex: trackIdx.int32Value, name: displayName, type: "subtitle")
                                                let audioInfo = self.getCurrentAudioTrackInfo()
                                                
                                                NotificationCenter.default.post(
                                                    name: NSNotification.Name("WakoPlayerTracksChanged"),
                                                    object: nil,
                                                    userInfo: [
                                                        "subtitleTrack": subtitleInfo,
                                                        "audioTrack": audioInfo
                                                    ]
                                                )
                                                break
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        self.resetControlsTimer()
                    }
                    alert.addAction(action)
                }
            }
        }
        
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        present(alert, animated: true)
    }

    // Helper function to get human-readable language name from code
    private func getLanguageName(from code: String) -> String? {
        let languageMap = [
            "en": "English",
            "fr": "French",
            "es": "Spanish",
            "de": "German",
            "it": "Italian",
            "pt": "Portuguese",
            "ru": "Russian",
            "ja": "Japanese",
            "zh": "Chinese",
            "ko": "Korean",
            "ar": "Arabic",
            "hi": "Hindi",
            "tr": "Turkish",
            "nl": "Dutch",
            "sv": "Swedish",
            "no": "Norwegian",
            "da": "Danish",
            "fi": "Finnish",
            "pl": "Polish",
            "cs": "Czech",
            "hu": "Hungarian",
            "el": "Greek",
            "he": "Hebrew",
            "th": "Thai",
            "fre": "French",
            "ger": "German",
            "ita": "Italian",
            "spa": "Spanish",
            "eng": "English"
        ]
        
        return languageMap[code.lowercased()]
    }

    @objc private func showAudioTracks() {
        guard let audioTracks = mediaPlayer.audioTrackIndexes as? [NSNumber],
              let audioNames = mediaPlayer.audioTrackNames as? [String],
              audioTracks.count == audioNames.count else {
            print("[WakoCapacitorVideoPlayer] No audio tracks available or mismatch in data")
            return
        }
        
        let alert = UIAlertController(title: "Select Audio Track", message: nil, preferredStyle: .actionSheet)
        
        // Configure popover for iPad
        if UIDevice.current.userInterfaceIdiom == .pad {
            if let popover = alert.popoverPresentationController {
                popover.sourceView = audioTrackButton
                popover.sourceRect = audioTrackButton.bounds
            }
        }
        
        var hasDisableOption = false
        for name in audioNames {
            if name.lowercased() == "disable" || name.lowercased() == "disabled" {
                hasDisableOption = true
                break
            }
        }
        
        if !hasDisableOption {
            let disableAction = UIAlertAction(title: "Disable", style: .default) { _ in
                print("[WakoCapacitorVideoPlayer] Disabling audio track")
                self.mediaPlayer.currentAudioTrackIndex = -1
                self.resetControlsTimer()
                
                let audioInfo = self.trackInfo(forTrackIndex: -1, name: "Disabled", type: "audio")
                let subtitleInfo = self.getCurrentSubtitleTrackInfo()
                
                NotificationCenter.default.post(
                    name: NSNotification.Name("WakoPlayerTracksChanged"),
                    object: nil,
                    userInfo: [
                        "audioTrack": audioInfo,
                        "subtitleTrack": subtitleInfo
                    ]
                )
            }
            disableAction.setValue(mediaPlayer.currentAudioTrackIndex == -1, forKey: "checked")
            alert.addAction(disableAction)
        }
        
        for (index, trackIndex) in audioTracks.enumerated() {
            let trackName = audioNames[index]
            let action = UIAlertAction(title: trackName, style: .default) { _ in
                print("[WakoCapacitorVideoPlayer] Selecting audio track: \(trackName) at index: \(trackIndex)")
                self.mediaPlayer.currentAudioTrackIndex = trackIndex.int32Value
                self.resetControlsTimer()
                
                let audioInfo = self.trackInfo(forTrackIndex: trackIndex.int32Value, name: trackName, type: "audio")
                let subtitleInfo = self.getCurrentSubtitleTrackInfo()
                
                NotificationCenter.default.post(
                    name: NSNotification.Name("WakoPlayerTracksChanged"),
                    object: nil,
                    userInfo: [
                        "audioTrack": audioInfo,
                        "subtitleTrack": subtitleInfo
                    ]
                )
            }
            action.setValue(mediaPlayer.currentAudioTrackIndex == trackIndex.int32Value, forKey: "checked")
            alert.addAction(action)
        }
        
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        present(alert, animated: true)
    }

    @objc private func showCastOptions() {
        guard let airplayButton = airplayButton else { return }
        
        let menu = UIMenuController.shared
        let targetRect = airplayButton.convert(airplayButton.bounds, to: view)
        
        let menuItems = [
            UIMenuItem(title: "AirPlay", action: #selector(showAirPlayMenu))
        ]
        menu.menuItems = menuItems
        
        menu.showMenu(from: view, rect: targetRect)
    }

    @objc private func showAirPlayMenu() {
        if let airplayButton = airplayButton {
            airplayButton.sendActions(for: .touchUpInside)
        }
    }

    @objc private func toggleControlsVisibility() {
        let shouldShow = controlsView.isHidden
        
        if shouldShow {
            controlsView.alpha = 0
            closeButton.alpha = 0
            titleLabel.alpha = 0
            castButton.alpha = 0
            
            controlsView.isHidden = false
            closeButton.isHidden = false
            titleLabel.isHidden = false
            castButton.isHidden = !isAirPlayAvailable
            
            UIView.animate(withDuration: 0.3, animations: {
                self.controlsView.alpha = 1
                self.closeButton.alpha = 1
                self.titleLabel.alpha = 1
                self.castButton.alpha = self.isAirPlayAvailable ? 1 : 0
            })
            
            resetControlsTimer()
        } else {
            UIView.animate(withDuration: 0.3, animations: {
                self.controlsView.alpha = 0
                self.closeButton.alpha = 0
                self.titleLabel.alpha = 0
                self.castButton.alpha = 0
            }, completion: { _ in
                self.controlsView.isHidden = true
                self.closeButton.isHidden = true
                self.titleLabel.isHidden = true
                self.castButton.isHidden = true
            })
        }
    }
    
    @objc private func hideControls() {
        UIView.animate(withDuration: 0.3, animations: {
            self.controlsView.alpha = 0
            self.closeButton.alpha = 0
            self.titleLabel.alpha = 0
            self.castButton.alpha = 0
        }, completion: { _ in
            self.controlsView.isHidden = true
            self.closeButton.isHidden = true
            self.titleLabel.isHidden = true
            self.castButton.isHidden = true
        })
    }

    private func resetControlsTimer() {
        controlsTimer?.invalidate()
        if !controlsView.isHidden && showControls {
            controlsTimer = Timer.scheduledTimer(timeInterval: 3.0, target: self, selector: #selector(hideControls), userInfo: nil, repeats: false)
        }
    }

    private func trackInfo(forTrackIndex index: Int32, name: String, type: String) -> [String: Any] {
        print("[WakoCapacitorVideoPlayer] Getting track info for index: \(index), type: \(type), name: \(name)")
        
        if index == -1 {
            return [
                "id": "-1",
                "language": "",
                "label": "Disabled"
            ]
        }
        
        let languageCode = extractLanguageCode(from: name) ?? "unknown"
        return [
            "id": "\(index)",
            "language": languageCode,
            "label": name
        ]
    }
    
    private func extractLanguageCode(from string: String) -> String? {
        // Common language code patterns
        let patterns = [
            // ISO 639-1 two-letter codes
            "[^a-zA-Z](en|fr|es|de|it|pt|ru|ja|zh|ko|ar|hi|tr|nl)[^a-zA-Z]",
            // Full language names
            "(english|french|spanish|german|italian|portuguese|russian|japanese|chinese|korean|arabic|hindi|turkish|dutch)"
        ]
        
        for pattern in patterns {
            if let regex = try? NSRegularExpression(pattern: pattern, options: .caseInsensitive) {
                let nsString = string as NSString
                let range = NSRange(location: 0, length: nsString.length)
                
                if let match = regex.firstMatch(in: string, options: [], range: range) {
                    let matchedString = nsString.substring(with: match.range)
                    // Clean up the matched string (remove non-letter characters)
                    let cleaned = matchedString.lowercased().trimmingCharacters(in: CharacterSet.letters.inverted)
                    return cleaned
                }
            }
        }
        
        // No match found using patterns, try to extract based on common language codes in the string
        let lowerString = string.lowercased()
        let languageCodes = ["en", "fr", "es", "de", "it", "pt", "ru", "ja", "zh", "ko", "ar", "hi", "tr", "nl"]
        
        for code in languageCodes {
            if lowerString.contains(code) {
                return code
            }
        }
        
        // Extract first 2 letters if looks like a language code
        if string.count >= 2 && string.allSatisfy({ $0.isLetter }) {
            return String(string.prefix(2)).lowercased()
        }
        
        return nil
    }

    private func selectTracks() {
        print("[WakoCapacitorVideoPlayer] Selecting tracks with options:")
        print("- subtitleTrackId: \(String(describing: subtitleTrackId))")
        print("- subtitleLocale: \(String(describing: subtitleLocale))")
        print("- audioTrackId: \(String(describing: audioTrackId))")
        print("- audioLocale: \(String(describing: audioLocale))")
        print("- preferredLocale: \(String(describing: preferredLocale))")

        if let subtitleTrackId = subtitleTrackId {
            print("[WakoCapacitorVideoPlayer] Attempting to select subtitle track by ID: \(subtitleTrackId)")
            if subtitleTrackId == "off" || subtitleTrackId == "#disabled" {
                mediaPlayer.currentVideoSubTitleIndex = -1
                print("[WakoCapacitorVideoPlayer] Disabled subtitles")
            } else if let subtitleTracks = mediaPlayer.videoSubTitlesIndexes as? [NSNumber] {
                for (index, trackIndex) in subtitleTracks.enumerated() {
                    if let trackName = mediaPlayer.videoSubTitlesNames[index] as? String {
                        print("[WakoCapacitorVideoPlayer] Checking subtitle track: \(trackName)")
                        if trackName == subtitleTrackId || "\(trackIndex)" == subtitleTrackId {
                            mediaPlayer.currentVideoSubTitleIndex = trackIndex.int32Value
                            print("[WakoCapacitorVideoPlayer] Selected subtitle track: \(trackName)")
                            break
                        }
                    }
                }
            }
        } else if let subtitleLocale = subtitleLocale {
            print("[WakoCapacitorVideoPlayer] Attempting to select subtitle track by locale: \(subtitleLocale)")
            if let subtitleTracks = mediaPlayer.videoSubTitlesIndexes as? [NSNumber] {
                for (index, trackIndex) in subtitleTracks.enumerated() {
                    if let trackName = mediaPlayer.videoSubTitlesNames[index] as? String,
                       let languageCode = extractLanguageCode(from: trackName),
                       languageCode == subtitleLocale.lowercased() {
                        print("[WakoCapacitorVideoPlayer] Checking subtitle track: \(trackName)")
                        mediaPlayer.currentVideoSubTitleIndex = trackIndex.int32Value
                        print("[WakoCapacitorVideoPlayer] Selected subtitle track: \(trackName)")
                        break
                    }
                }
            }
        }

        if let audioTrackId = audioTrackId {
            print("[WakoCapacitorVideoPlayer] Attempting to select audio track by ID: \(audioTrackId)")
            if audioTrackId == "off" || audioTrackId == "#disabled" {
                mediaPlayer.currentAudioTrackIndex = -1
                print("[WakoCapacitorVideoPlayer] Disabled audio track")
            } else if let audioTracks = mediaPlayer.audioTrackIndexes as? [NSNumber] {
                print("[WakoCapacitorVideoPlayer] Available audio tracks: \(audioTracks.count)")
                for (index, trackIndex) in audioTracks.enumerated() {
                    if let trackName = mediaPlayer.audioTrackNames[index] as? String {
                        print("[WakoCapacitorVideoPlayer] Checking audio track: \(trackName)")
                        if trackName == audioTrackId || "\(trackIndex)" == audioTrackId {
                            mediaPlayer.currentAudioTrackIndex = trackIndex.int32Value
                            print("[WakoCapacitorVideoPlayer] Selected audio track: \(trackName)")
                            break
                        }
                    }
                }
            }
        } else if let audioLocale = audioLocale {
            print("[WakoCapacitorVideoPlayer] Attempting to select audio track by locale: \(audioLocale)")
            if let audioTracks = mediaPlayer.audioTrackIndexes as? [NSNumber] {
                for (index, trackIndex) in audioTracks.enumerated() {
                    if let trackName = mediaPlayer.audioTrackNames[index] as? String,
                       let languageCode = extractLanguageCode(from: trackName),
                       languageCode == audioLocale.lowercased() {
                        print("[WakoCapacitorVideoPlayer] Checking audio track: \(trackName)")
                        mediaPlayer.currentAudioTrackIndex = trackIndex.int32Value
                        print("[WakoCapacitorVideoPlayer] Selected audio track: \(trackName)")
                        break
                    }
                }
            }
        } else if let preferredLocale = preferredLocale {
            print("[WakoCapacitorVideoPlayer] Attempting to select audio track by preferred locale: \(preferredLocale)")
            if let audioTracks = mediaPlayer.audioTrackIndexes as? [NSNumber] {
                for (index, trackIndex) in audioTracks.enumerated() {
                    if let trackName = mediaPlayer.audioTrackNames[index] as? String,
                       let languageCode = extractLanguageCode(from: trackName),
                       languageCode == preferredLocale.lowercased() {
                        print("[WakoCapacitorVideoPlayer] Checking audio track: \(trackName)")
                        mediaPlayer.currentAudioTrackIndex = trackIndex.int32Value
                        print("[WakoCapacitorVideoPlayer] Selected audio track: \(trackName)")
                        break
                    }
                }
            }
        }
    }
    
    private func getCurrentAudioTrackInfo() -> [String: Any] {
        if mediaPlayer.currentAudioTrackIndex == -1 {
            return self.trackInfo(forTrackIndex: -1, name: "Disabled", type: "audio")
        }
        
        if let audioTracks = mediaPlayer.audioTrackIndexes as? [NSNumber],
           let audioNames = mediaPlayer.audioTrackNames as? [String] {
            let currentIndex = mediaPlayer.currentAudioTrackIndex
            
            for (index, trackIndex) in audioTracks.enumerated() where trackIndex.int32Value == currentIndex {
                if index < audioNames.count, let trackName = audioNames[index] as? String {
                    return self.trackInfo(forTrackIndex: currentIndex, name: trackName, type: "audio")
                }
            }
        }
        
        return self.trackInfo(forTrackIndex: mediaPlayer.currentAudioTrackIndex, name: "Unknown", type: "audio")
    }
    
    private func getCurrentSubtitleTrackInfo() -> [String: Any] {
        if mediaPlayer.currentVideoSubTitleIndex == -1 {
            return self.trackInfo(forTrackIndex: -1, name: "Disabled", type: "subtitle")
        }
        
        if let subtitleTracks = mediaPlayer.videoSubTitlesIndexes as? [NSNumber],
           let subtitleNames = mediaPlayer.videoSubTitlesNames as? [String] {
            let currentIndex = mediaPlayer.currentVideoSubTitleIndex
            
            for (index, trackIndex) in subtitleTracks.enumerated() where trackIndex.int32Value == currentIndex {
                if index < subtitleNames.count, let trackName = subtitleNames[index] as? String {
                    return self.trackInfo(forTrackIndex: currentIndex, name: trackName, type: "subtitle")
                }
            }
        }
        
        return self.trackInfo(forTrackIndex: mediaPlayer.currentVideoSubTitleIndex, name: "Unknown", type: "subtitle")
    }
    
    public func getStartAtSec() -> Double {
        return startAtSec
    }
    
    public func getExternalSubtitles() -> [[String: Any]]? {
        return externalSubtitles
    }

    private func addSubtitlesAfterPlayback() {
        guard let externalSubs = externalSubtitles, !externalSubs.isEmpty else {
            print("[WakoCapacitorVideoPlayer] No external subtitles to add")
            return
        }
        
        // First log any internal subtitle tracks that might be present before adding external ones
        if let subtitleTracks = mediaPlayer.videoSubTitlesIndexes as? [NSNumber],
           let subtitleNames = mediaPlayer.videoSubTitlesNames as? [String] {
            print("[WakoCapacitorVideoPlayer] Internal subtitle tracks before adding external ones: \(subtitleTracks.count)")
            for (i, track) in subtitleTracks.enumerated() {
                if i < subtitleNames.count {
                    print("[WakoCapacitorVideoPlayer] Internal Track \(i): ID=\(track), Name='\(subtitleNames[i])'")
                }
            }
        } else {
            print("[WakoCapacitorVideoPlayer] No internal subtitle tracks found before adding external ones")
        }
        
        // Track the number of successful additions
        var addedCount = 0
        var failedCount = 0
        
        print("[WakoCapacitorVideoPlayer] Adding \(externalSubs.count) external subtitles sequentially")
        
        // Add each subtitle with a delay to ensure all are processed
        for (index, subtitle) in externalSubs.enumerated() {
            if let urlString = subtitle["url"] as? String, !urlString.isEmpty,
               let url = URL(string: urlString) {
                
                // More substantial delay between subtitle additions to prevent conflicts
                // This is important as VLC sometimes needs time to process each subtitle
                let delay = 0.3 * Double(index)
                DispatchQueue.main.asyncAfter(deadline: .now() + delay) {
                    print("[WakoCapacitorVideoPlayer] Adding external subtitle \(index+1)/\(externalSubs.count): \(urlString)")
                    
                    // For remote URLs, add directly
                    let result = self.mediaPlayer.addPlaybackSlave(url, type: .subtitle, enforce: true)
                    let success = result != 0
                    
                    print("[WakoCapacitorVideoPlayer] External subtitle \(index+1) added: \(success ? "SUCCESS" : "FAILED") (result=\(result))")
                    
                    if success {
                        addedCount += 1
                        // Enable subtitle button immediately upon first success
                        DispatchQueue.main.async {
                            self.subtitleButton.isEnabled = true
                        }
                    } else {
                        failedCount += 1
                    }
                    
                    // After all subtitles processed, log final status
                    if index == externalSubs.count - 1 {
                        // Additional delay to ensure VLC has processed all additions
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.7) {
                            print("[WakoCapacitorVideoPlayer] Subtitle addition complete. Added \(addedCount) subtitles, failed \(failedCount) subtitles.")
                            
                            // Log final subtitle tracks
                            if let finalTracks = self.mediaPlayer.videoSubTitlesIndexes as? [NSNumber],
                               let finalNames = self.mediaPlayer.videoSubTitlesNames as? [String] {
                                print("[WakoCapacitorVideoPlayer] Final subtitle tracks count: \(finalTracks.count)")
                                for (i, track) in finalTracks.enumerated() {
                                    if i < finalNames.count {
                                        print("[WakoCapacitorVideoPlayer] Final Track \(i): ID=\(track), Name='\(finalNames[i])'")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                print("[WakoCapacitorVideoPlayer] Invalid subtitle URL at index \(index)")
                failedCount += 1
            }
        }
    }

    private func setupDoubleTapIndicators() {
        let rewindView = UIView()
        rewindView.isHidden = true
        rewindView.backgroundColor = UIColor.black.withAlphaComponent(0.5)
        rewindView.layer.cornerRadius = 40
        rewindView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(rewindView)
        
        NSLayoutConstraint.activate([
            rewindView.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            rewindView.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 50),
            rewindView.widthAnchor.constraint(equalToConstant: 80),
            rewindView.heightAnchor.constraint(equalToConstant: 80)
        ])
        
        let rewindImageView = UIImageView()
        rewindImageView.translatesAutoresizingMaskIntoConstraints = false
        rewindImageView.contentMode = .scaleAspectFit
        rewindImageView.tintColor = .white
        
        if #available(iOS 13.0, *) {
            let config = UIImage.SymbolConfiguration(pointSize: 24, weight: .bold)
            rewindImageView.image = UIImage(systemName: "gobackward.10", withConfiguration: config)
        } else {
            let label = UILabel()
            label.text = "-10"
            label.textColor = .white
            label.font = .boldSystemFont(ofSize: 24)
            label.translatesAutoresizingMaskIntoConstraints = false
            rewindView.addSubview(label)
            NSLayoutConstraint.activate([
                label.centerXAnchor.constraint(equalTo: rewindView.centerXAnchor),
                label.centerYAnchor.constraint(equalTo: rewindView.centerYAnchor)
            ])
        }
        
        if rewindImageView.image != nil {
            rewindView.addSubview(rewindImageView)
            NSLayoutConstraint.activate([
                rewindImageView.centerXAnchor.constraint(equalTo: rewindView.centerXAnchor),
                rewindImageView.centerYAnchor.constraint(equalTo: rewindView.centerYAnchor),
                rewindImageView.widthAnchor.constraint(equalToConstant: 40),
                rewindImageView.heightAnchor.constraint(equalToConstant: 40)
            ])
        }
        
        rewindIndicator = rewindView
        
        let forwardView = UIView()
        forwardView.isHidden = true
        forwardView.backgroundColor = UIColor.black.withAlphaComponent(0.5)
        forwardView.layer.cornerRadius = 40
        forwardView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(forwardView)
        
        NSLayoutConstraint.activate([
            forwardView.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            forwardView.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -50),
            forwardView.widthAnchor.constraint(equalToConstant: 80),
            forwardView.heightAnchor.constraint(equalToConstant: 80)
        ])
        
        let forwardImageView = UIImageView()
        forwardImageView.translatesAutoresizingMaskIntoConstraints = false
        forwardImageView.contentMode = .scaleAspectFit
        forwardImageView.tintColor = .white
        
        if #available(iOS 13.0, *) {
            let config = UIImage.SymbolConfiguration(pointSize: 24, weight: .bold)
            forwardImageView.image = UIImage(systemName: "goforward.10", withConfiguration: config)
        } else {
            let label = UILabel()
            label.text = "+10"
            label.textColor = .white
            label.font = .boldSystemFont(ofSize: 24)
            label.translatesAutoresizingMaskIntoConstraints = false
            forwardView.addSubview(label)
            NSLayoutConstraint.activate([
                label.centerXAnchor.constraint(equalTo: forwardView.centerXAnchor),
                label.centerYAnchor.constraint(equalTo: forwardView.centerYAnchor)
            ])
        }
        
        if forwardImageView.image != nil {
            forwardView.addSubview(forwardImageView)
            NSLayoutConstraint.activate([
                forwardImageView.centerXAnchor.constraint(equalTo: forwardView.centerXAnchor),
                forwardImageView.centerYAnchor.constraint(equalTo: forwardView.centerYAnchor),
                forwardImageView.widthAnchor.constraint(equalToConstant: 40),
                forwardImageView.heightAnchor.constraint(equalToConstant: 40)
            ])
        }
        
        forwardIndicator = forwardView
    }
    
    private func setupDoubleTapGestureRecognizers() {
        if let existingGestures = videoView.gestureRecognizers {
            for gesture in existingGestures {
                videoView.removeGestureRecognizer(gesture)
            }
        }
        
        let singleTap = UITapGestureRecognizer(target: self, action: #selector(toggleControlsVisibility))
        singleTap.numberOfTapsRequired = 1
        videoView.addGestureRecognizer(singleTap)
        
        let leftDoubleTap = UITapGestureRecognizer(target: self, action: #selector(handleDoubleTap(_:)))
        leftDoubleTap.numberOfTapsRequired = 2
        videoView.addGestureRecognizer(leftDoubleTap)
        
        singleTap.require(toFail: leftDoubleTap)
        
        videoView.isUserInteractionEnabled = true
    }
    
    @objc private func handleDoubleTap(_ gesture: UITapGestureRecognizer) {
        let tapLocation = gesture.location(in: videoView)
        let screenWidth = videoView.bounds.width
        
        if tapLocation.x < screenWidth / 2 {
            seek(direction: -1)
            showRewindIndicator()
        } else {
            seek(direction: 1)
            showForwardIndicator()
        }
    }
    
    private func seek(direction: Float) {
        let currentTimeValue = mediaPlayer.time.value
        guard let floatValue = currentTimeValue?.floatValue else { return }
        
        let currentTime = floatValue / 1000.0
        let newTime = currentTime + (direction * seekDuration)
        
        mediaPlayer.time = VLCTime(int: Int32(newTime * 1000))
    }
    
    private func showRewindIndicator() {
        rewindIndicator?.isHidden = false
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            self?.rewindIndicator?.isHidden = true
        }
    }
    
    private func showForwardIndicator() {
        forwardIndicator?.isHidden = false
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) { [weak self] in
            self?.forwardIndicator?.isHidden = true
        }
    }

    private func setupMediaPlayer() {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.mediaPlayer.drawable = self.videoView
            self.mediaPlayer.delegate = self
            self.updatePlayPauseButton()
            self.loadingIndicator.startAnimating()
        }
    }

    // Support for external keyboard events
    override var canBecomeFirstResponder: Bool {
        return true
    }
    
    override var keyCommands: [UIKeyCommand]? {
        return [
            // Fast forward & rewind
            UIKeyCommand(input: "\u{F703}", modifierFlags: [], action: #selector(handleRightArrow)),
            UIKeyCommand(input: "\u{F702}", modifierFlags: [], action: #selector(handleLeftArrow)),
            
            // Controls visibility
            UIKeyCommand(input: "\u{F700}", modifierFlags: [], action: #selector(handleShowController)),
            UIKeyCommand(input: "\u{F701}", modifierFlags: [], action: #selector(handleShowController)),
            UIKeyCommand(input: "\r", modifierFlags: [], action: #selector(handleShowController)), // Enter key
            
            // Play/Pause with space bar
            UIKeyCommand(input: " ", modifierFlags: [], action: #selector(togglePlayPause))
        ]
    }
    
    // Handle physical key presses with Responder Chain
    override func pressesBegan(_ presses: Set<UIPress>, with event: UIPressesEvent?) {
        var handled = false
        
        for press in presses {
            guard let key = press.key else { continue }
            
            print("[WakoCapacitorVideoPlayer] Key pressed: \(key.charactersIgnoringModifiers ?? "unknown")")
            
            switch key.keyCode {
            case .keyboardRightArrow:
                handleRightArrow()
                handled = true
                
            case .keyboardLeftArrow:
                handleLeftArrow()
                handled = true
                
            case .keyboardUpArrow, .keyboardDownArrow, .keyboardReturn:
                handleShowController()
                handled = true
                
            case .keyboardSpacebar:
                togglePlayPause()
                handled = true
                
            default:
                break
            }
        }
        
        if !handled {
            super.pressesBegan(presses, with: event)
        }
    }
    
    @objc private func handleRightArrow() {
        // Fast forward 10s
        let currentTimeValue = mediaPlayer.time.value
        guard let floatValue = currentTimeValue?.floatValue else { return }
        
        let currentTime = floatValue / 1000.0
        let newTime = currentTime + 10.0
        
        print("[WakoCapacitorVideoPlayer] Keyboard: fast forward 10s")
        mediaPlayer.time = VLCTime(int: Int32(newTime * 1000))
        showForwardIndicator()
    }
    
    @objc private func handleLeftArrow() {
        // Rewind 10s
        let currentTimeValue = mediaPlayer.time.value
        guard let floatValue = currentTimeValue?.floatValue else { return }
        
        let currentTime = floatValue / 1000.0
        let newTime = max(0, currentTime - 10.0)
        
        print("[WakoCapacitorVideoPlayer] Keyboard: rewind 10s")
        mediaPlayer.time = VLCTime(int: Int32(newTime * 1000))
        showRewindIndicator()
    }
    
    @objc private func handleShowController() {
        print("[WakoCapacitorVideoPlayer] Keyboard: show controls")
        if controlsView.isHidden {
            toggleControlsVisibility()
        }
    }
}

extension PlayerViewController: VLCMediaPlayerDelegate {
    func mediaPlayerTimeChanged(_ aNotification: Notification) {
        guard let media = mediaPlayer.media else { return }
        if let duration = media.length.value?.intValue, duration > 0 {
            DispatchQueue.main.async { [weak self] in
                guard let self = self else { return }
                self.seekBar.maximumValue = Float(duration) / 1000
                self.seekBar.value = Float(self.mediaPlayer.time.value?.intValue ?? 0) / 1000
                
                let currentTime = self.mediaPlayer.time.value?.intValue ?? 0
                let totalDuration = duration
                
                let showHours = totalDuration > 3600000
                
                if showHours {
                    let currentHours = currentTime / 3600000
                    let currentMinutes = (currentTime % 3600000) / 60000
                    let currentSeconds = (currentTime % 60000) / 1000
                    self.currentTimeLabel.text = String(format: "%02d:%02d:%02d", currentHours, currentMinutes, currentSeconds)
                    
                    let totalHours = totalDuration / 3600000
                    let totalMinutes = (totalDuration % 3600000) / 60000
                    let totalSeconds = (totalDuration % 60000) / 1000
                    self.totalTimeLabel.text = String(format: "%02d:%02d:%02d", totalHours, totalMinutes, totalSeconds)
                } else {
                    let currentMinutes = currentTime / 60000
                    let currentSeconds = (currentTime % 60000) / 1000
                    self.currentTimeLabel.text = String(format: "%02d:%02d", currentMinutes, currentSeconds)
                    
                    let totalMinutes = totalDuration / 60000
                    let totalSeconds = (totalDuration % 60000) / 1000
                    self.totalTimeLabel.text = String(format: "%02d:%02d", totalMinutes, totalSeconds)
                }
                
                if !self.hasStartedPlaying {
                    self.hasStartedPlaying = true
                    
                    self.checkAndEnableSubtitles()
                    
                    let currentTime = Double(self.mediaPlayer.time.value?.intValue ?? 0) / 1000
                    print("[WakoCapacitorVideoPlayer] Sending WakoPlayerReady with currentTime: \(currentTime)")
                    NotificationCenter.default.post(
                        name: NSNotification.Name("WakoPlayerReady"),
                        object: nil,
                        userInfo: ["currentTime": currentTime]
                    )
                }
            }
        }
    }
    
    private func checkAndEnableSubtitles() {
        if let subtitleTracks = self.mediaPlayer.videoSubTitlesIndexes as? [NSNumber], !subtitleTracks.isEmpty {
            DispatchQueue.main.async {
                self.subtitleButton.isEnabled = true
            }
            print("[WakoCapacitorVideoPlayer] Internal subtitles available (media loaded): \(subtitleTracks.count) tracks")
        } else {
            print("[WakoCapacitorVideoPlayer] No internal subtitles available in the media")
        }
    }

    func mediaPlayerStateChanged(_ aNotification: Notification) {
        print("[WakoCapacitorVideoPlayer] State changed to: \(self.mediaPlayer.state.rawValue)")
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.updatePlayPauseButton()
            
            switch self.mediaPlayer.state {
            case .buffering:
                self.loadingIndicator.startAnimating()
                
            case .opening:
                // Pas besoin d'ajuster la position ici, on utilise l'option start-time
                print("[WakoCapacitorVideoPlayer] Media opening")
                
            case .playing:
                self.loadingIndicator.stopAnimating()
                
                if !self.hasStartedPlaying {
                    print("[WakoCapacitorVideoPlayer] First playback started")
                    
                    // Marquer comme démarré
                    self.hasStartedPlaying = true
                    
                    // Ajouter les sous-titres externes une fois que la lecture a commencé
                    self.addSubtitlesAfterPlayback()
                    
                    // Vérifier et activer les sous-titres internes s'ils existent
                    self.checkAndEnableSubtitles()
                    
                    // Après une courte pause pour laisser tout se charger
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                        // Sélectionner les pistes préférées
                        self.selectTracks()
                        
                        // Notifier que le lecteur est prêt
                        let currentTime = Double(self.mediaPlayer.time.value?.intValue ?? 0) / 1000
                        print("[WakoCapacitorVideoPlayer] Sending WakoPlayerReady with currentTime: \(currentTime)")
                        NotificationCenter.default.post(
                            name: NSNotification.Name("WakoPlayerReady"),
                            object: nil,
                            userInfo: ["currentTime": currentTime]
                        )
                    }
                }
                
            case .paused:
                self.loadingIndicator.stopAnimating()
                
            case .error:
                self.loadingIndicator.stopAnimating()
                self.dismiss(animated: true)
                print("[WakoCapacitorVideoPlayer] Playback error occurred")
                
            case .ended:
                self.loadingIndicator.stopAnimating()
                if self.loopOnEnd {
                    self.mediaPlayer.time = VLCTime(int: 0)
                    self.mediaPlayer.play()
                } else if self.exitOnEnd {
                    self.dismiss(animated: true)
                }
                
            default:
                break
            }
            
            if self.mediaPlayer.isPlaying && self.loadingIndicator.isAnimating {
                print("[WakoCapacitorVideoPlayer] Forcing loading indicator to stop as video is playing")
                self.loadingIndicator.stopAnimating()
            }
        }
    }
}