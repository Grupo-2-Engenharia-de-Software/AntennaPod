package de.danoeh.antennapod.model.feed;

import android.text.TextUtils;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class FeedItemFilter implements Serializable, Parcelable {

    private final String[] properties;

    public final boolean showPlayed;
    public final boolean showUnplayed;
    public final boolean showPaused;
    public final boolean showNotPaused;
    public final boolean showNew;
    public final boolean showQueued;
    public final boolean showNotQueued;
    public final boolean showDownloaded;
    public final boolean showNotDownloaded;
    public final boolean showHasMedia;
    public final boolean showNoMedia;
    public final boolean showIsFavorite;
    public final boolean showNotFavorite;
    public final boolean showInHistory;
    public final boolean includeNotSubscribed;

    public static final String PLAYED = "played";
    public static final String UNPLAYED = "unplayed";
    public static final String NEW = "new";
    public static final String PAUSED = "paused";
    public static final String NOT_PAUSED = "not_paused";
    public static final String IS_FAVORITE = "is_favorite";
    public static final String NOT_FAVORITE = "not_favorite";
    public static final String HAS_MEDIA = "has_media";
    public static final String NO_MEDIA = "no_media";
    public static final String QUEUED = "queued";
    public static final String NOT_QUEUED = "not_queued";
    public static final String DOWNLOADED = "downloaded";
    public static final String NOT_DOWNLOADED = "not_downloaded";
    public static final String IS_IN_HISTORY = "is_in_history";
    public static final String INCLUDE_NOT_SUBSCRIBED = "include_not_subscribed";

    public static FeedItemFilter unfiltered() {
        return new FeedItemFilter("");
    }

    public FeedItemFilter(String properties) {
        this(TextUtils.split(properties, ","));
    }

    public FeedItemFilter(FeedItemFilter filter, String... additionalProperties) {
        this(TextUtils.join(",", filter.getValues()) + "," + TextUtils.join(",", additionalProperties));
    }

    public FeedItemFilter(String... properties) {
        this.properties = properties;

        // see R.arrays.feed_filter_values
        showUnplayed = hasProperty(UNPLAYED);
        showPaused = hasProperty(PAUSED);
        showNotPaused = hasProperty(NOT_PAUSED);
        showPlayed = hasProperty(PLAYED);
        showQueued = hasProperty(QUEUED);
        showNotQueued = hasProperty(NOT_QUEUED);
        showDownloaded = hasProperty(DOWNLOADED);
        showNotDownloaded = hasProperty(NOT_DOWNLOADED);
        showHasMedia = hasProperty(HAS_MEDIA);
        showNoMedia = hasProperty(NO_MEDIA);
        showIsFavorite = hasProperty(IS_FAVORITE);
        showNotFavorite = hasProperty(NOT_FAVORITE);
        showNew = hasProperty(NEW);
        showInHistory = hasProperty(IS_IN_HISTORY);
        includeNotSubscribed = hasProperty(INCLUDE_NOT_SUBSCRIBED);
    }

    private boolean hasProperty(String property) {
        return Arrays.asList(properties).contains(property);
    }

    public String[] getValues() {
        return properties.clone();
    }

    public List<String> getValuesList() {
        return Arrays.asList(properties);
    }

    public boolean matches(FeedItem item) {
        if (showNew && !item.isNew()) {
            return false;
        } else if (showPlayed && !item.isPlayed()) {
            return false;
        } else if (showUnplayed && item.isPlayed()) {
            return false;
        } else if (showPaused && !item.isInProgress()) {
            return false;
        } else if (showNotPaused && item.isInProgress()) {
            return false;
        } else if (showNew && !item.isNew()) {
            return false;
        } else if (showQueued && !item.isTagged(FeedItem.TAG_QUEUE)) {
            return false;
        } else if (showNotQueued && item.isTagged(FeedItem.TAG_QUEUE)) {
            return false;
        } else if (showDownloaded && !item.isDownloaded()) {
            return false;
        } else if (showNotDownloaded && item.isDownloaded()) {
            return false;
        } else if (showHasMedia && !item.hasMedia()) {
            return false;
        } else if (showNoMedia && item.hasMedia()) {
            return false;
        } else if (showIsFavorite && !item.isTagged(FeedItem.TAG_FAVORITE)) {
            return false;
        } else if (showNotFavorite && item.isTagged(FeedItem.TAG_FAVORITE)) {
            return false;
        } else if (showInHistory && item.getMedia() != null
                && item.getMedia().getLastPlayedTimeHistory().getTime() == 0) {
            return false;
        } else if (!includeNotSubscribed && item.getFeed() != null
                && item.getFeed().getState() != Feed.STATE_SUBSCRIBED) {
            return false;
        }
        return true;
    }

    // Implementação de Parcelable
    
    protected FeedItemFilter(Parcel in) {
        properties = in.createStringArray();

        showPlayed = in.readByte() != 0;
        showUnplayed = in.readByte() != 0;
        showPaused = in.readByte() != 0;
        showNotPaused = in.readByte() != 0;
        showNew = in.readByte() != 0;
        showQueued = in.readByte() != 0;
        showNotQueued = in.readByte() != 0;
        showDownloaded = in.readByte() != 0;
        showNotDownloaded = in.readByte() != 0;
        showHasMedia = in.readByte() != 0;
        showNoMedia = in.readByte() != 0;
        showIsFavorite = in.readByte() != 0;
        showNotFavorite = in.readByte() != 0;
        showInHistory = in.readByte() != 0;
        includeNotSubscribed = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(properties);

        dest.writeByte((byte) (showPlayed ? 1 : 0));
        dest.writeByte((byte) (showUnplayed ? 1 : 0));
        dest.writeByte((byte) (showPaused ? 1 : 0));
        dest.writeByte((byte) (showNotPaused ? 1 : 0));
        dest.writeByte((byte) (showNew ? 1 : 0));
        dest.writeByte((byte) (showQueued ? 1 : 0));
        dest.writeByte((byte) (showNotQueued ? 1 : 0));
        dest.writeByte((byte) (showDownloaded ? 1 : 0));
        dest.writeByte((byte) (showNotDownloaded ? 1 : 0));
        dest.writeByte((byte) (showHasMedia ? 1 : 0));
        dest.writeByte((byte) (showNoMedia ? 1 : 0));
        dest.writeByte((byte) (showIsFavorite ? 1 : 0));
        dest.writeByte((byte) (showNotFavorite ? 1 : 0));
        dest.writeByte((byte) (showInHistory ? 1 : 0));
        dest.writeByte((byte) (includeNotSubscribed ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<FeedItemFilter> CREATOR = new Creator<FeedItemFilter>() {
        @Override
        public FeedItemFilter createFromParcel(Parcel in) {
            return new FeedItemFilter(in);
        }

        @Override
        public FeedItemFilter[] newArray(int size) {
            return new FeedItemFilter[size];
        }
    };
}
