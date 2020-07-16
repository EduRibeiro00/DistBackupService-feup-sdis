package peer.task;

import peer.chord.ChordNode;

public interface Task {
    void complete(ChordNode successor);
}

