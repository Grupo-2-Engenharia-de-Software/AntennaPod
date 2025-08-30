package de.danoeh.antennapod.playback.cast;

import androidx.appcompat.app.AppCompatActivity;

import android.view.Menu;

/**
 * Activity that allows for showing the MediaRouter button whenever there's a cast device in the
 * network.
 */
public abstract class CastEnabledActivity extends AppCompatActivity {
    public static final String TAG = "CastEnabledActivity";

    public final void requestCastButton(Menu menu) {
        // no-op
    }

    // O método setPlayerVisible agora é mais simples, focando apenas no player
    public abstract void setPlayerVisible(boolean visible);
}
