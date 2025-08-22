package de.test.antennapod.service.playback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.danoeh.antennapod.storage.preferences.SleepTimerPreferences;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

public class SleepTimerPreferencesTest {
    private Context context;
    private SharedPreferences defaultPrefs;
    private SharedPreferences sharedPrefs;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Initialize preference singletons
        UserPreferences.init(context);
        SleepTimerPreferences.init(context);

        sharedPrefs = context.getSharedPreferences("app_version", Context.MODE_PRIVATE);
        sharedPrefs.edit().clear().apply();
    }

    @After
    public void tearDown() throws Exception {
        context.getSharedPreferences(SleepTimerPreferences.PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply();
        sharedPrefs.edit().clear().apply();
    }

    @Test
    public void testIsInTimeRange() {
        assertTrue(SleepTimerPreferences.isInTimeRange(0, 10, 8));
        assertTrue(SleepTimerPreferences.isInTimeRange(1, 10, 8));
        assertTrue(SleepTimerPreferences.isInTimeRange(1, 10, 1));
        assertTrue(SleepTimerPreferences.isInTimeRange(20, 10, 8));
        assertTrue(SleepTimerPreferences.isInTimeRange(20, 20, 8));
        assertFalse(SleepTimerPreferences.isInTimeRange(1, 6, 8));
        assertFalse(SleepTimerPreferences.isInTimeRange(1, 6, 6));
        assertFalse(SleepTimerPreferences.isInTimeRange(20, 6, 8));
    }

    @Test
    public void testLastTimerValid() {
        SleepTimerPreferences.setLastTimer(37);

        assertEquals(37, SleepTimerPreferences.lastTimerValue());
    }

    @Test
    public void testLastTimerMillisValid() {
        SleepTimerPreferences.setLastTimer(37);

        // 37 minutes in milliseconds
        assertEquals(37 * 60 * 1000, SleepTimerPreferences.timerMillis());
    }
}
