package com.example.rotasim;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class TransportService {
    private String idServico;
    private Date inicio;
    private Date fim;
    private List<String> passageiros;
    private List<String> cargas;
    private List<String> motoristas;

    public TransportService() {
        this.passageiros = new ArrayList<>();
        this.cargas = new ArrayList<>();
        this.motoristas = new ArrayList<>();
    }

    public void createTransportService(String passageiro, String motorista1, String motorista2){
        idServico = UUID.randomUUID().toString();
        passageiros.add(passageiro);
        inicio = new Date();
        motoristas.add(motorista1);
        motoristas.add(motorista2);
        printTransportServiceDetails();
    }

    public void printTransportServiceDetails() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        System.out.println("Detalhes do Serviço de Transporte:");
        System.out.println("ID do Serviço: " + this.idServico);
        System.out.println("Data de Início: " + sdf.format(this.inicio));
        System.out.println("Data de Fim: " + (fim != null ? sdf.format(this.fim) : "N/A"));

        System.out.println("\nPassageiros:");
        for (String passageiro : this.passageiros) {
            System.out.println(passageiro);
        }

        System.out.println("\nCargas:");
        for (String carga : this.cargas) {
            System.out.println(carga);
        }

        System.out.println("\nMotoristas:");
        for (String motorista : this.motoristas) {
            System.out.println(motorista);
        }
    }

    public void adicionarPassageiro(String passageiro) {
        this.passageiros.add(passageiro);
    }

    public void adicionarCarga(String carga) {
        this.cargas.add(carga);
    }

    public void adicionarMotorista(String motorista) {
        this.motoristas.add(motorista);
    }

    // getters and setters
}