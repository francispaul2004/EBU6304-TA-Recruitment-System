# User Manual (Skeleton)

## 1. System Overview

System name: BUPT International School TA Recruitment System  
App type: Stand-alone JavaFX desktop application  
Roles: TA Applicant, Module Organiser (MO), Admin

## 2. Environment and Startup

### Requirements

- JDK 17
- Maven 3.9+

### Start steps

1. Open project root.
2. Run `mvn javafx:run`.
3. Login with sample credentials (password: `Password123!`).

## 3. Role Accounts (Sample)

- TA: `ta001` / `ta002` / `ta003` / `ta004` / `ta005`
- MO: `mo001` / `mo002`
- Admin: `admin`

## 4. Main Pages

### P01 Login

Purpose: user authentication and role entry point.

- Inputs: username, password
- Expected behavior: successful login routes to role homepage
- Validation: empty username/password, invalid account/password

Screenshot placeholder:  
![P01 Login](screenshots/P01_Login.png)

### P02 Main Shell

Purpose: role-based navigation shell and common layout.

- Left sidebar: role menu + logout
- Top bar: breadcrumb + term label
- Content area: current page container

Screenshot placeholder:  
![P02 Main Shell](screenshots/P02_Main_Shell.png)

### P03 TA Dashboard

Purpose: quick status summary for TA.

- Profile completion
- Resume completion
- Current workload
- Application count

Screenshot placeholder:  
![P03 TA Dashboard](screenshots/P03_TA_Dashboard.png)

### P04 Applicant Profile

Purpose: maintain applicant identity/profile information.

- Fields: full name, student ID, programme, year, email, phone
- Validation: required fields, email format, year range

Screenshot placeholder:  
![P04 Applicant Profile](screenshots/P04_Applicant_Profile.png)

### P05 Resume Information Form

Purpose: structured CV data input.

- Fields: modules, technical/language skills, experience, statement, availability, max weekly hours
- Validation: max hours > 0, availability required, statement length

Screenshot placeholder:  
![P05 Resume Information](screenshots/P05_Resume_Information.png)

### P06 Job Browser

Purpose: search and filter jobs with list/detail workspace.

- Filters: keyword, module code, status, skill
- List-detail linkage: left list + right detail preview

Screenshot placeholder:  
![P06 Job Browser](screenshots/P06_Job_Browser.png)

### P07 Job Detail & Apply

Purpose: view full job information and submit application.

- Apply button shown only for valid state
- Duplicate/closed/expired/profile-incomplete checks
- Match explanation panel shown for explainable recommendation

Screenshot placeholder:  
![P07 Job Detail Apply](screenshots/P07_Job_Detail_Apply.png)

### P08 My Applications

Purpose: TA application tracking.

- View applications with status
- Filter by status
- View decision notes

Screenshot placeholder:  
![P08 My Applications](screenshots/P08_My_Applications.png)

### P09 Job Management (MO)

Purpose: MO manages own jobs.

- View own jobs
- Create/edit/close operations
- Overview cards at top

Screenshot placeholder:  
![P09 Job Management](screenshots/P09_Job_Management.png)

### P10 Job Editor (MO)

Purpose: create/edit job posting.

- Required fields and validation
- Save draft / publish flow

Screenshot placeholder:  
![P10 Job Editor](screenshots/P10_Job_Editor.png)

### P11 Applicant List (MO)

Purpose: view applicants for selected job.

- Sorting and filtering
- Open review detail

Screenshot placeholder:  
![P11 Applicant List](screenshots/P11_Applicant_List.png)

### P12 Applicant Review (MO)

Purpose: review applicant and make decision.

- Candidate profile + resume summary
- Match score, missing skills, workload projection
- Accept / reject + decision note

Screenshot placeholder:  
![P12 Applicant Review](screenshots/P12_Applicant_Review.png)

### P13 Admin Dashboard

Purpose: system-level workload monitor and export.

- KPI summary
- Workload table
- Risk-level filtering
- CSV export

Screenshot placeholder:  
![P13 Admin Dashboard](screenshots/P13_Admin_Dashboard.png)

## 5. Shared Dialogs

### D01 Validation Error

Shown when input fails rules.

Screenshot placeholder:  
![D01 Validation Error](screenshots/D01_Validation_Error.png)

### D02 Success Feedback

Shown after successful save/apply/review actions.

Screenshot placeholder:  
![D02 Success Feedback](screenshots/D02_Success_Feedback.png)

### D03 Confirm Action

Used before irreversible actions (e.g., closing job).

Screenshot placeholder:  
![D03 Confirm Action](screenshots/D03_Confirm_Action.png)

### D04 Permission Denied

Shown when current role attempts unauthorized action.

Screenshot placeholder:  
![D04 Permission Denied](screenshots/D04_Permission_Denied.png)

### D05 Match / Workload Warning

Explainable panel or alert for missing skills / workload risk.

Screenshot placeholder:  
![D05 Match Warning](screenshots/D05_Match_Workload_Warning.png)

## 6. Acceptance Test Script (Manual)

1. TA logs in and completes profile + resume.
2. TA searches jobs and applies.
3. MO logs in, creates/edits/closes a job.
4. MO reviews applicants and accepts/rejects.
5. Admin checks workload and exports CSV.
6. Trigger invalid input and verify validation dialog.
7. Try duplicate apply and verify blocked.
8. Accept job exceeding max hours and verify warning/risk.

## 7. Evidence Attachment Guide

Place screenshots in `docs/screenshots/` with names used above.
