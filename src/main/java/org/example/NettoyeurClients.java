package org.example;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ConcurrentHashMap;

public class NettoyeurClients implements Runnable {
    private final ConcurrentHashMap<String, ClientInfo> clients;
    private final long timeoutMillis = 60000; // 60 secondes
    private final DatagramSocket socketPrincipal;

    public NettoyeurClients(ConcurrentHashMap<String, ClientInfo> clients, DatagramSocket socketPrincipal) {
        this.clients = clients;
        this.socketPrincipal = socketPrincipal;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            long maintenant = System.currentTimeMillis();
            for (ClientInfo client : clients.values()) {
                if (maintenant - client.getDerniereActivite() > 60000) {
                    try {
                        // On envoie le message via le socket principal (port 9000)
                        byte[] buffer = "TIMEOUT".getBytes();
                        DatagramPacket packet = new DatagramPacket(
                                buffer,
                                buffer.length,
                                client.getAdresseIp(),
                                client.getPort() // Le port initial du client
                        );
                        socketPrincipal.send(packet);

                        // On le supprime de la liste
                        clients.remove(client.getPseudo());


                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }
}