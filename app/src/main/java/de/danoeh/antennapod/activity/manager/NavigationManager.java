package de.danoeh.antennapod.activity.manager;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.ui.TransitionEffect;
import de.danoeh.antennapod.ui.screen.AddFeedFragment;
import de.danoeh.antennapod.ui.screen.AllEpisodesFragment;
import de.danoeh.antennapod.ui.screen.download.CompletedDownloadsFragment;
import de.danoeh.antennapod.ui.discovery.DiscoveryFragment;
import de.danoeh.antennapod.ui.screen.home.HomeFragment;
import de.danoeh.antennapod.ui.screen.InboxFragment;
import de.danoeh.antennapod.ui.screen.PlaybackHistoryFragment;
import de.danoeh.antennapod.ui.screen.queue.QueueFragment;
import de.danoeh.antennapod.ui.screen.subscriptions.SubscriptionFragment;
import de.danoeh.antennapod.ui.screen.feed.FeedItemlistFragment;
import de.danoeh.antennapod.ui.screen.drawer.NavDrawerFragment;
import org.apache.commons.lang3.Validate;

public class NavigationManager {
    private final MainActivity activity;
    private static final String MAIN_FRAGMENT_TAG = "main";

    public NavigationManager(MainActivity activity) {
        this.activity = activity;
    }

    public Fragment createFragmentInstance(String tag, Bundle args) {
        Fragment fragment;
        switch (tag) {
            case HomeFragment.TAG:
                fragment = new HomeFragment();
                break;
            case QueueFragment.TAG:
                fragment = new QueueFragment();
                break;
            case InboxFragment.TAG:
                fragment = new InboxFragment();
                break;
            case AllEpisodesFragment.TAG:
                fragment = new AllEpisodesFragment();
                break;
            case CompletedDownloadsFragment.TAG:
                fragment = new CompletedDownloadsFragment();
                break;
            case PlaybackHistoryFragment.TAG:
                fragment = new PlaybackHistoryFragment();
                break;
            case AddFeedFragment.TAG:
                fragment = new AddFeedFragment();
                break;
            case SubscriptionFragment.TAG:
                fragment = new SubscriptionFragment();
                break;
            case DiscoveryFragment.TAG:
                fragment = new DiscoveryFragment();
                break;
            default:
                fragment = new HomeFragment();
                args = null;
                break;
        }
        if (args != null) {
            fragment.setArguments(args);
        }
        return fragment;
    }

    public void loadFragment(String tag, Bundle args) {
        NavDrawerFragment.saveLastNavFragment(activity, tag);
        loadFragment(createFragmentInstance(tag, args));
    }

    public void loadFeedFragmentById(long feedId, Bundle args) {
        Fragment fragment = FeedItemlistFragment.newInstance(feedId);
        if (args != null) {
            fragment.setArguments(args);
        }
        NavDrawerFragment.saveLastNavFragment(activity, String.valueOf(feedId));
        loadFragment(fragment);
    }

    public void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        for (int i = 0; i < fragmentManager.getBackStackEntryCount(); i++) {
            fragmentManager.popBackStack();
        }

        FragmentTransaction t = fragmentManager.beginTransaction();
        t.replace(R.id.main_content_view, fragment, MAIN_FRAGMENT_TAG);
        fragmentManager.popBackStack();
        t.commitAllowingStateLoss();
    }

    public void loadChildFragment(Fragment fragment, TransitionEffect transition) {
        Validate.notNull(fragment);
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();

        if (transition == TransitionEffect.FADE) {
            transaction.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        } else if (transition == TransitionEffect.SLIDE) {
            transaction.setCustomAnimations(
                    R.anim.slide_right_in,
                    R.anim.slide_left_out,
                    R.anim.slide_left_in,
                    R.anim.slide_right_out);
        }

        transaction
                .hide(activity.getSupportFragmentManager().findFragmentByTag(MAIN_FRAGMENT_TAG))
                .add(R.id.main_content_view, fragment, MAIN_FRAGMENT_TAG)
                .addToBackStack(null)
                .commit();
    }

    public void loadChildFragment(Fragment fragment) {
        loadChildFragment(fragment, TransitionEffect.NONE);
    }
}