### Objective
Build a networking application using Java sockets. Specifically, the application should have a central Math server and 2+ clients.

### Requirements
- [ ] The server keeps track of all users, including
    - Who
    - When
    - How long the user has been connected
- [ ] The server should wait for the client's requests and log all details about the client upon connection
- [ ] Multiple client connections at once
- [ ] Accept **string** request containing math calculation and show who sent the request
- [ ] Respond to client requests in FIFO order
- [ ] The server should close the connection when the client requests, then log it.
- [ ] The client should provide their name upon connection, then wait for acknowledgment from the server.
- [ ] After the above, the client should **send at least 3** basic math calculation requests to the server at **random times**.
- [ ] At any point during this process, another client can/should join and send its own requests.
- [ ] The client should then send a close connection request.

The application terminates when all connections are closed.

### Protocol design
We'll use a JSON API.

#### Sample request
```json
{
  "HOST" : "ClientA",
  "EXPRESSION" : "(3+4)/2.2",
  "DECIMAL_PLACES" : 3
}
```
#### Sample response (success)
```json
{
  "ANSWER" : "3.182"
}
```

#### Sample response (exception)
```json
{
  "ANSWER" : "DivideByZeroException"
}
```