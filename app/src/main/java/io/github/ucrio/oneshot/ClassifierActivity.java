/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ucrio.oneshot;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import io.github.ucrio.oneshot.env.BorderedText;
import io.github.ucrio.oneshot.env.ImageUtils;
import io.github.ucrio.oneshot.env.Logger;
import io.github.ucrio.oneshot.tflite.Classifier;
import io.github.ucrio.oneshot.tflite.Classifier.Device;
import io.github.ucrio.oneshot.tflite.Classifier.Model;

public class ClassifierActivity extends CameraActivity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();
  //private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
  private static final Size DESIRED_PREVIEW_SIZE = new Size(1920, 1080);
  private static final float TEXT_SIZE_DIP = 10;
  private Bitmap rgbFrameBitmap = null;
  private long lastProcessingTimeMs;
  private Integer sensorOrientation;
  private Classifier classifier;
  private BorderedText borderedText;
  /** Input image size of the model along x axis. */
  private int imageSizeX;
  /** Input image size of the model along y axis. */
  private int imageSizeY;

  private int savedNum = 0;
  private final int SAVE_MAX_1 = 30;
  private final int SAVE_MAX_2 = 50;

  private boolean isPausing = false;

  SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_hhmmssSSS");

  @Override
  protected int getLayoutId() {
    return R.layout.camera_connection_fragment;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    recreateClassifier(getModel(), getDevice(), getNumThreads());
    if (classifier == null) {
      LOGGER.e("No classifier on preview!");
      return;
    }

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
  }

  @Override
  protected void processImage() {
    if (isPausing) {
      return;
    }

    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
    final int cropSize = Math.min(previewWidth, previewHeight);

    final Handler handler = new Handler();
    runInBackground(
        new Runnable() {
          @Override
          public void run() {
            if (classifier != null) {
              final long startTime = SystemClock.uptimeMillis();
              final List<Classifier.Recognition> results =
                  classifier.recognizeImage(rgbFrameBitmap, sensorOrientation);
              lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
              LOGGER.v("Detect: %s", results);

              if (checkResult(results)) {
                onRecognized(sensorOrientation, rgbFrameBitmap);
              }

              runOnUiThread(
                  new Runnable() {
                    @Override
                    public void run() {
                      showResultsInBottomSheet(results);
                      showFrameInfo(previewWidth + "x" + previewHeight);
                      showCropInfo(imageSizeX + "x" + imageSizeY);
                      showCameraResolution(cropSize + "x" + cropSize);
                      showRotationInfo(String.valueOf(sensorOrientation));
                      showInference(lastProcessingTimeMs + "ms");
                    }
                  });
            }
            readyForNextImage();
          }
        });
  }

  @Override
  protected void onInferenceConfigurationChanged() {
    if (rgbFrameBitmap == null) {
      // Defer creation until we're getting camera frames.
      return;
    }
    final Device device = getDevice();
    final Model model = getModel();
    final int numThreads = getNumThreads();
    runInBackground(() -> recreateClassifier(model, device, numThreads));
  }

  private void recreateClassifier(Model model, Device device, int numThreads) {
    if (classifier != null) {
      LOGGER.d("Closing classifier.");
      classifier.close();
      classifier = null;
    }
    if (device == Device.GPU
        && (model == Model.QUANTIZED_MOBILENET || model == Model.QUANTIZED_EFFICIENTNET)) {
      LOGGER.d("Not creating classifier: GPU doesn't support quantized models.");
      runOnUiThread(
          () -> {
            Toast.makeText(this, R.string.tfe_ic_gpu_quant_error, Toast.LENGTH_LONG).show();
          });
      return;
    }
    try {
      LOGGER.d(
          "Creating classifier (model=%s, device=%s, numThreads=%d)", model, device, numThreads);
      classifier = Classifier.create(this, model, device, numThreads);
    } catch (IOException e) {
      LOGGER.e(e, "Failed to create classifier.");
    }

    // Updates the input image size.
    imageSizeX = classifier.getImageSizeX();
    imageSizeY = classifier.getImageSizeY();
  }

  protected boolean checkResult(List<Classifier.Recognition> results) {
    if (results != null && results.size() >= 1) {
      Classifier.Recognition recognition = results.get(0);
      if (recognition != null) {
        if (recognition.getTitle() != null && recognition.getConfidence() != null) {
          // LOGGER.v(String.valueOf(recognition.getConfidence()));
          if ("Facing me".equals(recognition.getTitle()) && recognition.getConfidence() > getConfidenceThreshold()) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private void onRecognized(Integer sensorOrientation, Bitmap rgbFrameBitmap) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        saveImage(true);
      }
    }).start();
  }

  private void showAlert() {
    if (savedNum == SAVE_MAX_1) {
      showAlert(0);
      pause(true);
    } else if (savedNum == SAVE_MAX_2) {
      showAlert(1);
      pause(true);
    }
  }

  private void showAlert(int type) {
      AlertDialogFragment alert = new AlertDialogFragment(this, type, SAVE_MAX_1, SAVE_MAX_2);
      alert.show(getSupportFragmentManager(), "");
  }

  public void pause(boolean pause) {
    isPausing = pause;
  }

  private void saveImage(boolean autoSaveCounter) {
    Matrix mat = new Matrix();
    mat.postRotate(sensorOrientation);
    Bitmap cap = Bitmap.createBitmap(rgbFrameBitmap, 0, 0, rgbFrameBitmap.getWidth(), rgbFrameBitmap.getHeight(), mat, true);
    boolean saveResult = ImageUtils.saveBitmap(cap, sdf.format(Calendar.getInstance().getTime()), getContentResolver());
    if (saveResult) {
      if (autoSaveCounter) {
        savedNum++;
      }
    } else {
      Toast.makeText(this , getString(R.string.error_failed_to_save), Toast.LENGTH_LONG).show();
    }
    runOnUiThread(
            new Runnable() {
              @Override
              public void run() {
                updatePreviewThumbnail(cap);
                applyEffect();
                if (autoSaveCounter) {
                  showAlert();
                }
              }
            });
  }

  @Override
  protected void shutter() {
    saveImage(false);
  }
}
