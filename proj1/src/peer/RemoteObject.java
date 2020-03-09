package peer;

/**
 * Class that implements the remote interface and specifies the procedures executed for each possible client request.
 */
public class RemoteObject implements RemoteInterface {


    @Override
    public String test(String testString) {
        String str = "Ya, recebi. A string foi: " + testString;
        return str;
    }
}
