# EBU6304-TA-Recruitment-System

Software Engineering Group Project for EBU6304 – Teaching Assistant Recruitment System

## Group Members

| Name | GitHub ID | BUPT ID | QM ID | Email |
|---|---|---|---|---|
| Haocheng Gao | francispaul2004 | 2023213304 | 231220873 | jp2023213304@qmul.ac.uk |
| Xinshuang Liu | Jhon0213 | 2023213305 | 231221283 | jp2023213305@qmul.ac.uk |
| Chuan Yu | chuan123123456 | 2023213272 | 231221700 | jp2023213272@qmul.ac.uk |
| Ziqi Gao | Coder-Maxxx | 2023213316 | 231221250 | jp2023213316@qmul.ac.uk |
| Xianggan Xu | Gnaixux | 2023213306 | 231221412 | jp2023213306@qmul.ac.uk |
| Zhibo Zhang | iapetuszzb | 2023213278 | 231221777 | jp2023213278@qmul.ac.uk |

## Group Task Allocation

To improve development efficiency, Group40 assigned each member to take primary responsibility for a core module. These responsibilities reflect the main implementation focus of each member, while all members were still expected to contribute across the project lifecycle through discussion, integration, testing, and refinement.

Haocheng Gao – User and Identity Module & Style Framework
- System styling and UI framework
- Login functionality
- Role management
- Account information management

Zhibo Zhang – Applicant Profile and CV Module
- Profile editing
- CV file upload
- CV management

Chuan Yu – Job Browsing and Application Module
- Job list
- Job details
- Application submission
- My applications

Xinshuang Liu – MO Job Management Module
- Create job postings
- Edit job postings
- Close job postings
- Job status management

Xianggan Xu – Review and Offer Module
- View applicants
- Review decisions
- Matching explanations
- Offer result flow management

Ziqi Gao – Administrator Monitoring and Reporting Module
- Dashboard
- Workload statistics
- Export reports
- Audit logs

## Project Overview

EBU6304-TA-Recruitment-System is a web-based system developed for the Teaching Assistant recruitment process in a university setting. The project aims to improve the efficiency and organisation of TA recruitment by providing a clearer and more structured workflow for all participants.

The system is designed for three main user groups: Teaching Assistants (TAs), Module Organisers (MOs), and Administrators. TAs can submit and manage their applications, MOs can review candidates and make decisions for their modules, and Administrators can oversee and coordinate the overall process.

By replacing manual and fragmented handling with a centralised digital platform, the project helps reduce repetitive administrative work, improve transparency, and support a more user-friendly recruitment experience.

# BUPT International School TA Recruitment System

Stand-alone Java 17 desktop application (JavaFX) for TA recruitment workflow management, aligned to EBU6304 handout constraints.

## 1. Constraint Compliance

- Language/runtime: Java 17
- App type: JavaFX stand-alone desktop app
- Persistence: JSON/CSV/TXT only (no database)
- Architecture: layered `ui / controller / service / repository / model / util`
- Core-first delivery: implemented in staged iterations (core workflow before enhancements)
- Testability: unit + integration tests included
- Maintainability: repository/service separation, reusable controllers, centralized utilities
- Documentation: this README + user manual skeleton + JavaDoc generation command

## 2. Tech Stack

- Java 17
- JavaFX 17
- Maven
- Jackson (JSON)
- OpenCSV (CSV export)
- JUnit 5
- Mockito (optional dependency included)

## 3. Project Structure

```text
src/main/java/edu/bupt/ta/
  App.java
  config/
  model/
  enums/
  dto/
  repository/
  service/
  controller/
  ui/
  util/
src/main/resources/
  styles/
  sample-data/
src/test/java/
  service/
  repository/
  integration/
```

## 4. Roles and Core Workflows

### Roles

- TA Applicant
- Module Organiser (MO)
- Admin

### Core features implemented

- Login and role-based routing
- Applicant profile form
- Resume information form (structured CV)
- Job browse/search/filter + detail
- Job apply flow (with validation and duplicate prevention)
- My applications status tracking
- MO job management (create/edit/close)
- MO applicant list and review (accept/reject)
- Admin workload monitor
- CSV export (workload/application reports)

### Enhanced features implemented

- Match score
- Missing skills
- Workload projection warning / risk level
- Explainable match panel in job detail

## 5. Data Files (No DB)

Runtime data directory defaults to `data/` and can be overridden with:

```bash
-Dta.data.dir=/your/path
```

Required files used:

- `data/users.json`
- `data/applicant_profiles.json`
- `data/resume_infos.json`
- `data/jobs.json`
- `data/applications.json`
- `data/workloads.json`
- `data/audit_log.txt`
- `data/export/workload_report.csv`
- `data/export/application_report.csv`

Sample demo data is preloaded and mirrored under `src/main/resources/sample-data/`.

## 6. Demo Accounts

All sample accounts use password: `Password123!`

- TA: `ta001`, `ta002`, `ta003`, `ta004`, `ta005`
- MO: `mo001`, `mo002`
- Admin: `admin`

## 7. Run & Test

### Run desktop app

```bash
mvn javafx:run
```

### Run tests

```bash
mvn test
```

### Full verification

```bash
mvn verify
```

### Generate JavaDocs

```bash
mvn javadoc:javadoc
```

Output directory:

- `target/site/apidocs/`

## 8. Test Coverage

### Unit tests

- `AuthenticationServiceTest`
- `ApplicantProfileServiceTest`
- `ResumeServiceTest`
- `JobServiceTest`
- `ApplicationServiceTest`
- `ReviewServiceTest`
- `WorkloadServiceTest`
- `MatchingServiceTest`
- `JsonRepositoryTest`

### Integration tests

- create job -> save -> reload
- apply job -> application visible
- accept application -> workload updated
- close job -> block further apply
- expired job -> blocked
- export csv -> file generated

## 9. Iteration Plan and Delivery Status

### Phase 1: skeleton + repository + sample data

Completed.

### Phase 2: login + jobs + apply

Completed.

### Phase 3: profile/resume + applications status

Completed.

### Phase 4: review + workload + admin monitor

Completed.

### Phase 5: matching + docs + tests

Completed (including test suite and JavaDoc command verification).

## 10. User Manual

See:

- `docs/User-Manual.md`

This includes page-by-page operation guidance and screenshot placeholders for each main frame.

## 11. AI Implementation Guide

For reusable AI-assisted implementation rules, scope, and workflow, see:

- `docs/codex-implementation-guide.md`
