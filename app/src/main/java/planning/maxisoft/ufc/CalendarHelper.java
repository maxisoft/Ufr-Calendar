package planning.maxisoft.ufc;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;

import com.koushikdutta.ion.Ion;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.component.VEvent;

import java.io.IOException;
import java.io.InputStream;
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
    public static final int TIMEOUT = (int) TimeUnit.MILLISECONDS.convert(15, TimeUnit.SECONDS);
    private static final Map<Context, CalendarHelper> instances = new WeakHashMap<>();

    private final Context context;
    private long calId = -1;
    private volatile Future<InputStream> lastDownloadTask;
    private volatile Future lastTask;
    private ExecutorService executor;

    private CalendarHelper(@NonNull Context context) {
        this.context = context;
        executor = Executors.newSingleThreadExecutor();
    }

    public static synchronized CalendarHelper getInstance(Context context) {
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
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL).build();
    }

    private long createDefaultCalendar() {
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Calendars.NAME, CALENDAR_NAME);
        values.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "planning des cours");
        values.put(CalendarContract.Calendars.VISIBLE, 1);
        values.put(CalendarContract.Calendars.ACCOUNT_NAME, MainActivity.class.getPackage().getName());
        values.put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
        values.put(CalendarContract.Calendars.OWNER_ACCOUNT, "ufc planning");
        values.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER);
        values.put(CalendarContract.Calendars.SYNC_EVENTS, 1);
        Uri newCal = context.getContentResolver().insert(asSyncAdapter(CalendarContract.Calendars.CONTENT_URI,
                        MainActivity.class.getPackage().getName()),
                values);
        return Long.parseLong(newCal.getLastPathSegment());
    }

    public long getCalendarsId() {
        if (calId == -1) {
            Cursor cur = null;
            try {
                cur = context.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,
                        new String[]{CalendarContract.Calendars._ID},
                        "(" + CalendarContract.Calendars.NAME + " = ?)",
                        new String[]{CALENDAR_NAME},
                        null);
                calId = cur.moveToNext() ? cur.getLong(0) : createDefaultCalendar();
            } finally {
                if (cur != null) {
                    cur.close();
                }
            }
        }
        return calId;
    }

    public Future downloadCalendar() {
        cancelLastDownload();
        if (lastTask == null || lastTask.isDone()) {
            return downloadAndSetCalendar();
        }
        return lastTask;
    }

    public boolean cancelLastDownload() {
        return lastDownloadTask != null && lastDownloadTask.cancel(true);
    }

    public synchronized void updateCalendar() {
        long id = getCalendarsId();
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Calendars.NAME, CALENDAR_NAME);
        values.put(CalendarContract.Calendars.VISIBLE, 1);
        values.put(CalendarContract.Calendars.ACCOUNT_NAME, MainActivity.class.getPackage().getName());
        values.put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL);
        values.put(
                CalendarContract.Calendars.CALENDAR_TIME_ZONE,
                TimeZone.getDefault().getDisplayName());
        int color = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(context.getString(R.string.pref_calendar_color_key), Color.BLUE);
        values.put(
                CalendarContract.Calendars.CALENDAR_COLOR,
                color);
        values.put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_READ);
        values.put(CalendarContract.Calendars.SYNC_EVENTS, 1);
        context.getContentResolver().update(asSyncAdapter(CalendarContract.Calendars.CONTENT_URI,
                        MainActivity.class.getPackage().getName()),
                values, "(" + CalendarContract.Calendars._ID + " = ?)", new String[]{String.valueOf(id)});
    }

    @SuppressWarnings("unchecked")
    public void addCalendar(@NonNull net.fortuna.ical4j.model.Calendar calendar) {
        updateCalendar();
        List<VEvent> components = calendar.getComponents(Component.VEVENT);
        addEvents(components);
    }

    private Future<InputStream> downloadAndSetCalendar() {
        lastDownloadTask = Ion.with(context)
                .load("https://sedna.univ-fcomte.fr/jsp/custom/modules/plannings/anonymous_cal.jsp?data=8241fc38732002141e9729a804a2f84be0fa50826f0818af2370d544632bbb83906f45af276f59ae8fac93f781e861524e07c487e860b173d7c57dbfb39052a3c2973627c2eb073ba38388197b1a14238d3f4109b6629391")
                .setTimeout(TIMEOUT)
                .asInputStream()
                .setCallback((e, result) -> {
                    if (result != null) {
                        lastTask = executor.submit(() -> {
                            CalendarBuilder builder = new CalendarBuilder();
                            net.fortuna.ical4j.model.Calendar calendar = null;
                            try {
                                calendar = builder.build(result);
                            } catch (IOException | ParserException e1) {
                                e1.printStackTrace();
                                return;
                            }
                            clearEvents();
                            updateCalendar();
                            addCalendar(calendar);
                        });

                    }
                });
        return lastDownloadTask;
    }

    private void addEvents(@NonNull List<VEvent> event) {
        ContentValues values[] = new ContentValues[event.size()];
        int i = 0;
        for (VEvent vEvent : event) {
            values[i] = createEventContentValues(vEvent);
            i += 1;
        }
        context.getContentResolver().bulkInsert(asSyncAdapter(CalendarContract.Events.CONTENT_URI, MainActivity.class.getPackage().getName()), values);
    }

    private ContentValues createEventContentValues(VEvent event) {
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.DTSTART, event.getStartDate().getDate().getTime());
        values.put(CalendarContract.Events.DTEND, event.getEndDate().getDate().getTime());
        values.put(CalendarContract.Events.TITLE, event.getSummary().getValue());
        values.put(CalendarContract.Events.EVENT_LOCATION, event.getLocation().getValue());
        values.put(CalendarContract.Events.CALENDAR_ID, getCalendarsId());
        values.put(CalendarContract.Events.DESCRIPTION, event.getDescription().getValue());
        values.put(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_PUBLIC);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getDisplayName());
        return values;
    }

    private void clearEvents() {
        context.getContentResolver().delete(asSyncAdapter(CalendarContract.Events.CONTENT_URI, MainActivity.class.getPackage().getName()),
                "(" + CalendarContract.Events.CALENDAR_ID + " = ?)",
                new String[]{String.valueOf(getCalendarsId())});
    }

    public void cancelTasks() {
        cancelLastDownload();
        if (lastTask != null) {
            lastTask.cancel(true);
        }
    }

    public Future getLastTask() {
        return lastTask;
    }
}
