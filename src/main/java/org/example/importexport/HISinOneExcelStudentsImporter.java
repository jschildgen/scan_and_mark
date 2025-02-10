package org.example.importexport;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.model.Student;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HISinOneExcelStudentsImporter implements StudentImporter {

    private List<File> excelFiles;

    public HISinOneExcelStudentsImporter(List<File> excelFiles) {
        this.excelFiles = excelFiles;
    }

    public HISinOneExcelStudentsImporter(File excelFile) {
        this.excelFiles = List.of(excelFile);
    }

    @Override
    public List<Student> import_students() throws IOException {
        List<Student> students = new ArrayList<>();
        for (File file : excelFiles) {
            try (FileInputStream fis = new FileInputStream(file);
                 Workbook workbook = new XSSFWorkbook(fis)) {
                for (Sheet sheet : workbook) {
                    boolean startFound = false;
                    boolean endFound = false;
                    int lastNameCol = -1;
                    int firstNameCol = -1;
                    int matrikelCol = -1;

                    for (Row row : sheet) {
                        if (endFound) break;
                        if (!startFound) {
                            for (Cell cell : row) {
                                if (cell.getCellType() == CellType.STRING &&
                                        "startHISsheet".equalsIgnoreCase(cell.getStringCellValue())) {
                                    startFound = true;
                                    break;
                                }
                            }
                            continue;
                        }

                        for (Cell cell : row) {
                            if (cell.getCellType() == CellType.STRING &&
                                    "endHISsheet".equalsIgnoreCase(cell.getStringCellValue())) {
                                endFound = true;
                                break;
                            }
                        }
                        if (endFound) break;

                        if (lastNameCol == -1 || firstNameCol == -1 || matrikelCol == -1) {
                            for (Cell cell : row) {
                                if (cell.getCellType() == CellType.STRING) {
                                    String header = cell.getStringCellValue().toLowerCase();
                                    if (header.contains("nachname")) {
                                        lastNameCol = cell.getColumnIndex();
                                    } else if (header.contains("vorname")) {
                                        firstNameCol = cell.getColumnIndex();
                                    } else if (header.contains("matrikelnummer")) {
                                        matrikelCol = cell.getColumnIndex();
                                    }
                                }
                            }
                            continue;
                        }

                        Cell matrikelCell = row.getCell(matrikelCol);
                        Cell lastNameCell = row.getCell(lastNameCol);
                        Cell firstNameCell = row.getCell(firstNameCol);

                        String matrikelNummer = matrikelCell != null ? getCellValueAsString(matrikelCell).trim() : "";
                        String lastName = lastNameCell != null ? getCellValueAsString(lastNameCell) : "";
                        String firstName = firstNameCell != null ? getCellValueAsString(firstNameCell) : "";

                        if (!matrikelNummer.isEmpty()) {
                            if (matrikelNummer.matches("\\d+\\.0+")) {
                                matrikelNummer = matrikelNummer.substring(0, matrikelNummer.indexOf('.'));
                            }
                        }

                        if ((!matrikelNummer.isEmpty())) {
                            Student student = new Student(matrikelNummer);
                            student.setName1(lastName);
                            student.setName2(firstName);

                            students.add(student);
                        }
                    }
                }
            } catch (IOException e) {
                throw e;
            }
        }
        System.out.println(this.getClass().getName()+": Imported "+students.size()+" students.");
        return students;
    }

    public static boolean format_is_valid(Object o) {
        if (!(o instanceof File file)) {
            return false;
        }

        try {
            try (FileInputStream fis = new FileInputStream(file);
                 Workbook workbook = new XSSFWorkbook(fis)) {
                for (Sheet sheet : workbook) {
                    boolean startFound = false;
                    boolean endFound = false;
                    boolean hasLastNameCol = false;
                    boolean hasFirstNameCol = false;
                    boolean hasMatrikelCol = false;

                    for (Row row : sheet) {
                        if (endFound) break;

                        // Check for the start marker "startHISsheet"
                        if (!startFound) {
                            for (Cell cell : row) {
                                if (cell.getCellType() == CellType.STRING &&
                                        "startHISsheet".equalsIgnoreCase(cell.getStringCellValue())) {
                                    startFound = true;
                                    break;
                                }
                            }
                            continue;
                        }

                        // Check for the end marker "endHISsheet"
                        for (Cell cell : row) {
                            if (cell.getCellType() == CellType.STRING &&
                                    "endHISsheet".equalsIgnoreCase(cell.getStringCellValue())) {
                                endFound = true;
                                break;
                            }
                        }
                        if (endFound) break;

                        // Detect required columns
                        for (Cell cell : row) {
                            if (cell.getCellType() == CellType.STRING) {
                                String header = cell.getStringCellValue().toLowerCase();
                                if (header.contains("nachname")) {
                                    hasLastNameCol = true;
                                } else if (header.contains("vorname")) {
                                    hasFirstNameCol = true;
                                } else if (header.contains("matrikelnummer")) {
                                    hasMatrikelCol = true;
                                }
                            }
                        }

                        // If all required columns are found, format is valid
                        if (hasLastNameCol && hasFirstNameCol && hasMatrikelCol) {
                            return true;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // If no sheet with the required format was found, return false
        return false;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }
}
