package com.elselse.loklok;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PairActivity extends Activity {
    static int myID;
    private int fID;
    private long fIDI, myInBox, fIDF;
    private boolean b;
    private DatabaseReference reference;
    private DatabaseReference reference1;
    private EditText editText;
    static SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private Button connectBtn, cancelBtn;
    TextView textView3, statusText;
    ValueEventListener a;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

        setContentView(R.layout.activity_pair);
        TextView textview = findViewById(R.id.myIDText);
        connectBtn = findViewById(R.id.connect_button);
        cancelBtn = findViewById(R.id.cancel_button);

        prefs = getSharedPreferences("PAIR", MODE_PRIVATE);
        editor = prefs.edit();
        myID = prefs.getInt("myID", 0);
        if(myID == 0){
            textview.setText("...");
        }else{
            textview.setText(String.valueOf(myID));
        }
        myID = generateRandomID();
        Log.d("My ID", String.valueOf(myID));

        statusText = findViewById(R.id.statusText);
        editText = findViewById(R.id.frndId);
        textView3 = findViewById(R.id.textView3);

        if(prefs.getInt("FRIEND",0) != 0){
            fID = prefs.getInt("fID", 0);
            statusText.setText("You are connected to " + fID);
            editText.setVisibility(View.INVISIBLE);
            textView3.setVisibility(View.INVISIBLE);
            connectBtn.setText("Connected");
            cancelBtn.setText("Disconnect");
            connectBtn.setAlpha(0.5f);
        }
        reference = FirebaseDatabase.getInstance().getReference(String.valueOf(myID));

        a = reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.hasChild("My ID") && prefs.getInt("myID", 0) == 0){
                    Log.d("was here", "true");
                    myID = generateRandomID();
                    reference = FirebaseDatabase.getInstance().getReference(String.valueOf(myID));
                    onCreate(savedInstanceState);
                }else{
                    editor.putInt("myID", myID);
                    editor.apply();
                    myID = prefs.getInt("myID", 0);
                    reference.child("My ID").setValue(myID);
                    textview.setText(String.valueOf(myID));
                    Log.d("was here", "false");

                }

                if(snapshot.hasChild("INBOX") && snapshot.hasChild("REQUEST")){
                    if(myID > Integer.parseInt(String.valueOf(snapshot.child("INBOX").getValue()))) {
                        reference.child("FRIEND").setValue(myID * 10000 + Integer.parseInt(String.valueOf(snapshot.child("INBOX").getValue())));
                        editor.putInt("FRIEND", myID * 10000 + Integer.parseInt(String.valueOf(snapshot.child("INBOX").getValue())));
                    }
                    else {
                        reference.child("FRIEND").setValue(Integer.parseInt(String.valueOf(snapshot.child("INBOX").getValue())) * 10000 + myID);
                        editor.putInt("FRIEND", Integer.parseInt(String.valueOf(snapshot.child("INBOX").getValue())) * 10000 + myID);
                    }
                    editor.apply();
                    reference.child("INBOX").removeValue();
                    reference.child("REQUEST").removeValue();
                }else if(snapshot.hasChild("INBOX")){
                    fID = Integer.parseInt(String.valueOf(snapshot.child("INBOX").getValue()));
                    editor.putInt("fID", fID);
                    editor.apply();
                    myInBox = (long) snapshot.child("INBOX").getValue();
                    statusText.setText(myInBox + " has requested to connect with you.");
                    editText.setVisibility(View.INVISIBLE);
                    textView3.setVisibility(View.INVISIBLE);
                    connectBtn.setAlpha(1);
                    connectBtn.setText("Accept");
                    cancelBtn.setText("Decline");
                } else if(snapshot.hasChild("REQUEST")){
                    fID = Integer.parseInt(String.valueOf(snapshot.child("REQUEST").getValue()));
                    editor.putInt("fID", fID);
                    editor.apply();
                    statusText.setText("You have requested to connect with " + snapshot.child("REQUEST").getValue() + ".");
                    editText.setVisibility(View.INVISIBLE);
                    editText.setText("");
                    textView3.setVisibility(View.INVISIBLE);
                    connectBtn.setText("Connect");
                    connectBtn.setAlpha(0.5f);
                }else{
                    statusText.setText("Not connected!");
                    editText.setVisibility(View.VISIBLE);
                    connectBtn.setAlpha(1);
                    connectBtn.setText("Connect");
                    cancelBtn.setText("Cancel");
                    textView3.setVisibility(View.VISIBLE);
                }

                if(snapshot.hasChild("FRIEND")) {
                    fID = prefs.getInt("fID", 0);
                    statusText.setText("You are connected to " + fID);
                    editText.setVisibility(View.INVISIBLE);
                    textView3.setVisibility(View.INVISIBLE);
                    connectBtn.setText("Connected");
                    cancelBtn.setText("Disconnect");
                    connectBtn.setAlpha(0.5f);
                }else{
                    editor.remove("fID");
                    editor.remove("FRIEND");
                    editor.apply();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        textview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textview.setAlpha(0.5f);
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Copied Text", String.valueOf(myID));
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getApplicationContext(), myID + " Copied!", Toast.LENGTH_SHORT).show();
                textview.animate().setDuration(200).alpha(1f).start();
            }
        });

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                reference1 = FirebaseDatabase.getInstance().getReference(String.valueOf(s));
                reference1.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        b = snapshot.hasChild("My ID");
                        if(!String.valueOf(editText.getText()).equals(""))
                            if(Integer.parseInt(String.valueOf(editText.getText())) > 999) {
                                if(snapshot.hasChild("INBOX")) {
                                    fIDI = (long) snapshot.child("INBOX").getValue();
                                    Log.d("fIDI", String.valueOf(snapshot.child("INBOX").getValue()));
                                }
                                if(snapshot.hasChild("FRIEND")) {
                                    fIDF = (long) snapshot.child("FRIEND").getValue();
                                    Log.d("fIDI", String.valueOf(fIDF));
                                }
                                reference1.removeEventListener(this);
                            }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });
    }

    public void CancelBtn(View view) {
        reference.child("INBOX").removeValue();
        reference.child("REQUEST").removeValue();
        reference.child("FRIEND").removeValue();
        reference1 = FirebaseDatabase.getInstance().getReference(String.valueOf(fID));
        reference1.child("FRIEND").removeValue();
        reference1.child("INBOX").removeValue();
        reference1.child("REQUEST").removeValue();
        editor.remove("fID");
        editor.remove("FRIEND");
        editor.apply();
        editText.setVisibility(View.VISIBLE);
        connectBtn.setAlpha(1);
        connectBtn.setText("Connect");
        cancelBtn.setText("Cancel");
        textView3.setVisibility(View.VISIBLE);

    }

    public void ConnectBtn(View view) {
        if(statusText.getText() != "Loading status...") {
            if (!String.valueOf(editText.getText()).equals("")) {
                if (b && !String.valueOf(editText.getText()).equals(String.valueOf(myID)) && fIDI == 0 && fIDF == 0) {
                    reference.child("REQUEST").setValue(Integer.parseInt(String.valueOf(editText.getText())));
                    editor.putInt("fID", Integer.parseInt(String.valueOf(editText.getText())));
                    editor.apply();
                    reference1.child("INBOX").setValue(myID);
                } else if (String.valueOf(editText.getText()).equals(String.valueOf(myID))) {
                    Toast.makeText(getApplicationContext(), "Try your friend's ID instead of your own!", Toast.LENGTH_SHORT).show();
                } else if (!b) {
                    Toast.makeText(getApplicationContext(), "This ID doesn't exist!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Cannot request your friend.", Toast.LENGTH_SHORT).show();
                }
                Log.d("Edit Text", String.valueOf(editText.getText()));
                Log.d("myID", String.valueOf(myID));
            } else if (connectBtn.getText().equals("Accept")) {
                reference1 = FirebaseDatabase.getInstance().getReference(String.valueOf(myInBox));
                reference.child("REQUEST").setValue(myInBox);
                reference1.child("INBOX").setValue(myID);
            }
        }
    }

    private int generateRandomID(){
        if(myID == 0) {
            myID = (int) Math.round(Math.random() * 10000);
            if (myID < 1000) {
                myID = 0;
                generateRandomID();
            }
        }
        return myID;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        reference.removeEventListener(a);
    }

    public void navUP(View view) {
        this.finish();
    }
}
