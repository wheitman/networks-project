package protocol;

public class Response {
    public int seq; // Sequence number of the request-response pair
    public Status status;
    public Error error;
    public float answer; // Answer to math expression
}
