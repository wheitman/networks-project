[toc]

## Objective

Build a networking application using Java sockets. Specifically, the application should have a central Math server and 2+ clients.

## Requirements
- [ ] The server keeps track of all users, including
    - Who
    - When
    - How long the user has been connected
- [ ] Server should wait for the client's requests
- [ ] Log all details about the client upon connection
- [x] Multiple client connections at once
- [ ] Accept **string** request containing math calculation and show who sent the request
- [ ] Respond to client requests in FIFO order
- [ ] The server should close the connection when the client requests, then log it.
- [ ] The client should provide their name upon connection, then wait for acknowledgment from the server.
- [ ] After the above, the client should **send at least 3** basic math calculation requests to the server at **random times**.
- [ ] At any point during this process, another client can/should join and send its own requests.
- [ ] The client should then send a close connection request.

The application terminates when all connections are closed.

## Protocol design
We'll use a JSON API.

### 1. General process
1. Server waits for incoming connections with a server socket on port 3141
2. A client connects to this server socket and sends a join request.
3. The server sends a join response.
4. The client sends an unlimited number of math requests and receives responses in order.
5. Once the client sends a leave request, the server acknowledges it and closes the connection.

From step 2 onward, the server will create and maintain a log file on a per-connection basis.

### 2. Joining a connection
To establish a connection, the client and server must first perform a simple handshake. We're aware that the client's IP address and port number can serve as an identifier, and that this data can be read with `Socket.getInetAddress()` and `Socket.getPort()`. However, for the purposes of modeling our own protocol, we provide **a fake username** in the handshake that will serve as the ID.

##### Request

```
[[
   	Username: jsmith
    Seq:	1
    Action: join
]]
```
##### Response (success)

```json
[[
    Seq:	1
    Status: success
]]
```

##### Response (error)

```json
[[
   	Seq: 1
    Status: error
    Error: USERNAME_TAKEN
]]
```

### 3. Terminating a connection

##### Request

```json
[[
    Seq: 54, 		// Example value
    Action: leave
]]
```

##### Response

```json
[[
    Seq: 54, 		// Example value
    Status: success
]]
```

### 4. Sending and receiving math calculations

##### Request

```json
[[
    Seq: 14, 		// Example value
    Action: calculate
    Expression: 53+2/14
    Precision: 3
]]
```

##### Response (success)

```json
[[
    Seq: 14 		// Example value
    Status: success
    Answer: 53.143
]]
```

##### Response (error)

```json
[[
   	Seq: 14
    Status: error
    Error: DIVIDE_BY_ZERO
]]
```

### 5. Server-side logs

Each connection will have its own separate log file, where the filename is of the format `yyyy-MM-dd-HH-mm-username.log`, which specifies the connection time and username. Each request or response will be given a line in the log file.

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