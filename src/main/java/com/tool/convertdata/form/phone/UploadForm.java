package com.tool.convertdata.form.phone;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UploadForm {
    private MultipartFile file;
}
