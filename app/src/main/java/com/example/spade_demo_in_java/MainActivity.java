package com.example.spade_demo_in_java;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

    final int REQUEST_SAF = 1337;
    private Uri tempURI;
    @NotNull
    private String filename, filepath;
    @NotNull
    private byte[] iv;
    private KeyGenerator generator = KeyGenerator.getInstance("AES");
    private SecretKey secretKey;
    private Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

    public MainActivity() throws NoSuchAlgorithmException, NoSuchPaddingException {
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button loadButton = (Button)findViewById(R.id.load_button);
        Button executeButton = (Button)findViewById(R.id.execute_button);
        Spinner dataTypeDropdown = (Spinner)findViewById(R.id.data_type_spinner);
        Button decryptButton = (Button)findViewById(R.id.decrypt_button);
        ArrayAdapter arrayAdapter = ArrayAdapter.createFromResource((Context)this, R.array.data_types, android.R.layout.simple_spinner_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dataTypeDropdown.setAdapter((SpinnerAdapter)arrayAdapter);

        loadButton.setOnClickListener((View.OnClickListener)(new View.OnClickListener() {
            public final void onClick(View it) {
                openDirectory();
            }
        }));

        executeButton.setOnClickListener((View.OnClickListener)(new View.OnClickListener() {
            public final void onClick(View it) {
                try {
                    MainActivity.this.execute();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                }
            }
        }));

        decryptButton.setOnClickListener((View.OnClickListener)(new View.OnClickListener() {
            public final void onClick(View it) {
                try {
                    MainActivity.this.decrypt();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                } catch (InvalidKeyException e) {
                    e.printStackTrace();
                } catch (InvalidAlgorithmParameterException e){
                    e.printStackTrace();
                }
            }
        }));
    }

    public void quit(View view){
        super.finish();
        System.exit(0);
    }

    private void openDirectory(){
        Spinner dataTypeDropdown = (Spinner) findViewById(R.id.data_type_spinner);
        Intent intent;
        // MIME TYPE: pdf = application/pdf || mp3 = audio/mpeg
        switch(dataTypeDropdown.getSelectedItemPosition()){
            case 0:
                intent = (new Intent("android.intent.action.OPEN_DOCUMENT"))
                        .setType("application/pdf")  //later change it to */rte
                        .addCategory("android.intent.category.OPENABLE");
                break;
            case 1:
                intent = (new Intent("android.intent.action.OPEN_DOCUMENT"))
                        .setType("audio/mpeg")
                        .addCategory("android.intent.category.OPENABLE");
                break;
            case 2:
                intent = (new Intent("android.intent.action.OPEN_DOCUMENT"))
                        .setType("*/*") // later change it to DOF specific
                        .addCategory("android.intent.category.OPENABLE");
                break;
            case 3:
                intent = (new Intent("android.intent.action.OPEN_DOCUMENT"))
                        .setType("*/thr") // change to thr specific
                        .addCategory("android.intent.category.OPENABLE");
                break;
            default:
                intent = (new Intent("android.intent.action.OPEN_DOCUMENT"))
                        .setType("*/db") // change to Nav DB specific
                        .addCategory("android.intent.category.OPENABLE");
                break;
        }
        this.startActivityForResult(intent, REQUEST_SAF);
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_SAF && resultCode == RESULT_OK) {
            Uri uri = data != null ? data.getData() : null;
            this.tempURI = uri;
            filepath = tempURI.getPath();
            filename = filepath.substring(filepath.lastIndexOf("/")+1);
            ContentResolver cr = getApplicationContext().getContentResolver();
            String mimeType = cr.getType(tempURI);
            TextView pathTextView = (TextView) findViewById(R.id.pathTextView);
            pathTextView.setText(filepath);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
    
    private void execute() throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        File copiedFile = new File(getExternalFilesDir(null), filename + "copied");
        InputStream inputStream = getContentResolver().openInputStream(tempURI);
        copyStreamToFile(inputStream, copiedFile);
        // debugging variables
        /*Boolean existcheck = copiedFile.exists();
        Boolean readablecheck = copiedFile.canRead();*/
        
        encrypt(copiedFile);
    }


    private void copyStreamToFile(InputStream in, File file) {
        OutputStream out = null;

        try {
            out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            // Ensure that the InputStreams are closed even if there's an exception.
            try {
                if ( out != null ) {
                    out.close();
                }
                // If you want to close the "in" InputStream yourself then remove this from here but ensure that you close it yourself eventually.
                in.close();
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }

    private final void encrypt(File copiedFile) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        ActivityCompat.requestPermissions((Activity)this, new String[]{"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"}, 200);
        File path = new File(this.getExternalFilesDir((String)null), this.filename + "encrypted");
        FileInputStream fis = new FileInputStream(copiedFile);
        FileOutputStream fos = new FileOutputStream(path, false);
        this.generator.init(128);
        this.secretKey = this.generator.generateKey();
        cipher.init(Cipher.ENCRYPT_MODE, (Key) this.secretKey);
        CipherOutputStream output = new CipherOutputStream((FileOutputStream)fos, this.cipher);
        int bytesRead = 0;
        byte[] plainText = new byte[4096];

        while((bytesRead = fis.read(plainText)) >= 0) {
            output.write(plainText, 0, bytesRead);
        }
        output.flush();
        output.close();
        fos.close();
        fis.close();
        this.iv = cipher.getIV();
    }


    // Need to be written all over again similar to encrypt, but now decrypting. use the same key and iv.
    private final void decrypt() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        File path = new File(this.getExternalFilesDir(null), this.filename + "encrypted");
        FileInputStream fis = new FileInputStream(path);
        File path1 = new File(this.getExternalFilesDir(null), this.filename + "decrypted");
        FileOutputStream fos = new FileOutputStream(path1, false);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, (Key) this.secretKey, ivParameterSpec);
        CipherInputStream cis = new CipherInputStream(fis, cipher);
        int bytesRead = 0;
        byte[] plainText = new byte[4096];

        while((bytesRead = cis.read(plainText)) >= 0){
            fos.write(plainText, 0, bytesRead);
        }
        fos.flush();
        fos.close();
        cis.close();
    }

}