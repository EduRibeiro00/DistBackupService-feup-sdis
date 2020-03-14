package peer;

import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.Callable;

public class BackupProtocolThread implements Runnable {
    MulticastSocket mControl, mBackup, mRecovery;
    Queue<DatagramPacket> queue;

    public BackupProtocolThread(MulticastSocket mControl, MulticastSocket mBackup,
                                MulticastSocket mRecovery, Queue<DatagramPacket> queue) {
        this.mControl = mControl;
        this.mBackup = mBackup;
        this.mRecovery = mRecovery;
        this.queue = queue;
    }

    @Override
    public void run() {
        DatagramPacket pkt;
        while (true) {
//            Create mutex for queue size
//            Create mutex for queue accessing
            pkt = this.queue.remove();


            this.processPacket(pkt);

        }
    }

    private void processPacket(DatagramPacket pkt){
        byte[] buf = pkt.getData();

        String data = Arrays.toString(buf);

    }

}
