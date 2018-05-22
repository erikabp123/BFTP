import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Client {

    private String fileName;
    private long size;
    private int R;
    private byte[] byteArrayOfS;
    private byte[] byteArrayOfR;
    private byte[] byteArrayOfName;
    private RandomAccessFile raf;
    private int numOfPackets;
    private int port;
    private String host;
    private long[] status;
    private int fileNameLength;
    private byte[] byteArrayOfNameLength;
    private double maxMem;

    public Client(File file, int port, String host) {
        try {
            this.R = (new Random()).nextInt();
            this.size = file.length();
            this.fileName = file.getName();
            fileNameLength = fileName.length();
            this.byteArrayOfR = ByteBuffer.allocate(4).putInt(R).array();
            this.byteArrayOfS = ByteBuffer.allocate(8).putLong(size).array();
            this.byteArrayOfName = fileName.getBytes();
            this.raf = new RandomAccessFile(file, "r");
            this.numOfPackets = (int) (size + SentFile.B - 1) / SentFile.B + 1;
            this.port = port;
            this.host = host;
            this.status = new long[numOfPackets];
            this.byteArrayOfNameLength = ByteBuffer.allocate(4).putInt(fileNameLength).array();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public byte[] getBytePacket(int i){
        int start = i * SentFile.B;
        int end = start + SentFile.B;
        byte[] byteArrayOfI = ByteBuffer.allocate(4).putInt(i).array();
        byte[] packet = new byte[SentFile.B + 16];
        byte[] data = new byte[SentFile.B];

        try {
            raf.seek(start);
            //prevent index out of bounds exception in the case that we don't fully fill the last packet
            if (end > raf.length()) {
                int length = (int) (raf.length() - 1 - start);
                raf.read(data, 0, length);
            } else {
                raf.read(data, 0, SentFile.B);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(byteArrayOfR);
            outputStream.write(byteArrayOfS);
            outputStream.write(byteArrayOfI);
            outputStream.write(data);
            packet = outputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return packet;
    }

    public DatagramPacket createDatagramPacket(byte[] bytePacket){
        DatagramPacket packet = null;
        try {
            InetAddress address = InetAddress.getByName(host);
            packet = new DatagramPacket(bytePacket, bytePacket.length, address, port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return packet;
    }


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

    public String getFileExtension(File file) {
        String extension = "";

        int i = file.getName().lastIndexOf('.');
        if (i > 0) {
            extension = file.getName().substring(i + 1);
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

    public byte[] createFileNamePacket(byte[] R, byte[] size, byte[] i, byte[] fileName) {
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

    public byte[] createFileNamePacket() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int i = numOfPackets - 1;
        byte[] byteArrayOfI = ByteBuffer.allocate(4).putInt(i).array();
        try {
            outputStream.write(byteArrayOfR);
            outputStream.write(byteArrayOfS);
            outputStream.write(byteArrayOfI);
            outputStream.write(byteArrayOfNameLength);
            outputStream.write(byteArrayOfName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream.toByteArray();
    }

    public ArrayList<DatagramPacket> createDatagramPackets(ArrayList<byte[]> bytePackets) {
        ArrayList<DatagramPacket> packets = new ArrayList<>();
        try {
            String host = "localhost";
            int port = 6788;
            InetAddress address = InetAddress.getByName(host);

            for (byte[] bytePacket : bytePackets) {
                DatagramPacket packet = new DatagramPacket(bytePacket, bytePacket.length, address, port);
                packets.add(packet);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return packets;
    }

    public static void main(String[] args) {
        double memUseBefore = MemoryTools.usedMemory();
        int port = 6788;
        String host = "localhost";
        String fileName = "IMG_1098.MOV";
        File file = new File("testFiles/" + fileName);

        Client client = new Client(file, port, host);
//        ArrayList<DatagramPacket> packets = client.createDatagramPackets(client.splitByteArray(file));
//        client.sendPackets(packets);
        client.sendPackets();
        double memDuring = client.getMaxMem();
        System.out.println("Peak memory usage while transferring file: " + (memDuring - memUseBefore) + "MB");

    }

    public boolean isCompleted(long[] status) {
        for (int i = 0; i < status.length; i++) {
            if (status[i] != -1) {
                return false;
            }
        }
        System.out.println("#----------------------------------#");
        System.out.println("#       Completed transfer!        #");
        System.out.println("#----------------------------------#");
        return true;
    }

    public boolean isCompleted() {
        for (int i = 0; i < status.length; i++) {
            if (status[i] != -1) {
                return false;
            }
        }
        System.out.println("#----------------------------------#");
        System.out.println("#       Completed transfer!        #");
        System.out.println("#----------------------------------#");
        return true;
    }

    public double getMaxMemoryUsed(double previous){
        double memInUse = MemoryTools.usedMemory();
        if(memInUse > previous){
            return memInUse;
        }
        return previous;
    }

    public void sendPackets() {
        int windowStart = 0;
        int windowEnd = windowStart + SentFile.WINDOW_SIZE;
        DatagramSocket dSocket = null;
        boolean completed = false;
        System.out.println("Sending file: " + fileName);
        System.out.println("File size: " + MemoryTools.convertToMB(size) + "MB");
        System.out.println("Amount of packets: " + numOfPackets);

        maxMem = getMaxMemoryUsed(maxMem);

        try {
            dSocket = new DatagramSocket();
            while (!completed) {
                for (int i = windowStart; i < windowEnd; i++) {
                    long delay = System.currentTimeMillis() - status[i];
                    if (status[i] == 0 || status[i] >= delay) {
                        if(i == numOfPackets - 1){
                            dSocket.send(createDatagramPacket(createFileNamePacket()));
                        } else {
                            dSocket.send(createDatagramPacket(getBytePacket(i)));
                        }
                        status[i] = System.currentTimeMillis();

//                        if(i == packets.size() - 1){
//                            System.out.println(SentFile.extractStringFromByteArray(SentFile.extractFileNameFromPayload(packets.get(i).getData())));
//                        }

                        byte[] buffer = new byte[SentFile.B];
                        DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                        dSocket.receive(reply);
                        status[SentFile.extractIntFromByteArray(extractiFromResponse(reply.getData()))] = -1;
                    }
                    maxMem = getMaxMemoryUsed(maxMem);
                }
                if (status[windowStart] == -1) {
                    windowStart++;
                    windowEnd++;
                }
                completed = isCompleted();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (dSocket != null) {
                dSocket.close();
            }
        }

    }

    public double getMaxMem(){
        return maxMem;
    }

    public void sendPackets(ArrayList<DatagramPacket> packets) {
        int windowStart = 0;
        int windowEnd = windowStart + SentFile.WINDOW_SIZE;
        DatagramSocket dSocket = null;
        boolean completed = false;
        long[] status = new long[packets.size()];
        System.out.println("Amount of packets: " + packets.size());

        try {
            dSocket = new DatagramSocket();
            while (!completed) {
                for (int i = windowStart; i < windowEnd; i++) {
                    long delay = System.currentTimeMillis() - status[i];
                    if (status[i] == 0 || status[i] >= delay) {
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
                if (status[windowStart] == -1) {
                    windowStart++;
                    windowEnd++;
                }
                completed = isCompleted(status);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (dSocket != null) {
                dSocket.close();
            }
        }

    }

    public byte[] extractiFromResponse(byte[] payload) {
        return Arrays.copyOfRange(payload, 4, 8);
    }


}
