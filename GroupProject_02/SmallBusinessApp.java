import javax.swing.JOptionPane;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

class Order {
    private String customerName;
    private String productName;
    private double price;
    private int quantity;

    public Order(String customerName, String productName, double price, int
quantity) {
        this.customerName = customerName;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
    }

    public double calculateTotal() {
        return price * quantity;
    }

    public double calculateDiscount() {
        double subtotal = calculateTotal();
        if (subtotal >= 100) {
            return subtotal * 0.10;
        } else {
            return 0;
        }

    }

    public double calculateFinalTotal() {
        return calculateTotal() - calculateDiscount();

    }

    public String getSummary() {
        return "Customer:" + customerName
                + "\nProduct:" + productName
                + "\nPrice: $" + String.format("%.2f", price)
                + "\nQuantity:" + quantity
                + "\nSubtotal: $" + String.format("%.2f", calculateTotal())
                + "\nDiscount: $" + String.format("%.2f", calculateDiscount())
                + "\nTotal: $" + String.format("%.2f", calculateFinalTotal());

    }
    public String toCSV() throws IOException {
        return SmallBusinessApp.csvField(customerName) + ","
                + SmallBusinessApp.csvField(productName) + "," + price + "," + quantity;
    }
}

public class SmallBusinessApp {

    public static void main(String[] args) {

        try {
            
            readPeople();
            List<Order> orders = readOrders();
            if (!orders.isEmpty()) {
                StringBuilder previous = new StringBuilder();
                for (Order savedOrder : orders) {
                    previous.append(savedOrder.getSummary()).append("\n\n");
                }
                JOptionPane.showMessageDialog(null, previous.toString(),
                        "Saved Orders", JOptionPane.INFORMATION_MESSAGE);
            }
            String customerName = JOptionPane.showInputDialog(
                    null,
                    "Enter customer name:");

            String productName = JOptionPane.showInputDialog(
                    null,
                    "Enter product name:");
            String priceInput = JOptionPane.showInputDialog(
                    null,
                    "Enter price:");

            double price = Double.parseDouble(priceInput);

            String quantityInput = JOptionPane.showInputDialog(
                    null,
                    "Enter quantity:");
            int quantity = Integer.parseInt(quantityInput);
            Order order = new Order(customerName,
                    productName,
                    price,
                    quantity);

            
            writeOrder(order);

            JOptionPane.showMessageDialog(
                    null,
                    order.getSummary(),
                    "Order Summary",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            // Ousayd Jawabreh - AI-assisted addition: report file errors.
            JOptionPane.showMessageDialog(null, e.getMessage(),
                    "File Error", JOptionPane.ERROR_MESSAGE);
        } catch (NumberFormatException e) {

            JOptionPane.showMessageDialog(
                    null,
                    "Please enter valid numbers for price and quantity.",
                    "Input Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    
    public static void readPeople() throws IOException {
        Path file = Path.of("people.csv");
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).equals("name,position")) {
            throw new IOException("people.csv must start with name,position.");
        }
        StringBuilder people = new StringBuilder();
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) continue;
            List<String> fields = parseCSV(lines.get(i));
            if (fields.size() != 2 || fields.get(0).isBlank() || fields.get(1).isBlank()) {
                throw new IOException("Invalid person at row " + (i + 1));
            }
            people.append(fields.get(0)).append(" - ").append(fields.get(1)).append("\n");
        }
        if (people.length() == 0) {
            throw new IOException("Add actual names and positions to people.csv, then run again.");
        }
        JOptionPane.showMessageDialog(null, people.toString(),
                "People and Positions", JOptionPane.INFORMATION_MESSAGE);
    }

    
    public static List<Order> readOrders() throws IOException {
        List<Order> orders = new ArrayList<>();
        Path file = Path.of("orders.csv");
        if (!Files.exists(file)) return orders;
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).equals("customerName,productName,price,quantity")) {
            throw new IOException("Invalid orders.csv header.");
        }
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) continue;
            List<String> fields = parseCSV(lines.get(i));
            if (fields.size() != 4) throw new IOException("Invalid order at row " + (i + 1));
            try {
                orders.add(new Order(fields.get(0), fields.get(1),
                        Double.parseDouble(fields.get(2)), Integer.parseInt(fields.get(3))));
            } catch (NumberFormatException e) {
                throw new IOException("Invalid number in orders.csv row " + (i + 1), e);
            }
        }
        return orders;
    }

    public static void writeOrder(Order order) throws IOException {
        Path file = Path.of("orders.csv");
        String row = order.toCSV();
        if (!Files.exists(file) || Files.size(file) == 0) {
            Files.writeString(file, "customerName,productName,price,quantity\n",
                    StandardCharsets.UTF_8);
        }
        Files.writeString(file, row + "\n", StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    public static String csvField(String value) throws IOException {
        if (value == null || value.contains("\n") || value.contains("\r")) {
            throw new IOException("Order not saved: enter names on one line and do not cancel the form.");
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    public static List<String> parseCSV(String line) throws IOException {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean closed = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                        closed = true;
                    }
                } else field.append(c);
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
                closed = false;
            } else if (c == '"' && field.length() == 0 && !closed) {
                quoted = true;
            } else {
                if (closed || c == '"') throw new IOException("Malformed CSV row.");
                field.append(c);
            }
        }
        if (quoted) throw new IOException("Unclosed quote in CSV row.");
        fields.add(field.toString());
        return fields;
    }
}

