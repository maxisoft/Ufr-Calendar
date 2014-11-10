package planning.maxisoft.ufc;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.util.Log;

import com.koushikdutta.ion.Ion;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.component.VEvent;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CalendarHelper {
    public static final String CALENDAR_NAME = "maxisoft_ufc_planning";
    public static final String OWNER_ACCOUNT = "ufc planning";
    public static final int NETWORK_TIMEOUT = (int) TimeUnit.MILLISECONDS.convert(15, TimeUnit.SECONDS);
    private static final Map<Context, CalendarHelper> instances = new WeakHashMap<>();

    private final WeakReference<Context> context;
    private long calendarId = -1;
    private volatile Future<InputStream> lastDownloadTask;
    private volatile Future lastTask;
    private ExecutorService executor;

    private CalendarHelper(Context context) {
        this.context = new WeakReference<>(context);
        executor = Executors.newSingleThreadExecutor();
    }

    public static synchronized CalendarHelper getInstance(@NonNull Context context) {
        CalendarHelper instance = instances.get(context);
        if (instance == null) {
            instance = new CalendarHelper(context);
            instances.put(context, instance);
        }
        return instance;
    }

    public static Uri asSyncAdapter(Uri uri, String account) {
        return uri.buildUpon()
                .appendQueryParameter(android.provider.CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                .build();
    }

    private Uri createCalendarSyncAdapterUri() {
        return asSyncAdapter(CalendarContract.Calendars.CONTENT_URI, CalendarHelper.class.getPackage().getName());
    }

    private Uri createEventSyncAdapterUri() {
        return asSyncAdapter(CalendarContract.Events.CONTENT_URI, CalendarHelper.class.getPackage().getName());
    }

    private long createDefaultCalendar() {
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Calendars.NAME, CALENDAR_NAME);
        values.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, getContext().getString(R.string.calendar_display_name));
        values.put(CalendarContract.Calendars.VISIBLE, 1);
        values.put(CalendarContract.Calendars.ACCOUNT_NAME, MainActivity.class.getPackage().getName());
        values.put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
        values.put(CalendarContract.Calendars.OWNER_ACCOUNT, OWNER_ACCOUNT);
        values.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
        values.put(CalendarContract.Calendars.SYNC_EVENTS, 1);
        Uri newCal = getContext().getContentResolver().insert(createCalendarSyncAdapterUri(), values);
        return Long.parseLong(newCal.getLastPathSegment());
    }

    public long getCalendarId() {
        if (calendarId == -1) {
            Cursor cur = null;
            try {
                cur = getContext().getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,
                        new String[]{CalendarContract.Calendars._ID},
                        "(" + CalendarContract.Calendars.NAME + " = ?)",
                        new String[]{CALENDAR_NAME},
                        null);
                calendarId = cur.moveToNext() ? cur.getLong(0) : createDefaultCalendar();
            } finally {
                if (cur != null) {
                    cur.close();
                }
            }
        }
        return calendarId;
    }

    public Future downloadCalendar() {
        cancelLastDownload();
        if (!isLastTaskRunning()) {
            return downloadAndSetCalendar();
        }
        return lastTask;
    }

    public boolean isLastTaskRunning() {
        return lastTask != null && !lastTask.isDone();
    }

    public boolean isLastDownloadRunning() {
        return lastDownloadTask != null && !lastDownloadTask.isDone();
    }

    public boolean cancelLastDownload() {
        return lastDownloadTask != null && lastDownloadTask.cancel(true);
    }

    public synchronized void updateCalendar() {
        long id = getCalendarId();
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Calendars.NAME, CALENDAR_NAME);
        values.put(CalendarContract.Calendars.VISIBLE, 1);
        values.put(CalendarContract.Calendars.ACCOUNT_NAME, MainActivity.class.getPackage().getName());
        values.put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
        values.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, R.string.calendar_display_name);
        values.put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().getDisplayName());
        int color = getDefaultSharedPreferences().getInt(getContext().getString(R.string.pref_calendar_color_key), Color.BLUE);
        values.put(CalendarContract.Calendars.CALENDAR_COLOR, color);
        values.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
        values.put(CalendarContract.Calendars.SYNC_EVENTS, 1);
        getContext().getContentResolver().update(createCalendarSyncAdapterUri(),
                values,
                "(" + CalendarContract.Calendars._ID + " = ?)",
                new String[]{String.valueOf(id)});
    }

    private SharedPreferences getDefaultSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(getContext());
    }

    @SuppressWarnings("unchecked")
    public void addCalendar(@NonNull net.fortuna.ical4j.model.Calendar calendar) {
        updateCalendar();
        List<VEvent> components = calendar.getComponents(Component.VEVENT);
        addEvents(components);
    }

    private Future<InputStream> downloadAndSetCalendar() {
        Log.w("test", getDefaultSharedPreferences().getString("calendar_url", null));
        lastDownloadTask = Ion.with(getContext())
                .load(getDefaultSharedPreferences().getString("calendar_url", null))
                .noCache()
                .setTimeout(NETWORK_TIMEOUT)
                .asInputStream()
                .setCallback((e, result) -> {
                    if (result != null) {
                        lastTask = executor.submit(() -> {
                            CalendarBuilder builder = new CalendarBuilder();
                            net.fortuna.ical4j.model.Calendar calendar = null;
                            try {
                                calendar = builder.build(result);
                            } catch (IOException | ParserException e1) {
                                Log.e(CalendarHelper.class.getSimpleName(), "can't parse calendar", e1);
                                return;
                            }
                            clearEvents();
                            updateCalendar();
                            addCalendar(calendar);
                        });
                    } else if (e != null) {
                        Log.e(CalendarHelper.class.getSimpleName(), "can't download calendar", e);
                    }
                });
        return lastDownloadTask;
    }

    private void addEvents(List<VEvent> events) {
        ContentValues values[] = new ContentValues[events.size()];
        int i = 0;
        for (VEvent event : events) {
            values[i] = createEventContentValues(event);
            i += 1;
        }
        getContext().getContentResolver().bulkInsert(createEventSyncAdapterUri(), values);
    }

    private ContentValues createEventContentValues(VEvent event) {
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, event.getStartDate().getDate().getTime());
        values.put(CalendarContract.Events.DTEND, event.getEndDate().getDate().getTime());
        values.put(CalendarContract.Events.TITLE, event.getSummary().getValue());
        values.put(CalendarContract.Events.EVENT_LOCATION, event.getLocation().getValue());
        values.put(CalendarContract.Events.CALENDAR_ID, getCalendarId());
        values.put(CalendarContract.Events.DESCRIPTION, event.getDescription().getValue());
        values.put(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_PUBLIC);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getDisplayName());
        return values;
    }

    private void clearEvents() {
        getContext().getContentResolver().delete(createEventSyncAdapterUri(),
                "(" + CalendarContract.Events.CALENDAR_ID + " = ?)",
                new String[]{String.valueOf(getCalendarId())});

    }

    public void cancelTasks() {
        cancelLastDownload();
        if (lastTask != null) {
            lastTask.cancel(true);
        }
    }

    public boolean isRunningTask(){
        return isLastDownloadRunning() || isLastTaskRunning();
    }

    private Context getContext() {
        return context.get();
    }

    public Future getLastTask() {
        return lastTask;
    }
}
