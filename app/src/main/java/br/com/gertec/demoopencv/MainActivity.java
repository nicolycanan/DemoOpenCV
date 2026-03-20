package br.com.gertec.demoopencv;

import static android.Manifest.permission.MANAGE_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import org.opencv.android.OpenCVLoader;

import br.com.gertec.demoopencv.identificacao.FaceDetectActivity;
import br.com.gertec.demoopencv.registro.RegistroInternoActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Mainactivity";
    private static final int TYPE_DETECT_MATCH = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ativarFullScreen();

        //botao de abrir o cadastro interno
        findViewById(R.id.buttonCadastroInterno).setOnClickListener(v -> {
            Intent intent = new Intent(this, RegistroInternoActivity.class);
            startActivity(intent);
        });


        //botao de identificacao (cadastro interno)
        findViewById(R.id.buttonIdInterno).setOnClickListener(v -> {
            Intent intent = new Intent(this, FaceDetectActivity.class);
            intent.putExtra("typeCall", "facedetect");
            intent.putExtra("database", "intern");
            intent.putExtra("match", "none");
            startActivityForResult(intent, TYPE_DETECT_MATCH);
        });

        if (OpenCVLoader.initLocal()) {
            Log.i(TAG, "OpenCV loaded successfully");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }


        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, MANAGE_EXTERNAL_STORAGE}, 1);
        }
    }

    //ativar full screen
    private void ativarFullScreen() {
        // Ativa o modo full screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }

        View decorView = getWindow().getDecorView();
        WindowInsetsControllerCompat controller = ViewCompat.getWindowInsetsController(decorView);

        if (controller != null) {
            // Oculta as barras de sistema
            controller.hide(WindowInsetsCompat.Type.systemBars());
            // Permite que as barras apareçam com swipe
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

}