document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll("[data-otp-form]").forEach(function (form) {
        var digits = Array.from(form.querySelectorAll("[data-otp-digit]"));
        var code = form.querySelector("[data-otp-value]");
        var fallback = form.querySelector("[data-otp-fallback]");
        var digitGroup = form.querySelector(".otp-inputs");
        if (digits.length !== 6 || !code || !fallback || !digitGroup) {
            return;
        }

        fallback.removeAttribute("name");
        fallback.hidden = true;
        fallback.required = false;
        code.name = "code";
        digitGroup.hidden = false;
        digits.forEach(function (digit) {
            digit.disabled = false;
            digit.required = true;
        });

        function syncCode() {
            code.value = digits.map(function (digit) {
                return digit.value;
            }).join("");
        }

        digits.forEach(function (digit, index) {
            digit.addEventListener("input", function () {
                var numbers = digit.value.replace(/\D/g, "");
                digit.value = numbers.slice(-1);
                syncCode();
                if (digit.value && index < digits.length - 1) {
                    digits[index + 1].focus();
                }
            });

            digit.addEventListener("keydown", function (event) {
                if (event.key === "Backspace" && !digit.value && index > 0) {
                    digits[index - 1].focus();
                    digits[index - 1].value = "";
                    syncCode();
                } else if (event.key === "ArrowLeft" && index > 0) {
                    digits[index - 1].focus();
                } else if (event.key === "ArrowRight" && index < digits.length - 1) {
                    digits[index + 1].focus();
                }
            });

            digit.addEventListener("paste", function (event) {
                var clipboard = event.clipboardData || window.clipboardData;
                var pasted = clipboard.getData("text").replace(/\D/g, "").slice(0, digits.length);
                if (!pasted) {
                    return;
                }
                event.preventDefault();
                digits.forEach(function (field, fieldIndex) {
                    field.value = pasted[fieldIndex] || "";
                });
                syncCode();
                digits[Math.min(pasted.length, digits.length - 1)].focus();
            });
        });

        form.addEventListener("submit", function (event) {
            syncCode();
            if (code.value.length !== digits.length) {
                event.preventDefault();
                digits.find(function (digit) {
                    return !digit.value;
                }).focus();
            }
        });
    });

    document.querySelectorAll("[data-resend-form]").forEach(function (form) {
        var button = form.querySelector("[data-resend-button]");
        var countdown = document.querySelector("[data-resend-countdown]");
        var successAlert = document.querySelector(".alert-success");
        if (!button || !countdown || !successAlert) {
            return;
        }

        var secondsRemaining = 60;
        button.disabled = true;
        function updateCountdown() {
            if (secondsRemaining <= 0) {
                button.disabled = false;
                countdown.textContent = "";
                return;
            }
            countdown.textContent = "Có thể gửi lại sau " + secondsRemaining + " giây.";
            secondsRemaining -= 1;
            window.setTimeout(updateCountdown, 1000);
        }
        updateCountdown();
    });
});
