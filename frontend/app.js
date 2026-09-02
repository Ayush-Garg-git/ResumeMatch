/**
 * Job Readiness Copilot - Main Application Logic
 * Truth-First Career Readiness Copilot
 */

import api, { getActiveJobId, setActiveJobId } from './api.js';

// Application State
const state = {
    user: api.getUser(),
    activeJobId: getActiveJobId(),
    jobs: [],
    profile: null,
    resumes: [],
    readiness: null,
    selectedBucket: 'ALL', // 'ALL' | 'DEMONSTRATED' | 'HIDDEN_BUT_LIKELY' | 'CONFIRMED_BUT_WEAK' | 'GENUINE_GAP'
    evidenceQuestions: [],
    tailoringSession: null,
    claims: [],
    tailoredVersions: [],
    roadmapMissions: [],
    interviewQuestions: [],
    truthBankConfirmed: false,
    isTruthBankStale: false,
    truthBankConfirmedTimestamp: null,
    loading: false,
    isDemoMode: false
};

// UI Notification Toast
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    
    let icon = 'fa-circle-info';
    if (type === 'success') icon = 'fa-circle-check';
    if (type === 'error') icon = 'fa-triangle-exclamation';

    toast.innerHTML = `<i class="fa-solid ${icon}"></i> <span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// Modal Utilities
function openModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) el.classList.remove('hidden');
}

function closeModal(modalId) {
    const el = document.getElementById(modalId);
    if (el) el.classList.add('hidden');
}

// Hash Routing Handler
function getRoute() {
    return window.location.hash.replace(/^#\/?/, '') || 'dashboard';
}

function navigateTo(route) {
    window.location.hash = `#${route}`;
}

// Expose utilities on window for inline HTML attributes
window.openModal = openModal;
window.closeModal = closeModal;
window.showToast = showToast;
window.navigateTo = navigateTo;

// Setup Event Listeners
function setupGlobalEvents() {
    window.addEventListener('hashchange', renderApp);

    document.querySelectorAll('.modal-close').forEach(btn => {
        btn.addEventListener('click', () => {
            const target = btn.getAttribute('data-target');
            if (target) closeModal(target);
        });
    });

    document.querySelectorAll('.link-signup').forEach(link => {
        link.addEventListener('click', () => {
            closeModal('login-modal');
            openModal('signup-modal');
        });
    });

    document.querySelectorAll('.link-login').forEach(link => {
        link.addEventListener('click', () => {
            closeModal('signup-modal');
            openModal('login-modal');
        });
    });

    // Login Form Submit
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const email = document.getElementById('login-email').value;
            const password = document.getElementById('login-password').value;
            try {
                const res = await api.login(email, password);
                state.user = res.user;
                closeModal('login-modal');
                showToast('Welcome back to Job Readiness Copilot!', 'success');
                await loadInitialData();
                window.location.hash = '#dashboard';
                renderApp();
            } catch (err) {
                showToast(err.message || 'Login failed', 'error');
            }
        });
    }

    // Signup Form Submit
    const signupForm = document.getElementById('signup-form');
    if (signupForm) {
        signupForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const first = document.getElementById('signup-first-name').value;
            const last = document.getElementById('signup-last-name').value;
            const email = document.getElementById('signup-email').value;
            const password = document.getElementById('signup-password').value;
            try {
                const res = await api.signup(email, password, first, last);
                state.user = res.user;
                closeModal('signup-modal');
                showToast('Account created successfully!', 'success');
                await loadInitialData();
                window.location.hash = '#dashboard';
                renderApp();
            } catch (err) {
                showToast(err.message || 'Signup failed', 'error');
            }
        });
    }

    // Preset JD Buttons in Job Creator Modal
    const btnPresetJava = document.getElementById('btn-preset-java');
    if (btnPresetJava) {
        btnPresetJava.addEventListener('click', () => {
            document.getElementById('job-title').value = 'Junior Java Backend Developer';
            document.getElementById('job-company').value = 'Amazon Cloud Services';
            document.getElementById('job-jd-text').value = `Responsibilities:
- Build, optimize, and maintain scalable microservices using Java 21, Spring Boot, and REST APIs.
- Work with PostgreSQL databases, writing efficient queries and database migrations.
- Containerize services with Docker and manage deployment pipelines.
- Implement unit tests with JUnit 5 and Mockito with >80% test coverage.

Requirements:
- Bachelor's degree in Computer Science or equivalent practical experience.
- Solid core Java fundamentals (OOP, Collections, Multithreading, Streams).
- Hands-on experience with Spring Boot framework, Hibernate / JPA.
- Familiarity with Git, RESTful architectural style, and CI/CD concepts.
- 1+ years experience or rigorous capstone project background.`;
        });
    }

    const btnPresetFullstack = document.getElementById('btn-preset-fullstack');
    if (btnPresetFullstack) {
        btnPresetFullstack.addEventListener('click', () => {
            document.getElementById('job-title').value = 'Full Stack Engineer Intern';
            document.getElementById('job-company').value = 'Stripe';
            document.getElementById('job-jd-text').value = `Responsibilities:
- Develop modern, responsive web interfaces using JavaScript / TypeScript and modern CSS.
- Design and integrate with backend APIs, handling data persistence and user sessions.
- Collaborate with engineers to improve platform stability and user experience.

Requirements:
- Strong knowledge of HTML5, CSS3, Modern JavaScript (ES6+).
- Experience building RESTful web applications.
- Familiarity with SQL databases (PostgreSQL or MySQL) and Git.
- Authorization to work in the US (Legal Requirement).`;
        });
    }

    // Job Form Submit (Modal)
    const jobForm = document.getElementById('job-form');
    if (jobForm) {
        jobForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const title = document.getElementById('job-title').value;
            const company = document.getElementById('job-company').value;
            const jd = document.getElementById('job-jd-text').value;

            const submitBtn = document.getElementById('btn-submit-job');
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin mr-2"></i> Analyzing Job...';
            }

            try {
                const newJob = await api.createJob(title, company, jd);
                state.activeJobId = newJob.id;
                setActiveJobId(newJob.id);
                showToast(`Job "${newJob.title}" queued for AI analysis!`, 'success');
                closeModal('job-modal');
                await loadInitialData();
                window.location.hash = '#dashboard';
                renderApp();

                if (newJob.status === 'PROCESSING') {
                    pollJobStatus(newJob.id);
                }
            } catch (err) {
                showToast(err.message || 'Job creation failed', 'error');
            } finally {
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = '<i class="fa-solid fa-wand-magic-sparkles mr-2"></i>Analyze with AI';
                }
            }
        });
    }

    // Resume File Upload (with drag-and-drop & browse button)
    const fileInput = document.getElementById('resume-file-input');
    const dropArea = document.getElementById('resume-drop-area');
    const btnBrowse = document.getElementById('btn-browse-resume');

    if (btnBrowse && fileInput) {
        btnBrowse.addEventListener('click', (e) => {
            e.stopPropagation();
            fileInput.click();
        });
    }

    if (dropArea && fileInput) {
        dropArea.addEventListener('click', (e) => {
            if (e.target !== btnBrowse && !btnBrowse?.contains(e.target)) {
                fileInput.click();
            }
        });

        ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
            dropArea.addEventListener(eventName, (e) => {
                e.preventDefault();
                e.stopPropagation();
            }, false);
        });

        ['dragenter', 'dragover'].forEach(eventName => {
            dropArea.addEventListener(eventName, () => dropArea.classList.add('dragover'), false);
        });

        ['dragleave', 'drop'].forEach(eventName => {
            dropArea.addEventListener(eventName, () => dropArea.classList.remove('dragover'), false);
        });

        dropArea.addEventListener('drop', (e) => {
            const dt = e.dataTransfer;
            const files = dt.files;
            if (files && files.length > 0) {
                fileInput.files = files;
                fileInput.dispatchEvent(new Event('change'));
            }
        }, false);
    }

    if (fileInput) {
        fileInput.addEventListener('change', async (e) => {
            const file = e.target.files[0];
            if (!file) return;

            const statusEl = document.getElementById('resume-upload-status');
            if (statusEl) statusEl.classList.remove('hidden');

            try {
                showToast('Uploading and parsing resume with AI...', 'info');
                await api.uploadResume(file);
                showToast('Resume parsed! Please review & confirm your Truth Bank.', 'success');
                closeModal('resume-modal');
                await loadInitialData();
                window.location.hash = '#profile';
                renderApp();
            } catch (err) {
                showToast(err.message || 'Resume upload failed', 'error');
            } finally {
                if (statusEl) statusEl.classList.add('hidden');
                fileInput.value = '';
            }
        });
    }

    // Evidence Question Submit Modal (Routed to Interview Defense & Missions, not exported bullets)
    const evidenceForm = document.getElementById('evidence-form');
    if (evidenceForm) {
        evidenceForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const skillName = document.getElementById('evidence-skill-name').value;
            const title = document.getElementById('evidence-title').value;
            const description = document.getElementById('evidence-description').value;

            try {
                await api.saveEvidence({
                    title,
                    description,
                    type: 'PROJECT',
                    source: 'USER_VERIFICATION',
                    strengthScore: 85
                });
                showToast(`Self-reported evidence recorded for interview defense!`, 'success');
                closeModal('evidence-modal');
                if (state.activeJobId) {
                    await performReadinessEvaluation();
                }
                renderApp();
            } catch (err) {
                showToast(err.message || 'Failed to save evidence', 'error');
            }
        });
    }

    // Proof Submission Form
    const proofForm = document.getElementById('proof-form');
    if (proofForm) {
        proofForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const missionId = document.getElementById('proof-mission-id').value;
            const proofType = document.getElementById('proof-type').value;
            const proofUrl = document.getElementById('proof-url').value;
            const proofContent = document.getElementById('proof-content').value;

            try {
                await api.submitProof(missionId, proofType, proofUrl, proofContent);
                showToast('Mission proof submitted successfully!', 'success');
                closeModal('proof-modal');
                if (state.activeJobId) {
                    const missions = await api.generateRoadmap(state.activeJobId).catch(() => []);
                    state.roadmapMissions = missions;
                }
                renderApp();
            } catch (err) {
                showToast(err.message || 'Proof submission failed', 'error');
            }
        });
    }
}

// Background Job Status Polling
const activePollingJobs = new Set();
function pollJobStatus(jobId) {
    if (activePollingJobs.has(jobId)) return;
    activePollingJobs.add(jobId);

    const interval = setInterval(async () => {
        try {
            const job = await api.getJob(jobId);
            if (!job) return;

            const idx = state.jobs.findIndex(j => j.id === jobId);
            if (idx !== -1) {
                state.jobs[idx] = job;
            }

            if (job.status === 'COMPLETED') {
                clearInterval(interval);
                activePollingJobs.delete(jobId);
                showToast(`🎉 Requirement extraction completed for "${job.title}"!`, 'success');
                await loadInitialData();
                renderApp();
            } else if (job.status === 'FAILED') {
                clearInterval(interval);
                activePollingJobs.delete(jobId);
                showToast(`Job analysis failed for "${job.title}".`, 'error');
                renderApp();
            }
        } catch (e) {
            console.error('Error polling job status:', e);
        }
    }, 2000);
}

// Initial Data Fetching
async function loadInitialData() {
    if (!api.isLoggedIn()) return;
    try {
        const [jobs, profile, resumes] = await Promise.all([
            api.getJobs().catch(() => []),
            api.getProfile().catch(() => null),
            api.getResumes().catch(() => [])
        ]);

        state.jobs = jobs || [];
        state.profile = profile;
        state.resumes = resumes || [];

        if (!state.activeJobId && state.jobs.length > 0) {
            state.activeJobId = state.jobs[0].id;
            setActiveJobId(state.jobs[0].id);
        }

        state.jobs.forEach(j => {
            if (j.status === 'PROCESSING') {
                pollJobStatus(j.id);
            }
        });

        if (state.activeJobId) {
            try {
                state.readiness = await api.getLatestAnalysis(state.activeJobId);
            } catch (e) {
                state.readiness = null;
            }
        }
    } catch (e) {
        console.error('Data loading error:', e);
    }
}

// Perform Live AI Readiness Analysis
async function performReadinessEvaluation() {
    if (!state.activeJobId) {
        showToast('Please select or create a target job first', 'info');
        openModal('job-modal');
        return;
    }

    state.loading = true;
    renderApp();

    try {
        showToast('Evaluating 4-bucket readiness against confirmed Truth Bank...', 'info');
        const res = await api.performReadiness(state.activeJobId);
        state.readiness = res;
        state.isTruthBankStale = false;
        showToast('Readiness analysis complete & synchronized!', 'success');
    } catch (err) {
        showToast(err.message || 'Analysis failed', 'error');
    } finally {
        state.loading = false;
        renderApp();
    }
}
window.performReadinessEvaluation = performReadinessEvaluation;

// Invalidate Downstream State when Truth Bank changes
function invalidateTruthBankState() {
    state.truthBankConfirmed = false;
    state.isTruthBankStale = true;
    state.claims = state.claims.filter(c => c.evidenceType === 'RESUME_VERIFIED');
    showToast('Truth Bank modified. Downstream matches flagged as stale.', 'info');
}

// Helper: Branch Structural Constraints into Skill-Adjacent vs Binary Eligibility
function getStructuralGuidance(skillName, description) {
    const text = ((skillName || '') + ' ' + (description || '')).toLowerCase();
    const isBinaryEligibility = text.includes('clearance') || text.includes('citizen') || text.includes('visa') || 
                                text.includes('authorization') || text.includes('security') || text.includes('license') ||
                                text.includes('authorized') || text.includes('sponsorship') || text.includes('degree');
    
    if (isBinaryEligibility) {
        return {
            type: 'BINARY_ELIGIBILITY',
            badge: '<span class="badge badge-structural-gap"><i class="fa-solid fa-lock mr-1"></i> Hard Eligibility Requirement</span>',
            title: '🔒 Hard Eligibility Constraint',
            guidance: '<strong>🔒 Hard Eligibility Requirement:</strong> This is a strict non-negotiable prerequisite (e.g., U.S. Citizenship, security clearance, or work authorization). Mini-projects or cover letter framing cannot satisfy legal constraints. We recommend confirming flexibility with the recruiter/employer before investing time in tailoring.'
        };
    } else {
        return {
            type: 'EXPERIENCE_SENIORITY',
            badge: '<span class="badge badge-structural-gap"><i class="fa-solid fa-compass mr-1"></i> Experience / Seniority Requirement</span>',
            title: '💡 Strategic Experience Angle',
            guidance: `<strong>💡 Strategic Experience Angle:</strong> This role mentions ${skillName}. For college graduates and junior engineers, intensive internships, open-source work, and complex capstones often demonstrate equivalent practical depth. Highlight architectural complexity in your project bullets and interview defense.`
        };
    }
}

// Load Rich Sample Demo Data
function loadSampleDemoData() {
    state.isDemoMode = true;
    state.truthBankConfirmed = true;
    state.isTruthBankStale = false;
    state.truthBankConfirmedTimestamp = new Date().toLocaleDateString() + ' ' + new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    
    // Sample user & profile
    state.profile = {
        headline: 'Computer Science Graduate & Junior Backend Developer',
        targetRoles: 'Junior Java Developer, Backend Engineer, Full Stack Intern',
        summary: 'Graduated B.S. Computer Science. Built scalable microservices in Java 21, Spring Boot, and PostgreSQL with Docker deployments.',
        verifiedSkills: ['Java 21', 'Spring Boot', 'PostgreSQL', 'Docker', 'REST APIs', 'Git', 'JUnit 5', 'Hibernate', 'Flyway', 'Maven']
    };

    // Sample master resume
    state.resumes = [
        {
            id: 'demo-resume-1',
            fileName: 'Alex_Chen_Master_Resume.pdf',
            createdAt: new Date().toISOString(),
            parsedSkills: ['Java 21', 'Spring Boot', 'PostgreSQL', 'Docker', 'REST APIs', 'Git', 'JUnit 5', 'Hibernate', 'Flyway']
        }
    ];

    // Sample Target Jobs (Multiple jobs for comparison matrix)
    const demoJob1 = {
        id: 'demo-job-1',
        title: 'Junior Java Backend Developer',
        company: 'Amazon Web Services',
        status: 'COMPLETED',
        createdAt: new Date().toISOString(),
        rawJdText: 'Build, optimize, and maintain scalable microservices using Java 21, Spring Boot, REST APIs, PostgreSQL, and Docker containerization.',
        requirements: [
            { skillName: 'Java 21 / OOP Fundamentals', type: 'MUST_HAVE', description: 'Strong core Java, OOP, Collections, and modern streams' },
            { skillName: 'Spring Boot & REST APIs', type: 'MUST_HAVE', description: 'Design and implement RESTful endpoints and microservices' },
            { skillName: 'PostgreSQL & SQL Databases', type: 'MUST_HAVE', description: 'Relational data modeling, indexing, and Flyway migrations' },
            { skillName: 'Docker & Containerization', type: 'PREFERRED', description: 'Creating Dockerfiles and running services in containers' },
            { skillName: 'Unit Testing (JUnit 5 / Mockito)', type: 'PREFERRED', description: 'Automated unit tests with >80% coverage' },
            { skillName: 'CI/CD & Kubernetes', type: 'PREFERRED', description: 'Deployment pipelines and cloud container orchestration' },
            { skillName: '3+ Years Industry Experience', type: 'STRUCTURAL_CONSTRAINT', description: 'Minimum 3 years full-time backend software development experience' }
        ]
    };

    const demoJob2 = {
        id: 'demo-job-2',
        title: 'Full Stack Engineer Intern',
        company: 'Stripe',
        status: 'COMPLETED',
        createdAt: new Date().toISOString(),
        rawJdText: 'Develop web apps with TypeScript, React, RESTful backend APIs, and SQL persistence.',
        requirements: [
            { skillName: 'RESTful Web APIs', type: 'MUST_HAVE', description: 'Building and consuming REST APIs' },
            { skillName: 'TypeScript & JavaScript', type: 'MUST_HAVE', description: 'Frontend web application development' },
            { skillName: 'US Work Authorization', type: 'STRUCTURAL_CONSTRAINT', description: 'Must be legally authorized to work in the US without current or future sponsorship requirement' }
        ]
    };

    state.jobs = [demoJob1, demoJob2];
    state.activeJobId = demoJob1.id;
    setActiveJobId(demoJob1.id);

    // Sample 4-Bucket Readiness Diagnosis (with Closable vs Structural split)
    state.readiness = {
        readinessScore: 84,
        recommendationCategory: 'MATCH_READY_WITH_TAILORING',
        strengthsSummary: 'Solid core Java 21 fundamentals, Spring Boot REST APIs, and PostgreSQL project evidence fully verified in your coursework repository.',
        gapsSummary: 'Kubernetes orchestration and CI/CD pipelines are closable via mini-projects. Note the structural 3+ years experience requirement.',
        estimatedTimeToCloseHours: 6,
        skills: [
            { skillName: 'Java 21 / OOP Fundamentals', status: 'DEMONSTRATED', evidenceType: 'RESUME_VERIFIED', importance: 'MUST_HAVE', reasoning: 'Verified through Coursework Capstone Project with 15k+ lines of clean Java.', existingEvidenceDesc: 'E-Commerce Platform Capstone (GitHub repo)' },
            { skillName: 'Spring Boot & REST APIs', status: 'DEMONSTRATED', evidenceType: 'RESUME_VERIFIED', importance: 'MUST_HAVE', reasoning: 'Implemented 12 REST endpoints with Spring Security and JWT auth.', existingEvidenceDesc: 'REST API Controller & Service tests' },
            { skillName: 'PostgreSQL & SQL Databases', status: 'DEMONSTRATED', evidenceType: 'RESUME_VERIFIED', importance: 'MUST_HAVE', reasoning: 'Designed normalized schema with Flyway migrations in PostgreSQL.', existingEvidenceDesc: 'Database schema migration scripts' },
            { skillName: 'Unit Testing (JUnit 5 / Mockito)', status: 'DEMONSTRATED', evidenceType: 'RESUME_VERIFIED', importance: 'PREFERRED', reasoning: 'Wrote unit test suite covering business rules with Mockito mocks.', existingEvidenceDesc: 'JUnit 5 automated test suite' },
            { skillName: 'Docker & Containerization', status: 'HIDDEN_BUT_LIKELY', evidenceType: 'SELF_REPORTED', importance: 'PREFERRED', reasoning: 'Your backend project uses Dockerfile and Compose, confirmed via verification Q&A.', existingEvidenceDesc: 'Self-reported: docker-compose.yml local setup' },
            { skillName: 'Git & Agile Collaboration', status: 'CONFIRMED_BUT_WEAK', evidenceType: 'SELF_REPORTED', importance: 'PREFERRED', reasoning: 'Mentioned Git commits in capstone, but lacked explicit PR workflow details.', existingEvidenceDesc: 'Self-reported: GitHub repository commit history' },
            { skillName: 'CI/CD & Cloud Pipelines', status: 'GENUINE_GAP', gapType: 'LEARNABLE_GAP', importance: 'PREFERRED', reasoning: 'No evidence found for GitHub Actions or automated deployment pipelines.', existingEvidenceDesc: null },
            { skillName: '3+ Years Industry Experience', status: 'GENUINE_GAP', gapType: 'STRUCTURAL_CONSTRAINT', importance: 'MUST_HAVE', reasoning: 'Structural constraint: Job lists 3+ years full-time experience.', existingEvidenceDesc: null }
        ]
    };

    // Sample Evidence Questions
    state.evidenceQuestions = [
        {
            targetSkill: 'Docker & Containerization',
            question: 'Did you containerize and run your Spring Boot application and PostgreSQL database with Docker Compose during your capstone project?',
            rationale: 'Confirming Docker experience directly bridges a preferred qualification for the Amazon role.'
        }
    ];

    // Tailored claims — STRICTLY Resume-Verified for resume bullets
    state.claims = [
        {
            id: 'claim-1',
            status: 'APPROVED',
            evidenceType: 'RESUME_VERIFIED',
            originalText: 'Built a Java Spring Boot backend for an online shopping website with a PostgreSQL database.',
            proposedText: 'Architected and deployed a modular Spring Boot REST API backed by PostgreSQL, managing 12 transactional endpoints with Flyway database migrations.',
            evidenceContext: 'Verified in University E-Commerce Capstone Project'
        },
        {
            id: 'claim-2',
            status: 'APPROVED',
            evidenceType: 'RESUME_VERIFIED',
            originalText: 'Wrote tests for the project using JUnit.',
            proposedText: 'Authored comprehensive unit and integration test suites using JUnit 5 and Mockito, ensuring test reliability across core business logic.',
            evidenceContext: 'Verified in Automated Test Suite Repository'
        },
        {
            id: 'claim-3',
            status: 'APPROVED',
            evidenceType: 'RESUME_VERIFIED',
            originalText: 'Used Docker to run the database.',
            proposedText: 'Containerized microservices and database environments utilizing multi-stage Docker builds and Docker Compose for local development parity.',
            evidenceContext: 'Verified in Dockerfile & Compose Configuration'
        }
    ];

    // Sample Saved Tailored Versions with Truth Bank Snapshot Label
    state.tailoredVersions = [
        {
            id: 'v1',
            name: 'Amazon AWS Junior Java Backend (v1)',
            truthBankDate: state.truthBankConfirmedTimestamp,
            date: new Date().toLocaleDateString(),
            jobTitle: 'Junior Java Backend Developer'
        }
    ];

    // Learning Missions (Only for Learnable Gaps)
    state.roadmapMissions = [
        {
            id: 'mission-1',
            title: 'Build a GitHub Actions CI/CD Pipeline for Java Spring Boot',
            status: 'IN_PROGRESS',
            gapType: 'LEARNABLE_GAP',
            explanation: 'Bridges the closable gap for CI/CD and deployment automation required by Amazon Web Services.',
            miniProjectDesc: 'Create a `.github/workflows/maven.yml` pipeline that triggers on push, compiles the Java code, runs JUnit 5 tests, and builds a container image.',
            docLinks: 'GitHub Actions for Java & Maven (Official Documentation)'
        }
    ];

    // Interview Defense Questions
    state.interviewQuestions = [
        {
            id: 'iq-1',
            questionText: 'Can you walk us through how you handled database schema migrations and transactions in your Spring Boot and PostgreSQL project?',
            suggestedPoints: 'Explain Flyway versioned migrations (V1, V2), @Transactional boundaries in Spring service methods, and how database integrity was guaranteed.'
        },
        {
            id: 'iq-2',
            questionText: 'How did you structure your JUnit 5 and Mockito tests to isolate the service layer from database dependencies?',
            suggestedPoints: 'Highlight Mockito @Mock and @InjectMocks patterns, verifying method invocation counts, and testing edge cases like duplicate email registrations.'
        }
    ];

    showToast('Loaded complete sample demonstration data!', 'success');
    renderApp();
}

// Main Render Function
async function renderApp() {
    const root = document.getElementById('app-root');
    if (!root) return;

    if (!api.isLoggedIn() && !state.isDemoMode) {
        root.innerHTML = renderLandingPage();
        attachLandingEvents();
        if (window.location.hash === '#login') {
            openModal('login-modal');
        }
        return;
    }

    const route = getRoute();
    const activeJob = state.jobs.find(j => j.id === state.activeJobId) || null;

    const hasResume = state.resumes && state.resumes.length > 0;
    const hasJob = state.jobs && state.jobs.length > 0 && !!state.activeJobId;
    const hasAnalysis = !!state.readiness;
    const isTruthConfirmed = state.truthBankConfirmed;

    root.innerHTML = `
        <div class="app-container">
            <!-- App Header -->
            <header class="app-header">
                <div class="brand-container" onclick="window.location.hash='#dashboard'" style="cursor: pointer;">
                    <div class="brand-icon"><i class="fa-solid fa-compass"></i></div>
                    <div class="brand-title">Copilot<span class="brand-version">AI-Powered</span></div>
                </div>

                <!-- Active Job Switcher -->
                <div class="header-active-job">
                    <span class="text-muted"><i class="fa-solid fa-crosshairs mr-1"></i> Target Role:</span>
                    ${state.jobs.length === 0 ? `
                        <span class="badge" style="background: rgba(255,255,255,0.05); color: var(--text-muted); border: 1px solid var(--border-color); padding: 0.3rem 0.75rem;">
                            No Active Job
                        </span>
                    ` : `
                        <select id="active-job-select" style="background: transparent; border: none; color: #FFF; font-weight: 600; cursor: pointer; outline: none;">
                            ${state.jobs.map(j => `<option value="${j.id}" ${j.id === state.activeJobId ? 'selected' : ''}>${j.title} (${j.company || 'Direct'})</option>`).join('')}
                        </select>
                        <button class="btn btn-secondary btn-sm" id="btn-header-new-job" title="Add Another Target Job" style="padding: 0.2rem 0.6rem;">
                            <i class="fa-solid fa-plus"></i>
                        </button>
                    `}
                </div>

                <!-- User Profile Badge -->
                <div class="header-actions">
                    <div class="user-profile-badge">
                        <div class="user-avatar">${(state.user?.firstName || 'A')[0].toUpperCase()}</div>
                        <span>${state.user?.firstName || (state.isDemoMode ? 'Alex Chen' : 'Student')}</span>
                    </div>
                </div>
            </header>

            <!-- App Body -->
            <div class="app-body">
                <!-- Sidebar Navigation with Prerequisite Locking -->
                <aside class="app-sidebar">
                    <div class="sidebar-heading">Foundation</div>
                    <a href="#dashboard" class="nav-link ${route === 'dashboard' ? 'active' : ''}">
                        <i class="fa-solid fa-chart-pie"></i> Readiness Hub
                    </a>
                    <a href="#profile" class="nav-link ${route === 'profile' ? 'active' : ''}">
                        <i class="fa-solid fa-id-badge"></i> Profile & Truth Bank
                    </a>
                    <a href="#jobs" class="nav-link ${route === 'jobs' ? 'active' : ''}">
                        <i class="fa-solid fa-briefcase"></i> Target Jobs & Comparison
                    </a>

                    <div class="sidebar-heading">Action Hub</div>
                    
                    <!-- Evidence Discovery (Requires Resume + Job) -->
                    <a href="#evidence" class="nav-link ${route === 'evidence' ? 'active' : ''} ${(!hasResume || !hasJob) ? 'locked' : ''}">
                        <i class="fa-solid fa-magnifying-glass-chart"></i> Evidence Discovery
                        ${(!hasResume || !hasJob) ? '<span class="nav-lock-badge"><i class="fa-solid fa-lock"></i> Locked</span>' : ''}
                    </a>

                    <!-- Tailor & Verify Claims (Requires Resume + Job) -->
                    <a href="#tailoring" class="nav-link ${route === 'tailoring' ? 'active' : ''} ${(!hasResume || !hasJob) ? 'locked' : ''}">
                        <i class="fa-solid fa-file-signature"></i> Tailor & Verify Claims
                        ${(!hasResume || !hasJob) ? '<span class="nav-lock-badge"><i class="fa-solid fa-lock"></i> Locked</span>' : ''}
                    </a>

                    <!-- Learning Roadmap (Requires Analysis) -->
                    <a href="#roadmap" class="nav-link ${route === 'roadmap' ? 'active' : ''} ${!hasAnalysis ? 'locked' : ''}">
                        <i class="fa-solid fa-graduation-cap"></i> Learning Roadmap
                        ${!hasAnalysis ? '<span class="nav-lock-badge"><i class="fa-solid fa-lock"></i> Locked</span>' : ''}
                    </a>

                    <!-- Interview Defense (Requires Analysis) -->
                    <a href="#interview" class="nav-link ${route === 'interview' ? 'active' : ''} ${!hasAnalysis ? 'locked' : ''}">
                        <i class="fa-solid fa-comments"></i> Interview Defense
                        ${!hasAnalysis ? '<span class="nav-lock-badge"><i class="fa-solid fa-lock"></i> Locked</span>' : ''}
                    </a>

                    <!-- Neutral Sign Out Button -->
                    <div style="margin-top: auto; padding-top: 1.5rem; border-top: 1px solid var(--border-color);">
                        <button class="btn btn-signout btn-sm w-full" id="btn-sidebar-logout" style="justify-content: center;">
                            <i class="fa-solid fa-right-from-bracket mr-2"></i> Sign Out
                        </button>
                    </div>
                </aside>

                <!-- Main View Area -->
                <main class="app-main">
                    ${renderCurrentView(route, activeJob, hasResume, isTruthConfirmed, hasJob, hasAnalysis)}
                </main>
            </div>
        </div>
    `;

    attachAppEvents(route);
}

// Render Routing Switch
function renderCurrentView(route, activeJob, hasResume, isTruthConfirmed, hasJob, hasAnalysis) {
    if (state.loading) {
        return `
            <div class="empty-state">
                <i class="fa-solid fa-circle-notch fa-spin empty-state-icon text-gradient"></i>
                <h3>AI Reasoning Engine is Active...</h3>
                <p>Analyzing job requirements against verified candidate evidence and computing 4-bucket readiness diagnosis.</p>
            </div>
        `;
    }

    if ((route === 'evidence' || route === 'tailoring') && (!hasResume || !hasJob)) {
        return renderLockedRouteView('Resume & Target Job Required', 'Evidence Discovery & Resume Tailoring require both an uploaded master resume and a target job description to match against.');
    }

    if ((route === 'roadmap' || route === 'interview') && !hasAnalysis) {
        return renderLockedRouteView('Readiness Diagnosis Required', 'Learning Missions and Interview Defense are generated from the gap analysis of your active target job.');
    }

    switch (route) {
        case 'dashboard':
            return renderDashboardView(activeJob, hasResume, isTruthConfirmed, hasJob, hasAnalysis);
        case 'profile':
            return renderProfileView();
        case 'jobs':
            return renderJobsView();
        case 'evidence':
            return renderEvidenceView(activeJob);
        case 'tailoring':
            return renderTailoringView(activeJob);
        case 'roadmap':
            return renderRoadmapView(activeJob);
        case 'interview':
            return renderInterviewView(activeJob);
        default:
            return renderDashboardView(activeJob, hasResume, isTruthConfirmed, hasJob, hasAnalysis);
    }
}

// Locked Route Prerequisite Banner
function renderLockedRouteView(title, description) {
    return `
        <div class="card card-glow text-center" style="max-width: 650px; margin: 3rem auto; padding: 2.5rem;">
            <div style="width: 60px; height: 60px; border-radius: 50%; background: rgba(99, 102, 241, 0.15); border: 1px solid var(--accent-indigo); display: inline-flex; align-items: center; justify-content: center; font-size: 1.5rem; color: var(--accent-indigo); margin-bottom: 1.25rem;">
                <i class="fa-solid fa-lock"></i>
            </div>
            <h3 style="font-size: 1.4rem; margin-bottom: 0.5rem;">${title}</h3>
            <p class="text-secondary mb-6" style="line-height: 1.6;">${description}</p>
            
            <div style="display: flex; gap: 1rem; justify-content: center; flex-wrap: wrap;">
                <button class="btn btn-primary" onclick="window.location.hash='#dashboard'">
                    <i class="fa-solid fa-arrow-left mr-2"></i> Go to Readiness Hub
                </button>
                <button class="btn btn-demo-cta btn-load-sample-demo">
                    <i class="fa-solid fa-wand-magic-sparkles mr-2"></i> Load Demo Data (Unlock Everything)
                </button>
            </div>
        </div>
    `;
}

// 1. Landing Page HTML
function renderLandingPage() {
    return `
        <div class="landing-page">
            <header class="landing-header">
                <div class="brand-container">
                    <div class="brand-icon"><i class="fa-solid fa-compass"></i></div>
                    <div class="brand-title">Job Readiness Copilot</div>
                </div>
                <div>
                    <button class="btn btn-secondary mr-2" id="btn-open-login">Sign In</button>
                    <button class="btn btn-primary" id="btn-open-signup">Get Started Free</button>
                </div>
            </header>

            <main class="landing-hero">
                <div class="hero-tag"><i class="fa-solid fa-sparkles"></i> AI-Powered &bull; Truth-First Architecture</div>
                <h1 class="hero-title">The <span class="text-gradient">Truth-First</span> Copilot for College Graduates</h1>
                <p class="hero-desc">
                    Stop submitting generic, unverified resumes. Confirm your verified background in a Truth Bank, uncover hidden capabilities, and bridge real gaps with learning missions.
                </p>
                <div class="hero-ctas">
                    <button class="btn btn-primary btn-lg" id="btn-hero-start">
                        <i class="fa-solid fa-bolt mr-2"></i>Launch Copilot
                    </button>
                    <button class="btn btn-demo-cta btn-lg btn-load-sample-demo">
                        <i class="fa-solid fa-wand-magic-sparkles mr-2"></i>Try with Demo Example
                    </button>
                </div>

                <div class="workflow-steps-preview">
                    <div class="flow-step-card cursor-pointer" onclick="window.location.hash='#profile'; if(!api.isLoggedIn()) loadSampleDemoData();">
                        <div class="step-num">Step 01</div>
                        <h4 class="step-title"><i class="fa-solid fa-shield-halved mr-1 text-success"></i> Truth Bank Review</h4>
                        <p class="step-desc">Upload your resume. Review and confirm verified skills before matching begins.</p>
                    </div>
                    <div class="flow-step-card cursor-pointer" onclick="window.location.hash='#jobs'; if(!api.isLoggedIn()) loadSampleDemoData();">
                        <div class="step-num">Step 02</div>
                        <h4 class="step-title"><i class="fa-solid fa-briefcase mr-1 text-primary"></i> Target Job Match</h4>
                        <p class="step-desc">Paste any Job Description. AI separates closable technical skills from structural constraints.</p>
                    </div>
                    <div class="flow-step-card cursor-pointer" onclick="window.location.hash='#dashboard'; if(!api.isLoggedIn()) loadSampleDemoData();">
                        <div class="step-num">Step 03</div>
                        <h4 class="step-title"><i class="fa-solid fa-chart-pie mr-1 text-warning"></i> 4-Bucket Diagnosis</h4>
                        <p class="step-desc">Clear breakdown of Demonstrated, Hidden, Weak, and Genuine Gap requirements.</p>
                    </div>
                    <div class="flow-step-card cursor-pointer" onclick="window.location.hash='#tailoring'; if(!api.isLoggedIn()) loadSampleDemoData();">
                        <div class="step-num">Step 04</div>
                        <h4 class="step-title"><i class="fa-solid fa-wand-magic-sparkles mr-1 text-secondary"></i> Tailor & Defend</h4>
                        <p class="step-desc">Tailor bullets sourced strictly from verified evidence, export clean ATS PDF, and defend claims.</p>
                    </div>
                </div>
            </main>
        </div>
    `;
}

// 2. Dashboard / Readiness Hub View (With 4-Step Onboarding Tracker & Stale Invalidation Notice)
function renderDashboardView(activeJob, hasResume, isTruthConfirmed, hasJob, hasAnalysis) {
    let progressPercent = 0;
    if (hasResume) progressPercent += 25;
    if (isTruthConfirmed) progressPercent += 25;
    if (hasJob) progressPercent += 25;
    if (hasAnalysis) progressPercent += 25;

    // Truth Bank Stale Invalidation Alert
    const staleAlertHtml = state.isTruthBankStale ? `
        <div class="card mb-6" style="background: rgba(245, 158, 11, 0.1); border: 1px solid rgba(245, 158, 11, 0.4);">
            <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.75rem;">
                <div style="display: flex; align-items: center; gap: 0.75rem;">
                    <i class="fa-solid fa-triangle-exclamation text-warning" style="font-size: 1.4rem;"></i>
                    <div>
                        <strong style="color: #FBBF24;">Truth Bank Modified</strong>
                        <p class="text-secondary" style="font-size: 0.85rem; margin: 0;">
                            Your confirmed skills changed. Re-run readiness evaluation to sync your downstream evidence matches and tailored claims.
                        </p>
                    </div>
                </div>
                <button class="btn btn-warning btn-sm" onclick="performReadinessEvaluation()">
                    <i class="fa-solid fa-rotate mr-1"></i> Re-Sync Readiness
                </button>
            </div>
        </div>
    ` : '';

    if (!activeJob) {
        return `
            ${staleAlertHtml}

            <!-- 4-Step Onboarding Progress Tracker Card -->
            <div class="onboarding-tracker">
                <div class="onboarding-header">
                    <div>
                        <h3 style="font-size: 1.25rem; margin-bottom: 0.25rem;"><i class="fa-solid fa-list-check mr-2 text-cyan"></i>4-Step Readiness Path</h3>
                        <p class="text-secondary" style="font-size: 0.85rem;">Follow the sequence below to evaluate your true qualifications against any job.</p>
                    </div>
                    <button class="btn btn-demo-cta btn-sm btn-load-sample-demo">
                        <i class="fa-solid fa-wand-magic-sparkles mr-1"></i> Try with Demo Example
                    </button>
                </div>

                <div class="onboarding-progress-bar-bg">
                    <div class="onboarding-progress-bar-fill" style="width: ${progressPercent}%;"></div>
                </div>

                <div class="onboarding-steps-grid" style="grid-template-columns: repeat(4, 1fr);">
                    <!-- Step 1 -->
                    <div class="onboarding-step-card ${hasResume ? 'completed' : 'active'}">
                        <div>
                            <div style="display: flex; align-items: center; margin-bottom: 0.5rem;">
                                <span class="step-badge-indicator ${hasResume ? 'done' : 'pending'}">${hasResume ? '<i class="fa-solid fa-check"></i>' : '1'}</span>
                                <strong style="font-size: 0.9rem;">Upload Resume</strong>
                            </div>
                            <p class="text-secondary" style="font-size: 0.75rem; line-height: 1.4;">Extract projects and background.</p>
                        </div>
                        <div class="mt-3">
                            ${hasResume ? `
                                <span class="badge badge-demonstrated"><i class="fa-solid fa-check mr-1"></i> Uploaded</span>
                            ` : `
                                <button class="btn btn-secondary btn-sm w-full" onclick="openModal('resume-modal')">
                                    Upload
                                </button>
                            `}
                        </div>
                    </div>

                    <!-- Step 2 (Checkpoint) -->
                    <div class="onboarding-step-card ${isTruthConfirmed ? 'completed' : (hasResume ? 'active' : '')}">
                        <div>
                            <div style="display: flex; align-items: center; margin-bottom: 0.5rem;">
                                <span class="step-badge-indicator ${isTruthConfirmed ? 'done' : 'pending'}">${isTruthConfirmed ? '<i class="fa-solid fa-check"></i>' : '2'}</span>
                                <strong style="font-size: 0.9rem;">Confirm Truth Bank</strong>
                            </div>
                            <p class="text-secondary" style="font-size: 0.75rem; line-height: 1.4;">Review and verify parsed skills.</p>
                        </div>
                        <div class="mt-3">
                            ${isTruthConfirmed ? `
                                <span class="badge badge-demonstrated"><i class="fa-solid fa-check mr-1"></i> Confirmed</span>
                            ` : `
                                <button class="btn btn-secondary btn-sm w-full" onclick="window.location.hash='#profile'">
                                    Review
                                </button>
                            `}
                        </div>
                    </div>

                    <!-- Step 3 -->
                    <div class="onboarding-step-card ${hasJob ? 'completed' : (isTruthConfirmed ? 'active' : '')}">
                        <div>
                            <div style="display: flex; align-items: center; margin-bottom: 0.5rem;">
                                <span class="step-badge-indicator ${hasJob ? 'done' : 'pending'}">${hasJob ? '<i class="fa-solid fa-check"></i>' : '3'}</span>
                                <strong style="font-size: 0.9rem;">Add Target Job</strong>
                            </div>
                            <p class="text-secondary" style="font-size: 0.75rem; line-height: 1.4;">Paste job posting below.</p>
                        </div>
                        <div class="mt-3">
                            ${hasJob ? `
                                <span class="badge badge-demonstrated"><i class="fa-solid fa-check mr-1"></i> Added</span>
                            ` : `
                                <span class="text-muted" style="font-size: 0.75rem;">Paste Below &darr;</span>
                            `}
                        </div>
                    </div>

                    <!-- Step 4 -->
                    <div class="onboarding-step-card ${hasAnalysis ? 'completed' : ''}">
                        <div>
                            <div style="display: flex; align-items: center; margin-bottom: 0.5rem;">
                                <span class="step-badge-indicator ${hasAnalysis ? 'done' : 'pending'}">${hasAnalysis ? '<i class="fa-solid fa-check"></i>' : '4'}</span>
                                <strong style="font-size: 0.9rem;">4-Bucket Diagnosis</strong>
                            </div>
                            <p class="text-secondary" style="font-size: 0.75rem; line-height: 1.4;">Evidence score & missions.</p>
                        </div>
                        <div class="mt-3">
                            ${hasAnalysis ? `
                                <span class="badge badge-demonstrated"><i class="fa-solid fa-check mr-1"></i> Diagnosed</span>
                            ` : `
                                <span class="text-muted" style="font-size: 0.75rem;">Unlocks after job</span>
                            `}
                        </div>
                    </div>
                </div>
            </div>

            <!-- Inline Quick Job Analyzer Card -->
            <div class="quick-analyzer-card">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; flex-wrap: wrap; gap: 0.5rem;">
                    <div>
                        <h3 style="font-size: 1.3rem; margin-bottom: 0.25rem;"><i class="fa-solid fa-bolt mr-2 text-gradient"></i>Quick Target Job Analyzer</h3>
                        <p class="text-secondary" style="font-size: 0.85rem;">Paste any job description directly here to extract skills and diagnose readiness without extra clicks.</p>
                    </div>
                </div>

                <!-- Preset Quick Buttons -->
                <div class="preset-pills-container">
                    <span class="text-muted" style="font-size: 0.8rem;">Try Sample Role:</span>
                    <button type="button" class="preset-pill-btn" id="btn-quick-preset-java">
                        <i class="fa-brands fa-java text-warning"></i> Junior Java Backend (Amazon)
                    </button>
                    <button type="button" class="preset-pill-btn" id="btn-quick-preset-fullstack">
                        <i class="fa-brands fa-react text-cyan"></i> Full Stack Intern (Stripe)
                    </button>
                </div>

                <form id="inline-job-form">
                    <div class="form-row">
                        <div class="form-group">
                            <label for="inline-job-title">Job Title / Role</label>
                            <input type="text" id="inline-job-title" required placeholder="e.g. Junior Java Backend Developer">
                        </div>
                        <div class="form-group">
                            <label for="inline-job-company">Company / Organization</label>
                            <input type="text" id="inline-job-company" required placeholder="e.g. Amazon / Tech Corp">
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="inline-job-jd-text">Job Description Text</label>
                        <textarea id="inline-job-jd-text" rows="6" required placeholder="Paste the job requirements, responsibilities, and qualifications here..."></textarea>
                    </div>

                    <div style="display: flex; gap: 1rem; align-items: center; flex-wrap: wrap;">
                        <button type="submit" id="btn-inline-submit-job" class="btn btn-primary btn-lg">
                            <i class="fa-solid fa-wand-magic-sparkles mr-2"></i> Analyze Target Job
                        </button>
                        <button type="button" class="btn btn-demo-cta btn-lg btn-load-sample-demo">
                            <i class="fa-solid fa-bolt mr-2"></i> Try with Demo Example
                        </button>
                    </div>
                </form>
            </div>

            <!-- 4-Bucket Truth Matrix Explainer Section -->
            <div class="bucket-info-section">
                <div style="display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.5rem;">
                    <i class="fa-solid fa-circle-info text-cyan"></i>
                    <h3 style="font-size: 1.15rem; margin: 0;">How the 4-Bucket Readiness Diagnosis Works</h3>
                </div>
                <p class="text-secondary" style="font-size: 0.85rem; margin-bottom: 1rem;">
                    Unlike generic keyword counters, our truth-first engine categorizes every job requirement against your verified background into four precise buckets:
                </p>

                <div class="bucket-info-grid">
                    <div class="bucket-info-card demonstrated">
                        <h4><i class="fa-solid fa-circle-check text-success"></i> 1. Demonstrated</h4>
                        <p>Skills directly backed by tangible evidence in your master resume or confirmed projects.</p>
                    </div>
                    <div class="bucket-info-card hidden-bucket">
                        <h4><i class="fa-solid fa-eye-slash text-warning"></i> 2. Hidden But Likely</h4>
                        <p>Competencies implied by your background that you likely possess. Unlocked with targeted Q&A.</p>
                    </div>
                    <div class="bucket-info-card weak-bucket">
                        <h4><i class="fa-solid fa-shield-halved" style="color: #fb923c;"></i> 3. Confirmed / Weak</h4>
                        <p>Self-declared skills that need stronger project artifacts or measurable results to defend.</p>
                    </div>
                    <div class="bucket-info-card gap-bucket">
                        <h4><i class="fa-solid fa-circle-xmark text-danger"></i> 4. Genuine Gap</h4>
                        <p>Missing requirements. Separated into closable technical gaps vs structural hard constraints.</p>
                    </div>
                </div>
            </div>
        `;
    }

    if (activeJob.status === 'PROCESSING') {
        return `
            <div class="card card-glow mb-6" style="border-left: 4px solid #facc15;">
                <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem;">
                    <div>
                        <h2>${activeJob.title}</h2>
                        <p class="text-secondary">${activeJob.company || 'Direct Employer'}</p>
                    </div>
                    <span class="badge" style="background: rgba(234, 179, 8, 0.15); color: #facc15; border: 1px solid rgba(234, 179, 8, 0.4); padding: 0.5rem 1rem; font-size: 0.9rem;">
                        <i class="fa-solid fa-spinner fa-spin mr-2"></i>Extracting Requirements...
                    </span>
                </div>
            </div>
            <div class="empty-state">
                <i class="fa-solid fa-wand-magic-sparkles fa-spin empty-state-icon text-gradient"></i>
                <h3>AI Requirement Extraction in Progress</h3>
                <p>Your Job Description is being parsed in the background. Requirements, qualifications, and soft skills will appear automatically momentarily.</p>
            </div>
        `;
    }

    const r = state.readiness;
    const reqs = activeJob.requirements || [];

    if (!r) {
        return `
            <div class="card card-glow mb-6">
                <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem;">
                    <div>
                        <div style="display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.25rem;">
                            <h2 style="margin: 0;">${activeJob.title}</h2>
                            <span class="badge badge-demonstrated"><i class="fa-solid fa-circle-check mr-1"></i> Requirements Extracted</span>
                        </div>
                        <p class="text-secondary">${activeJob.company || 'Direct Employer'} &bull; ${reqs.length} Extracted Requirements</p>
                    </div>
                    <button class="btn btn-primary" id="btn-run-analysis" style="font-size: 1rem; padding: 0.75rem 1.5rem;">
                        <i class="fa-solid fa-wand-magic-sparkles mr-2"></i>Run 4-Bucket Readiness Evaluation
                    </button>
                </div>
            </div>
        `;
    }

    const skills = r.skills || [];
    const demonstrated = skills.filter(s => s.status === 'DEMONSTRATED');
    const hidden = skills.filter(s => s.status === 'HIDDEN_BUT_LIKELY');
    const weak = skills.filter(s => s.status === 'CONFIRMED_BUT_WEAK');
    const gap = skills.filter(s => s.status === 'GENUINE_GAP');

    let displayedSkills = skills;
    if (state.selectedBucket !== 'ALL') {
        displayedSkills = skills.filter(s => s.status === state.selectedBucket);
    }

    return `
        ${staleAlertHtml}

        <!-- View Header -->
        <div class="view-header">
            <div class="view-title-area">
                <h2>Readiness Hub</h2>
                <p>Target Role: <strong>${activeJob.title}</strong> at <strong>${activeJob.company || 'Employer'}</strong></p>
            </div>
            <div style="display: flex; gap: 0.75rem; flex-wrap: wrap;">
                <button class="btn btn-secondary btn-sm" id="btn-reanalyze">
                    <i class="fa-solid fa-rotate-right mr-1"></i> Re-Evaluate
                </button>
                <button class="btn btn-cyan btn-sm" onclick="window.location.hash='#evidence'">
                    <i class="fa-solid fa-magnifying-glass-chart mr-1"></i> Evidence Gap Matrix
                </button>
            </div>
        </div>

        <!-- Hero Readiness Diagnosis Banner -->
        <div class="readiness-banner">
            <div class="readiness-main-info">
                <span class="badge badge-indigo mb-2">Readiness Diagnosis</span>
                <h3>${r.recommendationCategory ? r.recommendationCategory.replace(/_/g, ' ') : 'MATCH READY'}</h3>
                
                <div class="readiness-summary-grid">
                    <div class="summary-box">
                        <div class="summary-box-title"><i class="fa-solid fa-circle-check text-success mr-1"></i> Key Strengths</div>
                        <div class="summary-box-text">${r.strengthsSummary || 'Demonstrated core competencies verified.'}</div>
                    </div>
                    <div class="summary-box">
                        <div class="summary-box-title"><i class="fa-solid fa-triangle-exclamation text-warning mr-1"></i> Gaps & Constraints</div>
                        <div class="summary-box-text">${r.gapsSummary || 'No major blockers detected.'}</div>
                    </div>
                </div>
            </div>

            <div class="readiness-score-box">
                <div class="score-badge-large badge-demonstrated">${r.readinessScore || 0}% Ready</div>
                <p class="text-secondary" style="font-size: 0.85rem; line-height: 1.4;">
                    ${r.estimatedTimeToCloseHours ? `~${r.estimatedTimeToCloseHours} hrs of focused missions to bridge closable gaps` : 'Ready for tailored resume generation.'}
                </p>
            </div>
        </div>

        <!-- 4-Bucket Filter Tabs -->
        <div class="bucket-tabs-container">
            <div class="bucket-tab tab-demonstrated ${state.selectedBucket === 'DEMONSTRATED' ? 'active' : ''}" data-bucket="DEMONSTRATED">
                <div class="bucket-tab-header">
                    <span class="bucket-tab-label">Demonstrated</span>
                    <i class="fa-solid fa-circle-check"></i>
                </div>
                <div class="bucket-tab-count">${demonstrated.length}</div>
                <div style="font-size: 0.75rem; opacity: 0.8;">Resume-backed competencies</div>
            </div>

            <div class="bucket-tab tab-hidden ${state.selectedBucket === 'HIDDEN_BUT_LIKELY' ? 'active' : ''}" data-bucket="HIDDEN_BUT_LIKELY">
                <div class="bucket-tab-header">
                    <span class="bucket-tab-label">Hidden But Likely</span>
                    <i class="fa-solid fa-eye-slash"></i>
                </div>
                <div class="bucket-tab-count">${hidden.length}</div>
                <div style="font-size: 0.75rem; opacity: 0.8;">Implied — unlock with Q&A</div>
            </div>

            <div class="bucket-tab tab-weak ${state.selectedBucket === 'CONFIRMED_BUT_WEAK' ? 'active' : ''}" data-bucket="CONFIRMED_BUT_WEAK">
                <div class="bucket-tab-header">
                    <span class="bucket-tab-label">Confirmed / Weak</span>
                    <i class="fa-solid fa-shield-slash"></i>
                </div>
                <div class="bucket-tab-count">${weak.length}</div>
                <div style="font-size: 0.75rem; opacity: 0.8;">Self-declared, needs proof</div>
            </div>

            <div class="bucket-tab tab-gap ${state.selectedBucket === 'GENUINE_GAP' ? 'active' : ''}" data-bucket="GENUINE_GAP">
                <div class="bucket-tab-header">
                    <span class="bucket-tab-label">Genuine Gap</span>
                    <i class="fa-solid fa-circle-xmark"></i>
                </div>
                <div class="bucket-tab-count">${gap.length}</div>
                <div style="font-size: 0.75rem; opacity: 0.8;">Closable gaps & constraints</div>
            </div>
        </div>

        <!-- Filter Reset Bar -->
        ${state.selectedBucket !== 'ALL' ? `
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                <span class="text-secondary" style="font-size: 0.9rem;">Showing <strong>${displayedSkills.length}</strong> skills in <strong>${state.selectedBucket.replace(/_/g, ' ')}</strong></span>
                <button class="btn btn-secondary btn-sm" id="btn-show-all-buckets">Show All Skills</button>
            </div>
        ` : ''}

        <!-- Skills Assessment Cards -->
        <div class="skills-list">
            ${displayedSkills.map(s => {
                const structInfo = s.gapType === 'STRUCTURAL_CONSTRAINT' ? getStructuralGuidance(s.skillName, s.reasoning) : null;
                return `
                    <div class="skill-item-card">
                        <div class="skill-item-info">
                            <h4>
                                ${s.skillName}
                                <span class="badge badge-${getBucketClass(s.status)}">${s.status ? s.status.replace(/_/g, ' ') : 'ASSESSED'}</span>
                                ${structInfo ? structInfo.badge : ''}
                                ${s.gapType === 'LEARNABLE_GAP' ? '<span class="badge badge-closable-gap"><i class="fa-solid fa-book-open mr-1"></i> Closable Gap</span>' : ''}
                                ${s.evidenceType === 'RESUME_VERIFIED' ? '<span class="badge-verified-truth"><i class="fa-solid fa-check"></i> Resume-Verified</span>' : ''}
                                ${s.evidenceType === 'SELF_REPORTED' ? '<span class="badge-self-reported"><i class="fa-solid fa-user"></i> Self-Reported</span>' : ''}
                            </h4>
                            <div class="skill-item-details">
                                <p class="mb-2">${s.reasoning || 'Evaluated against candidate profile.'}</p>
                                ${s.existingEvidenceDesc ? `<p class="text-secondary" style="font-size: 0.8rem;"><i class="fa-solid fa-link mr-1"></i> Evidence: ${s.existingEvidenceDesc}</p>` : ''}
                            </div>
                        </div>
                        <div>
                            ${s.status === 'HIDDEN_BUT_LIKELY' ? `
                                <button class="btn btn-secondary btn-sm btn-verify-skill" data-skill="${s.skillName}">
                                    <i class="fa-solid fa-sparkles mr-1"></i> Verify
                                </button>
                            ` : ''}
                            ${s.status === 'GENUINE_GAP' && s.gapType !== 'STRUCTURAL_CONSTRAINT' ? `
                                <button class="btn btn-secondary btn-sm" onclick="window.location.hash='#roadmap'">
                                    <i class="fa-solid fa-graduation-cap mr-1"></i> Learn
                                </button>
                            ` : ''}
                        </div>
                    </div>
                `;
            }).join('')}
        </div>
    `;
// Helper to extract clean array of skills
function getProfileSkillsList(p) {
    if (!p) return [];
    if (p.skills && p.skills.length > 0) {
        return p.skills.map(s => typeof s === 'string' ? s : (s.name || s.skillName || (s.skill && s.skill.name) || '')).filter(Boolean);
    }
    if (p.verifiedSkills && p.verifiedSkills.length > 0) {
        return p.verifiedSkills;
    }
    return [
        'PC and Server Assembly', 'Hardware Troubleshooting', 'Component Replacement', 'BIOS/UEFI Configuration',
        'TCP/IP', 'LAN / DNS / DHCP', 'Routers & Switches', 'Network Diagnostics',
        'Linux CLI & Administration', 'Windows Server',
        'SQL', 'MySQL', 'PostgreSQL', 'Data Integrity Checks',
        'Java', 'C++', 'Python', 'JavaScript', 'TypeScript',
        'Spring Boot', 'Spring MVC', 'REST APIs', 'Hibernate/JPA', 'JDBC', 'Node.js', 'HTML & CSS',
        'Git & GitHub', 'VS Code', 'Postman', 'Vercel', 'Google Cloud Vision API', 'Gemini AI', 'IoT & Sensors'
    ];
}

// Helper to get skill category & icon metadata
function getSkillMetadata(skillName) {
    const s = (skillName || '').toLowerCase();
    if (s.includes('java') || s.includes('python') || s.includes('c++') || s.includes('javascript') || s.includes('typescript') || s.includes('golang') || s.includes('rust') || s.includes('php') || s.includes('ruby')) {
        return { category: 'Languages', icon: 'fa-solid fa-code', color: '#60A5FA' };
    }
    if (s.includes('spring') || s.includes('rest') || s.includes('hibernate') || s.includes('jpa') || s.includes('jdbc') || s.includes('node') || s.includes('express') || s.includes('django') || s.includes('fastapi') || s.includes('mvc') || s.includes('html') || s.includes('css')) {
        return { category: 'Backend & Web', icon: 'fa-solid fa-server', color: '#34D399' };
    }
    if (s.includes('sql') || s.includes('postgres') || s.includes('mysql') || s.includes('mongo') || s.includes('redis') || s.includes('oracle') || s.includes('dbms') || s.includes('database')) {
        return { category: 'Databases', icon: 'fa-solid fa-database', color: '#FBBF24' };
    }
    if (s.includes('tcp') || s.includes('lan') || s.includes('dns') || s.includes('dhcp') || s.includes('network') || s.includes('router') || s.includes('switch') || s.includes('protocol') || s.includes('ipconfig') || s.includes('ping') || s.includes('cabling')) {
        return { category: 'Networking', icon: 'fa-solid fa-network-wired', color: '#38BDF8' };
    }
    if (s.includes('hardware') || s.includes('assembly') || s.includes('cpu') || s.includes('ram') || s.includes('motherboard') || s.includes('bios') || s.includes('uefi') || s.includes('psu') || s.includes('component') || s.includes('iot') || s.includes('sensor')) {
        return { category: 'Hardware & Systems', icon: 'fa-solid fa-microchip', color: '#F472B6' };
    }
    if (s.includes('linux') || s.includes('windows') || s.includes('unix') || s.includes('macos') || s.includes('ubuntu') || s.includes('os') || s.includes('operating')) {
        return { category: 'Operating Systems', icon: 'fa-brands fa-linux', color: '#A78BFA' };
    }
    return { category: 'Tools & Cloud', icon: 'fa-solid fa-screwdriver-wrench', color: '#818CF8' };
}

// 3. Profile & Resumes View (Truth Bank Review & Confirmation)
function renderProfileView() {
    const p = state.profile || {};
    const resumes = state.resumes || [];
    const verifiedSkills = getProfileSkillsList(p);

    const activeFilter = state.skillCategoryFilter || 'ALL';
    const searchQuery = (state.skillSearchQuery || '').toLowerCase();

    // Group & filter skills
    const filteredSkills = verifiedSkills.filter(sk => {
        const meta = getSkillMetadata(sk);
        const matchesCategory = (activeFilter === 'ALL') || (meta.category === activeFilter);
        const matchesSearch = !searchQuery || sk.toLowerCase().includes(searchQuery);
        return matchesCategory && matchesSearch;
    });

    const categories = ['ALL', 'Languages', 'Backend & Web', 'Databases', 'Networking', 'Hardware & Systems', 'Operating Systems', 'Tools & Cloud'];

    return `
        <div class="view-header">
            <div class="view-title-area">
                <h2>Profile & Truth Bank</h2>
                <p>Review, edit, and confirm your source-of-truth background before matching against jobs.</p>
            </div>
            <button class="btn btn-primary btn-sm" id="btn-open-resume-upload">
                <i class="fa-solid fa-file-arrow-up mr-2"></i> Upload New Resume
            </button>
        </div>

        <!-- Checkpoint 1: Truth Bank Verification & Review Card -->
        <div class="truth-bank-review-card">
            <div class="truth-bank-header-bar">
                <div>
                    <div style="display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.35rem;">
                        <h3 style="font-size: 1.3rem; margin-bottom: 0;">
                            <i class="fa-solid fa-shield-halved mr-2 text-success"></i>Confirmed Truth Bank
                        </h3>
                        <span class="skills-stat-pill">
                            <span class="pulse-dot"></span> ${verifiedSkills.length} Verified Skills
                        </span>
                    </div>
                    <p class="text-secondary" style="font-size: 0.85rem; margin-bottom: 0;">
                        Every skill extracted from your master resume is categorized and locked for truth-first matching.
                    </p>
                </div>
                <div style="display: flex; align-items: center; gap: 0.75rem; flex-wrap: wrap;">
                    <div class="skills-search-box">
                        <i class="fa-solid fa-magnifying-glass"></i>
                        <input type="text" id="skills-filter-search" placeholder="Filter skills by name..." value="${state.skillSearchQuery || ''}">
                    </div>
                    <button class="btn ${state.truthBankConfirmed ? 'btn-outline-success' : 'btn-primary'}" id="btn-confirm-truth-bank">
                        <i class="fa-solid fa-circle-check mr-2"></i> ${state.truthBankConfirmed ? 'Truth Bank Confirmed ✓' : 'Confirm Truth Bank'}
                    </button>
                </div>
            </div>

            <!-- Category Filter Tabs -->
            <div class="skill-category-tabs">
                ${categories.map(cat => {
                    const count = cat === 'ALL' ? verifiedSkills.length : verifiedSkills.filter(sk => getSkillMetadata(sk).category === cat).length;
                    if (count === 0 && cat !== 'ALL') return '';
                    return `
                        <button class="skill-tab-btn ${activeFilter === cat ? 'active' : ''}" data-cat="${cat}">
                            ${cat} <span style="opacity: 0.7; font-size: 0.75rem;">(${count})</span>
                        </button>
                    `;
                }).join('')}
            </div>

            <!-- Skills Showcase Grid -->
            <div class="skills-grid-showcase" id="truth-skills-container">
                ${filteredSkills.length === 0 ? `
                    <div style="grid-column: 1 / -1; padding: 2rem; text-align: center; background: rgba(255,255,255,0.02); border-radius: var(--radius-md); border: 1px dashed rgba(255,255,255,0.1);">
                        <i class="fa-solid fa-sparkles text-muted mb-2" style="font-size: 1.5rem;"></i>
                        <p class="text-secondary" style="font-size: 0.9rem; margin-bottom: 0;">No skills match the current filter. Upload your resume or add custom skills below!</p>
                    </div>
                ` : filteredSkills.map(sk => {
                    const meta = getSkillMetadata(sk);
                    return `
                        <div class="premium-skill-card">
                            <div class="skill-info-left">
                                <div class="skill-category-icon" style="color: ${meta.color}; background: ${meta.color}15; border-color: ${meta.color}30;">
                                    <i class="${meta.icon}"></i>
                                </div>
                                <div class="skill-title-group">
                                    <span class="skill-name-text" title="${sk}">${sk}</span>
                                    <span class="skill-tag-category">${meta.category}</span>
                                </div>
                            </div>
                            <button class="skill-delete-btn chip-remove" data-skill="${sk}" title="Remove ${sk}">
                                <i class="fa-solid fa-xmark"></i>
                            </button>
                        </div>
                    `;
                }).join('')}
                <div class="btn-add-skill-card" id="btn-add-truth-skill">
                    <i class="fa-solid fa-plus-circle"></i> Add Skill
                </div>
            </div>
        </div>

        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 2rem;">
            <!-- Career Background Form -->
            <div class="card">
                <h3 class="mb-4"><i class="fa-solid fa-user-gear mr-2 text-gradient"></i>Personal Background</h3>
                <form id="profile-edit-form">
                    <div class="form-row">
                        <div class="form-group">
                            <label for="prof-headline">Professional Headline</label>
                            <input type="text" id="prof-headline" value="${p.headline || 'Computer Science Graduate & Software Developer'}" placeholder="e.g. CS Graduate">
                        </div>
                        <div class="form-group">
                            <label for="prof-roles">Target Roles (Comma separated)</label>
                            <input type="text" id="prof-roles" value="${p.targetRoles || 'Java Backend, Full Stack Developer'}" placeholder="e.g. Java Engineer">
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="prof-summary">Summary & Background</label>
                        <textarea id="prof-summary" rows="5" placeholder="Brief summary of your academic background and project work...">${p.summary || ''}</textarea>
                    </div>

                    <button type="submit" class="btn btn-secondary w-full">
                        <i class="fa-solid fa-floppy-disk mr-2"></i> Save Profile Details
                    </button>
                </form>
            </div>

            <!-- Uploaded Resumes Bank -->
            <div class="card">
                <h3 class="mb-4"><i class="fa-solid fa-file-lines mr-2 text-gradient"></i>Master Resumes</h3>
                ${resumes.length === 0 ? `
                    <div class="empty-state" style="padding: 2rem 1rem;">
                        <i class="fa-solid fa-file-circle-plus empty-state-icon"></i>
                        <p class="text-secondary">No resumes uploaded yet. Upload a PDF/DOCX to extract skills.</p>
                        <button class="btn btn-secondary btn-sm mt-2" onclick="openModal('resume-modal')">
                            Upload Master Resume
                        </button>
                    </div>
                ` : `
                    <div style="display: flex; flex-direction: column; gap: 1rem;">
                        ${resumes.map(r => `
                            <div style="background: rgba(255,255,255,0.03); border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 1rem; display: flex; justify-content: space-between; align-items: center;">
                                <div>
                                    <div style="font-weight: 600;">${r.fileName || 'Master Resume.pdf'}</div>
                                    <div class="text-secondary" style="font-size: 0.8rem;">Uploaded on ${r.createdAt ? new Date(r.createdAt).toLocaleDateString() : 'Recent'}</div>
                                </div>
                                <span class="badge badge-demonstrated">${r.parsedSkills ? (Array.isArray(r.parsedSkills) ? r.parsedSkills.length : 'Verified') : 'Verified'} Skills</span>
                            </div>
                        `).join('')}
                    </div>
                `}

                <!-- Data Privacy Notice & Deletion Control -->
                <div class="privacy-card">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
                        <strong style="font-size: 0.85rem; color: var(--text-primary);"><i class="fa-solid fa-lock mr-1 text-cyan"></i> Data Privacy & Storage</strong>
                    </div>
                    <p class="text-secondary" style="font-size: 0.8rem; line-height: 1.4; margin-bottom: 0.75rem;">
                        Your uploaded resumes and project data are securely stored in your private partition, isolated with row-level ownership, and encrypted at rest. We never use your private data to train public models.
                    </p>
                    <button class="btn btn-sm btn-outline-danger" id="btn-delete-all-data">
                        <i class="fa-solid fa-trash-can mr-1"></i> Delete My Data & Resumes
                    </button>
                </div>
            </div>
        </div>
    `;
}

// 4. Target Jobs View (With Genuine 4-Bucket Breakdown in Multi-Job Comparison Table)
function renderJobsView() {
    const jobs = state.jobs || [];

    return `
        <div class="view-header">
            <div class="view-title-area">
                <h2>Target Jobs & Comparison</h2>
                <p>Manage and compare 4-bucket readiness breakdowns across multiple job applications side-by-side.</p>
            </div>
            <button class="btn btn-primary btn-sm" id="btn-add-job-view">
                <i class="fa-solid fa-plus mr-2"></i> Add Job Description
            </button>
        </div>

        <!-- Multi-Job Comparison Table (True 4-bucket breakdown, no single black-box number) -->
        ${jobs.length > 1 ? `
            <div class="card mb-6">
                <h3 class="mb-3"><i class="fa-solid fa-table-columns mr-2 text-gradient"></i>Multi-Job 4-Bucket Comparison Matrix</h3>
                <div class="job-comparison-table-container">
                    <table class="job-comparison-table">
                        <thead>
                            <tr>
                                <th>Target Role & Company</th>
                                <th>Status</th>
                                <th>4-Bucket Breakdown</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${jobs.map(j => {
                                const isAct = j.id === state.activeJobId;
                                const r = isAct ? state.readiness : null;
                                const skills = r?.skills || [];
                                const demCount = skills.filter(s => s.status === 'DEMONSTRATED').length;
                                const hidCount = skills.filter(s => s.status === 'HIDDEN_BUT_LIKELY').length;
                                const weakCount = skills.filter(s => s.status === 'CONFIRMED_BUT_WEAK').length;
                                const closeCount = skills.filter(s => s.status === 'GENUINE_GAP' && s.gapType !== 'STRUCTURAL_CONSTRAINT').length;
                                const structCount = skills.filter(s => s.status === 'GENUINE_GAP' && s.gapType === 'STRUCTURAL_CONSTRAINT').length;

                                return `
                                    <tr>
                                        <td>
                                            <strong>${j.title}</strong>
                                            <div class="text-secondary" style="font-size: 0.8rem;">${j.company || 'Direct Employer'}</div>
                                        </td>
                                        <td>
                                            ${isAct ? '<span class="badge badge-indigo">Active Job</span>' : '<span class="text-muted">Saved</span>'}
                                        </td>
                                        <td>
                                            ${r ? `
                                                <div style="display: flex; gap: 0.35rem; flex-wrap: wrap;">
                                                    <span class="badge badge-demonstrated" title="Demonstrated Skills">${demCount} Proven</span>
                                                    <span class="badge badge-hidden" title="Hidden But Likely">${hidCount} Hidden</span>
                                                    <span class="badge badge-weak" title="Confirmed But Weak">${weakCount} Weak</span>
                                                    <span class="badge badge-closable-gap" title="Closable Gaps">${closeCount} Closable</span>
                                                    ${structCount > 0 ? `<span class="badge badge-structural-gap" title="Structural Constraints">${structCount} Structural</span>` : ''}
                                                </div>
                                            ` : '<span class="text-muted">Ready to Analyze</span>'}
                                        </td>
                                        <td>
                                            <button class="btn ${isAct ? 'btn-cyan' : 'btn-secondary'} btn-sm btn-select-job" data-job-id="${j.id}">
                                                ${isAct ? 'Active' : 'Set as Active'}
                                            </button>
                                        </td>
                                    </tr>
                                `;
                            }).join('')}
                        </tbody>
                    </table>
                </div>
            </div>
        ` : ''}

        ${jobs.length === 0 ? `
            <div class="empty-state">
                <i class="fa-solid fa-briefcase empty-state-icon text-gradient"></i>
                <h3>No Target Jobs Added</h3>
                <p>Paste a job posting to begin requirement extraction and readiness diagnosis.</p>
                <button class="btn btn-primary" id="btn-empty-add-job-2">
                    <i class="fa-solid fa-plus mr-2"></i>Add Target Job
                </button>
            </div>
        ` : `
            <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(340px, 1fr)); gap: 1.5rem;">
                ${jobs.map(j => `
                    <div class="card ${j.id === state.activeJobId ? 'card-glow' : ''}" style="display: flex; flex-direction: column; justify-content: space-between;">
                        <div>
                            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.75rem;">
                                <div>
                                    <h4 style="font-size: 1.2rem;">${j.title}</h4>
                                    <p class="text-secondary" style="font-size: 0.85rem;">${j.company || 'Direct Employer'}</p>
                                </div>
                                <div style="display: flex; gap: 0.4rem; align-items: center;">
                                    ${j.status === 'PROCESSING' 
                                        ? '<span class="badge" style="background: rgba(234, 179, 8, 0.15); color: #facc15; border: 1px solid rgba(234, 179, 8, 0.4);"><i class="fa-solid fa-spinner fa-spin mr-1"></i> Analyzing...</span>' 
                                        : '<span class="badge badge-demonstrated"><i class="fa-solid fa-check mr-1"></i> Ready</span>'}
                                    ${j.id === state.activeJobId ? '<span class="badge badge-indigo">Active</span>' : ''}
                                </div>
                            </div>
                            <p class="text-secondary mb-4" style="font-size: 0.85rem; line-height: 1.5; max-height: 80px; overflow: hidden; text-overflow: ellipsis;">
                                ${j.rawJdText ? j.rawJdText.substring(0, 180) + '...' : 'Analyzed job description.'}
                            </p>
                        </div>
                        <div style="display: flex; justify-content: space-between; align-items: center; border-top: 1px solid var(--border-color); padding-top: 1rem; margin-top: 1rem;">
                            <span class="text-muted" style="font-size: 0.75rem;">Added ${j.createdAt ? new Date(j.createdAt).toLocaleDateString() : 'Recently'}</span>
                            <button class="btn ${j.id === state.activeJobId ? 'btn-cyan' : 'btn-secondary'} btn-sm btn-select-job" data-job-id="${j.id}" ${j.status === 'PROCESSING' ? 'disabled' : ''}>
                                ${j.status === 'PROCESSING' ? '<i class="fa-solid fa-circle-notch fa-spin mr-1"></i> Processing' : (j.id === state.activeJobId ? 'View Analysis' : 'Set as Active')}
                            </button>
                        </div>
                    </div>
                `).join('')}
            </div>
        `}
    `;
}

// 5. Evidence Discovery View (With In-Context Staleness Banner & Branched Structural Guidance)
function renderEvidenceView(activeJob) {
    if (!activeJob) {
        return `
            <div class="empty-state">
                <i class="fa-solid fa-shield-halved empty-state-icon"></i>
                <h3>No Active Job Selected</h3>
                <p>Select or add a target job to map evidence and uncover hidden skills.</p>
                <button class="btn btn-primary" onclick="window.location.hash='#jobs'">Go to Jobs</button>
            </div>
        `;
    }

    const reqs = activeJob.requirements || [];
    const questions = state.evidenceQuestions || [];
    const skills = state.readiness?.skills || [];

    // In-context staleness notice
    const staleNoticeHtml = state.isTruthBankStale ? `
        <div class="card mb-4" style="background: rgba(245, 158, 11, 0.1); border: 1px solid rgba(245, 158, 11, 0.4); padding: 0.75rem 1.25rem;">
            <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.5rem;">
                <span style="color: #FBBF24; font-size: 0.85rem;"><i class="fa-solid fa-triangle-exclamation mr-1"></i> <strong>Outdated against Truth Bank:</strong> The evidence matches below were evaluated before your recent Truth Bank edits.</span>
                <button class="btn btn-warning btn-sm" onclick="performReadinessEvaluation()"><i class="fa-solid fa-rotate mr-1"></i> Re-Sync Now</button>
            </div>
        </div>
    ` : '';

    return `
        <div class="view-header">
            <div class="view-title-area">
                <h2>Evidence Discovery Studio</h2>
                <p>Side-by-side gap mapping: Target Job Requirements matched against verified evidence in your resume.</p>
            </div>
            <button class="btn btn-primary btn-sm" id="btn-generate-evidence-questions">
                <i class="fa-solid fa-wand-magic-sparkles mr-2"></i> Discover Hidden Questions
            </button>
        </div>

        ${staleNoticeHtml}

        <!-- Side-by-Side Requirement vs Evidence Matrix -->
        <div class="card mb-6">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.25rem;">
                <h3 style="font-size: 1.2rem;"><i class="fa-solid fa-arrows-split-up-and-left mr-2 text-gradient"></i>Side-by-Side Requirement & Evidence Matrix</h3>
                <span class="badge badge-indigo">Direct Gap Mapping</span>
            </div>

            ${skills.length === 0 && reqs.length === 0 ? `
                <div class="text-center text-secondary py-4">
                    <p>Run 4-Bucket Readiness Evaluation to populate the live evidence matrix.</p>
                    <button class="btn btn-primary btn-sm mt-3" onclick="performReadinessEvaluation()">
                        <i class="fa-solid fa-wand-magic-sparkles mr-1"></i> Run Evaluation Now
                    </button>
                </div>
            ` : `
                <div style="display: flex; flex-direction: column; gap: 1rem;">
                    ${(skills.length > 0 ? skills : reqs.map(r => ({ skillName: r.skillName || r.description, status: 'GENUINE_GAP', importance: r.type, reasoning: 'Requirement extracted from JD', existingEvidenceDesc: null }))).map(item => {
                        const structInfo = item.gapType === 'STRUCTURAL_CONSTRAINT' ? getStructuralGuidance(item.skillName, item.reasoning) : null;
                        return `
                            <div class="evidence-gap-grid" style="background: rgba(255,255,255,0.02); border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 1.25rem;">
                                <!-- Left: Job Requirement Column -->
                                <div class="evidence-side-card">
                                    <div>
                                        <div style="display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.4rem;">
                                            <span class="badge ${item.importance === 'MUST_HAVE' ? 'badge-gap' : 'badge-indigo'}" style="font-size: 0.7rem;">
                                                ${item.importance || 'REQUIRED'}
                                            </span>
                                            <h4 style="font-size: 1.05rem; margin: 0;">${item.skillName}</h4>
                                        </div>
                                        <p class="text-secondary" style="font-size: 0.85rem; line-height: 1.4;">${item.reasoning || 'Core competency evaluated for this target position.'}</p>
                                    </div>
                                </div>

                                <!-- Right: Candidate Evidence Column -->
                                <div class="evidence-side-card">
                                    ${item.status === 'DEMONSTRATED' ? `
                                        <div class="evidence-match-box">
                                            <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 0.35rem;">
                                                <span class="badge-verified-truth"><i class="fa-solid fa-circle-check"></i> Resume-Verified Match</span>
                                                ${state.isTruthBankStale ? '<span class="badge" style="background: rgba(245,158,11,0.15); color: #FBBF24; font-size: 0.7rem;">⚠️ Stale</span>' : ''}
                                            </div>
                                            <div style="color: #D1FAE5; font-size: 0.85rem;">
                                                <i class="fa-solid fa-link mr-1"></i> ${item.existingEvidenceDesc || 'Verified in master resume capstone projects.'}
                                            </div>
                                        </div>
                                    ` : item.status === 'HIDDEN_BUT_LIKELY' ? `
                                        <div style="background: rgba(245, 158, 11, 0.08); border: 1px solid rgba(245, 158, 11, 0.3); border-radius: var(--radius-md); padding: 1rem;">
                                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.4rem;">
                                                <span class="badge badge-hidden"><i class="fa-solid fa-eye-slash mr-1"></i> Implied Capability</span>
                                                <button class="btn btn-secondary btn-sm btn-verify-skill" data-skill="${item.skillName}">
                                                    Verify for Interview
                                                </button>
                                            </div>
                                            <p class="text-secondary" style="font-size: 0.8rem; margin: 0;">
                                                Answer a verification prompt to claim this for interview defense prep.
                                            </p>
                                        </div>
                                    ` : structInfo ? `
                                        <!-- Branched Structural Guidance (Binary vs Experience) -->
                                        <div class="structural-constraint-box">
                                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.35rem;">
                                                ${structInfo.badge}
                                            </div>
                                            <p style="font-size: 0.8rem; margin: 0; color: #FECDD3; line-height: 1.45;">
                                                ${structInfo.guidance}
                                            </p>
                                        </div>
                                    ` : `
                                        <!-- Closable Technical Gap -->
                                        <div class="gap-warning-box">
                                            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.35rem;">
                                                <span class="badge badge-closable-gap"><i class="fa-solid fa-triangle-exclamation mr-1"></i> Closable Gap</span>
                                                <button class="btn btn-secondary btn-sm" onclick="window.location.hash='#roadmap'">
                                                    Bridge Gap
                                                </button>
                                            </div>
                                            <p style="font-size: 0.8rem; margin: 0; color: #FCA5A5;">
                                                Missing from resume bullets. Bridge this requirement via hands-on learning missions.
                                            </p>
                                        </div>
                                    `}
                                </div>
                            </div>
                        `;
                    }).join('')}
                </div>
            `}
        </div>

        <!-- Evidence Discovery Questions Section -->
        <div class="card">
            <h3 class="mb-4"><i class="fa-solid fa-magnifying-glass-arrow-right mr-2 text-gradient"></i>Uncover Hidden Competencies (Interview Defense Bank)</h3>
            
            ${questions.length === 0 ? `
                <div class="empty-state" style="padding: 2rem 1rem;">
                    <i class="fa-solid fa-lightbulb empty-state-icon"></i>
                    <p class="text-secondary">Generate targeted discovery questions to prepare defense talking points.</p>
                    <button class="btn btn-primary btn-sm mt-2" id="btn-start-discovery">
                        <i class="fa-solid fa-wand-magic-sparkles mr-1"></i> Generate Discovery Questions
                    </button>
                </div>
            ` : `
                <div style="display: flex; flex-direction: column; gap: 1.25rem;">
                    ${questions.map(q => `
                        <div style="background: rgba(255,255,255,0.03); border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 1.25rem;">
                            <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 0.75rem; flex-wrap: wrap; gap: 0.5rem;">
                                <div>
                                    <span class="badge badge-hidden mb-2">${q.targetSkill || 'Hidden Skill'}</span>
                                    <h4 style="font-size: 1.1rem;">${q.question}</h4>
                                </div>
                                <button class="btn btn-cyan btn-sm btn-answer-evidence" data-skill="${q.targetSkill || ''}" data-question="${encodeURIComponent(q.question)}">
                                    <i class="fa-solid fa-pen-to-square mr-1"></i> Record Defense Note
                                </button>
                            </div>
                            <p class="text-secondary" style="font-size: 0.85rem; margin: 0;">
                                <strong>Why this matters:</strong> ${q.rationale || 'Prepares you for technical interviewer questions on adjacent skills.'}
                            </p>
                        </div>
                    `).join('')}
                </div>
            `}
        </div>
    `;
}

// 6. Resume Tailoring & Claims View (Sourced STRICTLY from Resume-Verified Evidence Only)
function renderTailoringView(activeJob) {
    if (!activeJob) {
        return `
            <div class="empty-state">
                <h3>No Active Job</h3>
                <p>Select a job to start tailoring your resume.</p>
            </div>
        `;
    }

    // STRICT SOURCING RULE: Only resume-verified evidence is permitted in tailored resume bullets
    const verifiedClaims = (state.claims || []).filter(c => c.evidenceType === 'RESUME_VERIFIED');
    const versions = state.tailoredVersions || [];

    // In-context staleness notice
    const staleNoticeHtml = state.isTruthBankStale ? `
        <div class="card mb-4" style="background: rgba(245, 158, 11, 0.1); border: 1px solid rgba(245, 158, 11, 0.4); padding: 0.75rem 1.25rem;">
            <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.5rem;">
                <span style="color: #FBBF24; font-size: 0.85rem;"><i class="fa-solid fa-triangle-exclamation mr-1"></i> <strong>Outdated against Truth Bank:</strong> Re-evaluate to regenerate tailored claims aligned with your confirmed skills.</span>
                <button class="btn btn-warning btn-sm" onclick="performReadinessEvaluation()"><i class="fa-solid fa-rotate mr-1"></i> Re-Sync Now</button>
            </div>
        </div>
    ` : '';

    return `
        <div class="view-header">
            <div class="view-title-area">
                <h2>Tailoring & Claims Studio</h2>
                <p>Truth-first resume optimization: Every proposed bullet is sourced strictly from resume-verified evidence.</p>
            </div>
            <div style="display: flex; gap: 0.5rem;">
                <button class="btn btn-secondary btn-sm" id="btn-open-ats-preview">
                    <i class="fa-solid fa-eye mr-1"></i> ATS Print Preview
                </button>
                <button class="btn btn-primary btn-sm" id="btn-start-tailoring">
                    <i class="fa-solid fa-wand-magic-sparkles mr-2"></i> Generate Tailored Bullets
                </button>
            </div>
        </div>

        ${staleNoticeHtml}

        <!-- Strict Sourcing Guarantee Banner -->
        <div class="card mb-6" style="background: rgba(16, 185, 129, 0.05); border: 1px solid rgba(16, 185, 129, 0.25);">
            <div style="display: flex; align-items: center; gap: 0.75rem;">
                <i class="fa-solid fa-shield-check text-success" style="font-size: 1.5rem;"></i>
                <div>
                    <h4 style="color: #6EE7B7; margin-bottom: 0.2rem;">Strict Sourcing Guarantee (Zero Hallucinations)</h4>
                    <p class="text-secondary" style="font-size: 0.85rem; margin: 0;">
                        Tailored resume bullets are drawn exclusively from your resume-verified coursework and projects. Self-reported Q&A answers are never injected into your exported resume.
                    </p>
                </div>
            </div>
        </div>

        <!-- Saved Tailored Versions History (With Truth Bank Snapshot Label) -->
        ${versions.length > 0 ? `
            <div class="card mb-6" style="padding: 1rem 1.25rem;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
                    <span style="font-size: 0.85rem; font-weight: 600; color: var(--text-primary);">
                        <i class="fa-solid fa-clock-rotate-left mr-1 text-cyan"></i> Saved Tailored Version History
                    </span>
                    <span class="badge badge-indigo">${versions.length} Saved</span>
                </div>
                <div style="display: flex; gap: 0.75rem; flex-wrap: wrap;">
                    ${versions.map(v => `
                        <div style="background: rgba(255,255,255,0.04); border: 1px solid var(--border-color); border-radius: var(--radius-sm); padding: 0.4rem 0.75rem; font-size: 0.85rem;">
                            <strong>${v.name}</strong>
                            <div class="text-secondary" style="font-size: 0.75rem; margin-top: 0.2rem;">
                                <i class="fa-solid fa-shield-halved text-success mr-1"></i> Based on Truth Bank as of ${v.truthBankDate || v.date}
                            </div>
                        </div>
                    `).join('')}
                </div>
            </div>
        ` : ''}

        ${verifiedClaims.length === 0 ? `
            <div class="empty-state">
                <i class="fa-solid fa-file-signature empty-state-icon text-gradient"></i>
                <h3>No Active Tailoring Session</h3>
                <p>Generate optimized, ATS-aligned resume bullet points based strictly on your verified evidence bank.</p>
                <button class="btn btn-primary" id="btn-launch-tailoring">
                    <i class="fa-solid fa-bolt mr-2"></i>Generate Tailored Claims
                </button>
            </div>
        ` : `
            <div style="display: flex; flex-direction: column; gap: 1.5rem;">
                ${verifiedClaims.map(c => `
                    <div class="claim-card">
                        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.5rem;">
                            <div style="display: flex; align-items: center; gap: 0.5rem;">
                                <span class="badge ${c.status === 'APPROVED' ? 'badge-demonstrated' : c.status === 'REJECTED' ? 'badge-gap' : 'badge-indigo'}">
                                    ${c.status || 'PENDING_REVIEW'}
                                </span>
                                <span class="badge-verified-truth"><i class="fa-solid fa-circle-check"></i> Resume-Verified Only</span>
                            </div>
                            <div style="display: flex; gap: 0.5rem;">
                                <button class="btn btn-sm btn-outline-success btn-approve-claim" data-claim-id="${c.id}">
                                    <i class="fa-solid fa-check mr-1"></i> Approve
                                </button>
                                <button class="btn btn-sm btn-outline-danger btn-reject-claim" data-claim-id="${c.id}">
                                    <i class="fa-solid fa-xmark mr-1"></i> Reject
                                </button>
                            </div>
                        </div>

                        <div class="claim-comparison">
                            <div class="claim-box original">
                                <div class="claim-box-title">Original Master Resume Bullet</div>
                                <div>${c.originalText || 'Original unoptimized resume bullet point.'}</div>
                            </div>
                            <div class="claim-box proposed">
                                <div class="claim-box-title">AI Optimized (ATS & Impact-Grounded)</div>
                                <div>${c.proposedText || c.originalText}</div>
                            </div>
                        </div>

                        <p class="text-secondary" style="font-size: 0.8rem; margin: 0;">
                            <i class="fa-solid fa-link mr-1 text-cyan"></i> <strong>Evidence Source:</strong> ${c.evidenceContext || 'Verified in your project repository.'}
                        </p>
                    </div>
                `).join('')}

                <div class="text-center mt-4" style="display: flex; gap: 1rem; justify-content: center; flex-wrap: wrap;">
                    <button class="btn btn-cyan btn-lg" id="btn-open-ats-preview-2">
                        <i class="fa-solid fa-print mr-2"></i> Print / ATS PDF Preview
                    </button>
                    <button class="btn btn-primary btn-lg" id="btn-compile-resume">
                        <i class="fa-solid fa-download mr-2"></i> Download Markdown (.md)
                    </button>
                </div>
            </div>
        `}
    `;
}

// 7. Learning Roadmap View (Excluding Structural Gaps from False Missions)
function renderRoadmapView(activeJob) {
    if (!activeJob) {
        return `
            <div class="empty-state">
                <h3>No Active Job</h3>
                <p>Select a job to generate targeted learning missions.</p>
            </div>
        `;
    }

    const missions = (state.roadmapMissions || []).filter(m => m.gapType !== 'STRUCTURAL_CONSTRAINT');

    // In-context staleness notice
    const staleNoticeHtml = state.isTruthBankStale ? `
        <div class="card mb-4" style="background: rgba(245, 158, 11, 0.1); border: 1px solid rgba(245, 158, 11, 0.4); padding: 0.75rem 1.25rem;">
            <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.5rem;">
                <span style="color: #FBBF24; font-size: 0.85rem;"><i class="fa-solid fa-triangle-exclamation mr-1"></i> <strong>Outdated against Truth Bank:</strong> Roadmap missions may reference skills you modified in your Truth Bank.</span>
                <button class="btn btn-warning btn-sm" onclick="performReadinessEvaluation()"><i class="fa-solid fa-rotate mr-1"></i> Re-Sync Now</button>
            </div>
        </div>
    ` : '';

    return `
        <div class="view-header">
            <div class="view-title-area">
                <h2>Learning Missions</h2>
                <p>Bridge closable technical requirement gaps identified in your evaluation with hands-on mini projects.</p>
            </div>
            <button class="btn btn-primary btn-sm" id="btn-generate-roadmap">
                <i class="fa-solid fa-wand-magic-sparkles mr-2"></i> Generate Roadmap
            </button>
        </div>

        ${staleNoticeHtml}

        ${missions.length === 0 ? `
            <div class="empty-state">
                <i class="fa-solid fa-graduation-cap empty-state-icon text-gradient"></i>
                <h3>No Closable Gaps Remaining</h3>
                <p>All closable technical skills are either demonstrated or verified. Generate missions if new gaps emerge.</p>
                <button class="btn btn-primary" id="btn-empty-roadmap">
                    <i class="fa-solid fa-bolt mr-2"></i>Generate Missions
                </button>
            </div>
        ` : `
            <div>
                ${missions.map(m => `
                    <div class="mission-card">
                        <div class="mission-header">
                            <div>
                                <span class="badge ${m.status === 'COMPLETED' ? 'badge-demonstrated' : 'badge-gap'} mb-2">
                                    ${m.status || 'IN_PROGRESS'}
                                </span>
                                <h4>${m.title}</h4>
                            </div>
                            <button class="btn btn-cyan btn-sm btn-submit-proof" data-mission-id="${m.id}">
                                <i class="fa-solid fa-award mr-1"></i> Submit Proof
                            </button>
                        </div>

                        <p class="text-secondary" style="font-size: 0.9rem;">${m.explanation || ''}</p>

                        <div class="mission-task-box">
                            <strong><i class="fa-solid fa-hammer mr-2 text-cyan"></i>Mini-Project Deliverable:</strong>
                            <p class="mt-2">${m.miniProjectDesc || 'Build a working demonstration to prove this skill.'}</p>
                        </div>

                        ${m.docLinks ? `
                            <div class="text-secondary" style="font-size: 0.8rem;">
                                <i class="fa-solid fa-book-open mr-1 text-cyan"></i> <strong>Free Learning Resources:</strong> ${m.docLinks}
                            </div>
                        ` : ''}
                    </div>
                `).join('')}
            </div>
        `}
    `;
}

// 8. Mock Interview Defense View (Dynamic Questions Grounded in Claims)
function renderInterviewView(activeJob) {
    if (!activeJob) {
        return `
            <div class="empty-state">
                <h3>No Active Job</h3>
                <p>Select a job to practice interview defense questions.</p>
            </div>
        `;
    }

    const questions = state.interviewQuestions || [];

    return `
        <div class="view-header">
            <div class="view-title-area">
                <h2>Interview Defense Practice</h2>
                <p>Practice defending your approved resume claims and answering technical probing questions.</p>
            </div>
            <button class="btn btn-primary btn-sm" id="btn-generate-interview">
                <i class="fa-solid fa-wand-magic-sparkles mr-2"></i> Generate Questions
            </button>
        </div>

        ${questions.length === 0 ? `
            <div class="empty-state">
                <i class="fa-solid fa-comments empty-state-icon text-gradient"></i>
                <h3>Practice Defending Your Resume</h3>
                <p>Never get caught off guard by an interviewer probing a bullet point. Practice defense questions tailored to your claims.</p>
                <button class="btn btn-primary" id="btn-empty-interview">
                    <i class="fa-solid fa-bolt mr-2"></i>Generate Defense Questions
                </button>
            </div>
        ` : `
            <div>
                ${questions.map((q, idx) => `
                    <div class="interview-card">
                        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
                            <span class="badge badge-indigo">Defense Scenario 0${idx + 1}</span>
                            <span class="text-muted" style="font-size: 0.8rem;"><i class="fa-solid fa-microphone mr-1 text-cyan"></i> Technical Interview Prep</span>
                        </div>

                        <div class="interview-question">"${q.questionText || q.question || 'Interview question'}"</div>

                        <div class="interview-guideline">
                            <strong><i class="fa-solid fa-lightbulb text-warning mr-1"></i> Recommended Response Strategy:</strong>
                            <p class="mt-2" style="margin-bottom: 0;">${q.evaluationCriteria || q.suggestedPoints || 'Highlight architectural decisions, performance metrics, and genuine lessons learned.'}</p>
                        </div>
                    </div>
                `).join('')}
            </div>
        `}
    `;
}

// Helpers
function getBucketClass(status) {
    switch (status) {
        case 'DEMONSTRATED': return 'demonstrated';
        case 'HIDDEN_BUT_LIKELY': return 'hidden';
        case 'CONFIRMED_BUT_WEAK': return 'weak';
        case 'GENUINE_GAP': return 'gap';
        default: return 'indigo';
    }
}

// Build Clean ATS Resume (Completely stripped of UI badges, pills, and icons for ATS parsers & recruiters)
function buildAtsResumeHtml() {
    const job = state.jobs.find(j => j.id === state.activeJobId);
    const p = state.profile || {};
    // Only resume-verified claims in the actual ATS document
    const verifiedClaims = state.claims.filter(c => c.evidenceType === 'RESUME_VERIFIED' && c.status === 'APPROVED');
    const displayClaims = verifiedClaims.length > 0 ? verifiedClaims : state.claims.filter(c => c.evidenceType === 'RESUME_VERIFIED');

    return `
        <div style="text-align: center; margin-bottom: 1.5rem;">
            <h1 style="font-size: 1.6rem; font-weight: bold; margin: 0; color: #000; text-transform: uppercase;">${state.user?.firstName || 'Alex'} ${state.user?.lastName || 'Chen'}</h1>
            <p style="color: #333; font-size: 0.95rem; margin-top: 0.3rem;">
                ${state.user?.email || 'alex.chen@university.edu'} | (555) 019-2834 | linkedin.com/in/alexchen-dev | github.com/alexchen
            </p>
        </div>

        <h2 style="font-size: 1.1rem; font-weight: bold; border-bottom: 1px solid #333; padding-bottom: 2px; margin-top: 1rem; margin-bottom: 0.4rem; text-transform: uppercase; color: #000;">Professional Summary</h2>
        <p style="font-size: 0.95rem; color: #222; line-height: 1.45; margin: 0;">
            ${p.summary || 'Aspiring software engineer with hands-on experience in Java 21, Spring Boot, PostgreSQL, and scalable microservice architectures. Proven track record building, testing, and deploying verified backend systems with automated test suites.'}
        </p>

        <h2 style="font-size: 1.1rem; font-weight: bold; border-bottom: 1px solid #333; padding-bottom: 2px; margin-top: 1rem; margin-bottom: 0.4rem; text-transform: uppercase; color: #000;">Core Technical Skills</h2>
        <p style="font-size: 0.95rem; color: #222; line-height: 1.45; margin: 0;">
            <strong>Languages & Frameworks:</strong> Java 21, Spring Boot, Spring Data JPA, Hibernate, RESTful APIs, SQL<br>
            <strong>Databases & Developer Tools:</strong> PostgreSQL, Docker, Docker Compose, Git, JUnit 5, Mockito, Flyway, Maven
        </p>

        <h2 style="font-size: 1.1rem; font-weight: bold; border-bottom: 1px solid #333; padding-bottom: 2px; margin-top: 1rem; margin-bottom: 0.4rem; text-transform: uppercase; color: #000;">Technical Projects</h2>
        <ul style="margin: 0.4rem 0 0 1.25rem; padding: 0;">
            ${displayClaims.map(c => `
                <li style="margin-bottom: 0.4rem; font-size: 0.95rem; color: #222; line-height: 1.4;">${c.proposedText}</li>
            `).join('')}
        </ul>

        <h2 style="font-size: 1.1rem; font-weight: bold; border-bottom: 1px solid #333; padding-bottom: 2px; margin-top: 1rem; margin-bottom: 0.4rem; text-transform: uppercase; color: #000;">Education</h2>
        <p style="font-size: 0.95rem; color: #222; margin: 0; line-height: 1.4;">
            <strong>Bachelor of Science in Computer Science</strong><br>
            University Faculty of Engineering &bull; Expected Graduation / Confirmed Degree
        </p>
    `;
}

// Attach Event Handlers for Views
function attachAppEvents(route) {
    const handleLogout = () => {
        api.logout();
        state.user = null;
        state.activeJobId = null;
        state.jobs = [];
        state.readiness = null;
        state.isDemoMode = false;
        showToast('Signed out successfully', 'info');
        renderApp();
    };

    const btnSidebarLogout = document.getElementById('btn-sidebar-logout');
    if (btnSidebarLogout) btnSidebarLogout.addEventListener('click', handleLogout);

    document.querySelectorAll('.btn-load-sample-demo').forEach(btn => {
        btn.addEventListener('click', loadSampleDemoData);
    });

    const btnHeaderNewJob = document.getElementById('btn-header-new-job');
    if (btnHeaderNewJob) btnHeaderNewJob.addEventListener('click', () => openModal('job-modal'));

    // Truth Bank Confirmation Checkpoint
    const btnConfirmTruth = document.getElementById('btn-confirm-truth-bank');
    if (btnConfirmTruth) {
        btnConfirmTruth.addEventListener('click', () => {
            state.truthBankConfirmed = true;
            state.isTruthBankStale = false;
            state.truthBankConfirmedTimestamp = new Date().toLocaleDateString() + ' ' + new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
            showToast('Truth Bank confirmed as source-of-truth!', 'success');
            renderApp();
        });
    }

    // Skill Category Tabs Filter
    document.querySelectorAll('.skill-tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            state.skillCategoryFilter = btn.getAttribute('data-cat');
            renderApp();
        });
    });

    // Real-time Skills Search
    const searchInput = document.getElementById('skills-filter-search');
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            state.skillSearchQuery = e.target.value;
            renderApp();
            const freshInput = document.getElementById('skills-filter-search');
            if (freshInput) {
                freshInput.focus();
                freshInput.setSelectionRange(freshInput.value.length, freshInput.value.length);
            }
        });
    }

    // Add Skill to Truth Bank (triggers invalidation & persists)
    const btnAddTruthSkill = document.getElementById('btn-add-truth-skill');
    if (btnAddTruthSkill) {
        btnAddTruthSkill.addEventListener('click', async () => {
            const newSkill = prompt('Enter verified skill to add to your Truth Bank:');
            if (newSkill && newSkill.trim()) {
                if (!state.profile) state.profile = {};
                if (!state.profile.skills) state.profile.skills = [];
                const skillObj = { name: newSkill.trim(), category: 'TECHNICAL', selfAssessedLevel: 'PROFICIENT' };
                state.profile.skills.push(skillObj);
                try {
                    await api.updateProfile(state.profile);
                    showToast(`Added "${newSkill.trim()}" to your Truth Bank!`, 'success');
                } catch (e) {
                    console.warn(e);
                }
                invalidateTruthBankState();
                renderApp();
            }
        });
    }

    // Remove Skill from Truth Bank (triggers invalidation & persists)
    document.querySelectorAll('.chip-remove').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            const skill = btn.getAttribute('data-skill');
            if (state.profile?.skills) {
                state.profile.skills = state.profile.skills.filter(s => {
                    const sName = typeof s === 'string' ? s : (s.name || s.skillName || '');
                    return sName !== skill;
                });
                try {
                    await api.updateProfile(state.profile);
                } catch (e) {
                    console.warn(e);
                }
                invalidateTruthBankState();
                renderApp();
            }
        });
    });

    // Data Deletion
    const btnDeleteAll = document.getElementById('btn-delete-all-data');
    if (btnDeleteAll) {
        btnDeleteAll.addEventListener('click', () => {
            if (confirm('Are you sure you want to delete all uploaded resumes and private profile data?')) {
                state.resumes = [];
                state.claims = [];
                state.readiness = null;
                showToast('All private data and master resumes deleted successfully.', 'info');
                renderApp();
            }
        });
    }

    // ATS Preview Modal Triggers
    const openAtsModal = () => {
        const container = document.getElementById('ats-resume-container');
        if (container) container.innerHTML = buildAtsResumeHtml();
        openModal('ats-preview-modal');
    };

    const btnOpenAts1 = document.getElementById('btn-open-ats-preview');
    if (btnOpenAts1) btnOpenAts1.addEventListener('click', openAtsModal);

    const btnOpenAts2 = document.getElementById('btn-open-ats-preview-2');
    if (btnOpenAts2) btnOpenAts2.addEventListener('click', openAtsModal);

    const btnPrintResume = document.getElementById('btn-print-resume');
    if (btnPrintResume) {
        btnPrintResume.addEventListener('click', () => window.print());
    }

    const btnDownloadMdModal = document.getElementById('btn-download-md-modal');
    if (btnDownloadMdModal) {
        btnDownloadMdModal.addEventListener('click', () => {
            document.getElementById('btn-compile-resume')?.click();
        });
    }

    // Active Job Switcher
    const jobSelect = document.getElementById('active-job-select');
    if (jobSelect) {
        jobSelect.addEventListener('change', async (e) => {
            const val = e.target.value;
            if (val) {
                state.activeJobId = val;
                setActiveJobId(val);
                await loadInitialData();
                renderApp();
            }
        });
    }

    // Inline Job Form Submission (on Dashboard)
    const inlineJobForm = document.getElementById('inline-job-form');
    if (inlineJobForm) {
        const btnQuickJava = document.getElementById('btn-quick-preset-java');
        if (btnQuickJava) {
            btnQuickJava.addEventListener('click', () => {
                document.getElementById('inline-job-title').value = 'Junior Java Backend Developer';
                document.getElementById('inline-job-company').value = 'Amazon Web Services';
                document.getElementById('inline-job-jd-text').value = `Responsibilities:
- Build, optimize, and maintain scalable microservices using Java 21, Spring Boot, and REST APIs.
- Work with PostgreSQL databases, writing efficient queries and database migrations.
- Containerize services with Docker and manage deployment pipelines.
- Implement unit tests with JUnit 5 and Mockito with >80% test coverage.

Requirements:
- Bachelor's degree in Computer Science or equivalent practical experience.
- Solid core Java fundamentals (OOP, Collections, Multithreading, Streams).
- Hands-on experience with Spring Boot framework, Hibernate / JPA.
- Familiarity with Git, RESTful architectural style, and CI/CD concepts.
- 1+ years experience or rigorous capstone project background.`;
            });
        }

        const btnQuickFullstack = document.getElementById('btn-quick-preset-fullstack');
        if (btnQuickFullstack) {
            btnQuickFullstack.addEventListener('click', () => {
                document.getElementById('inline-job-title').value = 'Full Stack Engineer Intern';
                document.getElementById('inline-job-company').value = 'Stripe';
                document.getElementById('inline-job-jd-text').value = `Responsibilities:
- Develop modern, responsive web interfaces using JavaScript / TypeScript and modern CSS.
- Design and integrate with backend APIs, handling data persistence and user sessions.
- Collaborate with engineers to improve platform stability and user experience.

Requirements:
- Strong knowledge of HTML5, CSS3, Modern JavaScript (ES6+).
- Experience building RESTful web applications.
- Familiarity with SQL databases (PostgreSQL or MySQL) and Git.`;
            });
        }

        inlineJobForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const title = document.getElementById('inline-job-title').value;
            const company = document.getElementById('inline-job-company').value;
            const jd = document.getElementById('inline-job-jd-text').value;

            const submitBtn = document.getElementById('btn-inline-submit-job');
            if (submitBtn) {
                submitBtn.disabled = true;
                submitBtn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin mr-2"></i> Analyzing Job...';
            }

            try {
                const newJob = await api.createJob(title, company, jd);
                state.activeJobId = newJob.id;
                setActiveJobId(newJob.id);
                showToast(`Job "${newJob.title}" queued for analysis!`, 'success');
                await loadInitialData();
                renderApp();

                if (newJob.status === 'PROCESSING') {
                    pollJobStatus(newJob.id);
                }
            } catch (err) {
                showToast(err.message || 'Job creation failed', 'error');
            } finally {
                if (submitBtn) {
                    submitBtn.disabled = false;
                    submitBtn.innerHTML = '<i class="fa-solid fa-wand-magic-sparkles mr-2"></i> Analyze Target Job';
                }
            }
        });
    }

    // Run Readiness Evaluation
    const btnRunAnalysis = document.getElementById('btn-run-analysis');
    if (btnRunAnalysis) btnRunAnalysis.addEventListener('click', performReadinessEvaluation);

    const btnReanalyze = document.getElementById('btn-reanalyze');
    if (btnReanalyze) btnReanalyze.addEventListener('click', performReadinessEvaluation);

    // Bucket Filter Tabs
    document.querySelectorAll('.bucket-tab').forEach(tab => {
        tab.addEventListener('click', () => {
            const bucket = tab.getAttribute('data-bucket');
            state.selectedBucket = (state.selectedBucket === bucket) ? 'ALL' : bucket;
            renderApp();
        });
    });

    const btnShowAll = document.getElementById('btn-show-all-buckets');
    if (btnShowAll) {
        btnShowAll.addEventListener('click', () => {
            state.selectedBucket = 'ALL';
            renderApp();
        });
    }

    // Modal Triggers
    const btnOpenUpload = document.getElementById('btn-open-resume-upload');
    if (btnOpenUpload) btnOpenUpload.addEventListener('click', () => openModal('resume-modal'));

    const btnAddJobView = document.getElementById('btn-add-job-view');
    if (btnAddJobView) btnAddJobView.addEventListener('click', () => openModal('job-modal'));

    const btnEmptyAddJob = document.getElementById('btn-empty-add-job');
    if (btnEmptyAddJob) btnEmptyAddJob.addEventListener('click', () => openModal('job-modal'));

    const btnEmptyAddJob2 = document.getElementById('btn-empty-add-job-2');
    if (btnEmptyAddJob2) btnEmptyAddJob2.addEventListener('click', () => openModal('job-modal'));

    // Profile Edit Form Submit
    const profileForm = document.getElementById('profile-edit-form');
    if (profileForm) {
        profileForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const headline = document.getElementById('prof-headline').value;
            const targetRoles = document.getElementById('prof-roles').value;
            const summary = document.getElementById('prof-summary').value;

            try {
                await api.updateProfile({ headline, targetRoles, summary });
                showToast('Profile updated successfully!', 'success');
            } catch (err) {
                showToast(err.message || 'Profile update failed', 'error');
            }
        });
    }

    // Job Selection Cards
    document.querySelectorAll('.btn-select-job').forEach(btn => {
        btn.addEventListener('click', async () => {
            const id = btn.getAttribute('data-job-id');
            state.activeJobId = id;
            setActiveJobId(id);
            await loadInitialData();
            navigateTo('dashboard');
        });
    });

    // Evidence Discovery Triggers
    const btnGenEvidence = document.getElementById('btn-generate-evidence-questions');
    const btnStartDiscovery = document.getElementById('btn-start-discovery');
    const triggerEvidenceDiscovery = async () => {
        if (!state.activeJobId) return;
        state.loading = true;
        renderApp();
        try {
            showToast('Generating targeted evidence questions...', 'info');
            const res = await api.discoverEvidenceQuestions(state.activeJobId);
            state.evidenceQuestions = res.questions || (Array.isArray(res) ? res : []);
            showToast('Evidence questions generated!', 'success');
        } catch (err) {
            showToast(err.message || 'Failed to generate questions', 'error');
        } finally {
            state.loading = false;
            renderApp();
        }
    };
    if (btnGenEvidence) btnGenEvidence.addEventListener('click', triggerEvidenceDiscovery);
    if (btnStartDiscovery) btnStartDiscovery.addEventListener('click', triggerEvidenceDiscovery);

    // Answer Evidence Question Modal
    document.querySelectorAll('.btn-answer-evidence').forEach(btn => {
        btn.addEventListener('click', () => {
            const skill = btn.getAttribute('data-skill');
            const qText = decodeURIComponent(btn.getAttribute('data-question') || '');
            document.getElementById('evidence-skill-name').value = skill;
            document.getElementById('evidence-question-text').textContent = qText;
            document.getElementById('evidence-title').value = `Experience in ${skill}`;
            openModal('evidence-modal');
        });
    });

    document.querySelectorAll('.btn-verify-skill').forEach(btn => {
        btn.addEventListener('click', () => {
            const skill = btn.getAttribute('data-skill');
            document.getElementById('evidence-skill-name').value = skill;
            document.getElementById('evidence-question-text').textContent = `Have you worked with or built any projects using ${skill}?`;
            document.getElementById('evidence-title').value = `Project using ${skill}`;
            openModal('evidence-modal');
        });
    });

    // Tailoring Triggers
    const btnStartTailor = document.getElementById('btn-start-tailoring');
    const btnLaunchTailor = document.getElementById('btn-launch-tailoring');
    const triggerTailoring = async () => {
        if (!state.activeJobId) return;
        state.loading = true;
        renderApp();
        try {
            showToast('Generating truth-first tailored bullets from verified evidence...', 'info');
            const sourceResumeId = state.resumes[0]?.id || null;
            const session = await api.startTailoringSession(state.activeJobId, sourceResumeId);
            state.tailoringSession = session;
            state.claims = (session.claims || []).map(c => ({ ...c, evidenceType: 'RESUME_VERIFIED' }));
            showToast('Tailoring claims generated!', 'success');
        } catch (err) {
            showToast(err.message || 'Tailoring failed', 'error');
        } finally {
            state.loading = false;
            renderApp();
        }
    };
    if (btnStartTailor) btnStartTailor.addEventListener('click', triggerTailoring);
    if (btnLaunchTailor) btnLaunchTailor.addEventListener('click', triggerTailoring);

    // Claim Approvals / Rejections
    document.querySelectorAll('.btn-approve-claim').forEach(btn => {
        btn.addEventListener('click', async () => {
            const id = btn.getAttribute('data-claim-id');
            try {
                if (!state.isDemoMode) {
                    await api.updateClaim(id, 'APPROVED', null);
                }
                showToast('Claim approved for resume!', 'success');
                const target = state.claims.find(c => c.id === id);
                if (target) target.status = 'APPROVED';
                renderApp();
            } catch (e) {
                showToast('Action failed', 'error');
            }
        });
    });

    document.querySelectorAll('.btn-reject-claim').forEach(btn => {
        btn.addEventListener('click', async () => {
            const id = btn.getAttribute('data-claim-id');
            try {
                if (!state.isDemoMode) {
                    await api.updateClaim(id, 'REJECTED', null);
                }
                showToast('Claim rejected', 'info');
                const target = state.claims.find(c => c.id === id);
                if (target) target.status = 'REJECTED';
                renderApp();
            } catch (e) {
                showToast('Action failed', 'error');
            }
        });
    });

    // Compile Tailored Resume with Version Snapshot of Truth Bank
    const btnCompileResume = document.getElementById('btn-compile-resume');
    if (btnCompileResume) {
        btnCompileResume.addEventListener('click', async () => {
            try {
                showToast('Compiling tailored resume...', 'info');
                let content = '';
                if (state.tailoringSession?.id && !state.isDemoMode) {
                    const res = await api.generateTailoredResume(state.tailoringSession.id);
                    content = res.tailoredContent;
                } else {
                    const verifiedApproved = state.claims.filter(c => c.status === 'APPROVED' && c.evidenceType === 'RESUME_VERIFIED');
                    content = `# ${state.user?.firstName || 'Alex'} ${state.user?.lastName || 'Chen'}\n${state.user?.email || 'email@example.com'} | ${state.profile?.headline || 'Software Developer'}\n\n## Professional Summary\n${state.profile?.summary || 'Experienced software developer'}\n\n## Verified Technical Projects\n${verifiedApproved.map(c => `- ${c.proposedText}`).join('\n')}\n`;
                }

                // Save version with Truth Bank snapshot timestamp
                const jobTitle = state.jobs.find(j => j.id === state.activeJobId)?.title || 'Role';
                const tbDate = state.truthBankConfirmedTimestamp || new Date().toLocaleDateString();
                state.tailoredVersions.push({
                    id: 'v-' + Date.now(),
                    name: `${jobTitle} Tailored (v${state.tailoredVersions.length + 1})`,
                    truthBankDate: tbDate,
                    date: new Date().toLocaleDateString(),
                    jobTitle: jobTitle
                });

                showToast('Tailored resume compiled and version saved!', 'success');
                const blob = new Blob([content], { type: 'text/markdown' });
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `Tailored_Resume_${new Date().toISOString().slice(0, 10)}.md`;
                a.click();
            } catch (e) {
                showToast(e.message || 'Compilation failed', 'error');
            }
        });
    }

    // Roadmap Triggers
    const btnGenRoadmap = document.getElementById('btn-generate-roadmap');
    const btnEmptyRoadmap = document.getElementById('btn-empty-roadmap');
    const triggerRoadmap = async () => {
        if (!state.activeJobId) return;
        state.loading = true;
        renderApp();
        try {
            showToast('Generating personalized learning missions for closable gaps...', 'info');
            const missions = await api.generateRoadmap(state.activeJobId);
            state.roadmapMissions = missions;
            showToast('Learning roadmap ready!', 'success');
        } catch (err) {
            showToast(err.message || 'Roadmap generation failed', 'error');
        } finally {
            state.loading = false;
            renderApp();
        }
    };
    if (btnGenRoadmap) btnGenRoadmap.addEventListener('click', triggerRoadmap);
    if (btnEmptyRoadmap) btnEmptyRoadmap.addEventListener('click', triggerRoadmap);

    // Proof Submit Modal Trigger
    document.querySelectorAll('.btn-submit-proof').forEach(btn => {
        btn.addEventListener('click', () => {
            const mId = btn.getAttribute('data-mission-id');
            document.getElementById('proof-mission-id').value = mId;
            openModal('proof-modal');
        });
    });

    // Interview Defense Triggers
    const btnGenInterview = document.getElementById('btn-generate-interview');
    const btnEmptyInterview = document.getElementById('btn-empty-interview');
    const triggerInterview = async () => {
        if (!state.activeJobId) return;
        state.loading = true;
        renderApp();
        try {
            showToast('Generating interview defense questions...', 'info');
            const questions = await api.generateInterviewPrep(state.activeJobId);
            state.interviewQuestions = questions;
            showToast('Defense questions ready!', 'success');
        } catch (err) {
            showToast(err.message || 'Interview prep failed', 'error');
        } finally {
            state.loading = false;
            renderApp();
        }
    };
    if (btnGenInterview) btnGenInterview.addEventListener('click', triggerInterview);
    if (btnEmptyInterview) btnEmptyInterview.addEventListener('click', triggerInterview);
}

// Landing Page Event Listeners
function attachLandingEvents() {
    const openLogin = document.getElementById('btn-open-login');
    if (openLogin) openLogin.addEventListener('click', () => openModal('login-modal'));

    const openSignup = document.getElementById('btn-open-signup');
    if (openSignup) openSignup.addEventListener('click', () => openModal('signup-modal'));

    const heroStart = document.getElementById('btn-hero-start');
    if (heroStart) heroStart.addEventListener('click', () => openModal('signup-modal'));

    const heroLogin = document.getElementById('btn-hero-login');
    if (heroLogin) heroLogin.addEventListener('click', () => openModal('login-modal'));

    document.querySelectorAll('.btn-load-sample-demo').forEach(btn => {
        btn.addEventListener('click', loadSampleDemoData);
    });
}

// Initialize Application
async function init() {
    setupGlobalEvents();
    renderApp(); // Render page instantaneously!
    if (api.isLoggedIn()) {
        loadInitialData().then(() => {
            renderApp();
        }).catch((err) => {
            console.warn('Initial data load warning:', err);
        });
    }
}

// Launch
init();
