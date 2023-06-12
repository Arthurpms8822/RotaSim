package com.example.rotasim;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class RouteDrawer {
    private final GoogleMap mMap;
    private final String apiKey;
    private String totalDistance;
    private String totalTime;
    private final MapsActivity.RouteCallback routeCallback;

    /**
     * Cria um novo RouteDrawer para desenhar rotas no mapa.
     *
     * @param mMap            objeto GoogleMap para desenhar as rotas
     * @param apiKey          chave de API do Google Maps
     * @param routeCallback   callback para notificar quando a rota for desenhada
     */
    public RouteDrawer(GoogleMap mMap, String apiKey, MapsActivity.RouteCallback routeCallback) {
        this.mMap = mMap;
        this.apiKey = apiKey;
        this.routeCallback = routeCallback;
    }

    /**
     * Desenha uma rota no mapa entre dois pontos com uma velocidade desejada.
     *
     * @param start            ponto de partida
     * @param end              ponto de chegada
     */
    public void drawRoute(LatLng start, LatLng end) {
        String url = getDirectionsUrl(start, end);

        new DownloadTask().execute(url);
    }

    /**
     * Obtém a URL da API Directions do Google com as coordenadas de origem e destino.
     *
     * @param origin  coordenadas de origem
     * @param dest    coordenadas de destino
     * @return URL da API Directions do Google
     */
    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        String sensor = "sensor=false";
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&key=" + apiKey;
        return "https://maps.googleapis.com/maps/api/directions/json?" + parameters;
    }

    /**
     * Converte a distância total da rota em quilômetros.
     *
     * @return distância total em quilômetros
     */
    public double getTotalDistanceInKm() {
        double distanceValue = 0.0;
        if (totalDistance.contains("km")) {
            distanceValue = Double.parseDouble(totalDistance.replace(" km", ""));
        } else if (totalDistance.contains("m")) {
            distanceValue = Double.parseDouble(totalDistance.replace(" m", "")) / 1000;
        }
        return distanceValue;
    }

    @SuppressLint("StaticFieldLeak")
    private class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            new ParserTask().execute(result);
        }
    }

    /**
     * Faz o download dos dados JSON da rota a partir da URL fornecida.
     *
     * @param strUrl URL da rota
     * @return dados JSON da rota
     * @throws IOException se ocorrer um erro durante o download
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();
            br.close();
        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            assert iStream != null;
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    @SuppressLint("StaticFieldLeak")
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();
                routes = parser.parse(jObject);
                totalDistance = parser.getTotalDistance();
                double totalDistanceDouble = getTotalDistanceInKm();
                double totalTimeValue = 60 * totalDistanceDouble / 80;
                totalTime = totalTimeValue + " min";
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;
            new MarkerOptions();
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();
                List<HashMap<String, String>> path = result.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);
                    double lat = Double.parseDouble(Objects.requireNonNull(point.get("lat")));
                    double lng = Double.parseDouble(Objects.requireNonNull(point.get("lng")));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(Color.BLUE);
            }

            if (lineOptions != null) {
                mMap.addPolyline(lineOptions);
            }
            // Notificar o callback
            if (routeCallback != null) {
                routeCallback.onRouteDrawn(totalDistance, totalTime);
            }
        }
    }
}
