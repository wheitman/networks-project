package protocol;

public class Response {
    public int seq = -1; // Sequence number of the request-response pair
    public Status status = Status.SUCCESS;
    public Error error = Error.NONE;
    public float answer; // Answer to math expression
}
