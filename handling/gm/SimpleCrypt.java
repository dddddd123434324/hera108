package handling.gm;

/**
 * @author Lunatic
 */
public class SimpleCrypt {

    private static final int SC_KEY = 0x8A;

    public static byte[] memcpy(byte[] data) {
        byte[] ret = new byte[data.length];
        System.arraycopy(data, 0, ret, 0, data.length);
        return ret;
    }

    public static void simple_crypt(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            data[i] ^= SC_KEY - i;
        }
    }
}
