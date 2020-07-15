package com.example.spade_demo_in_java;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.Buffer;
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
    private String filename, filepath, fileExtension, fullfilename;
    @NotNull
    private byte[] iv;
    private KeyGenerator generator = KeyGenerator.getInstance("AES");
    private SecretKey secretKey;
    private Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    String ipAddress = "192.168.56.101"; // POC wifi= 172.30.76.58  POC my office LAN = 172.30.0.14  ubuntu = 192.168.56.101
    // under POC Employee wifi    Lab computer 172.16.0.6
    // 192.168.56.1, 172.30.76.58, 172.30.76.1, 192.168.1.123 doesnt work

    // For ipAddress, debug server java file line 49 InetAddress inet = InetAddress.getLocalHost() and use that value
    int port = 6000;

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
        Button sendButton = (Button) findViewById(R.id.send_button);
        Button receiveButton = (Button) findViewById(R.id.receive_button);
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

        sendButton.setOnClickListener((View.OnClickListener)(new View.OnClickListener() {
            public final void onClick(View it) {
                try {
                    MainActivity.this.send();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }));

        receiveButton.setOnClickListener((View.OnClickListener)(new View.OnClickListener(){
            public final void onClick(View it){

                    MainActivity.this.receive();

            }
        })
        );
    }

    private void receive() {
        Socket socket;
        ServerSocket serverSocket;
        ObjectInputStream ois;
        FileOutputStream fos;
        try{
            serverSocket = new ServerSocket(5000);
            serverSocket.setSoTimeout(30000);
            socket = serverSocket.accept();
            ois = new ObjectInputStream(socket.getInputStream());
            String filename = ois.readUTF();
            fos = new FileOutputStream(new File(this.getExternalFilesDir(null),"encrypted_"+filename));

        } catch (IOException e) {
            e.printStackTrace();
        }


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

            ContentResolver cr = getApplicationContext().getContentResolver();
            // THIS WILL BE USEFUL LATER WHEN WE TEST OTHER TYPES OF FILE
            // THEN WE CAN CHECK THE MIME TYPE TO SPECIFY THE SPINNER IN openDirectory()
            String mimeType = cr.getType(tempURI);

            DocumentFile documentFile = DocumentFile.fromSingleUri(this, tempURI);
            String fileNameWithExtension = documentFile.getName();
            int index = fileNameWithExtension.lastIndexOf(".");
            this.fullfilename = documentFile.getName();
            this.filename = fileNameWithExtension.substring(0,index);
            this.fileExtension = fileNameWithExtension.substring(index+1);

            TextView pathTextView = (TextView) findViewById(R.id.pathTextView);
            pathTextView.setText(fileNameWithExtension);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
    
    private void execute() throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        if(tempURI == null){
            Toast toast = Toast.makeText(getApplicationContext(), "No file is selected", Toast.LENGTH_SHORT);
            //toast.setMargin(50,50);
            toast.show();
        }
        else {
            File copiedFile = new File(getExternalFilesDir(null), filename + "_copied" + "." + fileExtension);
            InputStream inputStream = getContentResolver().openInputStream(tempURI);
            copyStreamToFile(inputStream, copiedFile);
            // debugging variables
        /*Boolean existcheck = copiedFile.exists();
        Boolean readablecheck = copiedFile.canRead();*/

            encrypt(copiedFile);
            copiedFile.delete(); // For security, copied file is deleted. Not sure if this is secure enough.
        }
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
        File path = new File(this.getExternalFilesDir((String)null), this.filename + "_encrypted"+ "." + fileExtension);
        FileInputStream fis = new FileInputStream(copiedFile);
        FileOutputStream fos = new FileOutputStream(path, false);
        this.generator.init(128);
        this.secretKey = this.generator.generateKey();
        cipher.init(Cipher.ENCRYPT_MODE, (Key) this.secretKey);
        CipherOutputStream output = new CipherOutputStream((FileOutputStream)fos, this.cipher);
        int bytesRead = 0;
        byte[] plainText = new byte[8192];

        while((bytesRead = fis.read(plainText)) >= 0) {
            output.write(plainText, 0, bytesRead);
            System.out.println(bytesRead + " is read");
        }
        System.out.println(path.length());
        output.flush();
        output.close();
        fos.close();
        fis.close();
        this.iv = cipher.getIV();

        //SAVING THE KEY
        File ivpath = new File(this.getExternalFilesDir(null), this.filename + "_iv.key");
        FileOutputStream ivfos = new FileOutputStream(ivpath);
        ObjectOutputStream ivoos = new ObjectOutputStream(ivfos);
        ivoos.writeObject(iv);
        File keypath = new File(this.getExternalFilesDir(null), this.filename + "_key.key");
        FileOutputStream keyfos = new FileOutputStream(keypath);
        ObjectOutputStream keyoos = new ObjectOutputStream(keyfos);
//        byte[] keyb = secretKey.getEncoded();
        keyoos.writeObject(secretKey);
        ivfos.flush();
        ivfos.close();
        keyoos.flush();
        keyoos.close();

        for(int j=0; j<iv.length; j++){
            System.out.println(iv[j]);
        }
    }


    // Need to be written all over again similar to encrypt, but now decrypting. use the same key and iv.
    private final void decrypt() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        File path = new File(this.getExternalFilesDir(null), this.filename + "_encrypted"+ "." + fileExtension);
        FileInputStream fis = new FileInputStream(path);
        File path1 = new File(this.getExternalFilesDir(null), this.filename + "_decrypted"+ "." + fileExtension);
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

    private final void send() throws FileNotFoundException {
        File path = new File(this.getExternalFilesDir(null), this.filename + "_encrypted"+ "." + fileExtension);
        File ivPath = new File(this.getExternalFilesDir(null), this.filename + "_iv.key");
        File keyPath = new File(this.getExternalFilesDir(null), this.filename + "_key.key");
        BackgroundTask b1 = new BackgroundTask();
        b1.execute(path, ivPath, keyPath);
    }

    class BackgroundTask extends AsyncTask<File,Void,Void>{
        Socket s, s1, s2;
        Socket s3;
        ObjectOutputStream out;
        ObjectOutputStream out2;
        DataOutputStream out1;
        //DataOutputStream out3;
        //ObjectOutputStream out3;
        @Override
        protected Void doInBackground(File... f) {
            try{
                s = new Socket(ipAddress,port);
                s1 = new Socket(ipAddress,port+1);
                s2 = new Socket(ipAddress,port+2);
                s3 = new Socket(ipAddress, port+3);
                out = new ObjectOutputStream(s.getOutputStream());
                out2 = new ObjectOutputStream(s2.getOutputStream());
                out1 = new DataOutputStream(s1.getOutputStream());
                //out3 = new DataOutputStream(s3.getOutputStream());
               // out3 = new ObjectOutputStream(s3.getOutputStream());

                // THIS WRITES THE ENCRYPTED FILE AND SEND IT OUT TO SOCKET
                FileInputStream fis1 = new FileInputStream(f[0]);
                final byte[] bytearray = new byte[(int) f[0].length()];
                BufferedInputStream bis = new BufferedInputStream(fis1);
                bis.read(bytearray, 0, bytearray.length);
//                out.writeUTF(fullfilename);
//                out.writeObject(bytearray);
                Data data = new Data(fullfilename, secretKey, iv, bytearray);
                out.writeObject(data);
//                fis1.close();
                out.flush();
                out.close();
                s.close();

                // THIS WRITES IV AS BYTE ARRAY
                out1.writeInt(iv.length);
                out1.write(iv);
                out1.flush();
                out1.close();
                s1.close();

                // THIS WRITES KEY AS AN OBJECT
                out2.writeObject(secretKey);
                out2.close();
                s2.close();

                // THIS WRITES THE FILE NAME WITH EXTENSION
                /*out3.writeUTF(fullfilename);
                out3.flush();
                out3.close();
                s3.close();*/
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}