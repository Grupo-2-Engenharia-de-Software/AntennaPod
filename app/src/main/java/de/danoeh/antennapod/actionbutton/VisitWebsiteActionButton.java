package de.danoeh.antennapod.actionbutton;

import android.content.Context;
import android.view.View;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.ui.common.IntentUtils;

public class VisitWebsiteActionButton extends ItemActionButton {

    public VisitWebsiteActionButton(FeedItem item) {
        super(item);
    }

    @Override
    @StringRes
    protected int getLabel() {
        return R.string.visit_website_label;
    }

    @Override
    @DrawableRes
    protected int getDrawable() {
        return R.drawable.ic_web;
    }

    @Override
    protected void onClick(Context context) {
        IntentUtils.openInBrowser(context, item.getLink());
    }

    @Override
    protected int getVisibility() {
        return (item.getLink() == null) ? View.INVISIBLE : View.VISIBLE;
    }
}
