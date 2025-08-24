package de.danoeh.antennapod.activity;

import android.content.Context;
import android.content.SharedPreferences;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.net.download.service.feed.FeedUpdateManagerImpl;
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import de.danoeh.antennapod.storage.importexport.AutomaticDatabaseExportWorker;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

public class StartupManager {
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