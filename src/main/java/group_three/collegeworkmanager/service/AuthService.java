package group_three.collegeworkmanager.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import group_three.collegeworkmanager.config.FirebaseConfig;
import group_three.collegeworkmanager.model.Role;
import group_three.collegeworkmanager.model.User;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class AuthService {
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();
    private static User currentUser = null;

    public static User signUp(String email, String password, String displayName) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);
        body.addProperty("returnSecureToken", true);

        String url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + FirebaseConfig.WEB_API_KEY;
        JsonObject response = post(url, body.toString());

        if (response.has("error")) {
            throw new Exception(response.getAsJsonObject("error").get("message").getAsString());
        }

        String uid = response.get("localId").getAsString();

        Firestore db = FirebaseService.getFirestore();
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("displayName", displayName);
        userData.put("role", null);
        db.collection("users").document(uid).set(userData).get();

        return new User(uid, email, displayName, null);
    }

    public static User signIn(String email, String password) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("email", email);
        body.addProperty("password", password);
        body.addProperty("returnSecureToken", true);

        String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + FirebaseConfig.WEB_API_KEY;
        JsonObject response = post(url, body.toString());

        if (response.has("error")) {
            throw new Exception(response.getAsJsonObject("error").get("message").getAsString());
        }

        String uid = response.get("localId").getAsString();

        Firestore db = FirebaseService.getFirestore();
        DocumentSnapshot doc = db.collection("users").document(uid).get().get();
        if (!doc.exists()) {
            throw new Exception("User record not found. Contact an administrator.");
        }

        String displayName = doc.getString("displayName");
        String roleStr = doc.getString("role");
        Role role = (roleStr != null && !roleStr.isEmpty()) ? Role.valueOf(roleStr) : null;

        currentUser = new User(uid, email, displayName, role);
        return currentUser;
    }

    public static void signOut() {
        currentUser = null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    private static JsonObject post(String url, String bodyJson) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        return GSON.fromJson(response.body(), JsonObject.class);
    }
}
