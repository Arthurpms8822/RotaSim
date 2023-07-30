package com.example.rotasim;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.rotasim.databinding.ActivityMapsBinding;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.switchmaterial.SwitchMaterial;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    // Definindo variáveis membro que serão utilizadas na classe
    private GoogleMap mMap; // Objeto que representa o mapa
    private LatLng startPosition; // Representa a posição de partida
    private LatLng endPosition; // Representa a posição de chegada
    private PlacesClient placesClient; // Cliente para a API de lugares do Google

    // Elementos da interface gráfica que serão atualizados durante a execução da aplicação
    private TextView totalDistanceView;
    private TextView totalTimeView;
    private TextView currentSpeed;
    private LocationThread locationThread;
    private TextView currentDistance;
    private TextView currentTime;
    private TextView optimalSpeed;
    private TextView fuelConsumption;
    private TextView efficiency;
    private TextView averageSpeed;

    private Vehicle vehicle;

    // Indica se o veículo é elétrico ou não
    private Boolean isElectric = false;

    private final int FETCH_INTERVAL = 1000; // Fetch data every 5 seconds
    private Handler mHandler;
    private RouteDrawer crossRoute;
    private Marker crossVehicleMarker = null;

    // Método chamado quando a atividade é criada
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflando o layout a partir do XML
        com.example.rotasim.databinding.ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Encontrando os elementos da interface gráfica a partir de seus IDs
        AutoCompleteTextView startLocation = findViewById(R.id.start_location);
        AutoCompleteTextView endLocation = findViewById(R.id.end_location);
        Button buttonGenerateRoute = findViewById(R.id.button_generate_route);
        Button buttonStartSimulation = findViewById(R.id.button_start_simulation);
        Button buttonStopSimulation = findViewById(R.id.button_stop_simulation);
        buttonStartSimulation.setEnabled(false);
        buttonStopSimulation.setEnabled(false);
        totalDistanceView = findViewById(R.id.total_distance);
        totalTimeView = findViewById(R.id.total_time);
        currentSpeed = findViewById(R.id.current_speed);
        optimalSpeed = findViewById(R.id.optmal_speed);
        currentDistance = findViewById(R.id.current_distance);
        currentTime = findViewById(R.id.current_time);
        fuelConsumption = findViewById(R.id.fuel_consumption);
        efficiency = findViewById(R.id.eficiency);
        averageSpeed = findViewById(R.id.average_speed);

        mHandler = new Handler();
        startRepeatingTask();

        // Definindo o comportamento do botão de geração de rota
        buttonGenerateRoute.setOnClickListener(v -> {
           vehicle = createVehicle(isElectric);
            // O código abaixo obtém os nomes dos lugares de partida e chegada, e em seguida obtém seus respectivos IDs e coordenadas
            // Os IDs e coordenadas são então usados para desenhar a rota no mapa
            String startLocationName = startLocation.getText().toString();
            String endLocationName = endLocation.getText().toString();
            buttonStartSimulation.setEnabled(true);
            // Início do processo de obtenção das coordenadas dos locais de partida e chegada
            getPlaceIdFromPrediction(startLocationName, startPlaceId -> getLatLngFromPlaceId(startPlaceId, startLatLng -> {
                startPosition = startLatLng;
                getPlaceIdFromPrediction(endLocationName, endPlaceId -> getLatLngFromPlaceId(endPlaceId, endLatLng -> {
                    endPosition = endLatLng;
                    // Geração da rota
                    if(mMap != null && startPosition != null && endPosition != null) {
                        mMap.clear();
                        RouteDrawer routeDrawer = new RouteDrawer(mMap, "AIzaSyB48taeeYRzcr5yAIXT518ODCvDY141HPc", (totalDistance, totalTime, totalDistanceDouble, totalTimeDouble) -> {
                            vehicle.createReconciliationData(totalDistanceDouble, totalTimeDouble);
                            vehicle.setEndPoint(endLatLng);
                            // Mostra a distância total e o tempo total nos TextViews
                            runOnUiThread(() -> {
                                String totalDistanceStr = totalDistance + " ";
                                totalDistanceView.setText(totalDistanceStr);
                                totalTimeView.setText(totalTime);
                            });
                        });
                        routeDrawer.drawRoute(startPosition, endPosition);

                        // Adicione um marcador na posição final
                        mMap.addMarker(new MarkerOptions().position(endPosition).title("End Position"));

                        // Mova a câmera para a posição inicial e ajuste o zoom para caber a rota
                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                        builder.include(startPosition);
                        builder.include(endPosition);
                        LatLngBounds bounds = builder.build();
                        int padding = 100; // offset das bordas do mapa em pixels
                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                        mMap.setOnMapLoadedCallback(() -> mMap.animateCamera(cameraUpdate));
                    }
                }));
            }));
        });

        // Switch para selecionar criação de carro elétrico
        SwitchMaterial switchVehicleType = findViewById(R.id.switch_vehicle_type);
        switchVehicleType.setOnCheckedChangeListener((buttonView, isChecked) -> isElectric = isChecked);


        buttonStartSimulation.setOnClickListener(v->{

            // Habilita o botão de parar simulação
            buttonStopSimulation.setEnabled(true);
            buttonStartSimulation.setEnabled(false);
            // Verifica se a aplicação possui permissão para acessar a localização do dispositivo.
            // Caso não tenha, solicita a permissão ao usuário
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                // Inicializa a thread de localização que é responsável por atualizar a localização do veículo
                locationThread = new LocationThread(this, vehicle , (speed, newLocation) -> runOnUiThread(() -> {
                    // Atualiza a interface do usuário com os dados da simulação
                    String currentSpeedStr = speed + " km/h ";
                    currentSpeed.setText(currentSpeedStr);
                    String currentDistanceStr = String.format(Locale.US, "%.1f", vehicle.getTotalDistanceTravelled()) + " km ";
                    currentDistance.setText(currentDistanceStr);
                    String optimalSpeedStr = String.format(Locale.US, "%.2f",vehicle.getOptimalDrivingSpeed())+ "km/h ";
                    optimalSpeed.setText(optimalSpeedStr);
                    String currentTimeStr = vehicle.getTotalTimeFormatted();
                    currentTime.setText(currentTimeStr);
                    String fuelConsumptionStr = vehicle.getConsumption();
                    fuelConsumption.setText(fuelConsumptionStr);
                    String efficiencyStr = vehicle.getUpdatedRate();
                    efficiency.setText(efficiencyStr);
                    String averageSpeedStr = String.format(Locale.US, "%.2f",vehicle.getOverallAverageSpeed()) + " km/h";
                    averageSpeed.setText(averageSpeedStr);

                    // Atualiza a posição do marcador no mapa
                    if (mMap != null && crossVehicleMarker == null) {
                        LatLng newLatLng = new LatLng(newLocation.getLatitude(), newLocation.getLongitude());
                        float zoomLevel = 16.0f; // Nível de zoom desejado. Este valor pode ser ajustado conforme necessário.
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newLatLng, zoomLevel));
                    }
                }));



                // Inicia a thread de localização e a simulação
                locationThread.start();
                locationThread.startTracking();
            }
        });

        buttonStopSimulation.setOnClickListener(v -> {
            // Se a thread de localização estiver ativa, pare e anule-a
            if (locationThread != null) {
                locationThread.stopTracking();
                locationThread = null;
            }

            // Desabilite o botão de parar simulação
            buttonStopSimulation.setEnabled(false);

            currentSpeed.setText("");
            optimalSpeed.setText("");
            currentDistance.setText("");
            currentTime.setText("");
            fuelConsumption.setText("");
            efficiency.setText("");
            startLocation.setText("");
            endLocation.setText("");
            averageSpeed.setText("");
            totalDistanceView.setText("");
            totalTimeView.setText("");
            mMap.clear();
        });

        // Inicialize a biblioteca Places com a chave da API
        Places.initialize(getApplicationContext(), "AIzaSyB48taeeYRzcr5yAIXT518ODCvDY141HPc");

        // Crie um cliente Places para enviar solicitações à API
        placesClient = Places.createClient(this);

        startLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Não é necessário fazer nada aqui
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Atualize o adaptador com as novas sugestões
                getPlaceSuggestions(s.toString(), suggestions -> startLocation.setAdapter(new ArrayAdapter<>(MapsActivity.this, android.R.layout.simple_dropdown_item_1line, suggestions)));
            }

            @Override
            public void afterTextChanged(Editable s) {
                getPlaceSuggestions(s.toString(), suggestions -> startLocation.setAdapter(new ArrayAdapter<>(MapsActivity.this, android.R.layout.simple_dropdown_item_1line, suggestions)));
            }
        });

        endLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Não é necessário fazer nada aqui
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Atualize o adaptador com as novas sugestões
                getPlaceSuggestions(s.toString(), suggestions -> endLocation.setAdapter(new ArrayAdapter<>(MapsActivity.this, android.R.layout.simple_dropdown_item_1line, suggestions)));
            }

            @Override
            public void afterTextChanged(Editable s) {
                getPlaceSuggestions(s.toString(), suggestions -> startLocation.setAdapter(new ArrayAdapter<>(MapsActivity.this, android.R.layout.simple_dropdown_item_1line, suggestions)));
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    private void getLatLngFromPlaceId(String placeId, OnLatLngReadyCallback onLatLngReadyCallback) {
        List<Place.Field> placeFields = Collections.singletonList(Place.Field.LAT_LNG);

        // Criando uma requisição para buscar informações do local a partir do ID do local
        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, placeFields);

        // Realizando a requisição de forma assíncrona
        placesClient.fetchPlace(request).addOnSuccessListener((response) -> {
            // No caso de sucesso, pegamos o local da resposta
            Place place = response.getPlace();

            // E chamamos o callback passando as coordenadas do local
            onLatLngReadyCallback.onLatLngReady(place.getLatLng());
        }).addOnFailureListener((exception) -> {
            // Em caso de falha, verificamos se a exceção é uma ApiException
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                // Se for, logamos o erro com o código de status
                Log.e(TAG, "Local não encontrado: " + apiException.getStatusCode());
            }
        });
    }

    private Vehicle createVehicle(boolean isElectric) {
        if (isElectric) {
            // Se o veículo for elétrico, criamos uma instância de ElectricCar
            return new ElectricCar(15.0);
        } else {
            // Se não for elétrico, criamos uma instância de Vehicle
            return new Vehicle(15.0);
        }
    }

    private void getPlaceIdFromPrediction(String placeName, OnPlaceIdReadyCallback onPlaceIdReadyCallback) {
        // Construímos uma requisição para buscar previsões de autocomplete
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(placeName)  // Definimos a query como o nome do local fornecido
                .build();

        // Fazemos a requisição ao cliente de lugares
        placesClient.findAutocompletePredictions(request).addOnSuccessListener((response) -> {
            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                // Se a previsão corresponder ao nome do local, recuperamos o ID do local e chamamos o callback
                if(prediction.getFullText(null).toString().equals(placeName)){
                    onPlaceIdReadyCallback.onPlaceIdReady(prediction.getPlaceId());
                    break;
                }
            }
        }).addOnFailureListener((exception) -> {
            // Em caso de falha, registramos o erro
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                Log.e(TAG, "Local não encontrado: " + apiException.getStatusCode());
            }
        });
    }

    public interface OnLatLngReadyCallback {
        void onLatLngReady(LatLng latLng);
    }

    public interface OnPlaceIdReadyCallback {
        void onPlaceIdReady(String placeId);
    }

    private void getPlaceSuggestions(String query, PlaceSuggestionsCallback callback) {
        // Cria uma instância de RectangularBounds
        RectangularBounds bounds = RectangularBounds.newInstance(
                new LatLng(-33.880490, 151.184363),
                new LatLng(-33.858754, 151.229596));

        // Usa a API Places para obter as previsões de sugestões de lugares
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setLocationBias(bounds)
                .setQuery(query)
                .build();

        // Faz uma chamada assíncrona para obter as sugestões de lugares
        placesClient.findAutocompletePredictions(request).addOnSuccessListener((response) -> {
            List<String> suggestions = new ArrayList<>();
            for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                suggestions.add(prediction.getFullText(null).toString());
            }
            // Chama o callback informando as sugestões de lugares encontradas
            callback.onSuggestionsReady(suggestions);
        }).addOnFailureListener((exception) -> {
            if (exception instanceof ApiException) {
                ApiException apiException = (ApiException) exception;
                Log.e(TAG, "Local não encontrado: " + apiException.getStatusCode());
            }
        });
    }

    /**
     * Manipula o mapa assim que estiver disponível.
     * Esse retorno de chamada é acionado quando o mapa estiver pronto para ser usado.
     * Aqui podemos adicionar marcadores ou linhas, adicionar ouvintes ou mover a câmera.
     * Neste caso, apenas adicionamos um marcador perto de Sydney, Austrália.
     * Se o Google Play services não estiver instalado no dispositivo, o usuário será solicitado a instalá-lo
     * dentro do SupportMapFragment. Este método só será acionado quando o usuário tiver
     * instalado o Google Play services e voltou para o aplicativo.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Verifica se a permissão de localização foi concedida
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permissão não concedida, solicite ao usuário
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        // Habilita a camada de localização do mapa
        mMap.setMyLocationEnabled(true);

        // Obtém a última localização conhecida do dispositivo
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                // Obtém as coordenadas da localização
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                // Move a câmera para a localização do usuário
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.0f));
            }
        });
    }

    public interface PlaceSuggestionsCallback {
        void onSuggestionsReady(List<String> suggestions);
    }

    public interface RouteCallback {
        void onRouteDrawn(String totalDistance, String totalTime, double totalDistanceDouble, double totalTimeDouble) throws Exception;
    }

    private void fetchJSON() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/receive")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    final String myResponse = response.body().string();
                    JSONObject jsonObject;
                    if(myResponse.isEmpty()){
                        return;
                    }

                    try {
                        jsonObject = new JSONObject(myResponse);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    // Extract the encrypted string from the JSON object
                    String encryptedData;
                    if (!jsonObject.has("data")) {
                        // The JSON object does not contain the "data" key, so there is nothing to decrypt
                        return;
                    }
                    try {
                        encryptedData = jsonObject.getString("data");
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    // Decrypt the encrypted string
                    String decryptedData;
                    try {
                        decryptedData = CryptoUtils.decrypt(encryptedData);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    JSONObject decryptedJson;
                    try {
                        decryptedJson = new JSONObject(decryptedData);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                    String vehicleId;
                    double distanceToArrive;
                    double latitude;
                    double longitude;
                    double endPointLat;
                    double endPointLong;
                    try {
                        vehicleId = decryptedJson.getString("id");
                        distanceToArrive = decryptedJson.getDouble("distanceToArive");
                        JSONObject location = decryptedJson.getJSONObject("location");
                        JSONObject endPoint = decryptedJson.getJSONObject("endPoint");
                        endPointLat = endPoint.getDouble("latitude");
                        endPointLong = endPoint.getDouble("longitude");
                        latitude = location.getDouble("latitude");
                        longitude = location.getDouble("longitude");
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    if (vehicle.vehicleId.toString() == vehicleId){
                        return;
                    }

                    LatLng vehiclePosition = new LatLng(latitude, longitude);
                    LatLng endPointPosition = new LatLng(endPointLat,endPointLong);
                    if (mMap!=null) {
                        crossRoute = new RouteDrawer(mMap, "AIzaSyB48taeeYRzcr5yAIXT518ODCvDY141HPc", new RouteCallback() {
                            @Override
                            public void onRouteDrawn(String totalDistance, String totalTime, double totalDistanceDouble, double totalTimeDouble) throws Exception {
                                runOnUiThread(() -> {
                                });
                            }
                        });
                        crossRoute.drawRoute(vehiclePosition, endPointPosition);
                    }


                    CrossDocking crossDocking = new CrossDocking(vehicle.getVehicleCrossDocking(), new CrossDockingVehicle(vehicleId,distanceToArrive,vehiclePosition,endPointPosition));
                    crossDocking.sendTimeDifference();

                    MapsActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Marker marker = crossVehicleMarker;
                            if (marker == null) {
                                // This is a new vehicle, so add a new marker for it
                                marker = mMap.addMarker(new MarkerOptions().position(vehiclePosition).title(vehicleId));
                                crossVehicleMarker = marker;
                            } else {
                                // This is an existing vehicle, so update its marker
                                marker.setPosition(vehiclePosition);
                            }
                        }
                    });
                }
            }
        });
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                fetchJSON(); // This function can change value of mInterval.
            } finally {
                mHandler.postDelayed(mStatusChecker, FETCH_INTERVAL);
            }
        }
    };

    void startRepeatingTask() {
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRepeatingTask();
    }
}