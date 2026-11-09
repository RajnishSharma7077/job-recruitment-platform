const state = {
    token: sessionStorage.getItem("workplace-token"),
    user: null,
    jobs: [],
    jobsPage: 0,
    jobsHasNext: false,
    totalJobs: 0,
    demoProfiles: [],
    companies: [],
    activeFilter: "all",
    search: { keyword: "", location: "" },
    applyJob: null,
    savedJobIds: new Set(JSON.parse(localStorage.getItem("workplace-saved-jobs") || "[]"))
};

const elements = {
    signin: document.querySelector("#signin-button"),
    signout: document.querySelector("#signout-button"),
    account: document.querySelector("#account-label"),
    dashboardButton: document.querySelector("#dashboard-button"),
    authDialog: document.querySelector("#auth-dialog"),
    applyDialog: document.querySelector("#apply-dialog"),
    authForm: document.querySelector("#auth-form"),
    authError: document.querySelector("#auth-error"),
    authTitle: document.querySelector("#auth-title"),
    authSubmit: document.querySelector("#auth-submit"),
    nameField: document.querySelector("#name-field"),
    roleField: document.querySelector("#role-field"),
    loginTab: document.querySelector("#login-tab"),
    registerTab: document.querySelector("#register-tab"),
    jobList: document.querySelector("#job-list"),
    jobCount: document.querySelector("#job-count"),
    jobError: document.querySelector("#job-error"),
    emptyState: document.querySelector("#empty-state"),
    dashboard: document.querySelector("#dashboard"),
    dashboardContent: document.querySelector("#dashboard-content"),
    toast: document.querySelector("#toast")
};

let authMode = "login";
let toastTimeout;

async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (options.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
    if (state.token) headers.set("Authorization", `Bearer ${state.token}`);

    const response = await fetch(path, { ...options, headers });
    const body = await response.text();
    let data = null;
    if (body) {
        try {
            data = JSON.parse(body);
        } catch {
            data = body;
        }
    }

    if (!response.ok) {
        if (response.status === 401 && state.token) clearSession();
        const message = typeof data === "string" ? data : data?.detail || data?.message || data?.error;
        throw new Error(message || `Request failed (${response.status})`);
    }
    return data;
}

function setSession(response) {
    state.token = response.token;
    state.user = { id: response.id, name: response.name, email: response.email, role: response.role };
    sessionStorage.setItem("workplace-token", response.token);
    updateAccount();
}

function clearSession() {
    state.token = null;
    state.user = null;
    sessionStorage.removeItem("workplace-token");
    if (elements.dashboard) elements.dashboard.hidden = true;
    updateAccount();
    renderJobs();
}

function updateAccount() {
    const signedIn = Boolean(state.user);
    if (elements.signin) elements.signin.hidden = signedIn;
    if (elements.signout) elements.signout.hidden = !signedIn;
    if (elements.dashboardButton) elements.dashboardButton.hidden = !signedIn;
    if (elements.account) {
        elements.account.hidden = !signedIn;
        elements.account.textContent = signedIn ? `Hi, ${state.user.name.split(" ")[0]}` : "";
    }
    const employerButton = document.querySelector("#employer-button");
    if (employerButton && document.body.dataset.page !== "employers") {
        employerButton.innerHTML = state.user?.role === "RECRUITER" ? "Open dashboard <span>↗</span>" : "I’m hiring <span>↗</span>";
    }
}

function escapeHtml(value = "") {
    return String(value).replace(/[&<>"']/g, character => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[character]);
}

function formatExperience(years) {
    if (years == null) return "Experience flexible";
    return `${years}+ ${years === 1 ? "year" : "years"} experience`;
}

function visibleJobs() {
    return state.jobs.filter(job => {
        if (state.activeFilter === "remote" && !/remote/i.test(job.location)) return false;
        if (state.activeFilter === "entry" && job.minExperience != null && job.minExperience > 2) return false;
        if (state.activeFilter === "saved" && !state.savedJobIds.has(job.id)) return false;
        const employmentType = document.querySelector("#employment-filter")?.value;
        if (employmentType && job.employmentType !== employmentType) return false;
        return true;
    });
}

function pageContent(response) {
    return Array.isArray(response) ? response : response?.content || [];
}

function renderJobs() {
    if (!elements.jobList) return;
    const jobs = visibleJobs();
    elements.jobList.innerHTML = jobs.map(job => {
        const company = job.companyName || "Growing team";
        const initials = company.trim().split(/\s+/).slice(0, 2).map(word => word[0]).join("");
        const isSaved = state.savedJobIds.has(job.id);
        return `
            <article class="job-card">
                <div class="job-card-top">
                    <span class="company-badge" aria-hidden="true">${escapeHtml(initials)}</span>
                    <div class="job-card-badges">
                        <span class="job-tag">${escapeHtml(job.employmentType || "Opportunity")}</span>
                        <button class="save-job-button ${isSaved ? "is-saved" : ""}" type="button" data-save-job-id="${job.id}" aria-label="${isSaved ? "Remove saved job" : "Save job"}" aria-pressed="${isSaved}">${isSaved ? "♥" : "♡"}</button>
                    </div>
                </div>
                <h3><a class="job-title-link" href="/job.html?id=${encodeURIComponent(job.id)}">${escapeHtml(job.title)}</a></h3>
                <p class="company-name">${escapeHtml(company)}</p>
                <p class="job-summary">${escapeHtml(job.description)}</p>
                <div class="job-meta">
                    <span class="job-meta-location">${escapeHtml(job.location || "Location flexible")}</span>
                    <span class="job-meta-experience">${escapeHtml(formatExperience(job.minExperience))}</span>
                    ${job.salaryRange ? `<span class="job-meta-salary">${escapeHtml(job.salaryRange)}</span>` : ""}
                </div>
                <div class="job-card-actions">
                    <span class="job-posted"><i></i>${escapeHtml(job.status || "OPEN")}</span>
                    <a class="button button-quiet apply-button" href="/job.html?id=${encodeURIComponent(job.id)}">View role ↗</a>
                </div>
            </article>`;
    }).join("");

    if (elements.jobCount) elements.jobCount.textContent = `${state.totalJobs} ${state.totalJobs === 1 ? "role" : "roles"} to explore`;
    if (elements.emptyState) elements.emptyState.hidden = jobs.length > 0;
    elements.jobList.hidden = jobs.length === 0;
    const loadMore = document.querySelector("#load-more-jobs");
    if (loadMore) loadMore.hidden = !state.jobsHasNext;
    const searchSummary = document.querySelector("#search-summary");
    if (searchSummary) searchSummary.textContent = state.search.keyword || state.search.location
        ? [state.search.keyword, state.search.location].filter(Boolean).join(" · ")
        : "";
    const savedCount = document.querySelector("#saved-count");
    if (savedCount) savedCount.textContent = state.savedJobIds.size;
    const resultNote = document.querySelector("#jobs-result-note");
    if (resultNote) resultNote.textContent = state.activeFilter === "saved"
        ? "Roles you saved on this browser."
        : "A thoughtful next step could be one search away.";
}

async function loadJobs() {
    if (!elements.jobList) return;
    if (elements.jobError) elements.jobError.hidden = true;
    if (elements.jobCount) elements.jobCount.textContent = "Finding opportunities…";
    state.jobs = [];
    state.jobsPage = 0;
    state.jobsHasNext = false;
    state.totalJobs = 0;
    const loadMore = document.querySelector("#load-more-jobs");
    if (loadMore) loadMore.hidden = true;
    const params = new URLSearchParams();
    if (state.search.keyword) params.set("keyword", state.search.keyword);
    if (state.search.location) params.set("location", state.search.location);
    params.set("page", "0");
    params.set("size", "20");
    params.set("sort", document.querySelector("#sort-filter")?.value || "newest");
    try {
        const response = await api(`/api/jobs/search?${params}`);
        state.jobs = pageContent(response);
        state.jobsPage = 0;
        state.jobsHasNext = !Array.isArray(response) && !response.last;
        state.totalJobs = Array.isArray(response) ? response.length : response.totalElements;
        renderJobs();
    } catch (error) {
        if (elements.jobCount) elements.jobCount.textContent = "Opportunities unavailable";
        elements.jobList.hidden = true;
        if (elements.emptyState) elements.emptyState.hidden = true;
        if (elements.jobError) {
            elements.jobError.textContent = error.message;
            elements.jobError.hidden = false;
        }
    }
}

async function loadMoreJobs() {
    if (!state.jobsHasNext) return;
    const button = document.querySelector("#load-more-jobs");
    if (button) {
        button.disabled = true;
        button.textContent = "Loading…";
    }
    const params = new URLSearchParams({
        page: String(state.jobsPage + 1),
        size: "20",
        sort: document.querySelector("#sort-filter")?.value || "newest"
    });
    if (state.search.keyword) params.set("keyword", state.search.keyword);
    if (state.search.location) params.set("location", state.search.location);
    try {
        const response = await api(`/api/jobs/search?${params}`);
        state.jobs = [...state.jobs, ...pageContent(response)];
        state.jobsPage = Array.isArray(response) ? state.jobsPage + 1 : response.page;
        state.jobsHasNext = !Array.isArray(response) && !response.last;
        renderJobs();
    } catch (error) {
        showToast(error.message, true);
    } finally {
        if (button) {
            button.disabled = false;
            button.textContent = "Load more roles";
        }
    }
}

function openAuth(mode = "login", role = null) {
    setAuthMode(mode);
    elements.authError.hidden = true;
    elements.authForm.reset();
    if (role && elements.authForm.elements.role) elements.authForm.elements.role.value = role;
    elements.authDialog.showModal();
}

async function loadDemoProfiles() {
    const section = document.querySelector("#demo-access");
    const list = document.querySelector("#demo-profile-list");
    if (!section || !list) return;

    try {
        state.demoProfiles = await api("/api/demo/profiles");
        if (!state.demoProfiles.length) return;
        list.innerHTML = state.demoProfiles.map(profile => `
            <button class="demo-profile-card" type="button" data-demo-role="${escapeHtml(profile.role)}">
                <span class="demo-avatar">${profile.role === "CANDIDATE" ? "AM" : "JL"}</span>
                <span class="demo-profile-copy">
                    <b>${escapeHtml(profile.name)}</b>
                    <small>${profile.role === "CANDIDATE" ? "Candidate preview" : "Recruiter preview"}</small>
                    <span>${escapeHtml(profile.summary)}</span>
                </span>
                <span class="demo-profile-arrow" aria-hidden="true">↗</span>
            </button>`).join("");
        section.hidden = false;
    } catch (error) {
        console.error("Could not load local demo profiles.", error);
    }
}

function enterDemoProfile(role) {
    const profile = state.demoProfiles.find(item => item.role === role);
    if (!profile) {
        showToast("That demo profile is not available in this environment.", true);
        return;
    }

    setAuthMode("login");
    elements.authForm.elements.email.value = profile.email;
    elements.authForm.elements.password.value = profile.password;
    elements.authError.hidden = true;
    elements.authForm.requestSubmit();
}

function setAuthMode(mode) {
    authMode = mode;
    const registering = mode === "register";
    elements.nameField.hidden = !registering;
    elements.roleField.hidden = !registering;
    elements.authTitle.textContent = registering ? "Make this your next move." : "Good to have you here.";
    elements.authSubmit.innerHTML = registering ? "Create account <span>↗</span>" : "Sign in <span>↗</span>";
    elements.loginTab.classList.toggle("is-active", !registering);
    elements.registerTab.classList.toggle("is-active", registering);
    elements.loginTab.setAttribute("aria-selected", String(!registering));
    elements.registerTab.setAttribute("aria-selected", String(registering));
    elements.authForm.elements.password.autocomplete = registering ? "new-password" : "current-password";
}

async function submitAuth(event) {
    event.preventDefault();
    elements.authError.hidden = true;
    elements.authSubmit.disabled = true;
    const formData = new FormData(elements.authForm);
    const payload = {
        email: formData.get("email").trim(),
        password: formData.get("password")
    };
    if (authMode === "register") {
        payload.name = formData.get("name").trim();
        payload.role = formData.get("role");
    }

    try {
        const response = await api(`/api/auth/${authMode === "register" ? "register" : "login"}`, {
            method: "POST",
            body: JSON.stringify(payload)
        });
        setSession(response);
        elements.authDialog.close();
        showToast(`Welcome${response.name ? `, ${response.name.split(" ")[0]}` : ""}. You're signed in.`);
        await loadJobs();
        if (response.role === "RECRUITER" || response.role === "ADMIN") {
            if (document.body.dataset.page === "jobs") {
                window.location.href = "/employers.html#dashboard";
            } else {
                await loadRecruiterDashboard();
            }
        }
    } catch (error) {
        elements.authError.textContent = error.message;
        elements.authError.hidden = false;
    } finally {
        elements.authSubmit.disabled = false;
    }
}

function handleJobAction(event) {
    const saveButton = event.target.closest("[data-save-job-id]");
    if (saveButton) {
        const jobId = Number(saveButton.dataset.saveJobId);
        if (state.savedJobIds.has(jobId)) state.savedJobIds.delete(jobId);
        else state.savedJobIds.add(jobId);
        localStorage.setItem("workplace-saved-jobs", JSON.stringify([...state.savedJobIds]));
        renderJobs();
        showToast(state.savedJobIds.has(jobId) ? "Saved to your list on this browser." : "Removed from your saved list.");
        return;
    }

    const button = event.target.closest("[data-job-id]");
    if (!button) return;
    const job = state.jobs.find(item => String(item.id) === button.dataset.jobId);
    if (job) openApplication(job);
}

function openApplication(job) {
    if (!state.user) {
        openAuth("login");
        return;
    }
    if (state.user.role !== "CANDIDATE") {
        showToast("Job applications are available to candidate accounts.", true);
        return;
    }
    state.applyJob = job;
    document.querySelector("#apply-job-title").textContent = `${job.title} · ${job.companyName}`;
    document.querySelector("#apply-error").hidden = true;
    document.querySelector("#apply-form").reset();
    elements.applyDialog.showModal();
}

async function loadJobDetail() {
    const content = document.querySelector("#job-detail-content");
    const loading = document.querySelector("#detail-loading");
    const error = document.querySelector("#detail-error");
    if (!content || !loading) return;

    try {
        const jobId = Number(new URLSearchParams(window.location.search).get("id"));
        if (!Number.isSafeInteger(jobId) || jobId <= 0) throw new Error("This job link is missing a valid role ID.");

        const job = await api(`/api/jobs/${jobId}`);
        state.applyJob = job;
        document.title = `${job.title} at ${job.companyName} — Workplace`;
        document.querySelector('meta[name="description"]')?.setAttribute("content", `${job.title} at ${job.companyName} in ${job.location}. Explore this opportunity on Workplace.`);
        document.querySelector("#detail-title").textContent = job.title;
        document.querySelector("#detail-company").textContent = job.companyName;
        document.querySelector("#detail-company-badge").textContent = job.companyName.trim().split(/\s+/).slice(0, 2).map(word => word[0]).join("");
        document.querySelector("#detail-description").textContent = job.description;
        document.querySelector("#detail-status").textContent = job.status;
        document.querySelector("#detail-facts").innerHTML = `
            <span><i>⌖</i>${escapeHtml(job.location || "Location flexible")}</span>
            <span><i>◷</i>${escapeHtml(job.employmentType)}</span>
            <span><i>↗</i>${escapeHtml(formatExperience(job.minExperience))}</span>
            ${job.salaryRange ? `<span><i>＄</i>${escapeHtml(job.salaryRange)}</span>` : ""}`;
        document.querySelector("#aside-title").textContent = job.companyName;
        document.querySelector("#aside-copy").textContent = `${job.employmentType} · ${job.location} · ${formatExperience(job.minExperience)}`;
        updateDetailSaveButton(jobId);
        content.hidden = false;
        loading.hidden = true;
        if (error) error.hidden = true;
    } catch (detailError) {
        loading.hidden = true;
        if (error) {
            error.textContent = detailError.message;
            error.hidden = false;
        }
    }
}

function updateDetailSaveButton(jobId) {
    const button = document.querySelector("#detail-save");
    if (!button) return;
    const saved = state.savedJobIds.has(jobId);
    button.setAttribute("aria-pressed", String(saved));
    button.innerHTML = `${saved ? "♥" : "♡"} <span>${saved ? "Saved" : "Save role"}</span>`;
}

function toggleDetailSavedJob() {
    const jobId = state.applyJob?.id;
    if (!jobId) return;
    if (state.savedJobIds.has(jobId)) state.savedJobIds.delete(jobId);
    else state.savedJobIds.add(jobId);
    localStorage.setItem("workplace-saved-jobs", JSON.stringify([...state.savedJobIds]));
    updateDetailSaveButton(jobId);
    showToast(state.savedJobIds.has(jobId) ? "Saved to your list on this browser." : "Removed from your saved list.");
}

async function submitApplication(event) {
    event.preventDefault();
    const submit = event.currentTarget.querySelector("button[type=submit]");
    const errorElement = document.querySelector("#apply-error");
    errorElement.hidden = true;
    submit.disabled = true;
    try {
        const resumeUrl = new FormData(event.currentTarget).get("resumeUrl").trim();
        await api("/api/applications/apply", {
            method: "POST",
            body: JSON.stringify({ jobId: state.applyJob.id, resumeUrl })
        });
        elements.applyDialog.close();
        showToast("Application sent. Good luck with your next step.");
        await loadCandidateDashboard();
    } catch (error) {
        errorElement.textContent = error.message;
        errorElement.hidden = false;
    } finally {
        submit.disabled = false;
    }
}

function showToast(message, isError = false) {
    clearTimeout(toastTimeout);
    elements.toast.textContent = message;
    elements.toast.classList.toggle("is-error", isError);
    elements.toast.classList.add("is-visible");
    toastTimeout = setTimeout(() => elements.toast.classList.remove("is-visible"), 3400);
}

function renderApplication(item) {
    return `<div class="dashboard-item">
        <span><b>${escapeHtml(item.jobTitle)}</b><small>${escapeHtml(item.companyName)} · Applied ${new Date(item.appliedAt).toLocaleDateString()}</small></span>
        <span class="status-pill">${escapeHtml(item.status)}</span>
    </div>`;
}

async function loadCandidateDashboard() {
    const dashboardTitle = document.querySelector("#dashboard-title");
    if (!elements.dashboard || !elements.dashboardContent || !dashboardTitle) {
        window.location.href = "/jobs.html#dashboard";
        return;
    }
    dashboardTitle.innerHTML = "Your applications<span class=\"heading-period\">.</span>";
    document.querySelector("#dashboard-description").textContent = "Keep track of every next step.";
    elements.dashboardContent.innerHTML = `<div class="dashboard-panel"><h3>Application activity</h3><div class="dashboard-list" id="candidate-applications">Loading your applications…</div></div>`;
    elements.dashboard.hidden = false;
    try {
        const applications = pageContent(await api("/api/applications/mine"));
        const list = document.querySelector("#candidate-applications");
        list.innerHTML = applications.length
            ? applications.map(renderApplication).join("")
            : '<p class="dashboard-empty">You haven’t applied to a role yet. Explore the opportunities above when you’re ready.</p>';
    } catch (error) {
        elements.dashboardContent.innerHTML = `<p class="notice notice-error">${escapeHtml(error.message)}</p>`;
    }
}

async function loadRecruiterDashboard() {
    if (!elements.dashboard || !elements.dashboardContent || !document.querySelector("#dashboard-title")) {
        window.location.href = "/employers.html#dashboard";
        return;
    }
    document.querySelector("#dashboard-title").innerHTML = "Build your team<span class=\"heading-period\">.</span>";
    document.querySelector("#dashboard-description").textContent = "Post a role, meet thoughtful candidates, and keep the conversation moving.";
    elements.dashboardContent.innerHTML = `
        <div class="dashboard-grid">
            <section class="dashboard-panel">
                <h3>Post an opportunity</h3>
                <form id="job-form" class="form-stack">
                    <label>Job title<input name="title" placeholder="e.g. Product Designer" required></label>
                    <label>About the role<input name="description" placeholder="What will this person work on?" required></label>
                    <label>Location<input name="location" placeholder="City or Remote" required></label>
                    <label>Employment type
                        <select name="employmentType"><option>Full-time</option><option>Part-time</option><option>Contract</option><option>Internship</option></select>
                    </label>
                    <label>Minimum experience (years)<input name="minExperience" type="number" min="0" max="60" placeholder="e.g. 2"></label>
                    <label>Salary range<input name="salaryRange" placeholder="e.g. ₹8–12 LPA"></label>
                    <label>Company
                        <select name="companyId" id="company-select" required><option value="">Loading companies…</option></select>
                    </label>
                    <p id="job-form-error" class="form-error" hidden></p>
                    <button class="button button-green" type="submit">Publish role <span>↗</span></button>
                </form>
            </section>
            <section class="dashboard-panel">
                <h3>Your roles</h3>
                <div class="dashboard-list" id="recruiter-jobs">Loading your roles…</div>
            </section>
        </div>
        <section class="dashboard-panel dashboard-section">
            <h3>Candidate applications</h3>
            <div class="application-list" id="recruiter-applications">Loading applications…</div>
        </section>`;

    elements.dashboard.hidden = false;
    document.querySelector("#job-form").addEventListener("submit", submitJob);
    await Promise.all([loadCompanies(), loadRecruiterData()]);
}

async function loadCompanies() {
    const select = document.querySelector("#company-select");
    if (!select) return;
    try {
        state.companies = await api("/api/companies");
        select.innerHTML = state.companies.length
            ? state.companies.map(company => `<option value="${company.id}">${escapeHtml(company.name)}</option>`).join("")
            : '<option value="">No companies yet</option>';
        if (!state.companies.length) {
            select.insertAdjacentHTML("afterend", '<p class="field-help">Ask an admin to add your company before posting.</p>');
        }
    } catch (error) {
        select.innerHTML = '<option value="">Companies unavailable</option>';
        showToast(error.message, true);
    }
}

async function loadRecruiterData() {
    const jobsList = document.querySelector("#recruiter-jobs");
    const applicationsList = document.querySelector("#recruiter-applications");
    try {
        const [jobsResponse, applicationsResponse] = await Promise.all([
            api("/api/jobs/my"),
            api("/api/applications/recruiter/me")
        ]);
        const jobs = pageContent(jobsResponse);
        const applications = pageContent(applicationsResponse);
        if (jobsList) {
            jobsList.innerHTML = jobs.length ? jobs.map(job => `
                <div class="dashboard-item">
                    <span><b>${escapeHtml(job.title)}</b><small>${escapeHtml(job.companyName)} · ${escapeHtml(job.location)}</small></span>
                    <span class="status-pill">${escapeHtml(job.status)}</span>
                </div>`).join("") : '<p class="dashboard-empty">No roles posted yet. Your next great hire starts with one.</p>';
        }
        if (applicationsList) {
            applicationsList.innerHTML = applications.length
                ? applications.map(renderApplication).join("")
                : '<p class="dashboard-empty">Applications will appear here when candidates apply to your roles.</p>';
        }
    } catch (error) {
        if (jobsList) jobsList.innerHTML = `<p class="form-error">${escapeHtml(error.message)}</p>`;
        if (applicationsList) applicationsList.innerHTML = "";
    }
}

async function submitJob(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const values = new FormData(form);
    const errorElement = document.querySelector("#job-form-error");
    errorElement.hidden = true;
    const submit = form.querySelector("button[type=submit]");
    submit.disabled = true;
    const minExperience = values.get("minExperience");
    const payload = {
        title: values.get("title").trim(),
        description: values.get("description").trim(),
        location: values.get("location").trim(),
        employmentType: values.get("employmentType"),
        minExperience: minExperience ? Number(minExperience) : null,
        salaryRange: values.get("salaryRange").trim() || null,
        companyId: Number(values.get("companyId"))
    };
    try {
        await api("/api/jobs", { method: "POST", body: JSON.stringify(payload) });
        showToast("Your role is live. Let’s find your next teammate.");
        form.reset();
        await Promise.all([loadJobs(), loadRecruiterData()]);
    } catch (error) {
        errorElement.textContent = error.message;
        errorElement.hidden = false;
    } finally {
        submit.disabled = false;
    }
}

async function openDashboard() {
    if (!state.user) {
        openAuth("login");
        return;
    }
    if (state.user.role === "CANDIDATE") await loadCandidateDashboard();
    else await loadRecruiterDashboard();
    if (elements.dashboard) elements.dashboard.scrollIntoView({ behavior: "smooth", block: "start" });
}

async function restoreSession() {
    if (!state.token) return;
    try {
        state.user = await api("/api/users/me");
        updateAccount();
    } catch {
        clearSession();
    }
}

const searchForm = document.querySelector("#search-form");
if (searchForm) searchForm.addEventListener("submit", event => {
    event.preventDefault();
    state.search.keyword = document.querySelector("#keyword").value.trim();
    state.search.location = document.querySelector("#location").value.trim();
    if (searchForm.dataset.searchPage === "true") {
        const params = new URLSearchParams();
        if (state.search.keyword) params.set("keyword", state.search.keyword);
        if (state.search.location) params.set("location", state.search.location);
        window.location.href = `/jobs.html${params.size ? `?${params}` : ""}`;
        return;
    }
    state.activeFilter = "all";
    document.querySelectorAll(".filter-chip").forEach(chip => chip.classList.toggle("is-active", chip.dataset.filter === "all"));
    loadJobs();
    document.querySelector("#opportunities")?.scrollIntoView({ behavior: "smooth", block: "start" });
});

document.querySelectorAll(".filter-chip").forEach(chip => chip.addEventListener("click", () => {
    state.activeFilter = chip.dataset.filter;
    document.querySelectorAll(".filter-chip").forEach(item => item.classList.toggle("is-active", item === chip));
    renderJobs();
}));

document.querySelector("#clear-search")?.addEventListener("click", () => {
    document.querySelector("#search-form").reset();
    state.search = { keyword: "", location: "" };
    state.activeFilter = "all";
    document.querySelectorAll(".filter-chip").forEach(chip => chip.classList.toggle("is-active", chip.dataset.filter === "all"));
    loadJobs();
});

elements.jobList?.addEventListener("click", handleJobAction);
elements.signin?.addEventListener("click", () => openAuth("login"));
elements.signout?.addEventListener("click", () => {
    clearSession();
    showToast("You’ve signed out.");
});
elements.dashboardButton?.addEventListener("click", openDashboard);
document.querySelector("#employer-button")?.addEventListener("click", () => {
    if (state.user?.role === "RECRUITER" || state.user?.role === "ADMIN") openDashboard();
    else if (state.user) showToast("Sign out before creating a recruiter account.", true);
    else openAuth("register", "RECRUITER");
});
document.querySelector("#refresh-dashboard")?.addEventListener("click", openDashboard);
elements.loginTab?.addEventListener("click", () => setAuthMode("login"));
elements.registerTab?.addEventListener("click", () => setAuthMode("register"));
elements.authForm?.addEventListener("submit", submitAuth);
document.querySelector("#demo-profile-list")?.addEventListener("click", event => {
    const button = event.target.closest("[data-demo-role]");
    if (button) enterDemoProfile(button.dataset.demoRole);
});
document.querySelectorAll("[data-open-guide]").forEach(button => button.addEventListener("click", () => {
    elements.authDialog?.close();
    document.querySelector("#guide-dialog")?.showModal();
}));
document.querySelector("#apply-form")?.addEventListener("submit", submitApplication);
document.querySelectorAll("[data-close-dialog]").forEach(button => button.addEventListener("click", () => {
    document.getElementById(button.dataset.closeDialog).close();
}));
[elements.authDialog, elements.applyDialog, document.querySelector("#guide-dialog")].filter(Boolean).forEach(dialog => dialog.addEventListener("click", event => {
    if (event.target === dialog) dialog.close();
}));

updateAccount();
const initialSearch = new URLSearchParams(window.location.search);
state.search.keyword = initialSearch.get("keyword") || "";
state.search.location = initialSearch.get("location") || "";
if (document.querySelector("#keyword")) document.querySelector("#keyword").value = state.search.keyword;
if (document.querySelector("#location")) document.querySelector("#location").value = state.search.location;
document.querySelector("#employment-filter")?.addEventListener("change", renderJobs);
document.querySelector("#sort-filter")?.addEventListener("change", loadJobs);
document.querySelector("#load-more-jobs")?.addEventListener("click", loadMoreJobs);
document.querySelector("#detail-save")?.addEventListener("click", toggleDetailSavedJob);
document.querySelector("#detail-apply")?.addEventListener("click", () => {
    if (state.applyJob) openApplication(state.applyJob);
});
document.querySelector("#aside-apply")?.addEventListener("click", () => {
    if (state.applyJob) openApplication(state.applyJob);
});
document.querySelector("#save-alert")?.addEventListener("click", () => {
    const alert = { ...state.search, createdAt: new Date().toISOString() };
    localStorage.setItem("workplace-job-alert", JSON.stringify(alert));
    showToast("Search preferences saved on this browser. Email alerts can be added next.");
});
restoreSession().finally(() => {
    if (elements.jobList) loadJobs();
    if (document.body.dataset.page === "job-detail") loadJobDetail();
    if (window.location.hash === "#dashboard" && state.user) openDashboard();
});
loadDemoProfiles();
