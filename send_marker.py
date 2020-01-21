import zmq

context = zmq.Context()

#  Socket to talk to server
socket = context.socket(zmq.PAIR)
print("Connecting to server…")
socket.connect("tcp://10.0.1.12:5555")

msg = b'{"strData":"","numData":0,"type":"MARKER","from":-1}'
socket.send(msg)
