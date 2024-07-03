package com.kryeit.servus.demo;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff")
@PreAuthorize("hasRole('STAFF')")
public class AdminController {

    @GetMapping
    @PreAuthorize("hasAuthority('staff:read')")
    public String get() {
        return "GET:: staff controller";
    }
    @PostMapping
    @PreAuthorize("hasAuthority('staff:create')")
    @Hidden
    public String post() {
        return "POST:: staff controller";
    }
    @PutMapping
    @PreAuthorize("hasAuthority('staff:update')")
    @Hidden
    public String put() {
        return "PUT:: staff controller";
    }
    @DeleteMapping
    @PreAuthorize("hasAuthority('staff:delete')")
    @Hidden
    public String delete() {
        return "DELETE:: staff controller";
    }
}
