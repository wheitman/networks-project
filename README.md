[toc]

## Objective

Build a networking application using Java sockets. Specifically, the application should have a central Math server and 2+ clients.

## Javadocs
Javadocs are available at [https://heit.mn/networks-project](https://heit.mn/networks-project/protocol/package-summary.html)

## Usage
1. Start the server with `java server.Main`
2. Start a client with `java client.Main`
3. Follow the prompts for the client. It will ask for your username and desired answer precision.
4. Send a math expression and wait for a response.
5. Repeat steps 2-4 with an arbitrary number of clients.

### Example expressions
- `2+4-3/(4+5*(3-4))`
- `2*sqrt(3)`
- `(3^2)^3`

## Requirements
- [x] The server keeps track of all users, including
    - Who
    - When
    - How long the user has been connected
- [x] Server should wait for the client's requests
- [x] Log all details about the client upon connection
- [x] Multiple client connections at once
- [x] Accept **string** request containing math calculation and show who sent the request
- [x] Respond to client requests in FIFO order
- [x] The server should close the connection when the client requests, then log it.
- [x] The client should provide their name upon connection, then wait for acknowledgment from the server.
- [x] After the above, the client should **send at least 3** basic math calculation requests to the server at **random times**.
- [x] At any point during this process, another client can/should join and send its own requests.
- [x] The client should then send a close connection request.

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

```
[[
    Seq:	1
    Status: success
]]
```

##### Response (error)

```
[[
   	Seq: 1
    Status: error
    Error: USERNAME_TAKEN
]]
```

### 3. Terminating a connection

##### Request

```
[[
    Seq: 54, 		// Example value
    Action: leave
]]
```

##### Response

```
[[
    Seq: 54, 		// Example value
    Status: success
]]
```

### 4. Sending and receiving math calculations

##### Request

```
[[
    Seq: 14, 		// Example value
    Action: calculate
    Expression: 53+2/14
    Precision: 3
]]
```

##### Response (success)

```
[[
    Seq: 14 		// Example value
    Status: success
    Answer: 53.143
]]
```

##### Response (error)

```
[[
   	Seq: 14
    Status: error
    Error: DIVIDE_BY_ZERO
]]
```

### 5. Server-side logs

Each connection will have its own separate log file, where the filename is of the format `username_yyyy-MM-dd-HH-mm-ss.log`, which specifies the connection time and username. Each request or response will be given a line in the log file.

#### Sample
```text
Dec. 05, 2022 6:14:16 PM server.Connection processRequest
INFO: Expression: 3+4/3.0, answer: 4.330000
Dec. 05, 2022 6:14:30 PM server.Connection processRequest
INFO: Expression: 2*3-(22.1*4), answer: -82.400000
Dec. 05, 2022 6:14:38 PM server.Connection processRequest
INFO: Expression: 63+0.12, answer: 63.120000
Dec. 05, 2022 6:14:40 PM server.Connection run
INFO: Client 'wheitman' with IP /127.0.0.1 disconnected at 2022/12/05 18:14:06, was connected for 0m33s
```
