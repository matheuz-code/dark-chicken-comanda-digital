package br.edu.unasp.comandadigital.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CnpjValidator implements ConstraintValidator<Cnpj, String> {

    private static final int[] PESOS_PRIMEIRO = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_SEGUNDO = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true;
        String digits = value.replaceAll("\\D", "");
        if (digits.length() != 14 || digits.chars().distinct().count() == 1) return false;
        return calcularDigito(digits.substring(0, 12), PESOS_PRIMEIRO) == digits.charAt(12) - '0'
                && calcularDigito(digits.substring(0, 13), PESOS_SEGUNDO) == digits.charAt(13) - '0';
    }

    private int calcularDigito(String digits, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (digits.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
