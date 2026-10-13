package com.example.gdagent;
import android.app.*;import android.os.*;import android.content.*;import android.provider.Settings;import android.text.InputType;import android.view.*;import android.widget.*;
public class MainActivity extends Activity {
 public void onCreate(Bundle b){super.onCreate(b);
  LinearLayout l=new LinearLayout(this);l.setPadding(24,24,24,24);l.setOrientation(1);
  TextView title=new TextView(this);title.setText("Geometry Dash Agent — прототип\\nРаботает только на Android 11+; управление включается вручную.\\nНе гарантирует прохождение уровней.");l.addView(title);
  EditText key=new EditText(this);key.setHint("Perplexity API key");key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
  key.setText(getPreferences(0).getString("key",""));l.addView(key);
  Button save=new Button(this);save.setText("Сохранить ключ на устройстве");save.setOnClickListener(v->{getPreferences(0).edit().putString("key",key.getText().toString().trim()).apply();GameService.apiKey=key.getText().toString().trim();Toast.makeText(this,"Сохранено",0).show();});l.addView(save);
  GameService.apiKey=getPreferences(0).getString("key","");
  Button access=new Button(this);access.setText("1. Включить службу доступности");access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));l.addView(access);
  Button start=new Button(this);start.setText("2. Включить бота (после входа в уровень)");start.setOnClickListener(v->{GameService.enabled=true;Toast.makeText(this,"Бот включён; откройте Geometry Dash",0).show();});l.addView(start);
  Button stop=new Button(this);stop.setText("Остановить бота");stop.setOnClickListener(v->{GameService.enabled=false;Toast.makeText(this,"Остановлен",0).show();});l.addView(stop);
  TextView info=new TextView(this);info.setText("Важно: служба доступности получает снимки экрана. Ключ и снимки отправляются в Perplexity при включённом боте. Чтобы полностью остановить, выключите службу в настройках Android. Не используйте для других приложений.");l.addView(info);
  setContentView(l);
 }
}
