package com.example.assignment2.assignment2;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import au.com.bytecode.opencsv.CSVWriter;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private Button loadContacts;
    private CSVWriter writer = null;
    private StringBuilder builder;
    private RelativeLayout relativeLayout;
    private Disposable disposable;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE_PICK_CONTACTS = 1;
    private Uri uriContact;
    private String contactID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        relativeLayout = (RelativeLayout) findViewById(R.id.relativelayout);
        loadContacts = (Button) findViewById(R.id.LoadContacts);
        loadContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(getApplicationContext(), "Permission Denied", Toast.LENGTH_LONG).show();
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_CONTACTS}, 1);
                    } else {


                        Observable<String> showContacts = getContact();
                        Observer<String> observer = getContactsObserver();
                        showContacts
                                .observeOn(Schedulers.io())
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(Schedulers.computation())
                                .subscribe(observer);

                    }
                }
            }
        });

    }

    @NonNull
    private String loadContacts() {
        StringBuilder builder = new StringBuilder();
        ContentResolver contentResolver=getContentResolver();
        Cursor c1=contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        while (c1.moveToNext())
        {
            String id = c1.getString(c1.getColumnIndex(ContactsContract.Contacts._ID));
            String name=c1.getString(c1.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
             Cursor c2=  contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    new String[]{id}, null);
            Cursor c3=contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,null,
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID+" = ?",new String[]{id},null);
             while (c2.moveToNext())
             {
                String phoneNumber=c2.getString(c2.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                 builder.append("Contact : ").append(name).append(", Phone Number : ").append(phoneNumber).append("\n\n");
             }
            c2.close();
             while (c3.moveToNext())
             {
                 String email=c3.getString(c3.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                 builder.append("Contact : ").append(name).append(", Email : ").append(email).append("\n\n");
             }
            c3.close();
        }
        c1.close();
        createCSVFile(builder.toString());//calling a create csv function
        return (builder.toString());
    }

    public void createCSVFile(String csv) {
        try {
            writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory().getAbsolutePath() + "/myfile.csv"), ',');

            String entries = csv;
            writer.writeNext(new String[]{entries});
            writer.close();
            Snackbar snackbar = Snackbar
                    .make(relativeLayout, "File Saved into Internal Storage", Snackbar.LENGTH_LONG);

            snackbar.show();
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            Log.d("ErrorInCSV", e.getStackTrace() + "");

        }

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

    @Override
    protected void onDestroy() {
        super.onDestroy();


        disposable.dispose();
    }


}