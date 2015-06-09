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
import com.dft.onyx.enroll.util.Consts;
import com.dft.onyx.enroll.util.EnrollmentMetric;
import com.dft.onyx.fingerwizard.FingerWizardIntentHelper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


public class FingerWizardSample extends Activity {
    private static final String TAG = "FingerWizardSample";
    private static final int FINGER_WIZARD_REQUEST_CODE = 1234;
    private File mPreprocessedImageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create a file to hold the preprocessed image bitmap
        mPreprocessedImageFile = new File(Environment.getExternalStorageDirectory(),
                "FingerImage.png");

        // create a new FingerWizardIntentHelper object to get an intent and start it for a result
        // It needs the context, Onyx license key, a boolean as to whether to show the Onyx Guide
        // or not, an int defining the FLIP_TYPE to apply to the preprocessed bitmap, the
        // file to save the preprocessed bitmap image to, and the image's compression type
        startActivityForResult(FingerWizardIntentHelper.getFingerWizardIntent(this,
            getResources().getString(R.string.onyx_license), false, Consts.FLIP_HORIZONTAL,
                mPreprocessedImageFile, Bitmap.CompressFormat.JPEG),
                FINGER_WIZARD_REQUEST_CODE);
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
            preprocessedBitmapImageView.setImageBitmap(processedBitmap);

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
