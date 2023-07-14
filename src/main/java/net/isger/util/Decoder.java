package net.isger.util;

import java.io.InputStream;

public interface Decoder {

    public Object decode(byte[] content);

    public Object decode(InputStream is);

}
