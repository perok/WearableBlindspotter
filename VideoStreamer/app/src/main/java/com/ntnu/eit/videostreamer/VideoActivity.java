package com.ntnu.eit.videostreamer;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * http://stackoverflow.com/questions/6116880/stream-live-video-from-phone-to-phone-using-socket-fd
 */
public class VideoActivity extends Activity {//implements SurfaceHolder.Callback{

    //private static final String TAG = VideoActivity.class.getSimpleName();
    //private Camera camera;
    //private boolean previewRunning;

    VideoView mView;
    TextView connectionStatus;
    SurfaceHolder mHolder;
    // Video variable
    MediaRecorder recorder;
    // Networking variables
    public static String SERVERIP="";
    public static final int SERVERPORT = 6775;
    private Handler handler = new Handler();
    private ServerSocket serverSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        mView = (VideoView) findViewById(R.id.video_preview);
        connectionStatus = (TextView) findViewById(R.id.connection_status_textview);
        mHolder = mView.getHolder();
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        SERVERIP = "192.168.1.126";

        // Run new thread to handle socket communications
        Thread sendVideo = new Thread(new SendVideoThread());
        sendVideo.start();
    }

//    @Override
//    public void surfaceCreated(SurfaceHolder holder) {
//        camera = Camera.open();
//        if (camera != null){
//            Camera.Parameters params = camera.getParameters();
//            camera.setParameters(params);
//        }
//        else {
//            Toast.makeText(getApplicationContext(), "Camera not available!", Toast.LENGTH_LONG).show();
//            finish();
//        }
//    }
//
//    @Override
//    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//        if (previewRunning){
//            camera.stopPreview();
//        }
//        Camera.Parameters p = camera.getParameters();
//        p.setPreviewSize(width, height);
//        p.setPreviewFormat(PixelFormat.JPEG);
//        camera.setParameters(p);
//
//        try {
//            camera.setPreviewDisplay(holder);
//            camera.startPreview();
//            previewRunning = true;
//        }
//        catch (IOException e) {
//            Log.e(TAG, e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    public void surfaceDestroyed(SurfaceHolder holder) {
//        camera.stopPreview();
//        previewRunning = false;
//        camera.release();
//    }

    public class SendVideoThread implements Runnable {
        public void run() {
            // From Server.java
            try {
                if (SERVERIP != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            connectionStatus.setText("Broadcasting on IP: " + SERVERIP + ":" + SERVERPORT);
                        }
                    });
                    serverSocket = new ServerSocket(SERVERPORT);

                    while (true) {
                        //listen for incoming clients
                        Socket client = serverSocket.accept();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                connectionStatus.setText("Connected.");
                            }
                        });
                        try {
                            // Begin video communication
                            final ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(client);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    recorder = new MediaRecorder();
                                    //recorder.setCamera(camera);
                                    recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                                    recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                                    recorder.setOutputFile(pfd.getFileDescriptor());
                                    recorder.setOutputFormat(8);//MediaRecorder.OutputFormat.);
                                    recorder.setVideoFrameRate(20);
                                    recorder.setVideoSize(176, 144);
                                    recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
                                    recorder.setPreviewDisplay(mHolder.getSurface());
                                    try {
                                        recorder.prepare();
                                    } catch (IllegalStateException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    recorder.start();
                                }
                            });
                        } catch (Exception e) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    connectionStatus.setText("Oops.Connection interrupted. Please reconnect your phones.");
                                }
                            });
                            e.printStackTrace();
                        }
                    }
                } else {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            connectionStatus.setText("Couldn't detect internet connection.");
                        }
                    });
                }
            } catch (Exception e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        connectionStatus.setText("Error");
                    }
                });
                e.printStackTrace();
            }
            // End from server.java
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_video, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
