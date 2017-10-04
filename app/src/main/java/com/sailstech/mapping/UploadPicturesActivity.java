package com.sailstech.mapping;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bilibili.boxing.Boxing;
import com.bilibili.boxing.BoxingMediaLoader;
import com.bilibili.boxing.model.config.BoxingConfig;
import com.bilibili.boxing.model.config.BoxingCropOption;
import com.bilibili.boxing.model.entity.BaseMedia;
import com.bilibili.boxing.utils.BoxingFileHelper;
import com.bilibili.boxing_impl.ui.BoxingActivity;
import com.bumptech.glide.Glide;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.sailstech.mapping.parse.NavigationPhotoTest;
import com.sailstech.mapping.ui.LocShareNameDialog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

public class UploadPicturesActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_WRITE_SD_AND_RECORD_AUDIO = 0x11;
    private static String hospitalName;

    private static String uploadFilePath = "";
    private static final String testBucket = "ipsmap";
    private static String uploadObject = "";
    private static final String downloadObject = "sampleObject";
    private final String DIR_NAME = "oss";
    private final String FILE_NAME = "caifang.m4a";
    private static final int REQUEST_CODE = 1024;
    private ImageView ivSrc;
    private ImageView ivCompress;
    private TextView fileSize;
    private TextView imageSize;
    private TextView thumbFileSize;
    private TextView thumbImageSize;
    private File lubanFile;
    private ProgressDialog progressDialog;
    private Button uploadParseServer;
    private NavigationPhotoTest navigationPhotoTest;
    private static final String URL = "userName";
    private static final String BUILDINGID = "buildingId";
    private static final String SELFID = "selfId";
    private static final String COMMENT = "comment";
    private int selfId = 0;
    private String buildingId  = null;
    private String descripPicture;
    private Button selectPiactur;
    private Button startUploadPicture;
    private ParseFile parseFile;
    private LocShareNameDialog locShareNameDialog;
    private NavigationPhotoTest navigationPhotoTest1;

    protected static Intent getIntent(Context context,String buildingId ,int  selfId ,String hospitalName){
        UploadPicturesActivity.hospitalName = hospitalName;
        Intent intent = new Intent(context, UploadPicturesActivity.class);
        intent.putExtra(BUILDINGID, buildingId);
        intent.putExtra(SELFID, selfId);
        return intent;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_picture);
        buildingId = getIntent().getStringExtra(BUILDINGID);
        selfId = getIntent().getIntExtra(SELFID,0);
        fileSize = (TextView) findViewById(R.id.file_size);
        imageSize = (TextView) findViewById(R.id.image_size);
        thumbFileSize = (TextView) findViewById(R.id.thumb_file_size);
        thumbImageSize = (TextView) findViewById(R.id.thumb_image_size);
        uploadParseServer = (Button) findViewById(R.id.btn_upload_parse_server);
//        uploadParseServer.setVisibility(View.GONE);
//        uploadParseServer.setVisibility(View.GONE);
        uploadParseServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                uploadParseServer();
            }
        });


        selectPiactur = (Button) findViewById(R.id.btn_start_select_picture);
        selectPiactur.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBoxing();
            }
        });
        startUploadPicture = (Button) findViewById(R.id.btn_start_upload);
//        startUploadPicture.setVisibility(View.GONE);
        startUploadPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locShareNameDialog = new LocShareNameDialog(UploadPicturesActivity.this, new LocShareNameDialog.OnFinishClickListener() {
                    @Override
                    public void onFinish(String descripPicture) {
                        if (!descripPicture.isEmpty() && descripPicture != null) {
                            UploadPicturesActivity.this.descripPicture = descripPicture;
                            doUpload(UploadPicturesActivity.this, descripPicture);
//                            uploadParseServer.setVisibility(View.VISIBLE);
                        }
                    }
                });
                locShareNameDialog.show();
                // startBoxing();
            }
        });
        ivSrc = (ImageView) findViewById(R.id.ivSrc);
        ivCompress = (ImageView) findViewById(R.id.ivCompress);
//        initOSS();
        quaryParseServer();

    }

    private void quaryParseServer() {

        ParseQuery<NavigationPhotoTest> query = ParseQuery.getQuery(NavigationPhotoTest.class);
        query.whereEqualTo(NavigationPhotoTest.SELFID,selfId).whereEqualTo(NavigationPhotoTest.BUILDINGID,buildingId);
        query.findInBackground(new FindCallback<NavigationPhotoTest>() {
            @Override
            public void done(List<NavigationPhotoTest> objects, ParseException e) {
                if (e != null) {
                    e.printStackTrace();
                }else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (objects.size() > 0){
                                selectPiactur.setText("上次已經上傳,請重新選擇");
                                Glide.with(UploadPicturesActivity.this).load(objects.get(0).getUrl()).into(ivCompress);
                                navigationPhotoTest1 = objects.get(0);
                            }
                        }
                    });

                }
            }
        });
    }

    private void uploadParseServer() {
//        private int selfId = 0;
//        private String buildingId  = null;

        if (navigationPhotoTest1!= null){
            navigationPhotoTest =  navigationPhotoTest1;
        }else if (navigationPhotoTest ==null){
            navigationPhotoTest = new NavigationPhotoTest();
        }
        navigationPhotoTest.put(NavigationPhotoTest.BUILDINGID,buildingId);
        navigationPhotoTest.put(NavigationPhotoTest.COMMENT,descripPicture);
        navigationPhotoTest.put(NavigationPhotoTest.SELFID,selfId);

        String str = descripPicture;
        String result = null;
        String hospitalNameEncode = null;
        try {
            result = URLEncoder.encode(str, "utf-8");
            hospitalNameEncode = URLEncoder.encode(hospitalName, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Log.e("锦江大酒店就",result);
        //%E9%94%A6%E6%B1%9F%E5%A4%A7%E9%85%92%E5%BA%97%E5%B0%B1
        //%E9%94%A6%E6%B1%9F%E5%A4%A7%E9%85%92%E5%BA%97%E5%B0%B1
//        navigationPhotoTest.put(NavigationPhotoTest.URL,"http://ipsmap.oss-cn-shanghai.aliyuncs.com/"+hospitalNameEncode+"/"+result+".jpg");
//        parseFile.
        navigationPhotoTest.put(NavigationPhotoTest.URL,parseFile.getUrl());
        navigationPhotoTest.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            locShareNameDialog.dismiss();
                        }
                    });
                    return;
                }else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            locShareNameDialog.dismiss();
                        }
                    });
                    Toast.makeText(getBaseContext(),"上穿成功!ParseServer",Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void doUpload(Context context ,String fileName ) {
        new Thread(new Runnable() {

            private String result;

            @Override
            public void run() {
                uploadFilePath = lubanFile.getAbsolutePath().toString();
                if (uploadFilePath!=null){
                    Log.e("uploadFilePath----",uploadFilePath);
                    uploadObject = hospitalName+"/"+fileName+".jpg";
                    byte[] bytes = image2byte(uploadFilePath);
                    parseFile = new ParseFile( "resume.jpg", bytes);
                    parseFile.saveInBackground();
                    parseFile.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e != null){
                                e.printStackTrace();
                            }else {
                                uploadParseServer();
                            }
                        }
                    });
                    //  uploadObject = random+".jpg";
                   // uploadObject = uploadFilePath.substring(uploadFilePath.lastIndexOf("//"));
//                    Log.e("uploadObject----",uploadObject);
//                    new PutObjectOss(oss, testBucket, uploadObject, uploadFilePath ,context).putObjectWithMetadataSetting();
                }else {
                    Toast.makeText(getApplicationContext(),"获取路径失败",Toast.LENGTH_LONG);
                }


            }
        }).start();
    }

    //图片到byte数组
    public byte[] image2byte(String path){
        byte[] data = null;
        FileInputStream input = null;
        try {
            input = new FileInputStream(new File(path));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int numBytesRead = 0;
            while ((numBytesRead = input.read(buf)) != -1) {
                output.write(buf, 0, numBytesRead);
            }
            data = output.toByteArray();
            output.close();
            input.close();
        }
        catch (FileNotFoundException ex1) {
            ex1.printStackTrace();
        }
        catch (IOException ex1) {
            ex1.printStackTrace();
        }
        return data;
    }

    private void startBoxing() {
        String cachePath = BoxingFileHelper.getCacheDir(this);
        if (TextUtils.isEmpty(cachePath)) {
            Toast.makeText(getApplicationContext(), R.string.boxing_storage_deny, Toast.LENGTH_SHORT).show();
            return;
        }
        Uri destUri = new Uri.Builder()
                .scheme("file")
                .appendPath(cachePath)
                .appendPath(String.format(Locale.US, "%s.jpg", System.currentTimeMillis()))
                .build();
        BoxingConfig singleCropImgConfig = new BoxingConfig(BoxingConfig.Mode.SINGLE_IMG).withCropOption(new BoxingCropOption(destUri))
                .withMediaPlaceHolderRes(R.mipmap.ic_launcher)
                .needCamera(R.drawable.ic_boxing_camera_white);
        Boxing.of(singleCropImgConfig).withIntent(this, BoxingActivity.class).start(this, REQUEST_CODE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            final ArrayList<BaseMedia> medias = Boxing.getResult(data);
            if (requestCode == REQUEST_CODE) {
                startUploadPicture.setVisibility(View.VISIBLE);
                Log.e("----------", medias.get(0).getPath());
                Log.e("----------", medias.get(0).getSize() + "");
                Log.e("----------", medias.get(0).getPath());
                Log.e("----------", medias.get(0).getPath());
                String pathSrc = medias.get(0).getPath();
                BoxingMediaLoader.getInstance().displayThumbnail(ivSrc, pathSrc, 150, 150);
                File imgFile = new File(pathSrc);
                fileSize.setText(imgFile.length() / 1024 + "k");
                imageSize.setText(computeSize(imgFile)[0] + "*" + computeSize(imgFile)[1]);
                compressWithLs(imgFile);
            } else {

            }
        }
    }

    /**
     * 压缩单张图片 Listener 方式
     */
    private void compressWithLs(File file) {
        Luban.with(this)
                .load(file)
                .setCompressListener(new OnCompressListener() {


                    @Override
                    public void onStart() {
                        Toast.makeText(UploadPicturesActivity.this, "I'm start", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onSuccess(File file) {
                        lubanFile = file;
                        Log.i("path", file.getAbsolutePath());
                        Glide.with(UploadPicturesActivity.this).load(file).into(ivCompress);
                        thumbFileSize.setText(file.length() / 1024 + "k");
                        thumbImageSize.setText(computeSize(file)[0] + "*" + computeSize(file)[1]);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                }).launch();
    }

    private int[] computeSize(File srcImg) {
        int[] size = new int[2];

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inSampleSize = 1;

        BitmapFactory.decodeFile(srcImg.getAbsolutePath(), options);
        size[0] = options.outWidth;
        size[1] = options.outHeight;

        return size;
    }
}
