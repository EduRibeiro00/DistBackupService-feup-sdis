package peer.chord;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Class that represents a node/peer in the Chord protocol.
 */
public class ChordNode implements Comparable<ChordNode> {
    /**
     * Id of the node.
     */
    private int id;

    /**
     * IP address of the node.
     */
    private String ipAddress;

    /**
     * MC port of the node.
     */
    private int portMC;

    /**
     * MDB port of the node.
     */
    private int portMDB;

    /**
     * MDR port of the node.
     */
    private int portMDR;

    /**
     * Chord port of the node.
     */
    private int portChord;

    /**
     * Getter for the ID of the node.
     * @return ID of the node.
     */
    public int getId() {
        return id;
    }

    /**
     * Setter for the ID of the node.
     * @param id New ID of the node
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Getter for the IP address of the node.
     * @return The IP address of the node.
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Setter for the IP address of the node.
     * @param ipAddress The new IP address of the node.
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Getter for the MC port of the node.
     * @return The MC port of the node.
     */
    public int getPortMC() {
        return portMC;
    }

    /**
     * Setter for the MC port of the node.
     */
    public void setPortMC(int portMC) {
        this.portMC = portMC;
    }

    /**
     * Getter for the MDB port of the node.
     * @return The MDB port of the node.
     */
    public int getPortMDB() {
        return portMDB;
    }

    /**
     * Setter for the MDB port of the node.
     */
    public void setPortMDB(int portMDB) {
        this.portMDB = portMDB;
    }

    /**
     * Getter for the MDR port of the node.
     * @return The MDR port of the node.
     */
    public int getPortMDR() {
        return portMDR;
    }

    /**
     * Setter for the MDR port of the node.
     */
    public void setPortMDR(int portMDR) {
        this.portMDR = portMDR;
    }

    /**
     * Getter for the Chord port of the node.
     * @return The Chord port of the node.
     */
    public int getPortChord() {
        return portChord;
    }

    /**
     * Setter for the Chord port of the node.
     */
    public void setPortChord(int portChord) {
        this.portChord = portChord;
    }

    /**
     * Constructor of the Chord node.
     * @param ipAddress IP address of the Chord node
     * @param portMC MC port of the Chord node
     * @param portMDB MDB port of the Chord node
     * @param portMDR MDR port of the Chord node
     * @param portChord Chord port of the Chord node
     * @param id Id of the node
     */
    public ChordNode(String ipAddress, int portMC, int portMDB, int portMDR, int portChord, int id) {
        this.ipAddress = ipAddress;
        this.portMC = portMC;
        this.portMDB = portMDB;
        this.portMDR = portMDR;
        this.portChord = portChord;
        this.id = id;
    }

    /**
     * Method that compares two chord nodes.
     * @param that The other node to compare
     * @return Negative int if this < that, 0 if this == that, positive int if this > that
     */
    @Override
    public int compareTo(ChordNode that) {
        return Integer.compare(this.id, that.id);
    }

    /**
     * Override of the equals method, that compares IDs.
     * @param o The object to which we want to compare
     * @return True if they are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChordNode chordNode = (ChordNode) o;
        return id == chordNode.id;
    }

    /**
     * Override of the hash code method.
     * @return Hash code of the Chord node.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
