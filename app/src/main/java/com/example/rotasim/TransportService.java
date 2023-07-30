package com.example.rotasim;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TransportService {
    private String idServico;
    private Date inicio;
    private Date fim;
    private List<String> passageiros;
    private List<String> cargas;
    private List<String> motoristas;

    public TransportService(String idServico, Date inicio, Date fim) {
        this.idServico = idServico;
        this.inicio = inicio;
        this.fim = fim;
        this.passageiros = new ArrayList<>();
        this.cargas = new ArrayList<>();
        this.motoristas = new ArrayList<>();
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