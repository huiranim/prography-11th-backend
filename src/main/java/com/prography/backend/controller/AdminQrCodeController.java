package com.prography.backend.controller;

import com.prography.backend.common.ApiResponse;
import com.prography.backend.dto.response.QrCodeResponse;
import com.prography.backend.service.QrCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/qrcodes")
@RequiredArgsConstructor
public class AdminQrCodeController {

    private final QrCodeService qrCodeService;

    @PutMapping("/{qrCodeId}")
    public ApiResponse<QrCodeResponse> renewQrCode(@PathVariable Long qrCodeId) {
        return ApiResponse.ok(qrCodeService.renewQrCode(qrCodeId));
    }
}
