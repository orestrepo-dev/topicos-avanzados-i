import java.util.*;
import java.time.LocalDateTime;
import java.util.stream.Collectors;


// ══════════════════════════════════════════════════════════════════════════════
// SECCIÓN 2 – SISTEMA DE NOTIFICACIONES MULTI-CANAL (DISEÑO DESDE CERO)
//
// Requisitos implementados:
//   - 4 canales: Email, SMS, Push, WhatsApp (simulado)
//   - 4 niveles de prioridad: LOW, NORMAL, HIGH, URGENT
//   - 3 formatos: Texto plano, HTML, Markdown
//   - Envío a canal específico y a múltiples canales
//   - Registro de intentos de envío (éxito/fallo)
//   - Estadísticas por canal
//   - Filtro por prioridad
//   - Reintentos automáticos para prioridad URGENT
//
// Principios SOLID aplicados (5 de 5):
//   S – Cada clase tiene una responsabilidad única
//   O – Nuevos canales/formatos se agregan sin modificar código existente
//   L – Todas las implementaciones son sustituibles por su abstracción
//   I – Interfaces pequeñas y enfocadas (NotificationChannel, MessageFormatter)
//   D – El servicio depende de abstracciones, no de implementaciones concretas
//
// Patrones de diseño aplicados (3):
//   1. Strategy   → canales de notificación y formateadores de mensaje
//   2. Observer    → listeners para registro de eventos de envío
//   3. Factory     → creación de canales y formateadores por tipo
//
// Diagrama de componentes:
//
//   ┌──────────────────────────────────────────────────────────────────┐
//   │                    NotificationDispatcher                        │
//   │              (coordina envío, aplica formato,                    │
//   │               maneja reintentos, notifica listeners)             │
//   └──────┬──────────────┬───────────────────┬───────────────────────┘
//          │              │                   │
//          ▼              ▼                   ▼
//   ┌─────────────┐ ┌───────────────┐ ┌─────────────────┐
//   │ «interface»  │ │ «interface»   │ │ «interface»     │
//   │ Notification │ │ Message       │ │ Notification    │
//   │ Channel      │ │ Formatter     │ │ Listener        │
//   └──────┬───────┘ └──────┬────────┘ └────────┬────────┘
//          │                │                   │
//    ┌─────┼─────┐    ┌─────┼─────┐       ┌─────┼─────┐
//    │     │     │    │     │     │       │           │
//    ▼     ▼     ▼    ▼     ▼     ▼       ▼           ▼
//  Email SMS  Push  Plain HTML  Mark   Logger    Statistics
//  Chan  Chan Chan  Text  Fmt   down   Listener  Collector
//  nel   nel  nel   Fmt         Fmt
//  + WhatsApp
//
//   ┌─────────────────────────────────────────────┐
//   │           Factory Classes                    │
//   │  NotificationChannelFactory                  │
//   │  MessageFormatterFactory                     │
//   └─────────────────────────────────────────────┘
//
// ══════════════════════════════════════════════════════════════════════════════


// ┌─────────────────────────────────────────────────────────────────────┐
// │  1. ENUMERACIONES                                                   │
// │  Definen los tipos válidos del sistema de forma type-safe           │
// └─────────────────────────────────────────────────────────────────────┘


/**
 * Niveles de prioridad para las notificaciones.
 * Determina el comportamiento de envío:
 *   LOW    → puede enviarse con retraso (simulado: se encola)
 *   NORMAL → se envía en el siguiente batch (simulado: envío normal)
 *   HIGH   → se envía inmediatamente
 *   URGENT → se envía inmediatamente con reintentos si falla
 */
enum Priority {
    LOW, NORMAL, HIGH, URGENT
}

/**
 * Tipos de canal de notificación soportados.
 * OCP: agregar un nuevo canal implica agregar una entrada aquí
 * y crear la clase que implemente NotificationChannel.
 */
enum ChannelType {
    EMAIL, SMS, PUSH, WHATSAPP
}

/**
 * Formatos de mensaje soportados.
 * OCP: agregar un nuevo formato implica agregar una entrada aquí
 * y crear la clase que implemente MessageFormatter.
 */
enum MessageFormat {
    PLAIN_TEXT, HTML, MARKDOWN
}


// ┌─────────────────────────────────────────────────────────────────────┐
// │  2. MODELO DE DOMINIO                                               │
// │  Notification: mensaje a enviar                                     │
// │  SendRecord: registro de un intento de envío                        │
// └─────────────────────────────────────────────────────────────────────┘


/**
 * Modelo inmutable que representa una notificación a enviar.
 * Contiene toda la información necesaria para el envío:
 * destinatario, contenido, prioridad y formato.
 */
class Notification {
    private final String id;
    private final String recipient;
    private final String subject;
    private final String body;
    private final Priority priority;
    private final MessageFormat format;
    private final LocalDateTime createdAt;

    public Notification(String recipient, String subject, String body,
                        Priority priority, MessageFormat format) {
        this.id = UUID.randomUUID().toString();
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.priority = priority;
        this.format = format;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public Priority getPriority() { return priority; }
    public MessageFormat getFormat() { return format; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public String toString() {
        return "[" + priority + "] " + subject + " -> " + recipient;
    }
}

/**
 * Registro inmutable de un intento de envío.
 * Almacena el resultado (éxito/fallo) junto con metadatos.
 * Usado por los observers (NotificationListener) para logging y estadísticas.
 */
class SendRecord {
    private final String notificationId;
    private final ChannelType channel;
    private final Priority priority;
    private final boolean success;
    private final LocalDateTime timestamp;
    private final String errorMessage;

    public SendRecord(String notificationId, ChannelType channel,
                      Priority priority, boolean success, String errorMessage) {
        this.notificationId = notificationId;
        this.channel = channel;
        this.priority = priority;
        this.success = success;
        this.timestamp = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    public String getNotificationId() { return notificationId; }
    public ChannelType getChannel() { return channel; }
    public Priority getPriority() { return priority; }
    public boolean isSuccess() { return success; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getErrorMessage() { return errorMessage; }

    @Override
    public String toString() {
        String status = success ? "OK" : "FAIL";
        return timestamp + " | " + channel + " | " + status
            + (errorMessage != null ? " | " + errorMessage : "");
    }
}


// ┌─────────────────────────────────────────────────────────────────────┐
// │  3. PATRÓN STRATEGY: CANALES DE NOTIFICACIÓN                       │
// │                                                                     │
// │  Justificación:                                                     │
// │  Cada canal tiene un algoritmo de envío diferente (Email vs SMS     │
// │  vs Push vs WhatsApp). Strategy permite intercambiar la             │
// │  implementación de envío sin que el servicio coordinador (Dispatch- │
// │  er) conozca los detalles de cada canal.                            │
// │                                                                     │
// │  Principios SOLID:                                                  │
// │  - OCP: nuevo canal = nueva clase, sin modificar las existentes     │
// │  - LSP: cualquier NotificationChannel es sustituible               │
// │  - ISP: interfaz mínima con solo getType() y send()                │
// │  - DIP: Dispatcher depende de la abstracción NotificationChannel   │
// └─────────────────────────────────────────────────────────────────────┘


/**
 * Interfaz Strategy para canales de notificación.
 * Cada implementación encapsula la lógica de envío específica del canal.
 *
 * ISP: solo dos métodos, ambos esenciales para cualquier canal.
 */
interface NotificationChannel {
    /** Identifica el tipo de canal. */
    ChannelType getType();

    /**
     * Envía una notificación con el cuerpo ya formateado.
     * @return true si el envío fue exitoso, false si falló
     */
    boolean send(Notification notification, String formattedBody);
}

/**
 * Canal de Email: envía notificaciones por correo electrónico.
 * Simulación: imprime en consola los datos del correo.
 */
class EmailChannel implements NotificationChannel {
    @Override
    public ChannelType getType() { return ChannelType.EMAIL; }

    @Override
    public boolean send(Notification notification, String formattedBody) {
        System.out.println("  [EMAIL] Para: " + notification.getRecipient());
        System.out.println("    Asunto: " + notification.getSubject());
        System.out.println("    Cuerpo: " + formattedBody);
        return true; // Simulación: siempre exitoso
    }
}

/**
 * Canal de SMS: envía mensajes de texto con truncamiento a 160 caracteres.
 * Simulación: imprime en consola el mensaje truncado.
 */
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

/**
 * Canal Push: envía notificaciones push a dispositivos móviles.
 * Simulación: imprime título y cuerpo.
 */
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

/**
 * Canal WhatsApp: envía mensajes por WhatsApp (simulado).
 * Simulación: imprime en consola con formato de WhatsApp.
 */
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


// ┌─────────────────────────────────────────────────────────────────────┐
// │  4. PATRÓN STRATEGY: FORMATEADORES DE MENSAJE                      │
// │                                                                     │
// │  Justificación:                                                     │
// │  Cada formato de mensaje transforma el contenido de manera          │
// │  diferente. Strategy permite agregar nuevos formatos (JSON, XML)    │
// │  sin modificar el dispatcher ni los canales.                        │
// │                                                                     │
// │  Principios SOLID:                                                  │
// │  - SRP: cada formateador solo se encarga de un formato              │
// │  - OCP: nuevo formato = nueva clase, sin modificar existentes       │
// │  - ISP: interfaz con solo getFormat() y format()                    │
// └─────────────────────────────────────────────────────────────────────┘


/**
 * Interfaz Strategy para formateadores de mensaje.
 */
interface MessageFormatter {
    /** Identifica el tipo de formato. */
    MessageFormat getFormat();

    /** Transforma el contenido al formato correspondiente. */
    String format(String content);
}

/**
 * Formateador de texto plano: retorna el contenido sin modificación.
 */
class PlainTextFormatter implements MessageFormatter {
    @Override
    public MessageFormat getFormat() { return MessageFormat.PLAIN_TEXT; }

    @Override
    public String format(String content) {
        return content;
    }
}

/**
 * Formateador HTML: envuelve el contenido en estructura HTML básica.
 */
class HtmlFormatter implements MessageFormatter {
    @Override
    public MessageFormat getFormat() { return MessageFormat.HTML; }

    @Override
    public String format(String content) {
        return "<html><body><p>" + content + "</p></body></html>";
    }
}

/**
 * Formateador Markdown: aplica énfasis Markdown al contenido.
 */
class MarkdownFormatter implements MessageFormatter {
    @Override
    public MessageFormat getFormat() { return MessageFormat.MARKDOWN; }

    @Override
    public String format(String content) {
        return "**Notificación:** " + content;
    }
}


// ┌─────────────────────────────────────────────────────────────────────┐
// │  5. PATRÓN OBSERVER: LISTENERS DE EVENTOS DE ENVÍO                 │
// │                                                                     │
// │  Justificación:                                                     │
// │  El sistema necesita registrar intentos de envío y recolectar       │
// │  estadísticas. Observer desacopla estas funcionalidades del         │
// │  proceso de envío: el dispatcher no conoce qué listeners existen.  │
// │                                                                     │
// │  Principios SOLID:                                                  │
// │  - SRP: cada listener tiene una responsabilidad (logging o stats)   │
// │  - OCP: nuevos listeners se agregan sin modificar el dispatcher    │
// │  - DIP: dispatcher depende de la abstracción NotificationListener  │
// └─────────────────────────────────────────────────────────────────────┘


/**
 * Interfaz Observer para eventos de envío de notificaciones.
 * Las implementaciones reaccionan ante cada intento de envío.
 */
interface NotificationListener {
    void onSendAttempt(SendRecord record);
}

/**
 * Observer concreto: Logger de notificaciones.
 * SRP: solo se encarga de imprimir logs de cada intento de envío.
 */
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

/**
 * Observer concreto: Recolector de estadísticas.
 * SRP: solo se encarga de acumular datos estadísticos.
 *
 * Provee:
 *   - Conteo de éxitos y fallos por canal
 *   - Total de envíos por canal
 *   - Filtrado de registros por prioridad
 *   - Impresión de reporte estadístico
 */
class StatisticsCollector implements NotificationListener {
    private final List<SendRecord> records = new ArrayList<>();

    @Override
    public void onSendAttempt(SendRecord record) {
        records.add(record);
    }

    /** Obtiene conteo de envíos exitosos agrupados por canal. */
    public Map<ChannelType, Long> getSuccessByChannel() {
        return records.stream()
                .filter(SendRecord::isSuccess)
                .collect(Collectors.groupingBy(SendRecord::getChannel, Collectors.counting()));
    }

    /** Obtiene conteo de envíos fallidos agrupados por canal. */
    public Map<ChannelType, Long> getFailuresByChannel() {
        return records.stream()
                .filter(r -> !r.isSuccess())
                .collect(Collectors.groupingBy(SendRecord::getChannel, Collectors.counting()));
    }

    /** Obtiene total de intentos para un canal específico. */
    public long getTotalByChannel(ChannelType type) {
        return records.stream()
                .filter(r -> r.getChannel() == type)
                .count();
    }

    /** Filtra registros de envío por prioridad. */
    public List<SendRecord> filterByPriority(Priority priority) {
        return records.stream()
                .filter(r -> r.getPriority() == priority)
                .collect(Collectors.toList());
    }

    /** Obtiene todos los registros (vista inmutable). */
    public List<SendRecord> getAllRecords() {
        return Collections.unmodifiableList(records);
    }

    /** Imprime reporte estadístico por canal. */
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


// ┌─────────────────────────────────────────────────────────────────────┐
// │  6. PATRÓN FACTORY: CREACIÓN DE CANALES Y FORMATEADORES            │
// │                                                                     │
// │  Justificación:                                                     │
// │  Factory centraliza la creación de objetos, evitando que el         │
// │  cliente conozca las clases concretas. Facilita agregar nuevos      │
// │  tipos: solo se agrega un case en el switch y la clase concreta.   │
// │                                                                     │
// │  Principio DIP: el cliente solicita por tipo (enum), no por clase. │
// └─────────────────────────────────────────────────────────────────────┘


/**
 * Factory para crear canales de notificación por tipo.
 */
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

    /** Crea todos los canales disponibles. */
    public static Map<ChannelType, NotificationChannel> createAllChannels() {
        Map<ChannelType, NotificationChannel> channels = new HashMap<>();
        for (ChannelType type : ChannelType.values()) {
            channels.put(type, createChannel(type));
        }
        return channels;
    }
}

/**
 * Factory para crear formateadores de mensaje por formato.
 */
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


// ┌─────────────────────────────────────────────────────────────────────┐
// │  7. SERVICIO COORDINADOR (DISPATCHER)                               │
// │                                                                     │
// │  SRP: solo coordina el flujo de envío (formatear → enviar →        │
// │       notificar listeners). No conoce detalles de canales,         │
// │       formatos ni logging.                                          │
// │  DIP: depende de abstracciones (NotificationChannel, Message-      │
// │       Formatter, NotificationListener), no de implementaciones.    │
// │  OCP: agregar un canal/formato/listener no requiere modificar      │
// │       esta clase.                                                   │
// └─────────────────────────────────────────────────────────────────────┘


/**
 * Servicio principal que coordina el envío de notificaciones.
 *
 * Responsabilidades:
 *   1. Aplicar el formateador correspondiente al formato del mensaje
 *   2. Delegar el envío al canal seleccionado
 *   3. Implementar lógica de reintentos para prioridad URGENT
 *   4. Notificar a los listeners (Observer) del resultado
 *
 * Este servicio NO:
 *   - Conoce cómo se envía por cada canal (delegado a Strategy)
 *   - Conoce cómo se formatea cada mensaje (delegado a Strategy)
 *   - Registra logs ni estadísticas directamente (delegado a Observer)
 */
class NotificationDispatcher {
    private final Map<ChannelType, NotificationChannel> channels;
    private final List<NotificationListener> listeners;

    // Número máximo de reintentos para notificaciones URGENT
    private static final int URGENT_MAX_RETRIES = 3;

    public NotificationDispatcher() {
        this.channels = new HashMap<>();
        this.listeners = new ArrayList<>();
    }

    /** Registra un canal de notificación. */
    public void registerChannel(ChannelType type, NotificationChannel channel) {
        channels.put(type, channel);
    }

    /** Agrega un listener (Observer) para eventos de envío. */
    public void addListener(NotificationListener listener) {
        listeners.add(listener);
    }

    /**
     * Envía una notificación a un canal específico.
     *
     * Flujo:
     *   1. Obtener el canal registrado
     *   2. Crear el formateador según el formato de la notificación
     *   3. Formatear el cuerpo del mensaje
     *   4. Enviar (con reintentos si es URGENT)
     *   5. Registrar el resultado vía observers
     */
    public void send(Notification notification, ChannelType channelType) {
        NotificationChannel channel = channels.get(channelType);
        if (channel == null) {
            throw new IllegalArgumentException(
                "Canal no registrado: " + channelType
                + ". Registre el canal antes de enviar.");
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

        // Intentar envío (con reintentos para URGENT)
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                success = channel.send(notification, formattedBody);
                if (success) break;
                errorMessage = "Intento " + attempt + " fallido en canal " + channelType;
            } catch (Exception e) {
                errorMessage = "Intento " + attempt + ": " + e.getMessage();
            }

            // Si no es el último intento, esperar antes de reintentar
            if (!success && attempt < maxAttempts) {
                System.out.println("  [REINTENTO] Intento " + (attempt + 1)
                    + " de " + maxAttempts + " para " + channelType);
            }
        }

        // Notificar a todos los listeners del resultado (Observer)
        SendRecord record = new SendRecord(
            notification.getId(), channelType,
            notification.getPriority(), success, errorMessage);
        notifyListeners(record);
    }

    /**
     * Envía una notificación a múltiples canales.
     * Cada canal recibe la misma notificación formateada.
     */
    public void sendToMultiple(Notification notification, List<ChannelType> channelTypes) {
        for (ChannelType type : channelTypes) {
            send(notification, type);
        }
    }

    /**
     * Envía una notificación a todos los canales registrados.
     */
    public void sendToAll(Notification notification) {
        sendToMultiple(notification, new ArrayList<>(channels.keySet()));
    }

    /** Notifica a todos los listeners registrados (Observer pattern). */
    private void notifyListeners(SendRecord record) {
        for (NotificationListener listener : listeners) {
            listener.onSendAttempt(record);
        }
    }
}


// ┌─────────────────────────────────────────────────────────────────────┐
// │  8. CLASE PRINCIPAL – EJEMPLO DE USO Y DEMOSTRACIÓN                │
// └─────────────────────────────────────────────────────────────────────┘


public class NotificationSystem {
    public static void main(String[] args) {

        System.out.println("==========================================================");
        System.out.println("  SISTEMA DE NOTIFICACIONES MULTI-CANAL – DEMOSTRACIÓN");
        System.out.println("==========================================================\n");

        // ── 1. Configuración del sistema ──
        // Crear el dispatcher (servicio coordinador)
        NotificationDispatcher dispatcher = new NotificationDispatcher();

        // Registrar canales usando Factory (DIP: no se instancian concretos aquí)
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

        // ── 2. Caso de uso: Envío a un canal específico (Email, formato HTML) ──
        System.out.println("--- CASO 1: Envío a canal específico (Email + HTML) ---");
        Notification n1 = new Notification(
            "usuario@ejemplo.com",
            "Bienvenido al sistema",
            "Gracias por registrarte en nuestra plataforma.",
            Priority.NORMAL,
            MessageFormat.HTML
        );
        dispatcher.send(n1, ChannelType.EMAIL);
        System.out.println();

        // ── 3. Caso de uso: Envío a múltiples canales (Email + SMS) ──
        System.out.println("--- CASO 2: Envío a múltiples canales (Email + SMS) ---");
        Notification n2 = new Notification(
            "+507-6000-1234",
            "Código de verificación",
            "Tu código de verificación es: 843291",
            Priority.HIGH,
            MessageFormat.PLAIN_TEXT
        );
        dispatcher.sendToMultiple(n2, Arrays.asList(ChannelType.EMAIL, ChannelType.SMS));
        System.out.println();

        // ── 4. Caso de uso: Envío urgente con reintentos (WhatsApp) ──
        System.out.println("--- CASO 3: Envío URGENT (WhatsApp + Markdown) ---");
        Notification n3 = new Notification(
            "+507-6999-5678",
            "Alerta de seguridad",
            "Se detectó un inicio de sesión inusual en tu cuenta.",
            Priority.URGENT,
            MessageFormat.MARKDOWN
        );
        dispatcher.send(n3, ChannelType.WHATSAPP);
        System.out.println();

        // ── 5. Caso de uso: Envío a TODOS los canales ──
        System.out.println("--- CASO 4: Envío a TODOS los canales (broadcast) ---");
        Notification n4 = new Notification(
            "admin@sistema.com",
            "Mantenimiento programado",
            "El sistema estará en mantenimiento el domingo de 2:00 a 6:00 AM.",
            Priority.LOW,
            MessageFormat.PLAIN_TEXT
        );
        dispatcher.sendToAll(n4);
        System.out.println();

        // ── 6. Caso de uso: Envío Push con prioridad baja ──
        System.out.println("--- CASO 5: Envío Push (LOW + Texto plano) ---");
        Notification n5 = new Notification(
            "device-token-abc123",
            "Nueva promoción",
            "Aprovecha 20% de descuento en tu próxima compra.",
            Priority.LOW,
            MessageFormat.PLAIN_TEXT
        );
        dispatcher.send(n5, ChannelType.PUSH);
        System.out.println();

        // ── 7. Estadísticas por canal ──
        System.out.println("--- ESTADÍSTICAS GENERALES ---");
        stats.printStatistics();
        System.out.println();

        // ── 8. Filtrado por prioridad ──
        System.out.println("--- FILTRO: Notificaciones con prioridad HIGH ---");
        List<SendRecord> highPriority = stats.filterByPriority(Priority.HIGH);
        for (SendRecord record : highPriority) {
            System.out.println("  " + record);
        }
        System.out.println();

        System.out.println("--- FILTRO: Notificaciones con prioridad URGENT ---");
        List<SendRecord> urgentRecords = stats.filterByPriority(Priority.URGENT);
        for (SendRecord record : urgentRecords) {
            System.out.println("  " + record);
        }
        System.out.println();

        // ── 9. Estadísticas detalladas por canal ──
        System.out.println("--- DETALLE POR CANAL ---");
        Map<ChannelType, Long> exitosos = stats.getSuccessByChannel();
        for (Map.Entry<ChannelType, Long> entry : exitosos.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " envíos exitosos");
        }

        System.out.println("\n==========================================================");
        System.out.println("  DEMOSTRACIÓN FINALIZADA");
        System.out.println("==========================================================");
    }
}
