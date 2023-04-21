package robertorodrigues.curso.uber.com.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

import robertorodrigues.curso.uber.com.R;
import robertorodrigues.curso.uber.com.config.ConfiguracaoFirebase;
import robertorodrigues.curso.uber.com.helper.UsuarioFirebase;
import robertorodrigues.curso.uber.com.model.Usuario;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText campoEmail, campoSenha;

    private FirebaseAuth autenticacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getSupportActionBar().hide(); // esconder toolbar

        campoEmail = findViewById(R.id.editLoginEmail);
        campoSenha = findViewById(R.id.editLoginSenha);
    }


    public  void logarUsuario(Usuario usuario){

        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();

        autenticacao.signInWithEmailAndPassword(
                usuario.getEmail(), usuario.getSenha()
        ).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {



                if (task.isSuccessful()){
                    // verificar o tipo de usuario(passageiro ou motorista)
                    UsuarioFirebase.redirecionaUsuarioLogado(LoginActivity.this);
                    finish();

                }else{
                    String excecao = "";

                    try{
                        throw task.getException();
                    }catch (FirebaseAuthInvalidUserException e){
                        excecao = "Usuario não esta cadastrado!";
                    }catch (FirebaseAuthInvalidCredentialsException e){
                        excecao = "Email ou senha não correspondem ao usuario cadastrado!";
                    }catch (Exception e){
                        excecao = "Erro ao cadastrar o usuario!"+ e.getMessage();
                        e.printStackTrace();
                    }

                    Toast.makeText(LoginActivity.this,
                            excecao,
                            Toast.LENGTH_SHORT).show();

                }
            }
        });
    }


    public void validarLoginUsuario(View view){
        String textoEmail = campoEmail.getText().toString();
        String textoSenha = campoSenha.getText().toString();

        Usuario  usuario = new Usuario();
        usuario.setEmail(textoEmail);
        usuario.setSenha(textoSenha);

        logarUsuario(usuario);

        // verifica se email e senha foram digitados
        if(!textoEmail.isEmpty()){ // verifica se o email nao esta vazio
            if(!textoSenha.isEmpty()){ // verifica se a senha nao esta vazio

            }else{
                Toast.makeText(LoginActivity.this,
                        "Preencha a senha",
                        Toast.LENGTH_SHORT).show();
            }

        }else{
            Toast.makeText(LoginActivity.this,
                    "Preencha o email",
                    Toast.LENGTH_SHORT).show();
        }


    }


}
