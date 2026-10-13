/* Public website: mobile menu, date-picker helpers, photo viewer, confirm-before-cancel. */
(function () {
    'use strict';

    // Mobile menu
    var toggle = document.querySelector('[data-nav-toggle]');
    var links = document.getElementById('nav-links');
    if (toggle && links) {
        toggle.addEventListener('click', function () {
            var open = links.classList.toggle('open');
            toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
        });
    }

    // Check-out must be after check-in: move its minimum (and value) along when check-in changes.
    document.addEventListener('change', function (e) {
        var checkIn = e.target.closest('[data-checkin]');
        if (!checkIn || !checkIn.form || !checkIn.value) return;
        var out = checkIn.form.querySelector('[data-checkout]');
        if (!out) return;
        var next = new Date(checkIn.value + 'T00:00:00');
        next.setDate(next.getDate() + 1);
        var min = next.getFullYear() + '-' + String(next.getMonth() + 1).padStart(2, '0') + '-' + String(next.getDate()).padStart(2, '0');
        out.min = min;
        if (!out.value || out.value < min) {
            out.value = min;
            out.dispatchEvent(new Event('change', { bubbles: true })); // refresh the live price
        }
    });

    // Full-screen photo viewer on the room page
    var lightbox = document.getElementById('lightbox');
    if (lightbox && typeof lightbox.showModal === 'function') {
        document.addEventListener('click', function (e) {
            var opener = e.target.closest('[data-gallery-open]');
            if (opener) {
                lightbox.showModal();
                var img = lightbox.querySelector('[data-index="' + opener.getAttribute('data-gallery-open') + '"]');
                if (img) img.scrollIntoView({ block: 'start' });
            }
            if (e.target.closest('[data-lightbox-close]') || e.target === lightbox) lightbox.close();
        });
    }

    // "Are you sure?" before cancelling a booking
    document.addEventListener('submit', function (e) {
        var message = e.target.getAttribute('data-confirm');
        if (message && !window.confirm(message)) e.preventDefault();
    });
})();
