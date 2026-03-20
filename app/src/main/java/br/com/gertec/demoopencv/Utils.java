package br.com.gertec.demoopencv;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.core.Mat;

import java.nio.ByteBuffer;

public class Utils {

    public static Rect getBestRect(int width, int height, Rect srcRect) {
        if (srcRect == null) {
            return null;
        }
        Rect rect = new Rect(srcRect);

        int maxOverFlow = Math.max(-rect.left, Math.max(-rect.top, Math.max(rect.right - width, rect.bottom - height)));
        if (maxOverFlow >= 0) {
            rect.inset(maxOverFlow, maxOverFlow);
            return rect;
        }

        int padding = rect.height() / 2;

        if (!(rect.left - padding > 0 && rect.right + padding < width && rect.top - padding > 0 && rect.bottom + padding < height)) {
            padding = Math.min(Math.min(Math.min(rect.left, width - rect.right), height - rect.bottom), rect.top);
        }
        rect.inset(-padding, -padding);
        return rect;
    }

    public static Bitmap crop(final Bitmap src, final int srcX, int srcY, int srcCroppedW, int srcCroppedH, int newWidth, int newHeight) {
        final int srcWidth = src.getWidth();
        final int srcHeight = src.getHeight();
        float scaleWidth = ((float) newWidth) / srcCroppedW;
        float scaleHeight = ((float) newHeight) / srcCroppedH;

        final Matrix m = new Matrix();

        m.setScale(1.0f, 1.0f);
        m.postScale(scaleWidth, scaleHeight);
        final Bitmap cropped = Bitmap.createBitmap(src, srcX, srcY, srcCroppedW, srcCroppedH, m,
                true /* filter */);
        return cropped;
    }

    public static String matToJson(Mat mat){
        JSONObject obj = new JSONObject();

        if(mat.isContinuous()){
            int cols = mat.cols();
            int rows = mat.rows();
            int elemSize = (int) mat.elemSize();

            float[] fdata = new float[cols * rows * elemSize];
            mat.get(0, 0, fdata);

            byte[] data = FloatToByte(fdata);

            // We cannot set binary data to a json object, so:
            // Encoding data byte array to Base64.
            String dataString = new String(Base64.encode(data, Base64.DEFAULT));

            try {
                obj.put("rows", mat.rows());
                obj.put("cols", mat.cols());
                obj.put("type", mat.type());
                obj.put("data", dataString);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            String json = obj.toString();
            return json;
        } else {
            Log.e("Utils", "Mat not continuous.");
        }
        return "{}";
    }

    public static Mat matFromJson(String json){
        JSONObject JsonObject = null;
        int rows;
        int cols;
        int type;
        String dataString;
        byte[] data;
        float[] fdata;

        try {
            JsonObject = new JSONObject(json);
            rows = JsonObject.getInt("rows");
            cols = JsonObject.getInt("cols");
            type = JsonObject.getInt("type");
            dataString = JsonObject.getString("data");
            data = Base64.decode(dataString.getBytes(), Base64.DEFAULT);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        fdata = ByteToFloat(data);
        Mat mat = new Mat(rows, cols, type);
        mat.put(0, 0, fdata);

        return mat;
    }

    public static byte[] FloatToByte(float[] input) {
        byte[] ret = new byte[input.length*4];
        for (int x = 0; x < input.length; x++) {
            ByteBuffer.wrap(ret, x*4, 4).putFloat(input[x]);
        }
        return ret;
    }

    public static float[] ByteToFloat(byte[] input) {
        float[] ret = new float[input.length/4];
        for (int x = 0; x < input.length; x+=4) {
            ret[x/4] = ByteBuffer.wrap(input, x, 4).getFloat();
        }
        return ret;
    }
}
