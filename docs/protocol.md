# Application Protocol

- [Commands](#commands)
  - [Join the Sever](#join-the-sever)
  - [Prepare the Game](#prepare-the-game)
  - [Start the Game](#start-the-game)
  - [Update the Progress](#update-the-progress)
  - [End the Game](#end-the-game)
  - [Quit the Sever](#quit-the-sever)
  - [Sequence Diagram](#sequence-diagram)
  - [General Protocol](#general-protocol)
  - [Client Error when Joining the Server](#client-error-when-joining-the-server)

## Overview

The "Fastpaws" protocol is a communication protocol that allow multiple clients to compete in a typing race.
## Transport protocol
```note
TODO : Remove these after validation that every question is answered :
    What protocol(s) is/are involved? On which port(s)?
    How are messages/actions encoded?
    How are messages/actions delimited?
    How are messages/actions treated (text or binary)?
    Who initiates/closes the communication?
    What happens on an unknown message/action/exception?
```
The "Fastpaws" protocol is a text transport protocol. It uses UDP transport protocol as we do not require reliability.

Every message is encoded in UTF-8 and delimited by a space character. They are treated as text messages.

For every port and address mentioned below, we specify the default value but these can be changed using options of the command lines.

When starting the server, it listens for unicast UDP messages on http://localhost:4445

The server will send messages using multicast with UDP fire-and-forget pattern /230.0.0.0:4446 
(we always refer to this when talking about the server sending multicast request below), this communication doesn't need to be initiated.

### Join server

The client initiate the communication using unicast with UDP request-response pattern on the server port : 4445.

This first request is asking to join the game with a given username. 

The server has to verify if that username isn't already taken. 

If the username is valid, the server stores it and responds with a list of all the other usernames of clients waiting to play
and indicating if they're ready or not. 
It will also send a multicast request to inform that a new client joined providing his username.

If it is not valid, it responds by asking the client to choose another username until a valid one is proposed.

After succeeding in joining the game, the client closes the unicast communication and join a multicast group to listen for multicast UDP messages on /230.0.0.0:4446.

### Prepare the game

The client initiate a request to indicate that he is ready to compete using unicast UDP fire-and-forget pattern on the server port : 4445.

The server send a multicast request to inform that this client is now ready.

### Start the game

Once the required number of ready clients is reached (by default 2), 
the server send a mulitcast request to inform that the game is starting by sending the text to type.

### Update the progress

The clients initiate a request at every valid keypress to send their progress using unicast UDP fire-and-forget pattern on the server port : 4445.

If one client reached 100%, the server send a multicast request indicating that the game is over and informing of the username of the winner.

Every 2 seconds, The server send a multicast request with the usernames and progress of all clients.

### Quit the server 

At any time, a client can send a request to quit the game using unicast UDP fire-and-forget pattern on the server port : 4445.

The server send a multicast request to inform a client left, indicating his username.

It also checks if all the clients left, and end the game if it is the case.

When the game end, every clients go back to the preparation of the game step where they are asked if they are ready, we can start a new game.

### Error

For every request sent by the client, if we have an unknown message/illegal number of arguments/unknown action/exception 
the server must send a unicast UDP response with an error message to the client indicating what the issue is. 
This of course uses the address and port of the client that initiated the communication.

## Messages

- `name` the username of the client
- `progress` is defined as the percentage of completed letters.
- `status` is defined as the player state the possible values being : not ready, ready and in game.

### Join the Sever

The client send a message to the servet indicating his username.

**Request:**

```
USER_JOIN <name>
```

**Response:**

```
OK <name1> <status1> <name2> <status2> : the client joined successfully, the list of connected client and their status
USER_JOIN_ERR <messsage> : an error occured when trying to join, the message describe what happended (ex : username already taken.)
```
The client is responsible for displaying the information about the other clients.

If the client joined successfuly, the server send a multicast request to inform a new client joinned.

**Request:**

```
NEW_USER <name>
```

After succesfully connecting, the client join the multicast group. 

### Prepare the Game 

The client initiates a request to indicate he is ready.

**Request:**

```
USER_READY <name>
```

**Response:**

```
No response.
```

The server send a multicast request to inform that this client is ready.

**Request:**

```
USER_READY <name>
```

### Start the Game 

The server send a multicast request to inform that the game starts and provide the text to type.

**Request:**

```
START_GAME <text>
```
The client is responsible for displaying the text and input of the player.

### Update the Progress

The client initiates a request to send his progress at every keypress, it is responsible for calculating the progress.

**Request:**

```
USER_PROGRESS <name> <progress> 
```

**Response:**

```
No response.
```

Every 5 seconds, the server send a multicast request to inform of the progress of each client.
```
ALL_USERS_PROGRESS <name1> <progress1> <name2> <progress2>  ...
```
The client is responsible for displaying the progress of all other clients.

### End the Game 

When a client reached 100%, the server send a multicast request to inform the game is over and indicate who the winner is.

**Request:**

```
END_GAME <winner_name>
```
- `winner_name` : username of the winning client

The client responsible for displaying the game over screen and the winner username.

### Quit the Sever

The client send a request to quit the server.

**Request:**

```
USER_QUIT <name>
```

**Response:**

```
No response.
```

The server send a multicast request to inform a client left.

**Request:**

```
DEL_USER <name>
```
The client is responsible updating the display of the current clients in game, by removing him.

### General error message

For all generic errors, the server can respond to a request with :

**Response:**

```
ERROR <errorMessage>
```
- `errorMessage` : specify what the issue is.

## Examples

### General Protocol

![Sequence Diagram](./diagram.svg)

### Client Error when Joining the Server

![Sequence Diagram with Error](./error-diagram.svg)


```staruml
@startuml
actor Client1
actor Client2
actor Server

== Joining the Server ==

Client1 -> Server: USER_JOIN <name>
Server -> Client1: OK
Server -> Client2: USER_NEW <name>

== Preparing the Game ==

Client1 -> Server: USER_READY <name>
Server -> Client2: USER_READY <name>

== Starting the Game ==

Server -> Client1: START_GAME <text> ...
Server -> Client2: START_GAME <text> ...

== Updating Progress ==

Client1 -> Server: USER_PROGRESS <name> <progress>
Server -> Client1: USERS_PROGRESS <name> <progress> <name> <progress> ...
Server -> Client2: USERS_PROGRESS <name> <progress> <name> <progress> ...

== Ending the Game ==

Server -> Client1: END_GAME <winner_name>
Server -> Client2: END_GAME <winner_name>

== Quitting the Server ==

Client1 -> Server: USER_QUIT <name>
Server -> Client2: USER_QUIT <name>
@enduml
```

```
@startuml
actor Client1
actor Server

== Joining the Server (Error) ==

Client1 -> Server: USER_JOIN <name>
Server -> Client1: USER_JOIN_ERR <msg>
@enduml
```