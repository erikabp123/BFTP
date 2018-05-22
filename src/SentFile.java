import java.io.*;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

public class SentFile {

    public static int B = 1000;
    public static int WINDOW_SIZE = 5;
    private HashMap<Integer, byte[]> fileData;
    private long fileSize;
    private int R;
    private int numOfPackets;
    private FileOutputStream fileOutputStream;
    private int lastPacketWritten;
    private String fileName;
    private String tempName;


    public SentFile(byte[] payload) {
        fileData = new HashMap<>();
        fileName = null;
        lastPacketWritten = -1; //no packets written to file yet
        try {
            tempName = System.currentTimeMillis() + "-received";
            fileOutputStream = new FileOutputStream(tempName + ".part");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int i = extractIntFromByteArray(extractiFromPayload(payload));
        fileSize = extractLongFromByteArray(extractSizeFromPayload(payload));
        R = extractIntFromByteArray(extractRFromPayload(payload));
        numOfPackets = (int) (fileSize + B - 1) / B + 1;

        byte[] data = null;

        if(isNamePacket(i)){
            data = extractFileNameFromPayload(payload);
            fileName = extractStringFromByteArray(data);
        } else if(i == numOfPackets - 2){
            data = extractDataFromFinalPacketPayload(payload, fileSize);
            determineIfWriteOrStore(i, data);
        } else {
            data = extractDataFromPayload(payload);
            determineIfWriteOrStore(i, data);
        }

        if(validateAllPacketsReceived()){
            renameFile();
        }
    }

    public void determineIfWriteOrStore(int i, byte[] data){
        if(i <= lastPacketWritten){
            return;
        }
        if (lastPacketWritten != i - 1) {
            fileData.put(i, data); //overwrites duplicated packages, which we want
        } else {
            writeToFileFromByteArray(data);
        }
    }

    public void renameFile(){
        try {
            fileOutputStream.close();
            fileOutputStream = null;
            System.gc();
            Path source = Paths.get(tempName + ".part");
            Files.move(source, source.resolveSibling(fileName));
//            File partFile = new File(tempName + ".part");
//            File finishedFile = new File(fileName);
//            partFile.renameTo(finishedFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int extractIntFromByteArray(byte[] array) {
        return ByteBuffer.wrap(array).getInt();
    }

    public static long extractLongFromByteArray(byte[] array) {
        return ByteBuffer.wrap(array).getLong();
    }

    public static String extractStringFromByteArray(byte[] array) {
        return new String(array);
    }

    public boolean validateAllPacketsReceived() {
        if (lastPacketWritten != numOfPackets - 1 && fileName == null) {
            return false;
        }
        return true;
    }

    public boolean isNamePacket(int i){
        if(i == numOfPackets - 1){
            return true;
        }
        return false;
    }

    public void writeToFileFromByteArray(byte[] data) {
        try {
            fileOutputStream.write(data);
            lastPacketWritten++;
            attemptToWriteStoredPacketToFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeStoredPacketsToFile() {
        try {
            int packetWindow = lastPacketWritten + WINDOW_SIZE;
            for (int i = lastPacketWritten; i < packetWindow; i++) {
                if (fileData.containsKey(lastPacketWritten)) {
                    fileOutputStream.write(fileData.get(lastPacketWritten));
                    fileData.remove(lastPacketWritten);
                    lastPacketWritten++;
                } else {
                    return;
                }
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public void attemptToWriteStoredPacketToFile() {
        try {
            int packetToBeWritten = lastPacketWritten + 1;
            if (fileData.containsKey(packetToBeWritten)) {
                fileOutputStream.write(fileData.get(packetToBeWritten));
                fileData.remove(packetToBeWritten);
                lastPacketWritten++;
                attemptToWriteStoredPacketToFile();
            } else {
                return;
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }


    public boolean packetReceived(byte[] payload) {
        int i = extractIntFromByteArray(Arrays.copyOfRange(payload, 12, 16));
        byte[] data = null;

        if(isNamePacket(i)){
            data = extractFileNameFromPayload(payload);
            fileName = extractStringFromByteArray(data);
        } else if(i == numOfPackets - 2){
            data = extractDataFromFinalPacketPayload(payload, fileSize);
            determineIfWriteOrStore(i, data);
        } else {
            data = extractDataFromPayload(payload);
            determineIfWriteOrStore(i, data);
        }

        if (validateAllPacketsReceived()) {
            renameFile();
            return true;
        }

        return false;
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
        int packetNumber  = (int) (fileSize + B - 1) / B - 1;
        int endPoint = (int) (fileSize - packetNumber*B + 16);
        return Arrays.copyOfRange(payload, 16, endPoint);
    }

    public static String getSenderAsIDString(DatagramPacket packet) {
        String address = packet.getAddress().getHostName();
        String port = "" + packet.getPort();
        String R = "" + extractIntFromByteArray(extractRFromPayload(packet.getData()));
        return R + ":" + address + ":" + port;
    }

}
