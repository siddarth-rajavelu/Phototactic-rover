package org.opencv.samples.tutorial3;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.IBinder;

/**
 * Following is an IOIO service. This service will attempt to
 * connect to a IOIO and set the pins 3 & 4 to control the motors. 
 * A notification will appear on the
 * notification bar, enabling the user to stop the service.
 */



public class MotorControlIOIOService extends IOIOService {
	@Override
	protected IOIOLooper createIOIOLooper() {
		return new BaseIOIOLooper() {
			private DigitalOutput led_,LMP,RMP;
			
			@Override
			protected void setup() throws ConnectionLostException,
					InterruptedException {
				led_ = ioio_.openDigitalOutput(IOIO.LED_PIN);
				LMP = ioio_.openDigitalOutput(3,false);
				RMP =ioio_.openDigitalOutput(4,false);
				}

			@Override
			public void loop() throws ConnectionLostException,
					InterruptedException {
				led_.write(PhototacticActivity.ledoff);
				//Log.i("pin",String.valueOf(Tutorial3Activity.LM));
				LMP.write(PhototacticActivity.LM);
				RMP.write(PhototacticActivity.RM);
				Thread.sleep(1000);
			}
					};
		
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (intent != null && intent.getAction() != null
				&& intent.getAction().equals("stop")) {
			// User clicked the notification. Need to stop the service.
			nm.cancel(0);
			stopSelf();
		} else {
			// Service starting. Create a notification.
			@SuppressWarnings("deprecation")
			Notification notification = new Notification(
					R.drawable.icon, "IOIO service running",
					System.currentTimeMillis());
			notification
					.setLatestEventInfo(this, "IOIO Service", "Click to stop",
							PendingIntent.getService(this, 0, new Intent(
									"stop", null, this, this.getClass()), 0));
			notification.flags |= Notification.FLAG_ONGOING_EVENT;
			nm.notify(0, notification);
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

}
