package net.isger.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import net.isger.util.reflect.Converter;

public class Https {

    private static final String ENCODING = StandardCharsets.UTF_8.name();

    private Https() {
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

    public static HttpClientConnectionManager createManager(String version) {
        Registry<ConnectionSocketFactory> socketFactoryRegistry;
        try {
            socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create().register("http", PlainConnectionSocketFactory.INSTANCE).register("https", new SSLConnectionSocketFactory(createContext(version), NoopHostnameVerifier.INSTANCE)).build();
            return new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        } catch (Exception e) {
            throw Asserts.state("Failure to create http/https client connection manager", e);
        }
    }

    public static HttpClientBuilder createBuilder(String version) {
        return HttpClients.custom().setConnectionManager(createManager(version));
    }

    public static String post(String url) {
        return post(url, null, null, null);
    }

    public static String post(String url, String content) {
        return post(url, null, content, null);
    }

    public static String post(String url, Object content) {
        return post(url, null, Reflects.toMap(content));
    }

    public static String post(String url, Map<String, Object> content) {
        return post(url, null, content);
    }

    public static String post(String url, Map<String, String> headers, Object content) {
        return post(url, headers, Reflects.toMap(content));
    }

    public static String post(String url, Map<String, String> headers, Map<String, Object> content) {
        return post(url, headers, format(content), null);
    }

    public static String post(String url, Map<String, String> headers, String content, String token) {
        String result = null;
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = createBuilder("TLSv1.2").build();
            HttpPost post = new HttpPost(url);
            /* 设置报文头 */
            post.setHeader("Content-type", "application/x-www-form-urlencoded");
            if (Strings.isNotEmpty(token)) post.setHeader("Authorization", token);
            post.setHeader("User-Agent", "Brick/1.0.0");
            String encoding = ENCODING;
            if (headers != null) {
                for (Entry<String, String> header : headers.entrySet()) post.setHeader(header.getKey(), header.getValue());
                encoding = Strings.empty(headers.get("encoding"), encoding);
            }
            /* 设置报文体 */
            StringEntity requestEntity = new StringEntity(content, ContentType.APPLICATION_JSON.withCharset(encoding));
            post.setEntity(requestEntity);
            /* 发送请求（同步阻塞） */
            response = client.execute(post);
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) result = EntityUtils.toString(responseEntity);
            EntityUtils.consume(responseEntity);
        } catch (Exception e) {
            throw Asserts.state("Failure to request [%s]", url, e);
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

    public static String put(String url, Map<String, String> headers, String content) {
        return put(url, headers, content, null);
    }

    public static String put(String url, Map<String, String> headers, String content, String token) {
        String result = null;
        CloseableHttpClient client = null;
        CloseableHttpResponse response = null;
        try {
            client = createBuilder("TLSv1.2").build();
            HttpPut put = new HttpPut(url);
            /* 设置报文头 */
            put.setHeader("Content-type", "application/x-www-form-urlencoded");
            if (Strings.isNotEmpty(token)) put.setHeader("Authorization", token);
            put.setHeader("User-Agent", "Brick/1.0.0");
            String encoding = ENCODING;
            if (headers != null) {
                for (Entry<String, String> header : headers.entrySet()) put.setHeader(header.getKey(), header.getValue());
                encoding = Strings.empty(headers.get("encoding"), encoding);
            }
            /* 设置报文体 */
            StringEntity requestEntity = new StringEntity(content, ContentType.APPLICATION_JSON.withCharset(encoding));
            put.setEntity(requestEntity);
            /* 发送请求（同步阻塞） */
            response = client.execute(put);
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) result = EntityUtils.toString(responseEntity);
            EntityUtils.consume(responseEntity);
        } catch (Exception e) {
            throw Asserts.state("Failure to request [%s]", url, e);
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
            client = createBuilder("TLSv1.2").build();
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
                result = EntityUtils.toString(responseEntity);
            }
            EntityUtils.consume(responseEntity);
        } catch (Exception e) {
            throw Asserts.state("Failure to request [%s]", url, e);
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
            if (Strings.isEmpty(key = Strings.empty(entry.getKey())) || Strings.isEmpty(value = new String(encoder.encode(entry.getValue())))) continue;
            buffer.append(key).append("=").append(value).append("&");
        }
        if (buffer.length() > 0) buffer.setLength(buffer.length() - 1);
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
