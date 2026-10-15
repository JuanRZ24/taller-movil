package com.fic.cursosfic.users;

import com.fic.cursosfic.BuildConfig;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class BackgroundWorker extends AsyncTask<String, Void, BackgroundWorker.ApiResult> {

    public static final String BASE_URL = BuildConfig.BASE_URL;

    private static final int TIMEOUT_MS = 15000;
    private static final String PREFS = "auth_prefs";
    private static final String KEY_TOKEN = "token";
    public static final String KEY_NAME = "user_name";
    public static final String KEY_EMAIL = "user_email";
    public static final String KEY_ROLE = "user_role";

    // Interfaz para avisarle a la pantalla que la tarea ya termino
    public interface OnTaskCompleted {
        void onCompleted(boolean isSuccess, String action, String email, ApiResult result);
    }

    private final Context context;
    private final OnTaskCompleted listener;
    private String actionExecuted;
    private String emailExecuted;

    // Constructor basico si no necesitamos avisarle a nadie al terminar
    public BackgroundWorker(Context ctx) {
        this(ctx, null);
    }

    // Constructor principal para mandar el contexto y la respuesta
    public BackgroundWorker(Context ctx, OnTaskCompleted listener) {
        this.context = ctx;
        this.listener = listener;
    }

    // Clase auxiliar para guardar el codigo HTTP y el cuerpo de la respuesta
    public static class ApiResult {
        public final int code;
        public final String body;

        public ApiResult(int code, String body) {
            this.code = code;
            this.body = body;
        }

        // Revisa si la peticion fue exitosa (codigo 200 a 299)
        public boolean isSuccess() {
            return code >= 200 && code < 300;
        }
    }

    // Hace la peticion HTTP POST en segundo plano para login o registro
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
                actionExecuted = "login";
                emailExecuted = (params.length > 1) ? params[1] : "";
                url = new URL(BASE_URL + "auth/login");
                payload.put("email", params[1]);
                payload.put("password", params[2]);
            } else if (isRegister) {
                actionExecuted = "register";
                emailExecuted = (params.length > 2) ? params[2] : "";
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
                return new ApiResult(code, "{\"message\":\"Servicio no disponible\"}");
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            return new ApiResult(code, sb.toString());

        } catch (Exception e) {
            return new ApiResult(-1, "{\"message\":\"Servicio no disponible\"}");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // Se ejecuta al terminar la peticion para guardar token y datos del usuario
    @Override
    protected void onPostExecute(ApiResult result) {
        if (result.isSuccess()) {
            try {
                JSONObject json = new JSONObject(result.body);
                String token = json.optString("token", json.optString("access_token", ""));
                if (token.isEmpty() && json.has("data")) {
                    JSONObject dataObj = json.optJSONObject("data");
                    if (dataObj != null) {
                        token = dataObj.optString("token", dataObj.optString("access_token", ""));
                    }
                }

                JSONObject user = json.optJSONObject("user");
                if (user == null && json.has("data")) {
                    user = json.optJSONObject("data");
                }

                SharedPreferences.Editor editor =
                        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();

                if (!token.isEmpty()) {
                    editor.putString(KEY_TOKEN, token);
                }
                if (user != null) {
                    editor.putString(KEY_NAME, user.optString("name", ""));
                    editor.putString(KEY_EMAIL, user.optString("email", ""));
                    editor.putString(KEY_ROLE, user.optString("role", ""));

                    String photo = user.optString("photo_url",
                            user.optString("profile_photo_url",
                            user.optString("photo",
                            user.optString("avatar", ""))));
                    if (!photo.isEmpty()) {
                        editor.putString(Profile.KEY_PROFILE_IMAGE, photo);
                    }
                }
                editor.apply();
            } catch (Exception ignored) {
            }
        }

        if (listener != null) {
            listener.onCompleted(result.isSuccess(), actionExecuted, emailExecuted, result);
        }
    }

    // Peticion GET para traer las categorias reales de la API
    public static ApiResult getCategories(Context context) {
        HttpURLConnection conn = null;
        try {
            String token = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, "");
            URL url = new URL(BASE_URL + "categories");

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/json");
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            int code = conn.getResponseCode();
            InputStream in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

            StringBuilder sb = new StringBuilder();
            if (in != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
            }
            return new ApiResult(code, sb.toString());
        } catch (Exception e) {
            return new ApiResult(-1, "{\"message\":\"Servicio no disponible\"}");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // Peticion POST para crear y publicar un curso nuevo
    public static ApiResult createCourse(Context context, String title, String description, int categoryId, String level) {
        HttpURLConnection conn = null;
        try {
            String token = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, "");
            URL url = new URL(BASE_URL + "courses");

            JSONObject payload = new JSONObject();
            payload.put("title", title);
            payload.put("description", description);
            payload.put("category_id", categoryId);
            payload.put("level", level);

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "application/json");
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }
            conn.setDoOutput(true);
            conn.setDoInput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            InputStream in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

            StringBuilder sb = new StringBuilder();
            if (in != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
            }
            return new ApiResult(code, sb.toString());
        } catch (Exception e) {
            return new ApiResult(-1, "{\"message\":\"Servicio no disponible\"}");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // Peticion POST para cambiar el rol del usuario a instructor
    public static ApiResult changeRoleToInstructor(Context context, String headline, String specialty, String biography) {
        HttpURLConnection conn = null;
        try {
            String token = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, "");
            URL url = new URL(BASE_URL + "instructors");

            JSONObject payload = new JSONObject();
            payload.put("headline", headline);
            payload.put("specialty", specialty);
            payload.put("biography", biography);

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Accept", "application/json");
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }
            conn.setDoOutput(true);
            conn.setDoInput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            InputStream in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

            StringBuilder sb = new StringBuilder();
            if (in != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
            }
            return new ApiResult(code, sb.toString());
        } catch (Exception e) {
            return new ApiResult(-1, "{\"message\":\"Servicio no disponible\"}");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // Peticion POST multipart para subir la foto de perfil del instructor
    public static ApiResult uploadPhoto(Context context, Uri imageUri) {
        HttpURLConnection conn = null;
        try {
            String token = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, "");
            URL url = new URL(BASE_URL + "instructors/me/photo");

            String boundary = "===" + System.currentTimeMillis() + "===";

            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            OutputStream outputStream = conn.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true);

            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"photo\"; filename=\"profile_photo.jpg\"\r\n");
            writer.append("Content-Type: image/jpeg\r\n\r\n");
            writer.flush();

            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
                inputStream.close();
            }

            writer.append("\r\n").flush();
            writer.append("--").append(boundary).append("--\r\n");
            writer.close();

            int code = conn.getResponseCode();
            InputStream in = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

            StringBuilder sb = new StringBuilder();
            if (in != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
            }
            return new ApiResult(code, sb.toString());
        } catch (Exception e) {
            return new ApiResult(-1, "{\"message\":\"Servicio no disponible\"}");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // Extrae un mensaje entendible del JSON devuelto por el servidor
    public static String humanMessage(String body) {
        if (body == null || body.isEmpty()) return "Servicio no disponible.";
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
            return msg.isEmpty() ? "Servicio no disponible" : msg;
        } catch (Exception ex) {
            return "Servicio no disponible";
        }
    }
}
