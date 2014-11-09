package planning.maxisoft.ufc;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompleted extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
            Intent itent = new Intent(context, UpdatePlanningService.class);
            context.startService(itent);
        }
    }
}
