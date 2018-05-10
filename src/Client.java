import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Client {


    public byte[] fileToByteArray(File file) {
        byte[] array = new byte[0];
        try {
            array = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            System.out.println("Error trying to read file " + file.getName());
            e.printStackTrace();
        }
        return array;
    }

    public ArrayList<byte[]> splitByteArray(File file) {
        long size = file.length();
        byte[] byteArrayOfS = ByteBuffer.allocate(8).putLong(size).array();

        int R = (new Random()).nextInt();
        byte[] byteArrayOfR = ByteBuffer.allocate(4).putInt(R).array();

        byte[] fileArray = fileToByteArray(file);
        int numOfPackets = (int) (size + SentFile.B - 1) / SentFile.B; //we need to round up since it's integer/long division and it automatically floors

        ArrayList<byte[]> packets = new ArrayList<>();
        for (int i = 0; i < numOfPackets; i++) {
            int start = i * SentFile.B;
            int end = start + SentFile.B;
            byte[] byteArrayOfI = ByteBuffer.allocate(4).putInt(i).array();
            byte[] packet;
            byte[] data;

            try {
                //prevent index out of bounds exception in the case that we don't fully fill the last packet
                if (fileArray.length < end) {
                    data = Arrays.copyOfRange(fileArray, start, fileArray.length);
                } else {
                    data = Arrays.copyOfRange(fileArray, start, end);
                }

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                outputStream.write(byteArrayOfR);
                outputStream.write(byteArrayOfS);
                outputStream.write(byteArrayOfI);
                outputStream.write(data);
                packet = outputStream.toByteArray();

                packets.add(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return packets;
    }

    public ArrayList<DatagramPacket> createDatagramPackets(ArrayList<byte[]> bytePackets){
        ArrayList<DatagramPacket> packets = new ArrayList<>();
        try {
            String host = "localhost";
            int port = 6788;
            InetAddress address = InetAddress.getByName(host);

            for(byte[] bytePacket : bytePackets){
                DatagramPacket packet = new DatagramPacket(bytePacket, bytePacket.length, address, port);
                packets.add(packet);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return packets;
    }

    public static void main(String[] args) {
        File file = new File("test.txt");
        Client client = new Client();
        ArrayList<DatagramPacket> packets = client.createDatagramPackets(client.splitByteArray(file));

        client.sendPackets(packets);

    }

    public boolean isCompleted(long[] status){
        for(int i=0; i<status.length; i++){
            if(status[i] != -1){
                return false;
            }
        }
        System.out.println("Completed");
        return true;
    }


    public void sendPackets(ArrayList<DatagramPacket> packets){
        int windowSize = 5;
        int windowStart = 0;
        int windowEnd = windowStart + windowSize - 1;
        DatagramSocket dSocket = null;
        boolean completed = false;
        long[] status = new long [packets.size()];

        try {
            dSocket = new DatagramSocket();
            while(!completed){
                for(int i = windowStart; i<windowEnd; i++){
                    long delay = System.currentTimeMillis() - status[i];
                    if(status[i] == 0 || status[i] >= delay){
                        dSocket.send(packets.get(i));
                        status[i] = System.currentTimeMillis();

                        byte[] buffer = new byte[SentFile.B];
                        DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                        dSocket.receive(reply);
                        status[SentFile.extractIntFromByteArray(extractiFromResponse(reply.getData()))] = -1;
                    }
                }
                if(status[windowStart] == -1){
                    windowStart++;
                    windowEnd++;
                }
                completed = isCompleted(status);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if(dSocket != null){
                dSocket.close();
            }
        }

    }

    public byte[] extractiFromResponse(byte[] payload){
        return Arrays.copyOfRange(payload, 4, 8);
    }


}
