package br.com.fiap.soat7.data.domain.dto;


import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record CarSellRequest(
        @NotBlank
        @Pattern(regexp = "\\d{11}", message = "CPF deve ter 11 dígitos (apenas números).")
        String buyerCpf,

        @NotNull
        LocalDateTime soldAt
) {}
