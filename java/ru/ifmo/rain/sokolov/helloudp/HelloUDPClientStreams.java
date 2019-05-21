package ru.ifmo.rain.sokolov.helloudp;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;


public class HelloUDPClientStreams extends HelloUDPStreams {

    private final InetSocketAddress serverAddress;
    private DatagramPacket receivePacket;

    public HelloUDPClientStreams(InetAddress address, int port, DatagramSocket socket) throws SocketException {
        super(socket);
        this.serverAddress = new InetSocketAddress(address, port);
        this.receivePacket = createReceivePacket();
    }

    @Override
    protected DatagramPacket getReceivePacket() {
        return receivePacket;
    }

    public void sendString(String requestMsg) throws IOException {
        //sendString(requestMsg, serverAddress);
        byte[] sendBuffer = requestMsg.getBytes(StandardCharsets.UTF_8);
        receivePacket.setData(sendBuffer);
        receivePacket.setSocketAddress(serverAddress);
        socket.send(receivePacket);
        resetAndResize();
    }

    public void resetAndResize() throws SocketException {

        receivePacket.setData(new byte[socket.getReceiveBufferSize()]);
    }
}