package net.isger.util;

import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
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

import net.isger.util.reflect.Converter;

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
            throw Asserts.state("Failure to create http/https client connection manager - {}", e.getMessage(), e.getCause());
        }
    }

    public static HttpClientBuilder createBuilder() {
        return HttpClients.custom().setConnectionManager(createManager());
    }

    public static String post(String url) {
        return post(url, null, null, null);
    }

    public static String post(String url, Object content) {
        return post(url, Reflects.toMap(content));
    }

    public static String post(String url, Map<String, Object> content) {
        return post(url, format(content));
    }

    public static String post(String url, String content) {
        return post(url, null, content, null);
    }

    public static String post(String url, Map<String, String> headers, String content, String token) {
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
            StringEntity requestEntity = new StringEntity(content);
            requestEntity.setContentType("application/json");
            post.setEntity(requestEntity);
            /* 发送请求（同步阻塞） */
            response = client.execute(post);
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                result = EntityUtils.toString(responseEntity);
            }
            EntityUtils.consume(responseEntity);
        } catch (Exception e) {
            throw Asserts.state("Failure to request [%s] - %s", url, e.getMessage(), e.getCause());
        } finally {
            Files.close(response);
            Files.close(client);
        }
        return result;
    }

    public static String put(String url) {
        return put(url, null, null, null);
    }

    public static String put(String url, String content) {
        return put(url, null, content, null);
    }

    public static String put(String url, Map<String, String> headers, String content, String token) {
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
            StringEntity requestEntity = new StringEntity(content);
            requestEntity.setContentType("application/json");
            put.setEntity(requestEntity);
            /* 发送请求（同步阻塞） */
            response = client.execute(put);
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                result = EntityUtils.toString(responseEntity);
            }
            EntityUtils.consume(responseEntity);
        } catch (Exception e) {
            throw Asserts.state("Failure to request [%s] - %s", url, e.getMessage(), e.getCause());
        } finally {
            Files.close(response);
            Files.close(client);
        }
        return result;
    }

    public static String get(String url) {
        return get(url, null, null, null);
    }

    public static String get(String url, String content) {
        return get(url, null, content, null);
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
            throw Asserts.state("Failure to request [%s] - %s", url, e.getMessage(), e.getCause());
        } finally {
            Files.close(response);
            Files.close(client);
        }
        return result;
    }

    public static String format(Map<String, Object> parameters) {
        return format(parameters, null);
    }

    public static String format(Map<String, Object> parameters, Encoder encoder) {
        if (encoder == null) {
            encoder = new Encoder() {
                public byte[] encode(Object content) {
                    return Strings.empty(content == null ? null : Converter.convert(String.class, content)).getBytes();
                }
            };
        }
        StringBuffer buffer = new StringBuffer(512);
        String key;
        String value;
        for (Entry<String, Object> entry : Helpers.sortByKey(parameters)) {
            if (Strings.isEmpty(key = Strings.empty(entry.getKey())) || Strings.isEmpty(value = new String(encoder.encode(entry.getValue())))) {
                continue;
            }
            buffer.append(key).append("=").append(value).append("&");
        }
        if (buffer.length() > 0) {
            buffer.setLength(buffer.length() - 1);
        }
        return buffer.toString();
    }

    public static Map<String, Object> parse(String content) {
        return parse(content, null);
    }

    public static Map<String, Object> parse(String content, Decoder decoder) {
        if (decoder == null) {
            decoder = new Decoder() {
                public Object decode(InputStream is) {
                    return null;
                }

                public Object decode(byte[] content) {
                    return new String(content);
                }
            };
        }
        Map<String, Object> parameters = new HashMap<String, Object>();
        for (String parameter : content.split("[&]")) {
            String[] entry = parameter.split("[=]", 2);
            parameters.put(entry[0], decoder.decode(entry[1].getBytes()));
        }
        return parameters;
    }

}
