import socket
import struct
import threading
import tensorflow as tf
import os.path
import time

class ThreadedServer(object):
    def __init__(self, host, port):
        self.json = "cam is not connected"
        self.host = host
        self.port = port
        self.dir = "/home/tk/project/tf_files/cam_photo/"
        self.i = 0
        self.cnt = 0
        self.sock = socket.socket()
        #self.sock.setsockopt('SOL_SOCKET', 'SO_REUSEADDR')
        self.sock.bind((self.host, self.port))
        self.sock.listen(1)

    def listen(self):
        while True:
            conn, address=self.sock.accept()
            connection=conn.makefile('rb')
            #self.sock.send(bytes("hihi"))
            print("connection established")
            threading.Thread(target = self.run,args = (connection,)).start()
            self.cnt+=1

    def imageWrite(self,connection):
        try:
            # Read the length of the image as a 32-bit unsigned int. If the
            # length is zero, quit the loop
            file = open('/home/tk/project/tf_files/cam_photo/test' + str(self.i+1) + '.jpeg', 'wb')
            image_len = struct.unpack('<L', connection.read(struct.calcsize('<L')))[0]
            if image_len==0:
                print("image_len==0")
                return 0
            # Construct a stream to hold the image data and read the image
            # data from the connection

            #image_stream = io.BytesIO()
            #image_stream.write(data)
            # Rewind the stream, open it as an image with PIL and do some
            # processing on it
            #image_stream.seek(0)
            #image = Image.open(image_stream)
            file.write(connection.read(image_len))
            #Run image modeling asynchronous.
        finally:
            print("file made")
            self.i+=1

    def imageModeling(self):

        # Loads label file, strips off carriage return
        self.label_lines = [line.rstrip() for line
                            in tf.gfile.GFile("/home/tk/project/tf_files/retrained_labels.txt")]

        # Unpersists graph from file

        with tf.gfile.FastGFile("/home/tk/project/tf_files/retrained_graph.pb", 'rb') as f:
            self.graph_def = tf.GraphDef()
            self.graph_def.ParseFromString(f.read())
            _ = tf.import_graph_def(self.graph_def, name='')

        while True:
            if self.i==0:
                pass
            #threshold=3

            if os.path.getsize(self.dir+'test'+str(self.i)+'.jpeg') == 0:
                pass
            # Read in the image-_data
            image_data = tf.gfile.FastGFile(self.dir+'test'+str(self.i)+'.jpeg', 'rb').read()

            with tf.Session() as sess:
                # Feed the image_data as input to the graph and get first prediction
                softmax_tensor = sess.graph.get_tensor_by_name('final_result:0')

                predictions = sess.run(softmax_tensor,{'DecodeJpeg/contents:0': image_data})

                # Sort to show labels of first prediction in order of confidence
                top_k = predictions[0].argsort()[-len(predictions[0]):][::-1]

                self.json='{'
                for node_id in top_k:
                    human_string = self.label_lines[node_id]
                    score = predictions[0][node_id]
                    self.json=self.json+"'score':'"+str((int)(score*10))+"', 'name':'"+human_string+"',"
                self.json=self.json+"\b}"
                print("image modeled")

    def run(self, connection):
        if self.cnt==0:
            print("cnt 0")
            threading.Thread(target=self.imageModeling).start()
            while True:
                self.imageWrite(connection)

        else:
            print("cnt else")
            while True:
                self.sock.send(self.json.encode('utf-8'))
                time.sleep(0.5)

#if not module
if __name__ == "__main__":

    #0.0.0.0 means server will accept every ip
    #port 0 means find usable port automatically
    server = ThreadedServer('0.0.0.0',8001)
    server.listen()