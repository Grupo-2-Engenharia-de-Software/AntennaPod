package de.test.antennapod.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.espresso.intent.rule.IntentsTestRule;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.MediaType;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.playback.service.PlaybackController;
import de.danoeh.antennapod.ui.screen.playback.audio.AudioPlayerFragment;
import de.danoeh.antennapod.ui.screen.playback.audio.CoverFragment;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class CoverFragmentTest {

    @Rule
    public IntentsTestRule<MainActivity> activityRule =
            new IntentsTestRule<>(MainActivity.class, false, false);

    private static class TestPlaybackController extends PlaybackController {
        volatile boolean playPauseCalled = false;
        volatile int lastSeekTo = -1;

        TestPlaybackController(MainActivity activity) {
            super(activity);
        }

        @Override
        public void loadMediaInfo() {
            // no-op for test
        }

        @Override
        public void playPause() {
            playPauseCalled = true;
        }

        @Override
        public void seekTo(int time) {
            lastSeekTo = time;
        }
    }

    @Test
    public void testClickingCoverTogglesPlayback() throws Throwable {
        activityRule.launchActivity(new Intent());
        MainActivity activity = activityRule.getActivity();

        final AtomicReference<TestPlaybackController> controllerRef = new AtomicReference<>();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            CoverFragment fragment = new CoverFragment();

            FrameLayout container = new FrameLayout(activity);
            int containerId = View.generateViewId();
            container.setId(containerId);
            activity.addContentView(container, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

            // Properly attach fragment using fragment manager
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(containerId, fragment, "cover")
                    .commitNow();

            TestPlaybackController testController = new TestPlaybackController(activity);
            controllerRef.set(testController);

            try {
                Field controllerField = CoverFragment.class.getDeclaredField("controller");
                controllerField.setAccessible(true);
                controllerField.set(fragment, testController);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            View coverView = fragment.getView().findViewById(R.id.imgvCover);
            if (coverView == null) {
                throw new AssertionError("Cover view (imgvCover) not found in layout");
            }
            coverView.performClick();
        });

        assertTrue("Expected playPause() to be called when clicking cover image",
                controllerRef.get().playPauseCalled);
    }

    @Test
    public void testSeekToNextChapterAdvancesAndSeeks() throws Throwable {
        activityRule.launchActivity(new Intent());
        MainActivity activity = activityRule.getActivity();

        final AtomicInteger displayedIndexAfter = new AtomicInteger(-1);
        final AtomicInteger seekToTime = new AtomicInteger(-1);

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            // Set up fragment
            CoverFragment fragment = new CoverFragment();
            FrameLayout container = new FrameLayout(activity);
            int containerId = View.generateViewId();
            container.setId(containerId);
            activity.addContentView(container, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            
            // Properly attach fragment using fragment manager
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(containerId, fragment, "cover")
                    .commitNow();

            // Prepare chapters
            List<Chapter> chapters = new ArrayList<>();
            chapters.add(new Chapter(0, "Chapter 1", null, null));
            chapters.add(new Chapter(30000, "Chapter 2", null, null)); // 30 seconds

            // Prepare Playable
            Playable playable = new Playable() {
                int duration = 60000;
                int position = 1000;
                List<Chapter> ch = chapters;

                @Override
                public String getEpisodeTitle() { return "Episode"; }

                @Override
                public List<Chapter> getChapters() { return ch; }

                @Override
                public String getWebsiteLink() { return null; }

                @Override
                public String getFeedTitle() { return "Feed"; }

                @Override
                public Date getPubDate() { return null; }

                @Override
                public Object getIdentifier() { return 1L; }

                @Override
                public int getDuration() { return duration; }

                @Override
                public int getPosition() { return position; }

                @Override
                public long getLastPlayedTimeStatistics() { return 0; }

                @Override
                public String getDescription() { return null; }

                @Override
                public MediaType getMediaType() { return MediaType.AUDIO; }

                @Override
                public String getLocalFileUrl() { return null; }

                @Override
                public String getStreamUrl() { return null; }

                @Override
                public boolean localFileAvailable() { return false; }

                @Override
                public void setPosition(int newPosition) { position = newPosition; }

                @Override
                public void setDuration(int newDuration) { duration = newDuration; }

                @Override
                public void setLastPlayedTimeStatistics(long lastPlayedTimestamp) { }

                @Override
                public void onPlaybackStart() { }

                @Override
                public void onPlaybackPause(Context context) { }

                @Override
                public void onPlaybackCompleted(Context context) { }

                @Override
                public int getPlayableType() { return 0; }

                @Override
                public void setChapters(List<Chapter> chapters) { ch = chapters; }

                @Override
                public String getImageLocation() { return null; }

                // Parcelable
                @Override
                public int describeContents() { return 0; }

                @Override
                public void writeToParcel(Parcel dest, int flags) { }
            };

            // Inject controller
            TestPlaybackController testController = new TestPlaybackController(activity);
            try {
                Field controllerField = CoverFragment.class.getDeclaredField("controller");
                controllerField.setAccessible(true);
                controllerField.set(fragment, testController);

                Field mediaField = CoverFragment.class.getDeclaredField("media");
                mediaField.setAccessible(true);
                mediaField.set(fragment, playable);

                Field indexField = CoverFragment.class.getDeclaredField("displayedChapterIndex");
                indexField.setAccessible(true);
                indexField.setInt(fragment, 0);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // Click next chapter
            View nextButton = fragment.getView().findViewById(R.id.butNextChapter);
            nextButton.performClick();

            // Read updated state
            try {
                Field indexField = CoverFragment.class.getDeclaredField("displayedChapterIndex");
                indexField.setAccessible(true);
                displayedIndexAfter.set(indexField.getInt(fragment));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            seekToTime.set(testController.lastSeekTo);
        });

        assertEquals("Displayed chapter index should advance to next", 1, displayedIndexAfter.get());
        assertEquals("Should seek to start of next chapter", 30000, seekToTime.get());
    }

    @Test
    public void testChapterButtonVisibleWhenItemHasChaptersButChaptersNull() throws Throwable {
        activityRule.launchActivity(new Intent());
        MainActivity activity = activityRule.getActivity();

        final int[] visibilityAfter = new int[1];

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            // Arrange: Create and attach CoverFragment view
            CoverFragment fragment = new CoverFragment();
            FrameLayout container = new FrameLayout(activity);
            int containerId = View.generateViewId();
            container.setId(containerId);
            activity.addContentView(container, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            
            // Properly attach fragment using fragment manager
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(containerId, fragment, "cover")
                    .commitNow();

            // Prepare FeedMedia whose item claims to have chapters, but actual chapters are not loaded (null)
            FeedItem item = new FeedItem() {
                @Override
                public boolean hasChapters() {
                    return true;
                }
            };
            FeedMedia media = new FeedMedia(item, "http://example.com/audio.mp3", 0, "audio/mpeg");

            // Inject media into fragment
            try {
                Field mediaField = CoverFragment.class.getDeclaredField("media");
                mediaField.setAccessible(true);
                mediaField.set(fragment, media);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // Ensure initial visibility is GONE to detect changes
            View chapterButton = fragment.getView().findViewById(R.id.chapterButton);
            chapterButton.setVisibility(View.GONE);

            // Act: Invoke private method updateChapterControlVisibility
            try {
                Method updateVisibility = CoverFragment.class.getDeclaredMethod("updateChapterControlVisibility");
                updateVisibility.setAccessible(true);
                updateVisibility.invoke(fragment);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // Capture result
            visibilityAfter[0] = chapterButton.getVisibility();
        });

        // Assert: Button should be visible because item indicates chapters exist even though not loaded
        assertEquals(View.VISIBLE, visibilityAfter[0]);
    }

    @Test
    public void testRefreshAtOrAfterLastChapterHidesNextButton() throws Throwable {
        activityRule.launchActivity(new Intent());
        MainActivity activity = activityRule.getActivity();

        final int[] indexAfterBeyondDuration = new int[1];
        final int[] indexAfterAtLast = new int[1];
        final int[] nextButtonVisibilityAfterBeyond = new int[1];
        final int[] nextButtonVisibilityAfterAtLast = new int[1];

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            // Arrange fragment and view
            CoverFragment fragment = new CoverFragment();
            FrameLayout container = new FrameLayout(activity);
            int containerId = View.generateViewId();
            container.setId(containerId);
            activity.addContentView(container, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            
            // Properly attach fragment using fragment manager
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(containerId, fragment, "cover")
                    .commitNow();

            // Build chapters list
            List<Chapter> chapters = new ArrayList<>();
            chapters.add(new Chapter(0, "Ch1", null, ""));
            chapters.add(new Chapter(20000, "Ch2", null, ""));

            // Local Playable implementation adjustable in test
            class TestPlayable implements Playable {
                int duration;
                int position;
                List<Chapter> ch = chapters;

                @Override public String getEpisodeTitle() { return "Ep"; }
                @Override public List<Chapter> getChapters() { return ch; }
                @Override public String getWebsiteLink() { return null; }
                @Override public String getFeedTitle() { return "Feed"; }
                @Override public Date getPubDate() { return null; }
                @Override public Object getIdentifier() { return 1L; }
                @Override public int getDuration() { return duration; }
                @Override public int getPosition() { return position; }
                @Override public long getLastPlayedTimeStatistics() { return 0; }
                @Override public String getDescription() { return null; }
                @Override public MediaType getMediaType() { return MediaType.AUDIO; }
                @Override public String getLocalFileUrl() { return null; }
                @Override public String getStreamUrl() { return null; }
                @Override public boolean localFileAvailable() { return false; }
                @Override public void setPosition(int newPosition) { position = newPosition; }
                @Override public void setDuration(int newDuration) { duration = newDuration; }
                @Override public void setLastPlayedTimeStatistics(long lastPlayedTimestamp) { }
                @Override public void onPlaybackStart() { }
                @Override public void onPlaybackPause(Context context) { }
                @Override public void onPlaybackCompleted(Context context) { }
                @Override public int getPlayableType() { return 0; }
                @Override public void setChapters(List<Chapter> chapters) { ch = chapters; }
                @Nullable @Override public String getImageLocation() { return null; }
                @Override public int describeContents() { return 0; }
                @Override public void writeToParcel(Parcel dest, int flags) { }
            }

            TestPlayable playable = new TestPlayable();

            // Inject media into fragment
            try {
                Field mediaField = CoverFragment.class.getDeclaredField("media");
                mediaField.setAccessible(true);
                mediaField.set(fragment, playable);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // Access private refreshChapterData
            Method refresh;
            try {
                refresh = CoverFragment.class.getDeclaredMethod("refreshChapterData", int.class);
                refresh.setAccessible(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // Scenario 1: playback position beyond duration -> cap index to last and hide next button
            playable.position = 1001;
            playable.duration = 1000;
            try {
                refresh.invoke(fragment, 0);
                Field idxField = CoverFragment.class.getDeclaredField("displayedChapterIndex");
                idxField.setAccessible(true);
                indexAfterBeyondDuration[0] = idxField.getInt(fragment);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            View nextButton = fragment.getView().findViewById(R.id.butNextChapter);
            nextButtonVisibilityAfterBeyond[0] = nextButton.getVisibility();

            // Scenario 2: navigation at last chapter index -> cap index to last and hide next button
            playable.position = 500;   // within duration now
            playable.duration = 2000;
            try {
                refresh.invoke(fragment, chapters.size() - 1);
                Field idxField = CoverFragment.class.getDeclaredField("displayedChapterIndex");
                idxField.setAccessible(true);
                indexAfterAtLast[0] = idxField.getInt(fragment);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            nextButtonVisibilityAfterAtLast[0] = nextButton.getVisibility();
        });

        // Assert for both scenarios
        assertEquals("Index should be capped to last when beyond duration",
                1, indexAfterBeyondDuration[0]);
        assertEquals("Next button should be hidden (INVISIBLE) when beyond duration",
                View.INVISIBLE, nextButtonVisibilityAfterBeyond[0]);

        assertEquals("Index should be capped to last when at last chapter",
                1, indexAfterAtLast[0]);
        assertEquals("Next button should be hidden (INVISIBLE) when at last chapter",
                View.INVISIBLE, nextButtonVisibilityAfterAtLast[0]);
    }

    @Test
    public void testClickingCoverWithNullControllerNoOp() throws Throwable {
        activityRule.launchActivity(new Intent());
        MainActivity activity = activityRule.getActivity();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            // Create fragment and its view without triggering onStart (controller remains null)
            CoverFragment fragment = new CoverFragment();

            FrameLayout container = new FrameLayout(activity);
            int containerId = View.generateViewId();
            container.setId(containerId);
            activity.addContentView(container, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

            // Properly attach fragment using fragment manager
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(containerId, fragment, "cover")
                    .commitNow();

            // Click cover image; should not crash even though controller is null
            View coverView = fragment.getView().findViewById(R.id.imgvCover);
            if (coverView == null) {
                throw new AssertionError("Cover view (imgvCover) not found in layout");
            }
            coverView.performClick();
        });

        // If we reach here, no crash occurred
        assertTrue("Clicking cover with null controller should not crash", true);
    }
}

