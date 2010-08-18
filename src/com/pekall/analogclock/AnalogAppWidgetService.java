package com.pekall.analogclock;

import java.util.TimeZone;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.FontMetrics;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextPaint;
import android.text.format.Time;
import android.widget.RemoteViews;

public class AnalogAppWidgetService extends Service {
	
	private static final int DATE_TEXT_OFFSET_Y = 3;
	private static final int DATE_TEXT_FONT_SIZE = 16;
	private static final int DATE_TEXT_TOP = 122;
	
	/**
	 * @see android.app.Service#onBind(Intent)
	 */
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Put your code here
		return null;
	}
	
	private boolean mScreenOn;
	
	private Time mCalendar;

	private Drawable mCenterDot;
	private Drawable mHourHand;
	private Drawable mMinuteHand;
	private Drawable mDial;

	private int mDialWidth;
	private int mDialHeight;

	private final Handler mHandler = new Handler();
	private float mMinutes;
	private float mHour;
	private boolean mChanged;
	
	private int mYear;
	private int mMonth;
	private int mDay;
	
	private TextPaint mPaint;

	private float mDateTextY;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		mCalendar = new Time();
		mPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		mPaint.setTextSize(DATE_TEXT_FONT_SIZE);
		mPaint.setColor(getResources().getColor(android.R.color.secondary_text_dark));
		
		FontMetrics fontMetrics = mPaint.getFontMetrics();
		
		mDateTextY = DATE_TEXT_TOP - fontMetrics.top + DATE_TEXT_OFFSET_Y;

		Resources resources = getResources();
		
		mCenterDot = resources.getDrawable(R.drawable.center_dot);
		mDial = resources.getDrawable(R.drawable.dial_day);
		mHourHand = resources.getDrawable(R.drawable.clock_hour);
		mMinuteHand = resources.getDrawable(R.drawable.clock_minute);

		mDialWidth = mDial.getIntrinsicWidth();
		mDialHeight = mDial.getIntrinsicHeight();
		
		IntentFilter screenOnOffFilter = new IntentFilter();
		screenOnOffFilter.addAction(Intent.ACTION_SCREEN_ON);
		screenOnOffFilter.addAction(Intent.ACTION_SCREEN_OFF);
		
		registerReceiver(mScreenOnOffReceiver, screenOnOffFilter, null, mHandler);
		
		IntentFilter timeChangedFilter = new IntentFilter();
		timeChangedFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		timeChangedFilter.addAction(Intent.ACTION_TIME_CHANGED);
		timeChangedFilter.addAction(Intent.ACTION_TIME_TICK);
		
		registerReceiver(mTimeChangedReceiver, timeChangedFilter, null, mHandler);
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		
		setForeground(true);
		
		mCalendar.setToNow();
		// Make sure we update to the current time
		onTimeChanged();
		updateWidget();
	}
	
	private final BroadcastReceiver mScreenOnOffReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_ON))
				mScreenOn = true;
			else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
				mScreenOn = false;
		}
	};
	
	private final BroadcastReceiver mTimeChangedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
				String tz = intent.getStringExtra("time-zone");
				mCalendar = new Time(TimeZone.getTimeZone(tz).getID());
			}

			if (mScreenOn) {
				onTimeChanged();
				updateWidget();
			}	
		}
	};
	
	public void onDestroy() {
		unregisterReceiver(mScreenOnOffReceiver);
		unregisterReceiver(mTimeChangedReceiver);
		
	};
	
	private void onTimeChanged() {
		mCalendar.setToNow();

		mYear = mCalendar.year;
		mMonth = mCalendar.month + 1;
		mDay = mCalendar.monthDay;
		
		int hour = mCalendar.hour;
		int minute = mCalendar.minute;
		int second = mCalendar.second;

		mMinutes = minute + second / 60.0f;
		mHour = hour + mMinutes / 60.0f;
		mChanged = true;
	}
	
	private void updateWidget() {
		RemoteViews views = new RemoteViews(getPackageName(), R.layout.analog_appwidget);
		
		Bitmap bitmap = Bitmap.createBitmap(mDialWidth, mDialHeight,
				Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);

		boolean changed = mChanged;
		if (changed) {
			mChanged = false;
		}

		int availableWidth = mDialWidth;
		int availableHeight = mDialHeight;

		int x = availableWidth / 2;
		int y = availableHeight / 2;

		// draw dial
		final Drawable dial = mDial;
		int w = dial.getIntrinsicWidth();
		int h = dial.getIntrinsicHeight();

		boolean scaled = false;

		if (availableWidth < w || availableHeight < h) {
			scaled = true;
			float scale = Math.min((float) availableWidth / (float) w,
					(float) availableHeight / (float) h);
			canvas.save();
			canvas.scale(scale, scale, x, y);
		}

		if (changed) {
			dial.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y + (h / 2));
		}
		dial.draw(canvas);
		
		// draw date
		String dateText = String.format("%02d-%02d", mMonth, mDay);
		float dateTextWidth = mPaint.measureText(dateText);
		float dateTextX = x - (dateTextWidth / 2);
		canvas.drawText(dateText, dateTextX, mDateTextY, mPaint);

		// draw hour
		canvas.save();
		canvas.rotate(mHour / 12.0f * 360.0f, x, y);
		final Drawable hourHand = mHourHand;
		if (changed) {
			w = hourHand.getIntrinsicWidth();
			h = hourHand.getIntrinsicHeight();
			hourHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y
					+ (h / 2));
		}
		hourHand.draw(canvas);
		canvas.restore();

		// draw minute
		canvas.save();
		canvas.rotate(mMinutes / 60.0f * 360.0f, x, y);

		final Drawable minuteHand = mMinuteHand;
		if (changed) {
			w = minuteHand.getIntrinsicWidth();
			h = minuteHand.getIntrinsicHeight();
			minuteHand.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y
					+ (h / 2));
		}
		minuteHand.draw(canvas);
		canvas.restore();
		
		// draw center dot
		final Drawable centerDot = mCenterDot;
		if (changed) {
			w = centerDot.getIntrinsicWidth();
			h = centerDot.getIntrinsicHeight();
			
			centerDot.setBounds(x - (w / 2), y - (h / 2), x + (w / 2), y
					+ (h / 2));
		}
		centerDot.draw(canvas);
		

		if (scaled) {
			canvas.restore();
		}

		views.setImageViewBitmap(R.id.analog_appwidget_1, bitmap);
		
		AppWidgetManager gm = AppWidgetManager.getInstance(this);
		
        gm.updateAppWidget(new ComponentName(this, AnalogAppWidgetProvider.class), views);
	}
	
	
}
