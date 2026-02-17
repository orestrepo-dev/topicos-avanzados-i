/**
 * Ejercicio 6.1 - Principios SOLID aplicados: OCP (Open/Closed) y DIP (Dependency Inversion)
 *
 * Problemas del codigo original:
 * 1. OCP violado: PaymentProcessor usa if-else para cada metodo de pago.
 *    Agregar un nuevo gateway requiere modificar PaymentProcessor.
 * 2. DIP violado: PaymentProcessor depende de clases concretas (StripePaymentGateway,
 *    PayPalPaymentGateway) en lugar de depender de la abstraccion PaymentGateway.
 * 3. Las clases gateway no implementan correctamente la interfaz (usan nombres propios).
 *
 * Solucion:
 * - Los gateways implementan correctamente la interfaz PaymentGateway con processPayment().
 * - PaymentProcessor recibe un PaymentGateway por inyeccion de dependencias (constructor).
 * - Para agregar un nuevo metodo de pago, solo se crea una nueva clase que implemente
 *   PaymentGateway, sin modificar PaymentProcessor (OCP cumplido).
 */

class PaymentDetails {
    private final double amount;
    private final String currency;
    private final String cardNumber;
    private final String cvv;

    public PaymentDetails(double amount, String currency, String cardNumber, String cvv) {
        this.amount = amount;
        this.currency = currency;
        this.cardNumber = cardNumber;
        this.cvv = cvv;
    }

    public double getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getCardNumber() { return cardNumber; }
    public String getCvv() { return cvv; }
}

// Interfaz correctamente definida
interface PaymentGateway {
    boolean processPayment(PaymentDetails details);
}

// Cada gateway implementa la interfaz correctamente
class StripePaymentGateway implements PaymentGateway {
    @Override
    public boolean processPayment(PaymentDetails details) {
        System.out.println("Processing payment through Stripe");
        System.out.println("Amount: " + details.getAmount() + " " + details.getCurrency());
        return true;
    }
}

class PayPalPaymentGateway implements PaymentGateway {
    @Override
    public boolean processPayment(PaymentDetails details) {
        System.out.println("Processing payment through PayPal");
        System.out.println("Amount: " + details.getAmount() + " " + details.getCurrency());
        return true;
    }
}

// PaymentProcessor ahora depende de la abstraccion (DIP) y es abierto a extension (OCP)
class PaymentProcessor {
    private final PaymentGateway gateway;

    // Inyeccion de dependencias via constructor
    public PaymentProcessor(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    public boolean processOrder(PaymentDetails details) {
        return gateway.processPayment(details);
    }
}

// Ejemplo de uso
public class Solid1 {
    public static void main(String[] args) {
        PaymentDetails stripePayment = new PaymentDetails(99.99, "USD", "4111111111111111", "123");
        PaymentDetails paypalPayment = new PaymentDetails(149.99, "EUR", "4111111111111111", "123");

        // Procesar con Stripe
        PaymentProcessor stripeProcessor = new PaymentProcessor(new StripePaymentGateway());
        stripeProcessor.processOrder(stripePayment);

        System.out.println();

        // Procesar con PayPal
        PaymentProcessor paypalProcessor = new PaymentProcessor(new PayPalPaymentGateway());
        paypalProcessor.processOrder(paypalPayment);

        // Para agregar un nuevo metodo de pago (ej. Bitcoin), solo se crea una nueva clase:
        // class BitcoinPaymentGateway implements PaymentGateway { ... }
        // No se necesita modificar PaymentProcessor (OCP cumplido)
    }
}
