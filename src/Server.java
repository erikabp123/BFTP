import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

public class Server {

    public static int B = 1000;

    public static void main(String[] args) {
        try {
            DatagramSocket dSocket = new DatagramSocket(6788);

            byte[] buffer = new byte[Server.B];
            ArrayList<byte[]> fileData = new ArrayList<>();
            boolean allReceived = false;
            while (!allReceived) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                dSocket.receive(request);

                byte[] i = Arrays.copyOfRange(request.getData(), 12, 16);

                DatagramPacket reply = new DatagramPacket(i, i.length, request.getAddress(), request.getPort());
                System.out.println(ByteBuffer.wrap(i).getInt());
                dSocket.send(reply); //Ack

                byte[] data = Arrays.copyOfRange(request.getData(), 16, request.getData().length);
                fileData.add(data);
                if (fileData.size() == 2292) {
                    allReceived = true;
                }
            }

            byte[] merged = mergeArrays(fileData);
            FileOutputStream fileOuputStream = new FileOutputStream("received");
            fileOuputStream.write(merged);
            fileOuputStream.close();

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] mergeArrays(ArrayList<byte[]> fileData) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            for (byte[] data : fileData) {
                outputStream.write(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }


}
