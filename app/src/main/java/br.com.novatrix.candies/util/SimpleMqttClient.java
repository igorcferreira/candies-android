package br.com.novatrix.candies.util;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import br.com.novatrix.candies.application.CandiesApplication;

/**
 * Class that generate MQTT Connections
 * Created by jprestes on 4/5/15.
 */
public class SimpleMqttClient {

    static final String BROKER_URL = "tcp://iot.eclipse.org:1883";
    static final String TOPIC = "jeffprestes/candies/world";

    private MqttClient mqttClient = null;
    private MqttTopic mqttTopic = null;

    /**
     * Generates a MQTT Client and connects to the broker
     * @return MQTT Client connected to a broker
     */
    public SimpleMqttClient ()   {

        this.connect();

    }


    private boolean connect()       {
        TelephonyManager tm = (TelephonyManager) CandiesApplication.get().getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        String clientId = tm.getDeviceId();

        try {
            mqttClient = new MqttClient(BROKER_URL, clientId, new MemoryPersistence());
            mqttClient.connect();

            if (mqttClient.isConnected()) {
                mqttTopic = mqttClient.getTopic(TOPIC);
                mqttClient.subscribe(TOPIC, 1);

                return true;
            }

        } catch (Exception ex)  {
            Log.e("SimpleMqttClient", "Error when creating MqttClient: " + ex.getLocalizedMessage(), ex);
            Toast.makeText(CandiesApplication.get().getApplicationContext(), "Error when connecting to Message Server: " + ex.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }

        return false;
    }

    /**
     * Publish a message into a Topic on a Broker
     * @param msg message to be published
     * @return whether the message was published or not
     */
    public boolean publish(String msg)      {

        boolean retorno = false;

        Log.i("SimpleMqttClient", "1 - Publishing message : [" + msg + "] at: " + TOPIC + " at " + BROKER_URL);

        if (!mqttClient.isConnected())      {
            this.connect();
            Log.i("SimpleMqttClient", "2 - Publishing message : [" + msg + "] at: " + TOPIC + " at " + BROKER_URL);
        }

        if (mqttClient.isConnected()) {
            MqttMessage message = new MqttMessage(msg.getBytes());
            message.setQos(1);
            message.setRetained(false);

            // Publish the message
            Log.i("SimpleMqttClient", "3 - Publishing message : [" + msg + "] at: " + TOPIC + " at " + BROKER_URL);

            MqttDeliveryToken token = null;
            try {
                // publish message to broker
                token = this.getMqttTopic().publish(message);
                // Wait until the message has been delivered to the broker
                token.waitForCompletion();
                Thread.sleep(100);
                Log.i("SimpleMqttClient", "4 - Publishing message : [" + msg + "] at: " + TOPIC + " at " + BROKER_URL);
                retorno = true;

            } catch (Exception ex) {
                Log.e("SimpleMqttClient", "Error when publish message: " + ex.getLocalizedMessage(), ex);
                Toast.makeText(CandiesApplication.get().getApplicationContext(), "Error when publish a message to server: " + ex.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        return retorno;
    }


    private MqttTopic getMqttTopic()    {
        return this.mqttTopic;
    }


    /**
    Disconnect from the Broker
     */
    public void disconnect()    {
        try {
            // wait to ensure subscribed messages are delivered
            Thread.sleep(500);
            this.mqttClient.disconnect();
        } catch (Exception ex) {
            Log.e("SimpleMqttClientDisconnect", "Error when disconnecting from the server: " + ex.getLocalizedMessage(), ex);
            Toast.makeText(CandiesApplication.get().getApplicationContext(), "Error when disconnecting from the server: " + ex.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
