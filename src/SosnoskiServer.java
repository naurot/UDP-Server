import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Scanner;

public class SosnoskiServer {
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
            DatagramPacket packetS, packetR = null;
//            byte[] response = new byte[1029];
            byte[] receive = new byte[1029];
//            byte[] length = ByteBuffer.allocate(4).putInt(1029).array();
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

                FileWriter fout = new FileWriter(("something1.html"));
                fout.write(webPage.toString());
                fout.close();

                bufferedReader.close();
                thing1 = new MessageType(webPage.toString());
                //send to client in packet payload size 1024 with seqNum
                String firstWord;
                while (thing1.hasNextByteArrayData()) {
                    thing1.makeByteArrayData();
                    firstWord = "Sending";
                    packetS = new DatagramPacket(thing1.getByteArrayData(), thing1.getPacketLength(), ip, port);
                    receive[0] = 2; // sets seqNum out of bounds - won't match anything
                    packetR = new DatagramPacket(receive, 1029, ip, port);
                    while (packetR.getData()[0] != thing1.getSeqNum()) {
                        System.out.println(firstWord + " Seq#" + thing1.getSeqNum());
                        System.out.println("\tPacketLength: " + thing1.getLength());
                        firstWord = "\t*** Resending";
                        socket.send(packetS);
                        try {
                            System.out.println("\tWaiting for ack. Expecting ack" + thing1.getSeqNum());
                            socket.receive(packetR);
                            System.out.println("\tReceived ack" + packetR.getData()[0]);
                        } catch (SocketTimeoutException e) {
                            System.out.println("\tSocketTimeoutException: " + e);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error: " + e);
                e.printStackTrace();
            }
            socket.close();
            System.out.println("Completed");
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

    public static class MessageType {
        static final int MAX_SIZE = 1024;
        static final int OVERHEAD = 5;// 1byte seqNum, 4 bytes length of message
        String msg;
        private int seqNum, lengthOfData, lengthOfMsg, start;
        byte[] data = new byte[MAX_SIZE + OVERHEAD];
        byte[] length;
        private byte seq;

        public MessageType() {

        }

        public MessageType(String s) {
            msg = s;
            seqNum = 1; // TODO ???
            seq = (byte) seqNum;
//            data = new byte[MAX_SIZE + OVERHEAD];
            start = 0;
        }

        public byte[] getByteArrayData() {
            return data;
        }

        public void makeByteArrayData() {
            incSeqNum();
            int end = Math.min(start + MAX_SIZE, msg.length());
            setLength(end - start);
            byte[] line = msg.substring(start, end).getBytes();
            start = start + MAX_SIZE;
            data[0] = seq;
            data[1] = length[0];
            data[2] = length[1];
            data[3] = length[2];
            data[4] = length[3];
            if (lengthOfMsg >= 0) System.arraycopy(line, 0, data, 5, lengthOfMsg);
            start = end;
        }

        public boolean hasNextByteArrayData() {
            return start < msg.length();
        }

        private void setLength(int length) {
            lengthOfMsg = length;
            lengthOfData = length + OVERHEAD;
            this.length = ByteBuffer.allocate(4).putInt(lengthOfData).array();
        }

        public int getPacketLength() {
            return lengthOfData;
        }

        public int getMsgLength() {
            return lengthOfMsg;
        }

        public int getLength() {
            return new BigInteger(length).intValue();
        }

        public int getSeqNum() {
            return seqNum;
        }

        private void incSeqNum() {
            seqNum = seqNum ^ 1;
            seq = (byte) seqNum;
        }
    }
}


