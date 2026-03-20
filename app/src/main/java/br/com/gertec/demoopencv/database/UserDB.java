package br.com.gertec.demoopencv.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.StrictMode;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class UserDB extends SQLiteOpenHelper {

    private static final String TAG = "UserDB";
    public static final String DATABASE_NAME = "users2.db";
    public static final String CONTACTS_TABLE_NAME = "users";
    public static final String CONTACTS_COLUMN_ID = "userId";
    public static final String CONTACTS_COLUMN_NAME = "userName";
    public static final String CONTACTS_COLUMN_FACE = "faceImage";
    public static final String CONTACTS_COLUMN_FEATURE = "featData";

    public static ArrayList<UserInfo> userInfos = new ArrayList<UserInfo>();

//    public static ArrayList<TesteEpiInfo> testeEpiInfos = new ArrayList<TesteEpiInfo>();

    public static final String SQL_SERVER_URL = "jdbc:jtds:sqlserver://192.168.0.251;";

    //public static final String SQL_SERVER_URL = "jdbc:jtds:sqlserver://172.18.4.52;";

    public static final String SQL_SERVER_USER = "sa";

    public static final String SQL_SERVER_PWD = "teste123";


    private Context appCtx;

    public UserDB(Context context) {
        super(context, DATABASE_NAME , null, 1);
        appCtx = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL(
                "create table users " +
                        "(userId integer primary key autoincrement,  userName text, faceImage blob, featData blob)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }

    //adicionar cadastro interno
    public int insertUser (String userName, Bitmap faceImage, String featData) {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        faceImage.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] face = byteArrayOutputStream.toByteArray();

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("userName", userName);
        contentValues.put("faceImage", face);
        contentValues.put("featData", featData);
        db.insert("users", null, contentValues);

        Cursor res =  db.rawQuery( "select last_insert_rowid() from users", null );
        res.moveToFirst();

        int userId = 0;
        while(res.isAfterLast() == false){
            userId = res.getInt(0);
            res.moveToNext();
        }
        return userId;
    }

//    public void insertUserToSqlServer (int userID, Bitmap faceImage, String featData) {
//
//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);
//
//        String url = "jdbc:jtds:sqlserver://192.168.0.251;";
//        try {
//            Class.forName("net.sourceforge.jtds.jdbc.Driver");
//            String username = "sa";
//            String password = "teste123";
//
//            Connection DbConn = DriverManager.getConnection(url, username, password);
//
//            Log.i(TAG, "open");
//
//            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//            faceImage.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
//
//            //teste - pegar direto do arquivo
//            //Bitmap tmp = BitmapFactory.decodeFile("/sdcard/Download/rgbNoRect1.jpg");
//            //tmp.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
//            //Mat img = Imgcodecs.imread("/sdcard/Download/rgbNoRect1.jpg");
//
//            byte[] face = byteArrayOutputStream.toByteArray();
//            byte[] b64Face = Base64.encode(face, Base64.DEFAULT);
//            byte[] b64Feat = Base64.encode(featData.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
//
//            String query = "INSERT INTO fotos2 (user_id, face_image, feat_data) ";
//            query += "VALUES (" + userID + ", ?, ?)";    // ? are placeholders
//            PreparedStatement statement = DbConn.prepareStatement(query);
//            statement.setBytes(1, b64Face);
//            statement.setBytes(2, b64Feat);
//            statement.executeUpdate();
//
//            DbConn.close();
//            Log.i(TAG, "Registro adicionado no SQLServer!");
//
//        }catch(ClassNotFoundException cnf){
//            Log.e(TAG, "Classe JDBC Driver nao encontrada.");
//        }catch(SQLException sql){
//            Log.e(TAG, "Erro ao se conectar com o banco de dados: " + sql.getMessage());
//        }catch (Exception e){
//            Log.e(TAG, "Error connection (for string " + url + ") : " + e.getMessage());
//        }
//    }

    public int insertTesteEPISqlServer (int userId, boolean testeOK) {

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        String url = "jdbc:jtds:sqlserver://192.168.0.251;";
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            Connection DbConn = DriverManager.getConnection(SQL_SERVER_URL, SQL_SERVER_USER, SQL_SERVER_PWD);

            Log.i(TAG, "open");

            int teste = testeOK ? 1 : 0;

            Statement stmt = DbConn.createStatement();
            String query = "insert into testes_epi (id_colaborador, resultado) values ('" + String.valueOf(userId) + "','" + teste + "')";
            stmt.executeUpdate(query);

            DbConn.close();
            Log.i(TAG, "Registro adicionado no SQLServer!");

        }catch(ClassNotFoundException cnf){
            Log.e(TAG, "Classe JDBC Driver nao encontrada.");
        }catch(SQLException sql){
            Log.e(TAG, "Erro ao se conectar com o banco de dados: " + sql.getMessage());
        }catch (Exception e){
            Log.e(TAG, "Error connection (for string " + url + ") : " + e.getMessage());
        }

        return 1;
    }


    public int getLastUserId() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select max(userId) from users", null );
        res.moveToFirst();

        int userId = 0;
        while(res.isAfterLast() == false){
            userId = res.getInt(0);
            res.moveToNext();
        }
        return userId;
    }

    public Integer deleteUser (String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("users",
                "userName = ? ",
                new String[] { name });
    }

    //deletar um cadastro
    public void deleteUserSqlServer (String name) {
        userInfos.clear();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        String url = "jdbc:jtds:sqlserver://192.168.0.251;";
        try {

            // SET CONNECTIONSTRING
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            String username = "sa";
            String password = "teste123";
            Connection DbConn = DriverManager.getConnection(url, username, password);

            Log.i(TAG, "open");
            Statement stmt = DbConn.createStatement();
            stmt.executeUpdate(" delete from fotos2 where user_id =" + name);
            DbConn.close();
            Log.i(TAG, "Registro " + name + "deletado!");

        }catch(ClassNotFoundException cnf){
            Log.e(TAG, "Classe JDBC Driver nao encontrada.");
        }catch(SQLException sql){
            Log.e(TAG, "Erro ao se conectar com o banco de dados: " + sql.getMessage());
        }catch (Exception e){
            Log.e(TAG, "Error connection (for string " + url + ") : " + e.getMessage());
        }
    }

    //deletar todos os cadastros
    public Integer deleteAllUser () {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from "+ CONTACTS_TABLE_NAME);
        return 0;
    }

//    public void deleteAllUserSqlServer () {
//        userInfos.clear();
//
//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);
//
//        String url = "jdbc:jtds:sqlserver://192.168.0.251;";
//        try {
//
//            // SET CONNECTIONSTRING
//            Class.forName("net.sourceforge.jtds.jdbc.Driver");
//            String username = "sa";
//            String password = "teste123";
//            Connection DbConn = DriverManager.getConnection(url, username, password);
//
//            Log.i(TAG, "open");
//            Statement stmt = DbConn.createStatement();
//            stmt.executeUpdate(" delete from fotos2");
//            DbConn.close();
//            Log.i(TAG, "Registros deletados!");
//
//        }catch(ClassNotFoundException cnf){
//            Log.e(TAG, "Classe JDBC Driver nao encontrada.");
//        }catch(SQLException sql){
//            Log.e(TAG, "Erro ao se conectar com o banco de dados: " + sql.getMessage());
//        }catch (Exception e){
//            Log.e(TAG, "Error connection (for string " + url + ") : " + e.getMessage());
//        }
//    }

    public void loadUsers() {
        userInfos.clear();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from users", null );
        res.moveToFirst();

        while(res.isAfterLast() == false){
            @SuppressLint("Range") int userId = res.getInt(res.getColumnIndex(CONTACTS_COLUMN_ID));
            @SuppressLint("Range") String userName = res.getString(res.getColumnIndex(CONTACTS_COLUMN_NAME));
            @SuppressLint("Range") byte[] faceData = res.getBlob(res.getColumnIndex(CONTACTS_COLUMN_FACE));
            @SuppressLint("Range") String featData = res.getString(res.getColumnIndex(CONTACTS_COLUMN_FEATURE));
            Bitmap faceImage = BitmapFactory.decodeByteArray(faceData, 0, faceData.length);

            UserInfo face = new UserInfo(userId, userName, faceImage, featData);
            userInfos.add(face);

            res.moveToNext();
        }
    }

    public boolean getUsersSqlServer(StringBuffer msgRetorno){
        boolean retorno = false;
        String strRetorno = "";
        userInfos.clear();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        String url = "jdbc:jtds:sqlserver://192.168.0.251;";
        //String url = "jdbc:jtds:sqlserver://172.18.20.193;";
        try {

            // SET CONNECTIONSTRING
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            String username = "sa";
            String password = "teste123";
            Connection DbConn = DriverManager.getConnection(url, username, password);

            Log.i(TAG, "open");
            Statement stmt = DbConn.createStatement();
            ResultSet reset = stmt.executeQuery(" select * from fotos2");

            while(reset.next()) {
                int userId = reset.getInt(2);
                Blob bfaceData = reset.getBlob(3);
                Blob bfeatData = reset.getBlob(4);

                Log.i(TAG, "Verificando id " + String.valueOf(userId) + "...");

                byte[] faceDataB64 = null;
                byte[] faceData = null;
                Bitmap faceImage = null;
                byte[] featDataB64 = null;
                String featData = null;
                if (bfaceData != null) {
                    faceDataB64 = bfaceData.getBytes(1L, (int) bfaceData.length());
                    faceData = Base64.decode(faceDataB64, Base64.DEFAULT);
                    faceImage = BitmapFactory.decodeByteArray(faceData, 0, faceData.length);
                }else{
                    strRetorno += "Não há imagem para o id " + String.valueOf(userId) + "!\n";
                    continue;
                }
                if (bfeatData != null) {
                    featDataB64 = bfeatData.getBytes(1L, (int) bfeatData.length());
                    featData = featDataB64.toString();

                }
                UserInfo face = new UserInfo(userId, String.valueOf(userId), faceImage, featData);
                userInfos.add(face);
                Log.i(TAG, "Adicionou foto de userId " + userId);
            }
            DbConn.close();
            Log.i(TAG, "Tudo OK!");
            retorno = true;
        }catch(ClassNotFoundException cnf){
            Log.e(TAG, "Classe JDBC Driver nao encontrada.");
            strRetorno = "Classe JDBC Driver nao encontrada.";
        }catch(SQLException sql){
            strRetorno = "Erro ao se conectar com o banco de dados: " + sql.getMessage();
            Log.e(TAG, strRetorno);
        }catch (Exception e){
            strRetorno = "Error connection (for string " + url + ") : " + e.getMessage();
            Log.e(TAG, strRetorno);
        }
        msgRetorno.append((strRetorno));
        return retorno;
    }

    public void loadTesteByUsers(int userID, int maxLastRegisters) {
//        testeEpiInfos.clear();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        String url = "jdbc:jtds:sqlserver://192.168.0.251;";
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            Connection DbConn = DriverManager.getConnection(SQL_SERVER_URL, SQL_SERVER_USER, SQL_SERVER_PWD);
            Statement stmt = DbConn.createStatement();

            Log.i(TAG, "open");

            ResultSet res = stmt.executeQuery(" select * from testes_epi where id_colaborador=" + String.valueOf(userID) + " order by data_hora DESC");

            int count=0;
            while(res.next()){
                int testeId = res.getInt(1);
                boolean result = res.getInt(3) != 0;
                Timestamp tDataHora = res.getTimestamp(4);

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                String strDataHora  = dateFormat.format(tDataHora.getTime());

//                TesteEpiInfo face = new TesteEpiInfo(testeId, userID, result, strDataHora);
//                testeEpiInfos.add(face);
//                Log.i(TAG, "Adicionou registro de teste " + userID);
//                if(++count == maxLastRegisters)
//                    break;
            }

            DbConn.close();
            Log.i(TAG, "Registro adicionado no SQLServer!");

        }catch(ClassNotFoundException cnf){
            Log.e(TAG, "Classe JDBC Driver nao encontrada.");
        }catch(SQLException sql){
            Log.e(TAG, "Erro ao se conectar com o banco de dados: " + sql.getMessage());
        }catch (Exception e){
            Log.e(TAG, "Error connection (for string " + url + ") : " + e.getMessage());
        }
    }
}