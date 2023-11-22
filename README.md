# UDP-Server
waits for clients on UDP port 11122. runs clients on a new thread. receives a msg containing a URL, which it then issues an http get request.
it saves the response in a single string and sends it to the client 1024 bytes at a time. The format of the msg sent to client is seqNum, length of message, message.
seqNum is 1 byte, length of message is 4 bytes, and message is min(1024, length of data left to be sent). Note: length of messgae is actually 5 + length(message).
The program uses stop and wait, i.e. it won't send the next packet until it receives the correct ack for the current packet. It also has a user-selected timeout.
If a timeout Exception or wrong ack is received the current packet is immediately sent. The try-catch for the timeout exception is handled inside a while loop.
At the end of transmission, a file "./something1.html" is created that Client can check tor anomalies.
