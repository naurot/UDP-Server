import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Scanner;

public class Main {
    static int delay;

    public static void main(String[] args) throws Exception {
        int port = 11122;
        byte[] receiveBuffer = new byte[1029]; //doesn't matter - just to get string for http request

        Scanner cin = new Scanner(System.in);
        System.out.print("Enter a timeout ms: ");
        delay = Integer.parseInt(cin.nextLine());

        DatagramSocket datagramSocket = new DatagramSocket(port, InetAddress.getLocalHost());
        System.out.println("Listening on port: " + datagramSocket.getLocalPort());

        while (true) {

            DatagramPacket request = new DatagramPacket(receiveBuffer, 1029);
            datagramSocket.receive(request);

            //run in thread
            Thread t = new Thread(new MyRunnable(request));
            t.start();
        }
    }

    public static class MyRunnable implements Runnable {
        int port, packetLength;
        InetAddress ip;
        byte[] data;
        DatagramSocket socket;
        MessageType thing1;
        String msg;

        public MyRunnable(DatagramPacket dp) throws SocketException {
            port = dp.getPort();
            ip = dp.getAddress();
            data = dp.getData();
            thing1 = new MessageType(new String(data).substring(MessageType.OVERHEAD));
            packetLength = extractLength(dp.getData());
            msg = extractMsg(dp.getData(), packetLength);
            socket = new DatagramSocket();
            socket.setSoTimeout(delay);
        }

        public void run() {
            DatagramPacket packetS, packetR;
            byte[] response = new byte[1029];
            byte[] receive = new byte[1029];
            byte[] length = ByteBuffer.allocate(4).putInt(1029).array();
            System.out.println("Connected: " + ip + ":" + port);
            System.out.println("\tmsg: " + msg);
            StringBuilder webPage = new StringBuilder();
            //make HTTP request to 'data'
            String webResp;
            try {
                URL url = new URL("https://" + msg);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("GET");
                InputStreamReader inputStreamReader = new InputStreamReader(httpURLConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                webResp = bufferedReader.readLine();
                while (webResp != null) {
                    webPage.append(webResp);
                    webResp = bufferedReader.readLine();
                }
                bufferedReader.close();
                thing1 = new MessageType(webPage.toString());
                thing1.makeByteArrayData();
                //send to client in packet payload size 1024 with seqNum
//                boolean finished = false;
                byte seq = 1;
                String line;
                byte[] lineB = new byte[1024];
                int i;
                for (i = 0; i < webPage.length() / 1024; i++) {
                    line = webPage.substring(i * 1024, (i + 1) * 1024);
                    System.out.println(line);
                    lineB = line.getBytes();
                    seq = (byte) (seq ^ 1);
                    response[0] = seq;
                    response[1] = length[0];
                    response[2] = length[1];
                    response[3] = length[2];
                    response[4] = length[3];
                    for (int j = 0; j < 1024; j++)
                        response[j + 5] = lineB[j];
                    packetS = new DatagramPacket(response, 1029, ip, port);
                    System.out.println("Sending Seq#: " + response[0]);
                    socket.send(packetS);
                    try {
                        packetR = new DatagramPacket(receive, 1029, ip, port);
                        System.out.println("\tWaiting for ack" + response[0]);
                        socket.receive(packetR);
                        System.out.println("\tReceived ack" + packetR.getData()[0] + ", expected ack" + response[0]);
                    } catch (SocketTimeoutException e) {
                        System.out.println("ServerTimeout");
                    }
                }
                //handle the excess
                line = webPage.substring(i * 1024, (webPage.length() % 1024) + i * 1024);
                seq = (byte) (seq ^ 1);
                response[0] = seq;
                response[1] = length[0];
                response[2] = length[1];
                response[3] = length[2];
                response[4] = length[3];
                for (int j = 0; j < 1024; j++)
                    response[j + 5] = lineB[j];
                packetS = new DatagramPacket(response, 1029, ip, port);
                System.out.println("Sending Seq#: " + response[0]);
                socket.send(packetS);
                try {
                    packetR = new DatagramPacket(receive, 1029, ip, port);
                    System.out.println("\tWaiting for ack" + response[0]);
                    socket.receive(packetR);
                    System.out.println("\tReceived ack" + packetR.getData()[0] + ", expected ack" + response[0]);
                    while (packetR.getData()[0] != response[0]) {

                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("ServerTimeout");
                }
                System.out.println("DONE?");
            } catch (Exception e) {
                System.out.println("Error: " + e);
                e.printStackTrace();
            }
        }

        public static byte extractSeq(byte[] data) {
            return data[0];
        }

        public static int extractLength(byte[] data) {
            byte[] tmp = new byte[4];
            tmp[0] = data[1];
            tmp[1] = data[2];
            tmp[2] = data[3];
            tmp[3] = data[4];
            return new BigInteger(tmp).intValue();
        }

        public static String extractMsg(byte[] data, int packetLength) {
            String packet = new String(data);
            packet = packet.substring(5, packetLength);
            return packet;
        }
    }
}
