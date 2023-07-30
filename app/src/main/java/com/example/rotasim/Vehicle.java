package com.example.rotasim;

import static java.lang.Math.ceil;
import static java.lang.Math.round;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import android.location.Location;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Vehicle extends Thread {

    // Variáveis relacionadas ao consumo de combustível
    private final double consumptionRateAt80; // Taxa de consumo de combustível a 80 km/h
    private double currentFuelConsumptionRate; // Taxa de consumo de combustível atualizada

    // Variáveis relacionadas à velocidade
    private double optimalDrivingSpeed; // Velocidade de condução ideal
    private double reconciliationFluxSpeed; // Velocidade do fluxo para reconciliação

    // Variáveis relacionadas à distância
    private double totalDistanceTravelled = 0; // Distância total percorrida
    private double distanceInCurrentFlow = 0; // Distância percorrida no fluxo atual

    // Variáveis relacionadas ao tempo
    private long simulationStartTime = 0; // Hora de início da simulação
    private long simulationTime = 0; // Tempo de simulação
    private double secondsToNexMeasurement = 0.0; // Segundos para a próxima medição
    private double totalTravelTime = 0.0; // Tempo total de viagem

    // Variáveis relacionadas à reconciliação
    private int reconciliationIteration = 1; // Iteração de reconciliação
    private double reconciliationSamplingPoints = 0.0; // Ticks de reconciliação
    private double[][] speedIncidenceMatrix; // Matriz de incidência
    private ArrayList<Double> rawSpeedData = new ArrayList<>(); // Dados de velocidade bruta
    private ArrayList<Double> processedSpeedData = new ArrayList<>(); // Dados de velocidade processados
    private ArrayList<Double> speedStandardDeviation = new ArrayList<>(); // Desvio padrão dos dados de velocidade

    // Estado da simulação
    private boolean isSimulationRunning = false; // A simulação está rodando
    private LatLng endPoint;
    private double totalTravelDistance;
    private float currentSpeed;
    private Location currentLocation;
    private ArrayList<Double> pointsTimes = new ArrayList<>();
    private ArrayList<Double> timeStandardDeviation = new ArrayList<>();
    private double fluxTimeExpected;
    private int fluxDistance;
    private boolean firstRun = true;
    private double timeToArrive;
    UUID vehicleId = UUID.randomUUID();

    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);


    public Vehicle(double rate) {
        this.consumptionRateAt80 = rate;
        this.optimalDrivingSpeed = 80.0; // Velocidade ideal padrão
    }
    @Override
    public void run() {

            // Inicie a execução do método sendJSONData a cada 2 segundos
            executor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    if(currentLocation != null) {
                        // Seu código para enviar dados JSON aqui
                        try {
                            sendJSONData(convertToJson(timeToArrive));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }, 0, 2, TimeUnit.SECONDS);
        while (!Thread.currentThread().isInterrupted()) {
            double timeElapsed = (System.currentTimeMillis() - simulationStartTime) / (1000.0);

            //Calcula o tempo passado para checar quando o tempo for igual ao desejado para cada nó
            if (distanceInCurrentFlow >= fluxDistance) {
                reconciliateAndSend(timeElapsed);
            }
        }
    }

    private void reconciliateAndSend(double timeElapsed){
        double F1_novo = (fluxTimeExpected + (fluxTimeExpected - timeElapsed));
        calculateSpeed(distanceInCurrentFlow, timeElapsed / (60 * 60)); //Calcula a velocidade no último fluxo
        double[] reconciliatedTimes = new double[] {};
        if (pointsTimes.size() > 2) { //somente para evitar crashes no ultimo fluxo (não é possivel reconciliar um vetor com menos de dois valores
            updateArrayList(pointsTimes, F1_novo);
            timeStandardDeviation.remove(0);

            speedIncidenceMatrix = createIncidenceMatrix(pointsTimes.size()); // cria matrix de incidencia com o novo tamanho de medidas
            double[] y = convertArrayListToArray(pointsTimes); // define o array de medições (_rawMeasurements) como uma cópia do vetor de velocidades
            double[] v = convertArrayListToArray(timeStandardDeviation);
            double[][] A = speedIncidenceMatrix;

            Reconciliation rec = new Reconciliation();
            rec.reconcile(y, v, A);
            System.out.println("Tempos reconciliados: " + new java.util.Date());
            System.out.println("Fluxo: F" + (reconciliationIteration++));
            System.out.println("Raw Measurements:");
            rec.printMatrix(y);
            System.out.print("Interation: ");
            System.out.println(reconciliationIteration++);
            System.out.println("Reconciled flow:");
            rec.printMatrix(rec.getReconciledFlow());
            optimalDrivingSpeed = fluxDistance / secondsToHours(rec.getReconciledFlow()[0]); //Atualiza a velocidade otimizada para o prox fluxo de medição
            distanceInCurrentFlow = 0; // Reseta a distância percorrida no fluxo
            simulationStartTime = System.currentTimeMillis(); // Reseta a contagem de tempo
            pointsTimes = convertArrayToArrayList(y);
            reconciliatedTimes = convertArrayListToArray(pointsTimes);
        } else {
            reconciliatedTimes = convertArrayListToArray(pointsTimes);
            distanceInCurrentFlow = 0;
        }


        timeToArrive = getTimeToArrive(reconciliatedTimes);
        System.out.println("Distance Covered:" + totalDistanceTravelled + " Total Distance: " + totalTravelDistance + "Diference: " + (totalTravelDistance - totalDistanceTravelled));
        System.out.println("remaining time at reconciliated speed:" + timeToArrive);
        System.out.println("Total travel time: " + minutesToHours(totalTravelTime/60) + "h Travelled time:" + miliSecondsToHours(System.currentTimeMillis() - simulationTime) + "h");
        try {
            sendJSONData(convertToJson(timeToArrive));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int findLargestDivisor(int number) {
        int maxDivisor = 1; // Inicializa o maior divisor como 1
        for (int i = 2; i <= number / 2; i++) { // Começa de 2 e vai até a metade do número
            if (number % i == 0) { // Se o número é divisível por i, então i é um divisor
                maxDivisor = i; // Atualiza o maior divisor
            }
        }
        return maxDivisor;
    }

    public String convertToJson(double distanceToArive) {
        Gson gson = new Gson();

        // Cria um mapa para armazenar os dados
        Map<String, Object> data = new HashMap<>();

        // Cria um mapa para armazenar os dados de localização
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", currentLocation.getLatitude());
        locationData.put("longitude", currentLocation.getLongitude());
        locationData.put("altitude", currentLocation.getAltitude());

        // Cria um mapa para armazenar os dados de latitude longitude do destino
        Map<String, Object> endPointData = new HashMap<>();
        endPointData.put("latitude", endPoint.latitude);
        endPointData.put("longitude", endPoint.longitude);

        // Adicione outros atributos conforme necessário

        // Adicione os dados ao mapa
        data.put("distanceToArive", distanceToArive);
        data.put("location", locationData); // Adicione o mapa de dados de localização
        data.put("id", vehicleId); // Substitua id pela sua string de id
        data.put("endPoint", endPointData); // Substitua id pela sua string de id

        // Converte o mapa para JSON
        String json = gson.toJson(data);

        return json;
    }


    private void sendJSONData(String json) throws Exception {
        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        String encryptedJSON = CryptoUtils.encrypt(json);

        Gson gson = new Gson();
        Map<String, String> map = new HashMap<>();
        map.put("data", encryptedJSON);
        String jsonToSend = gson.toJson(map);

        RequestBody body = RequestBody.create(mediaType, jsonToSend);

        Request request = new Request.Builder()
                .url("http://10.0.2.2:5000/send") // Replace with the IP and port of the server
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                } else {
                    // Do something with the response.
                    System.out.println(response.body().string());
                }
            }
        });
    }


    private double secondsToHours(double seconds){
        return seconds/3600;
    }

    private double minutesToHours(double minutes) {
        return minutes/60;
    }

    private double miliSecondsToHours(double miliSeconds){
        return miliSeconds/3600000;
    }

    public double getTotalDistanceTravelled() {
        return totalDistanceTravelled;
    }

    // Getter e Setter para a taxa de consumo de combustível
    // Getter e Setter para a velocidade ideal

    public double getOptimalDrivingSpeed() {
        return optimalDrivingSpeed;
    }
    /**
     * Inicia a simulação do veículo.
     * O tempo de simulação é calculado com base no tempo real do sistema.
     */
    public void startSimulation() {
        if (isSimulationRunning) {
            return; // Se a thread já estiver em execução, não faz nada
        }
        isSimulationRunning = true;
        simulationStartTime = System.currentTimeMillis();
        simulationTime = simulationStartTime;
        start();
    }

    private double getTimeToArrive(double[] newTimes ){
        double totalTimeToArive = 0.0;
        for (double time : newTimes) {
            // A distância é velocidade vezes tempo.
            // Como a velocidade está em km/h e o tempo em horas, o resultado será em km.
            totalTimeToArive += time;
        }
        return totalTimeToArive;
    }

    /**
     * Interrompe a simulação do veículo.
     */
    public void stopSimulation() {
        this.interrupt();
    }


    public double getOverallAverageSpeed() {
        double secondsTravelled = (System.currentTimeMillis() - simulationTime)/1000.0;
        double hoursTravelled = secondsTravelled/3600;
        return totalDistanceTravelled/hoursTravelled;
    }

    /**
     * Obtém o tempo total formatado em horas, minutos e segundos.
     *
     * @return O tempo total formatado.
     */
    public String getTotalTimeFormatted() {
        long totalTimeTravelled = (System.currentTimeMillis() - simulationTime) / 1000;
        long hours = totalTimeTravelled / 3600;
        long minutes = (totalTimeTravelled % 3600) / 60;
        long secs = totalTimeTravelled % 60;

        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, secs);
    }

    /**
     * Atualiza a distância total percorrida.
     *
     * @param distanceCovered A distância percorrida.
     */
    public void updateTotalDistance(double distanceCovered) {
        distanceInCurrentFlow += distanceCovered;
        totalDistanceTravelled += distanceCovered;
    }

    public String getUpdatedRate() {
        return String.format(Locale.US, "%.2f", currentFuelConsumptionRate) + " km/L";
    }

    /**
     * Obtém o consumo de combustível do veículo.
     *
     * @return O consumo de combustível no formato "X.XX L".
     */
    public String getConsumption() {
        currentFuelConsumptionRate = calculateConsumptionRate(reconciliationFluxSpeed);
        return String.format(Locale.US, "%.2f", totalDistanceTravelled / calculateConsumptionRate(reconciliationFluxSpeed)) + " L";
    }

    /**
     * Calcula a taxa de consumo de combustível com base na velocidade.
     *
     * @param speed A velocidade do veículo.
     * @return A taxa de consumo de combustível.
     */
    public double calculateConsumptionRate(double speed) {
        double newRate = 0;
        if (speed <= 80) {
            newRate = consumptionRateAt80;
        } else if (speed <= 100) {
            newRate = 0.8 * consumptionRateAt80;
        } else if (speed <= 120) {
            newRate = 0.6 * consumptionRateAt80;
        } else if (speed > 120) {
            newRate = 0.5 * consumptionRateAt80;
        }

        if (newRate > consumptionRateAt80) {
            newRate = consumptionRateAt80;
        }

        return newRate;
    }

    /**
     * Calcula a velocidade ideal com base na distância percorrida e no tempo decorrido.
     *
     * @param distanceCovered A distância percorrida.
     * @param timeElapsed     O tempo decorrido.
     */
    public void calculateSpeed(double distanceCovered, double timeElapsed) {
        // Distância que deveria ter sido percorrida a 80 km/h em 0,75 minutos
        // Ajuste de tempo
        reconciliationFluxSpeed = distanceCovered / timeElapsed;
        System.out.println("calculated flux speed: " + reconciliationFluxSpeed);
    }

    public void addSpeed(float kmphSpeed) {
        rawSpeedData.add((double) kmphSpeed);
    }

    private void updateSpeedData(){
        System.out.println("average calculated speed from gps: " + SpeedStatistics.calculateAverageSpeed(rawSpeedData));
        speedStandardDeviation.remove(0);
        speedStandardDeviation.add(0,SpeedStatistics.calculateStandardDeviation(rawSpeedData));
        rawSpeedData.clear();
    }

    public boolean hasReachedDestination(Location currentLocation) {
        float[] results = new float[1];
        Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(), endPoint.latitude, endPoint.longitude, results);
        float distanceInMeters = results[0];
        return distanceInMeters < 50;
    }

    public void createReconciliationData(double totalDistance, double totalTime) {

        double numberOfFluxPoints = findLargestDivisor((int) ceil(totalDistance));
        fluxDistance = (int) (ceil(totalDistance)/(numberOfFluxPoints));
        fluxTimeExpected = fluxDistance/80.0;
        fluxTimeExpected = (fluxTimeExpected*3600);
        totalTravelDistance = totalDistance;
        totalTravelTime = totalTime;
        totalTravelTime = (ceil(totalDistance)/80)*3600;
        timeToArrive = totalTime;
        reconciliationSamplingPoints = numberOfFluxPoints;
        System.out.println("Fluxos: " + (int) reconciliationSamplingPoints + " Nós: " + ((int) reconciliationSamplingPoints -1));
        speedIncidenceMatrix = createIncidenceMatrix((int) reconciliationSamplingPoints);
        fillPointsTimesData((int) reconciliationSamplingPoints, fluxTimeExpected);
        fillSpeedData(processedSpeedData, (int) reconciliationSamplingPoints);
    }

    public static double[][] createIncidenceMatrix(int numNodes) {
        // Crie uma matriz com (numNodes - 1) linhas e numNodes colunas
        double[][] incidenceMatrix = new double[numNodes - 1][numNodes];

        // Preencha a matriz
        for (int i = 0; i < numNodes - 1; i++) {
            // Coloque -1 na coluna que representa o fluxo de entrada
            incidenceMatrix[i][i] = -1;

            // Coloque 1 na coluna que representa o fluxo de saída
            incidenceMatrix[i][i + 1] = 1;
        }

        return incidenceMatrix;
    }

    public void fillSpeedData(ArrayList<Double> speedData, int n) {
        for (int i = 0; i < n; i++) {
            speedData.add(80.0);
            speedStandardDeviation.add(0.5);
        }
    }

    public void fillPointsTimesData(int n, double value) {
        for (int i = 0; i < n; i++) {
            pointsTimes.add(value);
            timeStandardDeviation.add(0.000000000001);
        }
    }

    @NonNull
    public static double[] convertArrayListToArray(ArrayList<Double> list) {
        double[] array = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    public static void updateArrayList(ArrayList<Double> list, double newValue) {
        if (list.size() > 1) {
            list.remove(0);
            list.set(0, newValue);
        }
    }

    public static ArrayList<Double> convertArrayToArrayList(double[] array) {
        ArrayList<Double> arrayList = new ArrayList<>();
        for (double num : array) {
            arrayList.add(num);
        }
        return arrayList;
    }

    public void setEndPoint(LatLng endLatLng) {
        endPoint = endLatLng;
    }

    public void setLocationAndSpeed(float kmphSpeed, Location location) {
        currentSpeed = kmphSpeed;
        currentLocation = location;
    }

    public CrossDockingVehicle getVehicleCrossDocking() {
        LatLng currentLocationLatLang = new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
        CrossDockingVehicle crossDockingVehicle = new CrossDockingVehicle(this.vehicleId.toString(), this.totalTravelDistance, currentLocationLatLang, this.endPoint);
        return crossDockingVehicle;
    }
}
