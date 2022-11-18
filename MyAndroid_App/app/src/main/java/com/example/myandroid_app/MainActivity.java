package com.example.myandroid_app;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private Button addPictureBtn;
    private Button mButton1;
    private Button mImport;
    private RelativeLayout ll;
    private IOException exception;

    private static final int CAMERA_REQUEST = 1888;
    private Bitmap bitmap;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int pictureId = 12;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton1 = (Button)findViewById(R.id.resetView);
        addPictureBtn = (Button)findViewById(R.id.addPicture);
        mImport = (Button)findViewById(R.id.importBtn);

        addPictureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                }
                else
                {
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                }

            }
        });

        mButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ll = (RelativeLayout)findViewById(R.id.imageList);
                ll.removeAllViews();
            }
        });

        mImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GetPicture();
            }
        });

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {

        if (requestCode == MY_CAMERA_PERMISSION_CODE)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
            else
            {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK)
        {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            bitmap = photo;
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

        byte[] byteArray = stream.toByteArray();
        String s = Base64.encodeToString(byteArray, Base64.DEFAULT);

        try {
            SavePicture("newImg", s);
            Update(bitmap);
        }
        catch (Exception ex)
        {

        }

    }

    private void post(String url, String json) throws IOException {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful())
                {
                    String responseResult = response.body().toString();
                }
            }
        });
    }

    private void SavePicture(String imageName, String imageString) throws IOException {


            JSONObject obj = new JSONObject();
        try {

            obj.put("imageName", imageName);
            obj.put("imageString", imageString);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        post("http://192.168.1.100:5123/Picture/PostPicture", obj.toString());
    }
    /**
     * Pre
     * Remember to set the Url to your local ip to get this to work and start the api
     *
     *
     * Post
     * If there is an error it will be set in exception
     */
    private void GetPicture(){
        OkHttpClient client = new OkHttpClient();
        String url = "http://10.108.137.29:5123/Picture";

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                exception = e;
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(response.isSuccessful()){
                    // Gets the response body as string
                    final String myResponse = response.body().string();

                    // Calls the ui thread to update
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            try {
                                // Converts the Json string to Json array
                                JSONArray root = new JSONArray(myResponse);
                                ll = (RelativeLayout)findViewById(R.id.imageList);

                                for (int i = 0; i < root.length();i++) {
                                    // Gets the Json object
                                    JSONObject jobj = root.getJSONObject(i);
                                    // Converts the imageString in the Json Object to bytes
                                    byte[] decodedString = Base64.decode(jobj.getString("imageString"), Base64.DEFAULT);
                                    // Generates a bitmap from the image bytes
                                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                    Update(decodedByte);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
    }

    private void Update(Bitmap bitmap){
        ImageView imgView = new ImageView(MainActivity.this);
        if(bitmap != null)
        {
            ll = (RelativeLayout)findViewById(R.id.imageList);
            imgView.setImageBitmap(bitmap);

            imgView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    ClipData.Item item = new ClipData.Item((CharSequence)v.getTag());
                    String[] mimeTypes = {ClipDescription.MIMETYPE_TEXT_PLAIN};

                    ClipData dragData = new ClipData(v.getTag().toString(),mimeTypes, item);
                    View.DragShadowBuilder myShadow = new View.DragShadowBuilder(imgView);

                    v.startDrag(dragData,myShadow,null,0);
                    return true;
                }
            });

            ll.addView(imgView);
        }

    }



}