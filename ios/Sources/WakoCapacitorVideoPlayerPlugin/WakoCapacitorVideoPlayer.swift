import Foundation
import MobileVLCKit
import UIKit

public class WakoCapacitorVideoPlayer {
    private var mediaPlayer: VLCMediaPlayer?
    private var playerViewController: PlayerViewController?

    public func createPlayer(completion: @escaping (Bool) -> Void) {
        DispatchQueue.main.async { [weak self] in
            guard let self = self else {
                completion(false)
                return
            }
            
            // S'assurer que le mediaPlayer est créé sur le thread principal
            let player = VLCMediaPlayer()
            
            // Configuration initiale qui pourrait affecter la vue
            player.drawable = nil
            player.audio?.volume = 100
            
            self.mediaPlayer = player
            self.playerViewController = PlayerViewController(mediaPlayer: player)
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
    private let playPauseButton = UIButton(type: .system)
    private let forwardButton = UIButton(type: .system)
    private let backwardButton = UIButton(type: .system)
    private let closeButton = UIButton(type: .system)
    private let seekBar = UISlider()
    private let subtitleButton = UIButton(type: .system)
    private let audioTrackButton = UIButton(type: .system)
    private let loadingIndicator = UIActivityIndicatorView(style: .large)
    private var controlsTimer: Timer?

    init(mediaPlayer: VLCMediaPlayer) {
        self.mediaPlayer = mediaPlayer
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            self.mediaPlayer.drawable = self.videoView
            self.mediaPlayer.delegate = self
            self.updatePlayPauseButton()
            self.resetControlsTimer()
            self.loadingIndicator.startAnimating()
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

        loadingIndicator.color = .white
        view.addSubview(loadingIndicator)
        loadingIndicator.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            loadingIndicator.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            loadingIndicator.centerYAnchor.constraint(equalTo: view.centerYAnchor)
        ])

        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(toggleControlsVisibility))
        videoView.addGestureRecognizer(tapGesture)
        videoView.isUserInteractionEnabled = true
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

    @objc private func showSubtitles() {
        guard let subtitleTracks = mediaPlayer.videoSubTitlesIndexes as? [NSNumber] else { return }
        let alert = UIAlertController(title: "Select Subtitle", message: nil, preferredStyle: .actionSheet)
        for (index, trackIndex) in subtitleTracks.enumerated() {
            let trackName = mediaPlayer.videoSubTitlesNames[index] as? String ?? "Subtitle \(index + 1)"
            alert.addAction(UIAlertAction(title: trackName, style: .default) { _ in
                self.mediaPlayer.currentVideoSubTitleIndex = trackIndex.int32Value
                self.resetControlsTimer()
                let subtitleInfo = self.trackInfo(forTrackIndex: trackIndex.int32Value, type: "subtitle")
                NotificationCenter.default.post(
                    name: NSNotification.Name("WakoPlayerTracksChanged"),
                    object: nil,
                    userInfo: ["subtitleTrack": subtitleInfo]
                )
            })
        }
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        present(alert, animated: true)
    }

    @objc private func showAudioTracks() {
        guard let audioTracks = mediaPlayer.audioTrackIndexes as? [NSNumber] else { return }
        let alert = UIAlertController(title: "Select Audio Track", message: nil, preferredStyle: .actionSheet)
        for (index, trackIndex) in audioTracks.enumerated() {
            let trackName = mediaPlayer.audioTrackNames[index] as? String ?? "Audio \(index + 1)"
            alert.addAction(UIAlertAction(title: trackName, style: .default) { _ in
                self.mediaPlayer.currentAudioTrackIndex = trackIndex.int32Value
                self.resetControlsTimer()
                let audioInfo = self.trackInfo(forTrackIndex: trackIndex.int32Value, type: "audio")
                NotificationCenter.default.post(
                    name: NSNotification.Name("WakoPlayerTracksChanged"),
                    object: nil,
                    userInfo: ["audioTrack": audioInfo]
                )
            })
        }
        alert.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        present(alert, animated: true)
    }

    @objc private func toggleControlsVisibility() {
        controlsView.isHidden = !controlsView.isHidden
        closeButton.isHidden = !closeButton.isHidden
        resetControlsTimer()
    }

    private func resetControlsTimer() {
        controlsTimer?.invalidate()
        if !controlsView.isHidden {
            controlsTimer = Timer.scheduledTimer(timeInterval: 3.0, target: self, selector: #selector(hideControls), userInfo: nil, repeats: false)
        }
    }
    
    @objc private func hideControls() {
        controlsView.isHidden = true
        closeButton.isHidden = true
    }

    private func trackInfo(forTrackIndex index: Int32, type: String) -> [String: Any] {
        let names = type == "audio" ? mediaPlayer.audioTrackNames : mediaPlayer.videoSubTitlesNames
        let label = (names[Int(index)] as? String) ?? "\(type) \(index)"
        return [
            "id": "\(index)",
            "language": label,
            "label": label
        ]
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
                if self.mediaPlayer.isPlaying && self.loadingIndicator.isAnimating {
                    self.loadingIndicator.stopAnimating()
                }
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
        // Utiliser performSelector pour exécuter le code sur le thread principal
        self.performSelector(onMainThread: #selector(handleStateChange), with: nil, waitUntilDone: false)
    }
    
    @objc private func handleStateChange() {
        print("[WakoCapacitorVideoPlayer] State changed to: \(self.mediaPlayer.state.rawValue)")
        self.updatePlayPauseButton()
        switch self.mediaPlayer.state {
        case .buffering:
            self.loadingIndicator.startAnimating()
        case .playing:
            self.loadingIndicator.stopAnimating()
        case .error:
            self.loadingIndicator.stopAnimating()
            self.dismiss(animated: true)
            print("[WakoCapacitorVideoPlayer] Playback error occurred")
        case .ended:
            self.loadingIndicator.stopAnimating()
            self.dismiss(animated: true)
        default:
            break
        }
    }
}