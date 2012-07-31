package edu.benlerner.perfectshuffle;

import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

public class Options extends Fragment {
  private static final int HELLO_ID = 1;
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.options, null);
    final Context context = this.getActivity().getApplicationContext();
    final NotificationManager mNotificationManager = (NotificationManager)this.getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
    Intent notificationIntent = new Intent();
    final PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
    view.findViewById(R.id.btnClearCache).setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        int icon = android.R.drawable.ic_dialog_info;
        CharSequence tickerText = "Clear cache clicked!";
        long when = System.currentTimeMillis();
        Notification notification = new Notification(icon, tickerText, when);
        notification.setLatestEventInfo(context, "The Perfect Shuffle", "Clicked the Clear Cache button", contentIntent);
        mNotificationManager.notify(HELLO_ID, notification);
        
        Toast toast = Toast.makeText(context, "Clicked on the Clear Cache button", Toast.LENGTH_LONG);
        toast.show();
      }
    });
    return view;
  }
}
