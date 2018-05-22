import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;

public class Server {

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
                        filesInTransit.put(senderID, newFileInTransit);
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
