package planning.maxisoft.ufc;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

/**
 * A placeholder fragment containing a simple view.
 */
public class CalendarUrlFragment extends Fragment {

    public static final int REQUEST_CODE = 0x6516;
    private CalendarUrlFragmentListener listener;
    private MenuItem okItem;

    @InjectView(R.id.url_edit_text)
    EditText urlEditText;

    public static boolean isValidPlanningUrl(String url){
        return url.trim().startsWith("https://sedna.univ-fcomte.fr/jsp/custom/modules/plannings/anonymous_cal.jsp?data=");
    }

    @OnClick(R.id.qr_code_img_view)
    void getQrCode(){
        try {
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // "PRODUCT_MODE for bar codes
            startActivityForResult(intent, REQUEST_CODE);
        } catch (Exception e) {
            Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
            Intent marketIntent = new Intent(Intent.ACTION_VIEW,marketUri);
            startActivity(marketIntent);
        }
    }

    public CalendarUrlFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_calendar_url, container, false);
        ButterKnife.inject(this, rootView);
        urlEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                getActivity().invalidateOptionsMenu();
            }
        });
        String calendarUrl = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("calendar_url", null);
        if (calendarUrl != null){
            urlEditText.setText(calendarUrl);
            getActivity().invalidateOptionsMenu();
        }
        return rootView;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (okItem != null){
            menu.removeItem(okItem.getItemId());
            okItem = null;
        }
        if (isValidPlanningUrl(urlEditText.getText().toString())){
            okItem = menu.add(R.string.ok_settings);

            okItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            okItem.setIcon(R.drawable.ic_done);
            okItem.setOnMenuItemClickListener(item -> {
                listener.urlUpdated(urlEditText.getText().toString());
                return true;
            });
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        try{
            listener = (CalendarUrlFragmentListener) getActivity();
        }catch (ClassCastException e){
            throw new IllegalStateException(getActivity().getClass() + " must implement " + CalendarUrlFragmentListener.class, e);
        }
        setHasOptionsMenu(true);
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE){
            if (resultCode == Activity.RESULT_OK) {
                String contents = data.getStringExtra("SCAN_RESULT");
                urlEditText.setText(contents);
                if (isValidPlanningUrl(contents)){
                    listener.urlUpdated(contents);
                }
            }
        }
    }

    static interface CalendarUrlFragmentListener {
        void urlUpdated(String url);
    }
}
