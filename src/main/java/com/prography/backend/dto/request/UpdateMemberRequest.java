package com.prography.backend.dto.request;

public record UpdateMemberRequest(String name, String phone, Long cohortId, Long partId, Long teamId) {}
