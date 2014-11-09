package planning.maxisoft.ufc;

import android.app.Fragment;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.koushikdutta.ion.Ion;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;

import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.Future;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class MainActivity extends ActionBarActivity implements SwipeRefreshLayout.OnRefreshListener {

    @InjectView(R.id.swipe_container)
    SwipeRefreshLayout swipeLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.swipe_container, SettingFragment.newInstance())
                    .commit();
        }
        ButterKnife.inject(this);

        swipeLayout.setOnRefreshListener(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_agenda){
            startIntentCalendar();
        }
        else if (id == R.id.action_refresh){
            swipeLayout.setRefreshing(true);
            onRefresh();
        }

        return super.onOptionsItemSelected(item);
    }

    private void startIntentCalendar() {
        Uri.Builder builder = CalendarContract.CONTENT_URI.buildUpon();
        builder.appendPath("time");
        ContentUris.appendId(builder, System.currentTimeMillis());
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setData(builder.build());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CalendarHelper.getInstance(this).cancelTasks();
    }

    @Override
    public void onRefresh() {
        CalendarHelper calendarHelper = CalendarHelper.getInstance(this);
        Future future = calendarHelper.downloadCalendar();
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Future lastTask = calendarHelper.getLastTask();
                swipeLayout.setColorSchemeColors(future.isDone() ? Color.GREEN : Color.BLUE);
                if (future.isDone() && lastTask != null && lastTask.isDone()) {
                    swipeLayout.setRefreshing(false);
                    swipeLayout.setColorSchemeColors(Color.BLACK);
                } else {
                    new Handler().postDelayed(this, 200);
                }
            }
        });
    }
}
