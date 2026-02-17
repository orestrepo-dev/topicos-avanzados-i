# Asignatura #1 - Patrones, Antipatrones y Principios de Diseno
## Universidad Tecnologica de Panama - Maestria en Ingenieria de Software
## Verano 2026

---

## Pregunta 1 - Complejidades de Software de Fred Brooks (10 puntos)

Fred Brooks, en su ensayo clasico "No Silver Bullet", define dos tipos de complejidad en el desarrollo de software:

**Complejidad Esencial (Essential Complexity):**
Es la complejidad inherente al problema que estamos tratando de resolver. Esta complejidad existe independientemente de las herramientas, lenguajes o frameworks que utilicemos. Por ejemplo, si estamos construyendo un sistema bancario, las reglas de negocio sobre tasas de interes, regulaciones financieras y transacciones concurrentes son complejidad esencial: no podemos simplificarlas porque son parte fundamental del dominio del problema. No importa que tan buena sea nuestra tecnologia, esta complejidad no desaparece porque es la naturaleza misma del problema.

**Complejidad Accidental (Accidental Complexity):**
Es la complejidad que nosotros mismos introducimos al resolver el problema, generada por las herramientas, tecnologias, decisiones de diseno o limitaciones tecnicas que elegimos. Ejemplos incluyen: configuraciones excesivas de frameworks, incompatibilidades entre librerias, codigo boilerplate innecesario, o malas decisiones arquitectonicas. A diferencia de la complejidad esencial, esta si puede (y debe) ser reducida mediante mejores herramientas, mejores practicas de diseno y refactorizacion constante. Brooks argumenta que la mayor parte del progreso en ingenieria de software ha sido en reducir la complejidad accidental, no la esencial.

---

## Pregunta 2 - Atributos de una buena arquitectura (4 puntos)

Los cuatro atributos principales de una buena arquitectura de software son:

1. **Escalabilidad:** La capacidad del sistema para manejar un aumento en la carga de trabajo (mas usuarios, mas datos, mas transacciones) sin degradar el rendimiento, ya sea escalando verticalmente u horizontalmente.

2. **Mantenibilidad:** La facilidad con la que el sistema puede ser modificado para corregir errores, mejorar funcionalidades existentes o adaptarse a nuevos requerimientos sin introducir efectos secundarios no deseados.

3. **Desempeno (Performance):** La capacidad del sistema para responder de manera eficiente y rapida a las solicitudes de los usuarios, optimizando el uso de recursos como CPU, memoria y red.

4. **Seguridad:** La capacidad del sistema para proteger los datos y funcionalidades contra accesos no autorizados, ataques maliciosos y perdida de informacion, garantizando confidencialidad, integridad y disponibilidad.

---

## Pregunta 3 - Restricciones de negocio y tecnicas (4 puntos)

**Restricciones de negocio:**
1. **Presupuesto del proyecto:** El limite financiero disponible para el desarrollo, que afecta directamente las decisiones sobre equipo, herramientas y alcance del proyecto.
2. **Tiempo establecido para el proyecto (Time-to-market):** La fecha limite en la que el software debe estar listo para produccion, influenciada por compromisos comerciales, contratos o ventanas de oportunidad del mercado.

**Restricciones tecnicas:**
1. **Lenguaje de programacion del proyecto:** La eleccion del lenguaje puede estar dictada por el equipo existente, la infraestructura disponible o requisitos de compatibilidad con sistemas legados.
2. **Herramientas de gestion del proyecto:** Las plataformas para comunicacion (Slack, Teams), gestion de tareas (Jira, Trello), control de versiones (Git) y CI/CD que el equipo debe utilizar segun estandares organizacionales.

---

## Pregunta 4 - Sintomas de complejidad segun John Ousterhout (6 puntos)

Segun John Ousterhout en "A Philosophy of Software Design", los tres sintomas principales de complejidad son:

1. **Amplificacion de cambio (Change Amplification):** Ocurre cuando un cambio conceptualmente simple requiere modificar codigo en muchos lugares diferentes del sistema. Esto indica que hay un alto acoplamiento entre componentes y que la logica esta dispersa en lugar de estar centralizada. Por ejemplo, si cambiar el formato de una fecha requiere modificar 20 archivos distintos, es una senal clara de amplificacion de cambio.

2. **Carga Cognitiva (Cognitive Load):** Es la cantidad de conocimiento y contexto que un desarrollador necesita tener en mente para completar una tarea en el sistema. Un buen diseno minimiza esta carga haciendo que las interfaces sean simples e intuitivas. Cuando un desarrollador necesita leer miles de lineas de codigo o entender multiples subsistemas para hacer un cambio pequeno, la carga cognitiva es demasiado alta.

3. **Desconocidos desconocidos (Unknown Unknowns):** Es el sintoma mas peligroso. Ocurre cuando no es evidente que piezas del codigo necesitan ser modificadas para completar una tarea, o cuando existen dependencias ocultas que no son obvias al leer el codigo. Esto provoca que los desarrolladores pierdan tiempo significativo buscando donde estan las afectaciones y, peor aun, pueden introducir bugs sin darse cuenta. Un buen diseno hace que el sistema sea predecible y "obvio", de modo que sea claro que se debe modificar para cada cambio.

---

## Pregunta 5 - Deteccion de Antipatrones y Code Smells

### 5.1 - Antipatron: Copy-Paste Programming / Codigo Duplicado (10 puntos)

**Antipatron detectado:** Copy-Paste Programming (Programacion de Copiar y Pegar), tambien conocido como el code smell "Duplicated Code".

**Problema:** Los metodos `processRegularStudent()` y `processExchangeStudent()` son practicamente identicos. Comparten la misma logica de validacion, generacion de ID y formateo de datos. La unica diferencia es el tipo de estudiante en el mensaje. Esto viola el principio DRY (Don't Repeat Yourself) y causa amplificacion de cambio: cualquier correccion en la logica de validacion o generacion de ID requiere modificar ambos metodos.

**Solucion:** Extraer la logica comun en un metodo privado parametrizado, y usar el tipo de estudiante como parametro diferenciador.

Ver archivo: `Ejercicio5_1/StudentManagementSystem.java`

---

### 5.2 - Antipatron: Data Clumps (10 puntos)

**Antipatron detectado:** Data Clumps (Grupos de Datos).

**Problema:** Los campos de direccion (streetAddress, city, state, zipCode, country) aparecen juntos tanto en la clase `Order` como en `Customer`, incluyendo el metodo `updateShippingAddress()` que es identico en ambas clases. Cuando varios datos siempre viajan juntos, es una senal de que deberian encapsularse en su propia clase.

**Solucion:** Crear una clase `Address` que encapsule todos los campos de direccion. Tanto `Order` como `Customer` tendran una referencia a un objeto `Address`, eliminando la duplicacion y mejorando la cohesion.

Ver archivo: `Ejercicio5_2/DataClumpsSolution.java`

---

### 5.3 - Antipatron: Anemic Domain Model (10 puntos)

**Antipatron detectado:** Anemic Domain Model (Modelo de Dominio Anemico) / Feature Envy.

**Problema:** La clase `Email` es un simple contenedor de datos sin comportamiento significativo (solo un getter). La logica de validacion que deberia pertenecer a `Email` esta separada en una clase externa `EmailValidator`. Esto representa un modelo de dominio anemico porque la clase `Email` no encapsula su propio comportamiento, y la clase `EmailValidator` tiene "envidia" de los datos de `Email` (Feature Envy).

**Solucion:** Mover la logica de validacion dentro de la clase `Email`, haciendo que se auto-valide en el constructor. De esta forma, un objeto `Email` siempre sera valido desde su creacion, aplicando el principio de encapsulamiento.

Ver archivo: `Ejercicio5_3/Email.java`

---

## Pregunta 6 - Principios SOLID

### 6.1 - Principios: OCP y DIP (Open/Closed + Dependency Inversion) (10 puntos)

**Principios SOLID que se pueden aplicar:**

- **OCP (Open/Closed Principle - Principio Abierto/Cerrado):** El `PaymentProcessor` usa una cadena de if-else para determinar que gateway usar. Para agregar un nuevo metodo de pago, es necesario modificar la clase `PaymentProcessor`, violando el principio de que las clases deben estar abiertas para extension pero cerradas para modificacion.

- **DIP (Dependency Inversion Principle - Principio de Inversion de Dependencias):** El `PaymentProcessor` depende directamente de las implementaciones concretas `StripePaymentGateway` y `PayPalPaymentGateway` en lugar de depender de la abstraccion `PaymentGateway`. Ademas, las clases gateway no implementan correctamente el metodo de la interfaz (tienen sus propios nombres de metodo).

**Solucion:** Hacer que los gateways implementen correctamente la interfaz `PaymentGateway`, y que `PaymentProcessor` reciba la dependencia via inyeccion (constructor) usando la abstraccion.

Ver archivo: `Ejercicio6_1/Solid1.java`

---

### 6.2 - Principio: ISP (Interface Segregation) (10 puntos)

**Principio SOLID que se puede aplicar:** ISP (Interface Segregation Principle - Principio de Segregacion de Interfaces).

**Problema:** La interfaz `Document` es demasiado "gorda" (fat interface): obliga a las clases que la implementan a implementar metodos que no soportan (fax, staple, encrypt, sign), lo que resulta en metodos que lanzan `UnsupportedOperationException`. Esto viola el ISP que dice que ningun cliente deberia estar obligado a depender de metodos que no utiliza.

**Solucion:** Dividir la interfaz grande en interfaces mas pequenas y cohesivas, para que cada implementacion solo necesite implementar los metodos que realmente soporta.

Ver archivo: `Ejercicio6_2/Solid2.java`

---

## Pregunta 7 - Patrones de Diseno Estructurales y Creacionales (6 puntos)

**Patrones Estructurales (Structural Design Patterns):**
1. **Adapter:** Permite que interfaces incompatibles trabajen juntas, actuando como un puente entre dos interfaces.
2. **Decorator:** Permite agregar funcionalidad adicional a un objeto de forma dinamica, sin modificar su estructura.
3. **Facade:** Proporciona una interfaz simplificada a un conjunto complejo de subsistemas.

**Patrones Creacionales (Creational Design Patterns):**
1. **Singleton:** Garantiza que una clase tenga una unica instancia y proporciona un punto de acceso global a ella.
2. **Factory Method:** Define una interfaz para crear objetos, pero deja que las subclases decidan que clase instanciar.
3. **Builder:** Permite construir objetos complejos paso a paso, separando la construccion de la representacion.

---

## Pregunta 8 - Patron Decorator: Sistema de Personajes RPG (10 puntos)

**Patron aplicado:** Decorator (Decorador).

Se implementan las tres clases de personajes base (Warrior, Mage, Rogue) y cinco decoradores de equipo que modifican las estadisticas del personaje de forma dinamica y apilable.

Ver archivo: `Ejercicio8/DecoratorPattern.java`

---

## Pregunta 9 - Patron Flyweight: Editor de Texto (10 puntos)

**Patron aplicado:** Flyweight.

Se implementan `CharacterFormattingFactory` (la fabrica que reutiliza instancias de formato) y `FormattedCharacter` (que combina un caracter con su formato compartido). La fabrica usa un `HashMap` para cachear y reutilizar los objetos de formato, de modo que millones de caracteres con el mismo formato comparten una sola instancia del objeto de formato.

Ver archivo: `Ejercicio9/DesignPatternFlyweight.java`
