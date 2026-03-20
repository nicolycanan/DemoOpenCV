package br.com.gertec.demoopencv.identificacao;

import android.content.Context;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.FaceDetectorYN;
import org.opencv.objdetect.FaceRecognizerSF;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import br.com.gertec.demoopencv.R;

/**
 * the java demo of FaceDetectorYN and FaceRecognizerSF
 */
public class FaceDetectRecognizer {

    private static final String    TAG  = "FaceDetectRecognizer";
    private static double cosine_similar_threshold = 0.363;

    private static double l2norm_similar_threshold = 1.128;

    public static FaceDetectorYN faceDetector = null;

    private static MatOfByte              mModelBuffer;
    private static MatOfByte              mConfigBuffer;

    private static FaceRecognizerSF faceRecognizer = null;

    static Context mContext;

    public Size                   mInputSize = null;

    public FaceDetectRecognizer(Context context){
        mContext = context;
        loadFaceDetector();
        loadFaceRecognizer();
    }

    public boolean loadFaceDetector() {
        Boolean Retorno = false;

        if (faceDetector != null) {
            return true;
        }

        byte[] buffer;
        try {
            InputStream is = mContext.getResources().openRawResource(R.raw.face_detection_yunet_2023mar);

            int size = is.available();
            buffer = new byte[size];
            int bytesRead = is.read(buffer);
            is.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to ONNX model from resources! Exception thrown: " + e);
            return Retorno;
        }

        mModelBuffer = new MatOfByte(buffer);
        mConfigBuffer = new MatOfByte();

        faceDetector = FaceDetectorYN.create("onnx", mModelBuffer, mConfigBuffer, new Size(320, 320));
        if (faceDetector == null) {
            return Retorno;
        } else{
            Log.i(TAG, "FaceDetectorYN initialized successfully!");
            Retorno = true;
        }

        return Retorno;
    }

    public boolean loadFaceRecognizer() {
        boolean Retorno = false;
        if (faceRecognizer != null) {
            return true;
        }

        Log.i(TAG, "StartFaceRecognizer...");
        String onnxFileName = mContext.getExternalFilesDir("Facial").toString() + "/face_recognition_sface_2021dec.onnx";
        File file = new File(onnxFileName);
        if(!file.exists()){
            InputStream in = mContext.getResources().openRawResource(R.raw.face_recognition_sface_2021dec);
            try (OutputStream out = new FileOutputStream(file)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } catch (FileNotFoundException e) {
                Log.i(TAG, "Ops: " + e.getMessage());
                return Retorno;
            } catch (IOException e) {
                Log.i(TAG, "Ops: " + e.getMessage());
                return Retorno;
            }
        }
        faceRecognizer = FaceRecognizerSF.create(onnxFileName, "");
        if(faceRecognizer!=null){
            Retorno = true;
        }
        return Retorno;
    }

    public static Mat GeraTemplate(Mat imgB) {
        Mat imgA = new Mat();
        Imgproc.cvtColor(imgB, imgA, Imgproc.COLOR_RGBA2BGR);
        // 2.detect face from given image
        Mat faceA = new Mat();
        faceDetector.setInputSize(imgA.size());
        faceDetector.detect(imgA, faceA);

        // 4.Aligning image to put face on the standard position
        Mat alignFaceA = new Mat();
        faceRecognizer.alignCrop(imgA, faceA.row(0), alignFaceA);

        // 5.Extracting face feature from aligned image
        Mat featureA = new Mat();
        faceRecognizer.feature(alignFaceA, featureA);
        featureA = featureA.clone();
        return featureA;
    }

    public static Mat GeraTemplateFromFace(Mat imgA, Mat faceA) {
        Mat alignFaceA = new Mat();
        faceRecognizer.alignCrop(imgA, faceA.row(0), alignFaceA);

        // 5.Extracting face feature from aligned image
        Mat featureA = new Mat();
        faceRecognizer.feature(alignFaceA, featureA);
        featureA = featureA.clone();
        return featureA;
    }

    public static boolean MatchFeatures(Mat featureA, Mat featureB, double[] matchs){
        double match1 = faceRecognizer.match(featureA, featureB, FaceRecognizerSF.FR_COSINE);
        // get l2norm similar
        double match2 = faceRecognizer.match(featureA, featureB, FaceRecognizerSF.FR_NORM_L2);
        matchs[0] = match1;
        matchs[1] = match2;
        if (match1 >= cosine_similar_threshold && match2 <= l2norm_similar_threshold) {
            return true;
        } else {
            return false;
        }
    }
}