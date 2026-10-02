package com.fic.cursosfic.users;

import com.fic.cursosfic.BuildConfig;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class BackgroundWorker extends AsyncTask<String, Void, BackgroundWorker.ApiResult> {

    public static final String BASE_URL = BuildConfig.BASE_URL;

    private static final int TIMEOUT_MS = 15000;
    private static final String PREFS = "auth_prefs";
    private static final String KEY_TOKEN = "token";

    private final Context context;
    private AlertDialog alertDialog;

    public BackgroundWorker(Context ctx) {
        this.context = ctx;
    }

    public static class ApiResult {
        public final int code;
        public final String body;

        ApiResult(int code, String body) {
            this.code = code;
            this.body = body;
        }

        public boolean isSuccess() {
            return code >= 200 && code < 300;
        }
    }

    @Override
    protected ApiResult doInBackground(String... params) {
        HttpURLConnection conn = null;
        try {
            if (params == null || params.length == 0) {
                return new ApiResult(-1, "{\"message\":\"Parametros insuficientes.\"}");
            }
            String action = params[0];
            URL url;
            JSONObject payload = new JSONObject();

            boolean isLogin = "login".equalsIgnoreCase(action) || "0".equals(action);
            boolean isRegister = "register".equalsIgnoreCase(action) || "1".equals(action);

            if (isLogin) {
                url = new URL(BASE_URL + "auth/login");
                payload.put("email", params[1]);
                payload.put("password", params[2]);
            } else if (isRegister) {
                url = new URL(BASE_URL + "auth/register");
                payload.put("name", params[1]);
                payload.put("email", params[2]);
                payload.put("password", params[3]);
                payload.put("password_confirmation", params[4]);
            } else {
                return new ApiResult(-1, "{\"message\":\"Operacion no valida.\"}");
            }

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            InputStream in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

            if (in == null) {
                return new ApiResult(code, "{\"message\":\"El servidor no devolvio cuerpo (codigo " + code + ").\"}");
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            return new ApiResult(code, sb.toString());

        } catch (java.net.SocketTimeoutException e) {
            return new ApiResult(-1, "{\"message\":\"El servidor tardo demasiado en responder.\"}");
        } catch (Exception e) {
            // toString() y no getMessage(): ConnectException suele traer null
            return new ApiResult(-1, "{\"message\":\"No se pudo conectar con el servidor. Verifica la IP/puerto. Detalle: " + escape(e.toString()) + "\"}");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    @Override
    protected void onPreExecute() {
        new Handler(Looper.getMainLooper()).post(() -> {
            alertDialog = new AlertDialog.Builder(context).create();
            alertDialog.setTitle("Autenticacion");
            alertDialog.setMessage("Procesando...");
            alertDialog.setCancelable(false);
            alertDialog.show();
        });
    }

    @Override
    protected void onPostExecute(ApiResult result) {
        String message;
        if (result.isSuccess()) {
            try {
                JSONObject json = new JSONObject(result.body);
                JSONObject user = json.optJSONObject("user");
                String token = json.optString("token", "");

                if (!token.isEmpty()) {
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            .edit().putString(KEY_TOKEN, token).apply();
                }

                message = "OK (" + result.code + ")\nRol: " + user.optString("role")
                        + "\nToken guardado enSharedPreferences";
            } catch (Exception ex) {
                message = "Respuesta 2xx pero no se pudo leer el JSON: " + result.body;
            }
        } else {
            message = "Error " + result.code + "\n" + humanMessage(result.body);
        }

        final String finalMessage = message;
        new Handler(Looper.getMainLooper()).post(() -> {
            if (alertDialog != null && alertDialog.isShowing()) {
                alertDialog.setMessage(finalMessage);
                alertDialog.setCancelable(true);
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                        (d, w) -> alertDialog.dismiss());
            }
        });
    }

    /** Extrae "message" y, si hay 422, tambien el mapa "errors". */
    private static String humanMessage(String body) {
        if (body == null || body.isEmpty()) return "Respuesta vacia del servidor.";
        try {
            JSONObject json = new JSONObject(body);
            JSONObject errors = json.optJSONObject("errors");
            if (errors != null) {
                StringBuilder sb = new StringBuilder();
                for (java.util.Iterator<String> it = errors.keys(); it.hasNext(); ) {
                    String key = it.next();
                    sb.append(key).append(": ").append(errors.optString(key)).append("\n");
                }
                return sb.toString().trim();
            }
            String msg = json.optString("message");
            return msg.isEmpty() ? body : msg;
        } catch (Exception ex) {
            return body;
        }
    }

    private static String escape(String s) {
        return s == null ? "desconocido" : s.replace('"', '\'');
    }
}