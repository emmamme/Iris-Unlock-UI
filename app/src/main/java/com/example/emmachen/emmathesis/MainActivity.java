package com.example.emmachen.emmathesis;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.Permission;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "MainActivity";
    float ax, ay, az;
    static int randBound = 3;
    SensorManager sensorManager;
    private ImageButton getpicture;
    private Size previewsize;
    private Size jpegSizes[]=null;
    private TextureView textureView;
    private CameraDevice cameraDevice;
    private CaptureRequest.Builder previewBuilder;
    private CameraCaptureSession previewSession;
    private static final SparseIntArray ORIENTATIONS=new SparseIntArray();
    static
    {
        ORIENTATIONS.append(Surface.ROTATION_0,90);
        ORIENTATIONS.append(Surface.ROTATION_90,0);
        ORIENTATIONS.append(Surface.ROTATION_180,270);
        ORIENTATIONS.append(Surface.ROTATION_270,180);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GifImageView gifImageView = (GifImageView) findViewById(R.id.GifImageView);
        gifImageView.setGifImageResource(R.drawable.android);

        Intent intent = new Intent(this, FaceTrackerActivity.class);
        startActivity(intent);

        int choice = (int) (Math.random()*10) % randBound;

        ImageView mask = ((ImageView) findViewById(R.id.daisy));

        switch (choice) {
            case 0:
                //do nothing
                break;

            case 1:
                mask.setImageResource(R.drawable.pikachu_mask);
                break;

            case 2:
                //do nothing
                break;

        }
        /*decalre sensor instance*/
        sensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

        textureView=(TextureView)findViewById(R.id.texture);
        textureView.setSurfaceTextureListener(surfaceTextureListener);
        getpicture=(ImageButton)findViewById(R.id.button_capture);
        getpicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPicture();
            }
        });
    }
    
    void getPicture()
    {
        if(cameraDevice==null)
        {
            return;
        }
        CameraManager manager=(CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try
        {
            CameraCharacteristics characteristics=manager.getCameraCharacteristics(cameraDevice.getId());
            if(characteristics!=null)
            {
                jpegSizes=characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
            }
            int width=1280,height=720;
            /*if(jpegSizes!=null && jpegSizes.length>0)
            {
                width=jpegSizes[0].getWidth();
                height=jpegSizes[0].getHeight();
            }*/
            ImageReader reader=ImageReader.newInstance(width,height,ImageFormat.JPEG,1);
            List<Surface> outputSurfaces=new ArrayList<Surface>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
            final CaptureRequest.Builder capturebuilder=cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            capturebuilder.addTarget(reader.getSurface());
            capturebuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            int rotation=getWindowManager().getDefaultDisplay().getRotation();
            capturebuilder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(rotation));
            ImageReader.OnImageAvailableListener imageAvailableListener=new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireLatestImage();

                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                        //final File faceFile = save(bytes);
                        Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);

                        writeToAlbum(rotateImage(bitmapImage, 270));
                    } catch (Exception ee) {
                        ee.printStackTrace();
                    }
                    finally {
                        if(image!=null)
                            image.close();
                    }
                }

                File save(byte[] bytes)
                {
                    File file12=getOutputMediaFile();
                    OutputStream outputStream=null;
                    try
                    {
                        outputStream=new FileOutputStream(file12);
                        outputStream.write(bytes);
                    }catch (Exception e)
                    {
                        e.printStackTrace();
                    }finally {
                        try {
                            if (outputStream != null)
                                outputStream.close();
                        }catch (Exception e){}
                    }
                    return file12;
                }
            };
            HandlerThread handlerThread=new HandlerThread("takepicture");
            handlerThread.start();
            final Handler handler=new Handler(handlerThread.getLooper());
            reader.setOnImageAvailableListener(imageAvailableListener,handler);
            final CameraCaptureSession.CaptureCallback  previewSSession=new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                }
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    startCamera();
                }
            };
            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try
                    {
                        session.capture(capturebuilder.build(),previewSSession,handler);
                    }catch (Exception e)
                    {
                    }
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            },handler);
        }
        catch (Exception e)
        {
        }
    }

    public  void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            String cameraId = manager.getCameraIdList()[1];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            previewsize = map.getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e)
        {
            e.printStackTrace();
        }

        Log.e(TAG, "openCamera X");
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener=new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };
    private CameraDevice.StateCallback stateCallback=new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice=camera;
            startCamera();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
        }
        @Override
        public void onError(CameraDevice camera, int error) {
        }
    };
    @Override
    protected void onPause() {
        super.onPause();
        if(cameraDevice!=null)
        {
            cameraDevice.close();
        }
    }
    void  startCamera()
    {
        if(cameraDevice==null||!textureView.isAvailable()|| previewsize==null)
        {
            return;
        }
        SurfaceTexture texture=textureView.getSurfaceTexture();
        if(texture==null)
        {
            return;
        }
        texture.setDefaultBufferSize(previewsize.getWidth(),previewsize.getHeight());
        Surface surface=new Surface(texture);
        try
        {
            previewBuilder=cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        }catch (Exception e)
        {
        }
        previewBuilder.addTarget(surface);
        try
        {
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    previewSession=session;
                    getChangedPreview();
                }
                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            },null);
        }catch (Exception e)
        {
        }
    }
    void getChangedPreview()
    {
        if(cameraDevice==null)
        {
            return;
        }
        previewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        HandlerThread thread=new HandlerThread("changed Preview");
        thread.start();
        Handler handler=new Handler(thread.getLooper());
        try
        {
            previewSession.setRepeatingRequest(previewBuilder.build(), null, handler);
        }catch (Exception e){}
    }

    private File getOutputMediaFile() {
        return new File(getFilesDir(), "picMe.jpg");
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            ax=event.values[0];
            ay=event.values[1];
            az=event.values[2];

            Log.d("x-y-z", ax + " " + ay + " " + az);

            distort();

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //
    }


    void distort(){

        ImageView daisy = (ImageView) findViewById(R.id.daisy);

        Bitmap bitmap = ((BitmapDrawable)daisy.getDrawable()).getBitmap();

        Log.d("IMG", "Width and Height: " + daisy.getMeasuredWidth() + " " + bitmap.getWidth() + " " + daisy.getMeasuredHeight() + " "
                + bitmap.getHeight());

        Matrix matrix = new Matrix();

        float dw = bitmap.getWidth();

        float dratio = (float)Math.min(10f, Math.abs(0 - az)) * 0.025f;

        float xratio = (float)Math.min(10f, Math.abs(0 - ax)) * 0.025f;

        float x[] = {dw*dratio, dw*(1-dratio), 0, dw};

        float y[] = {0, 0, dw, dw};

        if(ax > 0){
            y[1] = dw*xratio;
            y[3] = dw*(1-xratio);
        }else if(ax < 0){
            y[0] = dw*xratio;
            y[2] = dw*(1-xratio);
        }

        Log.d("XY", y.toString() + " " + x.toString());

        float sx = (float)daisy.getWidth()/(float)bitmap.getWidth();

        matrix.setPolyToPoly(
                new float[] {
                        0, 0,
                        dw, 0,
                        0, dw,
                        dw, dw
                }, 0,
                new float[] {
                        x[0], y[0],
                        x[1], y[1],
                        x[2], y[2],
                        x[3], y[3]
                }, 0,
                4);

        daisy.setScaleType(ImageView.ScaleType.MATRIX);

        matrix.postScale(sx, sx);

        daisy.setImageMatrix(matrix);
    }

    public void writeToAlbum(final Bitmap face){
        ImageView daisy = (ImageView) findViewById(R.id.daisy);

        Bitmap flower = ((BitmapDrawable)daisy.getDrawable()).getBitmap();

        ImageView back = (ImageView) findViewById(R.id.b_back);

        Bitmap blk = ((BitmapDrawable)back.getDrawable()).getBitmap();

        int w = face.getWidth();

        int h = face.getHeight();

        final Bitmap result = Bitmap.createBitmap(w, h, face.getConfig());

        Canvas canvas = new Canvas(result);

        canvas.drawBitmap(face, null, new Rect(0, 0, w, h), null);

        canvas.drawBitmap(blk, null, new Rect(0, 0, w, h), null);

        canvas.drawBitmap(flower, null, new Rect(0, 0, w, w), null);

        Log.d("SAVEIMG", "Saving face unlock");

        CapturePhotoUtils.insertImage(getContentResolver(), result, "FACE_UNLOCK.jpg", "LALALA");

        final Bitmap res = result;

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Bitmap bmp = BitmapFactory.decodeFile((new File(getFilesDir(), "picMe.jpg")).getPath());

                ((ImageView) findViewById(R.id.little_dis)).setImageBitmap(result);


            }
        });

        //ByteArrayOutputStream stream = new ByteArrayOutputStream();

        //result.compress(Bitmap.CompressFormat.PNG, 100, stream);

        //return stream.toByteArray();

    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

}