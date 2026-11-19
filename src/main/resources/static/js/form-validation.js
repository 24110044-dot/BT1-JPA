(() => {
    const forms = document.querySelectorAll("form.needs-validation");
    forms.forEach((form) => {
        form.addEventListener("submit", (event) => {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            form.classList.add("was-validated");
        });
    });

    document.querySelectorAll("input, textarea, select").forEach((field) => {
        field.addEventListener("input", () => {
            if (field.closest("form")?.classList.contains("was-validated")) {
                field.classList.toggle("is-invalid", !field.checkValidity());
                field.classList.toggle("is-valid", field.checkValidity());
            }
        });
    });
})();
