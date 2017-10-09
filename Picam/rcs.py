import io
import socket
import struct
import time
import picamera

client_socket=socket.socket()
client_socket.connect(('43.230.2.208', 8000))
connection=client_socket.makefile('wb')

try:
    with picamera.PiCamera() as camera:
        camera.resolution=(320, 240)
        camera.framerate=10
        time.sleep(2)
        stream=io.BytesIO()
        start=time.time()
        for foo in camera.capture_continuous(stream, 'jpeg', use_video_port=True):
            connection.write(struct.pack('<L', stream.tell()))
            connection.flush()
            stream.seek(0)
            connection.write(stream.read())
            if time.time() - start >30:
                break
            stream.seek(0)
            stream.truncate()
    connection.write(struct.pack('<L',0))
finally:
    connection.close()
    client_socket.close()