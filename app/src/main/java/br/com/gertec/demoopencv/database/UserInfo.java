package br.com.gertec.demoopencv.database;



import static br.com.gertec.demoopencv.Utils.matFromJson;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Mat;


public class UserInfo {

    public String userName;
    public Bitmap faceImage;

    public Mat mat;
    public int userId;

    public String featData;

    public Mat matFeature;

    public UserInfo() {

    }

    public UserInfo(int userId, String userName, Bitmap faceImage, String featData) {
        this.userId = userId;
        this.userName = userName;
        this.faceImage = faceImage;
        this.featData = featData;
        this.matFeature = matFromJson(featData);
        this.mat = new Mat();
        Utils.bitmapToMat(faceImage, this.mat);
    }
}
