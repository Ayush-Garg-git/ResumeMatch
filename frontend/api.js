/**
 * Job Readiness Copilot - Live API Client
 * Connects directly to the Spring Boot REST API
 * Powered by Google Gemini
 */

const DEFAULT_REMOTE_BACKEND = 'https://resumematch-fcnz.onrender.com/api/v1';

export const getApiBaseUrl = () => {
    if (typeof window !== 'undefined') {
        const customUrl = localStorage.getItem('jr_api_base_url');
        if (customUrl) return customUrl;
        if (window.API_BASE_URL) return window.API_BASE_URL;
        if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
            return 'http://localhost:8080/api/v1';
        }
    }
    return DEFAULT_REMOTE_BACKEND;
};

export const setApiBaseUrl = (url) => {
    if (url) localStorage.setItem('jr_api_base_url', url);
    else localStorage.removeItem('jr_api_base_url');
};

// Session Storage
export const getAuthToken = () => localStorage.getItem('jr_token');
export const setAuthToken = (token) => {
    if (token) localStorage.setItem('jr_token', token);
    else localStorage.removeItem('jr_token');
};

export const getAuthUser = () => JSON.parse(localStorage.getItem('jr_user') || 'null');
export const setAuthUser = (user) => {
    if (user) localStorage.setItem('jr_user', JSON.stringify(user));
    else localStorage.removeItem('jr_user');
};

export const getActiveJobId = () => localStorage.getItem('jr_active_job_id');
export const setActiveJobId = (jobId) => {
    if (jobId) localStorage.setItem('jr_active_job_id', jobId);
    else localStorage.removeItem('jr_active_job_id');
};

// Generic HTTP Request Handler
async function request(endpoint, options = {}) {
    const token = getAuthToken();
    const headers = {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
        ...options.headers
    };

    const config = {
        ...options,
        headers
    };

    let coldStartTimer = setTimeout(() => {
        if (typeof window !== 'undefined' && window.showToast) {
            window.showToast('Connecting to cloud backend... (Render instance waking up)', 'info');
        }
    }, 4000);

    try {
        const baseUrl = getApiBaseUrl();
        const response = await fetch(`${baseUrl}${endpoint}`, config);
        
        // Handle 401 Unauthorized / 403 Forbidden
        if (response.status === 401 || response.status === 403) {
            setAuthToken(null);
            setAuthUser(null);
            throw new Error('Session expired or unauthorized. Please log in again.');
        }

        const text = await response.text();
        let data = null;
        if (text && text.trim().length > 0) {
            try {
                data = JSON.parse(text);
            } catch (e) {
                data = { message: text };
            }
        }

        if (!response.ok) {
            throw new Error((data && data.message) || `Request failed with status ${response.status}`);
        }
        return data || {};
    } catch (err) {
        console.error(`API request error on ${endpoint}:`, err);
        throw err;
    } finally {
        clearTimeout(coldStartTimer);
    }
}

// Multipart File Upload Handler
async function uploadRequest(endpoint, formData) {
    const token = getAuthToken();
    const headers = {
        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    };

    let coldStartTimer = setTimeout(() => {
        if (typeof window !== 'undefined' && window.showToast) {
            window.showToast('Connecting to cloud backend... (Render instance waking up)', 'info');
        }
    }, 4000);

    try {
        const baseUrl = getApiBaseUrl();
        const response = await fetch(`${baseUrl}${endpoint}`, {
            method: 'POST',
            headers,
            body: formData
        });

        if (response.status === 401 || response.status === 403) {
            setAuthToken(null);
            setAuthUser(null);
            throw new Error('Session expired or unauthorized. Please log in again.');
        }

        const text = await response.text();
        let data = null;
        if (text && text.trim().length > 0) {
            try {
                data = JSON.parse(text);
            } catch (e) {
                data = { message: text };
            }
        }

        if (!response.ok) {
            throw new Error((data && data.message) || `File upload failed with status ${response.status}`);
        }
        return data || {};
    } catch (err) {
        console.error(`Upload error on ${endpoint}:`, err);
        throw err;
    } finally {
        clearTimeout(coldStartTimer);
    }
}

const api = {
    // 1. Authentication
    async signup(email, password, firstName, lastName) {
        const res = await request('/auth/signup', {
            method: 'POST',
            body: JSON.stringify({ email, password, firstName, lastName })
        });
        if (res.success && res.data) {
            setAuthToken(res.data.token);
            const userObj = {
                id: res.data.userId,
                email: res.data.email,
                firstName: firstName || 'User',
                lastName: lastName || ''
            };
            setAuthUser(userObj);
            return { token: res.data.token, user: userObj };
        }
        return res;
    },

    async login(email, password) {
        const res = await request('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });
        if (res.success && res.data) {
            setAuthToken(res.data.token);
            const userObj = {
                id: res.data.userId,
                email: res.data.email,
                firstName: email.split('@')[0],
                lastName: ''
            };
            setAuthUser(userObj);
            return { token: res.data.token, user: userObj };
        }
        return res;
    },

    logout() {
        setAuthToken(null);
        setAuthUser(null);
        setActiveJobId(null);
        window.location.hash = '#login';
    },

    isLoggedIn() {
        return !!getAuthToken();
    },

    getUser() {
        return getAuthUser();
    },

    // 2. Career Profile
    async getProfile() {
        const res = await request('/profile');
        return res.data || {};
    },

    async updateProfile(profileData) {
        const res = await request('/profile', {
            method: 'PUT',
            body: JSON.stringify(profileData)
        });
        return res.data;
    },

    // 3. Resumes
    async uploadResume(file) {
        const formData = new FormData();
        formData.append('file', file);
        const res = await uploadRequest('/resumes/upload', formData);
        return res.data;
    },

    async getResumes() {
        const res = await request('/resumes');
        return res.data || [];
    },

    async getResume(id) {
        const res = await request(`/resumes/${id}`);
        return res.data;
    },

    // 4. Jobs & AI Parsing
    async createJob(title, company, rawJdText) {
        const res = await request('/jobs', {
            method: 'POST',
            body: JSON.stringify({ title, company, rawJdText })
        });
        if (res.data && res.data.id) {
            setActiveJobId(res.data.id);
        }
        return res.data;
    },

    async getJobs() {
        const res = await request('/jobs');
        return res.data || [];
    },

    async getJob(id) {
        const res = await request(`/jobs/${id}`);
        return res.data;
    },

    // 5. Readiness Analysis (4-Bucket Evaluation)
    async performReadiness(jobId) {
        const res = await request(`/readiness?jobId=${jobId}`, {
            method: 'POST'
        });
        return res.data;
    },

    async getLatestAnalysis(jobId) {
        const res = await request(`/readiness/job/${jobId}`);
        return res.data;
    },

    // 6. Evidence Discovery
    async discoverEvidenceQuestions(jobId) {
        const res = await request(`/evidence/discover?jobId=${jobId}`, {
            method: 'POST'
        });
        let parsed = res.data;
        if (typeof parsed === 'string') {
            try {
                parsed = JSON.parse(parsed);
            } catch (e) {
                parsed = { questions: [] };
            }
        }
        return parsed;
    },

    async saveEvidence(evidenceData) {
        const res = await request('/evidence', {
            method: 'POST',
            body: JSON.stringify(evidenceData)
        });
        return res.data;
    },

    async getEvidenceBank() {
        const res = await request('/evidence');
        return res.data || [];
    },

    // 7. Resume Tailoring & Claims
    async startTailoringSession(jobId, sourceResumeId) {
        const res = await request('/tailoring/session', {
            method: 'POST',
            body: JSON.stringify({ jobId, sourceResumeId })
        });
        return res.data;
    },

    async getSessionClaims(sessionId) {
        const res = await request(`/tailoring/session/${sessionId}/claims`);
        return res.data || [];
    },

    async updateClaim(claimId, status, proposedText) {
        const params = new URLSearchParams();
        if (status) params.append('status', status);
        if (proposedText) params.append('proposedText', proposedText);
        
        const res = await request(`/tailoring/claims/${claimId}?${params.toString()}`, {
            method: 'PUT'
        });
        return res.data;
    },

    async generateTailoredResume(sessionId) {
        const res = await request(`/tailoring/session/${sessionId}/generate`, {
            method: 'POST'
        });
        return res.data;
    },

    // 8. Learning Roadmap
    async generateRoadmap(jobId) {
        const res = await request(`/roadmap/generate?jobId=${jobId}`, {
            method: 'POST'
        });
        return res.data || [];
    },

    async getRoadmap() {
        const res = await request('/roadmap');
        return res.data || [];
    },

    async submitProof(missionId, proofType, proofUrl, proofContent) {
        const res = await request(`/roadmap/mission/${missionId}/proof`, {
            method: 'POST',
            body: JSON.stringify({ proofType, proofUrl, proofContent })
        });
        return res.data;
    },

    // 9. Mock Interview Lab
    async generateInterviewPrep(jobId) {
        const res = await request(`/interview/prep/${jobId}`, {
            method: 'POST'
        });
        return res.data || [];
    },

    async getInterviewPrep(jobId) {
        const res = await request(`/interview/prep/${jobId}`);
        return res.data || [];
    }
};

export default api;
