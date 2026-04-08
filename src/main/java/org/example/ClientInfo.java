package org.example;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class ClientInfo {
    private String pseudo;
    private InetAddress adresseIp;
    private int port;

    public ClientInfo(String pseudo, InetAddress adresseIp, int port) {
        this.pseudo = pseudo;
        this.adresseIp = adresseIp;
        this.port = port;
    }

    public InetAddress getAdresseIp() {
        return adresseIp;
    }

    public void setAdresseIp(InetAddress adresseIp) {
        this.adresseIp = adresseIp;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public InetSocketAddress getInetSocketAddress() {
        return new InetSocketAddress(adresseIp, port);
    }
}
