package planning.maxisoft.ufc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.View;

import com.github.machinarius.preferencefragment.PreferenceFragment;

import net.margaritov.preference.colorpicker.ColorPickerPreference;


/**
 * A {@link PreferenceFragment} .
 * Use the {@link SettingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingFragment extends PreferenceFragment {
    public static final int REQUEST_CODE = 0x685;

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener bindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #bindPreferenceSummaryToValueListener
     */
    public static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(bindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        bindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SettingFragment.
     */
    public static SettingFragment newInstance() {
        SettingFragment fragment = new SettingFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public SettingFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupSimplePreferencesScreen();
        Preference pref = findPreference("calendar_url");
        pref.setOnPreferenceClickListener(preference -> {
            startActivityForPlanningUrl();
            return true;
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //autostart activity if first time
        if (getPreferenceManager().getSharedPreferences().getString("calendar_url", null) == null){
            startActivityForPlanningUrl();
        }
    }

    private void startActivityForPlanningUrl() {
        Intent intent = new Intent(getActivity(), CalendarUrlActivity.class);
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE){
            if (resultCode == Activity.RESULT_OK) {
                if(data != null){
                    String url = data.getStringExtra(CalendarUrlActivity.PLANNING_URL_DATA_KEY);
                    if (url != null){
                        getPreferenceManager().getSharedPreferences()
                                .edit()
                                .putString("calendar_url", url.trim())
                                .commit();
                    }
                }
            }else{
                getActivity().finish();
            }
        }
    }

    /**
     * Shows the simplified settings UI if the device configuration if the
     * device configuration dictates that a simplified, single-pane UI should be
     * shown.
     */
    private void setupSimplePreferencesScreen() {

        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.pref_general);

        ColorPickerPreference preference = createColorPickerPreference();
        getPreferenceScreen().addPreference(preference);

        // Add 'data and sync' preferences, and a corresponding header.
        PreferenceCategory fakeHeader = new PreferenceCategory(getActivity());
        fakeHeader.setTitle(R.string.pref_header_data_sync);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_data_sync);

        bindPreferenceSummaryToValue(findPreference("sync_frequency"));

    }

    private ColorPickerPreference createColorPickerPreference() {
        ColorPickerPreference colorPickerPreference = new ColorPickerPreference(getActivity());
        colorPickerPreference.setDefaultValue(Color.BLUE);
        colorPickerPreference.setTitle(R.string.pref_calendar_color_title);
        colorPickerPreference.setKey(getString(R.string.pref_calendar_color_key));
        colorPickerPreference.setSummary(R.string.pref_calendar_color_summary);
        colorPickerPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            CalendarHelper.getInstance().updateCalendar();
            return false;
        });
        return colorPickerPreference;
    }

}
