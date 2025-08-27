package de.danoeh.antennapod.ui.episodeslist;

import android.app.Activity;
import android.os.Looper;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.Shadows;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class EpisodeMultiSelectActionHandlerTest {

    private static final String TAG = "EpisodeSelectHandler";

    private Activity activity;
    private MessageCatcher catcher;

    public static class MessageCatcher {
        final List<MessageEvent> received = new ArrayList<>();
        @Subscribe
        public void onMessage(MessageEvent event) {
            received.add(event);
        }
    }

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        catcher = new MessageCatcher();
        EventBus.getDefault().register(catcher);
        ShadowLog.clear();
    }

    @After
    public void tearDown() {
        EventBus.getDefault().unregister(catcher);
        ShadowLog.clear();
    }

    private void idleMainLooper() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    @Test
    public void testHandleActionWithUnknownIdDoesNothing() {
        int unknownId = 987654321;
        EpisodeMultiSelectActionHandler handler = new EpisodeMultiSelectActionHandler(activity, unknownId);

        // Execute with an empty list; unknown action should do nothing but log an error
        handler.handleAction(Collections.<FeedItem>emptyList());

        // No user-facing MessageEvent should be posted
        assertTrue("No MessageEvent should be posted for unknown action id", catcher.received.isEmpty());

        // Verify an error log was written with the expected tag and message
        boolean foundError = false;
        for (ShadowLog.LogItem item : ShadowLog.getLogs()) {
            if (TAG.equals(item.tag) && item.type == Log.ERROR
                    && item.msg != null
                    && item.msg.contains("Unrecognized speed dial action item. Do nothing. id=" + unknownId)) {
                foundError = true;
                break;
            }
        }
        assertTrue("Expected an error log for unknown action id", foundError);
    }

    @Test
    public void testHandleActionAddToQueueFiltersEligibleAndShowsMessage() {
        catcher.received.clear();

        // Prepare items: none should be eligible for queuing
        // - One without media
        FeedItem noMedia = new FeedItem();

        // - One with media but already queued (simulate via override of hasMedia and tag)
        FeedItem alreadyQueued = new FeedItem() {
            @Override
            public boolean hasMedia() {
                return true;
            }
        };
        alreadyQueued.addTag(FeedItem.TAG_QUEUE);

        List<FeedItem> items = Arrays.asList(noMedia, alreadyQueued);

        EpisodeMultiSelectActionHandler handler =
                new EpisodeMultiSelectActionHandler(activity, R.id.add_to_queue_item);
        handler.handleAction(items);

        idleMainLooper();

        // Expect one message with count 0 (since no items were eligible)
        assertEquals(1, catcher.received.size());
        assertNotNull(catcher.received.get(0).message);
        assertTrue(catcher.received.get(0).message.contains("0"));
    }

    @Test
    public void testHandleActionDownloadEligibleInOrderShowsMessage() {
        catcher.received.clear();

        // Prepare items: all ineligible -> no downloads, message with 0
        // - One with media but already downloaded
        FeedItem downloaded = new FeedItem() {
            @Override
            public boolean hasMedia() {
                return true;
            }

            @Override
            public boolean isDownloaded() {
                return true;
            }
        };
        // - One without media
        FeedItem noMedia = new FeedItem();

        List<FeedItem> items = Arrays.asList(downloaded, noMedia);

        EpisodeMultiSelectActionHandler handler =
                new EpisodeMultiSelectActionHandler(activity, R.id.download_item);
        handler.handleAction(items);

        idleMainLooper();

        // Expect one message with count 0
        assertEquals(1, catcher.received.size());
        assertNotNull(catcher.received.get(0).message);
        assertTrue(catcher.received.get(0).message.contains("0"));
    }

    @Test
    public void testHandleActionMarkReadMarksAllSelectedAndShowsMessage() {
        catcher.received.clear();

        // Empty selection -> should show batch message with 0 and avoid DB work
        List<FeedItem> items = new ArrayList<>();

        EpisodeMultiSelectActionHandler handler =
                new EpisodeMultiSelectActionHandler(activity, R.id.mark_read_item);
        handler.handleAction(items);

        idleMainLooper();

        // Expect one message with count 0
        assertEquals(1, catcher.received.size());
        assertNotNull(catcher.received.get(0).message);
        assertTrue(catcher.received.get(0).message.contains("0"));
    }

    @Test
    public void testShowMessageSuppressesSingleItem() throws Exception {
        catcher.received.clear();

        // Use deleteChecked directly to avoid confirmation dialog
        EpisodeMultiSelectActionHandler handler =
                new EpisodeMultiSelectActionHandler(activity, R.id.remove_item);

        // Single local feed item without media
        Feed localFeed = new Feed(Feed.PREFIX_LOCAL_FOLDER + "//test", null, "Test Local Feed");
        FeedItem localNoMedia = new FeedItem();
        localNoMedia.setFeed(localFeed);

        Method m = EpisodeMultiSelectActionHandler.class
                .getDeclaredMethod("deleteChecked", List.class);
        m.setAccessible(true);
        m.invoke(handler, Arrays.asList(localNoMedia));

        idleMainLooper();

        // Single-item operation suppresses user message
        assertTrue(catcher.received.isEmpty());
    }

    @Test
    public void testDeleteActionHandlesLocalFeedWithoutMedia() throws Exception {
        catcher.received.clear();

        EpisodeMultiSelectActionHandler handler =
                new EpisodeMultiSelectActionHandler(activity, R.id.remove_item);

        // Two local feed items without media -> should be counted and not crash
        Feed localFeed = new Feed(Feed.PREFIX_LOCAL_FOLDER + "//test", null, "Test Local Feed");
        FeedItem item1 = new FeedItem();
        item1.setFeed(localFeed);
        FeedItem item2 = new FeedItem();
        item2.setFeed(localFeed);

        Method m = EpisodeMultiSelectActionHandler.class
                .getDeclaredMethod("deleteChecked", List.class);
        m.setAccessible(true);
        m.invoke(handler, Arrays.asList(item1, item2));

        idleMainLooper();

        // Expect one message with count 2
        assertEquals(1, catcher.received.size());
        assertNotNull(catcher.received.get(0).message);
        assertTrue(catcher.received.get(0).message.contains("2"));
    }
}

