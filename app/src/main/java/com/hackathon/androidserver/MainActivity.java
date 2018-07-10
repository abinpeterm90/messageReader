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
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.hackathon.androidserver.model.FlightList;
import com.hackathon.androidserver.network.GetDataService;
import com.hackathon.androidserver.network.RetrofitClientInstance;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final int READ_SMS_PERMISSIONS_REQUEST = 1;
    private static final int SEND_SMS_PERMISSIONS_REQUEST = 0;
    private ArrayAdapter arrayAdapter;
    private ListView messages;
    private Button button;
    private GetDataService ListingService;
    List<String> messageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerReceiver(broadcastReceiver, new IntentFilter("broadCastName"));
        getPermissionToReadSMS();
        messages = (ListView) findViewById(R.id.Messages);
        Button apiCallButton=(Button)findViewById(R.id.button);
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messageList);
        messages.setAdapter(arrayAdapter);
        ListingService = RetrofitClientInstance.getRetrofitInstance().create(GetDataService.class);
        apiCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callListingAPI("1","9048050286");
            }
        });
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            String messageBody = b.getString("messageBody");
            String messageAddress = b.getString("messageAddress");
            String str = "SMS From: " + messageAddress + "\n" + messageBody + "\n";
            callListingAPI(messageBody,messageAddress);
            arrayAdapter.add(str);
            arrayAdapter.notifyDataSetChanged();
            Toast.makeText(context, str, Toast.LENGTH_SHORT).show();

        }
    };

    private void callListingAPI(String messageBody, final String messageAddress) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(messageBody).append("$").append(messageAddress);
        com.hackathon.androidserver.model.Message message=new com.hackathon.androidserver.model.Message();
        message.setMobileNumber(messageAddress);
        message.setSearchCriteria(messageBody);
        if(messageBody.trim().length()==1) {
            message.setRepriceKey(messageBody);
            message.setSearchCriteria(null);
        }
        else{
            message.setRepriceKey(null);
        }
        Call<List<FlightList>> call = ListingService.callFlightListingAPI(message);
        call.enqueue(new Callback<List<FlightList>>() {

            @Override
            public void onResponse(Call<List<FlightList>> call, Response<List<FlightList>> response) {

                List<FlightList>resultTest=response.body();
                if(resultTest!=null && resultTest.size()>0) {
                    StringBuilder messageText = new StringBuilder();
                    for (FlightList i : resultTest) {
                        if (i.getId() != null) {
                            messageText.append(i.getId()).append(")");
                        }
                        if (i.getAirlines() != null) {
                            messageText.append(i.getAirlines());
                        }
                        if (i.getDeparture() != null) {
                            messageText.append(i.getDeparture());
                        }
                        if (i.getArrival() != null) {
                            messageText.append(i.getArrival());
                        }
                        if (i.getTotalcost() != null) {
                            messageText.append(i.getTotalcost());
                        }
                        if(i.getBookingConfirmationNumber()!=null){
                            messageText.append(i.getBookingConfirmationNumber());
                        }
                        messageText.append("\n");
                        sendTextMessage(messageAddress, messageText.toString());
                    }
                    Toast.makeText(getApplicationContext(),"success"+messageText.toString(),Toast.LENGTH_SHORT).show();
                }
                Log.e("Response",response.body().get(0).getAirlines());
            }

            @Override
            public void onFailure(Call<List<FlightList>> call, Throwable t) {
            Toast.makeText(getApplicationContext(),"fail",Toast.LENGTH_SHORT).show();
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
                String str = "SMS From: " + smsInboxCursor.getString(indexAddress) +
                        "\n" + smsInboxCursor.getString(indexBody) + "\n";
                messageList.add(str);
                arrayAdapter.add(str);

        } while (smsInboxCursor.moveToNext());
        return messageList;
    }

    public void sendTextMessage(String address,String response){
        SmsManager smsManager = SmsManager.getDefault();
        smsManager.sendTextMessage(address, null, response, null, null);
    }
}
