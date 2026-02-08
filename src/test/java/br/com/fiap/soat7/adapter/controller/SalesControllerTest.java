package br.com.fiap.soat7.adapter.controller;

import br.com.fiap.soat7.data.domain.Sale;
import br.com.fiap.soat7.data.domain.dto.PaymentWebhookRequest;
import br.com.fiap.soat7.data.domain.dto.PurchaseRequest;
import br.com.fiap.soat7.data.domain.dto.PurchaseResponse;
import br.com.fiap.soat7.usecase.services.SalesService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesControllerTest {

    @Mock
    SalesService salesService;

    @InjectMocks
    SalesController controller;

    @Test
    void listAvailable_deveRetornar200ComListaDoService() {
        // arrange
        Sale s1 = mock(Sale.class);
        Sale s2 = mock(Sale.class);
        List<Sale> expected = List.of(s1, s2);

        when(salesService.listAvailable()).thenReturn(expected);

        // act
        ResponseEntity<List<Sale>> resp = controller.listAvailable();

        // assert
        verify(salesService, times(1)).listAvailable();
        verifyNoMoreInteractions(salesService);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isSameAs(expected);
    }

    @Test
    void listSold_deveRetornar200ComListaDoService() {
        // arrange
        List<Sale> expected = List.of(mock(Sale.class));
        when(salesService.listSold()).thenReturn(expected);

        // act
        ResponseEntity<List<Sale>> resp = controller.listSold();

        // assert
        verify(salesService, times(1)).listSold();
        verifyNoMoreInteractions(salesService);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isSameAs(expected);
    }

    @Test
    void purchase_deveChamarServiceComCarIdEDepvolver200() {
        // arrange
        PurchaseRequest req = mock(PurchaseRequest.class);
        when(req.carId()).thenReturn(55L);

        PurchaseResponse expected = mock(PurchaseResponse.class);
        when(salesService.purchase(55L)).thenReturn(expected);

        // act
        ResponseEntity<PurchaseResponse> resp = controller.purchase(req);

        // assert
        verify(req, times(1)).carId();
        verify(salesService, times(1)).purchase(55L);
        verifyNoMoreInteractions(salesService);

        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody()).isSameAs(expected);
    }

    @Test
    void webhook_deveChamarHandlePaymentWebhookEDevolver204() {
        // arrange
        PaymentWebhookRequest req = mock(PaymentWebhookRequest.class);

        // act
        ResponseEntity<Void> resp = controller.webhook(req);

        // assert
        verify(salesService, times(1)).handlePaymentWebhook(req);
        verifyNoMoreInteractions(salesService);

        assertThat(resp.getStatusCode().value()).isEqualTo(204);
        assertThat(resp.getBody()).isNull();
    }

    @Test
    void webhook_quandoServiceLancarExcecao_devePropagar() {
        // arrange
        PaymentWebhookRequest req = mock(PaymentWebhookRequest.class);
        RuntimeException boom = new RuntimeException("falhou");
        doThrow(boom).when(salesService).handlePaymentWebhook(req);

        // act + assert
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> controller.webhook(req));

        verify(salesService, times(1)).handlePaymentWebhook(req);
        verifyNoMoreInteractions(salesService);
    }
}
