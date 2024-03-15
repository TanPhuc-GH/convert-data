package com.tool.convertdata.dto;

import lombok.Data;

@Data
public class ApiMessageDto<T> {
    private Boolean result = true;
    private String code = null;
    private T data = null;
    private String message = null;
}
