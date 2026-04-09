package org.example;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logique de base pour un serveur de messagerie : écoute des requêtes pour rejoindre le channel,
 * et création de thread pour chaque utilisateur souhaitant rejoindre.
 */
public  class ServeurChatUDP {

    // Liste des utilisateurs actuellement connectés
    private static ConcurrentHashMap<String, ClientInfo> clients;
    // Socket principal de l'application, écoutant les requêtes JOIN
    private static DatagramSocket socket;

    static void main() {
        clients = new ConcurrentHashMap<>();


        try {
            //Création du socket principal
            System.out.println("[DÉMARRAGE] Bienvenue notre service de messagerie!\n[DÉMARRAGE] Initialisation de la connection...");
            socket = new DatagramSocket(9000);
            NettoyeurClients tacheNettoyage = new NettoyeurClients(clients, socket);
            Thread threadNettoyeur = new Thread(tacheNettoyage);
            threadNettoyeur.setDaemon(true);
            threadNettoyeur.start();
            ecouterRequetesJoin();
        } catch (SocketException e) {
            // Si on ne parvient pas à créer le socket, on arrête l'application
            System.out.println("Impossible de démarrer sur le port 9000. Merci de vérifier que le port est libre pour les communications UDP.");
            System.exit(0);
        }
    }

    /**
     * Fonction servant à simplifier la logique d'envoi de message à un destinataire
     * @param buffer le buffer contenant le message à envoyer
     * @param destinataire le destinataire du message
     * @throws IOException si une erreur survient lors de l'envoi du message
     */
    private static void envoyerMessage(byte[] buffer, InetSocketAddress destinataire) throws IOException {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, destinataire);
        socket.send(packet);
    }

    /**
     * Simplifie la logique de verification de pseudo.
     * Vérifie pour le moment si le pseudo n'est pas déjà utilisé, mais d'autres verifications pourront être necessaire.
     * @param pseudo le pseudo à verifier
     * @return true si le pseudo peut être utilisé, false sinon
     */
    private static boolean verifierPseudo(String pseudo) {
        return !clients.containsKey(pseudo);
    }

    /**
     * Tant que le socket principal est ouvert, cette fonction va bloquer le thread principal en écoutant toutes les requêtes provenant du socket principal.
     * Si une requête reçue a le format JOIN:<pseudo>, le client à être créé, un port et le thread correspondant lui sera attribué.
     */
    private static void ecouterRequetesJoin() {
        while (!socket.isClosed()) {
            try {
                // buffer d'écoute
                byte[] buffer = new byte[126];
                DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                System.out.println("[INFO] Prêt à recevoir des requêtes.");
                socket.receive(response);
                // conversion de la réponse en string
                String responseString = new String(response.getData(), 0, response.getLength()).trim();
                if (responseString.startsWith("JOIN:") && responseString.length() > 5) {
                    // si la réponse a le bon format, on commence le processus de création d'utilisateur
                    String pseudo = responseString.substring(5).trim();
                    System.out.println("[INFO] requête de JOIN de la part de " + pseudo);
                    ClientInfo client = new ClientInfo(pseudo, response.getAddress(), response.getPort());
                    if (verifierPseudo(pseudo)) {
                        // Si le pseudo est correct, ont créé le nouvel utilisateur
                        ajouterNouveauClient(client);
                    } else {
                        // Si le pseudo est incorrect, on notifie l'utilisateur
                        System.out.println("[INFO] le pseudo " + pseudo + " est déjà utilisé. La connexion est refusée.");
                        envoyerMessage(("Pseudo déjà utilisé").getBytes(), client.getInetSocketAddress());
                    }
                } else {
                    // si la réponse n'a pas le bon format, on notifie l'utilisateur
                    envoyerMessage(("La requête doit avoir le format \"JOIN:<pseudo>\"").getBytes(), new InetSocketAddress(response.getAddress(), response.getPort()) );
                }
            } catch (IOException e) {
                System.out.println("[ERREUR] une erreur est survenue lors de l'écoute des requêtes JOIN : " + e);
            }
        }
    }

    /**
     * Logique permettant de créer un nouveau socket pour le nouveau client, le notifier du port qui lui a été attribué,
     * l'ajouter aux clients connectés et démarrer son thead attribué.
     * @param nouveauClient les données du nouveau client.
     */
    private static void ajouterNouveauClient(ClientInfo nouveauClient) {
        try {
            DatagramSocket socketClient = new DatagramSocket();
            System.out.println("[INFO] le nouvel utilisateur " + nouveauClient.getPseudo() + " aura le port " + socketClient.getLocalPort());
            envoyerMessage(("PORT:"+socketClient.getLocalPort()).getBytes(), nouveauClient.getInetSocketAddress());
            clients.put(nouveauClient.getPseudo(), nouveauClient);
            GestionnaireClient gestionnaireClient = new GestionnaireClient(nouveauClient, socketClient, clients);
            Thread thread = new Thread(gestionnaireClient);
            thread.start();
            System.out.println("[INFO] le nouvel utilisateur " + nouveauClient.getPseudo() + " peut maintenant rejoindre le chat.");
        } catch (IOException e) {
            System.out.println("[ERREUR] impossible de créer un nouvel utilisateur pour " + nouveauClient.getPseudo() + " : " + e);
        }

    }
}
