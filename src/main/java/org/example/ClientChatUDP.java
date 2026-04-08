package org.example;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class ClientChatUDP {
    private int port;
    private InetAddress ip;
    ClientChatUDP() {
    }
    public void main(){
        String pseudo = demanderPseudo();
        try {
            DatagramSocket socket = rejoindreServeur(pseudo);
            if (socket.isClosed()) return;
            demarrerEcoute(socket);
            Scanner sc = new Scanner(System.in);
            while (true) {
                String texte = demanderSaisirMessage(sc);
                if (texte.equalsIgnoreCase("exit")) {;
                    arreterEcouter(socket);
                    break;
                }
                byte[] buf = texte.getBytes();
                DatagramPacket p = new DatagramPacket(buf, buf.length, this.ip, this.port);
                socket.send(p);
            }

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
    public String demanderPseudo(){
        Scanner sc = new Scanner(System.in);
        while(true){
            System.out.println("Veuillez saisir votre pseudo : ");
            String pseudo = sc.nextLine();
            if(pseudo.isBlank() || pseudo.length()<3){
                System.out.println("Le pseudo ne peut pas être vide. Veuillez réessayer.");
            } else {
                return pseudo;
            }
        }
    }
    public String  demanderSaisirMessage(Scanner sc){
        return sc.nextLine();
    }
    public void demarrerEcoute(DatagramSocket socket){
        ThreadListeningClient thread = new ThreadListeningClient(socket);
        thread.setDaemon(true);
        thread.start();

    }

    public void arreterEcouter(DatagramSocket socket) throws IOException {
        byte[] exitBuf = "EXIT".getBytes();
        DatagramPacket exitPacket = new DatagramPacket(exitBuf, exitBuf.length,ip, this.port);
        socket.send(exitPacket);
        socket.close();
        System.out.println("Application fermée avec succès.");

    }
    public DatagramSocket rejoindreServeur(String pseudo) throws IOException {
        InetAddress inet= InetAddress.getLocalHost();
        DatagramSocket socket = new DatagramSocket();
        String message= "JOIN: " + pseudo;
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, inet, 9000);
        socket.send(packet);
        byte[] receptionBuffer = new byte[1024];
        DatagramPacket receptionPacket = new DatagramPacket(receptionBuffer, receptionBuffer.length);
        System.out.println("En attente de la confirmation du serveur...");
        socket.receive(receptionPacket);
        String confirmation = new String(receptionPacket.getData(), 0, receptionPacket.getLength());
        if (confirmation.startsWith("PORT:")) {
            String portString= confirmation.substring(6).trim();
            this.port = Integer.parseInt(portString);
            this.ip= inet;
            System.out.println("Connexion réussie ! Port attribué : " + this.port);
            return socket;
        } else {
            System.out.println("Erreur de connexion : " + confirmation);
            socket.close();
            return socket;
        }
    }
}
