package com.own.sample;

import android.app.LauncherActivity;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Toast;

public class CustomOnItemSelectedListener implements OnItemSelectedListener {

	public static final int ALARM_MODE_CUSTOM_POSITION = 0;
	public static final int ALARM_MODE_DEFAULT_RINGTONE = 1;

	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
		if (SampleVoiceRecognizationActivity.DEBUG)
			Toast.makeText(
					parent.getContext(),
					"OnItemSelectedListener : "
							+ parent.getItemAtPosition(pos).toString() + " "
							+ pos, Toast.LENGTH_SHORT).show();

		SampleVoiceRecognizationActivity.getInst().switchForAlarmModes(pos);
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
	}

}
