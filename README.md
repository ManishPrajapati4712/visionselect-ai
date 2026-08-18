<div align="center">

# 🏏 VisionSelect AI

**Explainable, Auditable Cricket Talent Intelligence — powered by Computer Vision & Gemini AI**

[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react)](https://react.dev)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.8-3178C6?logo=typescript)](https://www.typescriptlang.org)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql)](https://www.postgresql.org)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-4.x-06B6D4?logo=tailwindcss)](https://tailwindcss.com)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

</div>

---

## 📌 Overview

**VisionSelect AI** is a full-stack AI-powered cricket talent evaluation platform that analyses match footage to produce transparent, auditable player scores. Unlike black-box AI systems, every point in a player's score is anchored to a **specific timestamped moment** in the footage — so selectors, coaches, and players can see exactly *why* a score was given and challenge it if needed.

---

## 🎯 Problem Statement

Traditional cricket talent selection relies heavily on subjective observation, which introduces:
- **Inconsistency** — different selectors score the same play differently
- **Bias** — regional, stylistic, or personal bias can unfairly disadvantage players
- **Opacity** — players receive scores without any explanation or evidence
- **Scalability limits** — manual review of hundreds of hours of footage is impractical

---

## 💡 Solution

VisionSelect AI replaces subjective guesswork with a structured, evidence-based pipeline:

1. **Upload** match footage through a secure, multi-part upload flow
2. **Analyse** using computer vision (pose keypoints, bat tracking, event detection)
3. **Score** across five metric families with weighted, confidence-adjusted contributions
4. **Explain** every score point with a timestamped evidence trail and Gemini AI narrative
5. **Review** — human selectors retain full override authority; every override is logged

---

## ✨ Key Features

| Feature | Description |
|---|---|
| 🎥 **Secure Video Upload** | Multi-part upload with pre-signed URLs via the Spring Boot backend |
| 🤖 **AI Scoring Engine** | Pose estimation, bat tracking & event detection across 5 metric families |
| 🔍 **Explainable AI** | Every score point linked to a timestamped video moment |
| 💬 **Gemini AI Assistant** | Ask natural-language questions about any score or metric |
| ⚖️ **Fairness & Bias Reports** | Regional, stylistic and demographic variance monitoring |
| 👤 **Human-in-the-Loop** | Selectors can override, accept or reject any AI decision |
| 🔄 **Player Comparison** | Side-by-side metric comparison across candidates |
| 📊 **Role-Based Dashboard** | Tailored views for Players, Coaches, and Selectors |
| 🔐 **JWT Authentication** | Access token in-memory; refresh token in localStorage |
| 📱 **Fully Responsive** | Mobile-first design built with Tailwind CSS v4 |

---

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Browser Client                   │
│          React 19 + TanStack Router + Vite          │
│   (TypeScript · Tailwind CSS v4 · Radix UI · RHF)  │
└──────────────────────┬──────────────────────────────┘
                       │  REST / JSON  (JWT Bearer)
                       ▼
┌─────────────────────────────────────────────────────┐
│              Spring Boot Backend (Java)             │
│   Video upload · Auth · Storage · REST API          │
│              PostgreSQL via Flyway                  │
└──────────────────────┬──────────────────────────────┘
                       │  Internal service token
                       ▼
┌─────────────────────────────────────────────────────┐
│            Python AI Service  (planned)             │
│   Computer Vision · Pose estimation · Gemini API   │
└─────────────────────────────────────────────────────┘
```

---

## 🔄 Application Workflow

```
Player uploads video
        │
        ▼
Backend issues pre-signed upload URL
        │
        ▼
Video stored (LocalStorageProvider / S3 / GCS)
        │
        ▼
AI pipeline: frame sampling → pose keypoints → bat tracking → event detection
        │
        ▼
Score generated across 5 metric families
        │
        ▼
Gemini AI generates evidence narrative + timestamp links
        │
        ▼
Selector reviews score, evidence and fairness report
        │
        ▼
Human override accepted / rejected → logged for audit
```

---

## 🤖 AI & Gemini Integration

The Gemini AI layer powers three surfaces:

- **Score Explanation** (`/explain`) — streams a live narrative tying every score point to a timestamped video event
- **AI Assistant** (`/assistant`) — answers natural-language questions about a player's metrics ("Why did his batting score drop in the second innings?")
- **Comparison Verdict** (`/compare`) — Gemini summarises how two candidates differ across metric families

> The AI integration is currently implemented with a **streaming placeholder** in the frontend (`geminiService.ts`) that mimics a live model response. The production path will route through `Spring Boot → Python AI Service → Gemini API` without requiring any frontend changes.

---

## ⚖️ Fairness & Bias Transparency

The `/fairness` dashboard provides:
- **Regional variance** reporting — detects if players from certain regions are systematically scored lower
- **Stylistic variance** — controls for playing styles that may be penalised by purely statistical models
- **Confidence-weighted scoring** — low-confidence signals are down-weighted, not suppressed
- **Override audit trail** — every human override is stored and visible to compliance reviewers

---

## 👥 Human-in-the-Loop Design

VisionSelect AI is explicitly designed so that **AI assists, humans decide**:
- Selectors can **agree, disagree, or partially override** any AI score
- Override reasons are captured and stored
- Overrides feed back into model calibration (planned)
- All decisions are auditable end-to-end

---

## 🧱 Technology Stack

### Frontend
| Technology | Version | Purpose |
|---|---|---|
| React | 19 | UI framework |
| TypeScript | 5.8 | Type safety |
| Vite | 8 | Dev server & bundler |
| TanStack Router | 1.x | File-based routing |
| TanStack Query | 5.x | Server state management |
| Tailwind CSS | 4 | Utility-first styling |
| Radix UI | latest | Accessible primitives |
| React Hook Form | 7 | Form management |
| Zod | 3 | Schema validation |
| Lucide React | latest | Icons |
| Recharts | 2 | Data visualisations |
| Sonner | 2 | Toast notifications |

### Backend
| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Runtime |
| Spring Boot | 3.x | Application framework |
| Spring Security | 6.x | Auth & authorisation |
| PostgreSQL | 16 | Primary database |
| Flyway | 10.x | Database migrations |
| HikariCP | bundled | Connection pooling |
| SpringDoc / OpenAPI | 2.x | API documentation |

### Infrastructure (planned / extensible)
| Technology | Purpose |
|---|---|
| AWS S3 / GCS | Cloud video storage |
| Google Gemini API | AI narrative generation |
| Python AI Service | Computer vision pipeline |

---

## 📁 Repository Structure

```
visionselect-ai/
├── frontend/                   # React + Vite frontend
│   ├── src/
│   │   ├── components/         # Reusable UI components (brand, common, ui)
│   │   ├── context/            # React contexts (AuthContext, AppContext)
│   │   ├── data/               # Mock/seed data for AI responses
│   │   ├── hooks/              # Custom React hooks
│   │   ├── layouts/            # Page layout wrappers
│   │   ├── lib/                # Utilities (cn, etc.)
│   │   ├── routes/             # TanStack Router file-based routes
│   │   ├── services/           # API client, auth, video, player services
│   │   ├── styles.css          # Global styles & Tailwind configuration
│   │   └── types/              # Shared TypeScript types
│   ├── .env.example            # Environment variable template (safe to commit)
│   ├── vite.config.ts
│   └── package.json
│
└── backend/                    # Spring Boot backend
    ├── src/
    │   ├── main/java/com/visionselect/backend/
    │   │   ├── api/v1/videos/   # Video REST controller
    │   │   ├── config/          # Spring Security config
    │   │   ├── storage/         # StorageProvider abstraction + LocalStorageProvider
    │   │   └── video/           # Video domain (entity, service, repository, DTOs)
    │   └── main/resources/
    │       ├── application.yml  # All config via environment variables
    │       └── db/migration/    # Flyway SQL migrations
    ├── .env.example             # Environment variable template (safe to commit)
    └── pom.xml (or build.gradle)
```

---

## ⚙️ Environment Variables

### Frontend (`frontend/.env.example`)

```env
# Copy to .env.local — never commit .env.local
VITE_API_BASE_URL=http://localhost:8081
```

### Backend (`backend/.env.example`)

```env
# Copy to .env — never commit .env
SPRING_PROFILES_ACTIVE=dev

# Database
DB_URL=jdbc:postgresql://localhost:5432/visionselect
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password

# Server
SERVER_PORT=8081

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:8080

# JWT (minimum 256 bits for HS256)
JWT_SECRET=your_jwt_secret_minimum_256_bits
JWT_ACCESS_EXPIRATION_MINUTES=45
JWT_REFRESH_EXPIRATION_DAYS=21

# Internal service auth
INTERNAL_SERVICE_TOKEN=your_internal_token

# Storage
STORAGE_PROVIDER=local
STORAGE_LOCAL_BASE_DIRECTORY=./data/local-storage
STORAGE_LOCAL_PUBLIC_BASE_URL=http://localhost:8081
```

> ⚠️ **Never commit real secrets.** All config is injected via environment variables — see the `.env.example` files in each module.

---

## 🚀 Getting Started

### Prerequisites

- **Node.js** ≥ 20 (or [nvm](https://github.com/nvm-sh/nvm))
- **Java** 21 (e.g. via [SDKMAN](https://sdkman.io))
- **PostgreSQL** 16
- **Maven** 3.9+ or **Gradle** 8+

---

### 1. Clone the repository

```bash
git clone https://github.com/ManishPrajapati4712/visionselect-ai.git
cd visionselect-ai
```

---

### 2. Run the Frontend

```bash
cd frontend

# Install dependencies
npm install

# Create your local env file
cp .env.example .env.local
# Edit .env.local — set VITE_API_BASE_URL to your backend URL

# Start the dev server (http://localhost:8080)
npm run dev
```

---

### 3. Run the Backend

```bash
cd backend

# Create your local env file
cp .env.example .env
# Edit .env — set DB credentials, JWT secret, etc.

# Create the PostgreSQL database
psql -U postgres -c "CREATE DATABASE visionselect;"

# Start Spring Boot (Flyway will run migrations automatically)
./mvnw spring-boot:run
# or: ./gradlew bootRun
```

The backend will start on `http://localhost:8081` (set `SERVER_PORT=8081` to avoid conflict with the frontend's port 8080).

API docs (Swagger UI): `http://localhost:8081/swagger-ui.html`

---

## 🗺️ Application Routes

| Route | Description | Role |
|---|---|---|
| `/` | Landing page | Public |
| `/login` | Sign in | Public |
| `/register` | Create account | Public |
| `/roles` | Role selection | Authenticated |
| `/dashboard` | Command centre | All roles |
| `/upload` | Upload match footage | Coach / Selector |
| `/results` | Analysis results list | All roles |
| `/explain` | Score explanation & evidence | All roles |
| `/compare` | Side-by-side player comparison | Selector / Coach |
| `/analysis` | Detailed metric analysis | All roles |
| `/fairness` | Bias & fairness report | Selector |
| `/assistant` | Gemini AI chat assistant | All roles |
| `/report` | Printable selection report | Selector |
| `/notifications` | Activity notifications | All roles |
| `/profile` | User profile | All roles |
| `/settings` | Account settings | All roles |

---

## 📡 API Overview

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Register a new user |
| `POST` | `/api/v1/auth/login` | Login & receive JWT tokens |
| `POST` | `/api/v1/auth/refresh` | Refresh access token |
| `POST` | `/api/v1/auth/logout` | Revoke refresh token |
| `GET` | `/api/v1/users/me` | Get authenticated user profile |
| `POST` | `/api/v1/videos/upload-url` | Request a pre-signed upload URL |
| `POST` | `/api/v1/videos` | Register an uploaded video |
| `GET` | `/api/v1/videos` | List videos |
| `GET` | `/api/v1/videos/{id}` | Get video details |
| `GET` | `/v3/api-docs` | OpenAPI specification |
| `GET` | `/swagger-ui.html` | Swagger UI |

---

## 📸 Screenshots

> _Screenshots will be added as the UI stabilises._

---

## 🔮 Future Improvements

- [ ] Real Gemini API integration (Python AI service → Spring Boot → Frontend)
- [ ] Computer vision pipeline (pose estimation, bat tracking, event detection)
- [ ] AWS S3 / Google Cloud Storage integration for video storage
- [ ] Real-time analysis progress via WebSockets
- [ ] PDF export of selection reports
- [ ] Mobile app (React Native)
- [ ] Multi-language support (Hindi, regional languages)
- [ ] Team-level analytics and cohort comparison
- [ ] Integration with official cricket board databases

---

## 🤝 Contributing

Contributions are welcome! Please open an issue first to discuss what you'd like to change.

1. Fork the repository
2. Create your feature branch: `git checkout -b feat/your-feature`
3. Commit your changes: `git commit -m 'feat: add your feature'`
4. Push to the branch: `git push origin feat/your-feature`
5. Open a Pull Request

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<div align="center">

Built with ❤️ by [Manish Prajapati](https://github.com/ManishPrajapati4712)

</div>
