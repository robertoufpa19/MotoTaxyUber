package robertorodrigues.curso.uber.com.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import dmax.dialog.SpotsDialog;
import robertorodrigues.curso.uber.com.R;
import robertorodrigues.curso.uber.com.config.ConfiguracaoFirebase;
import robertorodrigues.curso.uber.com.helper.Local;
import robertorodrigues.curso.uber.com.helper.UsuarioFirebase;
import robertorodrigues.curso.uber.com.model.Destino;
import robertorodrigues.curso.uber.com.model.Requisicao;
import robertorodrigues.curso.uber.com.model.Usuario;

public class PassageiroActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    /*
     * Lat/lon destino:-23.556407, -46.662365 (Av. Paulista, 2439)
     * Lat/lon passageiro: -23.562791, -46.654668
     * Lat/lon Motorista (a caminho):
     *   inicial: -23.563196, -46.650607
     *   intermediaria: -23.564801, -46.652196
     *   final: -23.563136, -46.654247
     * */

    //Componentes
    private EditText editDestino;
    private LinearLayout linearLayoutDestino;
    private Button buttonChamarUber;

    private GoogleMap mMap;
    private FirebaseAuth autenticacao;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng localPassageiro;
    private boolean cancelarUber = false;
    private DatabaseReference firebaseRef;
    private Requisicao requisicao;

    private Usuario passageiro;
    private String statusRequisicao;
    private Destino destino;

    private Marker marcadorMotorista;
    private Marker marcadorPassageiro;
    private Marker marcadorDestino;

    private Usuario motorista;
    private LatLng localMotorista;
    private FloatingActionButton fabRota;

    private android.app.AlertDialog dialog;

    private String idUsuarioLogado;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passageiro);

        inicializarComponentes();

        //Adiciona listener para status da requisição
        if(autenticacao != null){
            verificaStatusRequisicao();
        }else{
            abrirHome();
        }


        

    }



    private void verificaStatusRequisicao(){

        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
        DatabaseReference requisicoes = firebaseRef.child("requisicoes");
        Query requisicaoPesquisa = requisicoes.orderByChild("passageiro/id")
                .equalTo( usuarioLogado.getId() );

        requisicaoPesquisa.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                List<Requisicao> lista = new ArrayList<>();
                for( DataSnapshot ds: dataSnapshot.getChildren() ){
                    lista.add( ds.getValue( Requisicao.class ) );
                }

                Collections.reverse(lista);
                if( lista!= null && lista.size()>0 ){
                    requisicao = lista.get(0);

                    if(requisicao != null){
                        if(!requisicao.getStatus().equals(Requisicao.STATUS_ENCERRADA)) { // caso a requisicao nao esteja com status encerrada
                            passageiro = requisicao.getPassageiro();
                            localPassageiro = new LatLng(
                                    Double.parseDouble(passageiro.getLatitude()),
                                    Double.parseDouble(passageiro.getLongitude())
                            );
                            statusRequisicao = requisicao.getStatus();
                            destino = requisicao.getDestino(); // recupera os dados do destino
                            if (requisicao.getMotorista() != null) {
                                motorista = requisicao.getMotorista();
                                localMotorista = new LatLng(
                                        Double.parseDouble(motorista.getLatitude()),
                                        Double.parseDouble(motorista.getLongitude())
                                );
                            }

                            alteraInterfaceStatusRequisicao(statusRequisicao);
                        }
                    }

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public void alteraInterfaceStatusRequisicao(String status){
                             // nao vazio
       if(status != null && !status.isEmpty()) {
           cancelarUber = false;
           switch (status) {
               case Requisicao.STATUS_AGUARDANDO:
                   requisicaoAguardando();
                   break;
               case Requisicao.STATUS_A_CAMINHO:
                   requisicaoACaminho();
                   break;
               case Requisicao.STATUS_VIAGEM:
                   requisicaoViagem();
                   break;
               case Requisicao.STATUS_FINALIZADA:
                   requisicaoFinalizada();
                   break;
               case Requisicao.STATUS_CANCELADA:
                   requisicaoCancelada();
                   break;
           }
       }else{
          // adiciona marcador do passageiro
          adicionaMarcadorPassageiro(localPassageiro,"Seu Local");
          centralizarMarcador(localPassageiro);
       }

    }

    private void requisicaoCancelada(){
        linearLayoutDestino.setVisibility( View.VISIBLE );
        buttonChamarUber.setText("Chamar Uber");
        cancelarUber = false;
    }

    public void requisicaoAguardando(){
        linearLayoutDestino.setVisibility( View.GONE );
        buttonChamarUber.setText("Cancelar Uber");
        cancelarUber = true;

        // adicionar Marcador Passageiro
        adicionaMarcadorPassageiro(localPassageiro, passageiro.getNome());
        centralizarMarcador(localPassageiro);


    }

    public void requisicaoACaminho(){
        linearLayoutDestino.setVisibility( View.GONE );
        fabRota.setVisibility(View.VISIBLE);
        buttonChamarUber.setText("Motorista a Caminho");
        buttonChamarUber.setEnabled(false);


        // adicionarMarcado rPassageiro
        adicionaMarcadorPassageiro(localPassageiro, passageiro.getNome());
        // adicionarMarcador Motorista
        adicionaMarcadorMotorista(localMotorista, motorista.getNome());
        // adicionar dois Marcador Passageiro // motorista
        centralizarDoisMarcadores(marcadorMotorista, marcadorPassageiro);

    }
    public void requisicaoViagem(){
        linearLayoutDestino.setVisibility( View.GONE );
        fabRota.setVisibility(View.VISIBLE);
        buttonChamarUber.setText("A caminho do destino");
        buttonChamarUber.setEnabled(false);
        // adicionarMarcador Motorista
        adicionaMarcadorMotorista(localMotorista, motorista.getNome());
        // adicionarMarcador Destino
        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );
        adicionaMarcadorDestino(localDestino, "Destino");
        // adicionar dois Marcador motorista / destino
        centralizarDoisMarcadores(marcadorMotorista, marcadorDestino);

    }

    public void requisicaoFinalizada(){

        linearLayoutDestino.setVisibility( View.GONE );
        fabRota.setVisibility(View.GONE);
        buttonChamarUber.setEnabled(false);

        // adicionarMarcador Destino
        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );
        adicionaMarcadorDestino(localDestino, "Destino");
         // centraliza marcador de destino
        centralizarMarcador(localDestino);

        // Calcular distancia
        float distancia = Local.calcularDistancia(localPassageiro, localDestino);
        float valor = distancia*3; // R$3,00 cada KM percorrido
        DecimalFormat decimal = new DecimalFormat("0.00");  // formato em que vai ficar o valor em dinheiro
        final String resultado = decimal.format(valor);
        buttonChamarUber.setText("Corrida Finalizada - R$ "+resultado);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle("Total da Viagem")
                        .setMessage("Sua viagem ficou: R$ "+resultado)
                        .setCancelable(false)
                        .setNegativeButton("Encerrar Viagem", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                requisicao.setStatus(Requisicao.STATUS_ENCERRADA);
                                requisicao.atualizarStatus();
                                finish();
                                startActivity(new Intent(getIntent())); // propria activity do passageiro

                            }
                        });
        AlertDialog dialog = builder.create();
           dialog.show();
    }

    private void adicionaMarcadorPassageiro(LatLng localizacao, String titulo){

        if( marcadorPassageiro != null )
            marcadorPassageiro.remove();

        marcadorPassageiro = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario))
        );

    }

    private void adicionaMarcadorMotorista(LatLng localizacao, String titulo){

        if( marcadorMotorista != null )
            marcadorMotorista.remove();

        marcadorMotorista = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.carro))
        );

    }

    private void adicionaMarcadorDestino(LatLng localizacao, String titulo){
        // remove marcador de passageiro
        if( marcadorPassageiro != null )
            marcadorPassageiro.remove();

        if( marcadorDestino != null )
            marcadorDestino.remove();

        marcadorDestino = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.destino))
        );

    }

    // centraliza marcador motorista ou passageiro
    private void centralizarMarcador(LatLng local){
        mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(local, 20)
        );
    }

 // centraliza marcador motorista e passageiro

    private void centralizarDoisMarcadores(Marker marcador1, Marker marcador2){

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        builder.include( marcador1.getPosition() );
        builder.include( marcador2.getPosition() );

        LatLngBounds bounds = builder.build();

        int largura = getResources().getDisplayMetrics().widthPixels;
        int altura = getResources().getDisplayMetrics().heightPixels;
        int espacoInterno = (int) (largura * 0.20);

        mMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(bounds,largura,altura,espacoInterno)
        );

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Recuperar localizacao do usuário
        recuperarLocalizacaoUsuario();

    }

    public void chamarUber(View view){
         // false -> uber nao pode ser cancelado
        //true -> uber  pode ser cancelado
        if( cancelarUber ){//Uber pode ser cancelado (cancelarUber == true)
            //Cancelar a requisição
             requisicao.setStatus(Requisicao.STATUS_CANCELADA);
             requisicao.atualizarStatus();


        }else {

            String enderecoDestino = editDestino.getText().toString();

            if( !enderecoDestino.equals("")){

                Address addressDestino = recuperarEndereco( enderecoDestino );
                if( addressDestino != null ){

                    final Destino destino = new Destino();
                    destino.setEstado( addressDestino.getAdminArea() ); // busca Estado
                    destino.setCidade( addressDestino.getSubAdminArea()); // busca cidade
                    destino.setCep( addressDestino.getPostalCode() );   // busca Cep
                    destino.setBairro( addressDestino.getSubLocality() ); // busca bairro
                    destino.setRua( addressDestino.getThoroughfare() );   // busca rua
                    destino.setNumero( addressDestino.getFeatureName() ); // busca numero de endereço
                    destino.setLatitude( String.valueOf(addressDestino.getLatitude()) );
                    destino.setLongitude( String.valueOf(addressDestino.getLongitude()) );

                    StringBuilder mensagem = new StringBuilder();
                    mensagem.append( "Estado: " + destino.getEstado() );
                    mensagem.append( "\nCidade: " + destino.getCidade() );
                    mensagem.append( "\nRua: " + destino.getRua() );
                    mensagem.append( "\nBairro: " + destino.getBairro() );
                    mensagem.append( "\nNúmero: " + destino.getNumero() );
                    mensagem.append( "\nCep: " + destino.getCep() );

                    AlertDialog.Builder builder = new AlertDialog.Builder(this)
                            .setTitle("Confirme seu endereço de destiona!")
                            .setMessage(mensagem)
                            .setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    //salvar requisição
                                    salvarRequisicao( destino );


                                }
                            }).setNegativeButton("cancelar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();

                }

            }else {
                Toast.makeText(this,
                        "Informe o endereço de destino!",
                        Toast.LENGTH_SHORT).show();
            }

        }

    }

    private void salvarRequisicao(Destino destino){

        Requisicao requisicao = new Requisicao();
        requisicao.setDestino( destino );

        Usuario usuarioPassageiro = UsuarioFirebase.getDadosUsuarioLogado();
        usuarioPassageiro.setLatitude( String.valueOf( localPassageiro.latitude ) );
        usuarioPassageiro.setLongitude( String.valueOf( localPassageiro.longitude ) );

        requisicao.setPassageiro( usuarioPassageiro );
        requisicao.setStatus( Requisicao.STATUS_AGUARDANDO );
        requisicao.salvar();

        linearLayoutDestino.setVisibility( View.GONE );
        buttonChamarUber.setText("Cancelar Uber");

    }

    private Address recuperarEndereco(String endereco){

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> listaEnderecos = geocoder.getFromLocationName(endereco, 1);
            if( listaEnderecos != null && listaEnderecos.size() > 0 ){
                Address address = listaEnderecos.get(0);

                return address;

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

    private void recuperarLocalizacaoUsuario() {

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);


        dialog = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Carregando sua localização...")
                .setCancelable(false)
                .build();
        dialog.show();

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                //recuperar latitude e longitude
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                localPassageiro = new LatLng(latitude, longitude);

                //Atualizar GeoFire
                UsuarioFirebase.atualizarDadosLocalizacao(latitude, longitude);

                // alterar interface de acordo com o staus
                alteraInterfaceStatusRequisicao(statusRequisicao);
                if(statusRequisicao != null && !statusRequisicao.isEmpty()) {
                    if (statusRequisicao.equals(Requisicao.STATUS_VIAGEM)
                            || statusRequisicao.equals(Requisicao.STATUS_FINALIZADA)) {
                        locationManager.removeUpdates(locationListener);
                    }else{
                        //Solicitar atualizações de localização
                        if (ActivityCompat.checkSelfPermission(PassageiroActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
                            locationManager.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER,
                                    10000,
                                    10,
                                    locationListener
                            );
                        }
                    }
                }
                dialog.dismiss();

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

            //Solicitar atualizações de localização
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        10000,
                        10,
                        locationListener
                );
            }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.menuSair :

                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(PassageiroActivity.this);
                builder.setTitle("Sair");
                builder.setMessage("Tem certeza que deseja sair?");

                builder.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        try {
                            autenticacao.signOut();
                            finish();

                        }catch (Exception  e){
                            e.printStackTrace();
                        }

                        finish();
                        abrirHome();
                    }
                });
                builder.setNegativeButton("Não", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {


                    }
                });

                android.app.AlertDialog dialog = builder.create();
                dialog.show();

               // autenticacao.signOut();
               // finish();
                break;

            case R.id.menuConfiguracoes :
                abrirConfiguracoes();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void inicializarComponentes(){

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Iniciar uma viagem");
        setSupportActionBar(toolbar);

        //Inicializar componentes
        editDestino = findViewById(R.id.editDestino);
        linearLayoutDestino = findViewById(R.id.linearLayoutDestino);
        buttonChamarUber = findViewById(R.id.buttonChamarUber);


        //Configurações iniciais
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        // verificar se o passageiro configurou seu perfil
        passageiro = UsuarioFirebase.getDadosUsuarioLogado();
        if(passageiro.getUrlImagem().equals("")){
            exibirMensagem("Configure seu perfil para melhor experiência!");
            abrirConfiguracoes();
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // adicionar evento de click no FabRota
        fabRota = findViewById(R.id.fabRota);
        fabRota.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String status = statusRequisicao;
                if(status != null && !status.isEmpty()){
                    String lat = "";
                    String lon = "";

                    switch ( status ){
                        case Requisicao.STATUS_A_CAMINHO :
                            lat = String.valueOf(localPassageiro.latitude);
                            lon = String.valueOf(localPassageiro.longitude);
                            break;

                        case Requisicao.STATUS_VIAGEM :
                            lat = destino.getLatitude();
                            lon = destino.getLongitude();
                            break;
                    }

                    // abrir rota
                    String latLong = lat +","+lon;

                    Uri uri= Uri.parse("google.navigation:q="+latLong+"&mode=d"); // d = dirigindo
                    Intent i = new Intent(Intent.ACTION_VIEW, uri);
                    i.setPackage("com.google.android.apps.maps");
                    startActivity(i);
                }

            }
        });

    }
    private void abrirConfiguracoes(){
        startActivity(new Intent(PassageiroActivity.this, ConfiguracoesPerfilctivity.class));
    }
    private void abrirHome(){
        startActivity(new Intent(PassageiroActivity.this, MainActivity.class));
    }

    private void exibirMensagem(String texto){
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show();
    }
}
