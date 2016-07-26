package com.own.sample;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.Adler32;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.SyncStateContract.Constants;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.own.sample.MyService.LocalBinder;
///http://www.truiton.com/2014/06/android-speech-recognition-without-dialog-custom-activity/
//http://code4reference.com/2012/07/tutorial-android-voice-recognition/
//http://www.learn2crack.com/2013/12/android-speech-recognition-example.html
//http://stackoverflow.com/questions/6348829/voice-recognition-commands-android
//http://android.stackexchange.com/questions/54424/usb-connectivity-problems-with-nexus-7-2013
//http://android-developers.blogspot.in/2009/09/introduction-to-text-to-speech-in.html

public class SampleVoiceRecognizationActivity extends Activity implements
		OnInitListener {
	/** Called when the activity is first created. */
	private static final int REQUEST_CODE = 1234;
	static final boolean DEBUG = false;
	private static final int MY_DATA_CHECK_CODE = 111;
	Button mStartStop;
	TextView Speech;
	Dialog match_text_dialog;
	ListView textlist;
	ArrayList<String> matches_text;
	String myString = "where are you";
	Uri ringtoneUri;
	Ringtone ringtoneSound;
	private TextToSpeech mTts;

	SharedPreferences recognizerPreference;
	SharedPreferences alarmModePreference;

	public static String SERVICE_STATE_PREFS = "service_state";
	public static String ALARM_MODE_PREFS = "alarm_mode";
	public static String CUSTOM_TEXT_PREFS = "custom_text";
	public static String RESPONSE_TEXT_PREFS = "response_text";

	boolean mServiceState;

	private Spinner mAlarmModeSpinner;
	private Spinner mCustomTextSpinner;
	private TextView mDefaultRingtone;
	private EditText mResponseText;
	LinearLayout mResponseTextLayout;
	Button mResponseTextButton;
	public static SampleVoiceRecognizationActivity mInst;

	public SampleVoiceRecognizationActivity() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

		mInst = this;
		ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
		ringtoneSound = RingtoneManager.getRingtone(getApplicationContext(),
				ringtoneUri);
		/*
		 * Intent checkIntent = new Intent();
		 * checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		 * startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);
		 */// /tts
		// preferences
		recognizerPreference = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		mServiceState = recognizerPreference.getBoolean(SERVICE_STATE_PREFS,
				false);
		mAlarmMode = recognizerPreference.getInt(ALARM_MODE_PREFS, 0);
		mCustomTextPos = recognizerPreference.getInt(CUSTOM_TEXT_PREFS, 0);

		mResponseTextLayout = (LinearLayout) findViewById(R.id.responseLayout);
		mDefaultRingtone = (TextView) findViewById(R.id.defaultRingTone);
		mResponseText = (EditText) findViewById(R.id.responseSpeechText);
		mResponseTextButton = (Button) findViewById(R.id.responseTextButton);
		mResponseText.setText(recognizerPreference.getString(
				RESPONSE_TEXT_PREFS, "I am here sir"));
		if (mResponseText.isEnabled()) {
			mResponseTextButton.setText("Set");
		} else {
			mResponseTextButton.setText("Edit");
		}

		// Alarm Mode Selection
		addItemsOnAlarmModeSpinner();
		addListenerOnSpinnerItemSelection();
if (DEBUG)
	Log.d("Check", "SampleVoiceRecognizationActivity::onCreate adding custom listner");
		addItemsOnCustomTextSpinner();
		addListenerOnSpinnerCustomTextItemSelection();
		if (DEBUG)
			Log.d("Check",
					"SampleVoiceRecognizationActivity::onCreate mServiceState "
							+ mServiceState);
		mStartStop = (Button) findViewById(R.id.button1);
		// Stop = (Button) findViewById(R.id.stopService);
		Speech = (TextView) findViewById(R.id.text1);

		if (mServiceState)
			mStartStop.setText("Stop Finder");
		else
			mStartStop.setText("Enable Finder");
		mStartStop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				/*
				 * if(ringtoneSound.isPlaying()) {
				 * Speech.setText("Enable Speak"); stopAlarm(); } else {
				 */
				mServiceState = recognizerPreference.getBoolean(
						SERVICE_STATE_PREFS, false);
				if (DEBUG)
					Log.d("Check",
							"SampleVoiceRecognizationActivity.onCreate(...).new OnClickListener() {...}::onClick 1 mService "
									+ mServiceState);
				if (!mServiceState ) {
					if (true) {
						/*
						 * Intent intent = new Intent(
						 * RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
						 * intent.putExtra
						 * (RecognizerIntent.EXTRA_LANGUAGE_MODEL,
						 * RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
						 * startActivityForResult(intent, REQUEST_CODE);
						 */// previous code do not touch

						Intent in = new Intent();
						in.setClass(getApplicationContext(), MyService.class);
						startService(in);
						recognizerPreference.edit()
								.putBoolean(SERVICE_STATE_PREFS, true).commit();
						mStartStop.setText("Stop finder");

						// service should be bound in start so we shud not worry
						myServiceBinder.setRecognizerEnabledOrNot(true);

					} else {
						Toast.makeText(getApplicationContext(),
								"Plese wait", Toast.LENGTH_LONG)
								.show();
					}

				} else {
					recognizerPreference.edit()
							.putBoolean(SERVICE_STATE_PREFS, false).commit();
					myServiceBinder.setRecognizerEnabledOrNot(false);
					mStartStop.setText("Enable finder");
					Intent in = new Intent();
					in.setClass(getApplicationContext(), MyService.class);
					// disabling service toatally for that unbind and stop both
					// shud be called
					stopService(in);
				}
			}
			// }
		});

		mResponseText.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (DEBUG)
					Log.d("Check",
							"SampleVoiceRecognizationActivity.onCreate(...).new OnClickListener() {...}::onClick---");
				// TODO Auto-generated method stub

			}
		});

		mResponseTextButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (mResponseText.isEnabled()) {
					myServiceBinder.changeResponseString(mResponseText
							.getText().toString());
					recognizerPreference
							.edit()
							.putString(RESPONSE_TEXT_PREFS,
									mResponseText.getText().toString())
							.commit();
					mResponseTextButton.setText("Edit");
					mResponseText.setEnabled(false);
				} else {
					mResponseTextButton.setText("Set");
					mResponseText.setEnabled(true);

				}
				if (DEBUG)
					Log.d("Check",
							"SampleVoiceRecognizationActivity.onCreate(...).new OnClickListener() {...}::onClick");

			}
		});
	}

	@Override
	protected void onStart() {
		if (DEBUG)
			Log.d("Check", "SampleVoiceRecognizationActivity::onStart");
		// TODO Auto-generated method stub
		// sarting service to communicate with activity feature not enabled
		start();// untill stop we shud have the binder to communicate // study
				// service lifecycles if doubtfull
		super.onStart();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		if (DEBUG)
			Log.d("Check", "SampleVoiceRecognizationActivity::onResume");
		super.onResume();
	}

	@Override
	protected void onPause() {
		if (DEBUG)
			Log.d("Check", "SampleVoiceRecognizationActivity::onPause");
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		if (DEBUG)
			Log.d("Check", "SampleVoiceRecognizationActivity::onStop");
		// stoping service to not communicate with activity feature if enabled
		// will not be disabled
		if (mServiceIsBound)
			stop();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if (DEBUG)
			Log.d("Check", "SampleVoiceRecognizationActivity::onDestroy");
		super.onDestroy();
	}

	public boolean isConnected() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo net = cm.getActiveNetworkInfo();
		if (net != null && net.isAvailable() && net.isConnected()) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (DEBUG)
			Log.d("Check",
					"SampleVoiceRecognizationActivity::onActivityResult requestCode "
							+ requestCode + "  resultCode " + resultCode);
		if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {

			matches_text = data
					.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);// to
																				// show
																				// the
																				// dialog
																				// for
																				// related
																				// texts
			// showDialogWithrelatedTexts(matches_text);
			Log.d("Check", "SampleVoiceRecognizationActivity::onActivityResult");
			matchText(matches_text);
		} else if (requestCode == MY_DATA_CHECK_CODE) {
			if (DEBUG)
				Log.d("Check",
						"SampleVoiceRecognizationActivity::onActivityResult my cehc code");
			// if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
			// success, create the TTS instance
			// mTts = new TextToSpeech(this, this);
			// myServiceBinder.setTts(mTts);
			/*
			 * } else { if (DEBUG) Log.d("Check",
			 * "SampleVoiceRecognizationActivity::onActivityResult else"); //
			 * missing data, install it Intent installIntent = new Intent();
			 * installIntent.setAction(
			 * TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
			 * startActivity(installIntent); }
			 */
		}
		super.onActivityResult(requestCode, resultCode, data);

	}

	private void showDialogWithrelatedTexts(final ArrayList<String> matches_text) {
		match_text_dialog = new Dialog(this);
		match_text_dialog.setContentView(R.layout.dialog_matches_frag);
		match_text_dialog.setTitle("Select Matching Text");
		textlist = (ListView) match_text_dialog.findViewById(R.id.list);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, matches_text);
		textlist.setAdapter(adapter);
		textlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				Speech.setText("You have said " + matches_text.get(position));
				match_text_dialog.hide();
			}
		});
		match_text_dialog.show();

	}

	private void matchText(ArrayList<String> matches_text) {
		if (DEBUG)
			Log.d("Check",
					"SampleVoiceRecognizationActivity::matchText matches_text "
							+ matches_text + "   " + mTts);
		if (matches_text.contains(myString)) {
			Toast.makeText(getBaseContext(), "your text found",
					Toast.LENGTH_LONG).show();
			triggerAlarm();
			Speech.setText("Stop Alarm");

		} else {
			Toast.makeText(getBaseContext(), "your text not found",
					Toast.LENGTH_LONG).show();
		}
	}

	private AlarmManager alarmMgr;
	private PendingIntent alarmIntent;

	private void triggerAlarm() {
		if (DEBUG)
			Log.d("Check", "SampleVoiceRecognizationActivity::triggerAlarm");
		/*
		 * AlarmManager alarmManager =
		 * (AlarmManager)getSystemService(Context.ALARM_SERVICE);//this canbe
		 * used if delayed alarm is needed int alarmType =
		 * AlarmManager.ELAPSED_REALTIME_WAKEUP; long timeOrLengthofWait = 10;
		 * Intent intentToFire = new Intent(this, AlarmReciever.class);
		 * PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 0,
		 * intentToFire, 0); alarmManager.set(alarmType, timeOrLengthofWait,
		 * alarmIntent);
		 */
		;
		String myText2 = "I am here sir";
		mTts.speak(myText2, TextToSpeech.QUEUE_FLUSH, null);
		// mTts.setEngineByPackageName(enginePackageName)
		if (ringtoneSound != null) {
			// ringtoneSound.play();
		}

	}

	public TextToSpeech getTtsInstance() {
		if (DEBUG)
			Log.d("Check",
					"SampleVoiceRecognizationActivity::getTtsInstance mTts "
							+ mTts);
		return mTts;
	}

	public void stopAlarm() {
		if (ringtoneSound != null) {
			ringtoneSound.stop();
		}
	}

	@Override
	public void onInit(int status) {
		if (DEBUG)
			Log.d("Check", "SampleVoiceRecognizationActivity::onInit");
		// TODO Auto-generated method stub

	}

	// service binding and unbinding
	MyService myServiceBinder;
	protected ServiceConnection mServerConn = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder binder) {
			Log.d("Check", "onServiceConnected");
			LocalBinder bin = (LocalBinder) binder;
			myServiceBinder = bin.getService();
			mServiceIsBound = true;
			mServiceState = recognizerPreference.getBoolean(
					SERVICE_STATE_PREFS, false);

			myServiceBinder.setRecognizerEnabledOrNot(mServiceState);
			/*
			 * if(mTts != null) myServiceBinder.setTts(mTts);
			 */
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d("Check", "onServiceDisconnected");
			mServiceIsBound = false;
		}

	};

	boolean mServiceIsBound = false;
	private int mAlarmMode = 0;
	private int mCustomTextPos = 0;

	public void start() {
		if (DEBUG)
			Log.d("Check", "SampleVoiceRecognizationActivity::start");
		// mContext is defined upper in code, I think it is not necessary to
		// explain what is it
		Intent intent = null;
		intent = new Intent(this, MyService.class);
		// Create a new Messenger for the communication back
		// From the Service to the Activity

		bindService(intent, mServerConn, Context.BIND_AUTO_CREATE);
		// getApplicationContext().startService(i);
	}

	public void stop() {
		if (DEBUG)
			Log.d("Check",
					"SampleVoiceRecognizationActivity::stop mServerConn  "
							+ mServerConn);
		// getApplicationContext().stopService(new
		// Intent(getApplicationContext(), MyService.class));
		if (mServerConn != null)
			unbindService(mServerConn);
	}

	// ///spinners

	public void addItemsOnAlarmModeSpinner() {

		mAlarmModeSpinner = (Spinner) findViewById(R.id.alarm_mode_spinner);
		List<String> list = new ArrayList<String>();
		list.add("Custom Text");
		list.add("Alarm Ringtone");
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, list);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mAlarmModeSpinner.setAdapter(dataAdapter);
		mAlarmModeSpinner.setSelection(mAlarmMode);
		switchForAlarmModes(mAlarmMode);

	}

	public void addListenerOnSpinnerItemSelection() {
		mAlarmModeSpinner = (Spinner) findViewById(R.id.alarm_mode_spinner);
		mAlarmModeSpinner
				.setOnItemSelectedListener(new CustomOnItemSelectedListener());
	}

	public static SampleVoiceRecognizationActivity getInst() {
		return mInst;
	}

	public void addItemsOnCustomTextSpinner() {
if (DEBUG)
	Log.d("Check",
			"SampleVoiceRecognizationActivity::addItemsOnCustomTextSpinner");
		mCustomTextSpinner = (Spinner) findViewById(R.id.custom_text_spinner);
		List<String> list = new ArrayList<String>();
		list.add("Find My Fone");
		list.add("Locate My Fone");
		list.add("Search My Fone");
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, list);
		dataAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mCustomTextSpinner.setAdapter(dataAdapter);
		mCustomTextSpinner.setSelection(mCustomTextPos);
//		/switchForCustomTexts(mCustomTextPos);

	}

	public void addListenerOnSpinnerCustomTextItemSelection() {
		mCustomTextSpinner = (Spinner) findViewById(R.id.custom_text_spinner);
		mCustomTextSpinner
				.setOnItemSelectedListener(new OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int position, long id) {
						// TODO Auto-generated method stub
						if (DEBUG)
							Log.d("Check",
									"SampleVoiceRecognizationActivity.addListenerOnSpinnerCustomTextItemSelection().new OnItemSelectedListener() {...}::onItemSelected pos "
											+ position);
						switchForCustomTexts(position);
					}

					@Override
					public void onNothingSelected(AdapterView<?> parent) {
						// TODO Auto-generated method stub

					}
				});
	}

	public void switchForAlarmModes(int pos) {
		if (pos == CustomOnItemSelectedListener.ALARM_MODE_CUSTOM_POSITION) {
			mResponseTextLayout.setVisibility(View.VISIBLE);
			mDefaultRingtone.setVisibility(View.GONE);
			if (myServiceBinder != null)// null check as we are also calling
										// from oncreate
				myServiceBinder
						.setAlarmMode(CustomOnItemSelectedListener.ALARM_MODE_CUSTOM_POSITION);

		} else if (pos == CustomOnItemSelectedListener.ALARM_MODE_DEFAULT_RINGTONE) {
			mResponseTextLayout.setVisibility(View.GONE);
			mDefaultRingtone.setVisibility(View.VISIBLE);
			if (myServiceBinder != null)
				myServiceBinder
						.setAlarmMode(CustomOnItemSelectedListener.ALARM_MODE_DEFAULT_RINGTONE);

		}
		setAlarmModePreference(pos);
	}

	public void setAlarmModePreference(int pos) {
		recognizerPreference.edit().putInt(ALARM_MODE_PREFS, pos).commit();
	}

	public int getalarmModePrefrence() {
		return mAlarmMode;
	}

	public void switchForCustomTexts(int pos) {

		if (DEBUG)
			Log.d("Check",
					"SampleVoiceRecognizationActivity::switchForCustomTexts pos "
							+ pos);
		switch (pos) {
		case 0 :
		case 1 :
		case 2 :
		default :
			if (myServiceBinder != null) {
				myServiceBinder.changeRecognizerString(pos);
				/*recognizerPreference.edit().putBoolean(SERVICE_STATE_PREFS, false).commit();
				myServiceBinder.setRecognizerEnabledOrNot(false);
		        mStartStop.setText("Enable Finder");
		        Intent in = new Intent();
				in.setClass(getApplicationContext(), MyService.class);
				stopService(in);*/
			}
			break;

		}
		setCustomTextPreference(pos);
	}

	public void setCustomTextPreference(int pos) {
		recognizerPreference.edit().putInt(CUSTOM_TEXT_PREFS, pos).commit();
	}

	public int getCustomTextPrefrence() {
		return mCustomTextPos;
	}
}