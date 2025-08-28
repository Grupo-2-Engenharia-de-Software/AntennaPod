package de.danoeh.antennapod.actionbutton;

import android.content.Context;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.playback.service.PlaybackService;
import de.danoeh.antennapod.playback.service.PlaybackServiceStarter;

public class PlayLocalActionButton extends ItemActionButton {

    public PlayLocalActionButton(FeedItem item) {
        super(item);
    }

    @Override
    @StringRes
    protected int getLabel() {
        return R.string.play_label;
    }

    @Override
    @DrawableRes
    protected int getDrawable() {
        return R.drawable.ic_play_24dp;
    }

    @Override
    protected void onClick(Context context) {
        final FeedMedia media = item.getMedia();
        if (media == null) {
            return;
        }

        new PlaybackServiceStarter(context, media)
                .callEvenIfRunning(true)
                .start();

        if (media.getMediaType() == MediaType.VIDEO) {
            context.startActivity(PlaybackService.getPlayerActivityIntent(context, media));
        }
    }
}
