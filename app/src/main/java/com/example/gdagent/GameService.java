package com.example.gdagent;
import android.accessibilityservice.*;import android.graphics.*;import android.os.*;import android.view.accessibility.*;import android.view.*;import org.json.*;
import java.io.*;import java.net.*;import java.util.*;import java.util.concurrent.*;
public class GameService extends AccessibilityService {
 public static volatile boolean enabled=false;
 public static volatile String apiKey="";
 private final Handler handler=new Handler(Looper.getMainLooper());
 private final ExecutorService pool=Executors.newSingleThreadExecutor();
 private volatile boolean busy=false;private long lastCall=0;
 private volatile String status="Ожидание";
 private static final String GAME="com.robtopx.geometryjump";
 private static final String GAME_LITE="com.robtopx.geometryjumplite";
 private boolean isGame(){return GAME.equals(activePackage)||GAME_LITE.equals(activePackage);}
 private final Runnable loop=new Runnable(){public void run(){
  if(enabled && !busy && System.currentTimeMillis()-lastCall>1800 && isGame()) capture();
  handler.postDelayed(this,250);
 }};
 private String activePackage="";
 @Override public void onServiceConnected(){super.onServiceConnected();handler.post(loop);}
 @Override public void onAccessibilityEvent(AccessibilityEvent e){if(e.getPackageName()!=null)activePackage=e.getPackageName().toString();}
 @Override public void onInterrupt(){enabled=false;}
 @Override public void onDestroy(){enabled=false;handler.removeCallbacks(loop);pool.shutdownNow();super.onDestroy();}
 private void capture(){if(apiKey.isEmpty())return;busy=true;lastCall=System.currentTimeMillis();
  takeScreenshot(0,getMainExecutor(),new TakeScreenshotCallback(){
   public void onSuccess(ScreenshotResult shot){pool.execute(()->{
    try {Bitmap bitmap=Bitmap.wrapHardwareBuffer(shot.getHardwareBuffer(),shot.getColorSpace());
     if(bitmap==null)return;
     Bitmap scaled=Bitmap.createScaledBitmap(bitmap,Math.min(720,bitmap.getWidth()),Math.max(1,(int)(bitmap.getHeight()*Math.min(720,bitmap.getWidth())/(float)bitmap.getWidth())),true);
     ByteArrayOutputStream bytes=new ByteArrayOutputStream();scaled.compress(Bitmap.CompressFormat.JPEG,65,bytes);
     String answer=request(Base64.getEncoder().encodeToString(bytes.toByteArray()),bitmap.getWidth(),bitmap.getHeight());
     JSONObject a=new JSONObject(answer);if(enabled && isGame())apply(a,bitmap.getWidth(),bitmap.getHeight());
    }catch(Exception ex){android.util.Log.w("GDAgent","Agent error: "+ex.getMessage());}
    finally{shot.getHardwareBuffer().close();busy=false;}
   });}
   public void onFailure(int error){busy=false;}
  });
 }
 private String request(String b64,int w,int h)throws Exception{
  String prompt="Ты помощник для Geometry Dash. Это кадр Android-игры, исходный экран "+w+"x"+h+". " +
   "Ты анализируешь одиночный кадр динамичной игры. Не выдумывай скорость или невидимые препятствия. " +
   "Если персонаж в меню, умер, экран загрузки или кадр неясен, выбери wait. " +
   "В игре прыжок выполняется нажатием на свободную область экрана. Ответь коротко, только JSON. " +
   "Из-за задержки сети не утверждай, что управляешь с точностью до кадра. Выбери: {\\\"action\\\":\\\"tap\\\",\\\"x\\\":число,\\\"y\\\":число} или {\\\"action\\\":\\\"wait\\\"}. " +
   "Только JSON. Из-за сетевой задержки это экспериментальный режим, не обещай прохождение.";
  JSONObject body=new JSONObject();body.put("model","sonar");body.put("temperature",0);body.put("max_tokens",90);
  JSONArray msgs=new JSONArray();msgs.put(new JSONObject().put("role","system").put("content",prompt));
  JSONArray parts=new JSONArray();parts.put(new JSONObject().put("type","text").put("text","Реши, прыгать ли сейчас."));
  parts.put(new JSONObject().put("type","image_url").put("image_url",new JSONObject().put("url","data:image/jpeg;base64,"+b64)));
  msgs.put(new JSONObject().put("role","user").put("content",parts));body.put("messages",msgs);
  HttpURLConnection c=(HttpURLConnection)new URL("https://api.perplexity.ai/chat/completions").openConnection();
  c.setRequestMethod("POST");c.setConnectTimeout(8000);c.setReadTimeout(12000);c.setDoOutput(true);
  c.setRequestProperty("Authorization","Bearer "+apiKey);c.setRequestProperty("Content-Type","application/json");
  try(OutputStream os=c.getOutputStream()){os.write(body.toString().getBytes("UTF-8"));}
  InputStream stream=(c.getResponseCode()<400)?c.getInputStream():c.getErrorStream();
  ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=stream.read(buf))!=-1)out.write(buf,0,n);
  if(c.getResponseCode()>=400)throw new IOException("API HTTP "+c.getResponseCode()+": "+out.toString("UTF-8").substring(0,Math.min(200,out.size())));
  String text=new JSONObject(out.toString("UTF-8")).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();
  if(text.startsWith("```")){text=text.replaceAll("(?s)^```(?:json)?\\s*|\\s*```$","");}
  return text;
 }
 private void apply(JSONObject a,int w,int h){if(!"tap".equals(a.optString("action")))return;
  int x=a.optInt("x",w/2),y=a.optInt("y",h/2);if(x<0||y<0||x>=w||y>=h)return;
  Path path=new Path();path.moveTo(x,y);GestureDescription.StrokeDescription stroke=new GestureDescription.StrokeDescription(path,0,65);
  dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(),null,null);
 }
}
