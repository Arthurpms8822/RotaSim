package com.example.rotasim;

import java.util.Locale;
import java.util.logging.Logger;

public class Vehicle extends Thread {

    private final double rateAt80; // Taxa de consumo de combustível em km/l a 80 km/h

    public String getUpdatedRate() {
        return String.format(Locale.US, "%.2f", updatedRate) + " km/L";
    }

    private double updatedRate; // Taxa de consumo de combustível atualizada em km/l
    private double optimalSpeed; // Velocidade ideal em km/h

    public double getAverageSpeed() {
        return averageSpeed;
    }

    private double averageSpeed;

    private double totalDistanceCovered = 0; // Distância total percorrida desde o início do rastreamento

    public double getTotalDistance() {
        return totalDistance;
    }

    private double totalDistance = 0;
    private long startTime = 0; // Horário de início do rastreamento
    private long simTime = 0;
    private boolean isRunning = false;

    private static final Logger logger = Logger.getLogger(Vehicle.class.getName());

    public Vehicle(double rate) {
        this.rateAt80 = rate;
        this.optimalSpeed = 80.0; // Velocidade ideal padrão
    }

    // Getter e Setter para a taxa de consumo de combustível

    // Getter e Setter para a velocidade ideal
    public double getOptimalSpeed() {
        return optimalSpeed;
    }

    /**
     * Inicia a simulação do veículo.
     * O tempo de simulação é calculado com base no tempo real do sistema.
     */
    public void startSimulation() {
        if (isRunning) {
            return; // Se a thread já estiver em execução, não faz nada
        }
        isRunning = true;
        startTime = System.currentTimeMillis();
        simTime = startTime;
        start();
    }

    /**
     * Interrompe a simulação do veículo.
     */
    public void stopSimulation() {
        this.interrupt();
    }

    /**
     * Obtém o tempo total formatado em horas, minutos e segundos.
     *
     * @return O tempo total formatado.
     */
    public String getTotalTimeFormatted() {
        long totalTimeTravelled = (System.currentTimeMillis() - simTime) / 1000;
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
        totalDistanceCovered += distanceCovered;
        totalDistance += distanceCovered;
    }

    /**
     * Obtém o consumo de combustível do veículo.
     *
     * @return O consumo de combustível no formato "X.XX L".
     */
    public String getConsumption() {
        updatedRate = calculateConsumptionRate(averageSpeed);
        return String.format(Locale.US, "%.2f", totalDistance / calculateConsumptionRate(averageSpeed)) + " L";
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
            newRate = rateAt80;
        } else if (speed <= 100) {
            newRate = 0.8 * rateAt80;
        } else if (speed <= 120) {
            newRate = 0.6 * rateAt80;
        } else if (speed > 120) {
            newRate = 0.5 * rateAt80;
        }

        if (newRate > rateAt80) {
            newRate = rateAt80;
        }

        return newRate;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            double timeElapsed = (System.currentTimeMillis() - startTime) / (1000.0);
            if (timeElapsed >= 45) { // Verifica se a distância percorrida é de 1 km ou mais
                calculateOptimalSpeed(totalDistanceCovered, timeElapsed / (60 * 60));
                totalDistanceCovered = 0; // Redefine a distância total percorrida
                startTime = System.currentTimeMillis();
            }
        }
    }

    /**
     * Calcula a velocidade ideal com base na distância percorrida e no tempo decorrido.
     *
     * @param distanceCovered A distância percorrida.
     * @param timeElapsed     O tempo decorrido.
     */
    public void calculateOptimalSpeed(double distanceCovered, double timeElapsed) {
        // Distância que deveria ter sido percorrida a 80 km/h em 0,75 minutos
        // Ajuste de tempo
        double subTrackSpeed = distanceCovered / timeElapsed;
        averageSpeed = subTrackSpeed;
        optimalSpeed = 80 * 80 / subTrackSpeed;
        if (optimalSpeed > 120) {
            optimalSpeed = 120;
        }

        logger.info("Velocidade ideal atualizada: " + optimalSpeed + " km/h às " + new java.util.Date());
    }
}
