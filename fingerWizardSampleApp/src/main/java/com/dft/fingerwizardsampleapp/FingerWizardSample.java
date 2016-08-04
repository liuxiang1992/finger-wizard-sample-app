package com.dft.fingerwizardsampleapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;

import com.dft.onyx.FingerprintTemplate;
import com.dft.onyx.MatVector;
import com.dft.onyx.core;
import com.dft.onyx.enroll.util.Consts;
import com.dft.onyx.enroll.util.EnrollmentMetric;
import com.dft.onyx.fingerwizard.FingerWizard;
import com.dft.onyx.fingerwizard.FingerWizardIntentHelper;
import com.dft.onyx.wizardroid.enrollwizard.EnrollWizardBuilder;
import com.dft.onyxcamera.ui.CaptureConfiguration;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;


public class FingerWizardSample extends Activity {
    private static final String TAG = "FingerWizardSample";
    private static final int FINGER_WIZARD_REQUEST_CODE = 1234;
    private File mPreprocessedImageFile;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Unable to load OpenCV!");
        } else {
            Log.i(TAG, "OpenCV loaded successfully");
            core.initOnyx();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create a file to hold the preprocessed image bitmap
        mPreprocessedImageFile = new File(Environment.getExternalStorageDirectory(),
                "FingerImage.png");

        // create a new EnrollWizardBuilder object to get an intent and start it for a result
        // It needs the context, a boolean as to use the SelfEnrollOnyx license key, a boolean as to whether to show the Onyx Guide,
        // a boolean as to whether to invert the image (invert valleys and ridges to make it like an
        // image received from a touch-based sensor),
        // a CaptureConfiguration.Flip defining the FLIP_TYPE to apply to the preprocessed bitmap,
        // the file to save the preprocessed bitmap image to,
        // and the image's compression type
        EnrollWizardBuilder ewb = new EnrollWizardBuilder().setUseOnyxGuide(true, true, false).
                setUseSelfEnroll(true).
                setLicenseKey(getResources().getString(R.string.onyx_license));
        if(null != mPreprocessedImageFile) {
            ewb.setPreprocessedFile(mPreprocessedImageFile, Bitmap.CompressFormat.PNG);
        }

        Intent fingerWizardIntent = ewb.build(this);
        boolean invertImage = true;
        fingerWizardIntent.putExtra("extra_invert_image", invertImage);
        fingerWizardIntent.putExtra("extra_flip_type", CaptureConfiguration.Flip.HORIZONTAL);
        fingerWizardIntent.setClass(this, FingerWizard.class);
        startActivityForResult(fingerWizardIntent, FINGER_WIZARD_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (FINGER_WIZARD_REQUEST_CODE == requestCode) {
            // If you want to display the preprocessed image on screen, put it in an ImageView
            // or similar
            ImageView preprocessedBitmapImageView = (ImageView) findViewById(
                    R.id.preprocessed_bitmap_image_view);
            Bitmap processedBitmap = null;
            // Decode the bitmap from the file
            try {
                FileInputStream fis = new FileInputStream(mPreprocessedImageFile);
                InputStream buffer = new BufferedInputStream(fis);
                processedBitmap = BitmapFactory.decodeStream(buffer);
                buffer.close();
            } catch (IOException iox) {
                Log.e(TAG, "Cannot perform input of processedBitmapFile. " + iox);
            }
            // TODO: this is an example of how to show the preprocessedBitmap.  In a complete application, we'd need
            // to handle screen orientation changes
            preprocessedBitmapImageView.setImageBitmap(processedBitmap);

            /**
             * It is possible to use Onyx's built-in image pyramiding as follows
             */
            if (processedBitmap != null) {
                double[] imageScales = new double[] { 0.8, 1.0, 1.2 }; // 80%, 100%, and 120%
                ArrayList<byte[]> scaledWSQs = new ArrayList<byte[]>();

                Mat mat = new Mat();
                Utils.bitmapToMat(processedBitmap, mat);
                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY); // ensure image is grayscale

                MatVector vector = core.pyramidImage(mat, imageScales);
                for (int i = 0; i < imageScales.length; i++) {
                    scaledWSQs.add(core.matToWsq(vector.get(i)));
                }

                for (int i = 0; i < scaledWSQs.size(); i++) {
                    // TODO: send scaledWSQs.get(i) to server for matching...
                    File inputFile = new File(Environment.getExternalStorageDirectory(),
                            "matToWsQ" + System.currentTimeMillis() / 1000 + ".wsq");

                    try {
                        FileOutputStream fos = new FileOutputStream(inputFile.getPath());
                        fos.write(scaledWSQs.get(i));
                        fos.close();
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }

            // Get the EnrollmentMetric
            EnrollmentMetric em = null;
            if (data != null && data.hasExtra(Consts.EXTRA_ENROLLMENT_METRIC)) {
                em = (EnrollmentMetric) data.getSerializableExtra(
                        Consts.EXTRA_ENROLLMENT_METRIC);
            }

            // Get the finger location
            if (em != null) {
                String fingerLocation = em.getFingerLocation().toString();
                Log.d(TAG, "The fingerLocation, " + fingerLocation + ", is the String " +
                        "representation of the finger in the enum, EnumFinger.");

                // If you want a fingerprint template for enrollment, and can be
                // matched using Onyx, get it in the following manner
                FingerprintTemplate ft = em.getFingerprintTemplateArray()[0];
                // The fingerprint template contains the NFIQ score of the pre-processed image
                // that was used to create it
                Log.d(TAG, "FingerprintTemplate NFIQ Score = " + ft.getNfiqScore());

                // The EnrollmentMetric also contains the NFIQ score
                int nfiqScore = em.getHighestNFIQScore();
                Log.d(TAG, "NFIQ Score = " + nfiqScore);
            }
        }
    }
}
