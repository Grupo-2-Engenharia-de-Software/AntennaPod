package de.danoeh.antennapod.activity;

import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import androidx.core.graphics.Insets;
import androidx.fragment.app.FragmentContainerView;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.playback.service.PlaybackServiceInterface;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.ui.common.IntentUtils;
import de.danoeh.antennapod.ui.screen.playback.audio.AudioPlayerFragment;
import de.danoeh.antennapod.ui.view.LockableBottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import androidx.annotation.NonNull;

public class PlayerStateManager {
    private final MainActivity activity;
    private LockableBottomSheetBehavior<View> sheetBehavior;
    private View playerView;
    private Insets systemBarInsets;
    private BottomSheetBehavior.BottomSheetCallback bottomSheetCallback;

    public PlayerStateManager(MainActivity activity) {
        this.activity = activity;
        this.bottomSheetCallback = createBottomSheetCallback();
    }

    public void initialize(View bottomSheet) {
        sheetBehavior = (LockableBottomSheetBehavior<View>) BottomSheetBehavior.from(bottomSheet);
        sheetBehavior.setHideable(false);
        playerView = bottomSheet;
        sheetBehavior.addBottomSheetCallback(bottomSheetCallback);
    }

    public void setSystemBarInsets(Insets insets) {
        this.systemBarInsets = insets;
        updatePlayerVisibility();
    }

    public void setPlayerVisible(boolean visible) {
        sheetBehavior.setLocked(!visible);
        playerView.setVisibility(visible ? View.VISIBLE : View.GONE);

        if (visible) {
            bottomSheetCallback.onStateChanged(null, sheetBehavior.getState());
        } else {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
        updatePlayerVisibility();
    }

    private void updatePlayerVisibility() {
        View bottomPaddingView = activity.findViewById(R.id.bottom_padding);
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) bottomPaddingView.getLayoutParams();
        params.height = systemBarInsets.bottom;
        bottomPaddingView.setLayoutParams(params);

        int externalPlayerHeight = (int) activity.getResources().getDimension(R.dimen.external_player_height);
        FragmentContainerView mainView = activity.findViewById(R.id.main_content_view);
        params = (ViewGroup.MarginLayoutParams) mainView.getLayoutParams();
        params.setMargins(systemBarInsets.left, 0, systemBarInsets.right,
                (playerView.getVisibility() == View.VISIBLE ? externalPlayerHeight : 0));
        mainView.setLayoutParams(params);

        sheetBehavior.setPeekHeight(externalPlayerHeight);
        sheetBehavior.setHideable(true);
        sheetBehavior.setGestureInsetBottomIgnored(true);

        FragmentContainerView playerContainer = activity.findViewById(R.id.playerFragment);
        ViewGroup.MarginLayoutParams playerParams = (ViewGroup.MarginLayoutParams) playerContainer.getLayoutParams();
        playerParams.setMargins(systemBarInsets.left, 0, systemBarInsets.right, 0);
        playerContainer.setLayoutParams(playerParams);

        RelativeLayout playerContent = activity.findViewById(R.id.playerContent);
        playerContent.setPadding(systemBarInsets.left, systemBarInsets.top, systemBarInsets.right, 0);
    }

    public LockableBottomSheetBehavior<View> getBottomSheet() {
        return sheetBehavior;
    }

    public BottomSheetBehavior.BottomSheetCallback getBottomSheetCallback() {
        return bottomSheetCallback;
    }

    private BottomSheetBehavior.BottomSheetCallback createBottomSheetCallback() {
        return new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View view, int state) {
                if (state == BottomSheetBehavior.STATE_COLLAPSED) {
                    onSlide(view, 0.0f);
                } else if (state == BottomSheetBehavior.STATE_EXPANDED) {
                    onSlide(view, 1.0f);
                } else if (state == BottomSheetBehavior.STATE_HIDDEN) {
                    IntentUtils.sendLocalBroadcast(activity,
                            PlaybackServiceInterface.ACTION_SHUTDOWN_PLAYBACK_SERVICE);
                    PlaybackPreferences.writeNoMediaPlaying();
                    setPlayerVisible(false);
                }
            }

            @Override
            public void onSlide(@NonNull View view, float slideOffset) {
                AudioPlayerFragment audioPlayer = (AudioPlayerFragment) activity.getSupportFragmentManager()
                        .findFragmentByTag(AudioPlayerFragment.TAG);
                if (audioPlayer == null) {
                    return;
                }

                if (slideOffset == 0.0f) {
                    audioPlayer.scrollToPage(AudioPlayerFragment.POS_COVER);
                }
                audioPlayer.fadePlayerToToolbar(slideOffset);
            }
        };
    }
}