package de.danoeh.antennapod.activity;

import android.content.Context;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import de.danoeh.antennapod.event.EpisodeDownloadEvent;
import de.danoeh.antennapod.event.FeedUpdateRunningEvent;
import de.danoeh.antennapod.model.download.DownloadStatus;
import de.danoeh.antennapod.net.download.service.feed.FeedUpdateManagerImpl;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import org.greenrobot.eventbus.EventBus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkManagerObserver {
    private final Context context;

    public WorkManagerObserver(Context context) {
        this.context = context;
    }

    public void observeFeedUpdates() {
        WorkManager.getInstance(context)
                .getWorkInfosByTagLiveData(FeedUpdateManagerImpl.WORK_TAG_FEED_UPDATE)
                .observeForever(workInfos -> {
                    boolean isRefreshingFeeds = false;
                    for (WorkInfo workInfo : workInfos) {
                        if (workInfo.getState() == WorkInfo.State.RUNNING ||
                                workInfo.getState() == WorkInfo.State.ENQUEUED) {
                            isRefreshingFeeds = true;
                        }
                    }
                    EventBus.getDefault().postSticky(new FeedUpdateRunningEvent(isRefreshingFeeds));
                });
    }

    public void observeDownloads() {
        WorkManager.getInstance(context)
                .getWorkInfosByTagLiveData(DownloadServiceInterface.WORK_TAG)
                .observeForever(workInfos -> {
                    Map<String, DownloadStatus> updatedEpisodes = processDownloadWorkInfos(workInfos);
                    DownloadServiceInterface.get().setCurrentDownloads(updatedEpisodes);
                    EventBus.getDefault().postSticky(new EpisodeDownloadEvent(updatedEpisodes));
                });
    }

    private Map<String, DownloadStatus> processDownloadWorkInfos(List<WorkInfo> workInfos) {
        Map<String, DownloadStatus> updatedEpisodes = new HashMap<>();
        for (WorkInfo workInfo : workInfos) {
            String downloadUrl = extractDownloadUrl(workInfo);
            if (downloadUrl == null) continue;

            int status = determineDownloadStatus(workInfo);
            int progress = extractProgress(workInfo, status);

            if (updatedEpisodes.containsKey(downloadUrl) && status == DownloadStatus.STATE_COMPLETED) {
                continue;
            }

            updatedEpisodes.put(downloadUrl, new DownloadStatus(status, progress));
        }
        return updatedEpisodes;
    }

    private String extractDownloadUrl(WorkInfo workInfo) {
        for (String tag : workInfo.getTags()) {
            if (tag.startsWith(DownloadServiceInterface.WORK_TAG_EPISODE_URL)) {
                return tag.substring(DownloadServiceInterface.WORK_TAG_EPISODE_URL.length());
            }
        }
        return null;
    }

    private int determineDownloadStatus(WorkInfo workInfo) {
        if (workInfo.getState() == WorkInfo.State.RUNNING) {
            return DownloadStatus.STATE_RUNNING;
        } else if (workInfo.getState() == WorkInfo.State.ENQUEUED ||
                workInfo.getState() == WorkInfo.State.BLOCKED) {
            return DownloadStatus.STATE_QUEUED;
        } else {
            return DownloadStatus.STATE_COMPLETED;
        }
    }

    private int extractProgress(WorkInfo workInfo, int status) {
        int progress = workInfo.getProgress().getInt(DownloadServiceInterface.WORK_DATA_PROGRESS, -1);
        if (progress == -1 && status != DownloadStatus.STATE_COMPLETED) {
            return 0;
        }
        return progress;
    }
}