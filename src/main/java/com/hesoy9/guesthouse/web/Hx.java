package com.hesoy9.guesthouse.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.Map;

// Small helpers for talking to HTMX from the staff controllers.
//
// The staff pages follow one pattern:
//   - a button/form sends an HTMX request
//   - the server answers with an "HX-Trigger" header carrying events for the page:
//       toast       -> staff.js shows a toast message (instead of a banner in the page)
//       dataChanged -> every table/board on the page re-fetches itself
//       closeDrawer -> the slide-over panel closes
//   - "HX-Reswap: none" means "don't put this response body anywhere".
public final class Hx {

    private Hx() {
        // utility class - not meant to be instantiated
    }

    public static boolean isHtmx(HttpServletRequest request) {
        return "true".equals(request.getHeader("HX-Request"));
    }

    // Action succeeded: toast + refresh lists + close the panel. Nothing is swapped.
    public static ResponseEntity<Void> done(String message) {
        return ResponseEntity.ok()
                .header("HX-Reswap", "none")
                .header("HX-Trigger", events(toastEvent("success", message, null, null), true, true))
                .build();
    }

    // Same as done(), plus a link in the toast (e.g. "Print receipt").
    public static ResponseEntity<Void> done(String message, String linkUrl, String linkText) {
        return ResponseEntity.ok()
                .header("HX-Reswap", "none")
                .header("HX-Trigger", events(toastEvent("success", message, linkUrl, linkText), true, true))
                .build();
    }

    // Action refused (business rule): show the reason, change nothing on the page.
    public static ResponseEntity<Void> fail(String message) {
        return ResponseEntity.ok()
                .header("HX-Reswap", "none")
                .header("HX-Trigger", events(toastEvent("error", message, null, null), false, false))
                .build();
    }

    // For handlers that render a fragment (e.g. a form re-shown with an error) and also want a toast.
    public static void toast(HttpServletResponse response, String type, String message) {
        response.setHeader("HX-Trigger", events(toastEvent(type, message, null, null), false, false));
    }

    // Toast + refresh the page's lists, while still swapping the returned fragment.
    public static void toastAndRefresh(HttpServletResponse response, String message, String linkUrl, String linkText) {
        response.setHeader("HX-Trigger", events(toastEvent("success", message, linkUrl, linkText), true, false));
    }

    private static Map<String, String> toastEvent(String type, String message, String linkUrl, String linkText) {
        Map<String, String> toast = new LinkedHashMap<>();
        toast.put("type", type);
        toast.put("message", message);
        if (linkUrl != null) {
            toast.put("link", linkUrl);
            toast.put("linkText", linkText);
        }
        return toast;
    }

    private static String events(Map<String, String> toast, boolean dataChanged, boolean closeDrawer) {
        StringBuilder json = new StringBuilder("{\"toast\":{");
        boolean first = true;
        for (Map.Entry<String, String> e : toast.entrySet()) {
            if (!first) json.append(',');
            json.append(quote(e.getKey())).append(':').append(quote(e.getValue()));
            first = false;
        }
        json.append('}');
        if (dataChanged) json.append(",\"dataChanged\":true");
        if (closeDrawer) json.append(",\"closeDrawer\":true");
        return json.append('}').toString();
    }

    // Minimal JSON string escaping. Non-ASCII is escaped too, because HTTP header values
    // are effectively ASCII-only.
    private static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : (value == null ? "" : value).toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20 || c > 0x7e) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('"').toString();
    }
}
