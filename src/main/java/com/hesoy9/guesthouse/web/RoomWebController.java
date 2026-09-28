package com.hesoy9.guesthouse.web;

import com.hesoy9.guesthouse.entity.Room;
import com.hesoy9.guesthouse.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/web/rooms")
public class RoomWebController {

    private final RoomService roomService;

    public RoomWebController(RoomService roomService) {
        this.roomService = roomService;
    }

    // Full page: GET /web/rooms
    @GetMapping
    public String listRooms(Model model) {
        model.addAttribute("rooms", roomService.getAllRooms());
        return "rooms";
    }

    // HTMX: POST /web/rooms -> re-renders the whole panel with the fresh list
    @PostMapping
    public String addRoom(@Valid @ModelAttribute Room room, BindingResult bindingResult, Model model) {
        model.addAttribute("rooms", roomService.getAllRooms());
        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", ValidationUtil.firstErrorMessage(bindingResult));
            return "fragments/rooms-panel :: panel";
        }
        roomService.addRoom(room);
        model.addAttribute("rooms", roomService.getAllRooms()); // refresh - includes the new one
        model.addAttribute("successMessage", "Room added.");
        return "fragments/rooms-panel :: panel";
    }

    // Classic full-page navigation (not HTMX) - editing gets its own simple page
    @GetMapping("/{id}/edit")
    public String editRoomForm(@PathVariable Long id, Model model) {
        model.addAttribute("room", roomService.getRoomById(id));
        return "room-edit";
    }

    // Plain HTML forms can only send GET/POST, so this is a POST, not PUT
    @PostMapping("/{id}/edit")
    public String updateRoom(@PathVariable Long id, @ModelAttribute Room room) {
        roomService.updateRoom(id, room);
        return "redirect:/web/rooms";
    }

    // HTMX: DELETE /web/rooms/5 -> re-renders the whole panel without that room
    @DeleteMapping("/{id}")
    public String deleteRoom(@PathVariable Long id, Model model) {
        roomService.removeRoom(id);
        model.addAttribute("rooms", roomService.getAllRooms());
        model.addAttribute("successMessage", "Room removed.");
        return "fragments/rooms-panel :: panel";
    }
}
