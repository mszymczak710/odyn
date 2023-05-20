package com.example.odyn.cam;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import com.example.odyn.FileHandler;
import com.example.odyn.R;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("ALL")
public class CamAccess {
    private ImageCapture imageCapture;
    private VideoCapture videoCapture;
    protected Activity main; // póki co spełnia dwie role: wątek (Context) i aktywność (wyświetlanie), później warto rozważyć rozdzielenie
    // korzysta z tego też klasa Cam (dziedziczy)
    /* Activity używane do:
        dostarczenia FileHandler'owi kontekstu x2
        bind widoku PreviewView
        otrzymanie CameraProvider
        otrzymanie wykonawcy x3
        menedżera okien
        otrzymanie LiveCycleOwner
        otrzymanie CameraManager
        utworzenie tosta
    */
    // konstruktor. PreviewView służy do wyświetlenia w nim obrazu z kamery
    public CamAccess(Activity main) {
        this.main = main;
        PreviewView prView2 = main.findViewById(R.id.previewView);
        cameraProviderSetup(prView2);
        Log.v("CamAccess", ">>> CamAccess constructor");
    }
    // te dwie poniższe funkcje służą do przygotowania kamery do przekazywania obrazu do <PreviewView> i robienia zdjęć
    @SuppressLint("RestrictedApi")
    private void cameraProviderSetup(PreviewView prView) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(main);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider, prView);
            } catch (ExecutionException | InterruptedException e) {
                // gdzie przechwycenie ???
            }
        }, ContextCompat.getMainExecutor(main));
    }
    @SuppressLint("RestrictedApi")
    private void bindPreview(ProcessCameraProvider cameraProvider, PreviewView prView) {

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        // Set up the preview use case
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(prView.getSurfaceProvider());
        ImageCapture.Builder builder = new ImageCapture.Builder();

        imageCapture = builder
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(main.getWindowManager().getDefaultDisplay().getRotation())
                .build();

        /* TODO settings nagrywanie z i bez dzwieku */
        //if(dzwiek) {

    VideoCapture.Builder builder_vid = new VideoCapture.Builder();
    videoCapture = builder_vid
            .setVideoFrameRate(60)
            .setAudioChannelCount(1)
            .setAudioBitRate(64000)
            .build();
//}else {
    VideoCapture.Builder builder_vid_noaudio = new VideoCapture.Builder();
    videoCapture = builder_vid_noaudio
            .setVideoFrameRate(60)
            .setAudioChannelCount(0)
            .setAudioBitRate(64000)
            .build();
//}
        // użyj kamery do wyświetlania w mainActivity (preview) i do robienia zdjęć (imageCapture)
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) main, cameraSelector, preview, imageCapture, videoCapture);
    }
    // robi zdjęcie
    public void takePicture(File file) {
        // Set up the output file and capture the image
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(main), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                // The image has been saved to the file
                Log.v("CamAccess", "---------ZapisywanieIMG---------");
            }
            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                // Handle any errors here
            }
        });
    }
    public class camInfo {
        private float FOV;
        private float width;
        private float height;
        private Bitmap BMP;
        private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
            Image image = imageProxy.getImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        public void takePictureBMP(){
            imageCapture.takePicture(ContextCompat.getMainExecutor(main), new ImageCapture.OnImageCapturedCallback() {
                @Override
                public void onCaptureSuccess(@NonNull ImageProxy image) {
                    // Tutaj otrzymujesz obraz z kamery, możesz go przetwarzać lub zapisać w pamięci
                    // Uwaga: Ta metoda jest wywoływana na innym wątku, więc musisz obsłużyć go odpowiednio
                    super.onCaptureSuccess(image);
                    BMP = imageProxyToBitmap(image);
                    // Zapisz obraz w pamięci podręcznej
                }
                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    // Obsłuż błędy związane z wykonywaniem zdjęcia
                    super.onError(exception);
                }
            });
        }
        public float calculateFOV(float focalLength, float aperture) {
            float horizontalFOV = (float) (2 * Math.atan2(aperture, (2 * focalLength)));
            float verticalFOV = (float) (2 * Math.atan2(aperture, (2 * focalLength)));
            return (float) Math.toDegrees(Math.sqrt(Math.pow(horizontalFOV, 2) + Math.pow(verticalFOV, 2)));
        }
        public void getInfo() {
            try {
                CameraManager cameraManager = (CameraManager) main.getSystemService(Context.CAMERA_SERVICE);
                String cameraId = cameraManager.getCameraIdList()[1]; // wybierz pierwszą kamerę
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                // uzyskanie wartości FOV
                float[] focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                float[] apertures = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES);
                FOV = calculateFOV(focalLengths[0], apertures[0]);

                // uzyskanie wartości rozdzielczości
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size[] sizes = map.getOutputSizes(SurfaceTexture.class);
                Size resolution = sizes[0];
                width = resolution.getWidth();
                height = resolution.getHeight();

            } catch (CameraAccessException e) {
                e.printStackTrace();
                Toast.makeText(main, "Wystąpił błąd podczas korzystania z kamery", Toast.LENGTH_LONG).show();
                Log.e("CamAccess", ">>> Wystąpił błąd podczas korzystania z kamery");
            }
        }
    }
    Timer timer = new Timer();
    @SuppressLint({"RestrictedApi", "MissingPermission"})
    public void takeVideo(boolean opcja) {
        if(opcja) {
            TimerTask task = new TimerTask() {
                int count = 0;
                public void run() {
                    if(count == 0)
                    {
                        File file = new FileHandler(main).createVideo("mp4");
                        VideoCapture.OutputFileOptions outputFileOptions = new VideoCapture.OutputFileOptions.Builder(file).build();
                        videoCapture.startRecording(outputFileOptions, ContextCompat.getMainExecutor(main), new VideoCapture.OnVideoSavedCallback() {
                            @Override
                            public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                                System.out.println("----------ZapisywanieVID----------");
                                Log.i("CamAccess", ">>> Zapisano nagranie");
                            }
                            @Override
                            public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                                System.out.println("----------GownoVID----------");
                                Log.e("CamAccess", ">>> Nie udało się zapisać nagrania");
                            }
                        });
                    }
                    count++;
                    /* TODO settings - dlugosc nagrania */
                    //System.out.println("Czas: " + count + " sekund");
                    //10+2 -> 2 to opoznienie aby nagrac film 10 sekundowy
                    if (count >= 10+2) {
                        videoCapture.stopRecording();
                        count = 0;
                    }
                }
            };
            timer.schedule(task, 0, 1000);
        }
        else
        {
             timer.cancel();
             videoCapture.stopRecording();
        }
    } // end of takeVideo()

}
