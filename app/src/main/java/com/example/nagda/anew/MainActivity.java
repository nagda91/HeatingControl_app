package com.example.nagda.anew;

import android.app.NotificationManager;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
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
import java.nio.channels.Channel;
import java.util.Arrays;
import java.util.Vector;

import static com.example.nagda.anew.App.CHANNEL_1_ID;

public class MainActivity extends AppCompatActivity {
    static String USERNAME = "USR";
    static String PASSW = "PSW";
    static String URIONE = "tcp://YOURBROKER.IP:1883";
    static  String URITWO = "tcp://your.ddns:1883";
    static String[] URIS = {URIONE, URITWO};
    String clientId = MqttClient.generateClientId();
    double thermDay = 21000;
    double thermNight = 19000;
    //double mainpipe, boiler1, boiler2, heater, house, chimney, solar, solarboiler;
    MqttAndroidClient client;
    String topic = "topic";
    int qos = 0;

    boolean getLog = true;
    boolean getAlert = true;

    //relay state strings
    String wtgasheater;
    String wtboilerpump;
    String wtsolarpump;

    private NotificationManagerCompat notificationManager;

    TextView output, mainPipe;// bojler1, bojler2, solar, aolarboiler, heater, chimney, house;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        notificationManager = NotificationManagerCompat.from(this);

        output = findViewById(R.id.output);
        mainPipe = findViewById(R.id.mainpipe);
        //////MQTT////////////////////////////////////////////////////////////////
        client = new MqttAndroidClient(this.getApplicationContext(),URIONE,clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        //options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
        options.setKeepAliveInterval(60);
        options.setUserName(USERNAME);
        options.setPassword(PASSW.toCharArray());
        options.setServerURIs(URIS);

        try {
            //System.out.println("AFTER SETSERVERURI TRY CLIENT: " + client.getServerURI() +"\n");
            //System.out.println("AFTER SETSERVERURI TRY OPTIONS: " + options.getServerURIs() +"\n");
            IMqttToken token = client.connect(options);
            /*if(client.isConnected()) {
                Toast.makeText(MainActivity.this, "1Connected! :)", Toast.LENGTH_LONG).show();
                sub();
            }
            else{
                Toast.makeText(MainActivity.this, "else - 1not Connected! :)", Toast.LENGTH_LONG).show();
            }*/

            //System.out.println("CONNECT TOKEN WAS TRUE " + token.getResponse() + "\n");
            //System.out.println("mqttconnecttoken: " + token.getException()+"\n");
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(MainActivity.this, "2Connected! :)",Toast.LENGTH_LONG).show();
                    //System.out.println("before sub function\n" + client.isConnected());
                    sub();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    //System.out.println("not connected\n");
                    Toast.makeText(MainActivity.this, "2Not connected! :(",Toast.LENGTH_LONG).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }

        /*if(client.isConnected()) System.out.println("Connected BEFORE SETCALLBACK\n" + client.isConnected());
        else{
            System.out.println(" NOT Connected BEFORE SETCALLBACK else\n" + client.isConnected());
        }*/

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Toast.makeText(MainActivity.this, "Connection lost! :(", Toast.LENGTH_LONG).show();
                reconn();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                if(getLog) output.setText(new String(message.getPayload()));
                //System.out.println("~~~~~~~~~~~~~~~~~~");
                //System.out.println(new String(message.getPayload()));
                //System.out.println("~~~~~~~~~~~~~~~~~~");
                setTemps(new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                //Toast.makeText(MainActivity.this, "Message arrived!:)", Toast.LENGTH_LONG).show();
            }
        });
        //////MQTT//////////////////////////////////////////////////////
    }

    private void sub(){
            try {
                IMqttToken subToken = client.subscribe("topic", qos);
                //System.out.println(Arrays.toString(subToken.getTopics()));
                System.out.println(subToken.getException()+ ", topic: " + Arrays.toString(subToken.getTopics()));
                subToken.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        //System.out.println("in sub suc\n");
                        //Toast.makeText(MainActivity.this, "Subscribed!:)",Toast.LENGTH_LONG).show();
                        sendMes("getTemps");
                        sendMes("getGPIO");
                        sendMes("getThermd");
                        sendMes("getWTs");
                        if(getLog) {
                            sendMes("getvlog");
                        }
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Toast.makeText(MainActivity.this, "Not subscribed!:(",Toast.LENGTH_LONG).show();
                    }
                });
            } catch (MqttException e) {
                //System.out.println("SUB EXCEPTION: " + e + "\n");
                e.printStackTrace();
            }
    }

    private void sendMes(String mes){
        String payload = "cmd " + mes;
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            IMqttToken token = client.publish(topic, message);
            //Toast.makeText(MainActivity.this, String.valueOf(token) ,Toast.LENGTH_LONG).show();
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "Message sent! :)",Toast.LENGTH_LONG).show();
        }
    }

    public void refresh(View v){

            sendMes("getTemps");
            sendMes("getThermd");
            sendMes("getGPIO");
            sendMes("getWTs");
            if(getLog) sendMes("getvlog");

    }

    public void reconn(){
        if(client.isConnected()){

        }
        else{
            try {
                //System.out.println("Recoonect ELSEBEN: " + client.getServerURI() + "\n");
                IMqttToken token = client.connect();
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        //Toast.makeText(MainActivity.this, "Reconnected!:)",Toast.LENGTH_LONG).show();
                        sub();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Toast.makeText(MainActivity.this, "Not reconnected!:(",Toast.LENGTH_LONG).show();

                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void setInput(double x) {
        TextView scoreView = findViewById(R.id.maininput);
        scoreView.setText(String.valueOf(x/1000)+"\u2103");
    }

    public void increaseTherm(View v){
        thermDay = thermDay+100;
        setInput(thermDay);
        sendMes("setthermD="+String.valueOf(thermDay));
    }

    public void decreaseTherm(View v){
        thermDay = thermDay - 100;
        setInput(thermDay);
        sendMes("setthermD="+String.valueOf(thermDay));
    }

    private void setTemps(String mes){
        int start = 0, end = 0;
        String row;
        do{
            if(mes.matches("gpio,(.*)")){
                end = mes.length()+2;
                System.out.println("in gpio\n");
                //row=gpio,0=1,1=1,2=1,3=1
                String gpio;
                //row=0=1,1=1,2=1,3=1
                //System.out.println(mes.substring(mes.indexOf("3="), mes.indexOf("3=")+3) + '\n');

                gpio = mes.substring(mes.indexOf("3="), mes.indexOf("3=")+3);
                if(gpio.matches("(.*)=0(.*)")){
                    gpio = gpio.replace("0", "ON");
                }
                if(gpio.matches("(.*)=1(.*)")){
                    gpio = gpio.replace("1", "OFF");
                }
                gpio = gpio.replace("3=", "Ház sz.: ");
                //wthousepump = gpio;
                TextView housepump = findViewById(R.id.housepump);
                housepump.setText(gpio);

                gpio = mes.substring(mes.indexOf("2="), mes.indexOf("2=")+3);
                if(gpio.matches("(.*)=0(.*)")){
                    gpio = gpio.replace("0", "ON");
                }
                if(gpio.matches("(.*)=1(.*)")){
                    gpio = gpio.replace("1", "OFF");
                }
                gpio = gpio.replace("2=", "Kazán: ");
                wtgasheater = gpio;
                System.out.println("in SetTemps: " + wtgasheater + "/n");
                //TextView heaterrelay = findViewById(R.id.heaterrelay);
                //heaterrelay.setText(gpio);

                gpio = mes.substring(mes.indexOf("0="), mes.indexOf("0=")+3);
                if(gpio.matches("(.*)=0(.*)")){
                    gpio = gpio.replace("=0", "=ON");
                }
                if(gpio.matches("(.*)=1(.*)")){
                    gpio = gpio.replace("1", "OFF");
                }
                gpio = gpio.replace("0=", "Bojler sz.: ");
                wtboilerpump = gpio;
                System.out.println("in SetTemps: " + wtboilerpump + "#######/n");
                //TextView boilerpump = findViewById(R.id.boilerpump);
                //boilerpump.setText(gpio);

                gpio = mes.substring(mes.indexOf("1="), mes.indexOf("1=")+3);
                if(gpio.matches("(.*)=0(.*)")){
                    gpio = gpio.replace("0", "ON");
                }
                if(gpio.matches("(.*)=1(.*)")){
                    gpio = gpio.replace("=1", "=OFF");
                }
                gpio = gpio.replace("1=", "Napk. sz.:");
                wtsolarpump = gpio;
                System.out.println(wtsolarpump + "/n");
                //TextView solarpump = findViewById(R.id.solarpump);
                //solarpump.setText(gpio);

                /*if(!WTmes.isEmpty()){
                    setGpioWithTime(WTmes);
                }*/

            }
            else if(mes.matches("OK(.*)")){
                end = mes.length()+2;
                noticationcall(mes);
                sendOnChannel1(mes.substring(mes.indexOf(",")+1));
            }
            else if(mes.matches("alert(.*)")){
                if(getAlert) {
                    end = mes.length() + 2;
                    sendOnChannel1(mes.substring(mes.indexOf(",") + 1));
                    noticationcall(mes.substring(mes.indexOf(",") + 1));
                }
            }
            else if (mes.matches("getthermD=(.*)")) {
                end = mes.length()+2;
                row = mes;
                //.substring(start, end);
                System.out.println("in getthermD##############");
                System.out.println(row.substring(row.indexOf('=') + 1) + " ##############");
                //name = row.substring(row.indexOf('=')+1);
                thermDay = Double.parseDouble(row.substring(row.indexOf('=') + 1));
                //thermDay = thermDay;
                System.out.print("thermDay= ");
                System.out.println(String.valueOf(thermDay));
                //TextView thermDay = findViewById(R.id.maininput);
                //thermDay.setText(name);
                setInput(thermDay);
            }
            else if(mes.matches("WTs g(.*)")){
                end = mes.length()+2;
                System.out.println("in SetTemps, WTs: " + mes + " /n");
                //WTmes = mes;
                setGpioWithTime(mes);
            }
            else {
                //System.out.println("IN ELSE IN ELSE###################");
                end = mes.indexOf("\n", start);
                if (end == -1) end = mes.length();
                row = mes.substring(start, end - 2);
                /*StringBuilder str;
                str = new StringBuilder(row);
                str.insert(str.indexOf("=") + 3, ',');
                row = str.toString();*/
                //System.out.println("before if row: " + row + '\n');
                start = end + 1;
                //row = row + "\u2103";*/
                // bojler1, bojler2, solar, solarboiler, heater, chimney, house;
                if (row.matches("focso(.*)")) {
                    /*System.out.println("in focso\n");
                    row = row.replaceAll("focso", "Főcső");
                    mainPipe.setText(row);*/
                    setMainPipeTemp(tempLength(row));
                } else if (row.matches("bojler1(.*)")) {
                    System.out.println("in boiler1###################################");
                    /*row = row.replace("bojler1", "Bojler teteje");
                    TextView bojler1 = findViewById(R.id.boilertop);
                    bojler1.setText(row);*/
                    setBoilerTopTemp(tempLength(row));
                } else if (row.matches("bojler2(.*)")) {
                    /*System.out.println("in boiler2\n");
                    row = row.replace("bojler2", "Bojler közepe");
                    TextView bojler2 = findViewById(R.id.boilerup);
                    bojler2.setText(row);*/
                    setBoilerMidTemp(tempLength(row));
                } else if (row.matches("kazan(.*)")) {
                    /*System.out.println("in kazan\n");
                    row = row.replace("kazan", "Kazán");
                    TextView heater = findViewById(R.id.heater);
                    heater.setText(row);*/
                    setHeaterTemp(tempLength(row));
                } else if (row.matches("lakas(.*)")) {
                    //System.out.println("in lakas\n");
                    /*row = row.replace("lakas", "Ház");
                    TextView house = findViewById(R.id.house);
                    house.setText(row);*/
                    setHouseTemp(tempLength(row));
                } else if (row.matches("kemeny(.*)")) {
                    /*System.out.println("in kemeny\n");
                    row = row.replace("kemeny", "Kémény");
                    TextView chimney = findViewById(R.id.chimney);
                    chimney.setText(row);*/
                    setChimneyTemp(tempLength(row));
                } else if (row.matches("napkollektor (.*)")) {
                    /*System.out.println("in nakollektor\n");
                    row = row.replace("napkollektor", "Nakoll.");
                    TextView solar = findViewById(R.id.solarpanel);
                    solar.setText(row);*/
                    setSolarTemp(tempLength(row));
                } else if (row.matches("napkollektorbojler(.*)")) {
                    /*System.out.println("in nakollektorboiler\n");
                    row = row.replace("napkollektorbojler", "Bojler alja:");
                    TextView solarB = findViewById(R.id.boilerbot);
                    solarB.setText(row);*/
                    setBBTemp(tempLength(row));
                } else if (row.matches("gpio,(.*)")) {
                    System.out.println("in gpio\n");
                }
                //name = row.substring(0, row.indexOf("\n")) + "\u2103";

                //mainpipe = Integer.parseInt(row.substring(row.indexOf("=")));
            }
        }while(end < mes.length());
    }

    public void settingsAct(View v){
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);

        try {
            IMqttToken disconToken = client.disconnect();
            disconToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(MainActivity.this, "Disconnected! :)" ,Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    Toast.makeText(MainActivity.this, "Could not disconnected! :)" ,Toast.LENGTH_LONG).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void noticationcall(String s){
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.ic_launcher_background))
            .setContentTitle("From New")
                .setContentText(s);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(1, notificationBuilder.build());
    }

    public void sendOnChannel1(String s) {

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_1_ID)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle("From thnenewone")
                .setContentText(s)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build();

        notificationManager.notify(1, notification);
    }

    private String tempLength(String mes){
        double x;
        int length = mes.length() - mes.indexOf("=") + 1;
        //System.out.println("LENGTH: " + String.valueOf(length) + "###############x");
        switch (length) {
            case 0:
                return "0";
            case 1:
                return "0";
            case 2:
                return "0";
            case 3:
                mes = mes.substring(mes.indexOf('=') + 1, mes.indexOf('=') + 2);
                x = Double.parseDouble(mes.substring(mes.indexOf('=') + 1));
                x = x/10;
                return String.valueOf(x);
            case 4:
                mes = mes.substring(mes.indexOf('=') + 1, mes.indexOf('=') + 3);
                x = Double.parseDouble(mes.substring(mes.indexOf('=') + 1));
                x = x/10;
                //mes = mes.substring(start, end);
                //System.out.println("after3: " + mes);
                return String.valueOf(x);
            case 5:
                mes = mes.substring(mes.indexOf('=') + 1, mes.indexOf('=') + 4);
                x = Double.parseDouble(mes.substring(mes.indexOf('=') + 1));
                x = x/10;
                return String.valueOf(x);
            case 6:
                mes = mes.substring(mes.indexOf('=') + 1, mes.indexOf('=') + 4);
                x = Double.parseDouble(mes.substring(mes.indexOf('=') + 1));
                x = x/10;
                return String.valueOf(x);
            }
        return "Wrong value!";
    }

    private void  setHouseTemp(String x) {
        String s = "Ház: "+ x +"\u2103";
        TextView scoreView = findViewById(R.id.house);
        scoreView.setText(s);
    }

    private void  setBoilerTopTemp(String x) {
        String s = "Bojler teteje: "+ x +"\u2103";
        TextView scoreView = findViewById(R.id.boilertop);
        scoreView.setText(s);
    }

    private void  setBoilerMidTemp(String x) {
        String s = "Bojler közepe: "+ x +"\u2103";
        TextView scoreView = findViewById(R.id.boilerup);
        scoreView.setText(s);
    }

    private void  setBBTemp(String x) {
        String s = "Bojler alja: " + x +"\u2103";
        TextView scoreView = findViewById(R.id.boilerbot);
        scoreView.setText(s);
    }

    private void  setSolarTemp(String x) {
        String s = "Napk.:" + x + "\u2103";
        TextView scoreView = findViewById(R.id.solarpanel);
        scoreView.setText(s);
    }

    private void  setMainPipeTemp(String x) {
        String s = "Főcső: " + x +"\u2103";
        TextView scoreView = findViewById(R.id.mainpipe);
        scoreView.setText(s);
    }

    private void  setChimneyTemp(String x) {
        String s = "Kémény: " + x +"\u2103";
        TextView scoreView = findViewById(R.id.chimney);
        scoreView.setText(s);
    }

    private void  setHeaterTemp(String x) {
        String s = "Kazán: " + x + "\u2103";
        TextView scoreView = findViewById(R.id.heater);
        scoreView.setText(s);
    }

    private void setGpioWithTime(String x) {
        // "WTs gasheater=0:0,solar=0:0,boiler=0:0"
        System.out.println("in setgpiowithtime:_" + x + "_,##########/n");
        //System.out.println("in setgpiowithtime:_" + x.substring(x.indexOf('='), x.indexOf(',')) + "_,##########/n");

        if(x.matches("WTs gasheater=(.*)")) {
            wtgasheater = wtgasheater + x.substring(x.indexOf('='), x.indexOf(',')) + " min.:sec.";
            wtgasheater = wtgasheater.replace("=", " - ");
            //System.out.println("in setgpiowithtime: " + wtgasheater + "##########/n");
            TextView gasheater = findViewById(R.id.heaterrelay);
            gasheater.setText(wtgasheater);
            x = x.substring(x.indexOf(',') + 1);
            //System.out.println("in setgpiowithtime:_" + x + "_,##########/n");
        }

        if(x.matches("solar=(.*)")) {
            wtsolarpump = wtsolarpump + x.substring(x.indexOf('='), x.indexOf(',')) + " min.:sec.";
            wtsolarpump = wtsolarpump.replace("=", " - ");
            //System.out.println("in setgpiowithtime: " + wtsolarpump + "##########/n");
            TextView solarpump = findViewById(R.id.solarpump);
            solarpump.setText(wtsolarpump);
            x = x.substring(x.indexOf(',') + 1);
            //System.out.println("in setgpiowithtime:_" + x + "_,##########/n");
        }

        if(x.matches("boiler=(.*)")) {
            wtboilerpump = wtboilerpump + x.substring(x.indexOf('=')) + " min.:sec.";
            wtboilerpump = wtboilerpump.replace("=", " - ");
            //System.out.println("in setgpiowithtime: " + wtboilerpump + "##########/n");
            TextView boilerpump = findViewById(R.id.boilerpump);
            boilerpump.setText(wtboilerpump);
        }
    }

    public void log_switch(android.view.View v) {
        android.widget.Switch a = findViewById(R.id.Log_switch);
        getLog = a.isChecked();

        if(!a.isChecked()) {
            output = findViewById(R.id.output);
            output.setText(null);
        }
        else{
            sendMes("getvlog");
        }

    }

    public void alert_switch(android.view.View v){
        android.widget.Switch a = findViewById(R.id.alert_switch);
        getAlert = a.isChecked();

    }
}
