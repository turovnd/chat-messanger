# Chat messenger
Chat messenger is an application that have JARs files for creating server and clients applications.

## Getting started
Run server app: `java -jar jars/server.jar`

Run client app: `java -jar jars/client.jar`

## Application features

### Server
Functions:
- Logging all connection (open/close). 
- Logging clients messages, commands, sending and downloading files.
- Send message to all clients.
- Broadcast client message to other clients.
- Avaliable commands:
  - Close application: `/exit` or `/q`
  - Load messages histiory: `/history` or `/h`
  - Load files list: `/files`
  - Show connections and clients info: `/info`

The full command for executing server application:
```$commandline
java -jar jars/server.jar dbClear=true dbName=database dirPath=/tmp port=9093
```

Parameter | Default value | Description
--- | --- | ---
dbClear | true | Clear database when server has started.
dbName | database | Database file name (to where data will store).
dirPath | /tmp | Directory to where files will placed when they will received.
port | 9093 | Port on which server will listen new connections.


### Client
Functions:
- Retrieving new messages from server.
- Send message to other clients.
- Send file to server.
- Avaliable commands:
  - Show online clients names: `/online` or `/o`
  - Load messages histiory: `/history` or `/h`
  - Load files list: `/files`
  - Close application: `/exit` or `/q`
  - Download file from server: `/download file_name`

The full command for executing client application:
```$commandline
java -jar jars/client.jar maxFileSize=100 dirPath=/tmp port=9093
```
Parameter | Default value | Description
--- | --- | ---
maxFileSize | 100 | Maximum file size that can be send to server.
dirPath | /tmp | Directory to where files will placed when they will downloaded.
port | 9093 | Port to which client will connect.
