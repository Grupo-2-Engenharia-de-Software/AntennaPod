package de.danoeh.antennapod.actionbutton;

import android.content.Context;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import android.view.View;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.storage.database.DBWriter;

public class MarkAsPlayedActionButton extends ItemActionButton {

    public MarkAsPlayedActionButton(FeedItem item) {
        super(item);
    }

    @Override
    @StringRes
    protected int getLabel() {
        return (item.hasMedia() ? R.string.mark_as_played_label : R.string.mark_read_no_media_label);
    }

    @Override
    @DrawableRes
    protected int getDrawable() {
        return R.drawable.ic_check;
    }

    @Override
    protected void onClick(Context context) {
        if (!item.isPlayed()) {
            DBWriter.markItemPlayed(item, FeedItem.PLAYED, true);
        }
    }

    @Override
    protected int getVisibility() {
        return (item.isPlayed()) ? View.INVISIBLE : View.VISIBLE;
    }
}
