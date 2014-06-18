package org.opencv.samples.tutorial3;


import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import org.apache.http.client.ClientProtocolException;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera.Size;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Following is based on Tutorial 3 of OpenCV samples.
 * Gives camera control and in OnCameraFrame(CvCameraViewFrame inputFrame) function
 * our algorithm is implemented. It divides the frame into 4 rectangles and it tracks the 
 * number of white pixels in all four regions.  
 */

public class PhototacticActivity extends Activity implements CvCameraViewListener2, OnTouchListener{
	
    private static final String TAG = "OCVSample::Activity";
    private Mat mGray;
    private OptionsView mOpenCvCameraView;
    private List<Size> mResolutionList;
    private MenuItem[] mResolutionMenuItems;
    private SubMenu mResolutionMenu;
    public static boolean RM=false,LM=false,ledoff=true;
    private SubMenu mColorEffectsMenu;
    private MenuItem[] mEffectMenuItems;
    
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    //mOpenCvCameraView.setOnTouchListener(Tutorial3Activity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public PhototacticActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        
        //starting IOIO service
        startService(new Intent(this, MotorControlIOIOService.class));
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.tutorial3_surface_view);

        mOpenCvCameraView = (OptionsView) findViewById(R.id.tutorial3_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
/* Use the LocationManager class to obtain GPS locations */
    	
    	LocationManager mlocManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
    	LocationListener mlocListener = new MyLocationListener();
    	mlocManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 0, 0, mlocListener);

    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    	mGray = new Mat();
    }

    public void onCameraViewStopped() {
    	if (mGray != null)
        mGray.release();
        mGray = null;
    }
   
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	mGray = inputFrame.gray();
        int c1=0,c2=0,c3=0,c4=0;
        Size resolution = mOpenCvCameraView.getResolution();
        int size = (int) mGray.total() * mGray.channels();
        Log.d("we",Integer.toString(size));
        byte[] data = new byte[size];
        mGray.get(0, 0, data);
        //draw boxes
       
                for(int i = 0; i < size; i++)
          {
        	 int s = data[i] & 0xFF;
             data[i] = (byte) ((s>=240) ? 255: 0);
             if(s >=240){
	             int check=i% resolution.width;
	             if(check<(resolution.width/4)-1) {c1++;}
		             else if(check<(resolution.width/2)-1) {c2++;}
		             else if(check<3*(resolution.width/4)-1){c3++;}
		             else if(check<(resolution.width)-1) {c4++;}
		             }
             
          }
                if (c1>0){
                	RM=true;
                	LM=false;
                }else if(c4>0){
                	RM=false;
                	LM=true;
                }else if(c1==0 && c2==0 && c3==0 && c4==0){
                	RM=false;
                	LM=false;
                }else{
                	RM=true;
                	LM=true;
                }
           Log.d("we","c1-"+c1+"-c2-"+c2+"-c3-"+c3+"-c4-"+c4+"--"+resolution.width);
        mGray.put(0, 0, data);
        data=null;
        return mGray;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        List<String> effects = mOpenCvCameraView.getEffectList();

        if (effects == null) {
            Log.e(TAG, "Color effects are not supported by device!");
            return true;
        }

        mColorEffectsMenu = menu.addSubMenu("Color Effect");
        mEffectMenuItems = new MenuItem[effects.size()];

        int idx = 0;
        ListIterator<String> effectItr = effects.listIterator();
        while(effectItr.hasNext()) {
           String element = effectItr.next();
           mEffectMenuItems[idx] = mColorEffectsMenu.add(1, idx, Menu.NONE, element);
           idx++;
        }

        mResolutionMenu = menu.addSubMenu("Resolution");
        mResolutionList = mOpenCvCameraView.getResolutionList();
        mResolutionMenuItems = new MenuItem[mResolutionList.size()];

        ListIterator<Size> resolutionItr = mResolutionList.listIterator();
        idx = 0;
        while(resolutionItr.hasNext()) {
            Size element = resolutionItr.next();
            mResolutionMenuItems[idx] = mResolutionMenu.add(2, idx, Menu.NONE,
                    Integer.valueOf(element.width).toString() + "x" + Integer.valueOf(element.height).toString());
            idx++;
         }

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item.getGroupId() == 1)
        {
            mOpenCvCameraView.setEffect((String) item.getTitle());
            Toast.makeText(this, mOpenCvCameraView.getEffect(), Toast.LENGTH_SHORT).show();
        }
        else if (item.getGroupId() == 2)
        {
            int id = item.getItemId();
            Size resolution = mResolutionList.get(id);
            mOpenCvCameraView.setResolution(resolution);
            resolution = mOpenCvCameraView.getResolution();
            String caption = Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
            Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
        }

        return true;
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG,"onTouch event");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String currentDateandTime = sdf.format(new Date());
        String fileName = Environment.getExternalStorageDirectory().getPath() +
                               "/sample_picture_" + currentDateandTime + ".jpg";
        mOpenCvCameraView.takePicture(fileName);
        Toast.makeText(this, fileName + " saved", Toast.LENGTH_SHORT).show();
        return false;
    }
    //Class HTTPClient

    private void sendPostRequest(String latitude, String longitude) {

    	class SendPostReqAsyncTask extends AsyncTask<String, Void, String>{

    		@Override
    		protected String doInBackground(String... params) {

    			String s1 = params[0];

    			String s2 = params[1];
    			
    					try{
    							URL requestURL = new URL("http://phototacticrover.appspot.com/data?LAT="+s1+"&LON="+s2);
    	
    						    
    							
    							HttpURLConnection requestConnection = (HttpURLConnection) requestURL.openConnection();
    							requestConnection.setRequestProperty("Connection", "Keep-Alive");
    							requestConnection.setDoOutput(true);
    							requestConnection.setRequestMethod("POST");
    							OutputStream reqStream = requestConnection.getOutputStream();
    					
    						if (requestConnection.getResponseCode() != 200) {
    							
    							System.out.println("Failed to send operation list: " + requestConnection.getResponseCode());
    						}
    						
    						reqStream.close();
    						
    						requestConnection.disconnect();
    						return "working"; 
    					} catch (ClientProtocolException cpe) {
    						System.out.println("Failed in operation sending: " + cpe);
    						cpe.printStackTrace();
    					}
    			
    					catch (IOException ioe) {
    						System.out.println("Second Exception caz of HttpResponse :" + ioe);
    						ioe.printStackTrace();
    					}
    				
    				return "did not work";
    				
    		}
    			
    		protected void onPostExecute(String result) {
    			super.onPostExecute(result);
    		}			
    	}

    	SendPostReqAsyncTask sendPostReqAsyncTask = new SendPostReqAsyncTask();
    	sendPostReqAsyncTask.execute(latitude,longitude);		
    }

    public class MyLocationListener implements LocationListener
    {
    	
    @Override
    public void onLocationChanged(Location loc)
    {
    		loc.getLatitude();
    		loc.getLongitude();
    		sendPostRequest(String.valueOf(loc.getLatitude()),String.valueOf(loc.getLongitude()));

    }

    @Override

    public void onProviderDisabled(String provider)
    {
    	Toast.makeText( getApplicationContext(),"Gps Disabled",Toast.LENGTH_SHORT ).show();
    }

    @Override
    public void onProviderEnabled(String provider)
    {
    	Toast.makeText( getApplicationContext(),"Gps Enabled",Toast.LENGTH_SHORT).show();
    }

    @Override

    public void onStatusChanged(String provider, int status, Bundle extras)
    {
    }
    }/* End of Class MyLocationListener */
   
}
