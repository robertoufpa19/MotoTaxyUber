package robertorodrigues.curso.uber.com.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ConfiguracaoFirebase {

    private static DatabaseReference database;
    private static FirebaseAuth auth;
    private static StorageReference referenciaStorage;


    // retorna a instancia do  FirebaseDatabase
    public  static DatabaseReference getFirebaseDatabase(){
        if(database == null){
            database = FirebaseDatabase.getInstance().getReference();
        }
        return  database;
    }
    // retorna a instancia do  FirebaseAuth
    public  static FirebaseAuth getFirebaseAutenticacao(){
        if(auth == null){
            auth = FirebaseAuth.getInstance();
        }
        return  auth;
    }

    // retorna a instancia do  FirebaseStorage
    public  static StorageReference getFirebaseStorage(){

        if(referenciaStorage == null){
            referenciaStorage = FirebaseStorage.getInstance().getReference();
        }

        return  referenciaStorage;


    }



}
