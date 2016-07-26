package com.own.sample;

import static android.widget.Toast.makeText;
import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.widget.Toast;
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

public class MyService extends Service implements OnInitListener,
		RecognitionListener {
	protected SpeechRecognizer mSpeechRecognizer;
	protected Intent mSpeechRecognizerIntent;
	protected static Messenger mServerMessenger = null;
	protected static String KWS_STRING_FIRST = "wakeup_one";
	protected static String KWS_STRING_SECOND = "wakeup_two";
	protected static String KWS_STRING_THIRD = "wakeup_three";
	ArrayList<String> list;
	ArrayList<String> resultsArray = new ArrayList<String>();
	static String mDefinedString = "where are you";
	String mResponseString = "i am here sir";
	
	////
	private static Context mContext;
	static RecognitionListener mRecognizerListener;

	TextToSpeech mTts;

	protected boolean mIsListening;
	private static boolean mIsStreamSolo;

	static final int MSG_RECOGNIZER_START_LISTENING = 1;
	static final int MSG_RECOGNIZER_CANCEL = 2;
	static final int MSG_SWITCH_SEARCH = 3;
	static final int MSG_REINIT_RECOGNIZER = 4;

	Uri ringtoneUri;
	Ringtone ringtoneSound;
	Handler mHandler;

	SharedPreferences recognizerPreference;
	public static String SERVICE_STATE_PREFS = "service_state";
	public static String ALARM_MODE_PREFS = "alarm_mode";
	public static String CUSTOM_TEXT_PREFS = "custom_text";
	public static String RESPONSE_TEXT_PREFS = "response_text";
	static String[] customTexts;
	public static int mCustomTextMode = 0;
	private static boolean mLoading = false;

	static {
		System.loadLibrary("pocketsphinx_jni");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		if (DEBUG)
			Log.d("Check", "MyService::onCreate");
		mContext = this;
		mRecognizerListener = this;
		mServerMessenger = new Messenger(
				new IncomingHandler(this));
		ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
		ringtoneSound = RingtoneManager.getRingtone(getApplicationContext(),
				ringtoneUri);
		customTexts = getResources().getStringArray(R.array.custom_text);
		recognizerPreference = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		mCustomTextMode = recognizerPreference.getInt(CUSTOM_TEXT_PREFS, 0);
		mDefinedString = (customTexts[recognizerPreference.getInt(
				CUSTOM_TEXT_PREFS, 0)]).toLowerCase();
		if (DEBUG)
			Log.d("Check", "MyService::onCreate string mdefinedString "
					+ mDefinedString);
		mTts = new TextToSpeech(this, this);
		mHandler = new Handler(getMainLooper());
		Message msg = new Message();
		msg.what = MSG_RECOGNIZER_START_LISTENING;
		try {
			mServerMessenger.send(msg);
		} catch (RemoteException e) {
			if (DEBUG)
				Log.d("Check", "MyService::onCreate catchc e " + e);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setupRecognizer();
	}

	protected static class IncomingHandler extends Handler {
		private WeakReference<MyService> mtarget;

		IncomingHandler(MyService target) {
			if (DEBUG)
				Log.d("Check", "MyService.IncomingHandler::IncomingHandler");
			mtarget = new WeakReference<MyService>(target);
		}

		@Override
		public void handleMessage(Message msg) {
			if (DEBUG)
				Log.d("Check", "MyService.IncomingHandler::handleMessage msg "
						+ msg.what);
			final MyService target = mtarget.get();

			switch (msg.what) {
			case MSG_RECOGNIZER_START_LISTENING:

				break;

			case MSG_RECOGNIZER_CANCEL:
				break;
			case MSG_SWITCH_SEARCH:
				switchSearch(mCustomTextMode);
				break;
			case MSG_REINIT_RECOGNIZER:
				reInititateRecognizer();
				break;
			}
		}
	}

	@Override
	public void onDestroy() {
		if (DEBUG)
			Log.d("Check", "MyService::onDestroy");
		super.onDestroy();

		if (mTts != null) {
			mTts.stop();
			mTts.shutdown();
			mTts = null;
		}
		stopForeground(true);
		recognizer.cancel();
		recognizer.shutdown();
		recognizer = null;
		recognizerPreference.edit().putBoolean(SERVICE_STATE_PREFS, false)
				.commit();
	}

	private final IBinder mBinder = new LocalBinder();
	private boolean mTalkEnabled = true;
	private int mAlarmMode;
	private Boolean mRecognizerEnabled = false;

	private static final int ALARM_MODE_CUSTOM_TEXT = 0;
	private static final int ALARM_MODE_DEFAULT_RINGTONE = 1;

	public class LocalBinder extends Binder {
		public MyService getService() {
			return MyService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		if (DEBUG)
			Log.d("Check", "MyService::onBind intent " + intent);
		// TODO Auto-generated method stub
		return mBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		if (DEBUG)
			Log.d("Check", "MyService::onStartCommand");
		/*
		 * Intent checkIntent = new Intent();
		 * checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
		 * startActivity(checkIntent);
		 */
		
		startForeground(FOREGROUND_ID,buildForegroundNotification());
		if (DEBUG)
			Log.d("Check", "MyService::onStartCommand mtts " + mTts);
		return super.onStartCommand(intent, flags, startId);
	}
	private static int FOREGROUND_ID = 1443;
	 private Notification buildForegroundNotification() {
		 Notification.Builder b=new Notification.Builder(this);
		 Intent notificationIntent = new Intent(this, SampleVoiceRecognizationActivity.class);
		 notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
		 | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		 PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
		 notificationIntent, 0);
		    b.setOngoing(true);

		    b.setContentTitle("Finder")
		     .setContentText("Active")
		     .setSmallIcon(android.R.drawable.ic_btn_speak_now)
		     .setContentIntent(pendingIntent);

		    return(b.build());
		  }

	@Override
	public boolean onUnbind(Intent intent) {
		// TODO Auto-generated method stub
		if (DEBUG)
			Log.d("Check", "MyService::onUnbind");
		return super.onUnbind(intent);
	}

	// activity communication methods

	public void setTts(TextToSpeech tts) {
		if (DEBUG)
			Log.d("Check", "MyService::setTts tts " + tts);
		mTts = tts;
	}

	// TextMAtch check

	private void matchText(String matches_text) {
		if (DEBUG)
			Log.d("Check",
					"SampleVoiceRecognizationActivity::matchText matches_text "
							+ matches_text + "   " + mTts+" mDefinedString "+mDefinedString);
		if (matches_text.equals(mDefinedString)) {
			if (DEBUG)
				Toast.makeText(getBaseContext(), "your text found",
						Toast.LENGTH_LONG).show();
			triggerAlarm();
			// Speech.setText("Stop Alarm");

		} else {
			if (DEBUG)
				Toast.makeText(getBaseContext(), "your text not found",
						Toast.LENGTH_LONG).show();
		}
	}

	// Alarming

	private void triggerAlarm() {
		if (DEBUG)
			Log.d("Check", "MyService::triggerAlarm getAlarmMode "
					+ getAlarmMode() + " mTts " + mTts + " mri" + ringtoneSound);
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
		if (getAlarmMode() == ALARM_MODE_CUSTOM_TEXT) {
			if (mTts != null)
				mTts.speak(mResponseString, TextToSpeech.QUEUE_FLUSH, null);
		} else if (getAlarmMode() == ALARM_MODE_DEFAULT_RINGTONE) {
			playRingTone();
		}
		// mTts.setEngineByPackageName(enginePackageName)

	}

	public void playRingTone() {
		ringtoneSound.play();

		mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				ringtoneSound.stop();
			}
		}, 5000);

	}

	public void changeRecognizerString(int mode) {
		if (DEBUG)
			Log.d("Check", "MyService::changeRecognizerString newString "
					+ customTexts[mode]);
		mCustomTextMode = mode;
		mDefinedString = customTexts[mode].toLowerCase();
		if (isLoading())
			return;
		Message msg = new Message();
		msg.what = MSG_SWITCH_SEARCH;
		try {
			mServerMessenger.send(msg);
		} catch (RemoteException e) {
			if (DEBUG)
				Log.d("Check", "MyService::onCreate catchc e " + e);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public boolean isLoading() {
		return mLoading;
	}
	
	
	

	
	public static void reInititateRecognizer(){
		
		setupRecognizer();
	}

	public void changeResponseString(String newString) {
		mResponseString = newString;
	}

	public void setAlarmMode(int mode) {
		mAlarmMode = mode;
	}

	public int getAlarmMode() {
		return mAlarmMode;
	}

	public void setRecognizerEnabledOrNot(Boolean enable) {
		if (DEBUG)
			Log.d("Check", "MyService::setRecognizerEnabledOrNot enable "
					+ enable);
		mRecognizerEnabled = enable;
	}

	public boolean recognizerEnabled() {
		return mRecognizerEnabled;
	}

	@Override
	public void onInit(int status) {
		if (DEBUG)
			Log.d("Check", "MyService::onInit");
		// TODO Auto-generated method stub

	}

	// pocketphinx

	private static final String KEY_PHRASE = "find my fone";
	private static boolean DEBUG = false;

	private static SpeechRecognizer recognizer;

	/**
	 * In partial result we get quick updates about current hypothesis. In
	 * keyword spotting mode we can react here, in other modes we need to wait
	 * for final result in onResult.
	 */
	@Override
	public void onPartialResult(Hypothesis hypothesis) {
		if (DEBUG)
			Log.d("Check", "PocketSphinxSample::onPartialResult");
		if (hypothesis != null) {

			String text = hypothesis.getHypstr();
			if (DEBUG)
				Log.d("Check", "PocketSphinxSample::onPartialResult text "
						+ text);
		}

	}

	/**
	 * This callback is called when we stop the recognizer.
	 */
	@Override
	public void onResult(Hypothesis hypothesis) {
		if (DEBUG)
			Log.d("Check", "PocketSphinxSample::onResult");
		if (hypothesis != null) {

			String text = hypothesis.getHypstr();
			if (DEBUG)
				Log.d("Check", "PocketSphinxActivity::onResult text " + text);
			if (recognizerEnabled())
				matchText(text);
			if(DEBUG)
			    makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
		}

		Message msg = new Message();
		msg.what = MSG_SWITCH_SEARCH;
		try {
			mServerMessenger.send(msg);
		} catch (RemoteException e) {
			if (DEBUG)
				Log.d("Check", "MyService::onCreate catchc e " + e);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onBeginningOfSpeech() {
		if (DEBUG)
			Log.d("Check", "PocketSphinxSample::onBeginningOfSpeech");
	}

	/**
	 * We stop recognizer here to get a final result
	 */
	@Override
	public void onEndOfSpeech() {
		/*
		 * if (!recognizer.getSearchName().equals(KWS_SEARCH))
		 * switchSearch(KWS_SEARCH);
		 */
		if (DEBUG)
			Log.d("Check",
					"PocketSphinxSample::onEndOfSpeech recognizer.getSearchName() "
							+ recognizer.getSearchName());

		recognizer.stop();
	}

	private static void switchSearch(int mode) {
		recognizer.cancel();
		if (DEBUG)
			Log.d("Check", "PocketSphinxSample::switchSearch searchName "
					+ customTexts[mCustomTextMode]);
		// If we are not spotting, start listening with timeout (10000 ms or 10
		// seconds).
		
		switch (mode) {

		case 0:

			recognizer.startListening(KWS_STRING_FIRST);
			break;

		case 1:
			recognizer.startListening(KWS_STRING_SECOND);

			break;

		case 2:

			recognizer.startListening(KWS_STRING_THIRD);

			break;

		}

	}

	public static void setupRecognizer() {
		new AsyncTask<Void, Void, Exception>() {

			@Override
			protected Exception doInBackground(Void... params) {
				mLoading = true;
				try {
					Assets assets = new Assets(mContext);
					File assetDir = assets.syncAssets();
					if (DEBUG)
						Log.d("Check",
								"PocketSphinxActivity::onCreate assetDir "
										+ assetDir.toString());
					setupRecognizer(assetDir);
				} catch (IOException e) {
					return e;
				}
				return null;
			}

			@Override
			protected void onPostExecute(Exception result) {
				if (DEBUG)
					Log.d("Check",
							"PocketSphinxSample.onCreate(...).new AsyncTask<Void,Void,Exception>() {...}::onPostExecute result "
									+ result);
				if (result != null) {
					if (DEBUG)
						Log.d("Check",
								"MyService.onCreate().new AsyncTask<Void,Void,Exception>() {...}::onPostExecute failed");
				} else {
					Message msg = new Message();
					msg.what = MSG_SWITCH_SEARCH;
					try {
						mServerMessenger.send(msg);
					} catch (RemoteException e) {
						if (DEBUG)
							Log.d("Check", "MyService::onCreate catchc e " + e);
						// TODO Auto-generated catch block
						e.printStackTrace();
					}				}
				mLoading = false;
			}
		}.execute();
        
	}

	private static void setupRecognizer(File assetsDir) throws IOException {
		if (DEBUG)
			Log.d("Check", "PocketSphinxActivity::setupRecognizer");
		// The recognizer can be configured to perform multiple searches
		// of different kind and switch between them
		File modelsDir = new File(assetsDir, "models");
		recognizer = defaultSetup()
				.setAcousticModel(new File(modelsDir, "hmm/en-us-ptm"))
				.setDictionary(new File(modelsDir, "dict/cmudict-en-us.dict"))

				// To disable logging of raw audio comment out this call (takes
				// a lot of space on the device)
				.setRawLogDir(assetsDir)

				// Threshold to tune for keyphrase to balance between false
				// alarms and misses
				.setKeywordThreshold(1e-45f)

				// Use context-independent phonetic search, context-dependent is
				// too slow for mobile
				.setBoolean("-allphone_ci", true)

				.getRecognizer();
		recognizer.addListener(mRecognizerListener);

		recognizer.addKeyphraseSearch(KWS_STRING_FIRST, customTexts[0].toLowerCase());
		recognizer.addKeyphraseSearch(KWS_STRING_SECOND, customTexts[1].toLowerCase());
		recognizer.addKeyphraseSearch(KWS_STRING_THIRD, customTexts[2].toLowerCase());
		
	}

	@Override
	public void onError(Exception error) {
		if (DEBUG)
			Log.d("Check", "PocketSphinxSample::onError error " + error);
			setupRecognizer();
	}

	@Override
	public void onTimeout() {
		if (DEBUG)
			Log.d("Check", "PocketSphinxSample::onTimeout");
		switchSearch(mCustomTextMode);
	}

	// pocketphinx end
	// service clear picture
	// https://sourceforge.net/p/cmusphinx/discussion/help/thread/1b015ee5/#d8ed/1fcf
	// https://sourceforge.net/p/cmusphinx/discussion/help/thread/6c8feb5b/
	// http://stackoverflow.com/questions/17146822/when-is-a-started-and-bound-service-destroyed
	// http://stackoverflow.com/questions/14940657/android-speech-recognition-as-a-service-on-android-4-1-4-2
	// http://stackoverflow.com/questions/24090922/how-to-use-speechrecognizerservice-for-receive-listening-permanently-on-android
}