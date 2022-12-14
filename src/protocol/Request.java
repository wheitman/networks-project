package protocol;

public class Request {
    public String username;
    public int seq; // Sequence number of the request-response pair
    public Action action;
    public String expression; // Math expression, e.g. "53+2/14"
    public int precision; // Desired number of decimal places in answer

    public String toString() {

        return """
                [[
                    Username: %s
                    Seq: %d
                    Action: %s
                    Expression: %s
                    Precision: %d
                ]]
                """.formatted(
                username,
                seq,
                action,
                expression,
                precision
        ).strip();
    }
}

