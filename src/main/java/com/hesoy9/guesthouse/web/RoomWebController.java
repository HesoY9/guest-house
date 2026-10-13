package com.hesoy9.guesthouse.web;

import com.hesoy9.guesthouse.dto.RoomForm;
import com.hesoy9.guesthouse.entity.Amenity;
import com.hesoy9.guesthouse.entity.Room;
import com.hesoy9.guesthouse.entity.RoomStatus;
import com.hesoy9.guesthouse.service.PhotoStorageService;
import com.hesoy9.guesthouse.service.RoomService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/web/rooms")
public class RoomWebController {

    private static final String FORM_DRAWER = "staff/drawers/room-form :: drawer";
    private static final String PHOTOS_FRAGMENT = "staff/drawers/room-form :: photos";
    static final List<String> ROOM_TYPES = List.of("Single", "Double", "Twin", "Triple", "Family", "Deluxe", "Suite");

    private final RoomService roomService;
    private final PhotoStorageService photoStorageService;

    public RoomWebController(RoomService roomService, PhotoStorageService photoStorageService) {
        this.roomService = roomService;
        this.photoStorageService = photoStorageService;
    }

    @GetMapping
    public String listRooms(Model model) {
        model.addAttribute("rooms", roomService.getAllRooms());
        return "staff/rooms";
    }

    @GetMapping("/new")
    public String newRoomForm(Model model) {
        RoomForm form = new RoomForm();
        form.setCapacity(2);
        addFormData(model, form, null);
        return FORM_DRAWER;
    }

    @PostMapping
    public Object addRoom(@Valid @ModelAttribute("form") RoomForm form, BindingResult bindingResult,
                          Model model, HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            return showFormError(model, form, null, ValidationUtil.firstErrorMessage(bindingResult), response);
        }
        try {
            Room room = roomService.createRoom(form); // FR1
            // Keep the panel open on the new room so photos can be added straight away.
            Hx.toastAndRefresh(response, "Room " + room.getRoomNumber() + " added - now add some photos.", null, null);
            addFormData(model, RoomForm.from(room), room);
            return FORM_DRAWER;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return showFormError(model, form, null, ex.getMessage(), response);
        }
    }

    @GetMapping("/{id}/edit")
    public String editRoomForm(@PathVariable Long id, Model model) {
        Room room = roomService.getRoomById(id);
        addFormData(model, RoomForm.from(room), room);
        return FORM_DRAWER;
    }

    @PostMapping("/{id}")
    public Object updateRoom(@PathVariable Long id, @Valid @ModelAttribute("form") RoomForm form,
                             BindingResult bindingResult, Model model, HttpServletResponse response) {
        if (bindingResult.hasErrors()) {
            return showFormError(model, form, roomService.getRoomById(id), ValidationUtil.firstErrorMessage(bindingResult), response);
        }
        try {
            Room room = roomService.updateRoom(id, form); // FR2
            return Hx.done("Room " + room.getRoomNumber() + " saved.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return showFormError(model, form, roomService.getRoomById(id), ex.getMessage(), response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        Room room = roomService.getRoomById(id);
        roomService.removeRoom(id); // FR3 - refuses if the room has booking history
        return Hx.done("Room " + room.getRoomNumber() + " removed.");
    }

    // ---- photos (inside the edit panel) ----

    // Either an uploaded file or a pasted image URL.
    @PostMapping("/{id}/photos")
    public String addPhoto(@PathVariable Long id,
                           @RequestParam(required = false) MultipartFile file,
                           @RequestParam(required = false) String url,
                           Model model, HttpServletResponse response) {
        if (file != null && !file.isEmpty()) {
            roomService.addPhotoUrl(id, photoStorageService.storeRoomPhoto(id, file));
        } else if (url != null && !url.isBlank()) {
            if (!url.trim().startsWith("https://") && !url.trim().startsWith("http://")) {
                throw new IllegalArgumentException("Photo links must start with https://");
            }
            roomService.addPhotoUrl(id, url);
        } else {
            throw new IllegalArgumentException("Choose a photo file or paste an image link");
        }
        Hx.toastAndRefresh(response, "Photo added.", null, null);
        model.addAttribute("room", roomService.getRoomById(id));
        return PHOTOS_FRAGMENT;
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    public String removePhoto(@PathVariable Long id, @PathVariable Long photoId,
                              Model model, HttpServletResponse response) {
        roomService.removePhoto(id, photoId);
        Hx.toastAndRefresh(response, "Photo removed.", null, null);
        model.addAttribute("room", roomService.getRoomById(id));
        return PHOTOS_FRAGMENT;
    }

    @PostMapping("/{id}/photos/{photoId}/cover")
    public String makeCover(@PathVariable Long id, @PathVariable Long photoId,
                            Model model, HttpServletResponse response) {
        roomService.makeCover(id, photoId);
        Hx.toastAndRefresh(response, "Cover photo updated.", null, null);
        model.addAttribute("room", roomService.getRoomById(id));
        return PHOTOS_FRAGMENT;
    }

    private String showFormError(Model model, RoomForm form, Room room, String message, HttpServletResponse response) {
        model.addAttribute("errorMessage", message);
        Hx.toast(response, "error", message);
        addFormData(model, form, room);
        return FORM_DRAWER;
    }

    private void addFormData(Model model, RoomForm form, Room room) {
        model.addAttribute("form", form);
        model.addAttribute("room", room); // null when adding a new room
        model.addAttribute("roomTypes", ROOM_TYPES);
        model.addAttribute("statuses", RoomStatus.values());
        model.addAttribute("amenities", Amenity.values());
    }
}
