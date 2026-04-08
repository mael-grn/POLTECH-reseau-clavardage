package org.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public  class ServeurChatUDP {

    private static ConcurrentHashMap<String, ClientInfo> clients;
    private static DatagramSocket socket;

    static void main() {
        clients = new ConcurrentHashMap<>();
        try {
            socket = new DatagramSocket(9000);
            ecouterRequetesJoin();
        } catch (SocketException e) {
            System.out.println("Impossible de démarrer sur le port 9000. Merci de vérifier que le port est libre pour les communications UDP.");
            System.exit(0);
        }
    }

    private static void envoyerMessage(byte[] message, InetSocketAddress destinataire) throws IOException {
        DatagramPacket packet = new DatagramPacket(message, message.length, destinataire);
        socket.send(packet);
    }

    private static boolean pseudoDejaUtilise(String pseudo) {
        return clients.containsKey(pseudo);
    }

    private static void ecouterRequetesJoin() {
        while (!socket.isClosed()) {
            try {
                byte[] buffer = new byte[126];
                DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                socket.receive(response);
                String responseString = new String(response.getData(), StandardCharsets.US_ASCII);
                if (responseString.startsWith("JOIN:") && responseString.length() > 5) {
                    String pseudo = responseString.substring(5);
                    ClientInfo client = new ClientInfo(pseudo, response.getAddress(), response.getPort());
                    if (!pseudoDejaUtilise(pseudo)) {
                        ajouterNouveauClient(client);
                    } else {
                        envoyerMessage(("Pseudo déjà utilisé").getBytes(), client.getInetSocketAddress());
                    }
                }
            } catch (IOException e) {
                System.out.println("[ERREUR] une erreur est survenue lors de l'écoute des requêtes JOIN : " + e);
            }
        }
    }

    private static void ajouterNouveauClient(ClientInfo nouveauClient) {
        try {
            DatagramSocket socketClient = new DatagramSocket();
            envoyerMessage(("PORT:"+socketClient.getPort()).getBytes(), nouveauClient.getInetSocketAddress());
            clients.put(nouveauClient.getPseudo(), nouveauClient);
            GestionnaireClient gestionnaireClient = new GestionnaireClient(nouveauClient, socketClient, clients);
            gestionnaireClient.run();
        } catch (IOException e) {
            System.out.println("[ERREUR] impossible de créer un nouvel utilisateur pour " + nouveauClient.getPseudo() + " : " + e);
        }

    }
}
