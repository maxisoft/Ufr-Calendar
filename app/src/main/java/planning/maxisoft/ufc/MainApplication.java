package planning.maxisoft.ufc;

import android.app.Application;

public class MainApplication extends Application {
    private static MainApplication instance;
    private CalendarHelper calendarHelper;
    private Alarm alarm;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        calendarHelper = CalendarHelper.getInstance();
        this.alarm = new Alarm(this);
    }

    public Alarm getAlarm() {
        return alarm;
    }

    public CalendarHelper getCalendarHelper() {
        return calendarHelper;
    }

    public static MainApplication getInstance() {
        return instance;
    }
}
