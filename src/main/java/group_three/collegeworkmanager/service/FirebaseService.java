package group_three.collegeworkmanager.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import group_three.collegeworkmanager.config.FirebaseConfig;

import java.io.FileInputStream;
import java.io.IOException;

public class FirebaseService {
    private static boolean initialized = false;

    public static void initialize() {
        if (initialized) return;
        try {
            FileInputStream serviceAccount = new FileInputStream(FirebaseConfig.SERVICE_ACCOUNT_PATH);
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            FirebaseApp.initializeApp(options);
            initialized = true;
        } catch (IOException e) {
            throw new RuntimeException("Firebase init failed. Ensure serviceAccountKey.json is in the project root.\n" + e.getMessage(), e);
        }
    }

    public static Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }

    public static FirebaseAuth getAuth() {
        return FirebaseAuth.getInstance();
    }
}
