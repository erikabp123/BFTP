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

    public String getFileExtension(File file){
        String extension = "";

        int i = file.getName().lastIndexOf('.');
        if (i > 0) {
            extension = file.getName().substring(i+1);
        }
        return extension;
    }

    public ArrayList<byte[]> splitByteArray(File file) {
        long size = file.length();
        byte[] byteArrayOfS = ByteBuffer.allocate(8).putLong(size).array();

        int R = (new Random()).nextInt();
        byte[] byteArrayOfR = ByteBuffer.allocate(4).putInt(R).array();

        byte[] fileArray = fileToByteArray(file);
        int numOfPackets = (int) (size + SentFile.B - 1) / SentFile.B; //we need to round up since it's integer/long division and it automatically floors

        String fileName = getFileExtension(file);

        byte[] byteArrayOfName = fileName.getBytes();

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

        byte[] fileNamePacket = createFileNamePacket(byteArrayOfR, byteArrayOfS, ByteBuffer.allocate(4).putInt(numOfPackets).array(), byteArrayOfName);

        packets.add(fileNamePacket);

        return packets;
    }

    public byte[] createFileNamePacket(byte[] R, byte[] size, byte[] i, byte[] fileName){
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(R);
            outputStream.write(size);
            outputStream.write(i);
            outputStream.write(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
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
        String fileName = "test.txt";
        File file = new File("testFiles/" + fileName);
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
        System.out.println("Completed transfer!");
        return true;
    }


    public void sendPackets(ArrayList<DatagramPacket> packets){
        int windowStart = 0;
        int windowEnd = windowStart + SentFile.WINDOW_SIZE;
        DatagramSocket dSocket = null;
        boolean completed = false;
        long[] status = new long [packets.size()];
        System.out.println("Amount of packets: " + packets.size());

        try {
            dSocket = new DatagramSocket();
            while(!completed){
                for(int i = windowStart; i<windowEnd; i++){
                    long delay = System.currentTimeMillis() - status[i];
                    if(status[i] == 0 || status[i] >= delay){
                        dSocket.send(packets.get(i));
                        status[i] = System.currentTimeMillis();

//                        if(i == packets.size() - 1){
//                            System.out.println(SentFile.extractStringFromByteArray(SentFile.extractFileNameFromPayload(packets.get(i).getData())));
//                        }

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
