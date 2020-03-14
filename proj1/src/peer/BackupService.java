package peer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class BackupService {
    MulticastSocket mControl, mBackup, mRecovery;

    public BackupService(String controlIpAddress, int controlPort, String backupIpAddress, int backupPort,
                               String recoveryIpAddress, int recoveryPort) throws IOException {

        this.mControl = new MulticastSocket(controlPort);
        this.mControl.joinGroup(InetAddress.getByName(controlIpAddress));

        this.mBackup = new MulticastSocket(backupPort);
        this.mBackup.joinGroup(InetAddress.getByName(backupIpAddress));

        this.mRecovery = new MulticastSocket(recoveryPort);
        this.mRecovery.joinGroup(InetAddress.getByName(recoveryIpAddress));
    }




}
