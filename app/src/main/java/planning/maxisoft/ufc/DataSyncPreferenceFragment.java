package planning.maxisoft.ufc;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * This fragment shows data and sync preferences only. It is used when the
 * activity is showing a two-pane settings UI.
 */
public class DataSyncPreferenceFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_data_sync);
        SettingFragment.bindPreferenceSummaryToValue(findPreference("sync_frequency"));
    }
}
