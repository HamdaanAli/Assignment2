package com.example.assignment2.assignment2;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileWriter;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVWriter;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private Button loadContacts;
    private TextView listContacts;
    private CSVWriter writer=null;
    private StringBuilder builder;
    private Thread thread;
    private Disposable disposable;
    private static final String TAG =MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        listContacts=(TextView) findViewById(R.id.ListContact);
        loadContacts=(Button) findViewById(R.id.LoadContacts);
        loadContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                {
                    if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_CONTACTS)!= PackageManager.PERMISSION_GRANTED)
                    {
                        Toast.makeText(getApplicationContext(),"Permission Denied",Toast.LENGTH_LONG).show();
                        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_CONTACTS},1);
                    }else
                    {


                     Observable<String> showContacts=getContact();
                     Observer<String> observer=getContactsObserver();
                     showContacts
                             .observeOn(Schedulers.io())
                             .subscribeOn(AndroidSchedulers.mainThread())
                             .subscribe(observer);

                    }
                }
            }
        });
    }

    private String loadContacts() {
        StringBuilder builder=new StringBuilder();
        ContentResolver contentResolver=getContentResolver();
        Cursor cursor= contentResolver.query(ContactsContract.Contacts.CONTENT_URI,null,null,null,null);
        if(cursor.getCount()>0)
        {
            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                int hasPhoneNumber = Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)));
                if (hasPhoneNumber > 0) {
                    Cursor cursor1 = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone._ID + " = ?",
                            new String[]{id}, null);

                    while (cursor1.moveToNext()) {
                        String phoneNumber = cursor1.getString(cursor1.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        builder.append("Contact : ").append(name).append(", Phone Number : ").append(phoneNumber).append("\n\n");
                    }
                    cursor1.close();
                }
            }
        }
       cursor.close();

        listContacts.setText(builder.toString());
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(getApplicationContext(),"Permission Denied",Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},2);
            }else
            {


                String file_csv=createCSVFile(builder.toString());
                Log.d("CSV",file_csv);
            }
        }
       return builder.toString();
    }

    public String createCSVFile(String csv)
    {
        try
        {
            writer = new CSVWriter(new FileWriter("/sdcard/myfile.csv"), ',');
            String entries = csv;
            writer.writeNext(new String[]{entries});
            writer.close();
        }
        catch (IOException e)
        {
            Toast.makeText(MainActivity.this,e.getMessage(),Toast.LENGTH_LONG).show();

        }
        return csv;
   }
    private Observer<String> getContactsObserver() {
        return new Observer<String>() {

            @Override
            public void onSubscribe(Disposable d) {
                Log.d(TAG, "onSubscribe");
                disposable = d;
            }

            @Override
            public void onNext(String s) {
                Log.d(TAG, "Name: " + s);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError: " + e.getMessage());
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "All items are emitted!");
            }
        };
    }
    private Observable<String> getContact() {
        return Observable.just(loadContacts());
    }
}
