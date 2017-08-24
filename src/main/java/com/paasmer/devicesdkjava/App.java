package com.paasmer.devicesdkjava;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTimeoutException;
import com.amazonaws.services.iot.client.AWSIotTopic;
import com.paasmer.devicesdkjava.TopicListener;
import com.paasmer.devicesdkjava.util.CommandArguments;
import com.paasmer.devicesdkjava.util.SampleUtil;
import com.paasmer.devicesdkjava.util.SampleUtil.KeyStorePasswordPair;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.net.Inet4Address;
/**
 * Hello world!
 *
 */
public class App 
{
	 private static final String PropertyFile = "config.properties";
	private static String deviceName="";
	private static  String TestTopic = "paasmerv2_device_online";
	private static String TestTopicSubscribe= "";
   private static final AWSIotQos TestTopicQos = AWSIotQos.QOS0;
   private static GpioController gpio;
   private static AWSIotMqttClient awsIotClient;
   
   public static void setClient(AWSIotMqttClient client) {
       awsIotClient = client;
   }
   public static void main( String[] args ) throws AWSIotException, AWSIotTimeoutException
   {
       CommandArguments arguments = CommandArguments.parse(args);
       initClient(arguments);
       awsIotClient.connect();
       
       AWSIotTopic topic = new TopicListener(TestTopicSubscribe, TestTopicQos);
       awsIotClient.subscribe(topic,true);
       
      Runnable pubRun=new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			
			try {
				publishFeedInfo();
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
      };
      
      ScheduledExecutorService exec=Executors.newScheduledThreadPool(1);
      exec.scheduleAtFixedRate(pubRun, 1L, 60L, TimeUnit.SECONDS);
       
   }
	private static String getMacAddress(){
		
		// TODO Auto-generated method stub
		InetAddress ip;
		try {

		ip = getCurrentIp();//InetAddress.getLocalHost();
		//System.out.println("Current IP address : " +getCurrentIp());

		NetworkInterface network = NetworkInterface.getByInetAddress(ip);

		byte[] mac = network.getHardwareAddress();

	//	System.out.print("Current MAC address :" );

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < mac.length; i++) {
		sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
		}
		return sb.toString();

		} catch (SocketException e){

		e.printStackTrace();

		}catch (NullPointerException e){
                }

		return null;
	}
	 public static InetAddress getCurrentIp() {
         try {
             Enumeration<NetworkInterface> networkInterfaces = NetworkInterface
                     .getNetworkInterfaces();
             while (networkInterfaces.hasMoreElements()) {
                 NetworkInterface ni = (NetworkInterface) networkInterfaces
                         .nextElement();
                 Enumeration<InetAddress> nias = ni.getInetAddresses();
                 while(nias.hasMoreElements()) {
                     InetAddress ia= (InetAddress) nias.nextElement();
                     if (!ia.isLinkLocalAddress() 
                      && !ia.isLoopbackAddress()
                      && ia instanceof Inet4Address) {
                         return ia;
                     }
                 }
             }
         } catch (SocketException e) {
           e.printStackTrace();
         }
         return null;
     }
	private static void publishFeedInfo() {
		// TODO Auto-generated method stub
		gpio=GpioFactory.getInstance();
		Properties prop = new Properties();
		InputStream input = null;

		try {
			URL resource = SampleUtil.class.getResource(PropertyFile);
			input = resource.openStream();//new FileInputStream("config.properties");

			// load a properties file
			prop.load(input);

			// get the property value and print it out
			String feeds= prop.getProperty("feed");
			deviceName= prop.getProperty("deviceName");
			String paasmerId=getMacAddress();//prop.getProperty("paasmerId");
			String timeout=prop.getProperty("timePeriod");
			String userName= prop.getProperty("userName");
			
			JSONArray jArray=new JSONArray(feeds);
			for (int i=0;i<jArray.length();i++) {
				JSONObject jObject=jArray.getJSONObject(i);
				
				Pin pin=RaspiPin.getPinByName("GPIO "+jObject.getString("pin"));
				GpioPinDigitalOutput gdo=gpio.provisionDigitalOutputPin(pin);//(pin,"my led",PinState.LOW);
				String state=gdo.getState().isHigh()? "1": "0";
				JSONObject feedInfo=new JSONObject();
				feedInfo.put("feedname", jObject.getString("name"));
				feedInfo.put("feedtype", jObject.getString("type"));
				feedInfo.put("feedpin", jObject.getString("pin"));
				feedInfo.put("feedvalue", state);
				
				JSONArray jFeedArray=new JSONArray();
				jFeedArray.put(feedInfo);
				
				JSONObject feedDetails=new JSONObject();
				feedDetails.put("feeds", jFeedArray);
				feedDetails.put("messagecount", i);
				feedDetails.put("paasmerid", paasmerId);
				feedDetails.put("username", userName);
				feedDetails.put("devicename", deviceName);
				feedDetails.put("devicetype", "SBC");
				
				awsIotClient.publish(TestTopic,feedDetails.toString());
				gpio.unprovisionPin(gdo);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} catch (IOException ex) {
			ex.printStackTrace();
		}catch(JSONException e) { 
			e.printStackTrace();
		} catch (AWSIotException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	private static void initClient(CommandArguments arguments) {
		// TODO Auto-generated method stub
		String clientEndpoint = "a3rwl3kghmkdtx.iot.us-west-2.amazonaws.com";//arguments.getNotNull("clientEndpoint",SampleUtil.getConfig("clientEndpoint"));
		//String clientId = arguments.getNotNull("clientId",SampleUtil.getConfig("clientId"));
		
		
	Properties prop = new Properties();
                InputStream input = null;

                try {
                        URL resource = SampleUtil.class.getResource(PropertyFile);
                        input = resource.openStream();//new FileInputStream("config.properties");

                        // load a properties file
                        prop.load(input);

                        // get the property value and print it out
                       
                        String deviceName= prop.getProperty("deviceName");
                        String paasmerId=prop.getProperty("paasmerId");
                        String timeout=prop.getProperty("timePeriod");
                        String userName= prop.getProperty("userName");
			
			TestTopicSubscribe = userName+"_"+deviceName;
			
				
	//	deviceName = arguments.getNotNull("deviceName",SampleUtil.getConfig("deviceName"));
		 String clientId = deviceName;
		String certificateFile = deviceName+"-certificate.pem.crt";//arguments.get("certificateFile", SampleUtil.getConfig("certificateFile"));
       String privateKeyFile = deviceName+"-private.pem.key";//arguments.get("privateKeyFile", SampleUtil.getConfig("privateKeyFile"));
       if (awsIotClient == null && certificateFile != null && privateKeyFile != null) {
           String algorithm = arguments.get("keyAlgorithm", SampleUtil.getConfig("keyAlgorithm"));

           KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certificateFile, privateKeyFile, algorithm);

           awsIotClient = new AWSIotMqttClient(clientEndpoint, clientId, pair.keyStore, pair.keyPassword);
       }

       if (awsIotClient == null) {
           String awsAccessKeyId = arguments.get("awsAccessKeyId", SampleUtil.getConfig("awsAccessKeyId"));
           String awsSecretAccessKey = arguments.get("awsSecretAccessKey", SampleUtil.getConfig("awsSecretAccessKey"));
           String sessionToken = arguments.get("sessionToken", SampleUtil.getConfig("sessionToken"));

           if (awsAccessKeyId != null && awsSecretAccessKey != null) {
               awsIotClient = new AWSIotMqttClient(clientEndpoint, clientId, awsAccessKeyId, awsSecretAccessKey,
                       sessionToken);
           }
       }

       if (awsIotClient == null) {
           throw new IllegalArgumentException("Failed to construct client due to missing certificate or credentials.");
       }
	}catch(Exception e){
		e.printStackTrace();		
	}	
		
	}
	
	
}
