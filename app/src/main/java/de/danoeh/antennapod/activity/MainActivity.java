package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.EpisodeDownloadEvent;
import de.danoeh.antennapod.event.FeedUpdateRunningEvent;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.model.download.DownloadStatus;
import de.danoeh.antennapod.net.download.service.feed.FeedUpdateManagerImpl;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager;
import de.danoeh.antennapod.net.sync.serviceinterface.SynchronizationQueue;
import de.danoeh.antennapod.playback.cast.CastEnabledActivity;
import de.danoeh.antennapod.playback.service.PlaybackServiceInterface;
import de.danoeh.antennapod.storage.importexport.AutomaticDatabaseExportWorker;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.TransitionEffect;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.appstartintent.MediaButtonStarter;
import de.danoeh.antennapod.ui.common.IntentUtils;
import de.danoeh.antennapod.ui.common.ThemeSwitcher;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.ui.discovery.DiscoveryFragment;
import de.danoeh.antennapod.ui.screen.AddFeedFragment;
import de.danoeh.antennapod.ui.screen.AllEpisodesFragment;
import de.danoeh.antennapod.ui.screen.InboxFragment;
import de.danoeh.antennapod.ui.screen.PlaybackHistoryFragment;
import de.danoeh.antennapod.ui.screen.SearchFragment;
import de.danoeh.antennapod.ui.screen.download.CompletedDownloadsFragment;
import de.danoeh.antennapod.ui.screen.download.DownloadLogFragment;
import de.danoeh.antennapod.ui.screen.drawer.BottomNavigation;
import de.danoeh.antennapod.ui.screen.drawer.NavDrawerFragment;
import de.danoeh.antennapod.ui.screen.drawer.NavigationNames;
import de.danoeh.antennapod.ui.screen.feed.FeedItemlistFragment;
import de.danoeh.antennapod.ui.screen.home.HomeFragment;
import de.danoeh.antennapod.ui.screen.playback.audio.AudioPlayerFragment;
import de.danoeh.antennapod.ui.screen.preferences.PreferenceActivity;
import de.danoeh.antennapod.ui.screen.queue.QueueFragment;
import de.danoeh.antennapod.ui.screen.rating.RatingDialogManager;
import de.danoeh.antennapod.ui.screen.subscriptions.SubscriptionFragment;
import de.danoeh.antennapod.ui.view.BottomSheetBackPressedCallback;
import de.danoeh.antennapod.ui.view.LockableBottomSheetBehavior;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.HashMap;
import java.util.Map;

/**
 * The activity that is shown when the user launches the app.
 */
public class MainActivity extends CastEnabledActivity {

    private static final String TAG = "MainActivity";
    public static final String MAIN_FRAGMENT_TAG = "main";

    public static final String PREF_NAME = "MainActivityPrefs";
    public static final String PREF_IS_FIRST_LAUNCH = "prefMainActivityIsFirstLaunch";

    public static final String EXTRA_REFRESH_ON_START = "refresh_on_start";
    public static final String KEY_GENERATED_VIEW_ID = "generated_view_id";

    private @Nullable DrawerLayout drawerLayout;
    private @Nullable ActionBarDrawerToggle drawerToggle;
    private BottomNavigation bottomNavigation;
    private View navDrawer;
    private LockableBottomSheetBehavior<View> sheetBehavior;
    private BottomSheetBackPressedCallback bottomSheetBackPressedCallback;
    private OnBackPressedCallback openDefaultPageBackPressedCallback;
    private RecyclerView.RecycledViewPool recycledViewPool = new RecyclerView.RecycledViewPool();
    private int lastTheme = 0;
    private Insets systemBarInsets = Insets.NONE;

    // Managers
    private DrawerManager drawerManager;
    private PlayerStateManager playerStateManager;
    private NavigationManager navigationManager;
    private StartupManager startupManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        lastTheme = ThemeSwitcher.getNoTitleTheme(this);
        setTheme(lastTheme);
        if (savedInstanceState != null) {
            ensureGeneratedViewIdGreaterThan(savedInstanceState.getInt(KEY_GENERATED_VIEW_ID, 0));
        }
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        recycledViewPool.setMaxRecycledViews(R.id.view_type_episode_item, 25);

        // Initialize managers
        drawerLayout = findViewById(R.id.drawer_layout);
        navDrawer = findViewById(R.id.navDrawerFragment);
        drawerManager = new DrawerManager(this, drawerLayout, navDrawer);
        playerStateManager = new PlayerStateManager(this);
        navigationManager = new NavigationManager(this);
        startupManager = new StartupManager(this);

        startupManager.checkFirstLaunch();

        drawerManager.setupDrawer();

        bottomNavigation = new BottomNavigation(findViewById(R.id.bottomNavigationView)) {
            @Override
            public void onItemSelected(@IdRes int itemId) {
                sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                if (itemId == R.id.bottom_navigation_settings) {
                    startActivity(new Intent(MainActivity.this, PreferenceActivity.class));
                    return;
                }
                navigationManager.loadFragment(NavigationNames.getBottomNavigationFragmentTag(itemId), null);
            }
        };
        if (UserPreferences.isBottomNavigationEnabled()) {
            bottomNavigation.buildMenu();
            if (drawerLayout == null) { // Tablet mode
                navDrawer.setVisibility(View.GONE);
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }
            drawerLayout = null;
        } else {
            bottomNavigation.hide();
            bottomNavigation = null;
        }
        openDefaultPageBackPressedCallback = new OpenDefaultPageBackPressedCallback();

        // Consume navigation bar insets - we apply them in setPlayerVisible()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_view), (v, insets) -> {
            systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            playerStateManager.setSystemBarInsets(systemBarInsets);
            return new WindowInsetsCompat.Builder(insets)
                    .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.NONE)
                    .build();
        });

        final FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(MAIN_FRAGMENT_TAG) == null) {
            if (!UserPreferences.DEFAULT_PAGE_REMEMBER.equals(UserPreferences.getDefaultPage())) {
                navigationManager.loadFragment(UserPreferences.getDefaultPage(), null);
            } else {
                String lastFragment = NavDrawerFragment.getLastNavFragment(this);
                if (ArrayUtils.contains(getResources().getStringArray(R.array.nav_drawer_section_tags), lastFragment)) {
                    navigationManager.loadFragment(lastFragment, null);
                } else {
                    try {
                        navigationManager.loadFeedFragmentById(Integer.parseInt(lastFragment), null);
                    } catch (NumberFormatException e) {
                        // it's not a number, this happens if we removed
                        // a label from the NAV_DRAWER_TAGS
                        // give them a nice default...
                        navigationManager.loadFragment(HomeFragment.TAG, null);
                    }
                }
            }
        }

        FragmentTransaction transaction = fm.beginTransaction();
        NavDrawerFragment navDrawerFragment = new NavDrawerFragment();
        transaction.replace(R.id.navDrawerFragment, navDrawerFragment, NavDrawerFragment.TAG);
        AudioPlayerFragment audioPlayerFragment = new AudioPlayerFragment();
        transaction.replace(R.id.audioplayerFragment, audioPlayerFragment, AudioPlayerFragment.TAG);
        transaction.commit();

        View bottomSheet = findViewById(R.id.audioplayerFragment);
        playerStateManager.initialize(bottomSheet);
        sheetBehavior = playerStateManager.getBottomSheet();
        bottomSheetBackPressedCallback = new BottomSheetBackPressedCallback(false, sheetBehavior, bottomSheet);

        startupManager.initializeAppServices();

        // CORREÇÃO DO WORKMANAGER - Usando LiveData corretamente
        WorkManager.getInstance(this)
                .getWorkInfosByTagLiveData(FeedUpdateManagerImpl.WORK_TAG_FEED_UPDATE)
                .observe(this, workInfos -> {
                    boolean isRefreshingFeeds = false;
                    for (WorkInfo workInfo : workInfos) {
                        if (workInfo.getState() == WorkInfo.State.RUNNING) {
                            isRefreshingFeeds = true;
                        } else if (workInfo.getState() == WorkInfo.State.ENQUEUED) {
                            isRefreshingFeeds = true;
                        }
                    }
                    EventBus.getDefault().postSticky(new FeedUpdateRunningEvent(isRefreshingFeeds));
                });

        WorkManager.getInstance(this)
                .getWorkInfosByTagLiveData(DownloadServiceInterface.WORK_TAG)
                .observe(this, workInfos -> {
                    Map<String, DownloadStatus> updatedEpisodes = new HashMap<>();
                    for (WorkInfo workInfo : workInfos) {
                        String downloadUrl = null;
                        for (String tag : workInfo.getTags()) {
                            if (tag.startsWith(DownloadServiceInterface.WORK_TAG_EPISODE_URL)) {
                                downloadUrl = tag.substring(DownloadServiceInterface.WORK_TAG_EPISODE_URL.length());
                            }
                        }
                        if (downloadUrl == null) {
                            continue;
                        }
                        int status;
                        if (workInfo.getState() == WorkInfo.State.RUNNING) {
                            status = DownloadStatus.STATE_RUNNING;
                        } else if (workInfo.getState() == WorkInfo.State.ENQUEUED
                                || workInfo.getState() == WorkInfo.State.BLOCKED) {
                            status = DownloadStatus.STATE_QUEUED;
                        } else {
                            status = DownloadStatus.STATE_COMPLETED;
                        }
                        int progress = workInfo.getProgress().getInt(DownloadServiceInterface.WORK_DATA_PROGRESS, -1);
                        if (progress == -1 && status != DownloadStatus.STATE_COMPLETED) {
                            status = DownloadStatus.STATE_QUEUED;
                            progress = 0;
                        }
                        if (updatedEpisodes.containsKey(downloadUrl) && status == DownloadStatus.STATE_COMPLETED) {
                            continue; // In case of a duplicate, prefer running/queued over completed
                        }
                        updatedEpisodes.put(downloadUrl, new DownloadStatus(status, progress));
                    }
                    DownloadServiceInterface.get().setCurrentDownloads(updatedEpisodes);
                    EventBus.getDefault().postSticky(new EpisodeDownloadEvent(updatedEpisodes));
                });
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        playerStateManager.setSystemBarInsets(systemBarInsets);
    }

    /**
     * View.generateViewId stores the current ID in a static variable.
     * When the process is killed, the variable gets reset.
     * This makes sure that we do not get ID collisions
     * and therefore errors when trying to restore state from another view.
     */
    @SuppressWarnings("StatementWithEmptyBody")
    private void ensureGeneratedViewIdGreaterThan(int minimum) {
        while (View.generateViewId() <= minimum) {
            // Generate new IDs
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_GENERATED_VIEW_ID, View.generateViewId());
    }

    public void setupToolbarToggle(@NonNull MaterialToolbar toolbar, boolean displayUpArrow) {
        if (drawerLayout != null) { // Tablet layout does not have a drawer
            if (drawerManager.getDrawerToggle() != null) {
                drawerLayout.removeDrawerListener(drawerManager.getDrawerToggle());
            }
            ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                    R.string.drawer_open, R.string.drawer_close);
            drawerLayout.addDrawerListener(drawerToggle);
            drawerToggle.syncState();
            drawerToggle.setDrawerIndicatorEnabled(!displayUpArrow);
            drawerToggle.setToolbarNavigationClickListener(v -> getSupportFragmentManager().popBackStack());
            drawerManager.setDrawerToggle(drawerToggle);
        } else if (!displayUpArrow) {
            toolbar.setNavigationIcon(null);
        } else {
            toolbar.setNavigationIcon(ThemeUtils.getDrawableFromAttr(this, R.attr.homeAsUpIndicator));
            toolbar.setNavigationOnClickListener(v -> getSupportFragmentManager().popBackStack());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        drawerManager.removeDrawerListener();
        if (bottomNavigation != null) {
            bottomNavigation.onDestroy();
        }
    }

    public boolean isDrawerOpen() {
        return drawerManager.isDrawerOpen();
    }

    public LockableBottomSheetBehavior<View> getBottomSheet() {
        return sheetBehavior;
    }

    public void setPlayerVisible(boolean visible) {
        playerStateManager.setPlayerVisible(visible);
    }

    public RecyclerView.RecycledViewPool getRecycledViewPool() {
        return recycledViewPool;
    }

    public Fragment createFragmentInstance(String tag, Bundle args) {
        return navigationManager.createFragmentInstance(tag, args);
    }

    public void loadFragment(String tag, Bundle args) {
        navigationManager.loadFragment(tag, args);
        drawerManager.closeDrawer();
    }

    public void loadFeedFragmentById(long feedId, Bundle args) {
        navigationManager.loadFeedFragmentById(feedId, args);
        drawerManager.closeDrawer();
    }

    public void loadFragment(Fragment fragment) {
        navigationManager.loadFragment(fragment);
        drawerManager.closeDrawer();
    }

    public void loadChildFragment(Fragment fragment, TransitionEffect transition) {
        navigationManager.loadChildFragment(fragment, transition);
    }

    public void loadChildFragment(Fragment fragment) {
        navigationManager.loadChildFragment(fragment);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerManager.syncState();
    }

    private void restartActivity() {
        finish();
        startActivity(new Intent(this, MainActivity.class));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerManager.onConfigurationChanged(newConfig);

        @StyleRes int requiredTheme = ThemeSwitcher.getNoTitleTheme(this);
        if (requiredTheme != lastTheme) {
            restartActivity();
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (getBottomSheet().getState() == BottomSheetBehavior.STATE_EXPANDED) {
            playerStateManager.getBottomSheetCallback().onSlide(null, 1.0f);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        new RatingDialogManager(this).showIfNeeded();
        if (bottomNavigation != null) {
            bottomNavigation.onStart();
        }
        getOnBackPressedDispatcher().addCallback(this, openDefaultPageBackPressedCallback);
        getOnBackPressedDispatcher().addCallback(this, bottomSheetBackPressedCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handleNavIntent();

        boolean hasBottomNavigation = bottomNavigation != null;
        if (lastTheme != ThemeSwitcher.getNoTitleTheme(this)
                || hasBottomNavigation != UserPreferences.isBottomNavigationEnabled()) {
            restartActivity();
        }
        if (UserPreferences.getHiddenDrawerItems().contains(NavDrawerFragment.getLastNavFragment(this))) {
            navigationManager.loadFragment(UserPreferences.getDefaultPage(), null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        lastTheme = ThemeSwitcher.getNoTitleTheme(this); // Don't recreate activity when a result is pending
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        if (bottomNavigation != null) {
            bottomNavigation.onStop();
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Glide.get(this).trimMemory(level);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Glide.get(this).clearMemory();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerManager.getDrawerToggle() != null && drawerManager.getDrawerToggle().onOptionsItemSelected(item)) {
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            }
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    class OpenDefaultPageBackPressedCallback extends OnBackPressedCallback {
        OpenDefaultPageBackPressedCallback() {
            super(true);
        }

        @Override
        public void handleOnBackPressed() {
            String defaultPage = UserPreferences.getDefaultPage();
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            } else if (!NavDrawerFragment.getLastNavFragment(MainActivity.this).equals(defaultPage)
                    && !UserPreferences.DEFAULT_PAGE_REMEMBER.equals(defaultPage)) {
                navigationManager.loadFragment(defaultPage, null);
            } else if (UserPreferences.backButtonOpensDrawer() && drawerLayout != null && bottomNavigation == null) {
                drawerLayout.openDrawer(navDrawer);
            } else {
                finish();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(MessageEvent event) {
        Log.d(TAG, "onEvent(" + event + ")");
        Snackbar snackbar;
        if (getBottomSheet().getState() == BottomSheetBehavior.STATE_COLLAPSED) {
            snackbar = Snackbar.make(findViewById(R.id.main_content_view), event.message, Snackbar.LENGTH_LONG);
            if (findViewById(R.id.audioplayerFragment).getVisibility() == View.VISIBLE) {
                snackbar.setAnchorView(findViewById(R.id.audioplayerFragment));
            }
        } else {
            snackbar = Snackbar.make(findViewById(android.R.id.content), event.message, Snackbar.LENGTH_LONG);
        }
        snackbar.show();

        if (event.action != null) {
            snackbar.setAction(event.actionText, v -> event.action.accept(this));
        }
    }

    private void handleNavIntent() {
        Log.d(TAG, "handleNavIntent()");
        Intent intent = getIntent();
        if (intent.hasExtra(MainActivityStarter.EXTRA_FEED_ID)) {
            long feedId = intent.getLongExtra(MainActivityStarter.EXTRA_FEED_ID, 0);
            Bundle args = intent.getBundleExtra(MainActivityStarter.EXTRA_FRAGMENT_ARGS);
            if (feedId > 0) {
                if (intent.getBooleanExtra(MainActivityStarter.EXTRA_CLEAR_BACK_STACK, false)) {
                    navigationManager.loadFeedFragmentById(feedId, args);
                } else {
                    navigationManager.loadChildFragment(FeedItemlistFragment.newInstance(feedId));
                }
            }
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else if (intent.hasExtra(MainActivityStarter.EXTRA_FRAGMENT_TAG)) {
            String tag = intent.getStringExtra(MainActivityStarter.EXTRA_FRAGMENT_TAG);
            Bundle args = intent.getBundleExtra(MainActivityStarter.EXTRA_FRAGMENT_ARGS);
            if (tag != null) {
                if (intent.getBooleanExtra(MainActivityStarter.EXTRA_CLEAR_BACK_STACK, false)) {
                    navigationManager.loadFragment(tag, null);
                } else {
                    navigationManager.loadChildFragment(navigationManager.createFragmentInstance(tag, args));
                }
            }
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        } else if (intent.getBooleanExtra(MainActivityStarter.EXTRA_OPEN_PLAYER, false)) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            playerStateManager.getBottomSheetCallback().onSlide(null, 1.0f);
        } else {
            handleDeeplink(intent.getData());
        }

        if (intent.getBooleanExtra(MainActivityStarter.EXTRA_OPEN_DRAWER, false) && drawerLayout != null) {
            drawerLayout.openDrawer(navDrawer);
        }
        if (intent.getBooleanExtra(MainActivityStarter.EXTRA_OPEN_DOWNLOAD_LOGS, false)) {
            new DownloadLogFragment().show(getSupportFragmentManager(), null);
        }
        if (intent.getBooleanExtra(EXTRA_REFRESH_ON_START, false)) {
            FeedUpdateManager.getInstance().runOnceOrAsk(this);
        }
        // to avoid handling the intent twice when the configuration changes
        setIntent(new Intent(MainActivity.this, MainActivity.class));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNavIntent();
    }

    /**
     * Handles the deep link incoming via App Actions.
     * Performs an in-app search or opens the relevant feature of the app
     * depending on the query.
     *
     * @param uri incoming deep link
     */
    private void handleDeeplink(Uri uri) {
        if (uri == null || uri.getPath() == null) {
            return;
        }
        Log.d(TAG, "Handling deeplink: " + uri.toString());
        switch (uri.getPath()) {
            case "/deeplink/search":
                String query = uri.getQueryParameter("query");
                if (query == null) {
                    return;
                }

                this.navigationManager.loadChildFragment(SearchFragment.newInstance(query));
                break;
            case "/deeplink/main":
                String feature = uri.getQueryParameter("page");
                if (feature == null) {
                    return;
                }
                switch (feature) {
                    case "DOWNLOADS":
                        navigationManager.loadFragment(CompletedDownloadsFragment.TAG, null);
                        break;
                    case "HISTORY":
                        navigationManager.loadFragment(PlaybackHistoryFragment.TAG, null);
                        break;
                    case "EPISODES":
                        navigationManager.loadFragment(AllEpisodesFragment.TAG, null);
                        break;
                    case "QUEUE":
                        navigationManager.loadFragment(QueueFragment.TAG, null);
                        break;
                    case "SUBSCRIPTIONS":
                        navigationManager.loadFragment(SubscriptionFragment.TAG, null);
                        break;
                    default:
                        EventBus.getDefault().post(new MessageEvent(getString(R.string.app_action_not_found, feature)));
                        return;
                }
                break;
            default:
                break;
        }
    }

    //Hardware keyboard support
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        View currentFocus = getCurrentFocus();
        if (currentFocus instanceof EditText) {
            return super.onKeyUp(keyCode, event);
        }

        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        Integer customKeyCode = null;
        EventBus.getDefault().post(event);

        switch (keyCode) {
            case KeyEvent.KEYCODE_P:
                customKeyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
                break;
            case KeyEvent.KEYCODE_J: //Fallthrough
            case KeyEvent.KEYCODE_A:
            case KeyEvent.KEYCODE_COMMA:
                customKeyCode = KeyEvent.KEYCODE_MEDIA_REWIND;
                break;
            case KeyEvent.KEYCODE_K: //Fallthrough
            case KeyEvent.KEYCODE_D:
            case KeyEvent.KEYCODE_PERIOD:
                customKeyCode = KeyEvent.KEYCODE_MEDIA_FAST_FORWARD;
                break;
            case KeyEvent.KEYCODE_PLUS: //Fallthrough
            case KeyEvent.KEYCODE_W:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                return true;
            case KeyEvent.KEYCODE_MINUS: //Fallthrough
            case KeyEvent.KEYCODE_S:
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                return true;
            case KeyEvent.KEYCODE_M:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                            AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI);
                    return true;
                }
                break;
            default:
                break;
        }

        if (customKeyCode != null) {
            sendBroadcast(MediaButtonStarter.createIntent(this, customKeyCode));
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }
}