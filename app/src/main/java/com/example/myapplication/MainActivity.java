package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Button sendButton;
    private TextView resultText;
    private static final String SERVER_IP = "54.197.223.49"; // Replace with your server IP
    private static final int SERVER_PORT = 65432;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendButton = findViewById(R.id.send_button);
        resultText = findViewById(R.id.result_text);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SocketTask().execute("Hello, Server!");
            }
        });
    }

    private class SocketTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... messages) {
            StringBuilder responseTimes = new StringBuilder();
            long totalRoundTripTime = 0; // Variable to store the total round-trip time

            for (int i = 0; i < 10; i++) {
                long sendTime, receiveTime, currentTimeMs;
                String response;

                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(SERVER_IP, SERVER_PORT), 5000);
                    sendTime = System.currentTimeMillis();

                    OutputStream outputStream = socket.getOutputStream();
                    InputStream inputStream = socket.getInputStream();

                    // Send message
                    outputStream.write(messages[0].getBytes());
                    outputStream.flush();

                    // Wait for ACK
                    byte[] buffer = new byte[1024];
                    int bytesRead = inputStream.read(buffer);
                    receiveTime = System.currentTimeMillis();

                    // Calculate round-trip time
                    long roundTripTime = receiveTime - sendTime;
                    totalRoundTripTime += roundTripTime; // Add round-trip time to total

                    // Get the current time in milliseconds
                    currentTimeMs = System.currentTimeMillis();

                    // Add the current time to the response log
                    response = "Round-trip time for message " + (i + 1) + ": " + roundTripTime + " ms (Current time: " + currentTimeMs + " ms)";
                    responseTimes.append(response).append("\n");

                } catch (IOException e) {
                    response = "Error on message " + (i + 1) + ": " + e.getMessage();
                    responseTimes.append(response).append("\n");
                }

                try {
                    // Short delay between messages
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Calculate the average round-trip time
            long averageRoundTripTime = totalRoundTripTime / 10;
            responseTimes.append("\nAverage Round-trip Time: ").append(averageRoundTripTime).append(" ms\n");

            // Run ping for 10 seconds and get the result
            String pingResult = runPing();

            // Append ping results to the responseTimes
            responseTimes.append("\nPing Results (10 seconds):\n").append(pingResult);

            // Save response times, average, and ping results to file
            saveToFile(responseTimes.toString());
            return responseTimes.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            resultText.setText(result);
        }

        // Method to save data to a file
        private void saveToFile(String data) {
            // Get current time for the file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = timeStamp + "_response_times.txt";

            // Create file in the app's private storage
            File file = new File(getApplicationContext().getExternalFilesDir(null), fileName);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                // Write data to file
                fos.write(data.getBytes());
                // Notify user about file save success
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "File saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show());
            } catch (IOException e) {
                e.printStackTrace();
                // Notify user about file save error
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error saving file", Toast.LENGTH_SHORT).show());
            }
        }

        // Method to run ping command for 10 seconds
        private String runPing() {
            StringBuilder pingResult = new StringBuilder();
            try {
                // Ping for 10 seconds (you can adjust the ping command parameters as needed)
                Process process = Runtime.getRuntime().exec("ping -c 10 -i 1 " + SERVER_IP);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    pingResult.append(line).append("\n");
                }
                reader.close();
            } catch (IOException e) {
                pingResult.append("Error running ping: ").append(e.getMessage()).append("\n");
            }
            return pingResult.toString();
        }
    }
}
