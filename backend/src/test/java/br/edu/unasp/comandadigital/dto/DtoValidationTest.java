package br.edu.unasp.comandadigital.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import br.edu.unasp.comandadigital.dto.auth.CadastroClienteRequest;
import br.edu.unasp.comandadigital.dto.usuario.UsuarioResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DtoValidationTest {

    private ValidatorFactory validatorFactory;
    private Validator validator;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void rejectsInvalidCustomerRegistrationData() {
        CadastroClienteRequest request = new CadastroClienteRequest(
                " ",
                "email-invalido",
                "123",
                " ",
                " ");

        Set<String> invalidFields = validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());

        assertThat(invalidFields)
                .contains("nome", "email", "senha", "telefone", "endereco");
    }

    @Test
    void acceptsACompleteCustomerRegistration() {
        CadastroClienteRequest request = new CadastroClienteRequest(
                "Cliente Teste",
                "cliente@teste.com",
                "senha123",
                "(11) 99999-9999",
                "Rua de Teste, 123");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void neverExposesPasswordInTheUserResponseContract() {
        Set<String> responseFields = Arrays.stream(UsuarioResponse.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());

        assertThat(responseFields)
                .doesNotContain("senha", "senhaHash", "senha_hash");
    }
}
