import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

public class Client {

    private String fileName;
    private long size;
    private byte[] byteArrayOfS;
    private byte[] byteArrayOfR;
    private byte[] byteArrayOfName;
    private RandomAccessFile raf;
    private int numOfPackets;
    private int port;
    private String host;
    private long[] status;
    private byte[] byteArrayOfNameLength;
    private double maxMem;

    public Client(File file, int port, String host) {
        try {
            int R = (new Random()).nextInt();
            this.size = file.length();
            this.fileName = file.getName();
            int fileNameLength = fileName.length();
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

                        byte[] buffer = new byte[SentFile.B];
                        DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                        dSocket.receive(reply);
                        status[ByteArrayMethods.extractIntFromByteArray(ByteArrayMethods.extractiFromResponse(reply.getData()))] = -1;
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







    public static void main(String[] args) {
        String host;
        int port;
        String filePath;

        if(args.length != 3){
            System.out.println("Incorrect amount of arguments! Must supply 3 arguments: host, port, and file path! " +
                    "Using default testing values!");
            host = "localhost";
            port = 6788;
            filePath = "testFiles/" + "test.txt";
        } else {
            host = args[0];
            port = new Integer(args[1]);
            filePath = args[2];
        }


        double memUseBefore = MemoryTools.usedMemory();
        long startTime = System.currentTimeMillis();

        File file = new File(filePath);

        Client client = new Client(file, port, host);
        client.sendPackets();
        double memDuring = client.getMaxMem();
        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);
        System.out.println("Peak memory usage while transferring file: " + (memDuring - memUseBefore) + "MB");
        System.out.println("File transfer took: " + duration + "ms");
    }

}
