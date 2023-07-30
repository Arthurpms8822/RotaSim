package com.example.rotasim;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

// A classe CryptoUtils contém métodos para criptografar e descriptografar dados
public class CryptoUtils {

    // Constante que representa o algoritmo de criptografia que será usado, neste caso, é AES
    private static final String ALGORITHM = "AES";
    // Constante que representa a chave secreta que será usada para criptografia e descriptografia
    private static final byte[] KEY = "CRIPTOGRAFIAAV02".getBytes(); // Substitua por sua própria chave

    // Método para criptografar dados. Ele usa o algoritmo AES.
    public static String encrypt(String data) throws Exception {
        // Cria um objeto Cipher para criptografia, configurado com o algoritmo AES
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        // Cria uma chave secreta com a chave especificada e o algoritmo AES
        SecretKeySpec secretKeySpec = new SecretKeySpec(KEY, ALGORITHM);
        // Inicializa o objeto Cipher para criptografia com a chave secreta
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        // Criptografa os dados, que são convertidos em bytes
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        // Converte os bytes criptografados para uma string codificada em Base64 e retorna
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    // Método para descriptografar dados que foram criptografados com o mesmo algoritmo e chave
    public static String decrypt(String encryptedData) throws Exception {
        // Decodifica a string codificada em Base64 para bytes
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
        // Cria um objeto Cipher para descriptografia, configurado com o algoritmo AES
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        // Cria uma chave secreta com a chave especificada e o algoritmo AES
        SecretKeySpec secretKeySpec = new SecretKeySpec(KEY, ALGORITHM);
        // Inicializa o objeto Cipher para descriptografia com a chave secreta
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        // Descriptografa os bytes, que são convertidos de volta para a string original
        byte[] originalBytes = cipher.doFinal(decodedBytes);
        // Converte os bytes descriptografados de volta para uma string e retorna
        return new String(originalBytes);
    }
}
