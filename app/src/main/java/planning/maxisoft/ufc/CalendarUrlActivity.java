package planning.maxisoft.ufc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;


public class CalendarUrlActivity extends ActionBarActivity implements CalendarUrlFragment.CalendarUrlFragmentListener {
    public static final String PLANNING_URL_DATA_KEY = "PLANNING_URL";
    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_url);
        url = PreferenceManager.getDefaultSharedPreferences(this).getString("calendar_url", null);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new CalendarUrlFragment())
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (url == null){
            finish();
        }else{
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_calendar_url, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void urlUpdated(String url) {
        this.url = url;
        finish();
    }

    @Override
    public void finish() {
        if (url != null){
            Intent data = new Intent();
            data.putExtra(PLANNING_URL_DATA_KEY, url);
            if (getParent() == null) {
                setResult(Activity.RESULT_OK, data);
            } else {
                getParent().setResult(Activity.RESULT_OK, data);
            }
        }else{
            if (getParent() == null) {
                setResult(Activity.RESULT_CANCELED);
            } else {
                getParent().setResult(Activity.RESULT_CANCELED);
            }
        }
        super.finish();
    }
}


