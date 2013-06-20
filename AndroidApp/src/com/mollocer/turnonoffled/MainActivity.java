package com.mollocer.turnonoffled;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;



import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.view.Menu;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	
	
    private final String TAG = "MainActivity";
    private static final String ACTION_USB_PERMISSION = "com.mollocer.turnonoffled.action.USB_PERMISSION";
    private boolean mPermissionRequestPending;
    
	
	private TextView mTvLedStatus ;
	private ToggleButton mTbLedSwitch;
	private TextView mTvLog;
	
	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	
    UsbAccessory mAccessory;
    ParcelFileDescriptor mFileDescriptor;
    FileInputStream mInputStream;
    FileOutputStream mOutputStream;
	
    
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        
        mTvLog = (TextView)findViewById(R.id.textViewLog);
        
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);
		
		mTvLog.setText("mUsbReceiver registered");
		
		if (getLastNonConfigurationInstance() != null) {
			mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
			openAccessory(mAccessory);
		}
        
        
        listenToLedSwitch();
        
        
    }
	

	@Override
	public Object onRetainNonConfigurationInstance() {
		if (mAccessory != null) {
			return mAccessory;
		} else {
			return super.onRetainNonConfigurationInstance();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		Intent intent = getIntent();
		if (mInputStream != null && mOutputStream != null) {
			return;
		}
		
		UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
		

		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				openAccessory(accessory);
			} else {
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						mUsbManager.requestPermission(accessory,
								mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			Log.d(TAG, "mAccessory is null");
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		closeAccessory();
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
	}	


    private void listenToLedSwitch() {
		
    	mTbLedSwitch = (ToggleButton)findViewById(R.id.toggleButtonLed);
    	mTvLedStatus = (TextView)findViewById(R.id.textViewStatus);
    	
    	mTbLedSwitch.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			
				if( isChecked){					
					mTvLedStatus.setText(R.string.led_is_on);
					mTvLog.setText(mTvLog.getText() + "\nLed on" );
					sendTurnOnLedCommand();
				}else{
					mTvLedStatus.setText(R.string.led_is_off);
					mTvLog.setText(mTvLog.getText() + "\nLed off" );
					sendTurnOffLedCommand();
				}
			}


		});
		
	}




	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
	
	
	

	
	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            
            mTvLog.setText(mTvLog.getText() + "\nonReceive action:" + action );
            
            
            if(ACTION_USB_PERMISSION.equals(action)){


                synchronized (this){
                	
                	UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                	                    
            		
                    if( intent.getBooleanExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED,false)){
                        openAccessory(accessory);
                    }else{
                    	mTvLog.setText(mTvLog.getText() + "\npermission denied for accessory:" + accessory );
                        Log.d(TAG, "permission denied for accessory"
                                + accessory);
                    }
                    mPermissionRequestPending = false;
                }

            }else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                
                UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                if (accessory != null && accessory.equals(mAccessory)) {
                    closeAccessory();
                }
            }

        }
    };

    private void openAccessory(UsbAccessory accessory) {
        mFileDescriptor = mUsbManager.openAccessory(accessory);
        if (mFileDescriptor != null) {
            mAccessory = accessory;
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);
            //Thread thread = new Thread(null, this, "TurnOnOffLedDemo");
            //thread.start();
            Log.d(TAG, "accessory opened");
            mTvLog.setText(mTvLog.getText() + "\naccessory opened" );
            enableControls(true);
        } else {
            Log.d(TAG, "accessory open fail");
            mTvLog.setText(mTvLog.getText() + "\naccessory open fail" );
        }
    }

    private void closeAccessory() {
        enableControls(false);

        try {
            if (mFileDescriptor != null) {
                mFileDescriptor.close();
            }
        } catch (IOException e) {
        } finally {
            mFileDescriptor = null;
            mAccessory = null;
        }
    }

    protected void enableControls(boolean enable) {
    }
	

	protected void sendTurnOffLedCommand() {
		
		byte command = 0x1;		
		sendCommand(command);
	}


	protected void sendTurnOnLedCommand() {
		byte command = 0x00;		
		sendCommand(command);
	}
    
	public void sendCommand(byte command){
		byte[] buffer = new byte[1];
		if (mOutputStream != null && buffer[0] != -1) {
			try {
				mOutputStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "write failed", e);
				mTvLog.setText(mTvLog.getText() + "\nwrite failed" + e.toString() );
			}
		}		
	}
    
	public void sendCommand2(byte command, byte target, int value) {
		byte[] buffer = new byte[3];
		if (value > 255)
			value = 255;

		buffer[0] = command;
		buffer[1] = target;
		buffer[2] = (byte) value;
		if (mOutputStream != null && buffer[1] != -1) {
			try {
				mOutputStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "write failed", e);
			}
		}
	}    
    
}
