package planning.maxisoft.ufc;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;

import java.io.IOException;
import java.net.URL;

public class UpdatePlanningService extends Service {
    public UpdatePlanningService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int ret = super.onStartCommand(intent, flags, startId);
        logcalendar();
        return ret;
    }

    private void logcalendar() {
        new Thread(() -> {
            CalendarBuilder builder = new CalendarBuilder();
            String spec = "https://sedna.univ-fcomte.fr/jsp/custom/modules/plannings/anonymous_cal.jsp?data=8241fc38732002141e9729a804a2f84be0fa50826f0818af2370d544632bbb83906f45af276f59ae8fac93f781e861524e07c487e860b173d7c57dbfb39052a3c2973627c2eb073ba38388197b1a14238d3f4109b6629391";
            try {
                Calendar calendar = builder.build(new URL(spec).openStream());
                Log.i(getString(R.string.app_name), String.valueOf(calendar));
            } catch (IOException | ParserException e) {
                Log.e(getString(R.string.app_name), "", e);
            }
        }
        ).start();
    }
}
