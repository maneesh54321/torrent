package com.lib.torrent;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;

public class HttpOverUDP {
    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);

        // Step 1:Create the socket object for
        // carrying the data.
        DatagramSocket ds = new DatagramSocket();

        InetAddress ip = InetAddress.getLocalHost();

        SocketAddress socketAddress = new InetSocketAddress("localhost", 8080);

        StringBuilder sb = new StringBuilder();
        byte buf[];

        System.out.println("Enter the message now!!");
//        int count = 0;
//        // loop while user not enters "bye"
//        while (count<2)
//        {
//            String inp = sc.nextLine();
//            sb.append(inp);
//            sb.append("\n");
//            count++;
//        }
//        sb.append("\n");
        sb.append("GET /hello HTTP/1.1\r\n");
        sb.append("Host: 127.0.0.1\r\n\r\n");

        System.out.println("Message is: \n" + sb.toString() + "\nSending now..");

        // convert the String input into the byte array.
        buf = sb.toString().getBytes();

        // Step 2 : Create the datagramPacket for sending
        // the data.
        DatagramPacket DpSend =
                new DatagramPacket(buf, buf.length, socketAddress);

        // Step 3 : invoke the send call to actually send
        // the data.
        ds.send(DpSend);
    }
}
