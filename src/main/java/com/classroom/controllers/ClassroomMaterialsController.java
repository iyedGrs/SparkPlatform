package com.classroom.controllers;

import com.classroom.services.MaterialService;
import com.spark.platform.models.Material;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class ClassroomMaterialsController {

    @FXML private Label storageModeLabel;

    @FXML private TableView<Material> materialsTable;
    @FXML private TableColumn<Material, Number> colId;
    @FXML private TableColumn<Material, String> colTitle;
    @FXML private TableColumn<Material, String> colType;
    @FXML private TableColumn<Material, String> colVisibility;
    @FXML private TableColumn<Material, String> colStatus;

    @FXML private TextField titleField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> visibilityCombo;
    @FXML private TextField filePathField;
    @FXML private TextArea contentArea;
    @FXML private Button archiveButton;

    private MaterialService materialService;
    private Material selectedMaterial;
    private File selectedPdfFile;

    @FXML
    private void initialize() {
        materialService = new MaterialService();

        colId.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getMaterialId()));
        colTitle.setCellValueFactory(cell -> new SimpleStringProperty(nullSafe(cell.getValue().getTitle())));
        colType.setCellValueFactory(cell -> new SimpleStringProperty(nullSafe(cell.getValue().getType())));
        colVisibility.setCellValueFactory(cell -> new SimpleStringProperty(nullSafe(cell.getValue().getVisibility())));
        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(nullSafe(cell.getValue().getStatus())));

        typeCombo.setItems(FXCollections.observableArrayList("PDF", "MIND_MAP", "QUIZ", "FLASHCARD", "SLIDE", "VIDEO", "AUDIO"));
        visibilityCombo.setItems(FXCollections.observableArrayList("PUBLIC", "PRIVATE", "CLASS_ONLY"));

        materialsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selectedMaterial = newSelection;
            if (newSelection == null) {
                clearForm();
                return;
            }
            fillForm(newSelection);
            archiveButton.setDisable("ARCHIVED".equalsIgnoreCase(newSelection.getStatus()));
        });

        materialsTable.setRowFactory(table -> {
            TableRow<Material> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showMaterialContent(row.getItem());
                }
            });
            return row;
        });

        storageModeLabel.setText("Storage: " + materialService.getStorageModeLabel());
        archiveButton.setDisable(true);
        refreshTable();
    }

    @FXML
    private void onNew() {
        materialsTable.getSelectionModel().clearSelection();
        selectedMaterial = null;
        selectedPdfFile = null;
        clearForm();
    }

    @FXML
    private void onSave() {
        try {
            Material material = new Material();

            if (selectedMaterial != null) {
                material.setMaterialId(selectedMaterial.getMaterialId());
                material.setCreatedAt(selectedMaterial.getCreatedAt());
                material.setStatus(selectedMaterial.getStatus());
            }

            material.setTitle(titleField.getText() == null ? null : titleField.getText().trim());
            material.setType(typeCombo.getValue());
            material.setVisibility(visibilityCombo.getValue());
            String resolvedFilePath = resolveFilePathForSave();
            material.setFilePath(resolvedFilePath);

            String manualContent = contentArea.getText() == null ? null : contentArea.getText().trim();
            if ("PDF".equalsIgnoreCase(material.getType())) {
                String extractedContent = extractPdfContent(resolvedFilePath);
                material.setContent(extractedContent == null || extractedContent.isBlank() ? manualContent : extractedContent);
            } else {
                material.setContent(manualContent);
            }

            Material saved = materialService.saveMaterial(material);
            refreshTable();
            selectMaterialById(saved.getMaterialId());
            showInfo("Material saved", "Material data saved successfully.");
        } catch (IllegalArgumentException ex) {
            showError("Validation error", ex.getMessage());
        } catch (RuntimeException ex) {
            showError("Save failed", ex.getMessage());
        }
    }

    @FXML
    private void onArchive() {
        if (selectedMaterial == null) {
            showError("No selection", "Select a material to archive.");
            return;
        }

        try {
            materialService.archiveMaterial(selectedMaterial.getMaterialId());
            refreshTable();
            selectMaterialById(selectedMaterial.getMaterialId());
            showInfo("Material archived", "Material archived successfully.");
        } catch (RuntimeException ex) {
            showError("Archive failed", ex.getMessage());
        }
    }

    @FXML
    private void onRefresh() {
        refreshTable();
    }

    @FXML
    private void onBrowsePdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select PDF Material");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );

        File selectedFile = fileChooser.showOpenDialog(filePathField.getScene().getWindow());
        if (selectedFile != null) {
            selectedPdfFile = selectedFile;
            if (typeCombo.getValue() == null || typeCombo.getValue().isBlank()) {
                typeCombo.setValue("PDF");
            }
            if (titleField.getText() == null || titleField.getText().isBlank()) {
                titleField.setText(stripPdfExtension(selectedFile.getName()));
            }
            filePathField.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    private void onOpenFile() {
        String filePathValue = filePathField.getText() == null ? "" : filePathField.getText().trim();
        if (filePathValue.isBlank()) {
            showError("No attachment", "Select or save a PDF first.");
            return;
        }

        Path path = Paths.get(filePathValue);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
        }

        if (!Files.exists(path)) {
            showError("File not found", "Attachment not found: " + path);
            return;
        }

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            showError("Open not supported", "This environment cannot open files directly.");
            return;
        }

        try {
            Desktop.getDesktop().open(path.toFile());
        } catch (IOException e) {
            showError("Open failed", "Could not open attachment: " + e.getMessage());
        }
    }

    private void refreshTable() {
        List<Material> materials = materialService.listMaterials();
        materialsTable.setItems(FXCollections.observableArrayList(materials));
        archiveButton.setDisable(selectedMaterial == null || "ARCHIVED".equalsIgnoreCase(selectedMaterial.getStatus()));
    }

    private void fillForm(Material material) {
        titleField.setText(material.getTitle());
        typeCombo.setValue(material.getType());
        visibilityCombo.setValue(material.getVisibility());
        filePathField.setText(material.getFilePath());
        contentArea.setText(material.getContent());
        selectedPdfFile = null;
    }

    private void clearForm() {
        titleField.clear();
        typeCombo.getSelectionModel().clearSelection();
        visibilityCombo.setValue("PUBLIC");
        filePathField.clear();
        contentArea.clear();
        archiveButton.setDisable(true);
        selectedPdfFile = null;
    }

    private String resolveFilePathForSave() {
        String currentValue = filePathField.getText() == null ? null : filePathField.getText().trim();
        File sourceFile = resolveSourceFile(currentValue);
        if (sourceFile == null) {
            return currentValue;
        }

        try {
            Path uploadDirectory = Paths.get("uploads", "materials");
            Files.createDirectories(uploadDirectory);

            String originalFileName = sourceFile.getName();
            String targetFileName = System.currentTimeMillis() + "_" + originalFileName;
            Path targetPath = uploadDirectory.resolve(targetFileName);

            Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            selectedPdfFile = null;
            return targetPath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy selected PDF to uploads/materials", e);
        }
    }

    private File resolveSourceFile(String currentValue) {
        if (selectedPdfFile != null) {
            return selectedPdfFile;
        }

        if (currentValue == null || currentValue.isBlank()) {
            return null;
        }

        Path candidate = Paths.get(currentValue);
        if (!candidate.isAbsolute()) {
            candidate = Paths.get(System.getProperty("user.dir")).resolve(candidate).normalize();
        }

        if (!Files.exists(candidate) || !Files.isRegularFile(candidate)) {
            return null;
        }

        String fileName = candidate.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".pdf")) {
            return null;
        }

        return candidate.toFile();
    }

    private String stripPdfExtension(String fileName) {
        String lowerCase = fileName.toLowerCase();
        if (lowerCase.endsWith(".pdf")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        return fileName;
    }

    private String extractPdfContent(String filePathValue) {
        if (filePathValue == null || filePathValue.isBlank()) {
            return null;
        }

        Path path = Paths.get(filePathValue);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
        }

        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return null;
        }

        String fileName = path.getFileName().toString().toLowerCase();
        if (!fileName.endsWith(".pdf")) {
            return null;
        }

        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            PDFTextStripper textStripper = new PDFTextStripper();
            String extracted = textStripper.getText(document);
            return extracted == null ? null : extracted.trim();
        } catch (IOException e) {
            return null;
        }
    }

    private void showMaterialContent(Material material) {
        String content = material.getContent() == null ? "" : material.getContent().trim();
        String body;

        if (!content.isBlank()) {
            body = content;
        } else if ("PDF".equalsIgnoreCase(material.getType())) {
            body = "No extracted PDF text is stored yet for this material.\n"
                    + "You can open the file using the Open File button.";
        } else {
            body = "No content available for this material.";
        }

        TextArea contentViewer = new TextArea(body);
        contentViewer.setEditable(false);
        contentViewer.setWrapText(true);
        contentViewer.setPrefWidth(700);
        contentViewer.setPrefHeight(420);

        VBox container = new VBox(10,
                new Label("Title: " + nullSafe(material.getTitle())),
                new Label("Type: " + nullSafe(material.getType())),
                contentViewer
        );

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Material Content");
        dialog.getDialogPane().setContent(container);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void selectMaterialById(int materialId) {
        for (Material material : materialsTable.getItems()) {
            if (material.getMaterialId() == materialId) {
                materialsTable.getSelectionModel().select(material);
                materialsTable.scrollTo(material);
                return;
            }
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
