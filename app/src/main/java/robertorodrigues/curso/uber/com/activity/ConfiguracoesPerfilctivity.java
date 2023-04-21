package robertorodrigues.curso.uber.com.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;

import de.hdodenhof.circleimageview.CircleImageView;
import dmax.dialog.SpotsDialog;
import robertorodrigues.curso.uber.com.R;
import robertorodrigues.curso.uber.com.config.ConfiguracaoFirebase;
import robertorodrigues.curso.uber.com.helper.Permissoes;
import robertorodrigues.curso.uber.com.helper.UsuarioFirebase;
import robertorodrigues.curso.uber.com.model.Usuario;

public class ConfiguracoesPerfilctivity extends AppCompatActivity {

    private EditText editUsuarioNome, editUsuarioCidade, editUsuarioBairro,
            editUsuarioRua, editUsuarioNumeroCasa;
    private CircleImageView imagePerfilUsuario;

    private static  final int SELECAO_GALERIA = 200;

    private String urlImagemSelecionada = null;

    private Usuario usuarioLogado;
    private String idUsuarioLogado;
    private DatabaseReference firebaseRef;
    private StorageReference storageReference;

    private AlertDialog dialog;

    private String[] permissoesNecessarias = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracoes_perfilctivity);

        getSupportActionBar().setTitle("Configurações");

        // validar permissões
        Permissoes.validarPermissoes(permissoesNecessarias, ConfiguracoesPerfilctivity.this, 1);

        //configurações iniciais
        inicializarComponentes();
        usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
        idUsuarioLogado = UsuarioFirebase.getIdentificadorUsuario();
        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();
        storageReference = ConfiguracaoFirebase.getFirebaseStorage();

        // selecionar foto de perfil
        imagePerfilUsuario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                if(i.resolveActivity(getPackageManager()) != null){
                    startActivityForResult(i, SELECAO_GALERIA);

                }

            }
        });

        recuperarDadosUsuario();

    }

    private  void recuperarDadosUsuario(){
        DatabaseReference usuarioRef = firebaseRef
                .child("usuarios")
                .child(idUsuarioLogado);
        usuarioRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.getValue() != null){
                    Usuario usuario = snapshot.getValue(Usuario.class);
                    editUsuarioNome.setText(usuario.getNome());
                    editUsuarioCidade.setText(usuario.getCidade());
                    editUsuarioBairro.setText(usuario.getBairro());
                    editUsuarioRua.setText(usuario.getRua());
                    editUsuarioNumeroCasa.setText(usuario.getNumeroCasa());

                    //recuperar imagem de perfil da empresa
                    urlImagemSelecionada = usuario.getUrlImagem();
                    if (  urlImagemSelecionada != null ){ // urlImagemSelecionada != ""
                        Picasso.get()
                                .load(urlImagemSelecionada)
                                .into(imagePerfilUsuario);
                    }



                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }



    public void validarDadosUsuario(View view) {  // metodo salvar

        // validar os campos que foram preenchidos
        String nome = editUsuarioNome.getText().toString();
        String cidade = editUsuarioCidade.getText().toString();
        String bairro = editUsuarioBairro.getText().toString();
        String rua = editUsuarioRua.getText().toString();
        String numero = editUsuarioNumeroCasa.getText().toString();
        String foto = urlImagemSelecionada;


        if(foto != null) {
            if (!nome.isEmpty()) {
                if (!cidade.isEmpty()) {
                    if (!bairro.isEmpty()) {
                        if (!rua.isEmpty()) {
                            if (!numero.isEmpty()) {

                                        atualizarFotoUsuario(Uri.parse(foto));
                                        // usuarioLogado.setIdUsuario(idUsuarioLogado); // teste colocar depois se dar  erro

                                        atualizarNomeUsuario(nome);
                                        usuarioLogado.setCidade(cidade);
                                        usuarioLogado.setBairro(bairro);
                                        usuarioLogado.setRua(rua);
                                        usuarioLogado.setNumeroCasa(numero);


                                        // indentificador Usuario Token para enviar notificação para um usuario
                                        // inicio cadastro do token usuario
                                        FirebaseMessaging.getInstance().getToken()
                                                .addOnCompleteListener(new OnCompleteListener<String>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<String> task) {
                                                        if (!task.isSuccessful()) {
                                                            Log.w("Cadastro token", "Fetching FCM registration token failed", task.getException());
                                                            return;
                                                        }

                                                        // Get new FCM registration token
                                                        String token = task.getResult();
                                                        usuarioLogado.setToken(token);
                                                        usuarioLogado.setId(idUsuarioLogado); // teste
                                                        usuarioLogado.atualizar();
                                                        exibirMensagem("Dados atualizados");
                                                        finish();//


                                                    }
                                                });    // fim cadastro do token




                            } else {
                                exibirMensagem("Digite o numero da sua casa!");
                            }

                        } else {
                            exibirMensagem("Digite o nome da sua rua!");
                        }

                    } else {
                        exibirMensagem("Digite o nome do seu bairro!");
                    }

                } else {
                    exibirMensagem("Digite o nome da sua cidade!");
                }


            } else {
                exibirMensagem("Digite seu nome!");
            }

        }else {
            exibirMensagem("Configure uma foto de Perfil!");
        }


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK){
            Bitmap imagem = null;

            try {
                switch (requestCode){
                    case  SELECAO_GALERIA:
                        Uri localImagem = data.getData();
                        imagem = MediaStore.Images.Media.getBitmap(getContentResolver(), localImagem);
                        break;
                }

                if(imagem != null){
                    imagePerfilUsuario.setImageBitmap(imagem);

                    // fazer upload da imagem para o firebase storage
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    imagem.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                    byte[] dadosImagem = baos.toByteArray();

                    // fazer upload da imagem antes de preencher outros campos
                    dialog = new SpotsDialog.Builder()
                            .setContext(this)
                            .setMessage("Carregando dados")
                            .setCancelable(false)
                            .build();
                    dialog.show();

                    final StorageReference imageRef = storageReference
                            .child("imagens")
                            .child("usuarios")
                            .child(idUsuarioLogado + "jpeg");

                    UploadTask uploadTask = imageRef.putBytes(dadosImagem);
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            exibirMensagem("Erro ao fazer upload da imagem!");
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // recupera a url da imagem (versao atualizada do firebase)
                            imageRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    Uri url =   task.getResult();
                                    urlImagemSelecionada = url.toString();


                                }
                            });

                            dialog.dismiss(); // fecha o carregando
                            exibirMensagem("Sucesso ao fazer upload da imagem!");
                        }
                    });

                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void atualizarFotoUsuario(Uri url){
        UsuarioFirebase.atualizarFotoUsuario(url);
        boolean retorno = UsuarioFirebase.atualizarFotoUsuario(url);
        if(retorno){
            usuarioLogado.setUrlImagem(url.toString());
            usuarioLogado.setId(idUsuarioLogado); // teste
            usuarioLogado.atualizar();
            // exibirMensagem("foto alterada!");
        }

    }

    public void atualizarNomeUsuario(String nome){
        UsuarioFirebase.atualizarNomeUsuario(nome);
        boolean retorno = UsuarioFirebase.atualizarNomeUsuario(nome);
        if(retorno){
            usuarioLogado.setNome(nome);
            usuarioLogado.setId(idUsuarioLogado); // teste
            usuarioLogado.atualizar();
            //  exibirMensagem("nome alterado!");
        }

    }

    private void inicializarComponentes(){
      editUsuarioNome = findViewById(R.id.nomeUsuarioConfig);
      editUsuarioCidade = findViewById(R.id.cidadeUsuarioConfig);
      editUsuarioBairro = findViewById(R.id.bairroUsuarioConfig);
      editUsuarioRua = findViewById(R.id.ruaUsuarioConfig);
      editUsuarioNumeroCasa = findViewById(R.id.numeroCasaUsuarioConfig);
      imagePerfilUsuario = findViewById(R.id.imagePerfilUsuarioConfig);

    }

    private void exibirMensagem(String texto){
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show();
    }


}