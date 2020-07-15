package com.example.spade_demo_in_java;

import java.io.File;
import java.io.Serializable;

import javax.crypto.SecretKey;

public class Data implements Serializable {
    private String filename;
    private SecretKey key;
    private byte[] iv;
    private byte[] file;

    public Data(){}

    public Data(String filename, SecretKey key, byte[] iv, byte[] file){
        this.filename = filename;
        this.key = key;
        this.iv = iv;
        this.file = file;
    }

    public String getFilename(){
        return this.filename;
    }

    public byte[] getIv(){
        return this.iv;
    }

    public byte[] getFile() {
        return file;
    }

    public SecretKey getKey() {
        return key;
    }

    public void setFile(byte[] file) {
        this.file = file;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setIv(byte[] iv) {
        this.iv = iv;
    }

    public void setKey(SecretKey key) {
        this.key = key;
    }
}
