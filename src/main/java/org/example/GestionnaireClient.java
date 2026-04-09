package org.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classe runnable (possédant ainsi son propre thread)
 * Celle-ci à pour vocation d'être créée par le serveur de messagerie. Celle-ci permet de concentrer la preoccupation de gestion d'échange avec un client dans un thread unique.
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

    /**
     * Actions principales effectuées par le thread
     */
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
     * @param buffer le message à envoyer
     * @throws IOException si une erreur se produit lors de l'envoi du message
     */
    private void diffuserMessage(byte[] buffer) throws IOException {
        for (ClientInfo connectedClient : clients.values()) {
            if (!connectedClient.getPseudo().equals(this.clientInfo.getPseudo())) {
                envoyerMessage(buffer, connectedClient.getInetSocketAddress());
            }
        }
    }

    /**
     * Envoyer un message à un destinataire particulier
     * @param buffer le message
     * @param destinataire le destinataire
     * @throws IOException si une erreur survient lors de l'envoi du message
     */
    private void envoyerMessage(byte[] buffer, InetSocketAddress destinataire) throws IOException {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, destinataire);
        socket.send(packet);
    }

    /**
     * Va ecouter les messages reçus sur le socket jusqu'à sa fermeture.
     * Si le message vaut "exit", alors on initie la procedure de fermeture de la session.
     */
    private void ecouterMessagesClient() {
        while (!socket.isClosed()) {
            try {
                // buffer d'écoute
                byte[] messageClient = new byte[126];
                DatagramPacket response = new DatagramPacket(messageClient, messageClient.length);
                socket.receive(response);
                clientInfo.actualiserActivite();
                if (!clients.containsKey(clientInfo.getPseudo())) {
                    socket.close();
                    return;
                }
                // conversion de la réponse au format string
                String responseStringValue = new String(response.getData(), 0, response.getLength()).trim();
                if (responseStringValue.equalsIgnoreCase("exit")) {
                    // si la réponse vaut exit, on initie la procedure de fermeture de la session
                    quitter();
                } else if (responseStringValue.equalsIgnoreCase("/liste")) {
                    System.out.println("[INFO] " + clientInfo.getPseudo() + " a demandé la liste des utilisateurs connectés.");
                    envoyerListePseudos();
                } else if (responseStringValue.startsWith("/mp ")) {
                    gererMessagePrive(responseStringValue);
                } else {
                    // sinon on diffuse le message à tous les utilisateurs
                    byte[] donneesUtiles = new byte[response.getLength()];
                    System.arraycopy(response.getData(), 0, donneesUtiles, 0, response.getLength());

                    diffuserMessage(donneesUtiles);
                }
            } catch (IOException e) {
                System.out.println("[ERREUR] gestionnaire client a planté pour " + clientInfo.getPseudo() + " : " + e);
                quitter();
            }
        }
    }

    /**
     * Procedure de fermeture de session.
     */
    private void quitter() {
        // Les autres utilisateurs sont avertis. Si une erreur survient lors de la diffusion du message, on ferme quand même la session.
        try {
            diffuserMessage((clientInfo.getPseudo() + " En a marre de parler et est a préféré quitter le channel.").getBytes());
        } catch (IOException e) {
            System.out.println("[ERREUR] gestionnaire client a planté pour " + clientInfo.getPseudo() + " : " + e);
        }
        // on retire le client de la liste des clients connectés
        clients.remove(clientInfo.getPseudo());
        // on ferme le socket
        socket.close();
    }

    private void envoyerListePseudos() throws IOException {
        String liste = "Utilisateurs connectés : " + String.join(", ", clients.keySet());
        byte[] buffer = liste.getBytes();
        // On renvoie le message à l'adresse et au port stockés dans clientInfo
        envoyerMessage(buffer, clientInfo.getInetSocketAddress());
    }


    private void gererMessagePrive(String message) throws IOException {
        // Le format attendu est "/mp <pseudo> <message>"
        String[] parties = message.split(" ", 3);
        if (parties.length < 3) {
            String erreur = "Format de message privé invalide. Utilisez : /mp <pseudo> <message>";
            envoyerMessage(erreur.getBytes(), clientInfo.getInetSocketAddress());
            return;
        }
        String pseudoDestinataire = parties[1];
        String contenuMessage = parties[2];

        ClientInfo destinataire = clients.get(pseudoDestinataire);
        if (destinataire == null) {
            String msgMPInconnu = "Utilisateur inconnu : " + pseudoDestinataire;
            envoyerMessage(msgMPInconnu.getBytes(), clientInfo.getInetSocketAddress());
            return;
        } else {
            String messagePrive = "[MP de " + clientInfo.getPseudo() + "] " + contenuMessage;
            envoyerMessage(messagePrive.getBytes(), destinataire.getInetSocketAddress());
        }

    }
}
