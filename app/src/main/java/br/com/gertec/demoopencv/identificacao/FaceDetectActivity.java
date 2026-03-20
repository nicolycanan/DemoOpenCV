package br.com.gertec.demoopencv.identificacao;

import static android.view.View.SYSTEM_UI_FLAG_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE;

import static br.com.gertec.demoopencv.Utils.matToJson;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsControllerCompat;


import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.com.gertec.demoopencv.R;
import br.com.gertec.demoopencv.database.UserDB;
import br.com.gertec.demoopencv.database.UserInfo;

public class FaceDetectActivity extends CameraActivity implements CvCameraViewListener2 {

    private static final String TAG = FaceDetectActivity.class.getSimpleName();

    private static final Scalar BOX_COLOR = new Scalar(0, 255, 0);

    private Mat mRgba;
    private Mat mBgr;
    private Mat mBgrScaled;
    private float mScale = 1.f;

    private Mat mFaces;

    int saved = 1;

    int doMatch = 0;

    boolean encontrouImage = false;

    FaceDetectRecognizer faceDetectRecognizer;

    private CameraBridgeViewBase mOpenCvCameraView;

    private String typeCall;
    private String tipoBD;
    private String tipoMatch;

    private UserDB userDb;

    private static boolean firstTime = true;

    private static final int TIMEOUT_KEYEVENT_SECONDS = 3;

    LocalDateTime dataHoraUltimoKeyEvent;

    private String keyEventInputed = "";

    TextView tvStatus;

    public boolean getAmbiente = false;


    public FaceDetectActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);

        if (Build.MODEL.equals("SK210")) {
            setFlags();
        }
        setContentView(R.layout.face_detect_surface_view);

        // Ocultar a barra de notificações
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);


        typeCall = getIntent().getStringExtra("typeCall");
        tipoBD = getIntent().getStringExtra("database");
        tipoMatch = getIntent().getStringExtra("match");
        Log.i(TAG, "tipo chamada -> " + typeCall);
        Log.i(TAG, "tipo bd -> " + tipoBD);

        tvStatus = findViewById(R.id.textViewStatus);
        runOnUiThread(() -> tvStatus.setText("Iniciando..."));

        if (typeCall.equals("facedetect")) {
            userDb = new UserDB(this);
            if (tipoBD.equals("intern")) {
                userDb.loadUsers();
            } else {
                StringBuffer msgRetorno = new StringBuffer();
                userDb.getUsersSqlServer(msgRetorno);
            }
        }

        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG).show();
            return;
        }

        faceDetectRecognizer = new FaceDetectRecognizer(this);
        if (!faceDetectRecognizer.loadFaceDetector()) {
            Log.e(TAG, "Erro ao executar o detect!");
        }

        if (!faceDetectRecognizer.loadFaceRecognizer()) {
            Log.e(TAG, "Erro ao executar o recognizer!");
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        dataHoraUltimoKeyEvent = LocalDateTime.now();

        mOpenCvCameraView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Clicou para salvar!!!");
                saved = 0;
            }
        });

        mOpenCvCameraView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                doMatch = 1;
                return true;
            }
        });


        runOnUiThread(() -> tvStatus.setText("Iniciado! Detectando faces..."));

    }

    private void setFlags() {
        getWindow().getDecorView().setSystemUiVisibility(SYSTEM_UI_FLAG_IMMERSIVE_STICKY | SYSTEM_UI_FLAG_FULLSCREEN | SYSTEM_UI_FLAG_HIDE_NAVIGATION | SYSTEM_UI_FLAG_LAYOUT_STABLE | SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mOpenCvCameraView != null) mOpenCvCameraView.enableView();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        char keyPress = (char) event.getUnicodeChar();
        Log.i(TAG, "key pressed -> " + keyPress);
        LocalDateTime dataHoraAgora = LocalDateTime.now();
        long sec = Duration.between(dataHoraUltimoKeyEvent, dataHoraAgora).toMillis() / 1000;

        if (sec > TIMEOUT_KEYEVENT_SECONDS) {
            Log.i(TAG, "KeyEvent - passaram " + sec + " segundos. Limpando...");
            keyEventInputed = "";
        }
        dataHoraUltimoKeyEvent = LocalDateTime.now();
        keyEventInputed += keyPress;
        Log.i(TAG, "keyEventInputed -> " + keyEventInputed);

        if (keyEventInputed.length() >= 10 && tipoMatch.equals("TesteEpi")) {
            keyEventInputed = "";
//            IniciaTestes(UserDB.userInfos.get(0).mat, UserDB.userInfos.get(0));
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        mBgr = new Mat();
        mBgrScaled = new Mat();
        mFaces = new Mat();
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mBgr.release();
        mBgrScaled.release();
        mFaces.release();
    }

    public void visualize(Mat rgba, Mat faces, double[] match) {

        int thickness = 2;
        float[] faceData = new float[faces.cols() * faces.channels()];

        for (int i = 0; i < faces.rows(); i++) {
            faces.get(i, 0, faceData);

            Log.i(TAG, "Detected face (" + faceData[0] + ", " + faceData[1] + ", " + faceData[2] + ", " + faceData[3] + ")");

            // Draw bounding box
            //if(encontrouImage) {
            Imgproc.rectangle(rgba, new Rect(Math.round(mScale * faceData[0]), Math.round(mScale * faceData[1]), Math.round(mScale * faceData[2]), Math.round(mScale * faceData[3])), BOX_COLOR, thickness);
            /*}else{
                Imgproc.rectangle(rgba, new Rect(Math.round(mScale*faceData[0]), Math.round(mScale*faceData[1]),
                                Math.round(mScale*faceData[2]), Math.round(mScale*faceData[3])),
                        BOX_COLOR_RED, thickness);
            }*/
            // Draw landmarks
            /*Imgproc.circle(rgba, new Point(Math.round(mScale*faceData[4]), Math.round(mScale*faceData[5])),
                    2, RIGHT_EYE_COLOR, thickness);
            Imgproc.circle(rgba, new Point(Math.round(mScale*faceData[6]), Math.round(mScale*faceData[7])),
                    2, LEFT_EYE_COLOR, thickness);
            Imgproc.circle(rgba, new Point(Math.round(mScale*faceData[8]), Math.round(mScale*faceData[9])),
                    2, NOSE_TIP_COLOR, thickness);
            Imgproc.circle(rgba, new Point(Math.round(mScale*faceData[10]), Math.round(mScale*faceData[11])),
                    2, MOUTH_RIGHT_COLOR, thickness);
            Imgproc.circle(rgba, new Point(Math.round(mScale*faceData[12]), Math.round(mScale*faceData[13])),
                    2, MOUTH_LEFT_COLOR, thickness);*/
        }
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if (faceDetectRecognizer.mInputSize == null) {
            faceDetectRecognizer.mInputSize = new Size(Math.round(mRgba.cols() / mScale), Math.round(mRgba.rows() / mScale));
            Log.i(TAG, "cols -> " + Math.round(mRgba.cols() / mScale) + "  rows -> " + Math.round(mRgba.rows() / mScale));
            faceDetectRecognizer.faceDetector.setInputSize(faceDetectRecognizer.mInputSize);
        }

        Imgproc.cvtColor(mRgba, mBgr, Imgproc.COLOR_RGBA2BGR);
        Imgproc.resize(mBgr, mBgrScaled, faceDetectRecognizer.mInputSize);

        if (faceDetectRecognizer.faceDetector != null) {
            int status = faceDetectRecognizer.faceDetector.detect(mBgrScaled, mFaces);
            double match[] = new double[2];
            match[0] = 0;
            match[1] = 0;

            if (mFaces.rows() == 0) {

                runOnUiThread(() -> tvStatus.setText("Face não detectada!"));
                if (!getAmbiente || saved == 0) {
                    getAmbiente = true;
                    saved = 1;
                    Imgcodecs.imwrite(getExternalFilesDir("TestesESD").toString() + "/" + "Ambiente.jpg", mRgba);
                }
            } else if (mFaces.rows() > 1) {
                runOnUiThread(() -> tvStatus.setText("Múltiplas faces detectadas!"));


                Log.i(TAG, "Face detectada! rows:" + mFaces.rows());
                visualize(mRgba, mFaces, match);
            } else if (mFaces.rows() == 1) {
                visualize(mRgba, mFaces, match);
                if (typeCall.equals("facedetect")) {
                    MatchRecognizer(mBgrScaled, mFaces, match);
                } else if (typeCall.equals("add")) {

                    runOnUiThread(() -> tvStatus.setText("Face detectada! Clique na tela para salvar a imagem."));

                    if (saved == 0) {
                        Intent intent = new Intent();
                        Imgcodecs.imwrite(getExternalFilesDir("TestesESD").toString() + "/" + "rgbNoRect1.jpg", mBgrScaled);
                        Mat temp2 = new Mat();
                        String featData = "";
                        Boolean tudoOK = false;
                        try {
                            //temp2 = FaceDetectRecognizer.GeraTemplate(mBgrScaled);
                            temp2 = FaceDetectRecognizer.GeraTemplateFromFace(mBgrScaled, mFaces);
                            featData = matToJson(temp2);
                            intent.putExtra("featData", featData);
                            tudoOK = true;
                            if (temp2 == null || featData == null) {
                                tudoOK = false;
                            }
                            if (tudoOK) {
                                setResult(Activity.RESULT_OK, intent);
                                finish();
                            }
                        } catch (CvException e) {
                            Log.e(TAG, "Erro: " + e.getMessage());

                            runOnUiThread(() -> tvStatus.setText("Erro ao gerar o template da imagem!"));

                        }
                    }
                }
            }
        }
        return mRgba;
    }

    public void DiffImages(Mat img1, Mat img2) {
        Imgproc.cvtColor(img2, img2, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.cvtColor(img1, img1, Imgproc.COLOR_RGBA2GRAY);

        img1.convertTo(img1, CvType.CV_32F);
        img2.convertTo(img2, CvType.CV_32F);
        //Log.d("ImageComparator", "img1:"+img1.rows()+"x"+img1.cols()+" img2:"+img2.rows()+"x"+img2.cols());
        Mat hist1 = new Mat();
        Mat hist2 = new Mat();
        MatOfInt histSize = new MatOfInt(180);
        MatOfInt channels = new MatOfInt(0);
        ArrayList<Mat> bgr_planes1 = new ArrayList<Mat>();
        ArrayList<Mat> bgr_planes2 = new ArrayList<Mat>();
        Core.split(img1, bgr_planes1);
        Core.split(img2, bgr_planes2);
        MatOfFloat histRanges = new MatOfFloat(0f, 180f);
        boolean accumulate = false;
        Imgproc.calcHist(bgr_planes1, channels, new Mat(), hist1, histSize, histRanges, accumulate);
        Core.normalize(hist1, hist1, 0, hist1.rows(), Core.NORM_MINMAX, -1, new Mat());
        Imgproc.calcHist(bgr_planes2, channels, new Mat(), hist2, histSize, histRanges, accumulate);
        Core.normalize(hist2, hist2, 0, hist2.rows(), Core.NORM_MINMAX, -1, new Mat());
        img1.convertTo(img1, CvType.CV_32F);
        img2.convertTo(img2, CvType.CV_32F);
        hist1.convertTo(hist1, CvType.CV_32F);
        hist2.convertTo(hist2, CvType.CV_32F);

        double compare = Imgproc.compareHist(hist1, hist2, Imgproc.CV_COMP_CHISQR);
        Log.i(TAG, "compare: " + compare);
        if (compare > 0 && compare < 1500) {
            Log.i(TAG, "Images may be possible duplicates, verifying");
        } else if (compare == 0) Log.i(TAG, "Images are exact duplicates");
        else Log.i(TAG, "Images are not duplicates");

    }

    public void MatchRecognizer(Mat templ1, Mat face, double[] matchs) {
        Log.i(TAG, "Iniciando MatchRecognizer...");
        LocalDateTime dataHoraInicial;
        dataHoraInicial = LocalDateTime.now();

        int i = 0;
        boolean matched = false;
        boolean matchedInAll = false;
        Mat templ = new Mat();
        try {
            templ = FaceDetectRecognizer.GeraTemplateFromFace(templ1, face);
        } catch (CvException e) {
            Log.e(TAG, "Erro ao gerar template do frame: " + e);
            return;
        }

        for (UserInfo user : UserDB.userInfos) {
            i++;
//            tvStatus.setText("Matching[" + i + "/" + UserDB.userInfos.size() + "]...");


            if (user.matFeature == null) {
                Log.i(TAG, "Gerando feature de " + i + "...");
                user.matFeature = new Mat();
                user.matFeature = FaceDetectRecognizer.GeraTemplate(user.mat);
            }

            matched = FaceDetectRecognizer.MatchFeatures(user.matFeature, templ, matchs);
            Log.i(TAG, "match1: " + String.format("%.3f", matchs[0]) + " match2: " + String.format("%.3f", matchs[1]));

            if (matched) {

                matchedInAll = true;
                Log.i(TAG, "Imagem[" + (i - 1) + "] -> bate como o banco");
                if (tipoMatch.equals("TesteEpi")) {
                    runOnUiThread(() -> tvStatus.setBackgroundColor(Color.GREEN));
                    return;
                }
            } else {
                if (tipoMatch.equals("TesteEpi")) {
                    runOnUiThread(() -> tvStatus.setBackgroundColor(Color.RED));
                    runOnUiThread(() -> tvStatus.setText("Rosto não cadatrado "));

                }
                Log.i(TAG, "Imagem[" + (i - 1) + "] -> IMAGE NOT MATCHED..");
            }
        }
        if (tipoMatch.equals("none")) {
            if (matchedInAll) {
                runOnUiThread(() -> tvStatus.setBackgroundColor(Color.GREEN));

            } else {
                runOnUiThread(() -> tvStatus.setBackgroundColor(Color.RED));

            }
            double msec = Duration.between(dataHoraInicial, LocalDateTime.now()).toMillis();
            Log.i(TAG, "Matching para " + UserDB.userInfos.size() + "imagens: " + String.format("%.3f", msec / 1000) + " segundos");

            runOnUiThread(() -> tvStatus.setText("Rosto cadastrado!"));
//            runOnUiThread(() -> tvStatus.setText("Matching para " + String.format("%04d", UserDB.userInfos.size()) + " imagens: " + String.format("%.3f", msec / 1000) + " segundos"));

            ;
        }

    }


}
