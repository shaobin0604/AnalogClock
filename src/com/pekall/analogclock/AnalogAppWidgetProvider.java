package com.pekall.analogclock;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AnalogAppWidgetProvider extends AppWidgetProvider {

    static final String TAG = AnalogAppWidgetProvider.class.getSimpleName();


	@Override
	public void onDisabled(Context context) {
		Log.d(TAG, "[onDisabled]");
		context.stopService(new Intent(context, AnalogAppWidgetService.class));
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		Log.d(TAG, "[onUpdate]");
		context.startService(new Intent(context, AnalogAppWidgetService.class));
	}
}
