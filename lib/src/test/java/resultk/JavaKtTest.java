package resultk;

import kotlin.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import resultk.demo.taxcalc.TaxCalculationService;

import java.util.Optional;

import static java.util.Collections.emptySet;
import static kotlin.collections.MapsKt.mapOf;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class JavaKtTest {

    @Mock
    private TaxCalculationService taxCalculationService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void handlingErrorWithTraditionalTryCatch() {

        //noinspection unchecked
        var error = new TaxCalculationService.CalculationError("taxRef", "badError", mapOf(
                new Pair<>(TaxCalculationService.CalculationError.Indicator.AmountNotInRange, "30 is not in range"),
                new Pair<>(TaxCalculationService.CalculationError.Indicator.TechnicalError, "Try again")));

        when(taxCalculationService.calculateTax(any(TaxCalculationService.TaxableEntity.class), any(), any())).thenReturn(resultk.ResultOperations.failure(error));

        Result<TaxCalculationService.CalculationError, TaxCalculationService.TaxCalculation> r = taxCalculationService.calculateTax(
                new TaxCalculationService.TaxableEntity("", ""), emptySet(), emptySet());

        System.out.println(r);

        resultk.ResultOperations.onFailure(r, e -> {});
        resultk.ResultOperations.onSuccess(r, v -> {});

        try {
            System.out.println(r.get());
        } catch (Exception e) {
            var re = resultk.ResultOperations.errorOrNull(r);
            System.out.println(re);
            assert re != null;
            System.out.println(re.hashCode());
        }

    }

    @Test
    void testIdiomaticUseOnExpectedFailure() {
        final String expectedError = "error";
        final Result<String, Integer> result = resultk.ResultOperations.result(String.class, () -> resultk.ResultOperations.failure(expectedError));
        assertThrows(WrappedFailureAsException.class, result::get);
        assertEquals(expectedError, resultk.ResultOperations.errorOrNull(result));
    }

    @Test
    void testIdiomaticUseOnExpectedOnSuccess() {
        final int expectedCount = 10;
        final Result<String,Integer> result = resultk.ResultOperations.result(String.class, () -> resultk.ResultOperations.success(expectedCount));
        assertDoesNotThrow(result::get);
        assertEquals(expectedCount, result.get());
    }

    @Test
    void testIdiomaticUseOnExpectedFailureAsOptional() {
        final String expectedError = "bean_counter_offline";
        final Result<String,Integer> result = resultk.ResultOperations.result(String.class, () -> resultk.ResultOperations.failure(expectedError));
        final Optional<String> error = resultk.ResultOperations.errorOrEmpty(result);
        assertThrows(WrappedFailureAsException.class, result::get);
        assertTrue(error.isPresent());
        assertEquals(expectedError, error.get());
    }

}
