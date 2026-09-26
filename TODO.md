# Implementation Plan - Scanner Flow + Full Workflow Fix

Status: all items below are implemented as of the Course/BO/Chief hierarchy
restructure. Left here as a historical record of what was built.

## Phase 1: Backend Changes

### 1.1 SubjectStatus enum
- [x] Add `PUBLISHED` status to `SubjectStatus.java`

### 1.2 RE Controller - Close & Publish endpoints
- [x] Add `POST /api/re/subjects/{subjectId}/close` to `ReController.java` + `SubjectService.java`
- [x] Add `POST /api/re/subjects/{subjectId}/publish` to `ReController.java` + `SubjectService.java`
- [x] Add dashboard progress endpoint (`GET /api/re/subjects/{subjectId}/progress`)

### 1.3 ValuationService - Add subject status guard
- [x] Add subject status check before: createScript (skip for scanner), formBundle, assignBundle, submitScriptMarks, reviewScript, approveBundle

### 1.4 ScannerController - Add file preview endpoint
- [x] Add `GET /api/scanner/scripts/{scriptId}/file` endpoint

### 1.5 MarksService - Add publish guard
- [x] Only return actual marks when subject status is PUBLISHED

## Phase 2: Frontend Changes

### 2.1 API client (valuation.js)
- [x] Add `lookupBooklet(bookletId)`
- [x] Add `closeSubject(subjectId)`, `publishSubject(subjectId)`
- [x] Add `getScannerScriptFile(scriptId)`
- [x] Add `getSubjectProgress(subjectId)`

### 2.2 Scanner Upload Modal
- [x] Wire search button to call `lookupBooklet()` and auto-fill form
- [x] Show "not found" error properly

### 2.3 Scanner Dashboard
- [x] Add "Preview" column with PDF preview button

### 2.4 RE Dashboard
- [x] Add "Close Evaluation" button for OPEN subjects
- [x] Add "Publish Results" button for CLOSED subjects
- [x] Show progress per subject
- [x] Show PUBLISHED status

### 2.5 Invigilator Mobile Page
- [x] Manual entry fallback when QR scanning isn't available

## Since this plan was written

The hierarchy was restructured: RE now creates a Course first (course + semester),
then Subjects (QP codes) within it; RE appoints one BO per Course (not per
Subject); the BO appoints a Subject Chief per subject; the Chief (not the BO)
creates the Reviewer and forms/assigns bundles. See
`HIERARCHY-RESTRUCTURE-CHANGES.md` and `SECURITY-AND-QUALITY-AUDIT.md` in the
project root for details.
