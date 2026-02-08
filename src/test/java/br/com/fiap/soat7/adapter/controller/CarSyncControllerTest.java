package br.com.fiap.soat7.adapter.controller;

import br.com.fiap.soat7.data.domain.dto.CarSyncRequest;
import br.com.fiap.soat7.usecase.services.CarSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarSyncControllerTest {

    @Mock
    CarSyncService carSyncService;

    @InjectMocks
    CarSyncController controller;

    @Test
    void upsert_deveChamarServiceEDevolver204() {
        // arrange
        CarSyncRequest req = mock(CarSyncRequest.class); // não depende do construtor do DTO

        // act
        ResponseEntity<Void> resp = controller.upsert(req);

        // assert
        verify(carSyncService, times(1)).upsert(req);
        verifyNoMoreInteractions(carSyncService);

        assertThat(resp.getStatusCode().value()).isEqualTo(204);
        assertThat(resp.getBody()).isNull();
    }

    @Test
    void upsert_quandoServiceLancarExcecao_devePropagar() {
        // arrange
        CarSyncRequest req = mock(CarSyncRequest.class);
        RuntimeException boom = new RuntimeException("falhou");
        doThrow(boom).when(carSyncService).upsert(req);

        // act + assert
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> controller.upsert(req));

        verify(carSyncService, times(1)).upsert(req);
        verifyNoMoreInteractions(carSyncService);
    }
}
