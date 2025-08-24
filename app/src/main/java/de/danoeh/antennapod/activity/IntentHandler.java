package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.ui.appstartintent.MainActivityStarter;
import de.danoeh.antennapod.ui.screen.SearchFragment;
import de.danoeh.antennapod.ui.screen.download.CompletedDownloadsFragment;
import de.danoeh.antennapod.ui.screen.PlaybackHistoryFragment;
import de.danoeh.antennapod.ui.screen.AllEpisodesFragment;
import de.danoeh.antennapod.ui.screen.queue.QueueFragment;
import de.danoeh.antennapod.ui.screen.subscriptions.SubscriptionFragment;
import de.danoeh.antennapod.ui.screen.feed.FeedItemlistFragment;
import de.danoeh.antennapod.ui.screen.download.DownloadLogFragment;
import de.danoeh.antennapod.net.download.serviceinterface.FeedUpdateManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import org.greenrobot.eventbus.EventBus;

public class IntentHandler {
    private final MainActivity activity;
    private final NavigationManager navigationManager;

    public IntentHandler(MainActivity activity, NavigationManager navigationManager) {
        this.activity = activity;
        this.navigationManager = navigationManager;
    }

    public void handleNavIntent(Intent intent) {
        if (intent.hasExtra(MainActivityStarter.EXTRA_FEED_ID)) {
            handleFeedIntent(intent);
        } else if (intent.hasExtra(MainActivityStarter.EXTRA_FRAGMENT_TAG)) {
            handleFragmentIntent(intent);
        } else if (intent.getBooleanExtra(MainActivityStarter.EXTRA_OPEN_PLAYER, false)) {
            handleOpenPlayerIntent();
        } else {
            handleDeeplink(intent.getData());
        }

        handleAdditionalIntentFlags(intent);
    }

    private void handleFeedIntent(Intent intent) {
        long feedId = intent.getLongExtra(MainActivityStarter.EXTRA_FEED_ID, 0);
        Bundle args = intent.getBundleExtra(MainActivityStarter.EXTRA_FRAGMENT_ARGS);
        if (feedId > 0) {
            if (intent.getBooleanExtra(MainActivityStarter.EXTRA_CLEAR_BACK_STACK, false)) {
                navigationManager.loadFeedFragmentById(feedId, args);
            } else {
                navigationManager.loadChildFragment(FeedItemlistFragment.newInstance(feedId));
            }
        }
        activity.getBottomSheet().setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void handleFragmentIntent(Intent intent) {
        String tag = intent.getStringExtra(MainActivityStarter.EXTRA_FRAGMENT_TAG);
        Bundle args = intent.getBundleExtra(MainActivityStarter.EXTRA_FRAGMENT_ARGS);
        if (tag != null) {
            if (intent.getBooleanExtra(MainActivityStarter.EXTRA_CLEAR_BACK_STACK, false)) {
                navigationManager.loadFragment(tag, null);
            } else {
                navigationManager.loadChildFragment(navigationManager.createFragmentInstance(tag, args));
            }
        }
        activity.getBottomSheet().setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void handleOpenPlayerIntent() {
        activity.getBottomSheet().setState(BottomSheetBehavior.STATE_EXPANDED);
        activity.getPlayerStateManager().getBottomSheetCallback().onSlide(null, 1.0f);
    }

    private void handleAdditionalIntentFlags(Intent intent) {
        if (intent.getBooleanExtra(MainActivityStarter.EXTRA_OPEN_DRAWER, false) && activity.isDrawerAvailable()) {
            activity.openDrawer();
        }
        if (intent.getBooleanExtra(MainActivityStarter.EXTRA_OPEN_DOWNLOAD_LOGS, false)) {
            new DownloadLogFragment().show(activity.getSupportFragmentManager(), null);
        }
        if (intent.getBooleanExtra(MainActivity.EXTRA_REFRESH_ON_START, false)) {
            FeedUpdateManager.getInstance().runOnceOrAsk(activity);
        }
    }

    public void handleDeeplink(Uri uri) {
        if (uri == null || uri.getPath() == null) return;

        switch (uri.getPath()) {
            case "/deeplink/search":
                handleSearchDeeplink(uri);
                break;
            case "/deeplink/main":
                handleMainDeeplink(uri);
                break;
        }
    }

    private void handleSearchDeeplink(Uri uri) {
        String query = uri.getQueryParameter("query");
        if (query != null) {
            navigationManager.loadChildFragment(SearchFragment.newInstance(query));
        }
    }

    private void handleMainDeeplink(Uri uri) {
        String feature = uri.getQueryParameter("page");
        if (feature == null) return;

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
                EventBus.getDefault().post(new MessageEvent(
                        activity.getString(R.string.app_action_not_found, feature)));
                break;
        }
    }
}