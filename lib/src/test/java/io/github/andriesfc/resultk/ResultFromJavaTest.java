package io.github.andriesfc.resultk;

import io.github.andriesfc.resultk.demo.taxcalc.TaxCalculationService;
import io.github.andriesfc.resultk.demo.taxcalc.TaxCalculationService.CalculationError;
import io.github.andriesfc.resultk.demo.taxcalc.TaxCalculationService.CalculationError.Indicator;
import io.github.andriesfc.resultk.demo.taxcalc.TaxCalculationService.TaxCalculation;
import io.github.andriesfc.resultk.demo.taxcalc.TaxCalculationService.TaxableEntity;
import kotlin.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static java.util.Collections.emptySet;
import static kotlin.collections.MapsKt.mapOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ResultFromJavaTest {

    @Mock
    private TaxCalculationService taxCalculationService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void handlingErrorWithTraditionalTryCatch() {

        var error = new CalculationError("taxRef", "badError", mapOf(
                new Pair<>(Indicator.AmountNotInRange, "30 is not in range"),
                new Pair<>(Indicator.TechnicalError, "Try again")));

        when(taxCalculationService.calculateTax(any(TaxableEntity.class), any(), any())).thenReturn(ResultOperations.failure(error));

        Result<CalculationError, TaxCalculation> r = taxCalculationService.calculateTax(
                new TaxableEntity("", ""), emptySet(), emptySet());

        System.out.println(r);

        try {
            System.out.println(ResultOperations.get(r));
        } catch (Exception e) {
            var re = ResultOperations.getErrorOrNull(r);
            System.out.println(re);
            assert re != null;
            System.out.println(re.hashCode());
        }

    }

}
