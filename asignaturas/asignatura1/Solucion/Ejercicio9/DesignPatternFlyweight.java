import java.util.*;

/**
 * Ejercicio 9 - Patron de Diseno: Flyweight
 *
 * Editor de texto que maneja documentos grandes (millones de caracteres).
 * Cada caracter puede tener diferentes propiedades de formato (fuente, tamano, color, estilo).
 *
 * El patron Flyweight permite compartir instancias de formato entre caracteres que
 * tienen el mismo formato, reduciendo drasticamente el uso de memoria.
 *
 * Clases implementadas:
 * - CharacterFormattingFactory: Fabrica Flyweight que cachea y reutiliza formatos.
 * - FormattedCharacter: Combina un caracter (estado extrinseco) con su formato compartido (estado intrinseco).
 */

// Interfaz Flyweight
interface CharacterFormat {
    void applyFormat(char character, int position);
}

// Flyweight Concreto - estado intrinseco (compartido)
class CharacterFormatting implements CharacterFormat {
    private final String font;
    private final int size;
    private final String color;
    private final String style;

    public CharacterFormatting(String font, int size, String color, String style) {
        this.font = font;
        this.size = size;
        this.color = color;
        this.style = style;
    }

    @Override
    public void applyFormat(char character, int position) {
        System.out.printf("Caracter '%c' en posicion %d con formato: %s, %dpt, %s, %s%n",
                character, position, font, size, color, style);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharacterFormatting that = (CharacterFormatting) o;
        return size == that.size &&
                font.equals(that.font) &&
                color.equals(that.color) &&
                style.equals(that.style);
    }

    @Override
    public int hashCode() {
        return Objects.hash(font, size, color, style);
    }

    @Override
    public String toString() {
        return String.format("[%s, %dpt, %s, %s]", font, size, color, style);
    }
}

// Fabrica Flyweight - cachea y reutiliza instancias de formato
class CharacterFormattingFactory {
    private final Map<String, CharacterFormat> cache = new HashMap<>();

    public CharacterFormat getFormatting(String font, int size, String color, String style) {
        String key = font + "-" + size + "-" + color + "-" + style;

        if (!cache.containsKey(key)) {
            cache.put(key, new CharacterFormatting(font, size, color, style));
        }

        return cache.get(key);
    }

    public int getCacheSize() {
        return cache.size();
    }
}

// Clase que combina el caracter (extrinseco) con su formato compartido (intrinseco)
class FormattedCharacter {
    private final char character;
    private final CharacterFormat format;
    private final int position;

    public FormattedCharacter(char character, CharacterFormat format, int position) {
        this.character = character;
        this.format = format;
        this.position = position;
    }

    public void display() {
        format.applyFormat(character, position);
    }

    public char getCharacter() { return character; }
    public CharacterFormat getFormat() { return format; }
    public int getPosition() { return position; }
}

// Documento que usa el patron Flyweight
class Document {
    private List<FormattedCharacter> characters = new ArrayList<>();
    private CharacterFormattingFactory factory;

    public Document(CharacterFormattingFactory factory) {
        this.factory = factory;
    }

    public void insertText(String text, String font, int size, String color, String style) {
        CharacterFormat format = factory.getFormatting(font, size, color, style);
        int position = characters.size();

        for (char c : text.toCharArray()) {
            characters.add(new FormattedCharacter(c, format, position++));
        }
    }

    public void display() {
        for (FormattedCharacter fc : characters) {
            fc.display();
        }
    }

    public int getCharacterCount() {
        return characters.size();
    }
}

// Clase de prueba
public class DesignPatternFlyweight {
    public static void main(String[] args) {
        CharacterFormattingFactory factory = new CharacterFormattingFactory();
        Document doc = new Document(factory);

        // Caso de prueba 1: Formato basico
        doc.insertText("Hello ", "Arial", 12, "Blue", "Bold");
        doc.insertText("World", "Times New Roman", 14, "Red", "Italic");

        System.out.println("Contenido del documento:");
        doc.display();

        System.out.println("\nObjetos de formato unicos creados: " + factory.getCacheSize());
        // Salida esperada: 2 (no 11, demostrando el ahorro de memoria)

        System.out.println("Total de caracteres en el documento: " + doc.getCharacterCount());
        // 11 caracteres pero solo 2 objetos de formato

        // Caso de prueba 2: Reutilizacion de formatos
        System.out.println("\n--- Caso de prueba 2: Reutilizacion ---");
        doc.insertText(" - More Arial text", "Arial", 12, "Blue", "Bold");

        System.out.println("Objetos de formato unicos despues de mas texto: " + factory.getCacheSize());
        // Sigue siendo 2, ya que "Arial, 12, Blue, Bold" ya existia en cache

        // Caso de prueba 3: Nuevo formato
        doc.insertText("!", "Courier", 16, "Green", "Normal");
        System.out.println("Objetos de formato unicos despues de nuevo formato: " + factory.getCacheSize());
        // Ahora es 3
    }
}
