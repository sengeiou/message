package com.tcl.manucase;

import com.tcl.autotest.tool.HomeKeyLocker;
import com.tcl.autotest.AllMainActivity;
import com.tcl.autotest.AutotestMainActivity;
import com.tcl.autotest.R;
import com.tcl.autotest.tool.Tool;
import com.tcl.autotest.utils.ManuFinishThread;
import com.tcl.autotest.utils.Msg;
import com.tcl.autotest.utils.Test;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.provider.Settings;

import java.util.List;
import java.util.ArrayList;

public class KeyPadTest extends Test{

	private static final String TAG = "KeyPadTest";
	private int testKey;
	private int num = 0x01;

	FrameLayout fl = null;
	LinearLayout keys = null;
	HomeReceiver homereceiver;
	IntentFilter intentfilter;

	KeyguardManager keyguardManager;
	KeyguardLock lock;
	//PhoneWindowManager phonewindowmanager;
	
	HomeKeyLocker mHomeKeyLocker;
	WindowManager wm;
	public static final int KEYCODE_AI = 294;
	public static final int KEYCODE_AI2 = 295;
	public static final int KEYCODE_AI_SEOUL_TF = 289;
	public static final int FLAG_HOMEKEY_DISPATCHED = 0x80000000; //��Ҫ�Լ������־
	public int ai_key_enabled = 0;
	private List<String> kp_project = new ArrayList<>();
	private void addProject(List project){
		project.add("5006");
		project.add("Venice");
		project.add("5007A");
		project.add("5028A");
		project.add("5028D");
		project.add("Seoul_GL_CAN");
		project.add("Seoul");
		project.add("Seoul_TF");
		project.add("Tokyo_TF");
		project.add("Tokyo");
		project.add("Tokyo_Lite");
		project.add("5029Y");
		project.add("5129Y");
		project.add("TokyoPro");
		project.add("Tokyo_CAN");
//		project.add("Morgan_4G");
	}
	public KeyPadTest(ID id, String name) {
		super(id, name);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void setUp() {
		addProject(kp_project);
		// TODO Auto-generated method stub
		Tool.toolLog(TAG + "_start_test");
		
		fl = new FrameLayout(mContext);
		mLayout = (LinearLayout) View.inflate(mContext,
				R.layout.manu_base_screen, null);
		//if(AllMainActivity.deviceName.equals("idol347") || true){
		if(AllMainActivity.deviceName.equals("idol347")||true ){
			if(kp_project.contains(AllMainActivity.deviceName)) {
				keys = (LinearLayout) View.inflate(mContext, R.layout.ilde_keypad_ai, null);
				testKey = 0xd8;
			}else {
				keys = (LinearLayout) View.inflate(mContext, R.layout.ilde_three_keypad, null);
				testKey = 0xf8; // 0xc0;
			}
		}else{
			keys = (LinearLayout) View.inflate(mContext, R.layout.keypad, null);
			testKey = 0xe0; // 0xc0;
		}
		// Add by Jianke.Zhang 03/13
		/*homereceiver = new HomeReceiver();
		intentfilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
	     mContext.registerReceiver(homereceiver, intentfilter);*/
		// End
		
		/*mContext.getWindow().setFlags(FLAG_HOMEKEY_DISPATCHED, FLAG_HOMEKEY_DISPATCHED);*/
		/*keyguardManager = (KeyguardManager) mContext
				.getSystemService(Context.KEYGUARD_SERVICE);
		lock = keyguardManager.newKeyguardLock(Context.KEYGUARD_SERVICE);*/
	/*	
		mHomeKeyLocker = new HomeKeyLocker();
		mHomeKeyLocker.lock(mContext);*/
		wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		ai_key_enabled = get_ai_key();
		if(ai_key_enabled==1){
			set_ai_key(0);
		}
	}
	private int get_ai_key(){
		int value = Settings.System.getInt(mContext.getContentResolver(),"ai_key_enabled",1);
		Tool.toolLog(TAG + " ai_key_enable: " + value);
		return value;
	}
	private void set_ai_key(int value){
		Settings.System.putInt(mContext.getContentResolver(),"ai_key_enabled",value);
		Tool.toolLog(TAG + " set ai_key_enable= " + value);
	}

	@Override
	public void initView() {
		// TODO Auto-generated method stub
		
		bt_left = (Button) mLayout.findViewById(R.id.bt_left);
		bt_right = (Button) mLayout.findViewById(R.id.bt_right);
		text_top_zone = (TextView) mLayout.findViewById(R.id.text_top_zone);

		text_top_zone.setText(mName);
		bt_left.setText(R.string.pass);
		bt_right.setText(R.string.fail);

		bt_left.setOnClickListener(pass_listener);
		bt_right.setOnClickListener(failed_listener);
		bt_left.setEnabled(false);

		fl.addView(keys);
		fl.addView(mLayout);

		mContext.setContentView(fl);
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		// Add by Jianke.Zhang 01/21
		// Tool.toolLog("finish KeyPadTest");
//		mContext.unregisterReceiver(homereceiver);
		
//		mHomeKeyLocker.unlock();
//		mHomeKeyLocker = null;
	}

	
	@Override
	public boolean onKeyTouch(KeyEvent event) {
		// TODO Auto-generated method stub
		Message msg = new Message();
		int keyCode = event.getKeyCode();
//		Tool.toolLog(TAG + " " + mName + " " + String.valueOf(keyCode));
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_UP:
			msg.what = KeyEvent.KEYCODE_VOLUME_UP;
			mHandler.sendMessage(msg);
			break;
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			msg.what = KeyEvent.KEYCODE_VOLUME_DOWN;
			mHandler.sendMessage(msg);
			break;
		case KeyEvent.KEYCODE_BACK:
			msg.what = KeyEvent.KEYCODE_BACK;
			mHandler.sendMessage(msg);
			break;
		case KeyEvent.KEYCODE_HOME:
			msg.what = KeyEvent.KEYCODE_HOME;
			mHandler.sendMessage(msg);
			break;
		/*
		 * case KeyEvent.KEYCODE_RECENTAPP: msg.what =
		 * KeyEvent.KEYCODE_RECENTAPP; mHandler.sendMessage(msg); break;
		 */
		// Task key should be menu key in some project
		case KeyEvent.KEYCODE_MENU:
			msg.what = KeyEvent.KEYCODE_MENU;
			mHandler.sendMessage(msg);
			break;
		// Add by Jianke.Zhang 01/21
		// case KeyEvent.KEYCODE_POWER:
		// msg.what = KeyEvent.KEYCODE_POWER;
		// mHandler.sendMessage(msg);
		// break;
		// End
        case KEYCODE_AI:
            msg.what = KEYCODE_AI;
            mHandler.sendMessage(msg);
            break;
		case KEYCODE_AI2:
			msg.what = KEYCODE_AI;
			mHandler.sendMessage(msg);
			break;
		case KEYCODE_AI_SEOUL_TF:
			msg.what = KEYCODE_AI;
			mHandler.sendMessage(msg);
			break;

		}
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
//		Tool.toolLog(TAG + " onKeyDown 88888888");
		return true;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
//		Tool.toolLog(TAG + " onKeyUp 66666666");
		 
		return true;
	}

	private Handler mHandler = new Handler() {

		@Override
		public void dispatchMessage(Message msg) {
			// TODO Auto-generated method stub
			Button bt = null;
//			Tool.toolLog(TAG + " msg.what " + msg.what);
			switch (msg.what) {
			case KeyEvent.KEYCODE_VOLUME_UP:
				testKey |= num;
				bt = (Button) keys.findViewById(R.id.bt_vol_up);
				break;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				testKey |= num << 1;
				bt = (Button) keys.findViewById(R.id.bt_vol_down);
				break;
			case KeyEvent.KEYCODE_BACK:
				testKey |= num << 2;
				bt = (Button) keys.findViewById(R.id.bt_back);
				break;
			case KeyEvent.KEYCODE_HOME:
				testKey |= num << 3;
				bt = (Button) keys.findViewById(R.id.bt_home);
				//lock.disableKeyguard();
				break;
			case KeyEvent.KEYCODE_MENU:
				testKey |= num << 4;
				bt = (Button) keys.findViewById(R.id.bt_task);
				//lock.disableKeyguard();
				break;
			// Add by Jianke.Zhang 01/21
			// case KeyEvent.KEYCODE_POWER:
			// testKey |= num << 5;
			// bt = (Button)keys.findViewById(R.id.bt_power);
			// break;
			// End
            case KEYCODE_AI:
                testKey |= num << 5;
                int tmp = num<<5;
                bt = (Button) keys.findViewById(R.id.bt_ai);
                break;
			}

			// Tool.toolLog("testKey " + testKey);
			// Log.d("MMITEST", String.valueOf(testKey));
			// When send a key the testKeyNu
			if (testKey == 0xff) {
				// Tool.toolLog(TAG + " key finish");
				bt_left.setEnabled(true);
				mContext.setResult(Test.RESULT.PASS.ordinal());
			}
			if(bt != null){
				bt.setEnabled(false);
			}
			super.handleMessage(msg);
		}

	};

	public class HomeReceiver extends BroadcastReceiver {

		@SuppressWarnings("deprecation")
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
				String reason = intent.getStringExtra("reason");
				if (reason != null) {
					Message msg = new Message();
					if (reason.equals("homekey")) {
						lock.disableKeyguard();
						Tool.toolLog(TAG + " homekey");
						Log.v(TAG, "homekey");
						msg.what = 0x3;
						mHandler.sendMessage(msg);

						
					} else if (reason.equals("recentapps")) {
						lock.disableKeyguard();						
						Tool.toolLog(TAG + " recentapps");
						msg.what = 0x52;
						mHandler.sendMessage(msg);
						
					}
				}
			}
		}
	}


	public String getmContextTag() {
		// TODO Auto-generated method stub
		return TAG;
	}

	@Override
	public void setmContextTag() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		ai_key_enabled = get_ai_key();
		if(ai_key_enabled==0){
			set_ai_key(1);
		}
	}

	@Override
	public void onStop() {
		ai_key_enabled = get_ai_key();
		if(ai_key_enabled==0){
			set_ai_key(1);
		}
		super.onStop();
	}

}
