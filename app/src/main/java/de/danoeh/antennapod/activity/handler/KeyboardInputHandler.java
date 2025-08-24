package de.danoeh.antennapod.activity.handler;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import de.danoeh.antennapod.storage.importexport.AutomaticDatabaseExportWorker;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.appstartintent.MediaButtonStarter;
import org.greenrobot.eventbus.EventBus;

public class KeyboardInputHandler {
    private final MainActivity activity;

    public KeyboardInputHandler(MainActivity activity) {
        this.activity = activity;
    }

    public boolean handleKeyUp(int keyCode, KeyEvent event) {
        View currentFocus = activity.getCurrentFocus();
        if (currentFocus instanceof EditText) {
            return false;
        }

        AudioManager audioManager = (AudioManager) activity.getSystemService(android.content.Context.AUDIO_SERVICE);
        EventBus.getDefault().post(event);

        Integer customKeyCode = mapKeyCodeToMediaKey(keyCode);
        if (customKeyCode != null) {
            activity.sendBroadcast(MediaButtonStarter.createIntent(activity, customKeyCode));
            return true;
        }

        return handleVolumeKeys(keyCode, audioManager);
    }

    private Integer mapKeyCodeToMediaKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_P:
                return KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
            case KeyEvent.KEYCODE_J:
            case KeyEvent.KEYCODE_A:
            case KeyEvent.KEYCODE_COMMA:
                return KeyEvent.KEYCODE_MEDIA_REWIND;
            case KeyEvent.KEYCODE_K:
            case KeyEvent.KEYCODE_D:
            case KeyEvent.KEYCODE_PERIOD:
                return KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
            default:
                return null;
        }
    }

    private boolean handleVolumeKeys(int keyCode, AudioManager audioManager) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_PLUS:
            case KeyEvent.KEYCODE_W:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                return true;
            case KeyEvent.KEYCODE_MINUS:
            case KeyEvent.KEYCODE_S:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                return true;
            case KeyEvent.KEYCODE_M:
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI);
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    public static class StartupManager {
        private static final String PREF_NAME = "MainActivityPrefs";
        private static final String PREF_IS_FIRST_LAUNCH = "prefMainActivityIsFirstLaunch";

        private final MainActivity activity;

        public StartupManager(MainActivity activity) {
            this.activity = activity;
        }

        public void checkFirstLaunch() {
            SharedPreferences prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            if (prefs.getBoolean(PREF_IS_FIRST_LAUNCH, true)) {
                FeedUpdateManager.getInstance().restartUpdateAlarm(activity, true);
                UserPreferences.setBottomNavigationEnabled(true);

                SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean(PREF_IS_FIRST_LAUNCH, false);
                edit.apply();
            }
        }

        public void initializeAppServices() {
            FeedUpdateManager.getInstance().restartUpdateAlarm(activity, false);
            SynchronizationQueue.getInstance().syncIfNotSyncedRecently();
            AutomaticDatabaseExportWorker.enqueueIfNeeded(activity, false);
        }
    }
}