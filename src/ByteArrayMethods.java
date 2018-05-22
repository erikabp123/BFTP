import java.nio.ByteBuffer;
import java.util.Arrays;

public class ByteArrayMethods {

    public static int extractIntFromByteArray(byte[] array) {
        return ByteBuffer.wrap(array).getInt();
    }

    public static long extractLongFromByteArray(byte[] array) {
        return ByteBuffer.wrap(array).getLong();
    }

    public static String extractStringFromByteArray(byte[] array) {
        return new String(array);
    }

    public static byte[] extractRFromPayload(byte[] payload) {
        return Arrays.copyOfRange(payload, 0, 4);
    }

    public static byte[] extractiFromPayload(byte[] payload) {
        return Arrays.copyOfRange(payload, 12, 16);
    }

    public static byte[] extractSizeFromPayload(byte[] payload) {
        return Arrays.copyOfRange(payload, 4, 12);
    }

    public static byte[] extractDataFromPayload(byte[] payload) {
        return Arrays.copyOfRange(payload, 16, payload.length);
    }

    public static byte[] extractFileNameFromPayload(byte[] payload){
        int fileNameLength = extractIntFromByteArray(Arrays.copyOfRange(payload, 16, 20));
        return Arrays.copyOfRange(payload, 20, 20 + fileNameLength);
    }

    public static byte[] extractDataFromFinalPacketPayload(byte[] payload, long fileSize) {
        int packetNumber  = (int) (fileSize + SentFile.B - 1) / SentFile.B - 1;
        int endPoint = (int) (fileSize - packetNumber*SentFile.B + 16);
        return Arrays.copyOfRange(payload, 16, endPoint);
    }

    public static byte[] extractiFromResponse(byte[] payload) {
        return Arrays.copyOfRange(payload, 4, 8);
    }



}
