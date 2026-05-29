package com.yunus.business.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 7 günü tek seferde set etmek için kullanılan toplu istek.
 * @Valid ile iç listedeki her WorkingHourRequest da validate edilir.
 */
public record BulkWorkingHourRequest(

        @NotEmpty(message = "Çalışma saati listesi boş olamaz")
        @Valid
        List<WorkingHourRequest> hours
) {
}
