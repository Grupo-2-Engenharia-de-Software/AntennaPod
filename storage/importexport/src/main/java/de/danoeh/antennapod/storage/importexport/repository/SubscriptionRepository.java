package de.danoeh.antennapod.storage.importexport.repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.NavDrawerData;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

import io.reactivex.Observable;


public class SubscriptionRepository {

    public Observable<List<Feed>> getNavDrawerFeeds() {
        return Observable.fromCallable(
                () -> {
                    NavDrawerData data = DBReader.getNavDrawerData(
                            UserPreferences.getSubscriptionsFilter(),
                            UserPreferences.getFeedOrder(),
                            UserPreferences.getFeedCounterSetting()
                    );
                    return getFeedItems(data.items);
                });
    }

    /*O método getFeedItems usava um result.contains(feed), que pode ser ineficiente (O(n)).
    Usar um HashSet para verificar a existência de um feed é muito mais rápido (O(1)).*/
    private List<Feed> getFeedItems(List<NavDrawerData.DrawerItem> items) {
        List<Feed> result = new ArrayList<>();
        Set<Long> addedFeedIds = new HashSet<>();
        getFeedItemsRecursive(items, result, addedFeedIds);
        return result;
    }

    private void getFeedItemsRecursive(List<NavDrawerData.DrawerItem> items, List<Feed> result, Set<Long> addedFeedIds) {
        for (NavDrawerData.DrawerItem item : items) {
            if (item.type == NavDrawerData.DrawerItem.Type.TAG) {
                getFeedItemsRecursive(((NavDrawerData.TagDrawerItem) item).getChildren(), result, addedFeedIds);
            } else {
                Feed feed = ((NavDrawerData.FeedDrawerItem) item).feed;
                if (!addedFeedIds.contains(feed.getId())) {
                    result.add(feed);
                    addedFeedIds.add(feed.getId());
                }
            }
        }
    }
}