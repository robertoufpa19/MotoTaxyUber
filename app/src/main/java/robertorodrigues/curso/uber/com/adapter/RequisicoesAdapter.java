package robertorodrigues.curso.uber.com.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import robertorodrigues.curso.uber.com.R;
import robertorodrigues.curso.uber.com.helper.Local;
import robertorodrigues.curso.uber.com.model.Requisicao;
import robertorodrigues.curso.uber.com.model.Usuario;

public class RequisicoesAdapter extends RecyclerView.Adapter<RequisicoesAdapter.MyViewHolder> {

    private List<Requisicao> listaRequisicao;
    private Context context;
    private Usuario motorista;

    public RequisicoesAdapter(List<Requisicao> listaRequisicao, Context context, Usuario motorista) {
        this.listaRequisicao = listaRequisicao;
        this.context = context;
        this.motorista = motorista;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View item = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_novas_requisicao, parent,false);

        return new MyViewHolder(item);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        Requisicao requisicao = listaRequisicao.get( position );
        Usuario passageiro = requisicao.getPassageiro();
        String fotoPassageiro = requisicao.getPassageiro().getUrlImagem();

        motorista = requisicao.getMotorista();

        holder.nome.setText( passageiro.getNome() );


      // falta configurar para aparecer a distancia do passageiro para o motorista
        if(motorista != null){

            LatLng localPassageiro = new LatLng(
                    Double.parseDouble(passageiro.getLatitude()),
                    Double.parseDouble(passageiro.getLongitude())
            );

          LatLng localMotorista = new LatLng(
                    Double.parseDouble(motorista.getLatitude()),
                    Double.parseDouble(motorista.getLongitude())
            );


           float distancia = Local.calcularDistancia(localPassageiro, localMotorista);
              String distanciaFormatada = Local.formatarDistancia(distancia);
                holder.distancia.setText(distanciaFormatada + "- aproximadamente");
        }

        if(fotoPassageiro != null){

            Uri uri = Uri.parse( fotoPassageiro );

            Picasso.get()
                    .load(uri)
                    .into(holder.foto);
        }else{

            holder.foto.setImageResource(R.drawable.perfil);

        }



      

    }

    @Override
    public int getItemCount() {
        return listaRequisicao.size();
    }

    public  class MyViewHolder extends RecyclerView.ViewHolder{

        TextView nome, distancia;
        CircleImageView foto;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            nome = itemView.findViewById(R.id.textNomePassageiro);
            distancia = itemView.findViewById(R.id.textDisntanciaPassageiro);
            foto = itemView.findViewById(R.id.imageViewFotoPassageiro);


        }
    }

}
