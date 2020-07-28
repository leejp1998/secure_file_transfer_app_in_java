package com.example.spade_demo_in_java;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import data.Data;

public class MainActivity extends AppCompatActivity {

    final int REQUEST_SAF = 1337;
    private Uri tempURI;
    @NotNull
    private String filename, filename1, filepath, fileExtension, fullfilename;
    @NotNull
    private byte[] iv = new byte[12];
    private byte[] iv1 = new byte[12];
    private KeyGenerator generator = KeyGenerator.getInstance("AES");
    private SecretKey secretKey, secretKey1;
    private Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    String ipAddress = "192.168.1.195"; // POC wifi= 172.30.76.58  POC my office LAN = 172.30.0.14  ubuntu = 192.168.56.101
    // under POC Employee wifi    Lab computer 172.16.0.6
    // 192.168.56.1, 172.30.76.58, 172.30.76.1, 192.168.1.123 doesnt work
    // Lab setup: "172.16.0.11"

    // For ipAddress, debug server java file line 49 InetAddress inet = InetAddress.getLocalHost() and use that value
    Socket socket;
    ServerSocket serverSocket;
    int port = 6000;
    int port1 = 6001;
    ServerSocketThread serverSocketThread;

    File encrypted_received = null;
    File decrypted_received = null;
    File encrypted_send = null;
    File copy_send = null;

    public MainActivity() throws NoSuchAlgorithmException, NoSuchPaddingException {
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView ipAddressTextView = (TextView) findViewById(R.id.ipAddressTextView);
        Button loadButton = (Button)findViewById(R.id.load_button);
        Button executeButton = (Button)findViewById(R.id.execute_button);
        Spinner dataTypeDropdown = (Spinner)findViewById(R.id.data_type_spinner);
        //Button decryptButton = (Button)findViewById(R.id.decrypt_button);
        Button sendButton = (Button) findViewById(R.id.send_button);
        Button receiveButton = (Button) findViewById(R.id.receive_button);
        ArrayAdapter arrayAdapter = ArrayAdapter.createFromResource((Context)this, R.array.data_types, android.R.layout.simple_spinner_item);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dataTypeDropdown.setAdapter((SpinnerAdapter)arrayAdapter);

        ipAddressTextView.setText(getIpAddress());

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
                } catch (InvalidAlgorithmParameterException e) {
                    e.printStackTrace();
                }
            }
        }));
       /* decryptButton.setOnClickListener((View.OnClickListener)(new View.OnClickListener() {
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
        }));*/
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

    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();
                    if (inetAddress.isSiteLocalAddress()) {
                        ip += "SiteLocalAddress: " + inetAddress.getHostAddress() + "\n";
                    }

                }

            }
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ip;
    }
    public class ServerSocketThread extends Thread {

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(port1);
                while (true) {
                    socket = serverSocket.accept();
                    FileTxThread fileTxThread = new FileTxThread(socket);
                    fileTxThread.start();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    public class FileTxThread extends Thread {
        Socket s;
        ObjectInputStream ois;
        FileOutputStream fos;

        FileTxThread(Socket socket){
            this.s= socket;
        }

        @Override
        public void run() {
            encrypted_received = new File(getExternalFilesDir(null),"encrypted_" + filename1);

            byte[] bytes = new byte[(int) encrypted_received.length()];
            try {
                ois = new ObjectInputStream(socket.getInputStream());
                fos = new FileOutputStream(encrypted_received);
                BufferedOutputStream Bos = new BufferedOutputStream(fos);
                Data data = new Data();
                data = (Data) ois.readObject();
                filename1 = data.getFilename();
                byte[] mybytearray = data.getFile();
                iv1 = data.getIv();
                secretKey1 = data.getKey();

                Bos.write(mybytearray);
                Bos.flush();
                fos.flush();
                //s.close();

                FileInputStream fis = new FileInputStream(encrypted_received);
                decrypted_received = new File(getExternalFilesDir(null), "decrypted_" + filename1);
                FileOutputStream fos = new FileOutputStream(decrypted_received, false);

                GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv1, 0, 12);
                cipher.init(Cipher.DECRYPT_MODE, (Key) secretKey1, gcmParameterSpec);
                CipherInputStream cis = new CipherInputStream(fis, cipher);

                int byteRead = 0;
                byte[] plainText = new byte[8192];
                while((byteRead = cis.read(plainText)) >= 0){
                    fos.write(plainText, 0, byteRead);
                }
                fos.flush();
                fos.close();
                cis.close();

                //Remove encrypted file
                encrypted_received.delete();
                //Toast.makeText(this, "File is decrypted.", Toast.LENGTH_SHORT).show();

                final String sentMsg = "File received and decrypted from: " + s.getInetAddress();
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                                sentMsg,
                                Toast.LENGTH_LONG).show();
                    }});

            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InvalidAlgorithmParameterException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }
    }

    private void receive() {
        serverSocketThread = new ServerSocketThread();
        serverSocketThread.start();
    }

   /* class BackgroundTask1 extends AsyncTask<Void,Void,Void>{
        ObjectInputStream ois;
        FileOutputStream fos;

        @Override
        protected Void doInBackground(Void... voids) {
            try{
                if(serverSocket == null){
                    serverSocket = new ServerSocket(port1);
                    serverSocket.setSoTimeout(15000);
                }} catch (IOException ex) {
                ex.printStackTrace();
            }
            try {
                socket = serverSocket.accept();
                String ipAddress = socket.getLocalAddress().toString();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            try{
                ois = new ObjectInputStream(socket.getInputStream());
                Data data = new Data();
                data = (Data) ois.readObject();
                filename1 = data.getFilename();
                fos = new FileOutputStream(new File(getExternalFilesDir(null),"encrypted_" + filename1));

                final BufferedOutputStream Bos = new BufferedOutputStream(fos);
                byte[] mybytearray = data.getFile();
                Bos.write(mybytearray);
                Bos.flush();

                iv1 = data.getIv();
                secretKey1 = data.getKey();

                serverSocket.close();
                socket.close();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }
    }*/

    // decrypt the received file
    // dont need this anymore
    private final void decrypt() throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        File path = new File(this.getExternalFilesDir(null), "encrypted_" + this.filename1);
        FileInputStream fis = new FileInputStream(path);
        File path1 = new File(this.getExternalFilesDir(null), "decrypted_" + this.filename1);
        FileOutputStream fos = new FileOutputStream(path1, false);

        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv1, 0, 12);
        cipher.init(Cipher.DECRYPT_MODE, (Key) secretKey1, gcmParameterSpec);
        CipherInputStream cis = new CipherInputStream(fis, cipher);

        int byteRead = 0;
        byte[] plainText = new byte[8192];
        while((byteRead = cis.read(plainText)) >= 0){
            fos.write(plainText, 0, byteRead);
        }
        fos.flush();
        fos.close();
        cis.close();
        Toast.makeText(this, "File is decrypted.", Toast.LENGTH_SHORT).show();
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
    
    private void execute() throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, InvalidAlgorithmParameterException {
        if(tempURI == null){
            Toast toast = Toast.makeText(getApplicationContext(), "No file is selected", Toast.LENGTH_SHORT);
            //toast.setMargin(50,50);
            toast.show();
        }
        else {
            copy_send = new File(getExternalFilesDir(null), filename + "_copied" + "." + fileExtension);
            InputStream inputStream = getContentResolver().openInputStream(tempURI);
            copyStreamToFile(inputStream, copy_send);

            encrypt(copy_send);
            copy_send.delete(); // For security, copied file is deleted. Not sure if this is secure enough.
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

    private final void encrypt(File copiedFile) throws IOException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        ActivityCompat.requestPermissions((Activity)this, new String[]{"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"}, 200);
        encrypted_send = new File(this.getExternalFilesDir((String)null), this.filename + "_encrypted"+ "." + fileExtension);
        FileInputStream fis = new FileInputStream(copiedFile);
        FileOutputStream fos = new FileOutputStream(encrypted_send, false);
        this.generator.init(128);
        this.secretKey = this.generator.generateKey();
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, (Key) this.secretKey, new GCMParameterSpec(128, iv, 0, 12));
        CipherOutputStream output = new CipherOutputStream((FileOutputStream)fos, this.cipher);
        int bytesRead = 0;
        byte[] plainText = new byte[8192];

        while((bytesRead = fis.read(plainText)) >= 0) {
            output.write(plainText, 0, bytesRead);
        }
        output.flush();
        output.close();
        fos.close();
        fis.close();
    }




    private final void send() throws FileNotFoundException {
        BackgroundTask b1 = new BackgroundTask();
        b1.execute(encrypted_send);
    }

    class BackgroundTask extends AsyncTask<File,Void,Void>{
        Socket s;
        ObjectOutputStream out;

        @Override
        protected Void doInBackground(File... f) {
            try{
                s = new Socket(ipAddress,port);
                out = new ObjectOutputStream(s.getOutputStream());

                // THIS WRITES THE ENCRYPTED FILE AND SEND IT OUT TO SOCKET
                FileInputStream fis1 = new FileInputStream(f[0]);
                final byte[] bytearray = new byte[(int) f[0].length()];
                BufferedInputStream bis = new BufferedInputStream(fis1);
                bis.read(bytearray, 0, bytearray.length);
                Data data = new Data(fullfilename, secretKey, iv, bytearray);
                out.writeObject(data);
                out.flush();
                out.close();
                s.close();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}