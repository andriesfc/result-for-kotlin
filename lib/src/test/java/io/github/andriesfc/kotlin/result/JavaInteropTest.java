package io.github.andriesfc.kotlin.result;

import io.github.andriesfc.kotlin.result.demo.taxcalc.TaxCalculationService;
import io.github.andriesfc.kotlin.result.demo.taxcalc.TaxCalculationService.CalculationError;
import io.github.andriesfc.kotlin.result.demo.taxcalc.TaxCalculationService.CalculationError.Indicator;
import io.github.andriesfc.kotlin.result.demo.taxcalc.TaxCalculationService.TaxCalculation;
import io.github.andriesfc.kotlin.result.demo.taxcalc.TaxCalculationService.TaxableEntity;
import kotlin.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static io.github.andriesfc.kotlin.result.ResultOperations.*;
import static java.util.Collections.emptySet;
import static kotlin.collections.MapsKt.mapOf;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class JavaInteropTest {

    @Mock
    private TaxCalculationService taxCalculationService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void handlingErrorWithTraditionalTryCatch() {

        //noinspection unchecked
        var error = new CalculationError("taxRef", "badError",mapOf(
                new Pair<>(Indicator.AmountNotInRange, "30 is not in range"),
                new Pair<>(Indicator.TechnicalError, "Try again")));

        when(taxCalculationService.calculateTax(any(TaxableEntity.class), any(), any())).thenReturn(failure(error));

        Result<CalculationError, TaxCalculation> r = taxCalculationService.calculateTax(
                new TaxableEntity("", ""), emptySet(), emptySet());

        System.out.println(r);

        onFailure(r, e -> {});
        onSuccess(r, v -> {});

        try {
            System.out.println(r.get());
        } catch (Exception e) {
            var re = errorOrNull(r);
            System.out.println(re);
            assert re != null;
            System.out.println(re.hashCode());
        }

    }

    @Test
    void testIdiomaticUseOnExpectedFailure() {
        final String expectedError = "error";
        final Result<String, Integer> result = result(String.class, () -> failure(expectedError));
        assertThrows(WrappedFailureAsException.class, result::get);
        assertEquals(expectedError, errorOrNull(result));
    }

    @Test
    void testIdiomaticUseOnExpectedOnSuccess() {
        final int expectedCount = 10;
        final Result<String,Integer> result = result(String.class, () -> success(expectedCount));
        assertDoesNotThrow(result::get);
        assertEquals(expectedCount, result.get());
    }

    @Test
    void testIdiomaticUseOnExpectedFailureAsOptional() {
        final String expectedError = "bean_counter_offline";
        final Result<String,Integer> result = result(String.class, () -> failure(expectedError));
        final Optional<String> error = errorOrEmpty(result);
        assertThrows(WrappedFailureAsException.class, result::get);
        assertTrue(error.isPresent());
        assertEquals(expectedError, error.get());
    }

}
