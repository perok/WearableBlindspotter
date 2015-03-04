package com.ntnu.eit.videostreamer;

import android.graphics.ImageFormat;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by PerØyvind on 04/03/2015.
 */
public class VideoMJPEGStreamer implements Camera.PreviewCallback, Runnable {
    public static final String TAG = VideoMJPEGStreamer.class.getSimpleName();
    Socket socket;
    DataOutputStream stream;
    boolean prepared = false;
    boolean streaming = true;

    public void start() {

        // Connect
        try
        {
            ServerSocket server = new ServerSocket(8080);

            socket = server.accept();

            server.close();

            Log.i(TAG, "New connection to :" + socket.getInetAddress());

            stream = new DataOutputStream(socket.getOutputStream());
            prepared = true;
        }
        catch (IOException e)
        {
            Log.e(TAG, e.getMessage());
        }



        // Start stream session
        if (stream != null)
        {
            try
            {
                // send the header
                stream.write(("HTTP/1.0 200 OK\r\n" +
                        "Server: iRecon\r\n" +
                        "Connection: close\r\n" +
                        "Max-Age: 0\r\n" +
                        "Expires: 0\r\n" +
                        "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n" +
                        "Pragma: no-cache\r\n" +
                        "Content-Type: multipart/x-mixed-replace; " +
                        "boundary=" + boundary + "\r\n" +
                        "\r\n" +
                        "--" + boundary + "\r\n").getBytes());

                stream.flush();

                streaming = true;
            }
            catch (IOException e)
            {
                notifyOnEncoderError(this, "Error while writing header: " + e.getMessage());
                stop();
            }
        }

    }
    Handler mHandler = new Handler();
    byte[] frame;
    @Override
    public void onPreviewFrame(byte[] data, Camera camera)
    {
        frame = data;

        if (streaming)
            mHandler.post(this);
    }

    @Override
    public void run()
    {
        // TODO: cache not filling?
        try
        {
            // buffer is a ByteArrayOutputStream
            buffer.reset();

            switch (imageFormat)
            {
                case ImageFormat.JPEG:
                    // nothing to do, leave it that way
                    buffer.write(frame);
                    break;

                case ImageFormat.NV16: break;
                case ImageFormat.NV21:
                case ImageFormat.YUY2:
                case ImageFormat.YV12:
                    new YuvImage(frame, imageFormat, w, h, null).compressToJpeg(area, 100, buffer);
                    break;

                default:
                    throw new IOException("Error while encoding: unsupported image format");
            }

            buffer.flush();

            // write the content header
            stream.write(("Content-type: image/jpeg\r\n" +
                    "Content-Length: " + buffer.size() + "\r\n" +
                    "X-Timestamp:" + timestamp + "\r\n" +
                    "\r\n").getBytes());

            buffer.writeTo(stream);
            stream.write(("\r\n--" + boundary + "\r\n").getBytes());

            stream.flush();
        }
        catch (IOException e)
        {
            stop();
            notifyOnEncoderError(this, e.getMessage());
        }
    }

}
