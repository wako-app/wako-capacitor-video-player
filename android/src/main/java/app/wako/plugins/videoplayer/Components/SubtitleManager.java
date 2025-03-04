package app.wako.plugins.videoplayer.Components;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelector;
import androidx.media3.ui.CaptionStyleCompat;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.PlayerControlView;

import app.wako.plugins.videoplayer.R;
import app.wako.plugins.videoplayer.Utilities.CustomDefaultTrackNameProvider;
import app.wako.plugins.videoplayer.Utilities.SubtitleUtils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

@UnstableApi
public class SubtitleManager {

    private final Context fragmentContext;
    private final View fragmentView;
    private final PlayerView playerView;
    private TrackSelector trackSelector;
    private ExoPlayer player;
    private String subtitleForegroundColor = "";
    private String subtitleBackgroundColor = "";
    private Integer subtitleFontSize = 16;
    public String preferredLocale;
    private CustomDefaultTrackNameProvider customDefaultTrackNameProvider;

    private static final String TAG = SubtitleManager.class.getName();

    public SubtitleManager(Context fragmentContext, View fragmentView, String subtitleForegroundColor, String subtitleBackgroundColor, Integer subtitleFontSize, String preferredLocale) {
        this.fragmentContext = fragmentContext;
        this.fragmentView = fragmentView;
        this.subtitleForegroundColor = subtitleForegroundColor;
        this.subtitleBackgroundColor = subtitleBackgroundColor;
        this.subtitleFontSize = subtitleFontSize;
        this.preferredLocale = preferredLocale;

        this.playerView = fragmentView.findViewById(R.id.videoViewId);


        this.customDefaultTrackNameProvider = new CustomDefaultTrackNameProvider(fragmentContext.getResources());
        try {

            // D'abord, accéder au contrôleur via réflexion
            Field controllerField = PlayerView.class.getDeclaredField("controller");
            controllerField.setAccessible(true);
            PlayerControlView controlView = (PlayerControlView) controllerField.get(playerView);

            // Puis définir le trackNameProvider
            final Field field = PlayerControlView.class.getDeclaredField("trackNameProvider");
            field.setAccessible(true);
            field.set(controlView, customDefaultTrackNameProvider);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Error setting custom track name provider: " + e.getMessage());
            e.printStackTrace();
        }


        ImageButton customSubtitleToggle = fragmentView.findViewById(R.id.custom_subtitle_toggle);

        if (customSubtitleToggle != null) {
            customSubtitleToggle.setVisibility(View.GONE);
        }

        updateCustomSubtitleButton();

    }

    public void setPlayer(ExoPlayer player) {
        this.player = player;
    }

    public void setTrackSelector(TrackSelector trackSelector) {
        this.trackSelector = trackSelector;
    }

    public void setSubtitleStyle() {
        int foreground = Color.WHITE;
        int background = Color.TRANSPARENT;
        if (subtitleForegroundColor.length() > 4 && subtitleForegroundColor.startsWith("rgba")) {
            foreground = SubtitleUtils.getColorFromRGBA(subtitleForegroundColor);
        }
        if (subtitleBackgroundColor.length() > 4 && subtitleBackgroundColor.startsWith("rgba")) {
            background = SubtitleUtils.getColorFromRGBA(subtitleBackgroundColor);
        }
        playerView
                .getSubtitleView()
                .setStyle(
                        new CaptionStyleCompat(foreground, background, Color.TRANSPARENT, CaptionStyleCompat.EDGE_TYPE_NONE, Color.WHITE, null)
                );
        playerView.getSubtitleView().setFixedTextSize(TypedValue.COMPLEX_UNIT_DIP, subtitleFontSize);

        if (player != null && trackSelector != null) {
            DefaultTrackSelector.Parameters parameters =
                    ((DefaultTrackSelector) trackSelector).getParameters().buildUpon().setSelectUndeterminedTextLanguage(true).build();
            trackSelector.setParameters(parameters);
        }

       // playerView.getSubtitleView().setVisibility(View.VISIBLE);

    }

    /**
     * Met à jour l'état du bouton personnalisé de sous-titres
     */
    public void updateCustomSubtitleButton() {
        // Vérifier si des pistes de sous-titres sont disponibles
        int totalSubtitles = 0;
        boolean hasSubtitleTracks = false;
        if (player != null) {
            Tracks tracks = player.getCurrentTracks();
            for (Tracks.Group trackGroup : tracks.getGroups()) {
                if (trackGroup.getType() == C.TRACK_TYPE_TEXT) {
                    totalSubtitles++;
                    hasSubtitleTracks = true;
                }
            }
        }

        this.playerView.setShowSubtitleButton(false);
        if(totalSubtitles > 1 || totalSubtitles == 0) {
            // Toujours utiliser le bouton natif d'Android
            this.playerView.setShowSubtitleButton(true);
            return;
        }
        // Mettre à jour notre bouton personnalisé uniquement si nécessaire
        ImageButton subtitleButton = fragmentView.findViewById(R.id.custom_subtitle_toggle);
        if (subtitleButton != null) {
            // Afficher le bouton uniquement si des sous-titres sont disponibles
            subtitleButton.setVisibility(hasSubtitleTracks ? View.VISIBLE : View.GONE);

            // Mettre à jour l'icône en fonction de l'état des sous-titres
            Format currentSubtitleTrack = getCurrentSubtitleTrack();
            if (currentSubtitleTrack != null) {
                subtitleButton.setImageResource(R.drawable.ic_subtitle_on);
            } else {
                subtitleButton.setImageResource(R.drawable.ic_subtitle_off);
            }

            // Configurer le menu contextuel pour afficher les pistes de sous-titres disponibles
            if (hasSubtitleTracks) {
                subtitleButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSubtitleTracksMenu(v);
                    }
                });
            }
        }
    }

    /**
     * Affiche un menu contextuel avec toutes les pistes de sous-titres disponibles
     *
     * @param anchorView Vue sur laquelle ancrer le menu
     */
    private void showSubtitleTracksMenu(View anchorView) {
        if (player == null || trackSelector == null) return;

        // Créer un menu contextuel (PopupMenu) avec un style personnalisé pour l'opacité
        Context wrapper = new ContextThemeWrapper(fragmentContext, R.style.PopupMenuStyle);
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(wrapper, anchorView);
        android.view.Menu menu = popup.getMenu();

        // Configurer l'affichage des icônes dans le menu
        setupPopupMenuIcons(menu);

        // Ajouter l'option pour désactiver les sous-titres
        android.view.MenuItem disableItem = menu.add(0, -1, 0, getTranslatedString("disable_subtitles", "Disable"));

        // Récupérer les pistes de sous-titres disponibles
        Tracks tracks = player.getCurrentTracks();
        int trackNumber = 0;

        // Récupérer la piste de sous-titres actuellement sélectionnée
        Format currentSubtitleFormat = getCurrentSubtitleTrack();

        // Si aucun sous-titre n'est sélectionné, marquer l'option "Désactiver" comme active
        if (currentSubtitleFormat == null) {
            disableItem.setChecked(true);
            disableItem.setIcon(R.drawable.ic_check);
        }

        // Parcourir toutes les pistes de sous-titres et les ajouter au menu
        for (Tracks.Group trackGroup : tracks.getGroups()) {
            if (trackGroup.getType() == C.TRACK_TYPE_TEXT) {
                TrackGroup group = trackGroup.getMediaTrackGroup();
                for (int i = 0; i < group.length; i++) {
                    Format format = group.getFormat(i);

                    // Toujours commencer par le nom de la langue
                    String trackName = customDefaultTrackNameProvider.getTrackName(format);

                    // Ajouter la piste au menu
                    android.view.MenuItem item = menu.add(0, trackNumber, i + 1, trackName);

                    // Marquer la piste active avec une coche
                    boolean isSelected = false;
                    if (currentSubtitleFormat != null) {
                        // Vérifier par ID si disponible
                        if (format.id != null && currentSubtitleFormat.id != null &&
                                format.id.equals(currentSubtitleFormat.id)) {
                            isSelected = true;
                        }
                    }


                    if (isSelected) {
                        item.setChecked(true);
                        item.setIcon(R.drawable.ic_check);
                    }

                    trackNumber++;
                }
            }
        }

        // Configurer le playerListener pour gérer les sélections
        popup.setOnMenuItemClickListener(new androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(android.view.MenuItem item) {
                int id = item.getItemId();

                if (id == -1) {
                    // Désactiver les sous-titres
                    enableSubtitles(false);
                    return true;
                }

                int trackNumber = 0;
                for (Tracks.Group trackGroup : tracks.getGroups()) {
                    if (trackGroup.getType() == C.TRACK_TYPE_TEXT) {
                        TrackGroup group = trackGroup.getMediaTrackGroup();
                        for (int i = 0; i < group.length; i++) {
                            Format format = group.getFormat(i);

                            if (trackNumber == id) {
                                selectSubtitleTrack(format.id);

                                return true;
                            }
                            trackNumber++;
                        }
                    }
                }


                return false;

            }
        });

        // Afficher le menu
        popup.show();
    }


    /**
     * Récupère une chaîne traduite
     * Utilise d'abord les chaînes du système Android si disponibles
     *
     * @param key          Identifiant de la chaîne
     * @param defaultValue Valeur par défaut si la traduction n'est pas disponible
     * @return Chaîne traduite
     */
    private String getTranslatedString(String key, String defaultValue) {
        // Cas spécifiques pour les chaînes système courantes
        if (key.equals("disable_subtitles")) {
            return fragmentContext.getResources().getString(android.R.string.no);
        } else if (key.equals("track")) {
            return fragmentContext.getResources().getString(android.R.string.untitled);
        } else if (key.equals("unknown_language")) {
            return "Langue inconnue"; // Pas de chaîne système équivalente
        } else if (key.equals("subtitles_on")) {
            return "Sous-titres activés"; // Pas de chaîne système équivalente
        } else if (key.equals("subtitles_off")) {
            return "Sous-titres désactivés"; // Pas de chaîne système équivalente
        }

        return defaultValue;
    }


    /**
     * Récupère la piste de sous-titres actuellement sélectionnée
     *
     * @return Format de la piste de sous-titres active, ou null si aucune n'est active
     */
    private Format getCurrentSubtitleTrack() {
        if (player == null) return null;

        // Vérifier d'abord si le renderer de texte est désactivé
        if (trackSelector instanceof DefaultTrackSelector defaultTrackSelector) {
            DefaultTrackSelector.Parameters parameters = defaultTrackSelector.getParameters();
            if (parameters.getRendererDisabled(getTextRendererIndex())) {
                return null; // Le renderer est désactivé, donc aucun sous-titre n'est sélectionné
            }
        }

        Tracks tracks = player.getCurrentTracks();
        for (Tracks.Group trackGroup : tracks.getGroups()) {
            if (trackGroup.getType() == C.TRACK_TYPE_TEXT && trackGroup.isSelected()) {
                TrackGroup group = trackGroup.getMediaTrackGroup();
                for (int i = 0; i < group.length; i++) {
                    if (trackGroup.isTrackSelected(i)) {
                        return group.getFormat(i);
                    }
                }
            }
        }
        return null;
    }


    /**
     * Sélectionne une piste de sous-titres spécifique
     *
     * @param trackId ID de la piste à sélectionner
     */
    private void selectSubtitleTrack(String trackId) {
        if (player == null || trackSelector == null) return;

        // Activer les sous-titres d'abord
        enableSubtitles(true);

        if (trackSelector instanceof DefaultTrackSelector defaultTrackSelector) {
            DefaultTrackSelector.Parameters.Builder parametersBuilder =
                    defaultTrackSelector.getParameters().buildUpon();

            // Réinitialiser d'abord toute sélection précédente
            parametersBuilder
                    .clearSelectionOverrides()
                    .setRendererDisabled(C.TRACK_TYPE_TEXT, false);

            // Trouver l'index du renderer de texte
            int textRendererIndex = getTextRendererIndex();
            if (textRendererIndex == -1) {
                Log.e(TAG, "No text renderer found");
                return;
            }

            try {
                // Obtenir les informations de piste mappées pour le lecteur
                androidx.media3.exoplayer.trackselection.MappingTrackSelector.MappedTrackInfo mappedTrackInfo =
                        defaultTrackSelector.getCurrentMappedTrackInfo();

                if (mappedTrackInfo == null) {
                    Log.e(TAG, "No mapped track info available");
                    return;
                }

                // Obtenir les groupes de pistes pour le renderer de texte
                TrackGroupArray textTrackGroups = mappedTrackInfo.getTrackGroups(textRendererIndex);

                // Chercher la piste avec l'ID spécifié
                for (int groupIndex = 0; groupIndex < textTrackGroups.length; groupIndex++) {
                    TrackGroup group = textTrackGroups.get(groupIndex);

                    for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                        Format format = group.getFormat(trackIndex);

                        if (Objects.equals(format.id, trackId)) {
                            Log.d(TAG, "Found subtitle track with ID: " + trackId);

                            // Créer un override pour sélectionner uniquement cette piste
                            int[] selectedTracks = new int[]{trackIndex};
                            DefaultTrackSelector.SelectionOverride override =
                                    new DefaultTrackSelector.SelectionOverride(groupIndex, selectedTracks);

                            // Appliquer l'override
                            parametersBuilder.setSelectionOverride(textRendererIndex, textTrackGroups, override);
                            defaultTrackSelector.setParameters(parametersBuilder.build());

                            // Forcer la mise à jour de l'UI
                            playerView.invalidate();
                            this.updateCustomSubtitleButton();

                            return;
                        }
                    }
                }

                Log.w(TAG, "No subtitle track found with ID: " + trackId);
            } catch (Exception e) {
                Log.e(TAG, "Error selecting subtitle track: " + e.getMessage());
            }
        }
    }

    /**
     * Enable or disable subtitles
     *
     * @param enabled true to enable subtitles, false to disable
     */
    public void enableSubtitles(boolean enabled) {
        if (trackSelector == null || player == null) return;
        
        if (trackSelector instanceof DefaultTrackSelector defaultTrackSelector) {
            DefaultTrackSelector.Parameters.Builder parametersBuilder =
                    defaultTrackSelector.getParameters().buildUpon();

            if (enabled) {
                // Enable subtitles
                parametersBuilder
                    .setRendererDisabled(C.TRACK_TYPE_TEXT, false)
                    .setDisabledTextTrackSelectionFlags(0)
                    .setSelectUndeterminedTextLanguage(true)
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT);
                
                if (playerView != null && playerView.getSubtitleView() != null) {
                    playerView.getSubtitleView().setVisibility(View.VISIBLE);
                }
            } else {
                // Disable subtitles by selecting no tracks
                int textRendererIndex = getTextRendererIndex();
                if (textRendererIndex != -1) {
                    try {
                        // Get the mapped track info
                        androidx.media3.exoplayer.trackselection.MappingTrackSelector.MappedTrackInfo mappedTrackInfo =
                                defaultTrackSelector.getCurrentMappedTrackInfo();

                        if (mappedTrackInfo != null) {
                            // Get the text track groups
                            TrackGroupArray textTrackGroups = mappedTrackInfo.getTrackGroups(textRendererIndex);
                            
                            // Create an empty selection override to select no tracks
                            DefaultTrackSelector.SelectionOverride override =
                                    new DefaultTrackSelector.SelectionOverride(0, new int[0]);
                            
                            // Apply the override
                            parametersBuilder.setSelectionOverride(textRendererIndex, textTrackGroups, override);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error disabling subtitles: " + e.getMessage());
                    }
                }

                // Hide subtitle view
                if (playerView != null && playerView.getSubtitleView() != null) {
                    playerView.getSubtitleView().setVisibility(View.GONE);
                }
            }

            // Apply parameters
            defaultTrackSelector.setParameters(parametersBuilder.build());
            
            // Force UI update
            if (playerView != null) {
                playerView.invalidate();
            }
            
            // Update button state
            updateCustomSubtitleButton();
            
            Log.d(TAG, "Subtitles " + (enabled ? "enabled" : "disabled"));
        }
    }




    /**
     * Configure l'affichage des icônes dans le menu popup
     *
     * @param menu Menu à configurer
     */
    private void setupPopupMenuIcons(android.view.Menu menu) {
        try {
            // Utiliser la réflexion pour accéder à la méthode setOptionalIconsVisible
            Class<?> menuClass = Class.forName("androidx.appcompat.view.menu.MenuBuilder");
            if (menu.getClass().equals(menuClass)) {
                java.lang.reflect.Method setOptionalIconsVisible =
                        menuClass.getDeclaredMethod("setOptionalIconsVisible", boolean.class);
                setOptionalIconsVisible.setAccessible(true);
                setOptionalIconsVisible.invoke(menu, true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting popup menu icons visible: " + e.getMessage());
        }
    }



    /**
     * Récupère l'index du renderer de texte/sous-titres
     *
     * @return l'index du renderer de texte, ou -1 si non trouvé
     */
    private int getTextRendererIndex() {
        if (player == null) return -1;

        for (int i = 0; i < player.getRendererCount(); i++) {
            if (player.getRendererType(i) == C.TRACK_TYPE_TEXT) {
                return i;
            }
        }
        return -1;
    }

    private static String getSubtitleLanguage(Uri uri) {
        final String path = uri.getPath().toLowerCase();

        if (path.endsWith(".srt")) {
            int last = path.lastIndexOf(".");
            int prev = last;

            for (int i = last; i >= 0; i--) {
                prev = path.indexOf(".", i);
                if (prev != last)
                    break;
            }

            int len = last - prev;

            if (len >= 2 && len <= 6) {
                // TODO: Validate lang
                return path.substring(prev + 1, last);
            }
        }

        return null;
    }


    /**
     * Récupère tous les groupes de pistes de sous-titres
     *
     * @return Tableau des groupes de pistes de texte
     */
    private Tracks.Group[] getTextTrackGroups() {
        if (player == null) return new Tracks.Group[0];

        Tracks tracks = player.getCurrentTracks();
        List<Tracks.Group> textGroups = new java.util.ArrayList<>();

        for (Tracks.Group group : tracks.getGroups()) {
            if (group.getType() == C.TRACK_TYPE_TEXT) {
                textGroups.add(group);
            }
        }

        return textGroups.toArray(new Tracks.Group[0]);
    }

    /**
     * Obtient les TrackGroupArray pour un renderer spécifique
     *
     * @param rendererIndex L'index du renderer
     * @return Le TrackGroupArray pour ce renderer, ou null si non disponible
     */
    private TrackGroupArray getRendererTrackGroups(int rendererIndex) {
        if (player == null || trackSelector == null ||
                rendererIndex < 0 || rendererIndex >= player.getRendererCount()) {
            return null;
        }

        try {
            // Obtenir les informations de piste mappées via le sélecteur de pistes
            if (trackSelector instanceof DefaultTrackSelector) {
                DefaultTrackSelector defaultTrackSelector = (DefaultTrackSelector) trackSelector;
                androidx.media3.exoplayer.trackselection.MappingTrackSelector.MappedTrackInfo mappedTrackInfo =
                        defaultTrackSelector.getCurrentMappedTrackInfo();

                if (mappedTrackInfo != null) {
                    return mappedTrackInfo.getTrackGroups(rendererIndex);
                }
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting renderer track groups: " + e.getMessage());
            return null;
        }
    }

    /**
     * Convertit un TrackGroup en TrackGroupArray pour utilisation avec setSelectionOverride
     *
     * @param group Le groupe de pistes à convertir
     * @return Un TrackGroupArray contenant le groupe spécifié
     */
    private TrackGroupArray getTrackGroupArrayFromGroup(TrackGroup group) {
        if (group == null) return null;

        // Créer un nouveau TrackGroupArray contenant uniquement ce groupe
        return new TrackGroupArray(group);
    }
}
