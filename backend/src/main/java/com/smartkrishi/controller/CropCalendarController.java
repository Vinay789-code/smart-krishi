package com.smartkrishi.controller;

import com.smartkrishi.dto.ApiResponse;
import com.smartkrishi.dto.CropCalendarDto;
import com.smartkrishi.service.CropCalendarService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/crop-calendar")
public class CropCalendarController {

    private final CropCalendarService cropCalendarService;

    public CropCalendarController(CropCalendarService cropCalendarService) {
        this.cropCalendarService = cropCalendarService;
    }

    /**
     * Returns list of supported crops for calendar generation.
     * GET /api/crop-calendar
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<String>>> getSupportedCrops() {
        List<String> crops = cropCalendarService.getSupportedCrops();
        return ResponseEntity.ok(ApiResponse.ok("Supported crops list", crops));
    }

    /**
     * Returns structured stage-by-stage farming calendar & advisory for a specific crop.
     * GET /api/crop-calendar/{crop}?state={state}
     */
    @GetMapping("/{crop}")
    public ResponseEntity<ApiResponse<CropCalendarDto>> getCropCalendar(
            @PathVariable String crop,
            @RequestParam(required = false) String state) {

        CropCalendarDto calendar = cropCalendarService.getCropCalendar(crop, state);
        return ResponseEntity.ok(ApiResponse.ok("Crop calendar retrieved successfully", calendar));
    }
}
