package com.example.demo.Service;

import com.example.demo.Entity.ClientEntity;
import com.example.demo.Entity.LoanEntity;
import com.example.demo.Entity.ToolEntity;
import com.example.demo.Repository.ClientRepository;
import com.example.demo.Repository.LoanRepository;
import com.example.demo.Repository.ToolRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanService - Tests Completos")
class LoanServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private ToolRepository toolRepository;
    @Mock private ConfigService configService;
    @Mock private KardexService kardexService;
    @Mock private ClientService clientService;

    @InjectMocks private LoanService loanService;

    // ==================== HELPERS ====================

    private ClientEntity client(Long id, String state) {
        ClientEntity c = new ClientEntity();
        c.setId(id);
        c.setName("Cliente Test");
        c.setRut("12.345.678-9");
        c.setEmail("test@test.cl");
        c.setPhone("+56912345678");
        c.setState(state);
        return c;
    }

    private ToolEntity tool(Long id, String status, Integer stock) {
        ToolEntity t = new ToolEntity();
        t.setId(id);
        t.setName("Herramienta Test");
        t.setCategory("Categoria");
        t.setStatus(status);
        t.setStock(stock);
        t.setReplacementValue(50000);
        return t;
    }

    private LoanEntity loan(Long id, ClientEntity client, ToolEntity tool,
                            LocalDate start, LocalDate due, String status) {
        LoanEntity l = new LoanEntity();
        l.setId(id);
        l.setClient(client);
        l.setTool(tool);
        l.setStartDate(start);
        l.setDueDate(due);
        l.setStatus(status);
        l.setFine(0.0);
        l.setRentalCost(7000.0);
        l.setDamaged(false);
        l.setIrreparable(false);
        return l;
    }

    // ==================== VALIDACIONES DE PARAMETROS ====================

    @Nested
    @DisplayName("Validaciones de parametros")
    class ParameterValidationTests {

        @Test
        @DisplayName("Debe rechazar clientId null")
        void createLoan_nullClientId_throwsException() {
            assertThatThrownBy(() ->
                    loanService.createLoan(null, 10L, LocalDate.now().plusDays(7), "admin"))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("Debe rechazar toolId null")
        void createLoan_nullToolId_throwsException() {
            assertThatThrownBy(() ->
                    loanService.createLoan(1L, null, LocalDate.now().plusDays(7), "admin"))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("Debe rechazar dueDate null")
        void createLoan_nullDueDate_throwsException() {
            assertThatThrownBy(() ->
                    loanService.createLoan(1L, 10L, null, "admin"))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("Debe rechazar cliente no encontrado")
        void createLoan_clientNotFound_throwsException() {
            when(clientRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    loanService.createLoan(1L, 10L, LocalDate.now().plusDays(7), "admin"))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("Debe rechazar herramienta no encontrada")
        void createLoan_toolNotFound_throwsException() {
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client(1L, "Activo")));
            when(toolRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    loanService.createLoan(1L, 10L, LocalDate.now().plusDays(7), "admin"))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }

    // ==================== REGLAS DE NEGOCIO ====================

    @Nested
    @DisplayName("Reglas de negocio")
    class BusinessRulesTests {

        @Test
        @DisplayName("Debe rechazar cliente con estado Restringido")
        void createLoan_clientRestricted_throwsException() {
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client(1L, "Restringido")));
            when(toolRepository.findById(10L)).thenReturn(Optional.of(tool(10L, "Disponible", 5)));

            assertThatThrownBy(() ->
                    loanService.createLoan(1L, 10L, LocalDate.now().plusDays(7), "admin"))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("Debe rechazar herramienta sin stock disponible")
        void createLoan_toolOutOfStock_throwsException() {
            when(clientRepository.findById(1L)).thenReturn(Optional.of(client(1L, "Activo")));
            when(toolRepository.findById(10L)).thenReturn(Optional.of(tool(10L, "Disponible", 0)));
            when(loanRepository.hasOverduesOrFines(1L)).thenReturn(false);
            when(loanRepository.findAll()).thenReturn(new ArrayList<>());

            assertThatThrownBy(() ->
                    loanService.createLoan(1L, 10L, LocalDate.now().plusDays(7), "admin"))
                    .isInstanceOf(ResponseStatusException.class);
        }
    }

    // ==================== CASO EXITOSO ====================

    @Nested
    @DisplayName("Caso exitoso")
    class SuccessTests {

        @Test
        @DisplayName("Debe crear prestamo exitosamente")
        void createLoan_success_returnsLoanEntity() {
            ClientEntity client = client(1L, "Activo");
            ToolEntity tool = tool(10L, "Disponible", 5);

            when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
            when(toolRepository.findById(10L)).thenReturn(Optional.of(tool));
            when(loanRepository.hasOverduesOrFines(1L)).thenReturn(false);
            when(loanRepository.findAll()).thenReturn(new ArrayList<>());
            when(configService.getTarifaArriendoDiaria()).thenReturn(5000.0);
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(invocation -> {
                LoanEntity saved = invocation.getArgument(0);
                saved.setId(100L);
                return saved;
            });
            when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            LoanEntity result = loanService.createLoan(1L, 10L, LocalDate.now().plusDays(7), "admin");

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getStatus()).isEqualTo("Vigente");
        }
    }

    // ==================== RETURN TOOL ====================

    @Nested
    @DisplayName("returnTool()")
    class ReturnToolTests {

        @Test
        @DisplayName("Debe rechazar prestamo no encontrado")
        void returnTool_loanNotFound_throwsException() {
            when(loanRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    loanService.returnTool(999L, false, false, "admin"))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("Debe devolver prestamo a tiempo sin danos")
        void returnTool_onTime_noDamage_returnsLoan() {
            ClientEntity client = client(1L, "Activo");
            ToolEntity tool = tool(10L, "Prestada", 5);
            LoanEntity loan = loan(100L, client, tool,
                    LocalDate.now().minusDays(5), LocalDate.now().plusDays(2), "Vigente");

            when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
            when(configService.getTarifaMultaDiaria()).thenReturn(2000.0);
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(loanRepository.findAll()).thenReturn(List.of(loan));

            LoanEntity result = loanService.returnTool(100L, false, false, "admin");

            assertThat(result.getStatus()).isEqualTo("Devuelto");
            assertThat(result.getFine()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Debe devolver prestamo atrasado con multa")
        void returnTool_late_withFine_returnsLoan() {
            ClientEntity client = client(1L, "Activo");
            ToolEntity tool = tool(10L, "Prestada", 5);
            LoanEntity loan = loan(100L, client, tool,
                    LocalDate.now().minusDays(10), LocalDate.now().minusDays(3), "Vigente");

            when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
            when(configService.getTarifaMultaDiaria()).thenReturn(2000.0);
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(loanRepository.findAll()).thenReturn(List.of(loan));

            LoanEntity result = loanService.returnTool(100L, false, false, "admin");

            assertThat(result.getStatus()).isEqualTo("Atrasado");
            assertThat(result.getFine()).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("Debe cobrar valor de reposicion por dano irreparable")
        void returnTool_irreparableDamage_chargesReplacementValue() {
            ClientEntity client = client(1L, "Activo");
            ToolEntity tool = tool(10L, "Prestada", 5);
            LoanEntity loan = loan(100L, client, tool,
                    LocalDate.now().minusDays(5), LocalDate.now().plusDays(2), "Vigente");

            when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
            when(configService.getTarifaMultaDiaria()).thenReturn(2000.0);
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(loanRepository.findAll()).thenReturn(List.of(loan));

            LoanEntity result = loanService.returnTool(100L, true, true, "admin");

            assertThat(result.getIrreparable()).isTrue();
            assertThat(result.getFine()).isEqualTo(50000.0);
        }

        @Test
        @DisplayName("Debe cobrar cargo de reparacion por dano reparable")
        void returnTool_reparableDamage_chargesRepairCost() {
            ClientEntity client = client(1L, "Activo");
            ToolEntity tool = tool(10L, "Prestada", 5);
            LoanEntity loan = loan(100L, client, tool,
                    LocalDate.now().minusDays(5), LocalDate.now().plusDays(2), "Vigente");

            when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
            when(configService.getTarifaMultaDiaria()).thenReturn(2000.0);
            when(configService.getCargoReparacion()).thenReturn(10000.0);
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(toolRepository.save(any(ToolEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(loanRepository.findAll()).thenReturn(List.of(loan));

            LoanEntity result = loanService.returnTool(100L, true, false, "admin");

            assertThat(result.getDamaged()).isTrue();
            assertThat(result.getFine()).isEqualTo(10000.0);
        }
    }

    // ==================== OTROS METODOS ====================

    @Nested
    @DisplayName("Otros metodos")
    class OtherMethodsTests {

        @Test
        @DisplayName("updateOverdueLoans debe actualizar prestamos vencidos")
        void updateOverdueLoans_updatesOverdueLoans() {
            ClientEntity client = client(1L, "Activo");
            ToolEntity tool = tool(10L, "Prestada", 5);
            LoanEntity loan = loan(100L, client, tool,
                    LocalDate.now().minusDays(10), LocalDate.now().minusDays(3), "Vigente");

            when(loanRepository.findAll()).thenReturn(List.of(loan));
            when(configService.getTarifaMultaDiaria()).thenReturn(2000.0);
            when(loanRepository.save(any(LoanEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

            int result = loanService.updateOverdueLoans();

            assertThat(result).isEqualTo(1);
        }

        @Test
        @DisplayName("scheduledUpdateLoanStatuses no debe lanzar excepciones")
        void scheduledUpdateLoanStatuses_doesNotThrowException() {
            when(loanRepository.findAll()).thenReturn(new ArrayList<>());

            assertThatCode(() -> loanService.scheduledUpdateLoanStatuses())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("getAllLoans debe retornar lista de prestamos")
        void getAllLoans_returnsList() {
            when(loanRepository.findAll()).thenReturn(new ArrayList<>());

            List<LoanEntity> result = loanService.getAllLoans();

            assertThat(result).isEmpty();
        }
    }
}