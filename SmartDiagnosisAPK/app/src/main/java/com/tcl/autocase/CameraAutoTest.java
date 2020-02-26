package com.tcl.autocase;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.EncodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import com.tcl.autotest.tool.Tool;
import com.tcl.autotest.utils.DownTimeCallBack;
import com.tcl.autotest.utils.Test;
import com.tcl.autotest.utils.TestCountDownTimer;
import com.tcl.manucase.FocusIndicatorRotateLayout;

public class CameraAutoTest extends Test implements DownTimeCallBack{

	private static final String TAG = "CameraAutoTest";
	FrameLayout fl;
	public Preview mPreview;
	View focusIndicatorLayout;
	int mZoomValue = 0;
	private final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
	int cameraId = 0;
	private static Activity tempActivity = null;
	Camera mxCamera;
	boolean  cameraStatus = false;
	public static boolean sysCamera = false;
	
	private boolean isPassBtn = false;
	
	private TestCountDownTimer testCountDownTimer = null;
	
	public CameraAutoTest(ID id, String name) {
		super(id, name);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onFinish() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTick() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setUp() {
		// TODO Auto-generated method stub

	}

	@Override
	public void initView() {
		// TODO Auto-generated method stub

	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub
		
	}

	@Override
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
		
	}
	@SuppressLint("NewApi")
	class Preview extends SurfaceView implements SurfaceHolder.Callback {
		SurfaceHolder mHolder;
		private  Camera mCamera;

		int mZoom = 0;
		private final AutoFocusCallback mAutoFocusCallback = new AutoFocusCallback();
		private final AutoFocusMoveCallback mAutoFocusMoveCallback = new AutoFocusMoveCallback();
		private int mViewFinderWidth;
		private int mViewFinderHeight;
		private boolean mAutoFocus = false;
		private static final int FOCUS_FRAME_DELAY = 1000;
		private static final int RESET_TOUCH_FOCUS = 0;
		private Handler mHandler;
		private FocusIndicatorRotateLayout mFocusIndicatorRotateLayout;

		private static final String TAG = "MMITEST_Preview";
		
		int cameraId = 0;
		Context mContext;

		public int getPreviewWidth() {
			return mViewFinderWidth;
		}

		public int getPreviewHeight() {
			return mViewFinderHeight;
		}

		class MainHandler extends Handler {
			public MainHandler(Looper looper) {
				super(looper);
			}

			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case RESET_TOUCH_FOCUS: 
					cancelAutoFocus();
					break;
				}
			}
		}

		class AutoFocusCallback implements Camera.AutoFocusCallback {
			@Override
			public void onAutoFocus(boolean focused, Camera camera) {
				doAutoFocus(focused);
				mHandler.sendEmptyMessageDelayed(RESET_TOUCH_FOCUS,
						FOCUS_FRAME_DELAY);
				mAutoFocus = true;
			}
		}

		@SuppressLint("NewApi")
		class AutoFocusMoveCallback implements Camera.AutoFocusMoveCallback {
			@Override
			public void onAutoFocusMoving(boolean moving, Camera camera) {
				mHandler.removeMessages(RESET_TOUCH_FOCUS);
				doMovingAutoFocus(moving);
			}
		}

		public void cancelAutoFocus() {
			resetTouchFocus();
		}

		public void resetTouchFocus() {
			// Put focus indicator to the center.
			RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) mFocusIndicatorRotateLayout
					.getLayoutParams();
			int[] rules = p.getRules();
			rules[RelativeLayout.CENTER_IN_PARENT] = RelativeLayout.TRUE;
			p.setMargins(0, 0, 0, 0);
			mFocusIndicatorRotateLayout.clear();
		}

		public void doAutoFocus(boolean focused) {
			mHandler.removeMessages(RESET_TOUCH_FOCUS);
			if (focused) {
				mFocusIndicatorRotateLayout.showSuccess(false);
			} else {
				mFocusIndicatorRotateLayout.showFail(false);
			}
		}

		public void doMovingAutoFocus(boolean moving) {
			if (moving) {
				mFocusIndicatorRotateLayout.showStart();
			} else {
				mFocusIndicatorRotateLayout.showSuccess(true);
			}
		}
		
		Preview(Context context,
				FocusIndicatorRotateLayout focusIndicatorRotateLayout, Looper looper) {
			super(context);
			mContext = context;
			mHandler = new MainHandler(looper);
			mFocusIndicatorRotateLayout = focusIndicatorRotateLayout;
			// Install a SurfaceHolder.Callback so we get notified when the
			// underlying surface is created and destroyed.
			mHolder = getHolder();
			mHolder.addCallback(this);
			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		Preview(Context context) {
			super(context);
			mContext = context;

			// Install a SurfaceHolder.Callback so we get notified when the
			// underlying surface is created and destroyed.
			mHolder = getHolder();
			mHolder.addCallback(this);
			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		Preview(Context context, int scale) {
			this(context);
			mZoom = scale;
		}

		Preview(Context context, int scale, int camera) {
			this(context, scale);
			cameraId = camera;
		}

		public  Camera getCamera() {
			return mCamera;
		}

		public void surfaceCreated(SurfaceHolder holder) {
			// The Surface has been created, acquire the camera and tell it
			// where
			// to draw.
			Tool.toolLog(TAG + " surfaceCreated");
			try {
				if (cameraId == 0) {
					mCamera = null;
					mCamera = Camera.open();
//					Tool.toolLog(TAG + " MMI Test " +  mCamera);
					//Jianke.Zhang 05/29
					if(mCamera != null){
						setCameraDisplayOrientation(mContext, cameraId, mCamera);
						cameraStatus = true;
					}else{
						Tool.toolLog(TAG + " surfaceCreated back failed ");
					}
					//End
				} else {
//					Tool.toolLog(TAG + " cameraId dadadada " + Camera.getNumberOfCameras());
					if (true/*cameraId < Camera.getNumberOfCameras()*/) {
						mCamera = null;
						Tool.toolLog(TAG + " mCamera��1111 " + mCamera);
						mCamera = Camera.open(cameraId);
						Tool.toolLog(TAG + " mCamera 2222 " + mCamera);
						if(mCamera != null){
							setCameraDisplayOrientation(mContext, cameraId,
									mCamera);
							cameraStatus = true;
						}else{
							Tool.toolLog(TAG + " surfaceCreated front failed ");
						}
					}
				}
			} catch (Exception e) {
				Tool.toolLog(TAG + " MMI Test " +  "can't open camera.");
				return;
			}
		}

		public  void setCameraDisplayOrientation(Context context,
				int cameraId, Camera camera) {
			// See android.hardware.Camera.setCameraDisplayOrientation for
			// documentation.
			Camera.CameraInfo info = new Camera.CameraInfo();
			Camera.getCameraInfo(cameraId, info);
			int degrees = getDisplayRotation(context);
			int result;
			if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				// result = (info.orientation + degrees) % 360;
				result = (info.orientation - degrees + 180) % 360;
//				Tool.toolLog(TAG + " info " + "front ********  ????");
			} else {
				result = (info.orientation - degrees + 360) % 360;
//				Tool.toolLog(TAG + " info " + "behind ******* ????");
			}
			camera.setDisplayOrientation(result);
		}

		public  int getDisplayRotation(Context context) {
			int rotation = ((WindowManager) (context
					.getSystemService(Context.WINDOW_SERVICE))).getDefaultDisplay()
					.getRotation();
			switch (rotation) {
			case Surface.ROTATION_0:
				return 0;
			case Surface.ROTATION_90:
				return 90;
			case Surface.ROTATION_180:
				return 180;
			case Surface.ROTATION_270:
				return 270;
			}
			return 0;
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			// Surface will be destroyed when we return, so stop the preview.
			// Because the CameraDevice object is not a shared resource,
			// it's very
			// important to release it when the activity is paused.
			Tool.toolLog(TAG + " surfaceDestroyed");
			if (cameraStatus /* && mCamera.previewEnabled() */) {
				try {
					int mCameraEnabled = 0;
					Camera.Parameters mParameters = mCamera.getParameters();
					mParameters.set("camera_enabled",
							String.valueOf(mCameraEnabled));
					mCamera.setParameters(mParameters);
					if (true/* mCamera.previewEnabled() */) {
						mCamera.stopPreview();
					}

					if (cameraStatus) {
						cameraStatus = false;
						mCamera.release();
					}
					
				} catch (Exception e) {
					Tool.toolLog(TAG + " MMI Test" + " can't stop preview ");
				}
				mCamera = null;
			}
		}

		public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
			// Now that the size is known, set up the camera parameters and
			// begin
			// the preview.
			Tool.toolLog(TAG + " surfaceChanged");
			if (mCamera != null) {
				try {
					mCamera.setPreviewDisplay(holder);
				} catch (Exception ex) {
					Tool.toolLog(TAG + " setPreview failed");
				}

				mViewFinderWidth = w;
				mViewFinderHeight = h;
				setCameraParameters();

				try {
					Tool.toolLog(TAG + " startPreview");
					Camera.Parameters mParameters = mCamera.getParameters();
					/*
					 * AutoFocusMoveCallback is only supported in continuous
					 * autofocus modes
					 */
//					Tool.toolLog(TAG + " xxx mCamera " + mCamera);
					mParameters
							.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
//					if (cameraId == 0) {
//						mCamera.setParameters(mParameters);
//					}
					mCamera.setAutoFocusMoveCallback(mAutoFocusMoveCallback);
					mCamera.startPreview();
//					Tool.toolLog(TAG + " xxx mFocusIndicatorRotateLayout " 
//										+ mFocusIndicatorRotateLayout);
					//Add by Jianke.Zhang 01/23
					if(mFocusIndicatorRotateLayout != null){
						mFocusIndicatorRotateLayout.showStart();
					}
					//End
				} catch (Throwable ex) {
					Tool.toolLog(TAG + " startPreview failed " + ex);
				}
			}
			
		}


		public void setZoom(int z) {
			Camera.Parameters mParameters = mCamera.getParameters();
			if (z < 0) {
				z = 0;
			} else if (z > 60) {
				z = 60;
			}
			Tool.toolLog(TAG + " set zoom value to " + z);
			mParameters.set("zoom", String.valueOf(z));
			mCamera.setParameters(mParameters);
		}

		private void setCameraParameters() {
//			Tool.toolLog(TAG + " setCameraParameters");

			final String ANDROID_QUALITY = "jpeg-quality";
			final String THUNDERST_TIMESTAMP = "thunderst_timestamp";
			final String THUNDERST_NIGHTMODE = "thunderst_nightmode";
			final String ANDROID_EFFECT = "effect";
			final String ANDROID_FLICKER_ADJ = "antibanding";
			final String PARM_PICTURE_SIZE = "picture-size";

			final String BRIGHTNESS = "luma-adaptation";
			final String WHITEBALANCE = "whitebalance";

			Camera.Parameters mParameters = mCamera.getParameters();

			mParameters.setPreviewSize(mViewFinderWidth, mViewFinderHeight);
//			Tool.toolLog(TAG + " setCameraParameter: mViewFinderWidth: " + mViewFinderWidth
//					+ " mViewFinderHeight: " + mViewFinderHeight);

			// if we don't set camera enabled, the HAL won't enable the camera
			// device
			// and we MUST disable camera when we stop camera device
			int mCameraEnabled = 1;
			//Jianke.Zhang 02/11
//			mParameters.set("camera_enabled", String.valueOf(mCameraEnabled));

			// to prevent auto clockwise rotation of 90 degree
			// mParameters.set("orientation", "portrait");
			// mParameters.set("rotation", 180);
			// SurfaceHolder.getSurface();
			// Surface.setOrientation(Display.DEFAULT_DISPLAY, Surface.ROTATION_90);
			// mParameters.set("orientation", "landscape");
			// mParameters.set("rotation", 90);
			// mParameters.setRotation(90);
			//Jianke.Zhang 02/11
//			mParameters.set("disp-rotate", 1);

			// Set picture size parameter.
			// String pictureSize = "800x600";
			// String pictureSize = "640x480";
			// mParameters.set(PARM_PICTURE_SIZE, pictureSize);
			//Jianke.Zhang 02/11
//			mParameters.setPreviewSize(640, 360);

			// Set zoom
			int currentZoomValue = 0;
			//Jianke.Zhang 02/11
//			mParameters.set("zoom", String.valueOf(currentZoomValue));

			// Set resolution
			// mParameters.set(PARM_PICTURE_SIZE, "1600x1200");
			//Jianke.Zhang 02/11
//			mCamera.setParameters(mParameters);

			// Set whitebalance
			String whiteBalance = "auto";
			//Jianke.Zhang 02/11
//			mParameters.set(WHITEBALANCE, whiteBalance);
//			mCamera.setParameters(mParameters);

			// Set brightness
			int brightness = 4;
			mParameters.set(BRIGHTNESS, brightness);

			// Set the MyCameraSettings' settings to camera device
			/*
			 * Quality 0:high 1:normal 2:basic
			 */
			String camera_quality = "1";
			String quality = "100";
			if (camera_quality.equals("1")) {
				quality = "60";
			} else if (camera_quality.equals("2")) {
				quality = "30";
			}
			mParameters.set(ANDROID_QUALITY, quality);

			/*
			 * Night mode 1:night mode 0:not night mode
			 */
			boolean nightmode = false;
			mParameters.set(THUNDERST_NIGHTMODE, nightmode ? "1" : "0");

			/*
			 * timestamp 1: with timestamp 0: without timestamp
			 */
			boolean timestamp = true;
			mParameters.set(THUNDERST_TIMESTAMP, timestamp ? "1" : "0");

			/*
			 * effect 0: None 1: Grayscale 2: Negative 3: Sepia
			 */
			String effect = "0";
			mParameters.set(ANDROID_EFFECT, effect);

			/*
			 * flicker adjustment 0: auto 1: 50 HZ 2: 60 HZ
			 */
			String flicker = "0";
			mParameters.set(ANDROID_FLICKER_ADJ, flicker);

			try {
				//Jianke.Zhang 02/11
//				mCamera.setParameters(mParameters);
			} catch (IllegalArgumentException e) {
				Tool.toolLog(TAG + "set Parameters error!!! " + e);
				e.printStackTrace();
			}
		}
		
	}
	}

