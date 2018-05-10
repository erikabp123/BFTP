import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;

public class SentFile {

    public static int B = 1000;
    private HashMap<Integer, byte[]> fileData;
    private long fileSize;
    private int R;
    private int numOfPackets;

    public SentFile(byte[] payload){
        fileData = new HashMap<>();

        int i = extractIntFromByteArray(extractiFromPayload(payload));
        fileSize = extractLongFromByteArray(extractSizeFromPayload(payload));
        R = extractIntFromByteArray(extractRFromPayload(payload));
        numOfPackets = (int)(fileSize + B - 1) / B;

        byte[] data = extractDataFromPayload(payload);
        fileData.put(i, data); //overwrites duplicated packages, which we want

        if(validateAllPacketsReceived()){
            createFileFromByteArray(mergeArrays());
        }
    }

    public static int extractIntFromByteArray(byte[] array){
        return ByteBuffer.wrap(array).getInt();
    }

    public static long extractLongFromByteArray(byte[] array){
        return ByteBuffer.wrap(array).getLong();
    }

    public static String extractStringFromByteArray(byte[] array){
        return new String(array);
    }

    public boolean validateAllPacketsReceived(){
        if(fileData.size() != numOfPackets){
            return false;
        }
        System.out.println("All packets received!");
        return true;
    }

    public byte[] mergeArrays() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            for(int i=0; i<fileData.size(); i++){
                outputStream.write(fileData.get(i));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }

    public void createFileFromByteArray(byte[] merged){
        FileOutputStream fileOuputStream = null;
        try {
            fileOuputStream = new FileOutputStream(System.currentTimeMillis() + "-received.txt");
            fileOuputStream.write(merged);
            fileOuputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean packetReceived(byte[] payload){
        int i = extractIntFromByteArray(Arrays.copyOfRange(payload, 12, 16));
        byte[] data = extractDataFromPayload(payload);

        fileData.put(i, data); //overwrites duplicated packages, which we want

        if(validateAllPacketsReceived()){
            createFileFromByteArray(mergeArrays());
            return true;
        }

        return false;
    }

    public static byte[] extractRFromPayload(byte[] payload){
        return Arrays.copyOfRange(payload, 0, 4);
    }

    public static byte[] extractiFromPayload(byte[] payload){
        return Arrays.copyOfRange(payload, 12, 16);
    }

    public static byte[] extractSizeFromPayload(byte[] payload){
        return Arrays.copyOfRange(payload, 4, 12);
    }

    public static byte[] extractDataFromPayload(byte[] payload){
        return Arrays.copyOfRange(payload, 16, payload.length);
    }

    public static String getSenderAsIDString(DatagramPacket packet){
        String address = packet.getAddress().getHostName();
        String port = "" + packet.getPort();
        String R = extractStringFromByteArray(extractRFromPayload(packet.getData()));
        return R + address + port;
    }

}
