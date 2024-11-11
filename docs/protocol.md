# Application Protocol

- [Commands](#commands)
  - [Join the sever](#join-the-sever)
  - [Prepare the game](#prepare-the-game)
  - [Start the game](#start-the-game)
  - [Update the progress](#update-the-progress)
  - [End the game](#end-the-game)
  - [Quit the sever](#quit-the-sever)
- [Sequence Diagram](#sequence-diagram)

## Commands

- `score` is defined as number of letters completed

### Join the sever

The client intitiates the request.

**Request:**

```
USER_PROFILE <name>
```

**Response:**

```
USER_PROFILE OK
USER_PROFILE ERR <msg>
```

**Response to other all clients:**

```
USER_JOIN <name>
```

### Prepare the game 

The client intitiates the request.

**Request:**

```
USER_READY <name>
```

**Response to all other clients:**

```
USER_READY <name>
```

### Start the game 

The server intitiates the request.

**Request:**

```
START_GAME <text> ...
```

**Response:**

No response.

### Update the progress

The client intitiates the request at every valid keypress from the client.


**Request:**

```
USER_PROGRESS <name> <score> 
```

**Response:**

```
USERS_PROGRESS <name> <score> <name> <score> <name> <score> <name> <score> ...
```

### End the game 

The server intitiates the request.

**Request:**

```
END_GAME <winner_name>
```

**Response:**

No response.

### Quit the sever

The client intitiates the request.

**Request:**

```
USER_QUIT <name>
```

**Response to all other clients:**

```
USER_QUIT <name>
```

## Sequence Diagram

![Sequence Diagram](./diagram.svg)


```staruml
@startuml
actor Client1
actor Client2
actor Server

== Joining the Server ==

Client1 -> Server: USER_PROFILE <name>
Server -> Client1: USER_PROFILE OK / USER_PROFILE ERR <msg>
Server -> Client2: USER_JOIN <name>

== Preparing the Game ==

Client1 -> Server: USER_READY <name>
Server -> Client2: USER_READY <name>

== Starting the Game ==

Server -> Client1: START_GAME <text> ...
Server -> Client2: START_GAME <text> ...

== Updating Progress ==

Client1 -> Server: USER_PROGRESS <name> <score>
Server -> Client1: USERS_PROGRESS <name> <score> <name> <score> ...
Server -> Client2: USERS_PROGRESS <name> <score> <name> <score> ...

== Ending the Game ==

Server -> Client1: END_GAME <winner_name>
Server -> Client2: END_GAME <winner_name>

== Quitting the Server ==

Client1 -> Server: USER_QUIT <name>
Server -> Client2: USER_QUIT <name>
@enduml
```