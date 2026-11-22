import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class StudentManagementSystem {
    private final Scanner scanner = new Scanner(System.in);
    private final Map<String, Student> students = new LinkedHashMap<String, Student>();
    private final Path dataFile = Paths.get("students.csv");

    public static void main(String[] args) {
        StudentManagementSystem app = new StudentManagementSystem();
        try {
            app.loadStudents();
        } catch (IOException e) {
            System.err.println("Could not load " + app.dataFile + ": " + e.getMessage());
            return;
        }
        app.run();
    }

    private void run() {
        while (true) {
            System.out.println();
            System.out.println("===== STUDENT MANAGEMENT =====");
            System.out.println("1. Add Student");
            System.out.println("2. View Students");
            System.out.println("3. Search Student");
            System.out.println("4. Update Student");
            System.out.println("5. Delete Student");
            System.out.println("6. Exit");
            System.out.print("Enter your choice: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1":
                    addStudent();
                    break;
                case "2":
                    viewStudents();
                    break;
                case "3":
                    searchStudent();
                    break;
                case "4":
                    updateStudent();
                    break;
                case "5":
                    deleteStudent();
                    break;
                case "6":
                    System.out.println("Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice. Please enter a number from 1 to 6.");
            }
        }
    }

    private void addStudent() {
        String id = readRequired("Enter ID: ");
        if (students.containsKey(id)) {
            System.out.println("A student with that ID already exists.");
            return;
        }

        String name = readRequired("Enter Name: ");
        String branch = readRequired("Enter Branch: ");
        Student student = new Student(id, name, branch);

        if (saveChange(() -> students.put(id, student))) {
            System.out.println("Student Added Successfully.");
        }
    }

    private void viewStudents() {
        if (students.isEmpty()) {
            System.out.println("No students found.");
            return;
        }

        System.out.println();
        System.out.println("ID\tName\t\tBranch");
        System.out.println("----------------------------------------");
        for (Student student : students.values()) {
            System.out.println(student.getId() + "\t" + student.getName() + "\t\t" + student.getBranch());
        }
    }

    private void searchStudent() {
        String query = readRequired("Enter ID, name, or branch to search: ");
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        boolean found = false;

        for (Student student : students.values()) {
            if (student.getId().equalsIgnoreCase(query)
                    || student.getName().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                    || student.getBranch().toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                printStudent(student);
                found = true;
            }
        }

        if (!found) {
            System.out.println("No matching student found.");
        }
    }

    private void updateStudent() {
        String id = readRequired("Enter the ID of the student to update: ");
        Student current = students.get(id);
        if (current == null) {
            System.out.println("Student not found.");
            return;
        }

        System.out.print("Enter Name [" + current.getName() + "]: ");
        String name = scanner.nextLine().trim();
        System.out.print("Enter Branch [" + current.getBranch() + "]: ");
        String branch = scanner.nextLine().trim();

        Student updated = new Student(
                id,
                name.isEmpty() ? current.getName() : name,
                branch.isEmpty() ? current.getBranch() : branch);

        if (saveChange(() -> students.put(id, updated))) {
            System.out.println("Student Updated Successfully.");
        }
    }

    private void deleteStudent() {
        String id = readRequired("Enter the ID of the student to delete: ");
        Student student = students.get(id);
        if (student == null) {
            System.out.println("Student not found.");
            return;
        }

        System.out.print("Delete " + student.getName() + " (ID " + id + ")? (y/n): ");
        String confirmation = scanner.nextLine().trim();
        if (!confirmation.equalsIgnoreCase("y")) {
            System.out.println("Delete cancelled.");
            return;
        }

        if (saveChange(() -> students.remove(id))) {
            System.out.println("Student Deleted Successfully.");
        }
    }

    private String readRequired(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("This field cannot be empty.");
        }
    }

    private boolean saveChange(Runnable change) {
        Map<String, Student> previousState = new LinkedHashMap<String, Student>(students);
        change.run();
        try {
            saveStudents();
            return true;
        } catch (IOException e) {
            students.clear();
            students.putAll(previousState);
            System.err.println("Could not save student data: " + e.getMessage());
            return false;
        }
    }

    private void loadStudents() throws IOException {
        if (!Files.exists(dataFile)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                List<String> fields;
                try {
                    fields = parseCsvLine(line);
                } catch (IllegalArgumentException e) {
                    throw new IOException("Invalid CSV at line " + lineNumber + ".", e);
                }
                if (fields.size() != 3 || fields.get(0).trim().isEmpty()) {
                    throw new IOException("Expected ID, name, and branch at line " + lineNumber + ".");
                }

                Student student = new Student(fields.get(0), fields.get(1), fields.get(2));
                if (students.putIfAbsent(student.getId(), student) != null) {
                    throw new IOException("Duplicate student ID at line " + lineNumber + ".");
                }
            }
        }
    }

    private void saveStudents() throws IOException {
        Path temporaryFile = Files.createTempFile(Paths.get("."), "students-", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                for (Student student : students.values()) {
                    writer.write(escapeCsv(student.getId()));
                    writer.write(',');
                    writer.write(escapeCsv(student.getName()));
                    writer.write(',');
                    writer.write(escapeCsv(student.getBranch()));
                    writer.newLine();
                }
            }

            try {
                Files.move(temporaryFile, dataFile,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<String>();
        StringBuilder field = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (insideQuotes) {
                if (current == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        insideQuotes = false;
                    }
                } else {
                    field.append(current);
                }
            } else if (current == ',') {
                fields.add(field.toString());
                field.setLength(0);
            } else if (current == '"' && field.length() == 0) {
                insideQuotes = true;
            } else {
                field.append(current);
            }
        }

        if (insideQuotes) {
            throw new IllegalArgumentException("Unclosed quoted field.");
        }
        fields.add(field.toString());
        return fields;
    }

    private String escapeCsv(String value) {
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private void printStudent(Student student) {
        System.out.println("ID: " + student.getId()
                + " | Name: " + student.getName()
                + " | Branch: " + student.getBranch());
    }

    private static class Student {
        private final String id;
        private final String name;
        private final String branch;

        private Student(String id, String name, String branch) {
            this.id = id;
            this.name = name;
            this.branch = branch;
        }

        private String getId() {
            return id;
        }

        private String getName() {
            return name;
        }

        private String getBranch() {
            return branch;
        }
    }
}
