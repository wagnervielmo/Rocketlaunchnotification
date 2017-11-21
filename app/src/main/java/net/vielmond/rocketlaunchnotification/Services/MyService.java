package net.vielmond.rocketlaunchnotification.Services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.SQLException;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import net.vielmond.rocketlaunchnotification.Dao.LaunchDAO;
import net.vielmond.rocketlaunchnotification.Entidades.Launch;
import net.vielmond.rocketlaunchnotification.Util.VerificaConexao;
import net.vielmond.rocketlaunchnotification.dbHelper.DBHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by root on 07/10/17.
 */

public class MyService extends Service {
    private URL url = null;
    private String json_url;
    private String JSON_STRING;
    private Launch r;
    private Cursor c;
    //private List<Worker> workerList = new ArrayList<>();
    private static final String TAG = "launchSchedule";
    private LaunchDAO db;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "onCreate()");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "onStartCommand()");

        Worker w = new Worker(this, new Intent("ROCKETLAUNCH_SERVICE"), startId);
        w.start();
        //workerList.add(w);

        return (super.onStartCommand(intent, flags, startId));
    }


    class Worker extends Thread {

        public Context context;
        public Intent intent;
        public int startId;
        public boolean ativo = true;

        public Worker(Context context, Intent intent, int startId) {
            this.context = context;
            this.intent = intent;
            this.startId = startId;
            db = new LaunchDAO(context);
        }

        public void run() {
            if (ativo) {
                try {

                    Thread.sleep(30000);

                    if (new VerificaConexao(context).verificaConexao()) {
                        Log.i(TAG, "Conectado à internet - iniciando atualização do Sqlite!");

                        // deleta os dados e adiciona novamente atualizado

                        db.open();
                        db.delete();
                        db.close();
                        json_url = "http://vielmond.net:8080/rocketschedule/launch_json";

                        try {
                            url = new URL(json_url);
                            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                            InputStream inputStream = httpURLConnection.getInputStream();
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                            StringBuilder stringBuilder = new StringBuilder();
                            while ((JSON_STRING = bufferedReader.readLine()) != null) {
                                stringBuilder.append(JSON_STRING + "\n");
                            }

                            JSONArray jsonArray = new JSONArray(stringBuilder.toString());
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);

                                Launch r = new Launch();
                                r.setId(jsonObject.getInt("id"));
                                r.setTitulo(jsonObject.getString("titulo"));
                                r.setDescricao(jsonObject.getString("descricao"));
                                r.setJanelaLancamento(jsonObject.getString("janelaLancamento"));
                                r.setLocalLancamento(jsonObject.getString("localLancamento"));
                                r.setLinkTransmissao(jsonObject.getString("linkTransmissao"));
                                r.setDiaLancamento(jsonObject.getInt("diaLancamento"));
                                r.setMesLancamento(jsonObject.getInt("mesLancamento"));
                                r.setAnoLancamento(jsonObject.getInt("anoLancamento"));
                                r.setHoraLancamento(jsonObject.getInt("horaLancamento"));
                                r.setMinutoLancamento(jsonObject.getInt("minutoLancamento"));
                                r.setSegundoLancamento(jsonObject.getInt("segundoLancamento"));
                                r.setDatahoraLancamento(jsonObject.getString("datahora_lancamento"));

                                db.open();
                                if (db.add(r) == -1) {
                                    Log.i(TAG, "id ja existente.");
                                } else {
                                    Log.i(TAG, "novos lançamentos cadastrados!");
                                }
                                db.close();

                            }

                            bufferedReader.close();
                            inputStream.close();
                            httpURLConnection.disconnect();

                        } catch (IOException ex) {
                            Log.i(TAG, ex.toString());
                        } catch (NullPointerException ex) {
                            Log.i(TAG, ex.toString());
                        } catch (JSONException ex) {
                            Log.i(TAG, ex.toString());
                        }

                        db.open();
                        c = db.getProximoEvento();
                        r = new Launch();
                        r.setId(c.getInt(c.getColumnIndex(DBHelper.LAUNCH_ID)));
                        r.setTitulo(c.getString(c.getColumnIndex(DBHelper.LAUNCH_TITULO)));
                        r.setDescricao(c.getString(c.getColumnIndex(DBHelper.LAUNCH_DESCRICAO)));
                        r.setJanelaLancamento(c.getString(c.getColumnIndex(DBHelper.LAUNCH_JANELA)));
                        r.setLocalLancamento(c.getString(c.getColumnIndex(DBHelper.LAUNCH_LOCAL)));
                        r.setLinkTransmissao(c.getString(c.getColumnIndex(DBHelper.LAUNCH_LINK)));
                        r.setDiaLancamento(c.getInt(c.getColumnIndex(DBHelper.LAUNCH_DIA)));
                        r.setMesLancamento(c.getInt(c.getColumnIndex(DBHelper.LAUNCH_MES)));
                        r.setAnoLancamento(c.getInt(c.getColumnIndex(DBHelper.LAUNCH_ANO)));
                        r.setHoraLancamento(c.getInt(c.getColumnIndex(DBHelper.LAUNCH_HORA)));
                        r.setMinutoLancamento(c.getInt(c.getColumnIndex(DBHelper.LAUNCH_MINUTO)));
                        r.setSegundoLancamento(c.getInt(c.getColumnIndex(DBHelper.LAUNCH_SEGUNDO)));
                        r.setNotificado(c.getInt(c.getColumnIndex(DBHelper.LAUNCH_NOTIFICADO)));

                        db.close();
                    } else {
                        Log.i(TAG, "Sem conexão com a internet, mantém tudo como está!.");
                    }

                    if (r.getId() != null) {

                        Calendar setcalendar = Calendar.getInstance();

                        setcalendar.set(Calendar.DAY_OF_MONTH, r.getDiaLancamento());
                        setcalendar.set(Calendar.HOUR_OF_DAY, r.getHoraLancamento());
                        setcalendar.set(Calendar.MINUTE, r.getMinutoLancamento());
                        setcalendar.set(Calendar.SECOND, r.getSegundoLancamento());

                        // parametros
                        intent.putExtra("id", r.getId());
                        intent.putExtra("titulo", r.getTitulo());
                        intent.putExtra("descricao", r.getDescricao());
                        intent.putExtra("dia", r.getDiaLancamento());
                        intent.putExtra("mes", r.getMesLancamento());
                        intent.putExtra("ano", r.getAnoLancamento());
                        intent.putExtra("hora", r.getHoraLancamento());
                        intent.putExtra("minuto", r.getMinutoLancamento());
                        intent.putExtra("segundo", r.getSegundoLancamento());
                        intent.putExtra("janela", r.getJanelaLancamento());
                        intent.putExtra("link", r.getLinkTransmissao());
                        intent.putExtra("local", r.getLocalLancamento());

                        //agendado para:
                        Log.i(TAG, "Agendado para: " + r.getDiaLancamento() + "."
                                + r.getMesLancamento() + "." + r.getAnoLancamento() + " as "
                                + r.getHoraLancamento() + ":" + r.getMinutoLancamento() + ":" + r.getSegundoLancamento());


                        PendingIntent pendingIntent =
                                PendingIntent.getBroadcast(context, r.getId(), intent, 0);

                        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                        alarm.set(AlarmManager.RTC_WAKEUP, setcalendar.getTimeInMillis(), pendingIntent);

                    } else {
                        Log.i(TAG, "nenhum lançamento encontrado");
                        ;
                    }




                } catch (CursorIndexOutOfBoundsException | SQLException e) {
                    Log.i(TAG, e.toString());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            stopSelf(startId);

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

//        for (int i = 0, tam = workerList.size(); i < tam; i++) {
//            workerList.get(i).ativo = false;
//        }

        Log.i(TAG, "ThreadsKilled ");

//        Worker.interrupted();
    }
}
