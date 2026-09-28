const API_URL = "/api";

// ── Helper: show alert inside the page instead of browser alert() ──
function showAlert(message, type = "danger") {
    const box = document.getElementById("alert-box");
    if (box) {
        box.innerHTML = `<div class="alert alert-${type} alert-dismissible fade show" role="alert">
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>`;
    }
}

// ── Login ──
async function login(username, password) {
    try {
        const response = await fetch(`${API_URL}/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password })
        });

        const text = await response.text();

        if (response.status === 302 || response.ok) {
            showAlert("Login successful! Redirecting...", "success");
            setTimeout(() => { window.location.href = "/dashboard.html"; }, 1000);
        } else {
            showAlert(text || "Invalid username or password.");
        }
    } catch (error) {
        console.error("Error:", error);
        showAlert("Unable to connect to the server.");
    }
}

// ── Register ──
async function register(name, password) {
    try {
        const response = await fetch(`${API_URL}/register`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name, password })
        });

        const text = await response.text();

        if (response.status === 201 || response.ok) {
            showAlert("Registration successful! Redirecting to login...", "success");
            setTimeout(() => { window.location.href = "/login.html"; }, 1200);
        } else {
            showAlert(text || "Registration failed. Please try again.");
        }
    } catch (error) {
        console.error("Error:", error);
        showAlert("Unable to connect to the server.");
    }
}

// ── Attach Login form listener ──
const loginForm = document.getElementById("loginForm");
if (loginForm) {
    loginForm.addEventListener("submit", function (event) {
        event.preventDefault();
        const username = document.getElementById("loginUsername").value.trim();
        const password = document.getElementById("loginPassword").value;
        login(username, password);
    });
}

// ── Attach Register form listener ──
const registerForm = document.getElementById("registerForm");
if (registerForm) {
    registerForm.addEventListener("submit", function (event) {
        event.preventDefault();
        const name = document.getElementById("registerUsername").value.trim();
        const password = document.getElementById("registerPassword").value;
        register(name, password);
    });
}