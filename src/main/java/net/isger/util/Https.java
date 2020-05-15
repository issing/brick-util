package net.isger.util;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

public class Https {

    private Https() {
    }

    public static SSLContext createContext() throws NoSuchAlgorithmException, KeyManagementException {
        return createContext("SSLv3");
    }

    public static SSLContext createContext(String version) throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext context = SSLContext.getInstance(version);
        /* 实现X509TrustManager接口（用于绕过验证） */
        X509TrustManager trustManager = new X509TrustManager() {
            public void checkClientTrusted(java.security.cert.X509Certificate[] paramArrayOfX509Certificate, String paramString) throws CertificateException {
            }

            public void checkServerTrusted(java.security.cert.X509Certificate[] paramArrayOfX509Certificate, String paramString) throws CertificateException {
            }

            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };
        context.init(null, new TrustManager[] { trustManager }, null);
        return context;
    }

    public static HttpClientConnectionManager createManager() {
        Registry<ConnectionSocketFactory> socketFactoryRegistry;
        try {
            socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create().register("http", PlainConnectionSocketFactory.INSTANCE).register("https", new SSLConnectionSocketFactory(createContext(), NoopHostnameVerifier.INSTANCE)).build();
            return new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        } catch (Exception e) {
            throw Asserts.state("Failure to create http/https client connection manager: {}", e.getMessage(), e.getCause());
        }
    }

    public static HttpClientBuilder createBuilder() {
        return HttpClients.custom().setConnectionManager(createManager());
    }

    public static String post(String url, byte[] content, String encoding) {
        return post(url, null, content, encoding, null);
    }

    public static String post(String url, Map<String, String> headers, byte[] content, String encoding, String token) {
        String result = null;
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = createBuilder().build();
            HttpPost post = new HttpPost(url);
            /* 设置报文头 */
            post.setHeader("Content-type", "application/x-www-form-urlencoded");
            if (Strings.isNotEmpty(token)) {
                post.setHeader("Authorization", token);
            }
            post.setHeader("User-Agent", "Brick/1.0.0");
            if (headers != null) {
                for (Entry<String, String> header : headers.entrySet()) {
                    post.setHeader(header.getKey(), header.getValue());
                }
            }
            /* 设置报文体 */
            StringEntity requestEntity = new StringEntity(new String(content, encoding));
            requestEntity.setContentType("application/json");
            post.setEntity(requestEntity);
            /* 发送请求（同步阻塞） */
            response = client.execute(post);
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                result = EntityUtils.toString(responseEntity, encoding);
            }
            EntityUtils.consume(responseEntity);
        } catch (Exception e) {
            throw Asserts.state("Failure to request [%s]: %s", url, e.getMessage(), e.getCause());
        } finally {
            Files.close(response);
            Files.close(client);
        }
        return result;
    }

    public static String put(String url, Map<String, String> headers, byte[] content, String encoding, String token) {
        String result = null;
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = createBuilder().build();
            HttpPut put = new HttpPut(url);
            /* 设置报文头 */
            put.setHeader("Content-type", "application/x-www-form-urlencoded");
            if (Strings.isNotEmpty(token)) {
                put.setHeader("Authorization", token);
            }
            put.setHeader("User-Agent", "Brick/1.0.0");
            if (headers != null) {
                for (Entry<String, String> header : headers.entrySet()) {
                    put.setHeader(header.getKey(), header.getValue());
                }
            }
            /* 设置报文体 */
            StringEntity requestEntity = new StringEntity(new String(content, encoding));
            requestEntity.setContentType("application/json");
            put.setEntity(requestEntity);
            /* 发送请求（同步阻塞） */
            response = client.execute(put);
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                result = EntityUtils.toString(responseEntity, encoding);
            }
            EntityUtils.consume(responseEntity);
        } catch (Exception e) {
            throw Asserts.state("Failure to request [%s]: %s", url, e.getMessage(), e.getCause());
        } finally {
            Files.close(response);
            Files.close(client);
        }
        return result;
    }

    public static String get(String url, Map<String, String> headers, String content, String token) {
        String result = null;
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = createBuilder().build();
            HttpGet get = new HttpGet(url + "?" + content);
            /* 设置报文头 */
            get.setHeader("Content-type", "application/x-www-form-urlencoded");
            if (Strings.isNotEmpty(token)) {
                get.setHeader("Authorization", token);
            }
            get.setHeader("User-Agent", "Brick/1.0.0");
            if (headers != null) {
                for (Entry<String, String> header : headers.entrySet()) {
                    get.setHeader(header.getKey(), header.getValue());
                }
            }
            /* 发送请求（同步阻塞） */
            response = client.execute(get);
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                result = EntityUtils.toString(responseEntity, "UTF-8");
            }
            EntityUtils.consume(responseEntity);
        } catch (Exception e) {
            throw Asserts.state("Failure to request [%s]: %s", url, e.getMessage(), e.getCause());
        } finally {
            Files.close(response);
            Files.close(client);
        }
        return result;
    }

}
