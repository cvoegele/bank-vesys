package bank.network.UDP;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Test {

    public static void main(String[] args) {

        try {
            DatagramSocket socket = new DatagramSocket();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream ous = new ObjectOutputStream(bos);
            BankPackage bankPackage = new BankPackage("GETACCOUNTS", null, null, null);
            ous.writeObject(bankPackage);
            ous.close();
            ous.flush();
            byte[] buf = bos.toByteArray();

            var packet = new DatagramPacket(buf, buf.length,
                    InetAddress.getByName("localhost"), 4711);

            socket.send(packet);
            int packetsSent = 0;
            while (packetsSent < 5) {
                try {
                    socket.setSoTimeout(5000);
                    DatagramPacket responseShell = new DatagramPacket(new byte[100000], 100000);
                    socket.receive(responseShell); //will throw if no response

                    //received response
                    byte[] data = responseShell.getData();
                    ObjectInputStream bis = new ObjectInputStream(new ByteArrayInputStream(data));
                    Object obj = bis.readObject();

                    if (obj instanceof BankPackage) {
                        //check if response to this
                        if (bankPackage.isResponse((BankPackage) obj)) {
                            //correct response sent by bank
                            //return bankPackage;
                            System.out.println(((BankPackage) obj).toString());
                            return;
                        }
                    }

                } catch (SocketException | EOFException e) {
                    System.out.println("received timeout... sending again...");
                    e.printStackTrace();
                    socket.send(packet);
                    packetsSent++;
                }
            }
            throw new Exception("no response received after 5 timeouts");

        } catch (Exception e) {
            System.out.println(e.toString());
            //throw new IOException("Did not receive response after 5 timeouts");
        }
    }
}
