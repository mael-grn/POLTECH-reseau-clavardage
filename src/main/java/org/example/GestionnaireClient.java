package org.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classe runnable (possédant ainsi son propre thread)
 * Celle-ci a pour vocation d'être créée par le serveur de messagerie. Celle-ci permet de concentrer la preoccupation de gestion d'échange avec un client dans un thread unique.
 */
public class GestionnaireClient implements Runnable {
    private final ClientInfo clientInfo;
    private final DatagramSocket socket;
    private final ConcurrentHashMap<String, ClientInfo> clients;

    public GestionnaireClient(ClientInfo clientInfo, DatagramSocket socket, ConcurrentHashMap<String, ClientInfo> clients) {
        this.clientInfo = clientInfo;
        this.socket = socket;
        this.clients = clients;
    }

    @Override
    public void run() {
        try {
            diffuserMessage(("Un tonnerre d'applaudissement pour " + clientInfo.getPseudo() + " !").getBytes());
            ecouterMessagesClient();
        } catch (IOException e) {

            //Si une erreur se produit, on quitte.
            System.out.println("[ERREUR] gestionnaire client a planté pour " + clientInfo.getPseudo() + " : " + e);
            quitter();
        }
    }

    /**
     * Diffuse un message à chaque client actuellement connecté
     * @param message le message a envoyé à tout le monde
     * @throws IOException
     */
    private void diffuserMessage(byte[] message) throws IOException {
        for (ClientInfo connectedClient : clients.values()) {
            if (!connectedClient.getPseudo().equals(this.clientInfo.getPseudo())) {
                envoyerMessage(message, connectedClient.getInetSocketAddress());
            }
        }
    }

    private void envoyerMessage(byte[] message, InetSocketAddress destinataire) throws IOException {
        DatagramPacket packet = new DatagramPacket(message, message.length, destinataire);
        socket.send(packet);
    }

    private void ecouterMessagesClient() {
        while (!socket.isClosed()) {
            try {
                byte[] messageClient = new byte[126];
                DatagramPacket response = new DatagramPacket(messageClient, messageClient.length);
                socket.receive(response);
                String responseStringValue = new String(response.getData(), StandardCharsets.US_ASCII);
                if (responseStringValue.equalsIgnoreCase("exit")) {
                    quitter();
                } else {
                    diffuserMessage(response.getData());
                }
            } catch (IOException e) {
                System.out.println("[ERREUR] gestionnaire client a planté pour " + clientInfo.getPseudo() + " : " + e);
                quitter();
            }
        }
    }

    private void quitter() {
        try {
            diffuserMessage((clientInfo.getPseudo() + " En a marre de parler et est a préféré quitter le channel.").getBytes());
        } catch (IOException e) {
            System.out.println("[ERREUR] gestionnaire client a planté pour " + clientInfo.getPseudo() + " : " + e);
            clients.remove(clientInfo.getPseudo());
            socket.close();
        }
    }
}
