package robertorodrigues.curso.uber.com.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import robertorodrigues.curso.uber.com.R;
import robertorodrigues.curso.uber.com.config.ConfiguracaoFirebase;
import robertorodrigues.curso.uber.com.helper.Permissoes;
import robertorodrigues.curso.uber.com.helper.UsuarioFirebase;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth autenticacao;
    private String[] permissoes = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().hide(); // ocultar toolbar

        // validar as permissoes
        Permissoes.validarPermissoes(permissoes, this,1);
        verificarGps();

        // desloga usuario
        /*autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        autenticacao.signOut(); // desloga usuario*/


    }

    public  void abrirTelaLogin(View view){
        startActivity(new Intent(this, LoginActivity.class));

    }

    public  void abrirTelaCadastro(View view){
        startActivity(new Intent(this, CadastroActivity.class));

    }

    @Override
    protected void onStart() {
        super.onStart();
        UsuarioFirebase.redirecionaUsuarioLogado(MainActivity.this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for(int permissaoResultado : grantResults){
            if (permissaoResultado == PackageManager.PERMISSION_DENIED){
                alertaValidacaoPermissao();
            }
        }
    }

    public void alertaValidacaoPermissao(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissão Negada!");
        builder.setMessage("Para utilizar o App é necessário aceitar as permissões!");
        builder.setCancelable(false);
        builder.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // abrir configurações para permitir acesso a localização
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);

            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    // permissoes para ativar o GPS
    private void verificarGps(){
        LocationManager locationManager;
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.e("gps", "GPS Desligado");

            Toast.makeText(MainActivity.this,
                    "Ative o GPS",
                    Toast.LENGTH_SHORT).show();

            //  ativaLocalizacaoGPS();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityForResult(intent, 1);



        } else {
            Log.e("gps", "GPS Ativado");

        }

    }

}
