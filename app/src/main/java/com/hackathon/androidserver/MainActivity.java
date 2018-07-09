package com.hackathon.androidserver;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.hackathon.androidserver.network.GetDataService;
import com.hackathon.androidserver.network.RetrofitClientInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final int READ_SMS_PERMISSIONS_REQUEST = 1;
    private static final int SEND_SMS_PERMISSIONS_REQUEST = 0;
    private ArrayAdapter arrayAdapter;
    private ListView messages;
    private EditText phone;
    private GetDataService ListingService;
    List<String> messageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerReceiver(broadcastReceiver, new IntentFilter("broadCastName"));
        getPermissionToReadSMS();
        messages = (ListView) findViewById(R.id.Messages);
        phone = (EditText) findViewById(R.id.phone);
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messageList);
        messages.setAdapter(arrayAdapter);
        ListingService = RetrofitClientInstance.getRetrofitInstance().create(GetDataService.class);
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            String messageBody = b.getString("messageBody");
            String messageAddress = b.getString("messageAdress");
            String str = "SMS From: " + messageAddress +
                    "\n" + messageBody + "\n";
            callListingAPI(messageBody,messageAddress);
            arrayAdapter.add(str);
            arrayAdapter.notifyDataSetChanged();
            Toast.makeText(context, str, Toast.LENGTH_SHORT).show();

        }
    };

    private void callListingAPI(String messageBody, final String messageAddress) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(messageBody).append("|").append(messageAddress);
        Call<String> call = ListingService.callFlightListingAPI();
        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                Toast.makeText(MainActivity.this, "response is received", Toast.LENGTH_SHORT).show();
                sendTextMessage(messageAddress,response);
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Something went wrong...Please try later!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.M)
    public void getPermissionToReadSMS() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_SMS)) {
                Toast.makeText(this, "Please allow permission!", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[]{Manifest.permission.READ_SMS},
                    READ_SMS_PERMISSIONS_REQUEST);
            requestPermissions(new String[]{Manifest.permission.SEND_SMS},
                    SEND_SMS_PERMISSIONS_REQUEST);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == READ_SMS_PERMISSIONS_REQUEST) {
            if (grantResults.length == 1 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Read SMS permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Read SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public List<String> refreshSmsInbox() {
        messageList.clear();
        ContentResolver contentResolver = getContentResolver();
        Cursor smsInboxCursor = contentResolver.query(Uri.parse("content://sms/inbox"), null, null, null, null);
        int indexBody = smsInboxCursor.getColumnIndex("body");
        int indexAddress = smsInboxCursor.getColumnIndex("address");
        if (indexBody < 0 || !smsInboxCursor.moveToFirst()) {
            messageList.add("message empty");
            return messageList;
        }
        do {
            if (smsInboxCursor.getString(indexAddress).equals(phone.getText())) {
                String str = "SMS From: " + smsInboxCursor.getString(indexAddress) +
                        "\n" + smsInboxCursor.getString(indexBody) + "\n";
                messageList.add(str);
                arrayAdapter.add(str);
            }
        } while (smsInboxCursor.moveToNext());
        return messageList;
    }

    public void sendTextMessage(String address,Response<String> response){
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(address, null, response.body(), null, null);
    }
}
