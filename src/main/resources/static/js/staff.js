/*
 * Staff portal behaviour on top of HTMX:
 *  - sends the CSRF token with every HTMX request
 *  - opens/closes the slide-over drawer
 *  - shows toast messages sent by the server in the HX-Trigger header
 *  - clickable table rows, mobile sidebar, small form helpers
 */
(function () {
    'use strict';

    // ---- CSRF: Spring Security rejects POST/PUT/DELETE without it ----
    var tokenMeta = document.querySelector('meta[name="_csrf"]');
    var headerMeta = document.querySelector('meta[name="_csrf_header"]');
    document.addEventListener('htmx:configRequest', function (e) {
        if (tokenMeta && headerMeta) {
            e.detail.headers[headerMeta.content] = tokenMeta.content;
        }
    });

    // ---- Toasts ----
    var toastBox = document.getElementById('toasts');
    var ICONS = { success: 'i-check-circle', error: 'i-alert' };

    function toast(detail) {
        if (!toastBox || !detail || !detail.message) return;
        var type = detail.type === 'error' ? 'error' : 'success';
        var el = document.createElement('div');
        el.className = 'toast toast-' + type;
        el.setAttribute('role', type === 'error' ? 'alert' : 'status');

        var icon = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
        icon.setAttribute('class', 'icon');
        var use = document.createElementNS('http://www.w3.org/2000/svg', 'use');
        use.setAttribute('href', '/img/icons.svg#' + ICONS[type]);
        icon.appendChild(use);

        var msg = document.createElement('div');
        msg.className = 'toast-msg';
        msg.textContent = detail.message; // textContent: server text is never parsed as HTML
        if (detail.link) {
            var a = document.createElement('a');
            a.href = detail.link;
            a.textContent = detail.linkText || 'Open';
            // Printable pages open in a new tab; in-app links stay in this one.
            if (/\/(receipt|print)$/.test(detail.link)) { a.target = '_blank'; a.rel = 'noopener'; }
            msg.appendChild(a);
        }

        var close = document.createElement('button');
        close.type = 'button';
        close.className = 'toast-close';
        close.setAttribute('aria-label', 'Dismiss');
        close.textContent = '×';
        close.addEventListener('click', function () { dismiss(el); });

        el.append(icon, msg, close);
        toastBox.appendChild(el);
        setTimeout(function () { dismiss(el); }, type === 'error' ? 8000 : (detail.link ? 9000 : 4500));
    }

    function dismiss(el) {
        if (!el.isConnected) return;
        el.classList.add('leaving');
        setTimeout(function () { el.remove(); }, 200);
    }

    document.addEventListener('toast', function (e) { toast(e.detail); });

    // A message carried over a full-page redirect (e.g. "Welcome back")
    if (document.body.dataset.toast) {
        toast({ message: document.body.dataset.toast, type: 'success' });
    }

    // Network / server problems the server couldn't turn into a toast itself
    document.addEventListener('htmx:responseError', function (e) {
        var status = e.detail.xhr.status;
        toast({
            type: 'error',
            message: status === 403
                ? 'Your session may have ended - reload the page and log in again.'
                : 'Something went wrong (' + status + '). Please try again.'
        });
    });
    document.addEventListener('htmx:sendError', function () {
        toast({ type: 'error', message: "Can't reach the server - check the connection and try again." });
    });

    // ---- Drawer ----
    var drawer = document.getElementById('drawer');
    var drawerBody = document.getElementById('drawer-body');
    var lastFocus = null;

    function openDrawer() {
        if (!drawer) return;
        if (!drawer.classList.contains('open')) lastFocus = document.activeElement;
        drawer.classList.add('open');
        drawer.setAttribute('aria-hidden', 'false');
        var focusTarget = drawerBody.querySelector('[data-autofocus]') || drawerBody.querySelector('input, select, textarea, button');
        if (focusTarget) setTimeout(function () { focusTarget.focus(); }, 60);
    }

    function closeDrawer() {
        if (!drawer || !drawer.classList.contains('open')) return;
        drawer.classList.remove('open');
        drawer.setAttribute('aria-hidden', 'true');
        setTimeout(function () { if (!drawer.classList.contains('open')) drawerBody.innerHTML = ''; }, 250);
        if (lastFocus && lastFocus.isConnected) lastFocus.focus();
    }

    document.addEventListener('htmx:afterSwap', function (e) {
        // Only when new content was loaded INTO the drawer. (Forms inside the drawer also fire
        // afterSwap even when the server says "swap nothing", and must not re-open it after closeDrawer.)
        if (drawerBody && e.detail.target === drawerBody) {
            openDrawer();
        }
    });
    document.addEventListener('closeDrawer', closeDrawer);
    document.addEventListener('click', function (e) {
        if (e.target.closest('[data-drawer-close]')) closeDrawer();
    });
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            if (drawer && drawer.classList.contains('open')) closeDrawer();
            document.body.classList.remove('sidebar-open');
        }
    });

    // ---- Clickable table rows ----
    // data-drawer-url: open in the drawer; data-href: go to that page. Buttons/links inside win.
    document.addEventListener('click', function (e) {
        var row = e.target.closest('tr[data-drawer-url], tr[data-href]');
        if (!row || e.target.closest('button, a, input, select, label')) return;
        if (row.dataset.drawerUrl) {
            htmx.ajax('GET', row.dataset.drawerUrl, { target: '#drawer-body' });
        } else if (row.dataset.href) {
            window.location.href = row.dataset.href;
        }
    });

    // ---- Mobile sidebar ----
    document.addEventListener('click', function (e) {
        if (e.target.closest('[data-sidebar-toggle]')) document.body.classList.toggle('sidebar-open');
        else if (e.target.closest('[data-sidebar-close]')) document.body.classList.remove('sidebar-open');
    });

    // ---- Form helpers ----
    // Check-out can't be on/before check-in: keep the min date in step.
    document.addEventListener('change', function (e) {
        var checkIn = e.target.closest('[data-checkin]');
        if (checkIn && checkIn.form) {
            var out = checkIn.form.querySelector('[data-checkout]');
            if (out && checkIn.value) {
                var next = new Date(checkIn.value + 'T00:00:00');
                next.setDate(next.getDate() + 1);
                // Local date parts, not toISOString(): that converts to UTC and would be a day early in Sri Lanka.
                var min = next.getFullYear() + '-' + String(next.getMonth() + 1).padStart(2, '0') + '-' + String(next.getDate()).padStart(2, '0');
                out.min = min;
                if (!out.value || out.value < min) out.value = min;
            }
        }
        // Photo picker uploads as soon as a file is chosen
        var file = e.target.closest('input[type=file][data-autosubmit]');
        if (file && file.files.length) htmx.trigger(file.form, 'submit');
    });

    // Upload progress bar for photo uploads
    document.addEventListener('htmx:xhr:progress', function (e) {
        var bar = e.target.querySelector && e.target.querySelector('.upload-progress');
        if (bar && e.detail.lengthComputable) {
            bar.hidden = false;
            bar.value = Math.round(e.detail.loaded / e.detail.total * 100);
        }
    });
})();
