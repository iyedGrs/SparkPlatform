# Materials CRUD — Implemented Scope (Iyed)

This document summarizes the delivered **Materials CRUD** slice in the JavaFX shell.

## 1) What was implemented

- New end-to-end Materials module loaded from sidebar `Classroom` route.
- CRUD operations for `materials` with **soft delete (archive)**.
- JavaFX UI for list + form actions:
  - Create
  - Update
  - Archive
  - Refresh
- Local attachment workflow for PDF files:
  - Select PDF from computer (`Browse PDF`)
  - Save copies file into local `uploads/materials/`
  - Persist stored path in `file_path`
- Open attachment from UI (`Open File`).
- Double-click row opens a content viewer dialog.

## 2) Architecture decisions

### Repository / service split

- `MaterialRepository` interface defines persistence contract.
- `JdbcMaterialRepository` handles MySQL access.
- `InMemoryMaterialRepository` handles local no-DB mode.
- `MaterialRepositoryFactory` selects backend automatically:
  - tries MySQL first
  - falls back to in-memory when DB config/connection is unavailable
- `MaterialService` contains validation/business rules.

### Why this design

- Keeps code aligned with production-style DB logic.
- Allows local development and demos without installing MySQL.
- Keeps UI independent from storage backend.

## 3) Save logic and validation rules

In `MaterialService`:

- `title` is required
- `type` is required
- if `type = PDF`, attachment (`file_path`) is required
- for non-PDF types, at least one of `file_path` or `content` is required
- defaults:
  - `visibility = PUBLIC` when missing
  - `status = ACTIVE` when missing

## 4) PDF handling behavior

### Attachment flow

1. User selects file via `Browse PDF`.
2. On save, selected file is copied to:
   - `uploads/materials/<timestamp>_<original-name>.pdf`
3. Copied path is stored in material `file_path`.

### Content extraction

- Added Apache PDFBox dependency (`org.apache.pdfbox:pdfbox`).
- On save for `PDF` materials, the app attempts to extract real PDF text.
- Extracted text is saved into `content`.
- If extraction fails/empty, it falls back to manual content entered by user.

## 5) UI behavior summary

- Table columns: `ID`, `Title`, `Type`, `Visibility`, `Status`.
- Form fields: title, type, visibility, file path, notes/description.
- Buttons:
  - `New`
  - `Save`
  - `Archive`
  - `Refresh`
  - `Browse PDF`
  - `Open File`
- Row interaction:
  - single click = load form for edit
  - double click = open content dialog

## 6) Files added/updated

### Added

- `src/main/java/com/classroom/controllers/ClassroomMaterialsController.java`
- `src/main/java/com/classroom/services/MaterialRepository.java`
- `src/main/java/com/classroom/services/JdbcMaterialRepository.java`
- `src/main/java/com/classroom/services/InMemoryMaterialRepository.java`
- `src/main/java/com/classroom/services/MaterialRepositoryFactory.java`
- `src/main/java/com/classroom/services/MaterialService.java`
- `src/main/resources/fxml/classroom-view.fxml`
- `docs/MATERIALS_CRUD.md`

### Updated

- `src/main/java/com/classroom/controllers/AppShellController.java`
- `src/main/resources/css/app-shell.css`
- `docs/SETUP_GUIDE.md`
- `pom.xml`

## 7) Known limitations

- No pagination/filter/search yet.
- No background processing for heavy PDFs.
- No separate field for "manual notes" vs "extracted text" (both currently map to `content`).
- In-memory fallback data is not persistent between app restarts.

## 8) Manual test checklist

1. Open app and navigate to `Classroom`.
2. Create PDF material using `Browse PDF` + `Save`.
3. Verify file appears in `uploads/materials/`.
4. Verify record appears in table.
5. Double-click row and confirm text dialog opens.
6. Click `Open File` and confirm OS opens the PDF.
7. Archive material and verify status becomes `ARCHIVED`.
8. Restart without DB config and verify fallback still works in-memory.

## 9) Suggested next step

- Add separate fields:
  - `content_extracted` (from PDF)
  - `notes` (manual teacher text)
so both user-authored notes and extracted text are preserved independently.
