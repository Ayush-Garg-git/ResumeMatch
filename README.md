# ResumeMatch - Truth-First Career Readiness Copilot

> **Zero-Hallucination AI Copilot for College Graduates & Job Seekers**  
> Diagnoses true readiness against any Job Description using a 4-Bucket Evidence Matrix, bridges real gaps with hands-on learning missions, and compiles ATS-optimized resumes sourced strictly from verified experience.

---

## Architecture & Features

1. **Profile & Truth Bank Review (Checkpoint 1)**:
   - Extracts skills, coursework, and projects from uploaded master resumes.
   - Interactive verification card allowing candidates to review and confirm their source-of-truth registry before matching.
2. **4-Bucket Readiness Diagnosis**:
   - **Demonstrated**: Directly verified by resume capstones and project evidence.
   - **Hidden But Likely**: Implied competencies unlocked with targeted 1-minute Q&A.
   - **Confirmed But Weak**: Self-declared skills requiring deeper project artifacts to defend.
   - **Genuine Gaps (Checkpoint 2)**: Dynamically branched into:
     - **Closable Technical Gaps**: Trigger hands-on mini-project learning missions.
     - **Structural Constraints**: Separates Seniority/Years from Binary Eligibility (Security Clearances, Visa Sponsorship, Work Authorization) with tailored guidance.
3. **Strict Sourcing & Zero-Hallucination Tailoring (Checkpoint 3)**:
   - Tailored resume bullets are drawn **exclusively** from resume-verified evidence.
   - Self-reported Q&A answers are routed strictly to technical **Interview Defense Practice**.
4. **ATS Single-Column Print / PDF Export**:
   - Clean, badge-free, standard-typography resume export optimized for ATS parsers and recruiters.
5. **Multi-Job Comparison Matrix**:
   - Side-by-side breakdown comparing readiness across all saved target roles.

---

## Tech Stack

- **Backend**: Java 21 LTS, Spring Boot 3.3, Spring Security (JWT), Spring Data JPA, Apache PDFBox, Apache POI
- **Database**: PostgreSQL 16 / Neon Serverless Postgres with automated Flyway database migrations
- **Message Broker**: RabbitMQ / CloudAMQP
- **AI Engine**: Google Gemini AI (1.5 Flash / 2.0 Flash) with per-user rate limiting & token telemetry
- **Frontend**: Vanilla ES6+ JavaScript, Modern CSS Glassmorphism Design System

---

## Quick Start (Local Development)

### 1. Prerequisites
- Java 21 JDK
- Node.js 20+
- PostgreSQL 16 / Neon Database URL
- RabbitMQ (or local Docker)

### 2. Environment Variables (.env)
```env
GEMINI_API_KEY=your_gemini_api_key
DB_URL=jdbc:postgresql://your-neon-host/neondb?sslmode=require
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password
JWT_SECRET=your_jwt_secret_key
SPRING_PROFILES_ACTIVE=dev
```

### 3. Launch Services
```bash
# Full stack via Docker Compose
docker compose up --build -d

# Or run frontend directly
cd frontend
node server.js
```
Open **`http://localhost:3000`** in your browser.

---

## License
MIT License. Built for university graduates and job seekers.
