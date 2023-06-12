package com.example.rotasim;

import java.util.Locale;

public class ElectricCar extends Vehicle {

    private final double energyConsumptionRate; // Taxa de consumo de energia em kWh por 100 km

    public ElectricCar(double energyConsumptionRate) {
        super(energyConsumptionRate);
        this.energyConsumptionRate = energyConsumptionRate;
    }

    /**
     * Calcula o consumo de energia para um carro elétrico.
     * O consumo de energia para um carro elétrico é geralmente mais constante, então esse método é sobrescrito.
     *
     * @return consumo de energia em kWh
     */

    public double calculateConsumption() {
        return energyConsumptionRate * this.getTotalDistance() / 100; // consumo de energia constante
    }

    /**
     * Obtém a taxa de consumo de energia atualizada do carro elétrico.
     *
     * @return taxa de consumo de energia formatada
     */
    @Override
    public String getUpdatedRate() {
        return String.format(Locale.US, "%.2f", energyConsumptionRate) + " kWh/100km";
    }

    /**
     * Obtém o consumo de energia do carro elétrico.
     *
     * @return consumo de energia formatado
     */
    @Override
    public String getConsumption() {
        return String.format(Locale.US, "%.2f", calculateConsumption()) + " kWh";
    }

}
