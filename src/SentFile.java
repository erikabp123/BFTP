import java.io.*;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class SentFile {

    public static int B = 1000;
    public static int WINDOW_SIZE = 5;
    private HashMap<Integer, byte[]> fileData;
    private long fileSize;
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

        int i = ByteArrayMethods.extractIntFromByteArray(ByteArrayMethods.extractiFromPayload(payload));
        fileSize = ByteArrayMethods.extractLongFromByteArray(ByteArrayMethods.extractSizeFromPayload(payload));
        numOfPackets = (int) (fileSize + B - 1) / B + 1;

        byte[] data = null;

        if(isNamePacket(i)){
            data = ByteArrayMethods.extractFileNameFromPayload(payload);
            fileName = ByteArrayMethods.extractStringFromByteArray(data);
        } else if(i == numOfPackets - 2){
            data = ByteArrayMethods.extractDataFromFinalPacketPayload(payload, fileSize);
            determineIfWriteOrStore(i, data);
        } else {
            data = ByteArrayMethods.extractDataFromPayload(payload);
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
            if((new File(fileName)).exists()){
                int randomPrefix = (new Random()).nextInt();
                Files.move(source, source.resolveSibling("(" + randomPrefix + ") " + fileName));
            } else {
                Files.move(source, source.resolveSibling(fileName));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        int i = ByteArrayMethods.extractIntFromByteArray(Arrays.copyOfRange(payload, 12, 16));
        byte[] data = null;

        if(isNamePacket(i)){
            data = ByteArrayMethods.extractFileNameFromPayload(payload);
            fileName = ByteArrayMethods.extractStringFromByteArray(data);
        } else if(i == numOfPackets - 2){
            data = ByteArrayMethods.extractDataFromFinalPacketPayload(payload, fileSize);
            determineIfWriteOrStore(i, data);
        } else {
            data = ByteArrayMethods.extractDataFromPayload(payload);
            determineIfWriteOrStore(i, data);
        }

        if (validateAllPacketsReceived()) {
            renameFile();
            return true;
        }

        return false;
    }

    public static String getSenderAsIDString(DatagramPacket packet) {
        String address = packet.getAddress().getHostName();
        String port = "" + packet.getPort();
        String R = "" + ByteArrayMethods.extractIntFromByteArray(ByteArrayMethods.extractRFromPayload(packet.getData()));
        return R + ":" + address + ":" + port;
    }


}
