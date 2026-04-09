package org.example;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class ClientChatUDP {

    private int portServeur;
    private InetAddress addressServeur;
    private DatagramSocket socket;
    private String pseudo;

    void main() {
        while (socket == null || socket.isClosed()) {
            demanderPseudo();
            rejoindreServeur(pseudo);
        }

        demarrerEcouteMessagesEntrant();

        System.out.println("\n Vous pouvez maintenant commencer à envoyer des messages.");
        while (!socket.isClosed()) {
            String message = demanderMessage();
            if (message.equalsIgnoreCase("exit")) {
                deconnexion();
            } else if (message.startsWith("/")) {
                envoyerMessage(message);
            } else {
                envoyerMessage(pseudo + " : " + message);
            }
        }
    }

    /**
     * Demande à l'utilisateur de saisir un pseudo, et mets à jour la variable locale
     */
    private void demanderPseudo() {
        Scanner sc = new Scanner(System.in);
        this.pseudo = "";
        while (this.pseudo.isEmpty()) {
            System.out.print("Veuillez saisir votre pseudo : ");
            pseudo = sc.nextLine();
            if (pseudo.isEmpty()) {
                System.out.println("Le pseudo ne peut pas être vide. Veuillez réessayer.");
            }
        }
    }

    /**
     * Demande à l'utilisateur de saisir un message
     *
     * @return le string non vide
     */
    private String demanderMessage() {
        Scanner sc = new Scanner(System.in);
        String message = "";
        while (message.isEmpty()) {
            message = sc.nextLine();
        }
        return message;
    }

    /**
     * Ecoute le socket pour tous les messages entrant, et les affiche. Fonctionne dans son propre thread
     */
    private void demarrerEcouteMessagesEntrant() {

        //Thread local en passant le Runnable directement en paramètre, simplifie le code
        Thread thread = new Thread(() -> {
            //On écoute tant que le socket est ouvert
            while (!socket.isClosed()) {
                try {
                    byte[] buffer = new byte[1024];
                    DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                    socket.receive(response);
                    String msg = new String(response.getData(), 0, response.getLength());
                    if (msg.equalsIgnoreCase("TIMEOUT")) {
                        System.out.println("\nTIMEOUT");
                        socket.close(); // On ferme le socket [cite: 71]
                        System.exit(0);
                    } else  {
                        System.out.println("\n" + msg);
                    }
                } catch (IOException e) {
                    System.out.println("Une erreur s'est produite lors de l'écoute des messages : " + e);
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();
    }

    private void envoyerMessage(String message) {
        try {
            byte[] buf = message.getBytes();
            DatagramPacket p = new DatagramPacket(buf, buf.length, this.addressServeur, this.portServeur);
            socket.send(p);
        } catch (IOException e) {
            System.out.println("Le message n'a pas pu être envoyé : " + e);
        }
    }

    /**
     * Envoie un message exit pour arrêter le thread coté serveur, puis ferme le socket
     */
    private void deconnexion() {
        // Envoi le message EXIT, puis ferme le socket dans tous les cas.
        try {
            byte[] exitBuf = "EXIT".getBytes();
            DatagramPacket exitPacket = new DatagramPacket(exitBuf, exitBuf.length, this.addressServeur, this.portServeur);
            socket.send(exitPacket);
        } catch (IOException e) {
            System.out.println("AVERTISSEMENT : l'envoie de la requête EXIT a échoué.");
        }
        socket.close();
        System.out.println("A bientôt !");

    }

    /**
     * Permet de démarrer la connexion avec le serveur, en partageant le pseudo et en se connectant au bon port
     *
     * @param pseudo le pseudo avec lequel se connecter
     */
    private void rejoindreServeur(String pseudo) {
        try {
            // récupération de l'adresse ip du serveur (dans notre cas localhost), et création du socket
            addressServeur = InetAddress.getLocalHost();
            socket = new DatagramSocket();

            // Envoi du message JOIN avec le pseudo au serveur
            String message = "JOIN: " + pseudo;
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, addressServeur, 9000);
            socket.send(packet);

            // Reception de la réponse du serveur
            byte[] receptionBuffer = new byte[1024];
            DatagramPacket receptionPacket = new DatagramPacket(receptionBuffer, receptionBuffer.length);
            System.out.println("En attente de la confirmation du serveur...");
            socket.receive(receptionPacket);
            String reponse = new String(receptionPacket.getData(), 0, receptionPacket.getLength());

            // Si la réponse du serveur commence avec PORT, la connexion a réussi et on se connecte
            if (reponse.startsWith("PORT:")) {
                //Récupération du numéro de port
                String portString = reponse.substring(5).trim();
                this.portServeur = Integer.parseInt(portString);
                System.out.println("Connexion réussie ! Port attribué : " + this.portServeur);
            } else {
                System.out.println("Erreur de connexion : " + reponse);
                socket.close();
            }
        } catch (Exception e) {
            System.out.println("La connexion avec le serveur a échouée : " + e);
        }

    }
}
