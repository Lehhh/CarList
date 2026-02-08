package br.com.fiap.soat7;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.*;

class CarStoreViewApplicationTest {

    @Test
    void main_shouldCallSpringApplicationRun() {

        try (MockedStatic<SpringApplication> mocked =
                     mockStatic(SpringApplication.class)) {

            CarStoreViewApplication.main(new String[]{});

            mocked.verify(() ->
                    SpringApplication.run(
                            CarStoreViewApplication.class,
                            new String[]{}
                    )
            );
        }
    }
}
