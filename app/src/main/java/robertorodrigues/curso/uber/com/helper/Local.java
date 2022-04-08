package robertorodrigues.curso.uber.com.helper;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;

public class Local {

     public  static  float calcularDistancia(LatLng latLngInicial, LatLng latLngFinal){

         Location localInicial = new Location("Local Inicial");
                  localInicial.setLatitude(latLngInicial.latitude);
                  localInicial.setLongitude(latLngInicial.longitude);

         Location localFinal = new Location("Local Final");
                  localFinal.setLatitude(latLngFinal.latitude);
                  localFinal.setLongitude(latLngFinal.longitude);

                  // calcular distancia - Resultado em metros
                  // Dividir por 1000 para converter em KM
         float distancia = localInicial.distanceTo(localFinal)/1000;


         return distancia;


     }

     public  static  String formatarDistancia(float distancia){
           String distanciaFormatada;

            if(distancia < 1){ // distancia em metros
               distancia = distancia*1000; // converte em metros

                distanciaFormatada = Math.round(distancia)+ "M";

            }else{ // distancia em Km
                DecimalFormat decimal = new DecimalFormat("0.0");
                distanciaFormatada = decimal.format(distancia) + "KM";

            }

            return distanciaFormatada;

     }
}
