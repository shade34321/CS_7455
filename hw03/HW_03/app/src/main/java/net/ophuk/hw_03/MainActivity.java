package net.ophuk.hw_03;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import org.opencv.android.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements CvCameraViewListener {

    private CameraBridgeViewBase openCvCameraView;
    private CascadeClassifier cascadeClassifier;
    private Mat grayscaleImage;
    private int absoluteFaceSize;
    List age_coefficient;
    List gender_coefficient;

    static {
        if(!OpenCVLoader.initDebug()) {
            Log.d("Shade", "OpenCV not loaded!");
        } else {
            Log.d("Shade", "OpenCV loaded!");
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            Log.d("Shade", "Running onManagerConnected");
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.d("Shade", "LoaderCallbackInterface succeeded.");
                    initializeOpenCVDependencies();
                    break;
                default:
                    Log.d("Shade", "LoaderCallbackInterface did not succeeded.");
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    /**
     * Takes in an inputstream and reads in the CSV file to the resulting Array List.
     * Casts each "column" in the CSV into a double since that is what I need. This is not good for
     * general use. If you need it for general use just take out the for each loop.
     *
     * @param InputStream is
     * @return List
     */
    private List readCSV(InputStream is) {
        List resultList = new ArrayList();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        try{
            String csvLine;
            while ((csvLine = reader.readLine()) != null){
                String[] row = csvLine.split(",");
                for (String column: row) {
                    resultList.add(new Double(column));
                }
            }
        } catch (IOException ex) {
            Log.d("HW_03: readCSV", "Errored out while trying to read csv file");
            throw new RuntimeException("Error in reading CSV file: " + ex);
        }

        return resultList;
    }

    private void initializeOpenCVDependencies() {

        Log.d("Shade", "initializeOpenCVDependencies running.");

        try {
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            InputStream gender = getResources().openRawResource(R.raw.wiki5_gender);
            InputStream age = getResources().openRawResource(R.raw.wiki5_age);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            //Get the coefficients from the file.
            age_coefficient = readCSV(age);
            gender_coefficient = readCSV(gender);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            gender.close();
            age.close();
            os.close();

            // Load the cascade classifier
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading cascade", e);
        }

        // And we are ready to go
        openCvCameraView.enableView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("Shade", "onCreate running.");
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        openCvCameraView = new JavaCameraView(this, -1);
        setContentView(openCvCameraView);
        openCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.d("Shade", "onCameraaViewStarted running.");
        grayscaleImage = new Mat(height, width, CvType.CV_8UC4);

        // The faces will be a 20% of the height of the screen
        absoluteFaceSize = (int) (height * 0.2);
    }

    @Override
    public void onCameraViewStopped() {
        Log.d("Shade", "onCameraViewStopped running.");
    }

    /**
     * Takes in the Rect face and mat aInputFrame and guess the age and gender of the face matrix.
     * Returns a list of doubles where the first element is the gender and the second element is the
     * age.
     * @param Rect face
     * @param Mat aInputFrame
     * @return List<double>
     *
     * TODO: Take out the parameter aInputFrame. Should be double without needing this.
     */
    private List guessGenderAndAge(Rect face, Mat aInputFrame) {
        List results = new ArrayList();

        Mat temp = aInputFrame.submat(face);
        Mat greyFace = new Mat(face.width, face.height, CvType.CV_8U);
        Mat greyFaceResize = new Mat(10, 10, CvType.CV_8U);
        Imgproc.cvtColor(temp, greyFace, Imgproc.COLOR_RGB2GRAY);
        Imgproc.resize(greyFace, greyFaceResize, new Size(10,10));

        double gender_y = ((Double)gender_coefficient.get(0));
        double age_y = ((Double)age_coefficient.get(0));
        int j = 1;

        for (int m = 0; m < 10; m++){
            for (int n = 0; n < 10; n++){
                double[] pixel = greyFaceResize.get(m, n);
                age_y += pixel[0] * ((Double)age_coefficient.get(j));
                gender_y += pixel[0] * ((Double)gender_coefficient.get(j));
                j++;
            }
        }

        results.add(gender_y);
        results.add(Math.round(age_y * 100)/100);
        return results;
    }

    @Override
    public Mat onCameraFrame(Mat aInputFrame) {
        Log.d("Shade", "onCameraFrame running.");
        // Create a grayscale image
        Imgproc.cvtColor(aInputFrame, grayscaleImage, Imgproc.COLOR_RGBA2RGB);

        MatOfRect faces = new MatOfRect();

        // Use the classifier to detect faces
        if (cascadeClassifier != null) {
            Log.d("Shade", "cascadeClassifier is not null");
            cascadeClassifier.detectMultiScale(grayscaleImage, faces, 1.1, 2, 2,
                    new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }

        // If there are any faces found, draw a rectangle around it
        Rect[] facesArray = faces.toArray();
        Log.d("Shade", "facesArray.length: " + facesArray.length);
        for (int i = 0; i < facesArray.length; i++) {
            Rect faceRect = new Rect(facesArray[i].tl(), facesArray[i].br());

            List guess = guessGenderAndAge(faceRect, aInputFrame);

            String gender;
            if (((double)guess.get(0)) > .5) {
                gender = "Male";
            } else {
                gender = "Female";
            }

            Log.d("onCameraFrame", "Drawing rectangles around face.");
            Imgproc.putText(aInputFrame, "Gender: " + ((double)guess.get(0) > .5 ? "Male" : "Female"), new Point(facesArray[i].x - 25, facesArray[i].y - 25), 3, 1, new Scalar(255, 0,0,255), 1);
            Imgproc.putText(aInputFrame, "Age: " + guess.get(1), new Point(facesArray[i].x + 25, facesArray[i].y + 25), 3, 1, new Scalar(255, 0,0,255), 1);
            Imgproc.rectangle(aInputFrame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 3);

        }


        return aInputFrame;
    }

    @Override
    public void onResume() {
        Log.d("Shade", "onResume running.");
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
    }
}
