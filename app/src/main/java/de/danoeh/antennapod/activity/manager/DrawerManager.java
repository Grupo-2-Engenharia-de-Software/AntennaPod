package de.danoeh.antennapod.activity.manager;

import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import de.danoeh.antennapod.R;

public class DrawerManager {
    private final Context context;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private View navDrawer;

    public DrawerManager(Context context, DrawerLayout drawerLayout, View navDrawer) {
        this.context = context;
        this.drawerLayout = drawerLayout;
        this.navDrawer = navDrawer;
    }

    public void setupDrawer() {
        if (drawerLayout != null) {
            setNavDrawerSize();
        }
    }

    public boolean isDrawerOpen() {
        return drawerLayout != null && navDrawer != null && drawerLayout.isDrawerOpen(navDrawer);
    }

    public void closeDrawer() {
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(navDrawer);
        }
    }

    private void setNavDrawerSize() {
        if (drawerLayout == null) {
            return;
        }

        float screenPercent = context.getResources().getInteger(R.integer.nav_drawer_screen_size_percent) * 0.01f;
        int width = (int) (getScreenWidth() * screenPercent);
        int maxWidth = (int) context.getResources().getDimension(R.dimen.nav_drawer_max_screen_size);

        navDrawer.getLayoutParams().width = Math.min(width, maxWidth);
    }

    private int getScreenWidth() {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.widthPixels;
    }

    public void syncState() {
        if (drawerToggle != null) {
            drawerToggle.syncState();
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        if (drawerToggle != null) {
            drawerToggle.onConfigurationChanged(newConfig);
        }
        setNavDrawerSize();
    }

    public void setDrawerToggle(ActionBarDrawerToggle toggle) {
        this.drawerToggle = toggle;
    }

    public ActionBarDrawerToggle getDrawerToggle() {
        return drawerToggle;
    }

    public void removeDrawerListener() {
        if (drawerLayout != null && drawerToggle != null) {
            drawerLayout.removeDrawerListener(drawerToggle);
        }
    }
}