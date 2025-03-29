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
            
            let player = VLCMediaPlayer()
            player.drawable = nil
           // player.audio?.volume = 100
            
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
        mediaPlayer.media = VLCMedia(url: mediaURL)
        
        DispatchQueue.main.async { [weak self] in
            guard let self = self else {
                completion(false)
                return
            }
            if let rootViewController = UIApplication.shared.windows.first?.rootViewController {
                playerViewController.modalPresentationStyle = .fullScreen
                rootViewController.present(playerViewController, animated: true) {
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
    private var hasStartedPlaying = false
    private let mediaPlayer: VLCMediaPlayer
    private let videoView = UIView()
    private let controlsView = UIView()
    private let titleLabel = UILabel()
    private let playPauseButton = UIButton(type: .system)
    private let forwardButton = UIButton(type: .system)
    private let backwardButton = UIButton(type: .system)
    private let closeButton = UIButton(type: .system)
    private let seekBar = UISlider()
    private let subtitleButton = UIButton(type: .system)
    private let audioTrackButton = UIButton(type: .system)
    private let loadingIndicator = UIActivityIndicatorView(style: .large)
    private let currentTimeLabel = UILabel()
    private let totalTimeLabel = UILabel()
    private let castButton = UIButton(type: .system)
    private var controlsTimer: Timer?
    
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
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        checkAirPlayAvailability()
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.mediaPlayer.drawable = self.videoView
            self.mediaPlayer.delegate = self
            self.updatePlayPauseButton()
            self.resetControlsTimer()
            self.loadingIndicator.startAnimating()
            
            self.controlsView.isHidden = !self.showControls
            self.closeButton.isHidden = !self.showControls
            self.titleLabel.isHidden = !self.showControls
            self.castButton.isHidden = !self.isAirPlayAvailable
            
            if self.startAtSec > 0 {
                print("[WakoCapacitorVideoPlayer] Setting initial time to: \(self.startAtSec)")
                self.mediaPlayer.time = VLCTime(int: Int32(self.startAtSec * 1000))
            }
        }
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        modalPresentationStyle = .fullScreen
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

        backwardButton.setImage(UIImage(systemName: "backward.fill"), for: .normal)
        backwardButton.tintColor = .white
        backwardButton.addTarget(self, action: #selector(jumpBackward), for: .touchUpInside)
        controlsView.addSubview(backwardButton)
        backwardButton.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            backwardButton.trailingAnchor.constraint(equalTo: playPauseButton.leadingAnchor, constant: -20),
            backwardButton.centerYAnchor.constraint(equalTo: controlsView.centerYAnchor),
            backwardButton.widthAnchor.constraint(equalToConstant: 50),
            backwardButton.heightAnchor.constraint(equalToConstant: 50)
        ])

        forwardButton.setImage(UIImage(systemName: "forward.fill"), for: .normal)
        forwardButton.tintColor = .white
        forwardButton.addTarget(self, action: #selector(jumpForward), for: .touchUpInside)
        controlsView.addSubview(forwardButton)
        forwardButton.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            forwardButton.leadingAnchor.constraint(equalTo: playPauseButton.trailingAnchor, constant: 20),
            forwardButton.centerYAnchor.constraint(equalTo: controlsView.centerYAnchor),
            forwardButton.widthAnchor.constraint(equalToConstant: 50),
            forwardButton.heightAnchor.constraint(equalToConstant: 50)
        ])

        subtitleButton.setImage(UIImage(systemName: "captions.bubble.fill"), for: .normal)
        subtitleButton.tintColor = .white
        subtitleButton.addTarget(self, action: #selector(showSubtitles), for: .touchUpInside)
        controlsView.addSubview(subtitleButton)
        subtitleButton.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            subtitleButton.leadingAnchor.constraint(equalTo: forwardButton.trailingAnchor, constant: 20),
            subtitleButton.centerYAnchor.constraint(equalTo: controlsView.centerYAnchor),
            subtitleButton.widthAnchor.constraint(equalToConstant: 50),
            subtitleButton.heightAnchor.constraint(equalToConstant: 50)
        ])

        audioTrackButton.setImage(UIImage(systemName: "speaker.wave.2.fill"), for: .normal)
        audioTrackButton.tintColor = .white
        audioTrackButton.addTarget(self, action: #selector(showAudioTracks), for: .touchUpInside)
        controlsView.addSubview(audioTrackButton)
        audioTrackButton.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            audioTrackButton.trailingAnchor.constraint(equalTo: backwardButton.leadingAnchor, constant: -20),
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
            castButton.leadingAnchor.constraint(equalTo: subtitleButton.trailingAnchor, constant: 20),
            castButton.centerYAnchor.constraint(equalTo: controlsView.centerYAnchor),
            castButton.widthAnchor.constraint(equalToConstant: 50),
            castButton.heightAnchor.constraint(equalToConstant: 50)
        ])

        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(toggleControlsVisibility))
        videoView.addGestureRecognizer(tapGesture)
        videoView.isUserInteractionEnabled = true

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

    @objc private func jumpBackward() {
        mediaPlayer.jumpBackward(10)
        resetControlsTimer()
    }

    @objc private func jumpForward() {
        mediaPlayer.jumpForward(10)
        resetControlsTimer()
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
              let subtitleNames = mediaPlayer.videoSubTitlesNames as? [String],
              subtitleTracks.count == subtitleNames.count else {
            print("[WakoCapacitorVideoPlayer] No subtitles available or mismatch in data")
            return
        }
        
        let alert = UIAlertController(title: "Select Subtitle", message: nil, preferredStyle: .actionSheet)
        
        // Vérifier si "Disable" existe déjà dans la liste
        var hasDisableOption = false
        for name in subtitleNames {
            if name.lowercased() == "disable" || name.lowercased() == "disabled" {
                hasDisableOption = true
                break
            }
        }
        
        // Ajouter l'option "Disable" au début si elle n'existe pas
        if !hasDisableOption {
            let disableAction = UIAlertAction(title: "Disable", style: .default) { _ in
                print("[WakoCapacitorVideoPlayer] Disabling subtitles")
                self.mediaPlayer.currentVideoSubTitleIndex = -1
                self.resetControlsTimer()
                let subtitleInfo = self.trackInfo(forTrackIndex: -1, name: "Disabled", type: "subtitle")
                NotificationCenter.default.post(
                    name: NSNotification.Name("WakoPlayerTracksChanged"),
                    object: nil,
                    userInfo: ["subtitleTrack": subtitleInfo]
                )
            }
            disableAction.setValue(mediaPlayer.currentVideoSubTitleIndex == -1, forKey: "checked")
            alert.addAction(disableAction)
        }
        
        for (index, trackIndex) in subtitleTracks.enumerated() {
            let trackName = subtitleNames[index]
            let action = UIAlertAction(title: trackName, style: .default) { _ in
                print("[WakoCapacitorVideoPlayer] Selecting subtitle track: \(trackName) at index: \(trackIndex)")
                self.mediaPlayer.currentVideoSubTitleIndex = trackIndex.int32Value
                self.resetControlsTimer()
                let subtitleInfo = self.trackInfo(forTrackIndex: trackIndex.int32Value, name: trackName, type: "subtitle")
                NotificationCenter.default.post(
                    name: NSNotification.Name("WakoPlayerTracksChanged"),
                    object: nil,
                    userInfo: ["subtitleTrack": subtitleInfo]
                )
            }
            action.setValue(mediaPlayer.currentVideoSubTitleIndex == trackIndex.int32Value, forKey: "checked")
            alert.addAction(action)
        }
        
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        present(alert, animated: true)
    }

    @objc private func showAudioTracks() {
        guard let audioTracks = mediaPlayer.audioTrackIndexes as? [NSNumber],
              let audioNames = mediaPlayer.audioTrackNames as? [String],
              audioTracks.count == audioNames.count else {
            print("[WakoCapacitorVideoPlayer] No audio tracks available or mismatch in data")
            return
        }
        
        let alert = UIAlertController(title: "Select Audio Track", message: nil, preferredStyle: .actionSheet)
        
        // Vérifier si "Disable" existe déjà dans la liste
        var hasDisableOption = false
        for name in audioNames {
            if name.lowercased() == "disable" || name.lowercased() == "disabled" {
                hasDisableOption = true
                break
            }
        }
        
        // Ajouter l'option "Disable" au début si elle n'existe pas
        if !hasDisableOption {
            let disableAction = UIAlertAction(title: "Disable", style: .default) { _ in
                print("[WakoCapacitorVideoPlayer] Disabling audio track")
                self.mediaPlayer.currentAudioTrackIndex = -1
                self.resetControlsTimer()
                let audioInfo = self.trackInfo(forTrackIndex: -1, name: "Disabled", type: "audio")
                NotificationCenter.default.post(
                    name: NSNotification.Name("WakoPlayerTracksChanged"),
                    object: nil,
                    userInfo: ["audioTrack": audioInfo]
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
                NotificationCenter.default.post(
                    name: NSNotification.Name("WakoPlayerTracksChanged"),
                    object: nil,
                    userInfo: ["audioTrack": audioInfo]
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
        controlsView.isHidden = !controlsView.isHidden
        closeButton.isHidden = !closeButton.isHidden
        titleLabel.isHidden = !titleLabel.isHidden
        castButton.isHidden = !isAirPlayAvailable || controlsView.isHidden
        resetControlsTimer()
    }
    
    @objc private func hideControls() {
        controlsView.isHidden = true
        closeButton.isHidden = true
        titleLabel.isHidden = true
        castButton.isHidden = true
    }

    private func resetControlsTimer() {
        controlsTimer?.invalidate()
        if !controlsView.isHidden {
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
    
    private func extractLanguageCode(from trackName: String) -> String? {
        // Dictionnaire pour convertir les noms de langue en codes ISO
        let languageMap: [String: String] = [
            "english": "en",
            "italian": "it",
            "french": "fr",
            "spanish": "es",
            "german": "de",
            "japanese": "ja",
            "korean": "ko",
            "chinese": "zh",
            "russian": "ru",
            "portuguese": "pt",
            "arabic": "ar",
            "dutch": "nl",
            "turkish": "tr",
            "hindi": "hi",
            "thai": "th"
        ]
        
        // Motifs pour extraire les codes ISO ou noms de langue
        let patterns = [
            "\\[([A-Za-z]+)\\]",            // Ex: [ENGLISH], [Italian]
            "\\(([a-z]{2,3})\\)",           // Ex: (en), (eng)
            "^([a-z]{2,3})\\s*-",           // Ex: en - English
            "\\b([a-z]{2,3})\\b"            // Ex: en isolé
        ]
        
        for pattern in patterns {
            if let match = trackName.range(of: pattern, options: .regularExpression) {
                let extracted = String(trackName[match]).trimmingCharacters(in: CharacterSet(charactersIn: "[]() -"))
                let code = extracted.lowercased()
                
                // Vérifier si c'est un code ISO direct
                if (2...3).contains(code.count) && code.allSatisfy({ $0.isLetter }) {
                    print("[WakoCapacitorVideoPlayer] Extracted language code: \(code) from \(trackName)")
                    return code
                }
                
                // Si c'est un nom de langue (ex. ENGLISH), convertir en code ISO
                if let languageCode = languageMap[code] {
                    print("[WakoCapacitorVideoPlayer] Mapped language code: \(languageCode) from \(trackName)")
                    return languageCode
                }
            }
        }
        
        print("[WakoCapacitorVideoPlayer] No valid ISO code found in: \(trackName)")
        return nil
    }

    private func selectTracks() {
        print("[WakoCapacitorVideoPlayer] Selecting tracks with options:")
        print("- subtitleTrackId: \(String(describing: subtitleTrackId))")
        print("- subtitleLocale: \(String(describing: subtitleLocale))")
        print("- audioTrackId: \(String(describing: audioTrackId))")
        print("- audioLocale: \(String(describing: audioLocale))")
        print("- preferredLocale: \(String(describing: preferredLocale))")

        // Sélection des sous-titres
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

        // Sélection des pistes audio
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
                let currentMinutes = currentTime / 60000
                let currentSeconds = (currentTime % 60000) / 1000
                self.currentTimeLabel.text = String(format: "%02d:%02d", currentMinutes, currentSeconds)
                
                let totalMinutes = duration / 60000
                let totalSeconds = (duration % 60000) / 1000
                self.totalTimeLabel.text = String(format: "%02d:%02d", totalMinutes, totalSeconds)
                
                if !self.hasStartedPlaying {
                    self.hasStartedPlaying = true
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

    func mediaPlayerStateChanged(_ aNotification: Notification) {
        print("[WakoCapacitorVideoPlayer] State changed to: \(self.mediaPlayer.state.rawValue)")
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.updatePlayPauseButton()
            
            switch self.mediaPlayer.state {
            case .buffering:
                self.loadingIndicator.startAnimating()
            case .playing:
                self.loadingIndicator.stopAnimating()
                // Vérifier les sous-titres et pistes audio avec un léger délai pour s'assurer qu'elles sont disponibles
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) { [weak self] in
                    guard let self = self else { return }
                    self.selectTracks()
                    if let subtitleTracks = self.mediaPlayer.videoSubTitlesIndexes as? [NSNumber], !subtitleTracks.isEmpty {
                        self.subtitleButton.isEnabled = true
                        print("[WakoCapacitorVideoPlayer] Subtitles available: \(subtitleTracks.count) tracks")
                    } else {
                        self.subtitleButton.isEnabled = false
                        print("[WakoCapacitorVideoPlayer] No subtitles available")
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
                if loopOnEnd {
                    mediaPlayer.time = VLCTime(int: 0)
                    mediaPlayer.play()
                } else if exitOnEnd {
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
