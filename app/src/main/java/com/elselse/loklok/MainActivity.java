package com.elselse.loklok;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

public class MainActivity extends Activity {

    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 2;
    ImageView bgWallpaper, hsv, mask;
    Button sizeFDBck;
    PaintView paintView;
    int pixel = Color.BLACK;
    static boolean b = true, k = true, l = false;
    Button pencilBtn, eraseBtn, clearBtn, hideBtn, pairBtn, undoBtn, redoBtn;
    SeekBar strokeSize;
    Animation myAnim;
    Drawable pencilColor;
    MyBounceInterpolator interpolator;
    Bitmap bitmap, blurredBitmap;
    File myFile1 = new File("data/data/com.elselse.loklok/cache/notlocal.png");
    int x = 0;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onAttachedToWindow() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        RequestPermission();
        if(this.getIntent().getExtras() != null) {
            if (this.getIntent().getExtras().getString("id").equals("From_Lockscreen")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    setShowWhenLocked(true);
                } else {
                    this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
                }
            }
        }
        int permissionCheck1 = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionCheck1 != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
        } else {}
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, LockScreenService.class);
        stopService(intent);
        startForegroundService(intent);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        pencilBtn = findViewById(R.id.pencil);
        eraseBtn = findViewById(R.id.erase);
        hideBtn = findViewById(R.id.hide);
        clearBtn = findViewById(R.id.clear);
        strokeSize = findViewById(R.id.seekBar);
        pairBtn = findViewById(R.id.pairBtn);
        undoBtn = findViewById(R.id.undo);
        redoBtn = findViewById(R.id.redo);
        sizeFDBck = findViewById(R.id.sizeFDBck);
        eraseBtn.setAlpha(0.5f);

        paintView = findViewById(R.id.paintView);
        bgWallpaper = findViewById(R.id.wallpaper_image);
        mask = findViewById(R.id.mask);


        pencilColor = getDrawable(R.drawable.ic_pencil);

        strokeSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.d("Progress", String.valueOf(progress));
                if(progress < 2){
                    progress = 2;
                }
                sizeFDBck.setVisibility(View.VISIBLE);
                sizeFDBck.setAlpha(1f);
                sizeFDBck.setScaleX(progress/50f);
                sizeFDBck.setScaleY(progress/50f);
                Drawable drawable = getDrawable(R.drawable.ic_brushsize);
                if(!b) {
                    paintView.sizeBrush = strokeSize.getProgress();
                    paintView.mPaint.setStrokeWidth(paintView.toPx(paintView.sizeBrush));
                    drawable.setColorFilter(new PorterDuffColorFilter(pixel, PorterDuff.Mode.SRC_ATOP));
                    sizeFDBck.setBackgroundDrawable(drawable);
                }else{
                    sizeFDBck.setAlpha(0.5f);
                    drawable.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP));
                    sizeFDBck.setBackgroundDrawable(drawable);
                    paintView.sizeEraser = strokeSize.getProgress();
                    paintView.mPaint.setStrokeWidth(paintView.toPx(paintView.sizeEraser));
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                sizeFDBck.animate().setDuration(300).alpha(0f).start();
            }
        });

        myAnim = AnimationUtils.loadAnimation(this, R.anim.bounce);
        interpolator = new MyBounceInterpolator(0.5, 5);
        myAnim.setInterpolator(interpolator);

        if(myFile1.exists()) {
            if (Integer.parseInt(String.valueOf(myFile1.length() / 1024)) < 1 || !myFile1.canRead()) {
                myFile1.delete();
                SharedPreferences prefs1 = getSharedPreferences("PAIR", MODE_PRIVATE);
                if (prefs1.getInt("FRIEND", 0) > 0) {
                    StorageReference ref = FirebaseStorage.getInstance().getReference(prefs1.getInt("FRIEND", 0) + ".png");
                    ref.getFile(Uri.fromFile(myFile1)).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            Intent intent1 = new Intent(getApplicationContext(), MainActivity.class);
                            intent1.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent1);
                        }
                    });

                }
            }
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onStart() {
        super.onStart();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setBackgroundImage();

        hsv = findViewById(R.id.hsv);
        hsv.setDrawingCacheEnabled(true);
        hsv.buildDrawingCache(true);
        hsv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Bitmap bitmap = hsv.getDrawingCache();
                if(event.getX() > 0 && event.getY() > 0 && event.getX() < bitmap.getWidth() && event.getY() < bitmap.getHeight()) {
                    pixel = bitmap.getPixel((int) event.getX(), (int) event.getY());
                }

                pencilColor.setColorFilter(new PorterDuffColorFilter(pixel, PorterDuff.Mode.SRC_ATOP));
                pencilBtn.setBackground(pencilColor);
                if(!b) {
                    paintView.mPaint.setColor(pixel);
                }
                return true;
            }
        });

    }


    public void erase(View view) {
        eraseBtn.startAnimation(myAnim);
        if(x == 0){
            mask.setVisibility(View.INVISIBLE);
            eraseBtn.setAlpha(1);
            pencilBtn.setAlpha(0.5f);
            b = true;
            hsv.setVisibility(View.INVISIBLE);
            clearBtn.setVisibility(View.VISIBLE);
            strokeSize.setVisibility(View.INVISIBLE);
            x = 1;
        }else if(x == 1){
            if(!l) {
                mask.setVisibility(View.VISIBLE);
                strokeSize.setVisibility(View.VISIBLE);
                strokeSize.setProgress(paintView.sizeEraser);
                l = true;
            }else{
                mask.setVisibility(View.INVISIBLE);
                strokeSize.setVisibility(View.INVISIBLE);
                l = false;
            }
            sizeFDBck.setVisibility(View.INVISIBLE);
        }
        paintView.erase();
    }

    public void pencil(View view) {
        pencilBtn.startAnimation(myAnim);
        if(x == 1){
            mask.setVisibility(View.INVISIBLE);
            pencilBtn.setAlpha(1);
            eraseBtn.setAlpha(0.5f);
            strokeSize.setVisibility(View.INVISIBLE);
            l = false;
            x = 0;
        }else if(x == 0){
            hsv.bringToFront();
            if(b) {
                mask.setVisibility(View.VISIBLE);
                b = false;
                paintView.setFocusableInTouchMode(false);
                hsv.setVisibility(View.VISIBLE);
                clearBtn.setVisibility(View.INVISIBLE);
                strokeSize.setVisibility(View.VISIBLE);
                strokeSize.setProgress(paintView.sizeBrush);
            }else {
                mask.setVisibility(View.INVISIBLE);
                b = true;
                paintView.setFocusableInTouchMode(false);
                paintView.setClickable(true);
                hsv.setVisibility(View.INVISIBLE);
                clearBtn.setVisibility(View.VISIBLE);
                strokeSize.setVisibility(View.INVISIBLE);
            }
            sizeFDBck.setVisibility(View.INVISIBLE);
        }
        paintView.pencil();
    }

    public void clear(View view) {
        clearBtn.startAnimation(myAnim);
        paintView.clear();
    }

    public void hide(View view) {
        hideBtn.startAnimation(myAnim);
        if(k){
            hideBtn.setAlpha(0.2f);
            pencilBtn.setVisibility(View.INVISIBLE);
            clearBtn.setVisibility(View.INVISIBLE);
            eraseBtn.setVisibility(View.INVISIBLE);
            pairBtn.setVisibility(View.INVISIBLE);
            k = false;
        }else{
            hideBtn.setAlpha(1f);
            pencilBtn.setVisibility(View.VISIBLE);
            clearBtn.setVisibility(View.VISIBLE);
            eraseBtn.setVisibility(View.VISIBLE);
            pairBtn.setVisibility(View.VISIBLE);
            k = true;
        }

    }

    public void undo(View view) {
        undoBtn.startAnimation(myAnim);
        paintView.undo();
    }

    public void redo(View view) {
        redoBtn.startAnimation(myAnim);
        paintView.redo();
    }

    public void pairBtn(View view) {
        pairBtn.startAnimation(myAnim);
        Intent intent = new Intent(this, PairActivity.class);
        startActivity(intent);
    }

    public void onTouch(View view) {
        mask.setVisibility(View.INVISIBLE);
        b = true;
        l = false;
        strokeSize.setVisibility(View.INVISIBLE);
        hsv.setVisibility(View.INVISIBLE);
        clearBtn.setVisibility(View.VISIBLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_READ_EXTERNAL_STORAGE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void RequestPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + this.getPackageName()));
            startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void setBackgroundImage() {
        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(getApplicationContext());


        ParcelFileDescriptor pfd = null;
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            if(wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_LOCK) == null && wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM) == null){
                @SuppressLint("UseCompatLoadingForDrawables") Drawable drawable = getDrawable(R.drawable.wallpaper);
                bgWallpaper.setImageDrawable(drawable);
            }else if(wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_LOCK) == null){
                pfd = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM);
                bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
                blurredBitmap = BlurBuilder.blur(this, bitmap);
                bgWallpaper.setImageBitmap(blurredBitmap);
            }else{
                pfd = wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_LOCK);
                bitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
                blurredBitmap = BlurBuilder.blur(this, bitmap);
                bgWallpaper.setImageBitmap(blurredBitmap);
            }
        }
        try {
            pfd.close();
        }catch (Exception ignored){

        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        hsv.setDrawingCacheEnabled(false);
        hsv.destroyDrawingCache();
        if(bitmap != null){
            bitmap.recycle();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hsv.setDrawingCacheEnabled(false);
        hsv.destroyDrawingCache();
        if(bitmap != null){
            bitmap.recycle();
        }
    }

}
