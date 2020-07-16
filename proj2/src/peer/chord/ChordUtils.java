package peer.chord;

/**
 * Class containing utility functions for Chord.
 */
public class ChordUtils {
    public static boolean isBetweenInc(int lower, int upper, int key) {
        if (lower < upper) {
            return key > lower && key <= upper;
        } else {
            return key > lower || key <= upper;
        }
    }

    public static boolean isBetween(int lower, int upper, int key) {
        if (lower < upper) {
            return key > lower && key < upper;
        } else {
            return key > lower || key < upper;
        }
    }
}
