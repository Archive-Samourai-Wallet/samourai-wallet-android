package com.samourai.wallet.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.transition.MaterialSharedAxis;
import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiActivity;
import com.samourai.wallet.payload.ExternalBackupManager;
import com.samourai.wallet.util.AppUtil;

public class SettingsActivity extends SamouraiActivity {

    enum ActiveFragment {MAIN, SETTING}

    private ActiveFragment activeFragment = ActiveFragment.MAIN;
    private final MainSettingsFragment mainSettingsFragment = new MainSettingsFragment();
    private PreferenceFragmentCompat currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_settings);
        setSupportActionBar(findViewById(R.id.toolbar_settings));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setFragment(mainSettingsFragment, true);
    }

    public void setFragment(PreferenceFragmentCompat preference, boolean entering) {
        currentFragment = preference;
        MaterialSharedAxis transition = new MaterialSharedAxis(MaterialSharedAxis.Y, entering);
        preference.setEnterTransition(transition);
        if (preference instanceof MainSettingsFragment) {
            ((MainSettingsFragment) preference).setTargetTransition(transition);
            this.activeFragment = ActiveFragment.MAIN;
            setTitle(R.string.action_settings);
        } else {
            ((SettingsDetailsFragment) preference).setTargetTransition(transition);
            this.activeFragment = ActiveFragment.SETTING;
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame,
                        preference)
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        AppUtil.getInstance(SettingsActivity.this).setIsInForeground(true);

        AppUtil.getInstance(SettingsActivity.this).checkTimeOut();

    }

    @Override
    public void onBackPressed() {
        if (this.activeFragment == ActiveFragment.SETTING) {
            setFragment(mainSettingsFragment, false);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ExternalBackupManager.onActivityResult(requestCode, resultCode, data, getApplication());
    }
}
