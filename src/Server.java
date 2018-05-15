import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Server {

    

//    public static void main(String[] args) {
//        try {
//            DatagramSocket dSocket = new DatagramSocket(6788);
//
//            byte[] buffer = new byte[SentFile.B];
//            HashMap<Integer, byte[]> fileData = new HashMap<>();
//            long fileSize = 0;
//
//            // receive packets loop
//            while (!validateAllPacketsReceived(fileData, fileSize)) {
//                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
//                dSocket.receive(request);
//
//                byte[] i = Arrays.copyOfRange(request.getData(), 12, 16);
//
//                DatagramPacket reply = new DatagramPacket(i, i.length, request.getAddress(), request.getPort());
//                System.out.println(ByteBuffer.wrap(i).getInt());
//                dSocket.send(reply); //Ack
//
//                byte[] sizeAsByte = Arrays.copyOfRange(request.getData(), 4, 12);
//                fileSize = ByteBuffer.wrap(sizeAsByte).getLong();
//                byte[] data = Arrays.copyOfRange(request.getData(), 16, request.getData().length);
//                fileData.put(SentFile.extractIntFromByteArray(i), data); //overwrites duplicated packages, which we want
//            }
//
//            byte[] merged = mergeArrays(fileData);
//            FileOutputStream fileOuputStream = new FileOutputStream("received.txt");
//            fileOuputStream.write(merged);
//            fileOuputStream.close();
//
//        } catch (java.io.IOException e) {
//            e.printStackTrace();
//        }
//    }

    public static void main(String[] args) {
        try {
            DatagramSocket dSocket = new DatagramSocket(6788);

            byte[] buffer = new byte[SentFile.B + 16];
            HashMap<String, SentFile> filesInTransit = new HashMap<>();

            // receive packets loop
            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                dSocket.receive(request);

                byte[] payload = request.getData();
                byte[] i = SentFile.extractiFromPayload(payload);
                byte[] R = SentFile.extractRFromPayload(payload);
                String senderID = SentFile.getSenderAsIDString(request);

                if(filesInTransit.containsKey(senderID)){
                    boolean finished = filesInTransit.get(senderID).packetReceived(payload);
                    if(finished){
                        filesInTransit.remove(senderID);
                    }
                } else {
                    System.out.println("Received new file from: " + senderID);
                    SentFile newFileInTransit = new SentFile(payload);
                    if(!newFileInTransit.validateAllPacketsReceived()){
                        filesInTransit.put(senderID, new SentFile(payload));
                    }
                }

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                try {
                    outputStream.write(R);
                    outputStream.write(i);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] msg = outputStream.toByteArray();

                DatagramPacket reply = new DatagramPacket(msg, msg.length, request.getAddress(), request.getPort());

                dSocket.send(reply); //Ack
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }


}
