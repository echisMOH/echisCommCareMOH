package org.commcare.views.media;

import android.media.MediaPlayer;
import android.util.Log;

/**
 * Audio playback is delegated through this singleton class since only one
 * track should play at a time. Audio buttons invoke this controller on button
 * presses. When activities that are playing audio are paused or unloaded they
 * pause or release the audio through this controller.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public enum AudioController {
    /**
     * Singleton instance
     */
    INSTANCE;

    private static final String TAG = AudioController.class.getSimpleName();

    /**
     * Only one audio entity should be playing at once, this is that entity.
     */
    private MediaEntity currentEntity;

    /**
     * Button that corresponds to the currentEntity media. Pressing the button
     * should trigger playback control methods here.
     */
    private AudioPlaybackButtonBase currentAudioButton;

    /**
     * Set the media to be played and store the playback button attached to
     * that media, enableing button display state to mirror playback state.
     *
     * @param newMedia       New media to be controlled
     * @param newAudioButton Button that corresponds to the new media, needed so
     *                       we can update the button's display state to mirror
     *                       the media's playback state
     */
    public void setCurrentMediaAndButton(MediaEntity newMedia,
                                         final AudioPlaybackButtonBase newAudioButton) {
        if (currentAudioButton != null && currentAudioButton != newAudioButton) {
            // reset the old button to not be playing
            currentAudioButton.resetPlaybackState();
        }
        currentAudioButton = newAudioButton;

        if (newMedia != currentEntity) {
            // newMedia is actually new, so release old media
            releaseCurrentMediaEntity();
            currentEntity = newMedia;
        }
        registerPlaybackFinishedCallback();
    }

    private void registerPlaybackFinishedCallback() {
        currentEntity.getPlayer().setOnCompletionListener(mediaPlayer -> releaseCurrentMediaEntity());
    }

    /**
     * When a view and its buttons are re-created, we need to re-register the
     * button that corresponds with the playing media.
     *
     * @param button Corresponds with the media that is currently
     *               loaded/playing
     */
    public void registerPlaybackButton(AudioPlaybackButtonBase button) {
        currentAudioButton = button;
        registerPlaybackFinishedCallback();
    }

    /**
     * Release current media resources.
     */
    public void releaseCurrentMediaEntity() {
        if (currentAudioButton != null) {
            currentAudioButton.endPlaying();
        }
        if (currentEntity != null) {
            MediaPlayer mp = currentEntity.getPlayer();
            mp.reset();
            mp.release();
        }
        currentEntity = null;
    }

    /**
     * Start audio playback of current media resource.
     */
    public void playCurrentMediaEntity() {
        if (currentEntity != null) {
            MediaPlayer player = currentEntity.getPlayer();
            player.start();
            currentEntity.setState(MediaState.Playing);
            currentAudioButton.startProgressBar(player.getCurrentPosition(), player.getDuration());
        }
    }

    /**
     * Pause playback of current media resource.
     */
    public void pauseCurrentMediaEntity() {
        if (currentEntity != null) {
            if (currentEntity.getState().equals(MediaState.Playing)) {
                MediaPlayer mp = currentEntity.getPlayer();
                mp.pause();
                currentEntity.setState(MediaState.Paused);
            }
        }
    }

    /**
     * Pauses playback when the controlling activity gets put in the
     * background. The pause is specially marked such that when the activity
     * resumes and calls playPreviousAudio, playback will resume.
     */
    public void systemInducedPause() {
        if (currentEntity != null) {
            boolean unpauseOnResume =
                    MediaState.Playing.equals(currentEntity.getState());

            pauseCurrentMediaEntity();

            if (unpauseOnResume) {
                currentEntity.setState(MediaState.PausedForRenewal);
            }
        }
    }

    /**
     * Play media that was paused due to a system interruption
     */
    public void playPreviousAudio() {
        if (currentEntity != null) {
            switch (currentEntity.getState()) {
                case PausedForRenewal:
                    currentAudioButton.startPlaying();
                    break;
                case Paused:
                    break;
                case Playing:
                case Ready:
                    Log.w(TAG, "State in loadPreviousAudio is invalid");
            }
        }
    }

    boolean isMediaLoaded() {
        return currentEntity != null;
    }

    boolean doesCurrentMediaCorrespondToButton(AudioPlaybackButtonBase audioButton) {
        return currentEntity != null && currentAudioButton == audioButton;
    }

    void seekTo(int pos) {
        if (currentEntity != null) {
            currentEntity.getPlayer().seekTo(pos);
        }
    }

    int getCurrentPosition() {
        if (currentEntity == null) {
            return -1;
        } else {
            return currentEntity.getPlayer().getCurrentPosition();
        }
    }

    int getDuration() {
        if (currentEntity == null) {
            return -1;
        } else {
            return currentEntity.getPlayer().getDuration();
        }
    }

    ViewId getMediaViewId() {
        return currentEntity.getId();
    }

    String getMediaUri() {
        return currentEntity.getSource();
    }

    MediaState getMediaState() {
        return currentEntity.getState();
    }
}
