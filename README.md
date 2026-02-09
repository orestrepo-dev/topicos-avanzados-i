# LABORATORIO #1 – PRINCIPIOS SOLID Y PATRONES DE DISEÑO

**Universidad Tecnológica de Panamá**
**Facultad de Ingeniería de Sistemas Computacionales**
**Maestría en Ingeniería de Software – Verano 2025**

**Profesor:** Danilo Domínguez Pérez, PhD
**Grupo 1:** Osvaldo Restrepo, Edgardo Pittí, Carlos Salazar

---

## TABLA DE CONTENIDOS

1. [Introducción](#1-introducción)
2. [Sección 1 – Análisis](#2-sección-1--análisis)
3. [Sección 1 – Refactorización](#3-sección-1--refactorización)
4. [Sección 1 – Trade-offs](#4-sección-1--trade-offs)
5. [Sección 2 – Arquitectura](#5-sección-2--arquitectura)
6. [Sección 2 – Patrones y SOLID](#6-sección-2--patrones-y-solid)
7. [Sección 2 – Código](#7-sección-2--código)
8. [Sección 2 – Ejemplo de uso](#8-sección-2--ejemplo-de-uso)
9. [Sección 2 – Trade-offs](#9-sección-2--trade-offs)
10. [Conclusión técnica](#10-conclusión-técnica)

---

## 1. INTRODUCCIÓN

El presente laboratorio aborda dos ejes fundamentales de la ingeniería de software: la **refactorización de código existente** con violaciones de principios SOLID, y el **diseño desde cero** de un sistema extensible aplicando patrones de diseño. Ambas secciones se resuelven en Java, manteniendo coherencia con el código proporcionado por el docente.

La **Sección 1** parte de un sistema monolítico `LibrarySystem` con múltiples antipatrones, y lo transforma en una arquitectura por capas con bajo acoplamiento. La **Sección 2** diseña e implementa un Sistema de Notificaciones Multi-Canal con énfasis en extensibilidad y testabilidad.

**Archivos de implementación producidos:**

| Carpeta / Archivo | Contenido |
|-------------------|-----------|
| `Seccion1-Biblioteca/LibrarySystemRefactored.java` | Sección 1 – Sistema de biblioteca refactorizado |
| `Seccion2-Notificaciones/NotificationSystem.java` | Sección 2 – Sistema de notificaciones multi-canal |

---

## 2. SECCIÓN 1 – ANÁLISIS

### 2.1 Código original bajo análisis

El código proporcionado en el enunciado del laboratorio consiste en una clase `LibrarySystem` con tres métodos (`processLoan`, `processReturn`, `generateMonthlyReport`) y tres clases de soporte anémicas (`Book`, `Member`, `Loan`). Todas las responsabilidades del sistema se concentran en una única clase.

### 2.2 Violaciones de Principios SOLID identificadas

#### VIOLACIÓN S – Single Responsibility Principle (GRAVEDAD: CRÍTICA)

La clase `LibrarySystem` asume al menos **seis responsabilidades distintas**:

| # | Responsabilidad | Evidencia en el código |
|---|----------------|----------------------|
| 1 | Almacenamiento de datos | `private List<Book> books`, `List<Member> members`, `List<Loan> loans` |
| 2 | Búsqueda de entidades | Bucles `for` repetidos para encontrar Book, Member, Loan |
| 3 | Lógica de préstamos | Validación de disponibilidad, límite de 5 préstamos |
| 4 | Cálculo de multas | `daysOverdue * 0.50` hardcodeado en `processReturn` |
| 5 | Envío de notificaciones | `System.out.println("Enviando email...")` |
| 6 | Generación de reportes | `generateMonthlyReport()` con lógica de agregación |

**Por qué es problema:** Cualquier cambio en una responsabilidad (ej.: cambiar el canal de notificación) obliga a modificar `LibrarySystem`, con riesgo de afectar las demás funcionalidades. Según Robert C. Martin, una clase debe tener *"una, y solo una, razón para cambiar"*.

#### VIOLACIÓN O – Open/Closed Principle (GRAVEDAD: ALTA)

- **Notificaciones hardcodeadas:** Para agregar SMS, hay que modificar `processLoan()` y `processReturn()`.
- **Multas hardcodeadas:** `daysOverdue * 0.50` — cambiar la política requiere editar el método.
- **Reportes inflexibles:** Agregar un reporte semanal requiere modificar la clase.

**Por qué es problema:** El software debería estar *abierto a extensión pero cerrado a modificación*. Cada nuevo requisito obliga a tocar código existente y potencialmente probado.

#### VIOLACIÓN L – Liskov Substitution Principle (GRAVEDAD: MEDIA)

Los modelos `Book` y `Loan` exponen setters públicos sin restricciones:

```java
book.setAvailable(false);  // cualquier clase puede poner estados incoherentes
loan.setReturned(true);    // no hay validación de transición de estado
```

**Por qué es problema:** Cualquier código externo puede violar las invariantes del dominio. Un `Loan` podría marcarse como "devuelto" sin que el `Book` correspondiente se libere, generando inconsistencias.

#### VIOLACIÓN I – Interface Segregation Principle (GRAVEDAD: MEDIA)

No existen interfaces en absoluto. Si otra clase necesitara solo consultar libros, debería depender de toda la clase `LibrarySystem` con sus 3 métodos y 3 listas internas.

**Por qué es problema:** Los clientes se ven forzados a depender de métodos que no utilizan, aumentando el acoplamiento innecesariamente.

#### VIOLACIÓN D – Dependency Inversion Principle (GRAVEDAD: ALTA)

- `LibrarySystem` depende directamente de `ArrayList` (implementación concreta).
- Las notificaciones son `System.out.println` literal — no hay abstracción.
- No existe inyección de dependencias; todo se crea internamente.

**Por qué es problema:** Es imposible sustituir la persistencia (ej.: base de datos real) o el canal de notificación sin modificar la clase. Tampoco es posible usar mocks para testing unitario.

### 2.3 Code Smells y Antipatrones identificados

| # | Code Smell | Ubicación | Gravedad |
|---|-----------|-----------|----------|
| 1 | **God Class** | `LibrarySystem` (toda la lógica en una clase) | Crítica |
| 2 | **Duplicated Code** | Bucle de búsqueda lineal repetido 5+ veces | Alta |
| 3 | **Magic Numbers** | `14` (días de préstamo), `0.50` (multa diaria), `5` (límite) | Media |
| 4 | **Feature Envy** | `LibrarySystem` manipula estado interno de `Book` y `Loan` | Media |
| 5 | **Long Method** | `processLoan()` (~40 líneas), `processReturn()` (~45 líneas) | Media |
| 6 | **Anemic Domain Model** | `Book`, `Member`, `Loan` son solo contenedores de datos | Media |
| 7 | **Hardcoded Dependencies** | Email = `System.out.println`, sin abstracción | Alta |
| 8 | **Shotgun Surgery** | Cambiar notificaciones requiere tocar `processLoan` Y `processReturn` | Media |
| 9 | **No Error Handling Strategy** | Usa `print + return` en lugar de excepciones | Media |

### 2.4 Clasificación por gravedad

```
CRÍTICA ──► God Class (LibrarySystem con 6+ responsabilidades)
ALTA    ──► Violación DIP (sin abstracciones ni inyección)
ALTA    ──► Violación OCP (no extensible sin modificar)
ALTA    ──► Código duplicado (búsquedas lineales x5)
MEDIA   ──► Magic numbers, Anemic Domain, Feature Envy
MEDIA   ──► Violación LSP, ISP
```

---

## 3. SECCIÓN 1 – REFACTORIZACIÓN

### 3.1 Arquitectura resultante

```
┌─────────────────────────────────────────────────────────────┐
│                    CAPA DE ABSTRACCIÓN                       │
│  NotificationService  FineStrategy  BookRepository          │
│  MemberRepository     LoanRepository                        │
├─────────────────────────────────────────────────────────────┤
│                    CAPA DE DOMINIO                           │
│  Book (markAsLoaned, markAsReturned)                        │
│  Member                                                      │
│  Loan (isOverdue, getDaysOverdue, markAsReturned)           │
├─────────────────────────────────────────────────────────────┤
│                 CAPA DE INFRAESTRUCTURA                      │
│  EmailNotificationService    StandardFineStrategy           │
│  InMemoryBookRepository      InMemoryMemberRepository       │
│  InMemoryLoanRepository                                      │
├─────────────────────────────────────────────────────────────┤
│                    CAPA DE SERVICIO                          │
│  LoanService (préstamos/devoluciones)                       │
│  ReportService (reportes mensuales)                          │
├─────────────────────────────────────────────────────────────┤
│                    ENTRY POINT                               │
│  LibrarySystemRefactored (main – wiring de dependencias)    │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 Patrones de diseño aplicados

#### Patrón Repository

Encapsula el acceso a datos detrás de interfaces (`BookRepository`, `MemberRepository`, `LoanRepository`). Las implementaciones en memoria (`InMemoryBookRepository`, etc.) usan `HashMap` para búsquedas O(1), eliminando los bucles lineales repetidos del código original.

**Interfaces definidas:**

```java
interface BookRepository {
    Optional<Book> findById(String id);
    void save(Book book);
    List<Book> findAll();
}

interface MemberRepository {
    Optional<Member> findById(String id);
    void save(Member member);
    List<Member> findAll();
}

interface LoanRepository {
    void save(Loan loan);
    Optional<Loan> findById(String id);
    long countActiveLoansByMember(String memberId);
    List<Loan> findAll();
    List<Loan> findByMonth(int month, int year);
}
```

#### Patrón Strategy

Se usa en dos ejes:

- **`NotificationService`**: permite intercambiar el canal de notificación (Email, SMS, etc.) sin modificar la lógica de préstamos.
- **`FineStrategy`**: permite intercambiar la política de multas (estándar, progresiva, etc.) sin modificar el servicio.

```java
interface NotificationService {
    void notify(String recipient, String message);
}

interface FineStrategy {
    double calculateFine(long daysOverdue);
}
```

#### Dependency Injection

Todos los servicios reciben sus dependencias por constructor, no las crean internamente. Esto permite sustituir implementaciones fácilmente (ej.: mocks para testing).

```java
class LoanService {
    public LoanService(
            BookRepository bookRepo,
            MemberRepository memberRepo,
            LoanRepository loanRepo,
            NotificationService notificationService,
            FineStrategy fineStrategy) { ... }
}
```

### 3.3 Cambios clave: Antes vs. Después

#### Búsqueda de entidades

```java
// ANTES: búsqueda lineal repetida 5+ veces
Book book = null;
for (Book b : books) {
    if (b.getId().equals(bookId)) {
        book = b;
        break;
    }
}
if (book == null) {
    System.out.println("No encontrado");
    return;
}

// DESPUÉS: Repository con Optional (1 línea, O(1), sin null)
Book book = bookRepo.findById(bookId)
    .orElseThrow(() -> new IllegalArgumentException("Libro no encontrado"));
```

#### Transiciones de estado del dominio

```java
// ANTES: setter público sin semántica de dominio
book.setAvailable(false);
loan.setReturned(true);

// DESPUÉS: métodos con semántica de dominio que protegen invariantes
book.markAsLoaned();
loan.markAsReturned();
```

#### Cálculo de multas

```java
// ANTES: magic number hardcodeado, lógica dispersa
long daysOverdue = ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now());
double fine = daysOverdue * 0.50;  // magic number

// DESPUÉS: Strategy inyectable + lógica encapsulada en Loan
if (loan.isOverdue()) {
    double fine = fineStrategy.calculateFine(loan.getDaysOverdue());
}
```

#### Notificaciones

```java
// ANTES: hardcoded System.out.println
System.out.println("Enviando email a " + member.getEmail() + "...");

// DESPUÉS: Strategy inyectable
notificationService.notify(member.getEmail(),
    "Préstamo confirmado: " + book.getTitle());
```

### 3.4 Corrección de bug crítico

En la versión anterior del archivo refactorizado existía un **bug en el orden de operaciones** dentro del método `returnBook()`:

```java
// BUG: se marcaba como devuelto ANTES de verificar si estaba vencido
loan.returnBook();        // returned = true
book.markAsReturned();

if (loan.isOverdue()) {   // isOverdue() verifica !returned → SIEMPRE false
    double fine = fineStrategy.calculateFine(loan.getDaysOverdue());
    // ¡La multa NUNCA se calculaba!
}
```

**Corrección aplicada:**

```java
// CORRECTO: calcular multa ANTES de marcar como devuelto
double fine = 0;
if (loan.isOverdue()) {
    fine = fineStrategy.calculateFine(loan.getDaysOverdue());
}

// Ahora sí, marcar como devuelto después del cálculo
loan.markAsReturned();
book.markAsReturned();
```

**Explicación:** El método `isOverdue()` de la clase `Loan` verifica la condición `!returned && LocalDate.now().isAfter(dueDate)`. Si `returned` ya es `true`, la expresión siempre evalúa a `false`, impidiendo el cálculo de la multa. La corrección asegura que la verificación de vencimiento ocurra mientras el préstamo aún está marcado como activo.

### 3.5 Funcionalidad restaurada: ReportService

El código original contenía `generateMonthlyReport()` que no estaba presente en la versión anterior del archivo refactorizado. Se creó `ReportService` como clase separada, cumpliendo SRP: los reportes tienen su propia clase independiente de los préstamos.

```java
class ReportService {
    private final LoanRepository loanRepo;
    private final FineStrategy fineStrategy;

    public ReportService(LoanRepository loanRepo, FineStrategy fineStrategy) {
        this.loanRepo = loanRepo;
        this.fineStrategy = fineStrategy;
    }

    public void generateMonthlyReport(int month, int year) {
        List<Loan> monthlyLoans = loanRepo.findByMonth(month, year);

        long totalLoans = monthlyLoans.size();
        long totalReturns = monthlyLoans.stream()
                .filter(Loan::isReturned).count();
        double totalPendingFines = monthlyLoans.stream()
                .filter(l -> !l.isReturned() && l.isOverdue())
                .mapToDouble(l -> fineStrategy.calculateFine(l.getDaysOverdue()))
                .sum();

        System.out.println("=== REPORTE MENSUAL (" + month + "/" + year + ") ===");
        System.out.println("Total de préstamos:     " + totalLoans);
        System.out.println("Total de devoluciones:  " + totalReturns);
        System.out.println("Multas pendientes:      $" + totalPendingFines);
    }
}
```

Para soportar esta funcionalidad, se agregó el método `findByMonth(int month, int year)` a la interfaz `LoanRepository` y su implementación correspondiente en `InMemoryLoanRepository`.

---

## 4. SECCIÓN 1 – TRADE-OFFS

| Aspecto | Beneficio | Costo |
|---------|----------|-------|
| **Más clases e interfaces** | Cada clase tiene responsabilidad única, es testeable y extensible | Mayor número de archivos; mayor complejidad estructural inicial |
| **Repository pattern** | Desacopla persistencia; facilita cambiar a BD real | Overhead para aplicaciones muy simples que nunca cambiarán de almacenamiento |
| **Strategy pattern** | Notificaciones y multas intercambiables sin recompilar | Indirección adicional: seguir el flujo requiere conocer las interfaces |
| **Inyección por constructor** | Testabilidad con mocks; configuración externa | Wiring manual verboso (mitigable con IoC container como Spring) |
| **Excepciones vs. print+return** | Flujo de error explícito; el llamador decide cómo manejar | Requiere que los clientes manejen excepciones (try-catch) |
| **Domain Model rico** | Invariantes protegidas; lógica cohesiva con los datos | Más complejo que un POJO simple; requiere disciplina |
| **ReportService separado** | Modificable sin riesgo a préstamos; testeable aisladamente | Una clase más en el sistema |

**Decisión consciente:** Para un sistema de producción con expectativa de crecimiento (nuevos canales, nuevas políticas de multa, nueva persistencia), los beneficios superan ampliamente los costos. Para un script desechable, el código original monolítico podría ser aceptable.

### Testabilidad mejorada

Con la nueva arquitectura, es posible escribir tests unitarios usando implementaciones alternativas:

```java
// Ejemplo conceptual de test con mock
class MockNotificationService implements NotificationService {
    public String lastRecipient;
    public String lastMessage;

    @Override
    public void notify(String recipient, String message) {
        this.lastRecipient = recipient;
        this.lastMessage = message;
    }
}

// En el test:
MockNotificationService mockNotifier = new MockNotificationService();
LoanService service = new LoanService(bookRepo, memberRepo, loanRepo,
    mockNotifier, new StandardFineStrategy(0.50));
service.borrowBook("M001", "B001");
assert mockNotifier.lastRecipient.equals("edgardo@example.com");
```

Esto es **imposible con el código original** porque las dependencias están hardcodeadas.

---

## 5. SECCIÓN 2 – ARQUITECTURA

### 5.1 Visión general

El Sistema de Notificaciones Multi-Canal se diseña con una arquitectura orientada a interfaces, donde el componente central (`NotificationDispatcher`) coordina el envío delegando a estrategias intercambiables para canales y formatos, y notifica a observers para logging y estadísticas.

### 5.2 Diagrama de componentes

```
┌──────────────────────────────────────────────────────────────────────┐
│                     NotificationDispatcher                            │
│          (coordina envío, formatea, reintenta, notifica observers)    │
└───────┬──────────────────┬───────────────────────┬───────────────────┘
        │                  │                       │
        ▼                  ▼                       ▼
┌───────────────┐  ┌────────────────┐  ┌───────────────────┐
│ «interface»    │  │ «interface»    │  │ «interface»       │
│ Notification   │  │ Message        │  │ Notification      │
│ Channel        │  │ Formatter      │  │ Listener          │
│  +getType()    │  │  +getFormat()  │  │  +onSendAttempt() │
│  +send()       │  │  +format()     │  │                   │
└───────┬────────┘  └───────┬────────┘  └────────┬──────────┘
        │                   │                    │
   ┌────┼────┬────┐   ┌────┼────┬────┐    ┌─────┼─────┐
   │    │    │    │   │    │    │    │    │           │
   ▼    ▼    ▼    ▼   ▼    ▼    ▼    │    ▼           ▼
 Email SMS Push WApp Plain HTML Mark │  Logger    Statistics
 Chan. Chan Chan Chan Text  Fmt  down│  Listener  Collector
                      Fmt       Fmt  │
                                     │
                            ┌────────┴────────┐
                            │  Factory Classes │
                            │  ChannelFactory  │
                            │  FormatterFactory│
                            └─────────────────┘
```

### 5.3 Modelos de dominio

```
Notification
├── id: String (UUID)
├── recipient: String
├── subject: String
├── body: String
├── priority: Priority (LOW | NORMAL | HIGH | URGENT)
├── format: MessageFormat (PLAIN_TEXT | HTML | MARKDOWN)
└── createdAt: LocalDateTime

SendRecord
├── notificationId: String
├── channel: ChannelType (EMAIL | SMS | PUSH | WHATSAPP)
├── priority: Priority
├── success: boolean
├── timestamp: LocalDateTime
└── errorMessage: String
```

### 5.4 Flujo de envío

```
Cliente → NotificationDispatcher.send(notification, channelType)
    │
    ├─ 1. Obtener canal registrado (Map<ChannelType, NotificationChannel>)
    │
    ├─ 2. Crear formateador (MessageFormatterFactory.createFormatter)
    │
    ├─ 3. Formatear cuerpo (formatter.format(body))
    │
    ├─ 4. Enviar por canal (channel.send) [con reintentos si URGENT]
    │
    └─ 5. Notificar listeners (logger.onSendAttempt + stats.onSendAttempt)
```

---

## 6. SECCIÓN 2 – PATRONES Y SOLID

### 6.1 Patrones de diseño seleccionados

#### Patrón 1: Strategy (Canales de notificación + Formateadores)

**Justificación:** Cada canal de notificación (Email, SMS, Push, WhatsApp) tiene un algoritmo de envío diferente. Cada formato de mensaje (Plain Text, HTML, Markdown) transforma el contenido de manera diferente. Strategy permite encapsular cada variante en su propia clase e intercambiarlas sin que el `NotificationDispatcher` conozca los detalles.

**Interfaces definidas:**

- `NotificationChannel` con métodos `getType()` y `send()` — 4 implementaciones
- `MessageFormatter` con métodos `getFormat()` y `format()` — 3 implementaciones

**Beneficio directo:** Agregar un canal Telegram requiere solo:

1. Agregar `TELEGRAM` al enum `ChannelType`
2. Crear `TelegramChannel implements NotificationChannel`
3. Agregar un case en `NotificationChannelFactory`

Ninguna clase existente se modifica (OCP).

#### Patrón 2: Observer (Listeners de eventos de envío)

**Justificación:** El sistema necesita registrar intentos de envío (logging) y recolectar estadísticas, pero estas funcionalidades son ortogonales al envío en sí. Observer desacopla la generación del evento (envío) de su procesamiento (logging, estadísticas).

**Interfaz definida:** `NotificationListener` con método `onSendAttempt(SendRecord)`

**Implementaciones:**

- `NotificationLogger` — imprime logs de cada intento
- `StatisticsCollector` — acumula datos para consultas posteriores

**Beneficio directo:** Agregar un listener que envíe alertas a Slack cuando un envío falla requiere solo crear `SlackAlertListener implements NotificationListener` y registrarlo con `dispatcher.addListener(...)`.

#### Patrón 3: Factory (Creación de canales y formateadores)

**Justificación:** Factory centraliza la lógica de creación, evitando que el código cliente conozca las clases concretas. El cliente solicita por tipo (enum), no por nombre de clase.

**Clases:**

- `NotificationChannelFactory.createChannel(ChannelType)` — crea canales por tipo
- `NotificationChannelFactory.createAllChannels()` — crea todos los canales disponibles
- `MessageFormatterFactory.createFormatter(MessageFormat)` — crea formateadores por formato

### 6.2 Principios SOLID aplicados

| Principio | Dónde se aplica | Cómo se evidencia |
|-----------|----------------|-------------------|
| **S – Single Responsibility** | Cada canal en su clase, logger separado de stats, dispatcher solo coordina | `EmailChannel` solo envía emails; `StatisticsCollector` solo recolecta datos; `NotificationLogger` solo imprime logs |
| **O – Open/Closed** | Interfaces Strategy permiten extensión sin modificación | Nuevo canal = nueva clase. No se toca `NotificationDispatcher` ni ninguna clase existente |
| **L – Liskov Substitution** | Todas las implementaciones de `NotificationChannel` son sustituibles | `EmailChannel` y `WhatsAppChannel` son intercambiables donde se espere `NotificationChannel` |
| **I – Interface Segregation** | Interfaces pequeñas y enfocadas | `NotificationChannel` (2 métodos), `MessageFormatter` (2 métodos), `NotificationListener` (1 método) |
| **D – Dependency Inversion** | `NotificationDispatcher` depende solo de abstracciones | Recibe `NotificationChannel` y `NotificationListener` como interfaces, no como clases concretas |

---

## 7. SECCIÓN 2 – CÓDIGO

El código completo se encuentra en el archivo `NotificationSystem.java`. A continuación se presentan las interfaces principales y las implementaciones más relevantes.

### 7.1 Interfaces principales

```java
// Strategy: Canal de notificación
interface NotificationChannel {
    ChannelType getType();
    boolean send(Notification notification, String formattedBody);
}

// Strategy: Formateador de mensaje
interface MessageFormatter {
    MessageFormat getFormat();
    String format(String content);
}

// Observer: Listener de eventos de envío
interface NotificationListener {
    void onSendAttempt(SendRecord record);
}
```

### 7.2 Implementaciones de canales (Strategy)

```java
class EmailChannel implements NotificationChannel {
    @Override
    public ChannelType getType() { return ChannelType.EMAIL; }

    @Override
    public boolean send(Notification notification, String formattedBody) {
        System.out.println("  [EMAIL] Para: " + notification.getRecipient());
        System.out.println("    Asunto: " + notification.getSubject());
        System.out.println("    Cuerpo: " + formattedBody);
        return true;
    }
}

class SmsChannel implements NotificationChannel {
    private static final int MAX_SMS_LENGTH = 160;

    @Override
    public ChannelType getType() { return ChannelType.SMS; }

    @Override
    public boolean send(Notification notification, String formattedBody) {
        String mensaje = formattedBody.length() > MAX_SMS_LENGTH
                ? formattedBody.substring(0, MAX_SMS_LENGTH) + "..."
                : formattedBody;
        System.out.println("  [SMS] Para: " + notification.getRecipient());
        System.out.println("    Mensaje (" + mensaje.length() + " chars): " + mensaje);
        return true;
    }
}

class PushChannel implements NotificationChannel {
    @Override
    public ChannelType getType() { return ChannelType.PUSH; }

    @Override
    public boolean send(Notification notification, String formattedBody) {
        System.out.println("  [PUSH] Dispositivo: " + notification.getRecipient());
        System.out.println("    Título: " + notification.getSubject());
        System.out.println("    Cuerpo: " + formattedBody);
        return true;
    }
}

class WhatsAppChannel implements NotificationChannel {
    @Override
    public ChannelType getType() { return ChannelType.WHATSAPP; }

    @Override
    public boolean send(Notification notification, String formattedBody) {
        System.out.println("  [WHATSAPP] Para: " + notification.getRecipient());
        System.out.println("    Mensaje: " + formattedBody);
        return true;
    }
}
```

### 7.3 Implementaciones de formateadores (Strategy)

```java
class PlainTextFormatter implements MessageFormatter {
    @Override
    public MessageFormat getFormat() { return MessageFormat.PLAIN_TEXT; }

    @Override
    public String format(String content) {
        return content;
    }
}

class HtmlFormatter implements MessageFormatter {
    @Override
    public MessageFormat getFormat() { return MessageFormat.HTML; }

    @Override
    public String format(String content) {
        return "<html><body><p>" + content + "</p></body></html>";
    }
}

class MarkdownFormatter implements MessageFormatter {
    @Override
    public MessageFormat getFormat() { return MessageFormat.MARKDOWN; }

    @Override
    public String format(String content) {
        return "**Notificación:** " + content;
    }
}
```

### 7.4 Implementaciones de observers

```java
class NotificationLogger implements NotificationListener {
    @Override
    public void onSendAttempt(SendRecord record) {
        String status = record.isSuccess() ? "EXITO" : "FALLO";
        System.out.println("  [LOG] " + record.getTimestamp()
            + " | " + record.getChannel()
            + " | " + record.getPriority()
            + " | " + status
            + (record.getErrorMessage() != null ? " | " + record.getErrorMessage() : ""));
    }
}

class StatisticsCollector implements NotificationListener {
    private final List<SendRecord> records = new ArrayList<>();

    @Override
    public void onSendAttempt(SendRecord record) {
        records.add(record);
    }

    public Map<ChannelType, Long> getSuccessByChannel() {
        return records.stream()
                .filter(SendRecord::isSuccess)
                .collect(Collectors.groupingBy(SendRecord::getChannel, Collectors.counting()));
    }

    public Map<ChannelType, Long> getFailuresByChannel() {
        return records.stream()
                .filter(r -> !r.isSuccess())
                .collect(Collectors.groupingBy(SendRecord::getChannel, Collectors.counting()));
    }

    public long getTotalByChannel(ChannelType type) {
        return records.stream().filter(r -> r.getChannel() == type).count();
    }

    public List<SendRecord> filterByPriority(Priority priority) {
        return records.stream()
                .filter(r -> r.getPriority() == priority)
                .collect(Collectors.toList());
    }

    public void printStatistics() {
        System.out.println("=== ESTADISTICAS DE NOTIFICACIONES ===");
        for (ChannelType type : ChannelType.values()) {
            long total = records.stream()
                    .filter(r -> r.getChannel() == type).count();
            long exitosos = records.stream()
                    .filter(r -> r.getChannel() == type && r.isSuccess()).count();
            long fallidos = total - exitosos;
            if (total > 0) {
                System.out.println("  " + type + ": Total=" + total
                    + " | Exitosos=" + exitosos + " | Fallidos=" + fallidos);
            }
        }
        System.out.println("  TOTAL GENERAL: " + records.size() + " intentos");
        System.out.println("======================================");
    }
}
```

### 7.5 Factory classes

```java
class NotificationChannelFactory {
    public static NotificationChannel createChannel(ChannelType type) {
        switch (type) {
            case EMAIL:    return new EmailChannel();
            case SMS:      return new SmsChannel();
            case PUSH:     return new PushChannel();
            case WHATSAPP: return new WhatsAppChannel();
            default:
                throw new IllegalArgumentException("Canal no soportado: " + type);
        }
    }

    public static Map<ChannelType, NotificationChannel> createAllChannels() {
        Map<ChannelType, NotificationChannel> channels = new HashMap<>();
        for (ChannelType type : ChannelType.values()) {
            channels.put(type, createChannel(type));
        }
        return channels;
    }
}

class MessageFormatterFactory {
    public static MessageFormatter createFormatter(MessageFormat format) {
        switch (format) {
            case PLAIN_TEXT: return new PlainTextFormatter();
            case HTML:       return new HtmlFormatter();
            case MARKDOWN:   return new MarkdownFormatter();
            default:
                throw new IllegalArgumentException("Formato no soportado: " + format);
        }
    }
}
```

### 7.6 NotificationDispatcher (servicio coordinador)

```java
class NotificationDispatcher {
    private final Map<ChannelType, NotificationChannel> channels;
    private final List<NotificationListener> listeners;
    private static final int URGENT_MAX_RETRIES = 3;

    public NotificationDispatcher() {
        this.channels = new HashMap<>();
        this.listeners = new ArrayList<>();
    }

    public void registerChannel(ChannelType type, NotificationChannel channel) {
        channels.put(type, channel);
    }

    public void addListener(NotificationListener listener) {
        listeners.add(listener);
    }

    public void send(Notification notification, ChannelType channelType) {
        NotificationChannel channel = channels.get(channelType);
        if (channel == null) {
            throw new IllegalArgumentException("Canal no registrado: " + channelType);
        }

        // Formatear el mensaje según el formato solicitado (Strategy)
        MessageFormatter formatter =
            MessageFormatterFactory.createFormatter(notification.getFormat());
        String formattedBody = formatter.format(notification.getBody());

        // Determinar número de intentos según prioridad
        int maxAttempts = (notification.getPriority() == Priority.URGENT)
                ? URGENT_MAX_RETRIES : 1;

        boolean success = false;
        String errorMessage = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                success = channel.send(notification, formattedBody);
                if (success) break;
                errorMessage = "Intento " + attempt + " fallido en canal " + channelType;
            } catch (Exception e) {
                errorMessage = "Intento " + attempt + ": " + e.getMessage();
            }
        }

        // Notificar a todos los listeners del resultado (Observer)
        SendRecord record = new SendRecord(
            notification.getId(), channelType,
            notification.getPriority(), success, errorMessage);
        notifyListeners(record);
    }

    public void sendToMultiple(Notification notification, List<ChannelType> types) {
        for (ChannelType type : types) {
            send(notification, type);
        }
    }

    public void sendToAll(Notification notification) {
        sendToMultiple(notification, new ArrayList<>(channels.keySet()));
    }

    private void notifyListeners(SendRecord record) {
        for (NotificationListener listener : listeners) {
            listener.onSendAttempt(record);
        }
    }
}
```

---

## 8. SECCIÓN 2 – EJEMPLO DE USO

El método `main()` en `NotificationSystem.java` demuestra cinco casos de uso completos:

```java
public static void main(String[] args) {

    // ── 1. Configuración del sistema ──
    NotificationDispatcher dispatcher = new NotificationDispatcher();

    // Registrar canales usando Factory
    Map<ChannelType, NotificationChannel> allChannels =
        NotificationChannelFactory.createAllChannels();
    for (Map.Entry<ChannelType, NotificationChannel> entry : allChannels.entrySet()) {
        dispatcher.registerChannel(entry.getKey(), entry.getValue());
    }

    // Registrar observers
    NotificationLogger logger = new NotificationLogger();
    StatisticsCollector stats = new StatisticsCollector();
    dispatcher.addListener(logger);
    dispatcher.addListener(stats);

    // ── 2. Envío a canal específico (Email + HTML) ──
    Notification n1 = new Notification(
        "usuario@ejemplo.com",
        "Bienvenido al sistema",
        "Gracias por registrarte en nuestra plataforma.",
        Priority.NORMAL, MessageFormat.HTML
    );
    dispatcher.send(n1, ChannelType.EMAIL);

    // ── 3. Envío a múltiples canales (Email + SMS) ──
    Notification n2 = new Notification(
        "+507-6000-1234",
        "Código de verificación",
        "Tu código de verificación es: 843291",
        Priority.HIGH, MessageFormat.PLAIN_TEXT
    );
    dispatcher.sendToMultiple(n2, Arrays.asList(ChannelType.EMAIL, ChannelType.SMS));

    // ── 4. Envío URGENT con reintentos (WhatsApp + Markdown) ──
    Notification n3 = new Notification(
        "+507-6999-5678",
        "Alerta de seguridad",
        "Se detectó un inicio de sesión inusual en tu cuenta.",
        Priority.URGENT, MessageFormat.MARKDOWN
    );
    dispatcher.send(n3, ChannelType.WHATSAPP);

    // ── 5. Broadcast a todos los canales ──
    Notification n4 = new Notification(
        "admin@sistema.com",
        "Mantenimiento programado",
        "El sistema estará en mantenimiento el domingo de 2:00 a 6:00 AM.",
        Priority.LOW, MessageFormat.PLAIN_TEXT
    );
    dispatcher.sendToAll(n4);

    // ── 6. Consultar estadísticas ──
    stats.printStatistics();

    // ── 7. Filtrar por prioridad ──
    List<SendRecord> highPriority = stats.filterByPriority(Priority.HIGH);
    System.out.println("Notificaciones HIGH: " + highPriority.size());

    List<SendRecord> urgentRecords = stats.filterByPriority(Priority.URGENT);
    System.out.println("Notificaciones URGENT: " + urgentRecords.size());
}
```

### Salida esperada (resumida)

```
--- CASO 1: Envío a canal específico (Email + HTML) ---
  [EMAIL] Para: usuario@ejemplo.com
    Asunto: Bienvenido al sistema
    Cuerpo: <html><body><p>Gracias por registrarte en nuestra plataforma.</p></body></html>
  [LOG] 2025-... | EMAIL | NORMAL | EXITO

--- CASO 2: Envío a múltiples canales (Email + SMS) ---
  [EMAIL] Para: +507-6000-1234
    Asunto: Código de verificación
    Cuerpo: Tu código de verificación es: 843291
  [LOG] 2025-... | EMAIL | HIGH | EXITO
  [SMS] Para: +507-6000-1234
    Mensaje (36 chars): Tu código de verificación es: 843291
  [LOG] 2025-... | SMS | HIGH | EXITO

--- CASO 3: Envío URGENT (WhatsApp + Markdown) ---
  [WHATSAPP] Para: +507-6999-5678
    Mensaje: **Notificación:** Se detectó un inicio de sesión inusual en tu cuenta.
  [LOG] 2025-... | WHATSAPP | URGENT | EXITO

--- CASO 4: Envío a TODOS los canales (broadcast) ---
  [EMAIL] Para: admin@sistema.com ...
  [SMS] Para: admin@sistema.com ...
  [PUSH] Dispositivo: admin@sistema.com ...
  [WHATSAPP] Para: admin@sistema.com ...

=== ESTADISTICAS DE NOTIFICACIONES ===
  EMAIL: Total=3 | Exitosos=3 | Fallidos=0
  SMS: Total=2 | Exitosos=2 | Fallidos=0
  PUSH: Total=1 | Exitosos=1 | Fallidos=0
  WHATSAPP: Total=2 | Exitosos=2 | Fallidos=0
  TOTAL GENERAL: 8 intentos
======================================
```

---

## 9. SECCIÓN 2 – TRADE-OFFS

| Aspecto | Beneficio | Costo |
|---------|----------|-------|
| **Strategy para canales** | Extensibilidad total: nuevo canal = nueva clase | Indirección: hay que rastrear la interfaz para entender qué canal se ejecuta |
| **Strategy para formatos** | Formatos desacoplados de canales y dispatcher | Cada combinación canal+formato funciona sin validación de compatibilidad (ej.: SMS + HTML podría no tener sentido visual) |
| **Observer para logging/stats** | Desacoplamiento total: agregar/quitar observers sin tocar el dispatcher | Overhead de notificar observers en cada envío; para alto volumen, considerar procesamiento asíncrono |
| **Factory para creación** | Centraliza creación; el cliente no conoce clases concretas | El `switch` en Factory requiere modificación al agregar tipos (mitigable con registro dinámico o reflexión) |
| **Reintentos solo para URGENT** | Implementación sencilla y clara del comportamiento por prioridad | Prioridades LOW/NORMAL no implementan cola real; en producción se necesitaría un sistema de colas (RabbitMQ, Kafka) |
| **Registro en memoria** | Simple y funcional para demostración y testing | No persiste entre ejecuciones; en producción se necesitaría BD o sistema de métricas |
| **Enums para tipos** | Type-safety en tiempo de compilación | Agregar un nuevo tipo requiere modificar el enum (trade-off aceptable por la seguridad de tipos) |

### Decisiones de diseño conscientes

1. **Strategy sobre Template Method:** Se eligió Strategy porque los canales no comparten un algoritmo base; cada uno es completamente independiente. Template Method sería apropiado si todos los canales siguieran un flujo común con pasos intercambiables.

2. **Observer sobre eventos con cola:** Se eligió Observer síncrono porque el requisito no exige asincronía. En un sistema de producción con alto volumen, se consideraría un bus de eventos asíncrono.

3. **Factory Method sobre Abstract Factory:** Se eligió Factory Method simple porque solo hay una familia de productos por categoría (canales o formateadores). Abstract Factory sería necesario si existieran familias relacionadas de objetos.

4. **Map para canales registrados:** Se usa `Map<ChannelType, NotificationChannel>` en lugar de `List<NotificationChannel>` para búsqueda O(1) por tipo y para prevenir registros duplicados del mismo tipo de canal.

---

## 10. CONCLUSIÓN TÉCNICA

### Sección 1 – Refactorización

La refactorización transformó una clase monolítica de ~150 líneas con 6+ responsabilidades en una arquitectura de 5 capas con 12 clases/interfaces, cada una con una responsabilidad clara. Los cinco principios SOLID se aplican de la siguiente manera:

- **SRP**: `LoanService` (préstamos), `ReportService` (reportes), `EmailNotificationService` (notificación), `StandardFineStrategy` (multas) — cada clase tiene exactamente una razón para cambiar.
- **OCP**: Nuevos canales, estrategias de multa o repositorios se agregan sin modificar código existente — solo se implementa la interfaz correspondiente.
- **LSP**: Todas las implementaciones son sustituibles por su interfaz sin alterar el comportamiento del sistema.
- **ISP**: Interfaces con 2-3 métodos cada una, sin forzar dependencias innecesarias en los clientes.
- **DIP**: `LoanService` y `ReportService` dependen exclusivamente de abstracciones inyectadas por constructor.

Se corrigió un **bug crítico** donde la multa nunca se calculaba por orden incorrecto de operaciones, y se restauró la funcionalidad de **reporte mensual** que estaba ausente en la versión previa del archivo refactorizado.

### Sección 2 – Diseño desde cero

El Sistema de Notificaciones Multi-Canal demuestra cómo un diseño orientado a interfaces y patrones permite construir software extensible desde su concepción. Con 3 patrones de diseño (Strategy, Observer, Factory) y los 5 principios SOLID aplicados explícitamente, el sistema cumple todos los requisitos funcionales:

- 4 canales de notificación (Email, SMS, Push, WhatsApp)
- 3 formatos de mensaje (Texto plano, HTML, Markdown)
- 4 niveles de prioridad (LOW, NORMAL, HIGH, URGENT)
- Envío individual, múltiple y broadcast
- Logging y estadísticas desacoplados mediante Observer
- Reintentos automáticos para notificaciones urgentes
- Filtrado por prioridad y estadísticas por canal

**Extensibilidad verificable:** Agregar un quinto canal (ej.: Telegram) requiere exactamente **3 adiciones** (entrada en enum, clase que implemente `NotificationChannel`, case en factory) y **0 modificaciones** a código existente, cumpliendo el principio Open/Closed de manera demostrable.

Ambas secciones ilustran que la inversión inicial en una arquitectura limpia y bien estructurada genera retornos significativos en mantenibilidad, testabilidad y capacidad de evolución del software.
