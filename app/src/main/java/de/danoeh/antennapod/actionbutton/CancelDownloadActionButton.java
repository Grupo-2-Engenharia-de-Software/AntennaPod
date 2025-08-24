package de.danoeh.antennapod.actionbutton;

import android.content.Context;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.net.download.serviceinterface.DownloadServiceInterface;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.storage.database.DBWriter;

public class CancelDownloadActionButton extends ItemActionButton {

    public CancelDownloadActionButton(FeedItem item) {
        super(item);
    }

    @Override
    @StringRes
    protected int getLabel() {
        return R.string.cancel_download_label;
    }

    @Override
    @DrawableRes
    protected int getDrawable() {
        return R.drawable.ic_cancel;
    }

    @Override
    protected void onClick(Context context) {
        FeedMedia media = item.getMedia();
        DownloadServiceInterface.get().cancel(context, media);
        item.disableAutoDownload();
        DBWriter.setFeedItem(item);
    }
}
