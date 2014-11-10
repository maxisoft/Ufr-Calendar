package planning.maxisoft.ufc;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

public class Alarm extends BroadcastReceiver {

    private Context context;

    public Alarm(){
        this(null);
    }

    public Alarm(Context context){
        this.context = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        CalendarHelper instance = CalendarHelper.getInstance();
        if (!instance.isRunningTask()){
            instance.downloadCalendar();
        }
        Log.w(Alarm.class.getSimpleName(), "Received event");
    }

    public void SetAlarm() {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, Alarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        long interval = Long.parseLong(getDefaultSharedPreferences().getString("sync_frequency", "-1"));
        if (interval > 0){
            interval = TimeUnit.MILLISECONDS.convert(interval, TimeUnit.MINUTES);
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, interval / 2 + 2 * 60 * 1000, interval, pi);
        }
    }

    private SharedPreferences getDefaultSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void CancelAlarm() {
        Intent intent = new Intent(context, Alarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }

    public void resetAlarm(){
        CancelAlarm();
        SetAlarm();
    }
}