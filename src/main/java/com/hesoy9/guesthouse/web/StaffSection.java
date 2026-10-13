package com.hesoy9.guesthouse.web;

import com.hesoy9.guesthouse.entity.Role;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// The single source of truth for per-role access (FR32). Each section of the staff portal
// lists its URL prefixes and the roles allowed in. The sidebar is built from this list
// (people only see menu items they can open) and RoleAccessInterceptor blocks everything else,
// so a hidden menu item can't be reached by typing its URL either.
public enum StaffSection {

    DASHBOARD("Dashboard", "i-dashboard", "/web/dashboard",
            EnumSet.of(Role.ADMIN, Role.MANAGER, Role.RECEPTIONIST), "/web/dashboard"),
    CALENDAR("Room calendar", "i-calendar", "/web/calendar",
            EnumSet.of(Role.ADMIN, Role.MANAGER, Role.RECEPTIONIST), "/web/calendar"),
    RESERVATIONS("Reservations", "i-clipboard", "/web/reservations",
            EnumSet.of(Role.ADMIN, Role.MANAGER, Role.RECEPTIONIST), "/web/reservations"),
    GUESTS("Guests", "i-users", "/web/guests",
            EnumSet.of(Role.ADMIN, Role.MANAGER, Role.RECEPTIONIST), "/web/guests"),
    PAYMENTS("Payments", "i-card", "/web/payments",
            EnumSet.of(Role.ADMIN, Role.MANAGER, Role.RECEPTIONIST), "/web/payments", "/web/invoices"),
    HOUSEKEEPING("Housekeeping", "i-sparkles", "/web/housekeeping",
            EnumSet.of(Role.ADMIN, Role.MANAGER, Role.RECEPTIONIST, Role.HOUSEKEEPING), "/web/housekeeping"),
    ROOMS("Rooms", "i-bed", "/web/rooms",
            EnumSet.of(Role.ADMIN, Role.MANAGER), "/web/rooms"),
    REPORTS("Reports", "i-chart", "/web/reports",
            EnumSet.of(Role.ADMIN, Role.MANAGER), "/web/reports"),
    USERS("Staff accounts", "i-shield", "/web/users",
            EnumSet.of(Role.ADMIN), "/web/users");

    private final String label;
    private final String icon;
    private final String path;
    private final Set<Role> roles;
    private final List<String> prefixes;

    StaffSection(String label, String icon, String path, Set<Role> roles, String... prefixes) {
        this.label = label;
        this.icon = icon;
        this.path = path;
        this.roles = roles;
        this.prefixes = List.of(prefixes);
    }

    public boolean allows(Role role) {
        return role != null && roles.contains(role);
    }

    // Which section a request path belongs to, if any.
    public static Optional<StaffSection> forPath(String path) {
        return Arrays.stream(values())
                .filter(s -> s.prefixes.stream().anyMatch(p -> path.equals(p) || path.startsWith(p + "/")))
                .findFirst();
    }

    public static List<StaffSection> visibleTo(Role role) {
        return Arrays.stream(values()).filter(s -> s.allows(role)).toList();
    }

    // Where a user lands after logging in: the first section their role can open.
    public static String landingPathFor(Role role) {
        return visibleTo(role).stream().findFirst().map(StaffSection::getPath).orElse("/web/login");
    }

    public String getLabel() { return label; }
    public String getIcon() { return icon; }
    public String getPath() { return path; }
    public Set<Role> getRoles() { return roles; }
}
