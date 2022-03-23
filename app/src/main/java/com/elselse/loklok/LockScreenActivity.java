package com.elselse.loklok;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;

public class LockScreenActivity extends Activity {

    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1;
    SharedPreferences prefs, prefs1;
    static String loc;
    float initY;
    boolean b;
    StorageReference myRef;
    long l1, l, startTime;
    static final int MAX_DURATION = 200;
    Bitmap sketchBitmap, bgBitmap, blurredBitmap;
    File notLocal = new File("data/data/com.elselse.loklok/cache","notlocal.png");

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true);
        }
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
        }else{
            this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
    }

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_lockscreen);
        prefs1 = getSharedPreferences("PAIR",MODE_PRIVATE   );
        int FRIEND = prefs1.getInt("FRIEND", 0);
        Log.d("loc", String.valueOf(FRIEND));
        if(FRIEND != 0){
            loc = String.valueOf(FRIEND);
            myRef = FirebaseStorage.getInstance().getReference(loc + ".png");
            b = true;
        }else {
            b = false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onStart() {
        super.onStart();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setBackgroundImage();
        Intent intent = new Intent(this, UploadService.class);
        startService(intent);
        setDrawingImage();
    }

    @Override
    public void onBackPressed() {}

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        ImageView lockImg = findViewById(R.id.img_lock);
        View cLayout = findViewById(R.id.c_layout);
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                initY = event.getY();
                if(System.currentTimeMillis() - startTime <= MAX_DURATION)
                {
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("id","From_Lockscreen");
                    startActivity(intent);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(event.getY() - initY < 0)
                    cLayout.setY(event.getY() - initY);
                if(cLayout.getY() < -600) {
                    try {
                        lockImg.setImageDrawable(getResources().getDrawable(R.drawable.unlock));
                    }catch (Exception ignored){}
                }
                else {
                    try {
                        lockImg.setImageDrawable(getResources().getDrawable(R.drawable.lock));
                    }catch(Exception ignored){}
                }
                break;
            case MotionEvent.ACTION_UP:
                startTime = System.currentTimeMillis();
                if(cLayout.getY() < -600) {
                    ObjectAnimator animation = ObjectAnimator.ofFloat(cLayout, "translationY", -2300);
                    animation.setDuration(300);
                    animation.start();
                    animation.addListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            finish();
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });
                }
                else {
                    cLayout.animate().translationY(0).setDuration(300).start();
                }
                break;
        }
        return true;
    }


    private void setDrawingImage() {
        prefs = this.getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        ImageView imageView = findViewById(R.id.sketch_image);
        if(notLocal.exists()){
            sketchBitmap = BitmapFactory.decodeFile(notLocal.getAbsolutePath());
            imageView.setImageBitmap(sketchBitmap);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }
        if(b) {
            myRef.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                @Override
                public void onSuccess(StorageMetadata storageMetadata) {
                    l1 = storageMetadata.getUpdatedTimeMillis();
                    Log.d("l1", String.valueOf(l1));
                    l = prefs.getLong("update_on", 0);
                    Log.d("l", String.valueOf(l));
                    if (l1 > l) {
                        myRef.getFile(Uri.fromFile(notLocal)).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                sketchBitmap = BitmapFactory.decodeFile(notLocal.getAbsolutePath());
                                imageView.setImageBitmap(sketchBitmap);
                                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                                l = l1;
                                Log.d("l", String.valueOf(l));
                                editor.putLong("update_on", l);
                                editor.apply();
                            }
                        });
                    }
                }
            });
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setBackgroundImage() {
        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());
        ImageView imageView = findViewById(R.id.wallpaper_image1);
        ParcelFileDescriptor pfd = null;
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            if(wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_LOCK) == null && wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM) == null){
                imageView.setImageDrawable(getResources().getDrawable(R.drawable.wallpaper));
            }else if(wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_LOCK) == null){
                pfd = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM);
                bgBitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
                blurredBitmap = BlurBuilder.blur(this, bgBitmap);
                imageView.setImageBitmap(blurredBitmap);
                bgBitmap.recycle();
            }else{
                pfd = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_LOCK);
                bgBitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
                blurredBitmap = BlurBuilder.blur(this, bgBitmap);
                imageView.setImageBitmap(blurredBitmap);
                bgBitmap.recycle();
            }
        }
        try {
            pfd.close();
        }catch (Exception ignored){

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(sketchBitmap != null){
            sketchBitmap.recycle();
        }
        if(blurredBitmap != null){
            blurredBitmap.recycle();
        }
        if(bgBitmap != null){
            bgBitmap.recycle();
        }
    }
}
