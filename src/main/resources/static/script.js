const API_URL = "http://localhost:8080/api";

// Login
async function login(username, password) {
    try {
    console.log("hi")
        const response = await fetch(`${API_URL}/login`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                username: username,
                password: password
            })
        });

        const data = await response.json();

        if (response.ok) {
            console.log("Login successful:", data);
            alert("Login successful!");

            // Example: redirect after successful login
            // window.location.href = "home.html";
        } else {
            console.error("Login failed:", data);
            alert(data.message || "Invalid username or password");
        }

    } catch (error) {
        console.error("Error:", error);
        alert("Unable to connect to the server.");
    }
}


// Register
async function register(username, password) {
    try {
        const response = await fetch(`${API_URL}/register`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                username: username,
                password: password
            })
        });

        const data = await response.json();

        if (response.ok) {
            console.log("Registration successful:", data);
            alert("Registration successful!");
        } else {
            console.error("Registration failed:", data);
            alert(data.message || "Registration failed");
        }

    } catch (error) {
        console.error("Error:", error);
        alert("Unable to connect to the server.");
    }
}


// Login form
const loginForm = document.querySelector("form");

loginForm.addEventListener("submit", function (event) {
    event.preventDefault();

    const username = document.querySelector(
        'input[name="username"]'
    ).value;

    const password = document.querySelector(
        'input[name="password"]'
    ).value;

    login(username, password);
});