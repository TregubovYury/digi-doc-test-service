package com.ee.digi_doc.web.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of = "id")
public class FileDto {

    private Long id;

    private String name;

    private String url;

    private String contentType;

    private LocalDateTime uploadedOn;

}
