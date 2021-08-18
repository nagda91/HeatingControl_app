package com.example.nagda.anew;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class SettingsActivity extends AppCompatActivity {
    double thermDay = 21.0, thermNight = 19.0;
    double winterStart, winterEnd, nightStarttime,
            nightEndtime, solarDiff, houseDiff, whilehouseDiff,
            boilerDiff, onlyPumpchimneymin, onlyPump, afterCirculation, heaterMax;
    int qos = 0;
    String topic = "topic";
    MqttAndroidClient client;

    TextView input, input1, input2, input3, input4, input5, input6;
    TextView input7, input8, input9, input10, input11, input12;

    /*TextView input = findViewById(R.id.input);
    TextView input1 = findViewById(R.id.input1);
    TextView input2 = findViewById(R.id.input2);
    TextView input3 = findViewById(R.id.input3);
    TextView input4 = findViewById(R.id.input4);
    TextView input5 = findViewById(R.id.input5);
    TextView input6 = findViewById(R.id.input6);
    TextView input7 = findViewById(R.id.input7);
    TextView input8 = findViewById(R.id.input8);
    TextView input9 = findViewById(R.id.input9);
    TextView input10 = findViewById(R.id.input10);
    TextView input11 = findViewById(R.id.input11);
    TextView input12 = findViewById(R.id.input12);*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        //////MQTT////////////////////////////////////////////////////////////////
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://YOURBROKER.IP:1883", clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);

        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(SettingsActivity.this, "Connected!:)", Toast.LENGTH_LONG).show();
                    sub();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(SettingsActivity.this, "Not connected!:(", Toast.LENGTH_LONG).show();

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Toast.makeText(SettingsActivity.this, "Connection lost!:(", Toast.LENGTH_LONG).show();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                setTemps(new String(message.getPayload()));
                //output.setText(new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                //Toast.makeText(SettingsActivity.this, "Message arrived!:)", Toast.LENGTH_LONG).show();
            }
        });
        //////MQTT//////////////////////////////////////////////////////
    }

    private void sub() {
        if (client.isConnected()) {
            try {
                IMqttToken subToken = client.subscribe("topic", qos);
                //System.out.println(Arrays.toString(subToken.getTopics()));
                System.out.println(subToken.getException() + ", topic: " + Arrays.toString(subToken.getTopics()));
                subToken.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        System.out.println("in sub suc\n");
                        Toast.makeText(SettingsActivity.this, "Subscribed!:)", Toast.LENGTH_LONG).show();
                        sendCmd("getTDT");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Toast.makeText(SettingsActivity.this, "Not subscribed!:(", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (MqttException e) {
                System.out.println("SUB EXCEPTION: " + e + "\n");
                e.printStackTrace();
            }
        } else {
            try {
                IMqttToken token = client.connect();
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Toast.makeText(SettingsActivity.this, "Connected!:)", Toast.LENGTH_LONG).show();
                        sub();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        System.out.println("in sub not conn\n");
                        Toast.makeText(SettingsActivity.this, "Not connected!:(", Toast.LENGTH_LONG).show();

                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }
    public void sendCmd(String mes) {
        String topic = "topic";
        String payload = "cmd " + mes;
        byte[] encodedPayload = new byte[0];
        try {
            System.out.println("IN SENDCMD, PAYLOAD:" + payload);
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            IMqttToken token = client.publish(topic, message);
            //Toast.makeText(SettingsActivity.this, String.valueOf(token), Toast.LENGTH_LONG).show();
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
            Toast.makeText(SettingsActivity.this, "Message sent! :)", Toast.LENGTH_LONG).show();
        }
    }
    private void setTemps(String mes){
        int start = 0, end=0;
        String row, value;
        System.out.println(String.valueOf(mes.length())+'\n');
        do{
            end = mes.indexOf("\n",start);
            if(end == -1) end = mes.length();
            row = mes.substring(start, end);
            //StringBuilder str;
            //str = new StringBuilder(row);
            //str.insert(str.indexOf("=")+3,'.');
            //row = str.toString();
            System.out.println(row +'\n');
            //System.out.println(String.valueOf(start)+'\n');
            //System.out.println(String.valueOf(end)+'\n');
            start = end+1;

            if(row.matches("thermostatNight(.*)")){
                System.out.println("thermostatNight\n");
                value = row.substring(row.indexOf('=')+1);
                //System.out.println(value);
                thermNight = Double.parseDouble(value);
                TextView input = findViewById(R.id.input);
                input.setText(String.valueOf(thermNight/1000) + "\u2103");
                //System.out.println(value + "\n");
                System.out.println("thermNight: " + String.valueOf(thermNight));
            }else if(row.matches("winterStart(.*)")){
                System.out.println("winterStart\n");
                //row = row.replace("bojler1","Bojler teteje");
                value = row.substring(row.indexOf('=')+1);
                System.out.println(value + "\n");
                winterStart = Double.parseDouble(value);
                TextView input11 = findViewById(R.id.input11);
                input11.setText(String.valueOf(winterStart));
            }else if(row.matches("winterEnd(.*)")){
                System.out.println("winterEnd\n");
                //row = row.replace("bojler1","Bojler teteje");
                value = row.substring(row.indexOf('=')+1);
                System.out.println(value + "\n");
                winterEnd = Double.parseDouble(value);
                TextView input12 = findViewById(R.id.input12);
                input12.setText(String.valueOf(winterEnd));
            }else if(row.matches("nightStarttime(.*)")){
                System.out.println("in nightStarttime\n");
                //row = row.replace("bojler2","Bojler közepe");
                value = row.substring(row.indexOf('=')+1);
                System.out.println(value + "\n");
                nightStarttime = Double.parseDouble(value);
                TextView input1 = findViewById(R.id.input1);
                input1.setText(String.valueOf(nightStarttime));
            }else if(row.matches("nightEndtime(.*)")){
                System.out.println("in nightEndtime\n");
                //row = row.replace("kazan","Kazán");
                value = row.substring(row.indexOf('=')+1);
                nightEndtime = Double.parseDouble(value);
                TextView input2 = findViewById(R.id.input2);
                input2.setText(String.valueOf(nightEndtime));
            }else if(row.matches("solarDiff(.*)")){
                System.out.println("in solarDiff\n");
                //row = row.replace("lakas","Ház");
                value = row.substring(row.indexOf('=')+1);
                solarDiff = Double.parseDouble(value);
                TextView input3 = findViewById(R.id.input3);
                input3.setText(String.valueOf(solarDiff/1000) + "\u2103");
            }else if(row.matches("houseDiff(.*)")){
                System.out.println("in houseDiff\n");
                //row = row.replace("kemeny","Kémény");
                value = row.substring(row.indexOf('=')+1);
                houseDiff = Double.parseDouble(value);
                TextView input4 = findViewById(R.id.input4);
                input4.setText(String.valueOf(houseDiff/1000));
            }else if(row.matches("whilehouseDiff(.*)")){
                System.out.println("in whilehouseDiff\n");
                //row = row.replace("napkollektor","Nakoll.");
                value = row.substring(row.indexOf('=')+1);
                whilehouseDiff = Double.parseDouble(value);
                TextView input7 = findViewById(R.id.input7);
                input7.setText(String.valueOf(whilehouseDiff/1000) + "\u2103");
            }else if(row.matches("boilerDiff(.*)")){
                System.out.println("in boilerDiff\n");
                //row = row.replace("napkollektorbojler","Bojler alja:");
                value = row.substring(row.indexOf('=')+1);
                boilerDiff = Double.parseDouble(value);
                System.out.println(String.valueOf(boilerDiff) +'\n');
                TextView input5 = findViewById(R.id.input5);
                input5.setText(String.valueOf(boilerDiff/1000) + "\u2103");
            }else if(row.matches("onlyPumpchimneymin(.*)")){
                System.out.println("in onlyPumpchimneymin\n");
                //row = row.replace("napkollektorbojler","Bojler alja:");
                value = row.substring(row.indexOf('=')+1);
                onlyPumpchimneymin = Double.parseDouble(value);
                TextView input8 = findViewById(R.id.input8);
                input8.setText(String.valueOf(onlyPumpchimneymin/1000) + "\u2103");
            }else if(row.matches("onlyPump(.*)")){
                System.out.println("in onlyPump\n");
                //row = row.replace("napkollektorbojler","Bojler alja:");
                value = row.substring(row.indexOf('=')+1);
                onlyPump = Double.parseDouble(value);
                TextView input9 = findViewById(R.id.input9);
                input9.setText(String.valueOf(onlyPump/1000) + "\u2103");
            }else if(row.matches("afterCirculation(.*)")){
                System.out.println("in afterCirculation\n");
                //row = row.replace("napkollektorbojler","Bojler alja:");
                value = row.substring(row.indexOf('=')+1);
                afterCirculation = Double.parseDouble(value);
                TextView input10 = findViewById(R.id.input10);
                input10.setText(String.valueOf(afterCirculation/1000) + "\u2103");
            }else if(row.matches("heaterMax(.*)")){
                System.out.println("in heaterMax\n");
                //row = row.replace("napkollektorbojler","Bojler alja:");
                value = row.substring(row.indexOf('=')+1);
                heaterMax = Double.parseDouble(value);
                TextView input6 = findViewById(R.id.input6);
                input6.setText(String.valueOf(heaterMax/1000) + "\u2103");
            }
            //name = row.substring(0, row.indexOf("\n")) + "\u2103";

            //mainpipe = Integer.parseInt(row.substring(row.indexOf("=")));

        }while(end < mes.length());
    }


    public void inputPlus(View v) {
        thermNight = thermNight + 100;
        sendCmd("thermN=" + String.valueOf(thermNight));
        TextView input = findViewById(R.id.input);
        input.setText(String.valueOf(thermNight/1000) + "\u2103");
    }

    public void inputMinus(View v) {
        thermNight = thermNight - 100;
        sendCmd("thermN=" + String.valueOf(thermNight));
        TextView input = findViewById(R.id.input);
        input.setText(String.valueOf(thermNight/1000) + "\u2103");
    }

    public void inputPlus6(View v) {
        heaterMax = heaterMax + 500;
        sendCmd("hMax=" + String.valueOf(heaterMax));
        TextView input = findViewById(R.id.input6);
        input.setText(String.valueOf(heaterMax/1000));
    }

    public void inputMinus6(View v) {
        heaterMax = heaterMax - 500;
        sendCmd("hMax=" + String.valueOf(heaterMax));
        TextView input = findViewById(R.id.input6);
        input.setText(String.valueOf(heaterMax/1000));
    }

    public void inputPlus11(View v) {
        if(winterStart == 11) Toast.makeText(SettingsActivity.this, "Túl nagy érték, max = 11(=December)!", Toast.LENGTH_LONG).show();
        else{
            winterStart = winterStart + 1;
            sendCmd("wS=" + String.valueOf(winterStart));
            TextView input = findViewById(R.id.input11);
            input.setText(String.valueOf(winterStart));
        }

    }

    public void inputMinus11(View v) {
        if(winterStart == 0) Toast.makeText(SettingsActivity.this, "Túl kicsi érték, min = 0(=Január)!", Toast.LENGTH_LONG).show();
        else{
            winterStart = winterStart - 1;
            sendCmd("wS=" + String.valueOf(winterStart));
            TextView input = findViewById(R.id.input11);
            input.setText(String.valueOf(winterStart));
        }

    }

    public void inputPlus12(View v) {
        if(winterEnd == 11) Toast.makeText(SettingsActivity.this, "Túl nagy érték, max = 11(=December)!", Toast.LENGTH_LONG).show();
        else{
            winterEnd = winterEnd + 1;
            sendCmd("wE=" + String.valueOf(winterEnd));
            TextView input = findViewById(R.id.input12);
            input.setText(String.valueOf(winterEnd));
        }

    }

    public void inputMinus12(View v) {
        if(winterEnd == 0) Toast.makeText(SettingsActivity.this, "Túl kicsi érték, min = 0(=Január)!", Toast.LENGTH_LONG).show();
        else{
            winterEnd = winterEnd - 1;
            sendCmd("wE=" + String.valueOf(winterEnd));
            TextView input = findViewById(R.id.input12);
            input.setText(String.valueOf(winterEnd));
        }

    }

    public void inputPlus1(View v) {
        if(nightStarttime == 1439) Toast.makeText(SettingsActivity.this, "Túl nagy érték, max = 1439(=23:59, 22:00=1320)!", Toast.LENGTH_LONG).show();
        else {
            nightStarttime = nightStarttime + 1;
            sendCmd("nStim=" + String.valueOf(nightStarttime));
            TextView input = findViewById(R.id.input1);
            input.setText(String.valueOf(nightStarttime));
        }
    }

    public void inputMinus1(View v) {
        if(nightStarttime == 0) Toast.makeText(SettingsActivity.this, "Túl kicsi érték, min = 0(=0:00, 6:00=360)!", Toast.LENGTH_LONG).show();
        else {
            nightStarttime = nightStarttime - 1;
            sendCmd("nStim=" + String.valueOf(nightStarttime));
            TextView input = findViewById(R.id.input1);
            input.setText(String.valueOf(nightStarttime));
        }
    }

    public void inputPlus2(View v) {
        nightEndtime = nightEndtime + 1;
        sendCmd("nEtim=" + String.valueOf(nightEndtime));
        TextView input = findViewById(R.id.input2);
        input.setText(String.valueOf(nightEndtime));
    }

    public void inputMinus2(View v) {
        nightEndtime = nightEndtime - 1;
        sendCmd("nEtim=" + String.valueOf(nightEndtime));
        TextView input = findViewById(R.id.input2);
        input.setText(String.valueOf(nightEndtime));
    }

    public void inputPlus3(View v) {
        solarDiff = solarDiff + 500;
        sendCmd("sDiff=" + String.valueOf(solarDiff));
        TextView input = findViewById(R.id.input3);
        input.setText(String.valueOf(solarDiff/1000));
    }

    public void inputMinus3(View v) {
        solarDiff = solarDiff - 500;
        sendCmd("sDiff=" + String.valueOf(solarDiff));
        TextView input = findViewById(R.id.input3);
        input.setText(String.valueOf(solarDiff/1000));
    }

    public void inputPlus4(View v) {
        houseDiff = houseDiff + 100;
        sendCmd("hdiff=" + String.valueOf(houseDiff));
        TextView input = findViewById(R.id.input4);
        input.setText(String.valueOf(houseDiff/1000));
    }

    public void inputMinus4(View v) {
        houseDiff = houseDiff - 100;
        sendCmd("hdiff=" + String.valueOf(houseDiff));
        TextView input = findViewById(R.id.input4);
        input.setText(String.valueOf(houseDiff/1000));
    }

    public void inputPlus7(View v) {
        whilehouseDiff = whilehouseDiff + 100;
        sendCmd("whDiff=" + String.valueOf(whilehouseDiff));
        TextView input = findViewById(R.id.input7);
        input.setText(String.valueOf(whilehouseDiff/1000));
    }

    public void inputMinus7(View v) {
        whilehouseDiff = whilehouseDiff - 100;
        sendCmd("whDiff=" + String.valueOf(whilehouseDiff));
        TextView input = findViewById(R.id.input7);
        input.setText(String.valueOf(whilehouseDiff/1000));
    }

    public void inputPlus5(View v) {
        boilerDiff = boilerDiff + 100;
        sendCmd("bDiff=" + String.valueOf(boilerDiff));
        TextView input = findViewById(R.id.input5);
        input.setText(String.valueOf(boilerDiff/1000));
    }

    public void inputMinus5(View v) {
        boilerDiff = boilerDiff - 100;
        sendCmd("bDiff=" + String.valueOf(boilerDiff));
        TextView input = findViewById(R.id.input5);
        input.setText(String.valueOf(boilerDiff/1000));
    }

    public void inputPlus8(View v) {
        onlyPumpchimneymin = onlyPumpchimneymin + 100;
        sendCmd("oPchim=" + String.valueOf(onlyPumpchimneymin));
        TextView input = findViewById(R.id.input8);
        input.setText(String.valueOf(onlyPumpchimneymin/1000));
    }

    public void inputMinus8(View v) {
        onlyPumpchimneymin = onlyPumpchimneymin - 100;
        sendCmd("oPchim=" + String.valueOf(onlyPumpchimneymin));
        TextView input = findViewById(R.id.input8);
        input.setText(String.valueOf(onlyPumpchimneymin/1000));
    }

    public void inputPlus9(View v) {
        onlyPump = onlyPump + 100;
        sendCmd("oP=" + String.valueOf(onlyPump));
        TextView input = findViewById(R.id.input9);
        input.setText(String.valueOf(onlyPump/1000));
    }

    public void inputMinus9(View v) {
        onlyPump = onlyPump - 100;
        sendCmd("oP=" + String.valueOf(onlyPump));
        TextView input = findViewById(R.id.input9);
        input.setText(String.valueOf(onlyPump/1000));
    }

    public void inputPlus10(View v) {
        afterCirculation = afterCirculation + 500;
        sendCmd("aftCirc=" + String.valueOf(afterCirculation));
        TextView input = findViewById(R.id.input10);
        input.setText(String.valueOf(afterCirculation/1000));
    }

    public void inputMinus10(View v) {
        afterCirculation = afterCirculation - 500;
        sendCmd("aftCirc=" + String.valueOf(afterCirculation));
        TextView input = findViewById(R.id.input10);
        input.setText(String.valueOf(afterCirculation/1000));
    }
}
