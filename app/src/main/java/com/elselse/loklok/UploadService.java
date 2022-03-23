package com.elselse.loklok;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;

public class UploadService extends Service {
    private File myFile = new File("data/data/com.elselse.loklok/cache/","local.png");

    StorageReference myRef;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        uploadToFirebase();
        return START_NOT_STICKY;
    }
    public void uploadToFirebase() {
        String loc;
        SharedPreferences prefs1 = getSharedPreferences("PAIR",MODE_PRIVATE   );
        int FRIEND = prefs1.getInt("FRIEND", 0);
        if(FRIEND != 0){
            loc = String.valueOf(FRIEND);
            myRef = FirebaseStorage.getInstance().getReference(loc + ".png");
            new AsyncTask<Void, String, Void>() {

                @Override
                protected void onPreExecute() {
                    // before executing background task. Like showing a progress dialog
                }

                @SuppressLint("StaticFieldLeak")
                @Override
                protected Void doInBackground(Void... params) {
                    Log.d("Uploading", "true");
                    // Do background task here
                    if(myFile.exists()) {
                        myRef.putFile(Uri.fromFile(myFile)).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                myFile.delete();
                                stopSelf();
                            }
                        });
                    }else{
                        stopSelf();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void msg) {
                }
            }.execute(null, null, null);
        }
    }
}
