package org.example;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class ThreadListeningClient extends Thread {
    private final DatagramSocket socket;

    public ThreadListeningClient( DatagramSocket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            while (!isInterrupted()) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String msg = new String(packet.getData(), 0, packet.getLength());
                System.out.println("\nMessage reçu : " + msg);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
