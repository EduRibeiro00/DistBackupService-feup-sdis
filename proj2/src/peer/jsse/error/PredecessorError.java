package peer.jsse.error;

import peer.chord.ChordRingInfo;

public class PredecessorError implements OnError {

    private ChordRingInfo ringInfo;

    public PredecessorError(ChordRingInfo ringInfo) {
        this.ringInfo = ringInfo;
    }

    @Override
    public void errorOccurred() {
        ringInfo.handlePredecessorError();
    }
}
