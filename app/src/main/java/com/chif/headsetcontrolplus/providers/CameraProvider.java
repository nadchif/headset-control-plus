package com.chif.headsetcontrolplus.providers;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import com.chif.headsetcontrolplus.HeadsetControlPlusService;
import com.chif.headsetcontrolplus.R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CameraProvider extends Activity {
  private static final String APP_TAG = HeadsetControlPlusService.class.getSimpleName();
  private static final int REQUEST_TAKE_PHOTO = 1;
  private CameraManager mCameraManager;
  private CameraDevice mCameraDevice;
  private String mCameraId;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // get the action to perform
    String action = getIntent().getStringExtra("action");
    Log.i(APP_TAG, action);

    if (action.equals("open_camera")) {
      openCamera();
    }
    if (action.equals("take_photo")) {
      openCameraDevice();
    }
  }


  File mImageFile;

  /**
   * This method creates opens the camera app and saves the image to DCIM.
   */
  public void openCamera() {
    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (intent.resolveActivity(getPackageManager()) != null) {
      // create image filename
      String imageTimestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
      String imageFileName = "IMG_" + imageTimestamp;
      Log.i(APP_TAG, "Filename:" + imageFileName);

      // create the file for the image
      Uri photoUri;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, imageFileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM");

        ContentResolver contentResolver = this.getContentResolver();
        photoUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        contentValues);
      } else {
        // check if DCIM folder exists
        File imageDirectory = new File(Environment.getExternalStorageDirectory() + "/DCIM");
        if (!imageDirectory.exists()) {
          imageDirectory.mkdir();
        }

        // create the file and get the URI
        String pathName = Environment.getExternalStorageDirectory() + "/DCIM/";
        mImageFile = new File(pathName + imageFileName + ".jpg");
        photoUri = FileProvider.getUriForFile(this,
            "com.chif.headsetcontrolplus.fileprovider",
            mImageFile);
      }

      if (photoUri != null) {
        // starts the camera app intent
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(intent, REQUEST_TAKE_PHOTO);
      } else {
        finish();
      }
    } else {
      finish();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    // when a picture is taken, update the gallery
    if (requestCode == REQUEST_TAKE_PHOTO) {
      if (resultCode == RESULT_OK && mImageFile != null) {
        updateGallery(Uri.fromFile(mImageFile));
      }

      finish();
    }
  }

  /**
   * Makes the image visible after it is saved.
   * This is only necessary for phones API 28 and under.
   *
   * @param photoUri - uri of the image file
   */
  private void updateGallery(Uri photoUri) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      return;
    } else {
      sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, photoUri));
    }
  }

  /**
   * Finds a back-facing camera and opens a connection.
   * If it doesn't find one, it finishes the activity.
   */
  private void openCameraDevice() {
    mCameraManager = (CameraManager) this.getSystemService(this.CAMERA_SERVICE);
    try {
      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
          == PackageManager.PERMISSION_GRANTED) {
        // find the back facing camera
        for (String cameraId : mCameraManager.getCameraIdList()) {
          CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);

          Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
          if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
            continue;
          }

          StreamConfigurationMap map = characteristics.get(
              CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

          if (map == null) {
            continue;
          }

          // open the camera device
          mCameraId = cameraId;
          mCameraManager.openCamera(mCameraId, new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
              // set the camera
              mCameraDevice = camera;

              // take a photo, but at a delay of 500ms to resolve dark photos
              new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                  try {
                    createCaptureSession();
                  } catch (CameraAccessException e) {
                    e.printStackTrace();
                    finish();
                  }
                }
              }, 250);
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
              finish();
            }

            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
              finish();
            }
          }, null);

          break;
        }

        // if no camera available, then end activity
        if (mCameraId == null) {
          finish();
        }
      } else {
        Log.i(APP_TAG, "Camera permissions are not granted");
        finish();
      }
    } catch (CameraAccessException e) {
      e.printStackTrace();
      finish();
    }
  }

  /**
   * This method configures the imageSize and the camera settings,
   * then it starts the capture session.
   *
   * @throws CameraAccessException exception if mCameraDevice is null
   */
  private void createCaptureSession() throws CameraAccessException {
    if (mCameraDevice == null) {
      throw new CameraAccessException(CameraAccessException.CAMERA_ERROR);
    }

    // get the camera characteristics
    CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(
        mCameraDevice.getId());
    StreamConfigurationMap streamConfigurationMap = characteristics.get(
        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

    // set the image output size
    int width = 640;
    int height = 480;
    Size[] imageSize = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);

    if (imageSize != null && imageSize.length != 0) {
      width = imageSize[0].getWidth();
      height = imageSize[0].getHeight();
    }
    Log.i(APP_TAG, "Image Size: " + width + "x" + height);

    // create the image reader to read one image
    ImageReader imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2);
    List<Surface> outputSurfaces = new ArrayList<>();
    outputSurfaces.add(imageReader.getSurface());

    final CaptureRequest.Builder captureBuilder = mCameraDevice.createCaptureRequest(
        CameraDevice.TEMPLATE_PREVIEW);
    captureBuilder.addTarget(imageReader.getSurface());

    captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, characteristics.get(
        CameraCharacteristics.SENSOR_ORIENTATION));

    imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
      @Override
      public void onImageAvailable(ImageReader reader) {
        // save the image and close
        saveImage(reader.acquireLatestImage());

        // close the camera device and imageReader
        if (mCameraDevice != null) {
          mCameraDevice.close();
          mCameraDevice = null;
        }

        finish();
      }
    }, null);

    mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
      @Override
      public void onConfigured(@NonNull CameraCaptureSession session) {
        try {
          session.setRepeatingRequest(captureBuilder.build(), null, null);
        } catch (CameraAccessException e) {
          e.printStackTrace();
          finish();
        }
      }

      @Override
      public void onConfigureFailed(@NonNull CameraCaptureSession session) {
        finish();
      }
    }, null);
  }

  /**
   * Saves the latest image taken from the CameraDevice.
   */
  private int mImageCount = 0;

  private void saveImage(Image image) {
    if (image == null) {
      return;
    }

    // counts images to only save the last
    mImageCount++;
    if (mImageCount == 2) {
      // give image data to buffer
      ByteBuffer buffer = image.getPlanes()[0].getBuffer();
      final byte[] bytes = new byte[buffer.capacity()];
      buffer.get(bytes);

      // use the same timestamp for any new images received
      String imageTimestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
      String imageFileName = "IMG_" + imageTimestamp;
      Log.i(APP_TAG, imageFileName);

      Uri photoUri;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, imageFileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM");

        ContentResolver resolver = this.getContentResolver();
        photoUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        try (OutputStream outputStream = resolver.openOutputStream(photoUri)) {
          outputStream.write(bytes);
          Toast.makeText(this, getString(R.string.image_saved), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else {
        String pathName = Environment.getExternalStorageDirectory() + "/DCIM/";
        File imageFile = new File(pathName + imageFileName + ".jpg");
        photoUri = FileProvider.getUriForFile(this,
            "com.chif.headsetcontrolplus.fileprovider",
            imageFile);

        try (OutputStream outputStream = new FileOutputStream(imageFile)) {
          outputStream.write(bytes);
          Toast.makeText(this, getString(R.string.image_saved), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
          e.printStackTrace();
        }

        updateGallery(Uri.fromFile(imageFile));
      }
    }

    image.close();
  }
}
