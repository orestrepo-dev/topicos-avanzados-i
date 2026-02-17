/**
 * Ejercicio 5.1 - Solucion al antipatron: Copy-Paste Programming / Codigo Duplicado
 *
 * Problema original: Los metodos processRegularStudent() y processExchangeStudent()
 * contenian logica de validacion, generacion de ID y formateo practicamente identica.
 *
 * Solucion: Extraer la logica comun en un metodo privado parametrizado (processStudent),
 * eliminando la duplicacion y respetando el principio DRY.
 */
public class StudentManagementSystem {

    private boolean validateStudentData(String name, int age, String course) {
        if (name == null || name.trim().isEmpty()) {
            System.out.println("Error: Name cannot be empty");
            return false;
        }
        if (age < 16 || age > 99) {
            System.out.println("Error: Invalid age");
            return false;
        }
        if (course == null || course.trim().isEmpty()) {
            System.out.println("Error: Course cannot be empty");
            return false;
        }
        return true;
    }

    private String generateStudentId(String name, int age, String course) {
        return name.substring(0, 3).toUpperCase() + age + course.substring(0, 2).toUpperCase();
    }

    private void processStudent(String name, int age, String course, String studentType) {
        if (!validateStudentData(name, age, course)) {
            return;
        }

        String studentId = generateStudentId(name, age, course);

        String studentInfo = String.format("%s Student - ID: %s\nName: %s\nAge: %d\nCourse: %s",
                studentType, studentId, name, age, course);

        System.out.println("Saving to database: " + studentInfo);
        System.out.println(studentType + " student processed successfully");
    }

    public void processRegularStudent(String name, int age, String course) {
        processStudent(name, age, course, "Regular");
    }

    public void processExchangeStudent(String name, int age, String course) {
        processStudent(name, age, course, "Exchange");
    }

    public static void main(String[] args) {
        StudentManagementSystem system = new StudentManagementSystem();

        // Test with regular student
        system.processRegularStudent("John Smith", 20, "Computer Science");

        System.out.println();

        // Test with exchange student
        system.processExchangeStudent("Maria Garcia", 22, "Physics");
    }
}
