/**
 * Ejercicio 8 - Patron de Diseno: Decorator (Decorador)
 *
 * Sistema de personalizacion de personajes para un juego de rol.
 * Los jugadores pueden equipar a sus personajes con diferentes objetos y encantamientos.
 * Cada objeto afecta las estadisticas del personaje (ataque, defensa, velocidad).
 *
 * Personajes base: Warrior, Mage, Rogue
 * Decoradores: LegendarySword, DragonScaleArmor, SwiftBoots, MagicAmulet, InvisibilityCloak
 */

// Clase de estadisticas
class Stats {
    private int attack;
    private int defense;
    private int speed;

    public Stats(int attack, int defense, int speed) {
        this.attack = attack;
        this.defense = defense;
        this.speed = speed;
    }

    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public int getSpeed() { return speed; }

    public void addAttack(int value) { this.attack += value; }
    public void addDefense(int value) { this.defense += value; }
    public void addSpeed(int value) { this.speed += value; }

    @Override
    public String toString() {
        return String.format("ATK: %d, DEF: %d, SPD: %d", attack, defense, speed);
    }
}

// Interfaz base del componente
interface GameCharacter {
    Stats getStats();
    String getDescription();
}

// ===================== Componentes Concretos (Personajes Base) =====================

class Warrior implements GameCharacter {
    @Override
    public Stats getStats() {
        return new Stats(20, 15, 10);
    }

    @Override
    public String getDescription() {
        return "Warrior";
    }
}

class Mage implements GameCharacter {
    @Override
    public Stats getStats() {
        return new Stats(15, 10, 12);
    }

    @Override
    public String getDescription() {
        return "Mage";
    }
}

class Rogue implements GameCharacter {
    @Override
    public Stats getStats() {
        return new Stats(18, 8, 20);
    }

    @Override
    public String getDescription() {
        return "Rogue";
    }
}

// ===================== Decorador Abstracto =====================

abstract class CharacterDecorator implements GameCharacter {
    protected GameCharacter character;

    public CharacterDecorator(GameCharacter character) {
        this.character = character;
    }
}

// ===================== Decoradores Concretos (Equipo) =====================

// Espada Legendaria: +15 ataque, -2 velocidad
class LegendarySword extends CharacterDecorator {
    public LegendarySword(GameCharacter character) {
        super(character);
    }

    @Override
    public Stats getStats() {
        Stats stats = character.getStats();
        stats.addAttack(15);
        stats.addSpeed(-2);
        return stats;
    }

    @Override
    public String getDescription() {
        return character.getDescription() + " + Legendary Sword";
    }
}

// Armadura de Escamas de Dragon: +20 defensa, -5 velocidad
class DragonScaleArmor extends CharacterDecorator {
    public DragonScaleArmor(GameCharacter character) {
        super(character);
    }

    @Override
    public Stats getStats() {
        Stats stats = character.getStats();
        stats.addDefense(20);
        stats.addSpeed(-5);
        return stats;
    }

    @Override
    public String getDescription() {
        return character.getDescription() + " + Dragon Scale Armor";
    }
}

// Botas Veloces: +8 velocidad
class SwiftBoots extends CharacterDecorator {
    public SwiftBoots(GameCharacter character) {
        super(character);
    }

    @Override
    public Stats getStats() {
        Stats stats = character.getStats();
        stats.addSpeed(8);
        return stats;
    }

    @Override
    public String getDescription() {
        return character.getDescription() + " + Swift Boots";
    }
}

// Amuleto Magico: +10 ataque, +5 defensa
class MagicAmulet extends CharacterDecorator {
    public MagicAmulet(GameCharacter character) {
        super(character);
    }

    @Override
    public Stats getStats() {
        Stats stats = character.getStats();
        stats.addAttack(10);
        stats.addDefense(5);
        return stats;
    }

    @Override
    public String getDescription() {
        return character.getDescription() + " + Magic Amulet";
    }
}

// Capa Invisible: +5 defensa, +10 velocidad
class InvisibilityCloak extends CharacterDecorator {
    public InvisibilityCloak(GameCharacter character) {
        super(character);
    }

    @Override
    public Stats getStats() {
        Stats stats = character.getStats();
        stats.addDefense(5);
        stats.addSpeed(10);
        return stats;
    }

    @Override
    public String getDescription() {
        return character.getDescription() + " + Invisibility Cloak";
    }
}

// ===================== Clase de Prueba =====================

public class DecoratorPattern {
    public static void printCharacter(GameCharacter character) {
        System.out.println("Character: " + character.getDescription());
        System.out.println("Stats: " + character.getStats());
        System.out.println();
    }

    public static void main(String[] args) {
        // Guerrero base
        System.out.println("=== Base Characters ===");
        GameCharacter warrior = new Warrior();
        printCharacter(warrior);

        GameCharacter mage = new Mage();
        printCharacter(mage);

        GameCharacter rogue = new Rogue();
        printCharacter(rogue);

        // Guerrero con Espada Legendaria y Armadura de Dragon
        System.out.println("=== Equipped Characters ===");
        GameCharacter equippedWarrior = new DragonScaleArmor(new LegendarySword(new Warrior()));
        printCharacter(equippedWarrior);
        // Warrior: ATK=20+15=35, DEF=15+20=35, SPD=10-2-5=3

        // Mago con Amuleto Magico y Capa Invisible
        GameCharacter equippedMage = new InvisibilityCloak(new MagicAmulet(new Mage()));
        printCharacter(equippedMage);
        // Mage: ATK=15+10=25, DEF=10+5+5=20, SPD=12+10=22

        // Picaro con Botas Veloces, Espada Legendaria y Capa Invisible
        GameCharacter equippedRogue = new InvisibilityCloak(new LegendarySword(new SwiftBoots(new Rogue())));
        printCharacter(equippedRogue);
        // Rogue: ATK=18+15=33, DEF=8+5=13, SPD=20+8-2+10=36
    }
}
