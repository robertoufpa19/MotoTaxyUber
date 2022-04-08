package robertorodrigues.curso.uber.com.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import de.hdodenhof.circleimageview.CircleImageView;
import robertorodrigues.curso.uber.com.R;
import robertorodrigues.curso.uber.com.config.ConfiguracaoFirebase;
import robertorodrigues.curso.uber.com.helper.UsuarioFirebase;
import robertorodrigues.curso.uber.com.model.Usuario;

public class CadastroActivity extends AppCompatActivity {

    private TextInputEditText campoNome, campoEmail, campoSenha;
    private Switch switchTipoUsuario;
    private FirebaseAuth autenticacao;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        getSupportActionBar().hide(); // esconder toolbar

        //iniciar os componentes
        campoNome = findViewById(R.id.editCadastroNome);
        campoEmail = findViewById(R.id.editCadastroEmail);
        campoSenha = findViewById(R.id.editCadastroSenha);
        switchTipoUsuario = findViewById(R.id.switchTipoUsuario);



    }

    public void validarCadastroUsuario(View view){

        //recuperar textos dos campos
        String textoNome = campoNome.getText().toString();
        String textoEmail = campoEmail.getText().toString();
        String textoSenha = campoSenha.getText().toString();

        if(!textoNome.isEmpty()){ // verifica se o campo nome nao esta vazio
            if(!textoEmail.isEmpty()){ // verifica se o campo email nao esta vazio
                if(!textoSenha.isEmpty()){ // verifica se o campo senha nao esta vazio

                    Usuario usuario = new Usuario();
                    usuario.setNome(textoNome);
                    usuario.setEmail(textoEmail);
                    usuario.setSenha(textoSenha);
                    usuario.setTipo(verificaTipoUsuario());

                    cadastrarUsuario(usuario);

                }else{
                    Toast.makeText(
                            CadastroActivity.this,
                            "Preencha a Senha!",
                            Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(
                        CadastroActivity.this,
                        "Preencha o E-mail!",
                        Toast.LENGTH_SHORT).show();
            }
        }else{
            Toast.makeText(
                    CadastroActivity.this,
                    "Preencha o Nome!",
                    Toast.LENGTH_SHORT).show();
        }

    }

    public  void cadastrarUsuario(final Usuario usuario){

        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        autenticacao.createUserWithEmailAndPassword(
                usuario.getEmail(),
                usuario.getSenha()
        ).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if(task.isSuccessful()){

                    try {

                        String idUsuario  = task.getResult().getUser().getUid();
                        usuario.setId(idUsuario);
                        usuario.salvar();

                        // atualizar nome do UserProfile
                        UsuarioFirebase.atualizarNomeUsuario(usuario.getNome());

                        //redireciona usuario com base no seu tipo(Passageiro ou Motorista)
                        // se usuario for passageiro sera chamado a activity maps
                        // senao chama a activity requisicoes

                        if(verificaTipoUsuario() == "P"){ // passageiro
                            startActivity(new Intent(CadastroActivity.this, PassageiroActivity.class));
                            finish();
                            Toast.makeText(
                                    CadastroActivity.this,
                                    "Sucesso ao cadastrar Passageiro!",
                                    Toast.LENGTH_SHORT).show();
                        }else{
                            startActivity(new Intent(CadastroActivity.this, RequisicoesActivity.class));
                            finish();
                            Toast.makeText(
                                    CadastroActivity.this,
                                    "Sucesso ao cadastrar Motorista!",
                                    Toast.LENGTH_SHORT).show();

                        }


                    }catch (Exception e){
                        e.printStackTrace();
                    }


                }else{

                    String excecao = "";

                    try{
                        throw task.getException();
                    }catch (FirebaseAuthWeakPasswordException e){
                        excecao = "Digite uma senha mais forte!";
                    }catch (FirebaseAuthInvalidCredentialsException e){
                        excecao = "Digite uma email valido!";
                    }catch (FirebaseAuthUserCollisionException e){
                        excecao = "Esta conta ja foi cadastrada!";
                    }catch (Exception e){
                        excecao = "Erro ao cadastrar o usuario!"+ e.getMessage();
                        e.printStackTrace();
                    }

                    Toast.makeText(CadastroActivity.this,
                            excecao,
                            Toast.LENGTH_SHORT).show();

                }
            }
        });
    }


    public  String verificaTipoUsuario(){

        //  "?" operador ternario   // funciona como o if  e else
        // "M" motorista e  "P" passageiro

        // se switchTipoUsuario esta selecionado ele vai executar o que esta apos a "?"
        // se switchTipoUsuario nao esta selecionado ele vai executar o que esta apos a ":"

        // verifica se esta selecionado       //true  // false
        return  switchTipoUsuario.isChecked()  ? "M" : "P";
    }

}
