import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class Client {

    int B = 1000;

    public byte[] fileToByteArray(File file){
        byte[] array = new byte[0];
        try {
            array = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            System.out.println("Error trying to read file " + file.getName());
            e.printStackTrace();
        }
        return array;
    }

    public ArrayList<byte[]> splitByteArray(File file){
        long size = file.length();
        int R = (new Random()).nextInt();
        byte[] fileArray = fileToByteArray(file);
        int numOfPackets = (int) (size + B - 1)/B; //we need to round up since it's integer/long division and it automatically floors
        ArrayList<byte[]> blocks = new ArrayList<>();
        for(int i = 0; i<numOfPackets; i++){
            System.out.println("loop: " + i);
            int start = i*B;
            int end = start + B;
            if(fileArray.length < end){
                System.out.println("too short");
                blocks.add(Arrays.copyOfRange(fileArray, start, fileArray.length));
                break; //prevent index out of bounds exception in the case that we don't fully fill the last packet
            }
            blocks.add(Arrays.copyOfRange(fileArray, start, end));
        }
        return blocks;
    }

    public static void main(String[] args){
        File file = new File("C:\\Users\\Erik\\Desktop\\BFTP\\test.txt");
        Client client = new Client();
        ArrayList<byte[]> blocks = client.splitByteArray(file);

        for(byte[] block : blocks){
            DatagramPacket packet = new DatagramPacket(block, client.B);
            packet.
            System.out.println(block);
        }
    }
}
