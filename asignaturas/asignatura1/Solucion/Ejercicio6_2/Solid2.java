/**
 * Ejercicio 6.2 - Principio SOLID aplicado: ISP (Interface Segregation Principle)
 *
 * Problema del codigo original:
 * La interfaz Document es demasiado grande ("fat interface") con 7 metodos.
 * ProfessionalPrinter se ve obligada a implementar metodos que no soporta
 * (fax, staple, encrypt, sign), lanzando UnsupportedOperationException.
 * Esto viola el ISP: ningun cliente debe depender de metodos que no usa.
 *
 * Solucion:
 * Dividir la interfaz Document en interfaces mas pequenas y cohesivas.
 * Cada dispositivo implementa solo las interfaces de las capacidades que soporta.
 */

// Interfaces segregadas - cada una representa una capacidad especifica
interface Scannable {
    void scan();
}

interface Printable {
    void print();
}

interface Faxable {
    void fax();
}

interface Photocopiable {
    void photocopy();
}

interface Stapleable {
    void staple();
}

interface Encryptable {
    void encrypt();
}

interface Signable {
    void sign();
}

// ProfessionalPrinter solo implementa las interfaces de lo que realmente soporta
class ProfessionalPrinter implements Scannable, Printable, Photocopiable {
    @Override
    public void scan() {
        System.out.println("Scanning document at 300 DPI");
    }

    @Override
    public void print() {
        System.out.println("Printing document in high quality");
    }

    @Override
    public void photocopy() {
        System.out.println("Making a photocopy");
    }
}

// Una impresora multifuncion de oficina podria implementar mas interfaces
class OfficePrinter implements Scannable, Printable, Faxable, Photocopiable, Stapleable {
    @Override
    public void scan() {
        System.out.println("Scanning document at 600 DPI");
    }

    @Override
    public void print() {
        System.out.println("Printing document");
    }

    @Override
    public void fax() {
        System.out.println("Sending fax");
    }

    @Override
    public void photocopy() {
        System.out.println("Making a photocopy");
    }

    @Override
    public void staple() {
        System.out.println("Stapling document");
    }
}

// Clase de prueba
public class Solid2 {
    public static void main(String[] args) {
        System.out.println("=== Professional Printer ===");
        ProfessionalPrinter professional = new ProfessionalPrinter();
        professional.scan();
        professional.print();
        professional.photocopy();

        System.out.println("\n=== Office Printer ===");
        OfficePrinter office = new OfficePrinter();
        office.scan();
        office.print();
        office.fax();
        office.photocopy();
        office.staple();
    }
}
