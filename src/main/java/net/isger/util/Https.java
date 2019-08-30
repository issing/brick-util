package net.isger.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class Https {

    private Https() {
    }

    public static String post(String url, byte[] content, String encoding) {
        DataOutputStream out = null;
        BufferedReader reader = null;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("Accept-Charset", encoding);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            // 发送请求参数
            out = new DataOutputStream(conn.getOutputStream());
            out.write(content);
            out.flush();
            // 读取响应数据
            StringWriter writer = new StringWriter();
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
            }
            return writer.toString();
        } catch (Exception e) {
            throw Asserts.state("Failure to request url[%s]: %s", url, e.getMessage());
        } finally {
            Files.close(out);
            Files.close(reader);
        }
    }

    public static String get(String url, String encoding) {
        DataOutputStream out = null;
        BufferedReader reader = null;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestProperty("Accept-Charset", encoding);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestMethod("GET");
            conn.setDoOutput(false);
            conn.setUseCaches(false);
            // 读取响应数据
            StringWriter writer = new StringWriter();
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
            }
            return writer.toString();
        } catch (Exception e) {
            throw Asserts.state("Failure to request url[%s]: %s", url, e.getMessage());
        } finally {
            Files.close(out);
            Files.close(reader);
        }
    }

}
