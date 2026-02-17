/**
 * Ejercicio 5.2 - Solucion al antipatron: Data Clumps (Grupos de Datos)
 *
 * Problema original: Los campos de direccion (streetAddress, city, state, zipCode, country)
 * aparecian duplicados en Order y Customer, junto con el metodo updateShippingAddress().
 *
 * Solucion: Crear una clase Address que encapsula los campos de direccion.
 * Order y Customer ahora tienen una referencia a Address, eliminando la duplicacion.
 */

// Clase Address que encapsula los datos de direccion
class Address {
    private String streetAddress;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    public Address(String streetAddress, String city, String state, String zipCode, String country) {
        this.streetAddress = streetAddress;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.country = country;
    }

    // Getters
    public String getStreetAddress() { return streetAddress; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getZipCode() { return zipCode; }
    public String getCountry() { return country; }

    // Setters
    public void setStreetAddress(String streetAddress) { this.streetAddress = streetAddress; }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }
    public void setCountry(String country) { this.country = country; }

    @Override
    public String toString() {
        return streetAddress + ", " + city + ", " + state + " " + zipCode + ", " + country;
    }
}

class Order {
    private Address shippingAddress;
    private int quantity;
    private double price;

    public Order(Address shippingAddress, int quantity, double price) {
        this.shippingAddress = shippingAddress;
        this.quantity = quantity;
        this.price = price;
    }

    public void updateShippingAddress(Address newAddress) {
        this.shippingAddress = newAddress;
    }

    public Address getShippingAddress() { return shippingAddress; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
}

class Customer {
    private String name;
    private String email;
    private Address address;

    public Customer(String name, String email, Address address) {
        this.name = name;
        this.email = email;
        this.address = address;
    }

    public void updateAddress(Address newAddress) {
        this.address = newAddress;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public Address getAddress() { return address; }
}

// Clase de prueba
public class DataClumpsSolution {
    public static void main(String[] args) {
        Address customerAddress = new Address("123 Main St", "Springfield", "IL", "62701", "USA");

        Customer customer = new Customer("John Doe", "john@email.com", customerAddress);
        System.out.println("Customer: " + customer.getName());
        System.out.println("Address: " + customer.getAddress());

        Order order = new Order(customerAddress, 2, 49.99);
        System.out.println("\nOrder shipping to: " + order.getShippingAddress());

        // Actualizar la direccion de envio
        Address newAddress = new Address("456 Oak Ave", "Chicago", "IL", "60601", "USA");
        order.updateShippingAddress(newAddress);
        System.out.println("Updated shipping: " + order.getShippingAddress());
    }
}
