/**
 * Ejercicio 5.3 - Solucion al antipatron: Anemic Domain Model / Feature Envy
 *
 * Problema original: La clase Email era un simple contenedor de datos (solo un getter)
 * y la logica de validacion estaba separada en EmailValidator. La clase Email no
 * encapsulaba su propio comportamiento.
 *
 * Solucion: Mover la validacion dentro de la clase Email. Un Email se auto-valida
 * en el constructor, garantizando que toda instancia de Email sea siempre valida.
 * Se elimina la clase EmailValidator ya que su logica ahora pertenece a Email.
 */
class Email {
    private final String email;

    public Email(String email) {
        if (!isValid(email)) {
            throw new IllegalArgumentException("Invalid email: " + email);
        }
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    private static boolean isValid(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }

    @Override
    public String toString() {
        return email;
    }

    // Clase de prueba
    public static void main(String[] args) {
        // Email valido
        Email validEmail = new Email("usuario@correo.com");
        System.out.println("Email valido creado: " + validEmail);

        // Email invalido - lanza excepcion
        try {
            Email invalidEmail = new Email("correo-invalido");
        } catch (IllegalArgumentException e) {
            System.out.println("Error esperado: " + e.getMessage());
        }

        // Email nulo - lanza excepcion
        try {
            Email nullEmail = new Email(null);
        } catch (IllegalArgumentException e) {
            System.out.println("Error esperado: " + e.getMessage());
        }
    }
}
