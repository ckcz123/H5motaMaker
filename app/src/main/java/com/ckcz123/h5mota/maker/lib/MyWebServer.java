package com.ckcz123.h5mota.maker.lib;

import android.util.Base64;
import android.util.Log;

import com.ckcz123.h5mota.maker.MainActivity;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.SimpleWebServer;

/**
 * Created by oc on 2018/4/24.
 */

public class MyWebServer extends SimpleWebServer{

    public MyWebServer(String host, int port, File wwwroot, boolean quiet) {
        super(host, port, wwwroot, quiet);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String path = session.getUri();
        if (session.getMethod() == Method.POST && path.startsWith("/") && MainActivity.workingDirectory!=null) {

            if (path.startsWith("/readFile") || path.startsWith("/writeFile") || path.startsWith("/writeMultiFiles") || path.startsWith("/listFile")) {
                try {
                    HashMap<String, String> map = new HashMap<>();
                    // String content = Utils.readStream(session.getInputStream());
                    int length = Integer.parseInt(session.getHeaders().get("content-length"));
                    String content = new String(IOUtils.readFully(session.getInputStream(), length));
                    String[] params = content.split("&");
                    for (String param : params) {
                        int index = param.indexOf("=");
                        if (index >= 0)
                            map.put(param.substring(0, index), param.substring(index + 1));
                    }
                    if (path.startsWith("/readFile")) return readFile(map);
                    if (path.startsWith("/writeFile")) return writeFile(map);
                    if (path.startsWith("/writeMultiFiles")) return writeMultiFiles(map);
                    if (path.startsWith("/listFile")) return listFile(map);
                } catch (Exception e) {
                    e.printStackTrace();
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "");
                }
            }
        }

        return super.serve(session);
    }

    private Response readFile(HashMap<String, String> map) {
        try {
            String type = map.get("type");
            if (type==null || !type.equals("base64")) type = "utf8";
            String filename = map.get("name");
            File file = new File(MainActivity.makerDir, MainActivity.workingDirectory+"/"+filename);
            if (!file.exists())
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "");
            byte[] bytes = FileUtils.readFileToByteArray(file);
            String content = "base64".equals(type)?Base64.encodeToString(bytes, Base64.DEFAULT):
                    new String(bytes, "UTF-8");
            return newFixedLengthResponse(Response.Status.OK, "text/plain", content);
        }
        catch (Exception e) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "");
        }
    }

    private Response writeFile(HashMap<String, String> map) {
        try {
            String type = map.get("type");
            if (type==null || !type.equals("base64")) type = "utf8";
            String filename = map.get("name"), content = map.get("value");
            File file = new File(MainActivity.makerDir, MainActivity.workingDirectory+"/"+filename);
            byte[] bytes;
            if ("base64".equals(type))
                bytes = Base64.decode(content, Base64.DEFAULT);
            else
                bytes = content.getBytes("UTF-8");
            FileUtils.writeByteArrayToFile(file, bytes);
            return newFixedLengthResponse(Response.Status.OK, "text/plain", String.valueOf(bytes.length));
        }
        catch (Exception e) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "");
        }
    }

    private Response writeMultiFiles(HashMap<String, String> map) {
        try {
            String[] filenames = map.get("name").split(";"), contents = map.get("value").split(";");
            long length = 0;
            for (int i=0;i<filenames.length;i++) {
                String filename = filenames[i], content = contents[i];
                File file = new File(MainActivity.makerDir, MainActivity.workingDirectory+"/"+filename);
                byte[] bytes = Base64.decode(content, Base64.DEFAULT);
                FileUtils.writeByteArrayToFile(file, bytes);
                length += bytes.length;
            }
            return newFixedLengthResponse(Response.Status.OK, "text/plain", String.valueOf(length));
        }
        catch (Exception e) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "");
        }
    }

    private Response listFile(HashMap<String, String> map) {
        try {
            String filename = map.get("name");
            File file = new File(MainActivity.makerDir, MainActivity.workingDirectory+"/"+filename);
            File[] files = file.listFiles();
            StringBuilder builder = new StringBuilder("[");
            for (int i=0;i<files.length;i++) {
                builder.append('"').append(files[i].getName()).append('"');
                if (i!=files.length-1) builder.append(',');
            }
            builder.append(']');
            return newFixedLengthResponse(Response.Status.OK, "application/json", builder.toString());
        }
        catch (Exception e) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "");
        }

    }

}