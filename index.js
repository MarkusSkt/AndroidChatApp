const express = require('express'),
http = require('http'),
app = express(),

server = http.createServer(app),
io = require('socket.io').listen(server);

// Server listens for connections
server.listen(3000,() => {
  console.log('Node Server is listening on port 3000');
});

app.get('/', (req, res) => {
  res.send('Chat Server is running on port 3000')
});

// Client connects to server
io.on('connection', (socket) => {
  console.log('client connected')

    // Client created new room
    socket.on("create_room", (roomName) => {
        socket.join(roomName);
        socket.emit("room_created");

        console.log("Room [" + roomName + "] created/joined");
    });

    socket.on("joined_room", (nickname, roomName) => {
        let dataObj = {
          "nickname":nickname,
          "online": io.sockets.adapter.rooms[roomName].length
        };

        io.sockets.in(roomName).emit("user_joined_room", dataObj);
    });

    socket.on("left_room", (nickname, roomName) => {
        console.log(nickname + " left room " + roomName);

        let dataObj = {
          "nickname":nickname,
          "online": io.sockets.adapter.rooms[roomName].length-1
        };

        io.sockets.in(roomName).emit("user_left_room", dataObj);
        socket.leave(roomName);
    });

      // Detect client sending a message in a specific room
      socket.on("message_detection_room", (senderNickname, messageContent, roomName) => {
         console.log(senderNickname+" :" +messageContent + " [in room: " + roomName + "]")

         let  message = { "message":messageContent, "senderNickname":senderNickname }
          // send the message to the client side
         io.sockets.in(roomName).emit('message', message );
        });

    // Send all rooms to the requesting client
    socket.on("get_rooms", function(args, id) {
      var rooms = io.sockets.adapter.rooms;

      console.log("client " + socket.id + " fetched " + Object.keys(rooms).length + " rooms");

      socket.emit("rooms", rooms);
    });

   socket.on('disconnect', function() {
     console.log("client has disconnected")
     socket.broadcast.emit("userdisconnect"," user has left ")

     socket.disconnect();

    });
  });
