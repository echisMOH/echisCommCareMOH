package org.commcare.views.media;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.widget.ProgressBar;

import org.commcare.dalvik.R;

/**
 * Small audio playback button that shows progress via a circular progress bar
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class AudioPlaybackButton extends AudioPlaybackButtonBase {

    private ProgressBar progressBar;
    private ObjectAnimator animation;

    /**
     * Used by media inflater.
     */
    public AudioPlaybackButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @param URI     audio to load when play button pressed
     * @param viewId  Id for the ListAdapter view that contains this button
     * @param visible Should the button be visible?
     */
    public AudioPlaybackButton(Context context, String URI,
                               ViewId viewId, boolean visible) {
        super(context, URI, viewId, visible);
    }

    @Override
    protected void setupView(Context context) {
        super.setupView(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            progressBar = findViewById(R.id.circular_progress_bar);
            final int startPosition = 0;
            final int progressBarMax = 500;
            animation = ObjectAnimator.ofInt(progressBar, "progress", startPosition, progressBarMax);
        }
    }

    @Override
    protected int getLayout() {
        return R.layout.small_audio_playback;
    }

    @Override
    protected void startProgressBar(int milliPosition, int milliDuration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            resetProgressBar();
            animation.setDuration(milliDuration);
            animation.setCurrentPlayTime(milliPosition);
            animation.setInterpolator(new LinearInterpolator());
            animation.start();
        }
    }

    @Override
    protected void resetProgressBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            animation.removeAllListeners();
            animation.end();
            animation.cancel();
            progressBar.clearAnimation();
            progressBar.setProgress(0);
        }
    }

    @Override
    protected void pauseProgressBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            animation.cancel();
        }
    }
}
