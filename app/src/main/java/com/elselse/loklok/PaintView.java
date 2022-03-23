package com.elselse.loklok;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.storage.StorageManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PaintView extends View {

    private static final float TOUCH_TOLERANCE = 4;
    public static Bitmap btmView;
    public Bitmap[] undo = new Bitmap[10];
    public Paint mPaint = new Paint();
    private final Path mPath = new Path();
    boolean b = false;
    float mX, mY;
    int sizeBrush;
    int sizeEraser;
    private int i = -1, j;
    private Canvas mCanvas;
    private Intent serviceIntent;
    private final File myFile = new File("data/data/com.elselse.loklok/cache/","local.png"),
            myFile1 = new File("data/data/com.elselse.loklok/cache/","notlocal.png");


    public PaintView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        init();

    }

    private void init() {
        serviceIntent = new Intent(getContext(), UploadService.class);
        getContext().startService(serviceIntent);
        sizeEraser = 6;
        sizeBrush = 6;
        mPaint.setColor(Color.BLACK);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeWidth(toPx(sizeBrush));
        if(!myFile.exists()){
            if(!myFile1.exists()){
                btmView = Bitmap.createBitmap(getResources().getDisplayMetrics().widthPixels,getResources().getDisplayMetrics().heightPixels,Bitmap.Config.ARGB_8888);
            }else{
                btmView = BitmapFactory.decodeFile(myFile1.getAbsolutePath()).copy(Bitmap.Config.ARGB_8888, true);
            }
        }else{
            btmView = BitmapFactory.decodeFile(myFile.getAbsolutePath()).copy(Bitmap.Config.ARGB_8888,true);
        }
        i++;
        undo[i] = btmView.copy(btmView.getConfig(), true);
        j = i-1;
    }

    public float toPx(int sizeBrush) {
        return sizeBrush * (getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCanvas = new Canvas(btmView);//-->set bitmap
    }

    protected void onDraw(Canvas canvas){
        canvas.drawBitmap(btmView, 0, 0, null);//--> draw canvasBitmap on canvas

    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        b = true;
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStart(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);

                break;
            case MotionEvent.ACTION_UP:
                touchUP();
        }
        return true;
    }

    private void touchUP(){
        saveToUndo(btmView);
        mPath.reset();
    }

    private void saveToUndo(Bitmap btmView) {
        i++;
        for(j = i; j < undo.length; j++ ){
            Log.d("i", String.valueOf(i));
            undo[j] = null;
        }
        if(i >= undo.length-1){
            i = undo.length-1;
            for(int k = 0; k < i; k++){
                undo[k] = undo[k+1].copy(undo[k+1].getConfig(), true);
                undo[i] = btmView.copy(btmView.getConfig(),true);
            }
        }else{
            undo[i] = btmView.copy(btmView.getConfig(), true);
        }
        j = i-1;

    }

    public void undo(){
        if(i > 0) {
            i--;
            btmView = undo[i].copy(undo[i].getConfig(), true);
            mCanvas = new Canvas(btmView);
            invalidate();
        }
    }

    public void redo() {
        if(i >= 0 && i <= j) {
            i++;
            btmView = undo[i].copy(undo[i].getConfig(), true);
            mCanvas = new Canvas(btmView);
            invalidate();
        }
    }


    private void touchStart(float x, float y) {
        mPath.moveTo(x, y);
        mPath.lineTo(x,y);
        mX = x;
        mY = y;
        mCanvas.drawPath(mPath, mPaint);
        invalidate();
    }

    private void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
        mCanvas.drawPath(mPath, mPaint);
        invalidate();
    }

    public void saveBitmapToStorage(Bitmap bitmap) {
        new AsyncTask<Void, String, Void>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                Log.d("Saving Initiated", "Yes!");
            }

            @Override
            protected Void doInBackground(Void... voids) {
                Log.d("Saving Initiated", "Yes! In progress!");
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(myFile);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos = new FileOutputStream(myFile1);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                return null;
            }


            @Override
            protected void onPostExecute(Void unused) {
                super.onPostExecute(unused);

            }
        }.execute(null,null,null);
    }

    public void erase(){
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        mPaint.setStrokeWidth(toPx(sizeEraser));
    }
    public void pencil() {
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
        mPaint.setStrokeWidth(toPx(sizeBrush));
    }
    public void clear() {
        b = true;
        btmView = Bitmap.createBitmap(getResources().getDisplayMetrics().widthPixels,getResources().getDisplayMetrics().heightPixels,Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(btmView);
        saveToUndo(btmView);
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        for(int l = 0; l < undo.length-1; l++){
            undo[l] = null;
        }
        if(b) {
            saveBitmapToStorage(btmView);
            getContext().startService(serviceIntent);
        }
    }
}