package de.danoeh.antennapod.actionbutton;

import android.content.Context;
import android.widget.ImageView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import android.view.View;
import android.widget.TextView;

import de.danoeh.antennapod.playback.service.PlaybackStatus;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

public abstract class ItemActionButton {
    FeedItem item;

    ItemActionButton(FeedItem item) {
        this.item = item;
    }

    @StringRes
    protected abstract int getLabel();

    @DrawableRes
    protected abstract int getDrawable();

    protected abstract void onClick(Context context);

    protected int getVisibility() {
        return View.VISIBLE;
    }

    @NonNull
    public static ItemActionButton forItem(@NonNull FeedItem item) {
        final FeedMedia media = item.getMedia();
        if (media == null) {
            return new MarkAsPlayedActionButton(item);
        }

        final boolean isDownloadingMedia = DownloadServiceInterface.get().isDownloadingEpisode(media.getDownloadUrl());
        if (PlaybackStatus.isCurrentlyPlaying(media)) {
            return new PauseActionButton(item);
        } else if (item.getFeed().isLocalFeed()) {
            return new PlayLocalActionButton(item);
        } else if (media.isDownloaded()) {
            return new PlayActionButton(item);
        } else if (isDownloadingMedia) {
            return new CancelDownloadActionButton(item);
        } else if (UserPreferences.isStreamOverDownload()) {
            return new StreamActionButton(item);
        } else {
            return new DownloadActionButton(item);
        }
    }

    public void configure(@NonNull View button, @NonNull ImageView icon, Context context) {
        button.setVisibility(getVisibility());
        button.setContentDescription(context.getString(getLabel()));
        button.setOnClickListener((view) -> onClick(context));
        icon.setImageResource(getDrawable());
    }

    public void copyIntoViews(TextView textView, ImageView imageView, View view) {
        textView.setText(this.getLabel());
        textView.setTransformationMethod(null);
        imageView.setImageResource(this.getDrawable());
        view.setVisibility(this.getVisibility());
    }

    public void performClick(Context context) {
        onClick(context);
    }
}
