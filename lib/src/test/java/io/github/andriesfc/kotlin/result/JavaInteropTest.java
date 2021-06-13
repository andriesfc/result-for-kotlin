package io.github.andriesfc.kotlin.result;

import io.github.andriesfc.kotlin.result.demo.taxcalc.TaxCalculationService;
import io.github.andriesfc.kotlin.result.demo.taxcalc.TaxCalculationService.CalculationError;
import io.github.andriesfc.kotlin.result.demo.taxcalc.TaxCalculationService.CalculationError.Indicator;
import io.github.andriesfc.kotlin.result.demo.taxcalc.TaxCalculationService.TaxCalculation;
import io.github.andriesfc.kotlin.result.demo.taxcalc.TaxCalculationService.TaxableEntity;
import kotlin.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
            var re = getErrorOrNull(r);
            System.out.println(re);
            assert re != null;
            System.out.println(re.hashCode());
        }

    }

    @ParameterizedTest
    @CsvSource(value = {"@null", "6"}, nullValues = "@null")
    void convertingOptionalToResult(Integer beansCounted) {
        Optional<Integer> optional = Optional.ofNullable(beansCounted);
        Result<String, Integer> r = result(optional, () -> "bean_counter_offline");
        if (optional.isPresent()) {
            assertEquals(beansCounted, r.get());
        } else {
            assertEquals("bean_counter_offline", getErrorOrNull(r));
        }
    }

    @Test
    void resultHandlingOnThrowingException() {
        File file = new File(System.getProperty("user.home"), "819fe5fe-1722-4457-810b-978525f6999b");
        Result<IOException, Long> fileSize = result(IOException.class, () -> {
            if (!file.exists()) {
                throw new FileNotFoundException(String.format("File not found: %s", file));
            }
            if (!file.isFile()) {
                throw new IOException("Unexpected file type : " + file);
            }
            return file.length();
        });
        onSuccess(fileSize, j -> {
            System.out.println("file size = " + j);
        });
        onFailure(fileSize, e -> {
            System.out.println("error = " + e);
        });
        assertTrue(isFailure(fileSize));
        assertThrows(IOException.class, fileSize::get);

        Result<String, Long> fileSizeString = mapFailure(fileSize, Throwable::getMessage);
        System.out.printf("%s%n", fileSizeString);

        var error = getErrorOrNull(fileSize);
        if (error != null) {
            error.printStackTrace();
        }
    }
}
