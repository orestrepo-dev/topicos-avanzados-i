import java.util.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;


// ══════════════════════════════════════════════════════════════════════════════
// SECCIÓN 1 – SISTEMA DE GESTIÓN DE BIBLIOTECA (REFACTORIZADO)
//
// Código original:  LibrarySystem (clase monolítica con múltiples violaciones SOLID)
// Código mejorado:  Arquitectura en capas con separación de responsabilidades
//
// Principios SOLID aplicados:
//   S – Cada clase tiene una única responsabilidad
//   O – Extensible mediante interfaces sin modificar código existente
//   L – Implementaciones sustituibles sin alterar el comportamiento
//   I – Interfaces pequeñas y cohesivas (BookRepository, MemberRepository, etc.)
//   D – Las capas superiores dependen de abstracciones, no de implementaciones
//
// Patrones de diseño aplicados:
//   - Repository  → desacopla acceso a datos de la lógica de negocio
//   - Strategy    → notificaciones y cálculo de multas intercambiables
//   - Dependency Injection → inyección por constructor en servicios
// ══════════════════════════════════════════════════════════════════════════════


// ┌─────────────────────────────────────────────────────────────────────┐
// │  1. CAPA DE ABSTRACCIÓN (INTERFACES)                               │
// │  Principios: DIP (depender de abstracciones), OCP (extensibilidad),│
// │              ISP (interfaces segregadas y cohesivas)                │
// └─────────────────────────────────────────────────────────────────────┘


/**
 * Strategy para envío de notificaciones.
 * OCP: se pueden agregar nuevos canales (SMS, Slack, WhatsApp) sin modificar
 * el código existente, solo implementando esta interfaz.
 * ISP: la interfaz expone únicamente la operación de notificar.
 */
interface NotificationService {
    void notify(String recipient, String message);
}

/**
 * Strategy para cálculo de multas por devolución tardía.
 * OCP: se pueden definir nuevas políticas de multa (progresiva, escalonada)
 * sin modificar el servicio de préstamos.
 */
interface FineStrategy {
    double calculateFine(long daysOverdue);
}

/**
 * Repository para libros.
 * DIP: el servicio de negocio depende de esta abstracción, no de ArrayList.
 * ISP: solo expone operaciones relevantes para la gestión de libros.
 *
 * Reemplaza los bucles repetitivos de búsqueda lineal del código original:
 *     for (Book b : books) {
 *         if (b.getId().equals(bookId)) { book = b; break; }
 *     }
 */
interface BookRepository {
    Optional<Book> findById(String id);
    void save(Book book);
    List<Book> findAll();
}

/**
 * Repository para miembros.
 * DIP + ISP: misma justificación que BookRepository.
 *
 * Reemplaza:
 *     for (Member m : members) {
 *         if (m.getId().equals(memberId)) { member = m; break; }
 *     }
 */
interface MemberRepository {
    Optional<Member> findById(String id);
    void save(Member member);
    List<Member> findAll();
}

/**
 * Repository para préstamos.
 * DIP + ISP: desacopla persistencia de lógica de negocio.
 *
 * Reemplaza:
 *     - Búsquedas lineales por préstamo
 *     - Conteo manual de préstamos activos con bucle y contador
 *     - Filtrado manual por mes para reportes
 */
interface LoanRepository {
    void save(Loan loan);
    Optional<Loan> findById(String id);
    long countActiveLoansByMember(String memberId);
    List<Loan> findAll();
    List<Loan> findByMonth(int month, int year);
}


// ┌─────────────────────────────────────────────────────────────────────┐
// │  2. CAPA DE DOMINIO (MODELOS CON COMPORTAMIENTO)                   │
// │  Principio: SRP – cada modelo gestiona su propio estado            │
// │  Se elimina el antipatrón "Anemic Domain Model"                    │
// │  Se elimina el code smell "Feature Envy"                           │
// └─────────────────────────────────────────────────────────────────────┘


/**
 * Entidad de dominio: Libro.
 *
 * ANTES (código original):
 *     book.setAvailable(false);  // cualquier clase podía alterar el estado
 *
 * DESPUÉS (refactorizado):
 *     book.markAsLoaned();       // transición de estado con semántica de dominio
 *     book.markAsReturned();     // protege invariantes del modelo
 *
 * Se eliminó el setter público setAvailable(boolean) para proteger la
 * encapsulación. Solo el propio modelo decide cuándo cambia su estado.
 */
class Book {
    private String id;
    private String title;
    private String author;
    private boolean available;

    public Book(String id, String title, String author) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.available = true;
    }

    // Lógica de dominio encapsulada: transiciones de estado controladas
    public void markAsLoaned() {
        this.available = false;
    }

    public void markAsReturned() {
        this.available = true;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public boolean isAvailable() { return available; }

    @Override
    public String toString() { return title + " (" + author + ")"; }
}

/**
 * Entidad de dominio: Miembro de la biblioteca.
 */
class Member {
    private String id;
    private String name;
    private String email;

    public Member(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
}

/**
 * Entidad de dominio: Préstamo.
 *
 * ANTES (código original):
 *     loan.setReturned(true);   // setter público sin validación
 *     // Cálculo de multa disperso en LibrarySystem:
 *     long daysOverdue = ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now());
 *     double fine = daysOverdue * 0.50;  // magic number
 *
 * DESPUÉS (refactorizado):
 *     loan.isOverdue();         // el modelo sabe si está vencido
 *     loan.getDaysOverdue();    // el modelo calcula días de atraso
 *     loan.markAsReturned();    // transición de estado controlada
 *
 * Se eliminó:
 *   - Magic number 14 → constante DEFAULT_LOAN_DAYS
 *   - Setter público setReturned(boolean) → método markAsReturned()
 *   - Generación externa del ID → el modelo genera su propio UUID
 */
class Loan {
    private String id;
    private String memberId;
    private String bookId;
    private LocalDate loanDate;
    private LocalDate dueDate;
    private boolean returned;

    // Eliminación de Magic Number: 14 días → constante con nombre semántico
    private static final int DEFAULT_LOAN_DAYS = 14;

    public Loan(String memberId, String bookId) {
        this.id = UUID.randomUUID().toString();
        this.memberId = memberId;
        this.bookId = bookId;
        this.loanDate = LocalDate.now();
        this.dueDate = loanDate.plusDays(DEFAULT_LOAN_DAYS);
        this.returned = false;
    }

    /**
     * Lógica de dominio: determina si el préstamo está vencido.
     * Encapsula la regla de negocio dentro del modelo.
     */
    public boolean isOverdue() {
        return !returned && LocalDate.now().isAfter(dueDate);
    }

    /**
     * Lógica de dominio: calcula los días de atraso.
     * Antes, este cálculo estaba disperso en LibrarySystem.processReturn().
     */
    public long getDaysOverdue() {
        if (!isOverdue()) return 0;
        return ChronoUnit.DAYS.between(dueDate, LocalDate.now());
    }

    /**
     * Transición de estado controlada.
     * Reemplaza: loan.setReturned(true)
     */
    public void markAsReturned() {
        this.returned = true;
    }

    public String getId() { return id; }
    public String getMemberId() { return memberId; }
    public String getBookId() { return bookId; }
    public LocalDate getLoanDate() { return loanDate; }
    public LocalDate getDueDate() { return dueDate; }
    public boolean isReturned() { return returned; }
}


// ┌─────────────────────────────────────────────────────────────────────┐
// │  3. CAPA DE INFRAESTRUCTURA (IMPLEMENTACIONES CONCRETAS)           │
// │  Patrón Repository: implementación en memoria simulando BD         │
// │  Patrón Strategy: implementaciones concretas de multas y email     │
// └─────────────────────────────────────────────────────────────────────┘


/**
 * Implementación concreta: Notificación por Email.
 * OCP: si se necesita SMS, se crea SmsNotificationService implements NotificationService.
 * No se modifica ninguna clase existente.
 */
class EmailNotificationService implements NotificationService {
    @Override
    public void notify(String recipient, String message) {
        System.out.println(">>> [EMAIL] Para: " + recipient);
        System.out.println("    Mensaje: " + message);
        System.out.println("------------------------------------------------");
    }
}

/**
 * Implementación concreta: Estrategia de multa estándar (tarifa diaria).
 *
 * ANTES: double fine = daysOverdue * 0.50;  (hardcoded en LibrarySystem)
 * DESPUÉS: la tarifa se inyecta por constructor → configurable sin recompilar.
 *
 * OCP: para multa progresiva, crear ProgressiveFineStrategy implements FineStrategy.
 */
class StandardFineStrategy implements FineStrategy {
    private final double dailyRate;

    public StandardFineStrategy(double dailyRate) {
        this.dailyRate = dailyRate;
    }

    @Override
    public double calculateFine(long daysOverdue) {
        return daysOverdue * dailyRate;
    }
}

/**
 * Repository en memoria para libros. Usa HashMap para búsquedas O(1).
 *
 * ANTES: búsqueda lineal O(n) con for-each en ArrayList.
 * DESPUÉS: acceso directo O(1) por clave en HashMap.
 */
class InMemoryBookRepository implements BookRepository {
    private final Map<String, Book> store = new HashMap<>();

    @Override
    public Optional<Book> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public void save(Book book) {
        store.put(book.getId(), book);
    }

    @Override
    public List<Book> findAll() {
        return new ArrayList<>(store.values());
    }
}

/**
 * Repository en memoria para miembros.
 */
class InMemoryMemberRepository implements MemberRepository {
    private final Map<String, Member> store = new HashMap<>();

    @Override
    public Optional<Member> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public void save(Member member) {
        store.put(member.getId(), member);
    }

    @Override
    public List<Member> findAll() {
        return new ArrayList<>(store.values());
    }
}

/**
 * Repository en memoria para préstamos.
 * Incluye consulta findByMonth() para soportar el reporte mensual
 * que existía en el código original (generateMonthlyReport).
 */
class InMemoryLoanRepository implements LoanRepository {
    private final Map<String, Loan> store = new HashMap<>();

    @Override
    public void save(Loan loan) {
        store.put(loan.getId(), loan);
    }

    @Override
    public Optional<Loan> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public long countActiveLoansByMember(String memberId) {
        return store.values().stream()
                .filter(l -> l.getMemberId().equals(memberId) && !l.isReturned())
                .count();
    }

    @Override
    public List<Loan> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Loan> findByMonth(int month, int year) {
        return store.values().stream()
                .filter(l -> l.getLoanDate().getMonthValue() == month
                          && l.getLoanDate().getYear() == year)
                .collect(Collectors.toList());
    }
}


// ┌─────────────────────────────────────────────────────────────────────┐
// │  4. CAPA DE SERVICIO (LÓGICA DE NEGOCIO)                          │
// │  SRP: LoanService → solo coordina préstamos y devoluciones         │
// │  SRP: ReportService → solo genera reportes                         │
// │  DIP: ambos dependen de abstracciones inyectadas por constructor   │
// └─────────────────────────────────────────────────────────────────────┘


/**
 * Servicio de préstamos: coordina el flujo de préstamo y devolución.
 *
 * SRP: solo maneja la lógica transaccional de préstamos.
 * DIP: todas las dependencias son interfaces inyectadas por constructor.
 *
 * ANTES (LibrarySystem.processLoan + processReturn):
 *   - Buscaba entidades con bucles lineales
 *   - Calculaba multas con magic numbers
 *   - Enviaba emails con System.out.println hardcodeado
 *   - Generaba reportes en el mismo método
 *   - No lanzaba excepciones (usaba print + return)
 *
 * DESPUÉS (LoanService):
 *   - Busca entidades vía Repository (abstracción)
 *   - Delega multas a FineStrategy (Strategy pattern)
 *   - Delega notificaciones a NotificationService (Strategy pattern)
 *   - Reportes en clase separada (ReportService)
 *   - Usa excepciones para flujo de errores (más testeable)
 */
class LoanService {
    private final BookRepository bookRepo;
    private final MemberRepository memberRepo;
    private final LoanRepository loanRepo;
    private final NotificationService notificationService;
    private final FineStrategy fineStrategy;

    // Eliminación de Magic Number: 5 → constante semántica
    private static final int MAX_ACTIVE_LOANS = 5;

    // Inyección de Dependencias por constructor (DIP)
    public LoanService(
            BookRepository bookRepo,
            MemberRepository memberRepo,
            LoanRepository loanRepo,
            NotificationService notificationService,
            FineStrategy fineStrategy
    ) {
        this.bookRepo = bookRepo;
        this.memberRepo = memberRepo;
        this.loanRepo = loanRepo;
        this.notificationService = notificationService;
        this.fineStrategy = fineStrategy;
    }

    /**
     * Procesa un préstamo de libro.
     *
     * @param memberId identificador del miembro
     * @param bookId   identificador del libro
     * @return el préstamo creado
     * @throws IllegalArgumentException si miembro o libro no existen
     * @throws IllegalStateException    si el libro no está disponible o se superó el límite
     */
    public Loan borrowBook(String memberId, String bookId) {
        // 1. Validaciones (usando Repository en lugar de bucles lineales)
        Member member = memberRepo.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Error: Miembro no encontrado con ID: " + memberId));

        Book book = bookRepo.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Error: Libro no encontrado con ID: " + bookId));

        if (!book.isAvailable()) {
            throw new IllegalStateException(
                "Error: El libro '" + book.getTitle() + "' no está disponible.");
        }

        // 2. Regla de negocio: límite de préstamos activos
        if (loanRepo.countActiveLoansByMember(memberId) >= MAX_ACTIVE_LOANS) {
            throw new IllegalStateException(
                "Error: Límite de " + MAX_ACTIVE_LOANS + " préstamos activos alcanzado.");
        }

        // 3. Ejecución: crear préstamo y actualizar estado del libro
        Loan loan = new Loan(memberId, bookId);
        book.markAsLoaned();

        // 4. Persistencia
        loanRepo.save(loan);
        bookRepo.save(book);

        // 5. Notificación (delegada a estrategia inyectada)
        notificationService.notify(member.getEmail(),
            "Préstamo confirmado: " + book.getTitle()
            + ". Fecha límite de devolución: " + loan.getDueDate());

        return loan;
    }

    /**
     * Procesa la devolución de un libro.
     *
     * CORRECCIÓN CRÍTICA respecto a la versión anterior:
     * El cálculo de multa se realiza ANTES de marcar el préstamo como devuelto,
     * porque isOverdue() verifica !returned. Si se marca primero como devuelto,
     * isOverdue() siempre retorna false y la multa nunca se calcula.
     *
     * @param loanId identificador del préstamo
     * @return monto de la multa (0 si no hay atraso)
     */
    public double returnBook(String loanId) {
        Loan loan = loanRepo.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "Error: Préstamo no encontrado con ID: " + loanId));

        Book book = bookRepo.findById(loan.getBookId())
                .orElseThrow(() -> new IllegalStateException(
                    "Error de integridad: Libro no existe en el sistema."));

        Member member = memberRepo.findById(loan.getMemberId())
                .orElseThrow(() -> new IllegalStateException(
                    "Error de integridad: Miembro no existe en el sistema."));

        // 1. Cálculo de multa ANTES de marcar como devuelto (corrección del bug)
        double fine = 0;
        if (loan.isOverdue()) {
            fine = fineStrategy.calculateFine(loan.getDaysOverdue());
            System.out.println("$$$ MULTA APLICADA: $" + fine
                + " por retraso de " + loan.getDaysOverdue() + " días.");
        }

        // 2. Marcar como devuelto (después del cálculo de multa)
        loan.markAsReturned();
        book.markAsReturned();

        // 3. Persistencia
        loanRepo.save(loan);
        bookRepo.save(book);

        // 4. Notificación
        String mensaje = "Devolución confirmada: " + book.getTitle();
        if (fine > 0) {
            mensaje += ". Multa generada: $" + fine;
        }
        notificationService.notify(member.getEmail(), mensaje);

        return fine;
    }
}

/**
 * Servicio de reportes: genera informes estadísticos.
 *
 * SRP: esta clase SOLO se encarga de generar reportes.
 * En el código original, generateMonthlyReport() estaba dentro de LibrarySystem
 * junto con processLoan() y processReturn(), violando SRP.
 *
 * Separar el reporte en su propia clase permite:
 *   - Modificar la lógica de reportes sin afectar préstamos
 *   - Testear reportes de forma aislada
 *   - Agregar nuevos tipos de reporte sin tocar LoanService
 */
class ReportService {
    private final LoanRepository loanRepo;
    private final FineStrategy fineStrategy;

    public ReportService(LoanRepository loanRepo, FineStrategy fineStrategy) {
        this.loanRepo = loanRepo;
        this.fineStrategy = fineStrategy;
    }

    /**
     * Genera el reporte mensual de préstamos, devoluciones y multas.
     *
     * ANTES (en LibrarySystem.generateMonthlyReport):
     *     - Iteraba loans con bucle for y contadores manuales
     *     - Usaba magic number 0.50 para calcular multas
     *     - Mezclaba lógica de reporte con lógica de préstamos
     *
     * DESPUÉS:
     *     - Usa LoanRepository.findByMonth() para filtrar
     *     - Usa Stream API para cálculos declarativos
     *     - Delega cálculo de multa a FineStrategy (consistencia)
     */
    public void generateMonthlyReport(int month, int year) {
        List<Loan> monthlyLoans = loanRepo.findByMonth(month, year);

        long totalLoans = monthlyLoans.size();
        long totalReturns = monthlyLoans.stream()
                .filter(Loan::isReturned)
                .count();
        double totalPendingFines = monthlyLoans.stream()
                .filter(l -> !l.isReturned() && l.isOverdue())
                .mapToDouble(l -> fineStrategy.calculateFine(l.getDaysOverdue()))
                .sum();

        System.out.println("=== REPORTE MENSUAL (" + month + "/" + year + ") ===");
        System.out.println("Total de préstamos:     " + totalLoans);
        System.out.println("Total de devoluciones:  " + totalReturns);
        System.out.println("Multas pendientes:      $" + totalPendingFines);
        System.out.println("================================================");
    }
}


// ┌─────────────────────────────────────────────────────────────────────┐
// │  5. CLASE PRINCIPAL (ENTRY POINT)                                  │
// │  Configura dependencias y demuestra el funcionamiento del sistema   │
// └─────────────────────────────────────────────────────────────────────┘


public class LibrarySystemRefactored {
    public static void main(String[] args) {

        // ── 1. Configuración de Dependencias (Wiring Manual) ──
        // En un proyecto real, esto lo haría un contenedor IoC (Spring, Guice).
        BookRepository bookRepo       = new InMemoryBookRepository();
        MemberRepository memberRepo   = new InMemoryMemberRepository();
        LoanRepository loanRepo       = new InMemoryLoanRepository();
        NotificationService emailSvc  = new EmailNotificationService();
        FineStrategy fineStrategy     = new StandardFineStrategy(0.50);

        // ── 2. Inyección en Servicios ──
        LoanService loanService = new LoanService(
            bookRepo, memberRepo, loanRepo, emailSvc, fineStrategy);
        ReportService reportService = new ReportService(loanRepo, fineStrategy);

        // ── 3. Datos de Prueba ──
        Book b1 = new Book("B001", "Clean Code", "Robert C. Martin");
        Book b2 = new Book("B002", "Design Patterns", "Gang of Four");
        bookRepo.save(b1);
        bookRepo.save(b2);

        Member m1 = new Member("M001", "Edgardo Pitti", "edgardo@example.com");
        memberRepo.save(m1);

        // ── 4. Caso de Uso: Préstamo Exitoso ──
        try {
            System.out.println("=== INTENTO DE PRÉSTAMO ===");
            Loan prestamo = loanService.borrowBook("M001", "B001");
            System.out.println("Préstamo creado con ID: " + prestamo.getId());

            // ── 5. Caso de Uso: Préstamo Duplicado (Debe fallar) ──
            System.out.println("\n=== INTENTO DE PRÉSTAMO DUPLICADO ===");
            loanService.borrowBook("M001", "B001");

        } catch (Exception e) {
            System.out.println("Excepción controlada: " + e.getMessage());
        }

        // ── 6. Caso de Uso: Devolución ──
        try {
            String loanId = loanRepo.findAll().get(0).getId();
            System.out.println("\n=== PROCESO DE DEVOLUCIÓN ===");
            double multa = loanService.returnBook(loanId);
            System.out.println("Multa total: $" + multa);

        } catch (Exception e) {
            System.out.println("Error en devolución: " + e.getMessage());
        }

        // ── 7. Caso de Uso: Reporte Mensual ──
        System.out.println();
        LocalDate hoy = LocalDate.now();
        reportService.generateMonthlyReport(hoy.getMonthValue(), hoy.getYear());
    }
}
