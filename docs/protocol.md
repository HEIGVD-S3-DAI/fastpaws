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
```note
TODO : Remove these after validation that every question is answered :
    What is the goal of the protocol?
    What is the problem that it tries to solve?
    What the application protocol is used for?
```
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

When starting the server, it listens for unicast UDP messages on host and port that can be determined in command options,
by default it is listening on http://localhost:4445

The client initiate the communication using unicast with UDP request-response pattern on the server port, by default set to 4445.

This first request is asking to join the game with a given username. 

The server has to check if the game has already started.

If it is still waiting for player :

The server has to verify if that username isn't already taken. 

If the username is valid, the server stores it and responds with a confirmation.

After succeeding in joining the game the client starts listening for multicast UDP messages on the server multicast port, by default set to 4446 and multicast group by default 230.0.0.0.

The server also informs the other clients that a new one joined, using multicast UDP fire-and-forget pattern on the server multicast port, by default set to 4446.

If it is not valid,  it responds by asking the client to choose another username until a valid one is proposed.

If it is already running :

The server responds telling the client to wait. 
The client starts listening for multicast UDP messages on the server multicast adress and port, by default set to 230.0.0.0:4446.
And then he go back to the first step to try joining the next game.

//TODO : Est-ce que c'est vraiment "initier" une nouvelle communication ou c'est la même que le join ??
Pour moi c'est la communication qui est "initiée" les requêtes sont "request"//

The client initiate a request to indicate he is ready to compete using unicast UDP request-response pattern on the server port, by default set to 4445.

The server has to respond with the usernames of every ready client that joined the game before.

The server also informs the other clients who already are ready that this client is now ready, using multicast UDP fire-and-forget pattern on the server multicast port, by default set to 4446.

Once the required number of ready clients is reached (by default 2), 
the server initiate the request to start the game sending the text to type using multicast UDP fire-and-forget pattern on the server multicast port, by default set to 4446.

The clients initiate a request at every valid keypress to send their progress using unicast UDP fire-and-forget pattern on the server port, by default set to 4445.

//TODO : Since this is fire-and-forget actually this isn't a response, this is a multicast from the server. 
The check of 100% and multicast of end game should be in the multicastprogress function that is independent
of any requests sent by the client.//

// Shoudln't be here !!!

The server responds with the usernames and progress of all clients.

It also checks if one client reached 100%, if so multicast to all clients that the game ended and give the username of the winner.
//

//Should be this instead !!! (implies a small code modification.)

Every two second, the server initiate a multicast UDP request to inform all clients of everyone progress.

It also checks if one client reached 100% and if so intiate a request to multicast that the game ended, indicating who won.
//

At any time, a client can quit the game which will close the communication.

The server multicast the username of the client that left to all other clients.

It also checks if all the clients left, and end the game if it is the case.

When the game end, every clients go back to the step where they are asked if they are ready, we can start a new game.

For every request, if we have an unknown message/illegal number of arguments/action/exception the server must send an error message to the client. With information about what went wrong.


## Messages

- `progress` is defined as the percentage of completed letters.

### Join the Sever

The client intitiates the request.

**Request:**

```
USER_JOIN <name>
```

**Response:**

```
OK
USER_JOIN_ERR <msg>
WAIT
```

**Response to other all Clients:**

```
NEW_USER <name>
```

### Prepare the Game 

The client intitiates the request.

**Request:**

```
USER_READY <name>
```

**Response:**

```
CURRENT_USERS_READY <name> <name> <name> ...
```

**Response to all other clients:**

```
USER_READY <name>
```

### Start the Game 

The server intitiates the request.

**Request:**

```
START_GAME <text> ...
```

**Response:**

No response.

### Update the Progress

The client intitiates the request at every valid keypress from the client.


**Request:**

```
USER_PROGRESS <name> <progress> 
```

**Response:**

```
ALL_USERS_PROGRESS <name> <progress> <name> <progress> <name> <progress> <name> <progress> ...
```

### End the Game 

The server intitiates the request.

**Request:**

```
END_GAME <winner_name>
```

**Response:**

No response.

### Quit the Sever

The client intitiates the request.

**Request:**

```
USER_QUIT <name>
```

**Response to all other clients:**

```
DEL_USER <name>
```

### General error message

For all generic errors, the server can send:

**Request:**

```
ERROR <errorMessage>
```

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