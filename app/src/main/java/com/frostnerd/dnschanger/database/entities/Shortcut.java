package com.frostnerd.dnschanger.database.entities;

import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class Shortcut implements Serializable {
    private ArrayList<IPPortPair> servers;
    private final String name;

    public Shortcut(String name, ArrayList<IPPortPair> servers) {
        this.name = name;
        this.servers = servers;
    }

    public ArrayList<IPPortPair> getServers() {
        return servers;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(
                    new Base64OutputStream(baos, Base64.NO_PADDING
                            | Base64.NO_WRAP));
            oos.writeObject(this);
            oos.close();
            return baos.toString("UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Shortcut fromString(String s) {
        try {
            return (Shortcut) new ObjectInputStream(new Base64InputStream(
                    new ByteArrayInputStream(s.getBytes()), Base64.NO_PADDING
                    | Base64.NO_WRAP)).readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}