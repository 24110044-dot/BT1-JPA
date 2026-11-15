document.addEventListener("DOMContentLoaded", function () {
    const imageInput = document.getElementById("imageFile");
    const imageUrlInput = document.getElementById("images");
    const preview = document.querySelector(".category-preview");

    if (imageInput && preview) {
        imageInput.addEventListener("change", function () {
            const file = imageInput.files && imageInput.files[0];
            if (file && file.type.startsWith("image/")) {
                preview.src = URL.createObjectURL(file);
            }
        });
    }

    if (imageUrlInput && preview) {
        imageUrlInput.addEventListener("input", function () {
            if (imageUrlInput.value.trim()) {
                preview.src = imageUrlInput.value.trim();
            }
        });
    }
});
