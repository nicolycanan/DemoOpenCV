package br.com.gertec.demoopencv.registro;

import static android.Manifest.permission.MANAGE_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import static br.com.gertec.demoopencv.Utils.matToJson;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;


import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.Mat;

import java.io.IOException;

import br.com.gertec.demoopencv.R;
import br.com.gertec.demoopencv.database.UserDB;
import br.com.gertec.demoopencv.database.UserInfo;
import br.com.gertec.demoopencv.database.UsersAdapter;
import br.com.gertec.demoopencv.identificacao.FaceDetectActivity;
import br.com.gertec.demoopencv.identificacao.FaceDetectRecognizer;

public class RegistroInternoActivity extends AppCompatActivity {

    private static final String TAG = RegistroInternoActivity.class.getSimpleName();
    private static final int ADD_USER_PICTURE = 1;
    private static final int ADD_USER_CAMERA = 2;

    private UserDB userDb;
    private UsersAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_registro_interno);

        // Ocultar a barra de notificações
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);


        userDb = new UserDB(this);
        userDb.loadUsers();

        adapter = new UsersAdapter(this, UserDB.userInfos);
        ListView listView = findViewById(R.id.userList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((adapterView, view, i, l) -> {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(RegistroInternoActivity.this);
            alertDialog.setTitle(getString(R.string.delete_user));

            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.activity_dialog, null);
            alertDialog.setView(dialogView);

            // Access the views in the custom dialog layout
            ImageView imageView = dialogView.findViewById(R.id.dialogFaceView);
            TextView textView = dialogView.findViewById(R.id.dialogTextView);

            // Customize the views
            // Get the data item for this position
            imageView.setImageBitmap(UserDB.userInfos.get(i).faceImage);
            textView.setText(UserDB.userInfos.get(i).userName);

            // Set positive button and its click listener
            alertDialog.setPositiveButton(getString(R.string.delete), (dialogView1, which) -> {
                // Handle positive button click, if needed
                userDb.deleteUser(UserDB.userInfos.get(i).userName);
                UserDB.userInfos.remove(i);

                adapter.notifyDataSetChanged();
                dialogView1.dismiss();
            });

            // Set negative button and its click listener
            alertDialog.setNegativeButton(getString(R.string.delete_all), (dialogView12, which) -> {
                // Handle negative button click, if needed
                userDb.deleteAllUser();
                UserDB.userInfos.clear();

                adapter.notifyDataSetChanged();
                dialogView12.dismiss();
            });

            // Create and show the AlertDialog
            AlertDialog alert = alertDialog.create();
            alert.show();
        });

        findViewById(R.id.buttonAdd).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_PICK);
            startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), ADD_USER_PICTURE);
        });

        findViewById(R.id.buttonCamera).setOnClickListener(v -> {
            Intent intent = new Intent(this, FaceDetectActivity.class);
            intent.putExtra("typeCall", "add");
            startActivityForResult(intent, ADD_USER_CAMERA);
        });

        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, MANAGE_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_USER_PICTURE && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            Bitmap faceImage;
            try {
                faceImage = getResizedBitmap(MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri),
                        360,
                        360);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            if (OpenCVLoader.initLocal()) {
                Log.i(TAG, "OpenCV loaded successfully");
            } else {
                Log.e(TAG, "OpenCV initialization failed!");
                (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
                return;
            }

            FaceDetectRecognizer faceDetectRecognizer = new FaceDetectRecognizer(this);
            if (!faceDetectRecognizer.loadFaceDetector()) {
                Toast.makeText(this, "Erro ao executar o detect!", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Erro ao executar o detect!");
                return;
            }

            if (!faceDetectRecognizer.loadFaceRecognizer()) {
                Log.e(TAG, "Erro ao executar o recognizer!");
                Toast.makeText(this, "Erro ao executar o recognizer!", Toast.LENGTH_LONG).show();
                return;
            }
            Mat facem = new Mat();
            try {
                Utils.bitmapToMat(faceImage, facem);
            } catch (CvException e) {
                Toast.makeText(this, "Imagem inválida!", Toast.LENGTH_LONG).show();
                return;
            }

            Mat temp2 = new Mat();
            try {
                temp2 = FaceDetectRecognizer.GeraTemplate(facem);
            } catch (CvException e) {
                Toast.makeText(this, "Erro ao gerar template!", Toast.LENGTH_LONG).show();
                return;
            } catch (Exception e) {
                Toast.makeText(this, "Erro ao gerar template!", Toast.LENGTH_LONG).show();
                return;
            }

            String featData = matToJson(temp2);

            String userName = String.format("User%03d", userDb.getLastUserId() + 1);

            View inputView = LayoutInflater.from(this).inflate(R.layout.dialog_input_view, null, false);
            EditText editText = inputView.findViewById(R.id.et_user_name);
            ImageView ivHead = inputView.findViewById(R.id.iv_head);
            ivHead.setImageBitmap(faceImage);
            editText.setText(userName);
            AlertDialog confirmUpdateDialog = new AlertDialog.Builder(this)
                    .setView(inputView)
                    .setPositiveButton("OK", null)
                    .setNegativeButton("Cancel", null)
                    .create();
            confirmUpdateDialog.show();

            confirmUpdateDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String s = editText.getText().toString();
                if (TextUtils.isEmpty(s)) {
                    editText.setError(getString(R.string.name_should_not_be_empty));
                    return;
                }

                boolean exists = false;
                for (UserInfo user : UserDB.userInfos) {
                    if (TextUtils.equals(user.userName, s)) {
                        exists = true;
                        break;
                    }
                }

                if (exists) {
                    editText.setError(getString(R.string.duplicated_name));
                    return;
                }

                int userId = userDb.insertUser(s, faceImage, featData);
                UserInfo face = new UserInfo(userId, s, faceImage, featData);
                UserDB.userInfos.add(face);

                confirmUpdateDialog.dismiss();

                adapter.notifyDataSetChanged();
                Toast.makeText(this, getString(R.string.register_successed), Toast.LENGTH_SHORT).show();
            });

        }
        if (requestCode == ADD_USER_CAMERA && resultCode == RESULT_OK) {
            try {
                //Bitmap faceImage =  (Bitmap) data.getParcelableExtra("identified_face");
                Bitmap faceImage = BitmapFactory.decodeFile(getExternalFilesDir("TestesESD").toString() + "/" + "rgbNoRect1.jpg");
                String featData = data.getStringExtra("featData");

                String userName = String.format("User%03d", userDb.getLastUserId() + 1);

                View inputView = LayoutInflater.from(this).inflate(R.layout.dialog_input_view, null, false);
                EditText editText = inputView.findViewById(R.id.et_user_name);
                ImageView ivHead = inputView.findViewById(R.id.iv_head);
                ivHead.setImageBitmap(faceImage);
                editText.setText(userName);
                AlertDialog confirmUpdateDialog = new AlertDialog.Builder(this)
                        .setView(inputView)
                        .setPositiveButton("OK", null)
                        .setNegativeButton("Cancel", null)
                        .create();
                confirmUpdateDialog.show();
                confirmUpdateDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String s = editText.getText().toString();
                    if (TextUtils.isEmpty(s)) {
                        editText.setError(getString(R.string.name_should_not_be_empty));
                        return;
                    }

                    boolean exists = false;
                    for (UserInfo user : UserDB.userInfos) {
                        if (TextUtils.equals(user.userName, s)) {
                            exists = true;
                            break;
                        }
                    }

                    if (exists) {
                        editText.setError(getString(R.string.duplicated_name));
                        return;
                    }

                    int userId = userDb.insertUser(s, faceImage, featData);
                    UserInfo face = new UserInfo(userId, s, faceImage, featData);
                    UserDB.userInfos.add(face);

                    confirmUpdateDialog.dismiss();

                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, getString(R.string.register_successed), Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                //handle exception
                e.printStackTrace();
            }
        }
    }
}
