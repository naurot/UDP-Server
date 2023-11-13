import java.math.BigInteger;
import java.nio.ByteBuffer;

public class MessageType {
    static final int MAX_SIZE = 1024;
    static final int OVERHEAD = 5;
    String msg;
    int seqNum, lengthOfData, lengthOfMsg, start;
    byte[] data, length;
    byte seq;

    public MessageType() {

    }

    public MessageType(String s) {
        msg = s;
        seqNum = 1;
        seq = (byte) seqNum;
        data = new byte[MAX_SIZE + OVERHEAD];
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
