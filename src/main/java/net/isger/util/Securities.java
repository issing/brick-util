package net.isger.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.KeySpec;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;
import javax.security.auth.x500.X500PrivateCredential;

import org.apache.commons.codec.binary.Base32;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class Securities {

    private static final int WINDOW_SIZE = 1;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private Securities() {
    }

    /**
     * 创建秘钥套件库
     * 
     * @param type
     * @param root
     * @param intermediate
     * @param entity
     * @param password
     * @param algorithm
     * @param issuer
     * @param period
     * @return
     * @throws Exception
     */
    public static KeyStore createSuite(String type, String root, String intermediate, String entity, char[] password, String algorithm, String issuer, long period) throws Exception {
        /* 创建密钥库 */
        KeyStore store = KeyStore.getInstance(type);
        store.load(null, null);
        /* 创建根凭证 */
        KeyPair rkp = createKeyPair(algorithm);
        X500PrivateCredential rootCredential = createCredential(root, rkp.getPrivate(), createRoot("CN=" + root + "," + issuer, period, 1, rkp));
        /* 创建中级凭证 */
        KeyPair ikp = createKeyPair(algorithm);
        X500PrivateCredential interCredential = createCredential(intermediate, ikp.getPrivate(), createIntermediate("CN=" + intermediate + "," + issuer, period, 1, ikp.getPublic(), rootCredential.getPrivateKey(), rootCredential.getCertificate()));
        /* 创建实体凭证 */
        KeyPair ekp = createKeyPair(algorithm);
        X500PrivateCredential entityCredential = createCredential(entity, ekp.getPrivate(), createEntity("CN=" + entity + "," + issuer, period, 1, ekp.getPublic(), interCredential.getPrivateKey(), interCredential.getCertificate()));
        /* 设置凭证入口 */
        store.setCertificateEntry(rootCredential.getAlias(), rootCredential.getCertificate());
        /* 设置秘钥入口 */
        store.setKeyEntry(entityCredential.getAlias(), entityCredential.getPrivateKey(), password, new Certificate[] { entityCredential.getCertificate(), interCredential.getCertificate(), rootCredential.getCertificate() });
        return store;
    }

    /**
     * 创建证书库
     * 
     * @param name
     * @param password
     * @param issuer
     * @param period
     * @return
     * @throws Exception
     */
    public static KeyStore createKeyStore(String name, String password, String issuer, long period) throws Exception {
        return createKeyStore(name, password, issuer, period, createKeyPair());
    }

    /**
     * 创建证书库
     * 
     * @param algorithm
     * @param name
     * @param password
     * @param issuer
     * @param period
     * @return
     * @throws Exception
     */
    public static KeyStore createKeyStore(String algorithm, String name, String password, String issuer, long period) throws Exception {
        return createKeyStore(algorithm, name, password, issuer, period, createKeyPair());
    }

    /**
     * 创建证书库
     * 
     * @param name
     * @param password
     * @param issuer
     * @param period
     * @param keyPair
     * @return
     * @throws Exception
     */
    public static KeyStore createKeyStore(String name, String password, String issuer, long period, KeyPair keyPair) throws Exception {
        return createKeyStore(name, password, keyPair.getPrivate(), createRoot(issuer, period, 1, keyPair));
    }

    /**
     * 创建证书库
     * 
     * @param algorithm
     * @param name
     * @param password
     * @param issuer
     * @param period
     * @param keyPair
     * @return
     * @throws Exception
     */
    public static KeyStore createKeyStore(String algorithm, String name, String password, String issuer, long period, KeyPair keyPair) throws Exception {
        return createKeyStore(algorithm, name, password, keyPair.getPrivate(), createRoot(issuer, period, 1, keyPair));
    }

    /**
     * 创建证书库
     * 
     * @param name
     * @param password
     * @param key
     * @param certificates
     * @return
     * @throws Exception
     */
    public static KeyStore createKeyStore(String name, String password, Key key, Certificate... certificates) throws Exception {
        return createKeyStore("PKCS12", name, password, key, certificates);
    }

    /**
     * 创建证书库
     * 
     * @param algorithm
     * @param name
     * @param password
     * @param key
     * @param certificates
     * @return
     * @throws Exception
     */
    public static KeyStore createKeyStore(String algorithm, String name, String password, Key key, Certificate... certificates) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(algorithm);
        keyStore.load(null, null);
        keyStore.setKeyEntry(name, key, Strings.isEmpty(password) ? null : password.toCharArray(), certificates);
        return keyStore;
    }

    /**
     * 获取证书库
     * 
     * @param algorithm
     * @param path
     * @param password
     * @return
     * @throws Exception
     */
    public static KeyStore getKeyStore(String algorithm, String path, char[] password) throws Exception {
        return getKeyStore(algorithm, Reflects.getResourceAsStream(path), password);
    }

    /**
     * 获取证书库
     * 
     * @param algorithm
     * @param data
     * @param password
     * @return
     * @throws Exception
     */
    public static KeyStore getKeyStore(String algorithm, byte[] data, char[] password) throws Exception {
        return getKeyStore(algorithm, new ByteArrayInputStream(data), password);
    }

    /**
     * 获取证书库
     * 
     * @param algorithm
     * @param stream
     * @param password
     * @return
     * @throws Exception
     */
    public static KeyStore getKeyStore(String algorithm, InputStream stream, char[] password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(algorithm);
        try {
            keyStore.load(stream, password);
        } finally {
            Files.close(stream);
        }
        return keyStore;
    }

    /**
     * 创建凭证
     * 
     * @param name
     * @param key
     * @param cert
     * @return
     */
    public static X500PrivateCredential createCredential(String name, PrivateKey key, X509Certificate cert) {
        return new X500PrivateCredential(cert, key, name);
    }

    /**
     * 创建根证书（JCA）
     * 
     * @param issuer
     * @param period
     * @param keyPair
     * @return
     * @throws Exception
     */
    public static X509Certificate createRoot(String issuer, long period, long serial, KeyPair keyPair) throws Exception {
        Date notBefore = new Date();
        X509v1CertificateBuilder builder = new JcaX509v1CertificateBuilder(new X500Name(issuer), BigInteger.valueOf(serial), notBefore, Dates.getDate(notBefore, period), new X500Name(issuer), keyPair.getPublic());
        return getCertificate(builder.build(createSigner(keyPair.getPrivate())));
    }

    /**
     * 创建根证书
     * 
     * @param issuer
     * @param period
     * @param keyPair
     * @return
     * @throws Exception
     */
    public static Certificate createRoot(String issuer, long period, AsymmetricCipherKeyPair keyPair) throws Exception {
        Date notBefore = new Date();
        X509v1CertificateBuilder builder = new X509v1CertificateBuilder(new X500Name(issuer), BigInteger.valueOf(1), notBefore, Dates.getDate(notBefore, period), new X500Name(issuer), SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(keyPair.getPublic()));
        return getCertificate(builder.build(createSigner(keyPair)).toASN1Structure().getEncoded());
    }

    /**
     * 创建中级证书
     * 
     * @param subject
     * @param period
     * @param serial
     * @param inKey
     * @param caKey
     * @param caCert
     * @return
     * @throws Exception
     */
    public static X509Certificate createIntermediate(String subject, long period, long serial, PublicKey inKey, PrivateKey caKey, X509Certificate caCert) throws Exception {
        Date notBefore = new Date();
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(caCert.getSubjectX500Principal(), BigInteger.valueOf(serial), notBefore, Dates.getDate(notBefore, period), new X500Principal(subject), inKey);
        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
        builder.addExtension(Extension.authorityKeyIdentifier, false, extUtils.createAuthorityKeyIdentifier(caCert)).addExtension(Extension.subjectKeyIdentifier, false, extUtils.createSubjectKeyIdentifier(inKey)).addExtension(Extension.basicConstraints, true, new BasicConstraints(0)).addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyCertSign | KeyUsage.cRLSign));
        return getCertificate(builder.build(createSigner(caKey)));
    }

    /**
     * 创建实体证书
     * 
     * @param subject
     * @param period
     * @param entityKey
     * @param caKey
     * @param caCert
     * @param extensions
     * @return
     * @throws Exception
     */
    public static X509Certificate createEntity(String subject, long period, long serail, PublicKey entityKey, PrivateKey caKey, X509Certificate caCert, Extension... extensions) throws Exception {
        Date notBefore = new Date();
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(caCert.getSubjectX500Principal(), BigInteger.valueOf(serail), notBefore, Dates.getDate(notBefore, period), new X500Principal(subject), entityKey);
        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
        builder.addExtension(Extension.authorityKeyIdentifier, false, extUtils.createAuthorityKeyIdentifier(caCert)).addExtension(Extension.subjectKeyIdentifier, false, extUtils.createSubjectKeyIdentifier(entityKey)).addExtension(Extension.basicConstraints, true, new BasicConstraints(false)).addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        return getCertificate(builder.build(createSigner(caKey)));
    }

    /**
     * 创建证书
     * 
     * @param holder
     * @return
     * @throws Exception
     */
    public static X509Certificate getCertificate(X509CertificateHolder holder) throws Exception {
        return new JcaX509CertificateConverter().getCertificate(holder);
    }

    /**
     * 创建证书
     * 
     * @param data
     * @return
     * @throws Exception
     */
    public static X509Certificate getCertificate(byte[] data) throws Exception {
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(data));
    }

    /**
     * 获取证书
     * 
     * @param path
     * @return
     * @throws Exception
     */
    public static Certificate getCertificate(String path) throws Exception {
        InputStream is = Reflects.getResourceAsStream(path);
        try {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        } finally {
            Files.close(is);
        }
    }

    /**
     * 获取私钥
     * 
     * @param store
     * @param name
     * @param password
     * @return
     * @throws Exception
     */
    public static PrivateKey getPrivateKey(KeyStore store, String name, String password) throws Exception {
        return (PrivateKey) store.getKey(name, Strings.isEmpty(password) ? null : password.toCharArray());
    }

    /**
     * 获取公钥
     * 
     * @param certificate
     * @return
     * @throws Exception
     */
    public static PublicKey getPublicKey(Certificate certificate) throws Exception {
        return certificate.getPublicKey();
    }

    /**
     * 创建公钥
     * 
     * @param algorithm
     * @param keySpec
     * @return
     * @throws Exception
     */
    public static PublicKey createPublicKey(String algorithm, KeySpec keySpec) throws Exception {
        return KeyFactory.getInstance(algorithm).generatePublic(keySpec);
    }

    /**
     * 创建私钥
     * 
     * @param algorithm
     * @param keySpec
     * @return
     * @throws Exception
     */
    public static PrivateKey createPrivateKey(String algorithm, KeySpec keySpec) throws Exception {
        return KeyFactory.getInstance(algorithm).generatePrivate(keySpec);
    }

    /**
     * 创建秘钥
     * 
     * @return
     * @throws Exception
     */
    public static SecretKey createSecretKey() throws Exception {
        return KeyGenerator.getInstance("HmacMD5").generateKey();
    }

    /**
     * 创建秘钥
     * 
     * @param algorithm
     * @return
     * @throws Exception
     */
    public static SecretKey createSecretKey(String algorithm) throws Exception {
        return KeyGenerator.getInstance(algorithm).generateKey();
    }

    /**
     * 创建秘钥
     * 
     * @param data
     * @return
     * @throws Exception
     */
    public static SecretKey createSecretKey(byte[] data) throws Exception {
        return createSecretKey("HmacMD5", data);
    }

    /**
     * 创建秘钥
     * 
     * @param algorithm
     * @param data
     * @return
     * @throws Exception
     */
    public static SecretKey createSecretKey(String algorithm, byte[] data) throws Exception {
        return new SecretKeySpec(data, algorithm);
    }

    /**
     * 创建秘钥对
     * 
     * @return
     * @throws Exception
     */
    public static KeyPair createKeyPair() throws Exception {
        return createKeyPair("RSA", 2048);
    }

    /**
     * 创建秘钥对
     * 
     * @param algorithm
     * @return
     * @throws Exception
     */
    public static KeyPair createKeyPair(String algorithm) throws Exception {
        return createKeyPair(algorithm, 2048);
    }

    /**
     * 创建秘钥对
     * 
     * @param algorithm
     * @param keySize
     * @return
     * @throws Exception
     */
    public static KeyPair createKeyPair(String algorithm, int keySize) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm); // "BC");
        kpg.initialize(keySize, new SecureRandom());
        return kpg.generateKeyPair();
    }

    /**
     * 创建非对称秘钥对
     * 
     * @return
     */
    public static AsymmetricCipherKeyPair createACKeyPair() {
        RSAKeyPairGenerator kpg = new RSAKeyPairGenerator();
        RSAKeyGenerationParameters param = new RSAKeyGenerationParameters(BigInteger.valueOf(3), new SecureRandom(), 2048, 24);
        kpg.init(param);
        return kpg.generateKeyPair();
    }

    /**
     * 创建签名
     * 
     * @param keyPair
     * @return
     */
    public static ContentSigner createSigner(AsymmetricCipherKeyPair keyPair) throws Exception {
        return createSigner("SHA1withRSA", keyPair);
    }

    /**
     * 创建签名
     * 
     * @param algorithm
     * @param keyPair
     * @return
     * @throws Exception
     */
    public static ContentSigner createSigner(String algorithm, AsymmetricCipherKeyPair keyPair) throws Exception {
        AlgorithmIdentifier sigAlg = new DefaultDigestAlgorithmIdentifierFinder().find(algorithm);
        AlgorithmIdentifier digAlg = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlg);
        return new BcRSAContentSignerBuilder(sigAlg, digAlg).build(keyPair.getPrivate());
    }

    /**
     * 创建签名
     * 
     * @param key
     * @return
     * @throws Exception
     */
    public static ContentSigner createSigner(PrivateKey key) throws Exception {
        return createSigner("SHA1withRSA", key);
    }

    /**
     * 创建签名
     * 
     * @param algorithm
     * @param key
     * @return
     * @throws Exception
     */
    public static ContentSigner createSigner(String algorithm, PrivateKey key) throws Exception {
        return new JcaContentSignerBuilder(algorithm)// .setProvider("BC")
                .build(key);
    }

    /**
     * 公钥加密
     * 
     * @param certificate
     * @param data
     * @return
     * @throws Exception
     */
    public static byte[] toEncrypt(Certificate certificate, byte[] data) throws Exception {
        return toEncrypt(getPublicKey(certificate), data);
    }

    /**
     * 公钥解密
     * 
     * @param data
     * @param certificatePath
     * @return
     * @throws Exception
     */
    public static byte[] toDecrypt(Certificate certificate, byte[] data) throws Exception {
        return toDecrypt(getPublicKey(certificate), data);
    }

    /**
     * 私钥加密
     * 
     * @param keyStore
     * @param name
     * @param data
     * @return
     * @throws Exception
     */
    public static byte[] toEncrypt(KeyStore keyStore, String name, byte[] data) throws Exception {
        return toEncrypt(getPrivateKey(keyStore, name, null), data);
    }

    /**
     * 私钥加密
     * 
     * @param keyStore
     * @param name
     * @param password
     * @param data
     * @return
     * @throws Exception
     */
    public static byte[] toEncrypt(KeyStore keyStore, String name, String password, byte[] data) throws Exception {
        return toEncrypt(getPrivateKey(keyStore, name, password), data);
    }

    /**
     * 私钥解密
     * 
     * @param keyStore
     * @param name
     * @param password
     * @param data
     * @return
     * @throws Exception
     */
    public static byte[] toDecrypt(KeyStore keyStore, String name, String password, byte[] data) throws Exception {
        return toDecrypt(getPrivateKey(keyStore, name, password), data);
    }

    /**
     * 数据加密
     * 
     * @param key
     * @param data
     * @return
     * @throws Exception
     */
    public static byte[] toEncrypt(Key key, byte[] data) throws Exception {
        return toEncrypt(key, data, null);
    }

    /**
     * 数据加密
     *
     * @param key
     * @param data
     * @param initializer
     * @return
     * @throws Exception
     */
    public static byte[] toEncrypt(Key key, byte[] data, Callable<Void> initializer) throws Exception {
        return toCipher(Cipher.ENCRYPT_MODE, key, data, initializer);
    }

    /**
     * 数据解密
     * 
     * @param key
     * @param data
     * @return
     * @throws Exception
     */
    public static byte[] toDecrypt(Key key, byte[] data) throws Exception {
        return toDecrypt(key, data, null);
    }

    /**
     * 数据解密
     *
     * @param key
     * @param data
     * @param initializer
     * @return
     * @throws Exception
     */
    public static byte[] toDecrypt(Key key, byte[] data, Callable<Void> initializer) throws Exception {
        return toCipher(Cipher.DECRYPT_MODE, key, data, initializer);
    }

    /**
     * 数据密文
     * 
     * @param mode
     * @param key
     * @param data
     * @return
     * @throws Exception
     */
    public static byte[] toCipher(int mode, Key key, byte[] data) throws Exception {
        return toCipher(mode, key, data, null);
    }

    /**
     * 数据密文
     *
     * @param mode
     * @param key
     * @param data
     * @param initializer
     * @return
     * @throws Exception
     */
    public static byte[] toCipher(int mode, Key key, byte[] data, Callable<Void> initializer) throws Exception {
        return toCipher(mode, key, key.getAlgorithm(), data, initializer);
    }

    /**
     * 数据密文
     * 
     * @param mode
     * @param key
     * @param transformation
     * @param data
     * @param initializer
     * @return
     * @throws Exception
     */
    public static byte[] toCipher(int mode, Key key, String transformation, byte[] data, Callable<Void> initializer) throws Exception {
        Cipher cipher = Cipher.getInstance(transformation);
        if (initializer == null) {
            cipher.init(mode, key);
        } else {
            initializer.call(cipher, mode, key);
        }
        return cipher.doFinal(data);
    }

    /**
     * 消息认证
     * 
     * @param algorithm
     * @param key
     * @param data
     * @return
     * @throws Exception
     */
    public static byte[] toMac(String algorithm, byte[] key, byte[] data) throws Exception {
        SecretKey secretKey = createSecretKey(algorithm, key);
        Mac mac = Mac.getInstance(secretKey.getAlgorithm());
        mac.init(secretKey);
        return mac.doFinal(data);
    }

    /**
     * 消息认证
     * 
     * @param key
     * @param data
     * @return
     * @throws Exception
     */
    public static byte[] toMac(Key key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance(key.getAlgorithm());
        mac.init(key);
        return mac.doFinal(data);
    }

    /**
     * 消息摘要
     * 
     * @param algorithm
     * @param data
     * @return
     * @throws Exception
     */
    public static byte[] toDigest(String algorithm, byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        digest.update(data);
        return digest.digest();
    }

    /**
     * 私钥签名
     * 
     * @param algorithm
     * @param key
     * @param data
     * @return
     * @throws Exception
     */
    public static byte[] toSign(String algorithm, PrivateKey key, byte[] data) throws Exception {
        Signature signature = Signature.getInstance(algorithm);
        signature.initSign(key);
        signature.update(data);
        return signature.sign();
    }

    /**
     * 公钥验签
     * 
     * @param key
     * @param data
     * @param sign
     * @return
     * @throws Exception
     */
    public static boolean toVerify(PublicKey key, byte[] data, byte[] sign) throws Exception {
        Signature signature = Signature.getInstance(key.getAlgorithm());
        signature.initVerify(key);
        signature.update(data);
        return signature.verify(sign);
    }

    public static boolean toVerify(String secret, int code) throws Exception {
        return toVerify(secret, code, System.currentTimeMillis());
    }

    public static boolean toVerify(String secret, int code, long time) throws Exception {
        Base32 codec = new Base32();
        byte[] key = codec.decode(secret);
        time /= TimeUnit.SECONDS.toMillis(30);
        for (int i = -WINDOW_SIZE; i <= WINDOW_SIZE; i++) {
            if (toVerify(key, time + i) == code) return true;
        }
        return false;
    }

    private static long toVerify(byte[] key, long time) throws Exception {
        byte[] data = new byte[8];
        long value = time;
        for (int i = 8; i-- > 0; value >>>= 8) {
            data[i] = (byte) value;
        }
        SecretKey secretKey = createSecretKey("HmacSHA1", key);
        byte[] hash = toMac(secretKey, data);
        int offset = hash[19] & 0xF;
        long truncatedHash = 0;
        for (int i = 0; i < 4; i++) {
            truncatedHash <<= 8;
            truncatedHash |= (hash[offset + i] & 0xFF);
        }
        truncatedHash &= 0x7FFFFFFF;
        truncatedHash %= 1000000;
        return truncatedHash;
    }

}
