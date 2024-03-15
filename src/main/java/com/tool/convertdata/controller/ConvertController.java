package com.tool.convertdata.controller;

import com.tool.convertdata.constant.DataConstant;
import com.tool.convertdata.exceptions.BadRequestException;
import com.tool.convertdata.exceptions.NotFoundException;
import com.tool.convertdata.form.phone.UploadForm;
import com.tool.convertdata.storage.model.Phone;
import com.tool.convertdata.storage.repositories.PhoneRepository;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.util.*;

@RestController
@RequestMapping("/v1/convert")
public class ConvertController {

    @Autowired
    private PhoneRepository phoneRepository;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> convertData(@Valid UploadForm uploadForm) {
        if (uploadForm.getFile() == null || uploadForm.getFile().isEmpty()) {
            throw new BadRequestException("File is required");
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HttpHeaders httpHeaders = new HttpHeaders();
        try {
            FileInputStream fileDataInput = (FileInputStream) uploadForm.getFile().getInputStream();
            Workbook workbookInput;

            if (Objects.requireNonNull(uploadForm.getFile().getOriginalFilename()).endsWith(DataConstant.XLSX_FORMAT)) {
                workbookInput = new XSSFWorkbook(fileDataInput);
            } else if (uploadForm.getFile().getOriginalFilename().endsWith(DataConstant.XLS_FORMAT)) {
                workbookInput = new HSSFWorkbook(fileDataInput);
            }else {
                throw new BadRequestException("Do not support this file format");
            }

            Sheet sheetDataInput = workbookInput.getSheetAt(workbookInput.getActiveSheetIndex());

            int columnIndexInput = findColumnIndexByName(sheetDataInput); // find column index of phone number column
            if (columnIndexInput == -1) {
                throw new NotFoundException(String.format("Column %s not found", DataConstant.COLUMN_NAME));
            }

            Workbook workbookOutput = new XSSFWorkbook();
            Sheet sheetDataOutput = workbookOutput.createSheet(DataConstant.OUTPUT_SHEET_NAME);
            sheetDataOutput.setColumnWidth(1, 8000);
            sheetDataOutput.setColumnWidth(2, 7000);
            sheetDataOutput.setColumnWidth(3, 6000);
            sheetDataOutput.setColumnWidth(4, 5000);
            sheetDataOutput.setColumnWidth(5, 4000);
            Row headerRow = sheetDataOutput.createRow(0);

            final String[] headers = {"STT", "Họ và tên", "Địa chỉ", "Điện thoại", "Email", "Facebook"};
            for (int i = 0; i < headers.length; i++ ){
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int batchSize = 5000;
            int totalRows = sheetDataInput.getLastRowNum() + 1; //starts from 0 to n-1, n is the number of rows
            int startRow = 0;
            int STT = 1;
            int rowIndexOutput = 1; // Ignore the header at the first line
            Map<String,String> phoneMap = new HashMap<>();

            System.out.println("============================> Start reading the data");
            while (startRow < totalRows) {
                int endRow = Math.min(startRow + batchSize, totalRows);

                // Read phone number
                List<Double> phoneNumberList = new ArrayList<>();
                for (int i = startRow; i < endRow; i++) {
                    Row row = sheetDataInput.getRow(i);
                    if (row != null) {
                        Cell cellDataInput = row.getCell(columnIndexInput);

                        if (cellDataInput != null) {
                            System.out.println("---------------------> Read row: "+i);
                            String phoneNumberString;
                            CellType cellType = cellDataInput.getCellType();
//                            if (cellType.name().equals("STRING")) {
//                                phoneNumberString = cellDataInput.getStringCellValue().replaceAll("[^0-9]", "");
//                                System.out.println("--------------------->{INPUT} Phone as STRING: "+phoneNumberString);
//                            } else if (cellType.name().equals("NUMERIC")) {
//                                double phoneNumber = cellDataInput.getNumericCellValue();
//                                System.out.println("--------------------->{INPUT} Phone as NUMERIC: "+phoneNumber);
//                                phoneNumberString = String.valueOf((long) phoneNumber);
//                            } else {
//                                phoneNumberString = "";
//                            }

                            if (cellType == CellType.STRING) {
                                phoneNumberString = cellDataInput.getStringCellValue().replaceAll("[^0-9]", "");
                                System.out.println("--------------------->{INPUT} Phone as STRING: "+phoneNumberString);
                            } else if (cellType == CellType.NUMERIC) {
                                long phoneNumber = (long) cellDataInput.getNumericCellValue();
                                phoneNumberString = String.valueOf(phoneNumber);
                                System.out.println("--------------------->{INPUT} Phone as NUMERIC: "+phoneNumberString);
                            } else {
                                phoneNumberString = "";
                            }

                            if (!phoneNumberString.isEmpty()) {
                                if (phoneNumberString.startsWith("0")) {
                                    phoneNumberString = DataConstant.PHONE_AREA_CODE + phoneNumberString.substring(1);
                                } else {
                                    if (!phoneNumberString.startsWith(DataConstant.PHONE_AREA_CODE)) {
                                        phoneNumberString = DataConstant.PHONE_AREA_CODE + phoneNumberString;
                                    }
                                }
                                double phoneNumberConverted = Double.parseDouble(phoneNumberString);
                                phoneNumberList.add(phoneNumberConverted);
                            }
                        }
                    }
                }

                List<Phone> phones = phoneRepository.findAllByPhoneIn(phoneNumberList);
                for (Phone phone : phones){
                    String key = String.valueOf((long)phone.getPhone().doubleValue());
                    String value = String.valueOf((long)phone.getId().doubleValue());
                    if (!phoneMap.containsKey(key)){
                        phoneMap.put(key,value);
                    }
                }

                // Export data
                for (int i = startRow; i < endRow; i++) {
                    Row row = sheetDataInput.getRow(i);
                    if (row != null) {
                        Cell cellDataInput = row.getCell(columnIndexInput);
                        if (cellDataInput != null) {

                            CellType cellType = cellDataInput.getCellType();
                            String phoneNumberString;
//                            if (cellType.name().equals("STRING")) {
//                                phoneNumberString = cellDataInput.getStringCellValue().replaceAll("[^0-9]", "");
//                            } else if (cellType.name().equals("NUMERIC")) {
//                                double phoneNumber = cellDataInput.getNumericCellValue();
//                                phoneNumberString = String.valueOf((long) phoneNumber);
//                            } else {
//                                phoneNumberString = "";
//                            }

                            if (cellType == CellType.STRING) {
                                phoneNumberString = cellDataInput.getStringCellValue().replaceAll("[^0-9]", "");
                            } else if (cellType == CellType.NUMERIC) {
                                long phoneNumber = (long) cellDataInput.getNumericCellValue();
                                phoneNumberString = String.valueOf(phoneNumber);
                            } else {
                                phoneNumberString = "";
                            }

                            if (!phoneNumberString.isEmpty()) {
                                if (phoneNumberString.startsWith("0")) {
                                    phoneNumberString = DataConstant.PHONE_AREA_CODE + phoneNumberString.substring(1);
                                } else {
                                    if (!phoneNumberString.startsWith(DataConstant.PHONE_AREA_CODE)) {
                                        phoneNumberString = DataConstant.PHONE_AREA_CODE + phoneNumberString;
                                    }
                                }
                            }
                            String facebookId = phoneMap.getOrDefault(phoneNumberString, "");

                            // Fill data into output file
                            if (!facebookId.isEmpty()) {
                                String absolutePath = DataConstant.FACEBOOK_RELATIVE_PATH + facebookId;
                                Row currentRowOutput = sheetDataOutput.createRow(rowIndexOutput++);

                                // STT column
                                Cell cellSTTOutput = currentRowOutput.createCell(0);
                                cellSTTOutput.setCellValue(STT++);

                                // Name column
                                Cell cellNameInput = row.getCell(1);
                                if (cellNameInput != null) {
                                    Cell cellNameOutput = currentRowOutput.createCell(1);
                                    cellNameOutput.setCellValue(row.getCell(1).getStringCellValue());
                                }

                                // Address column
                                Cell cellAddressInput = row.getCell(2);
                                if (cellAddressInput != null) {
                                    Cell cellAddressOutput = currentRowOutput.createCell(2);
                                    cellAddressOutput.setCellValue(row.getCell(2).getStringCellValue());
                                }

                                // Phone column
                                Cell cellPhoneOutput = currentRowOutput.createCell(3);
                                if (cellType.name().equals("STRING")) {
                                    cellPhoneOutput.setCellValue(cellDataInput.getStringCellValue());
                                }else {
                                    if (cellType.name().equals("NUMERIC")){
                                        cellPhoneOutput.setCellValue(String.valueOf((long) cellDataInput.getNumericCellValue()));
                                    }
                                }

                                // Email column
                                Cell cellEmailInput = row.getCell(4);
                                if (cellEmailInput != null) {
                                    Cell cellEmailOutput = currentRowOutput.createCell(4);
                                    cellEmailOutput.setCellValue(row.getCell(4).getStringCellValue());
                                }

                                // Facebook column
                                Cell cellFacebookOutput = currentRowOutput.createCell(5);
                                cellFacebookOutput.setCellValue(absolutePath);
                            }
                        }
                    }
                }
                startRow = endRow;
            }
            workbookOutput.write(outputStream);
            workbookOutput.close();

            workbookInput.close();
            fileDataInput.close();

        }catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        byte[] fileBytes = outputStream.toByteArray();
        Resource resource = new ByteArrayResource(fileBytes);
        String contentType = "application/octet-stream";
        String fileName = StringUtils.cleanPath(Objects.requireNonNull(uploadForm.getFile().getOriginalFilename()));

        httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
        return ResponseEntity.ok()
                .headers(httpHeaders)
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    private int findColumnIndexByName(Sheet sheet) {
        for (Row currentRow : sheet) {
            for (Cell currentCell : currentRow) {
                if (currentCell.getCellType().name().equals("STRING")) {
                    String currentCellValue = currentCell.getStringCellValue();
                    if (currentCellValue.equals(DataConstant.COLUMN_NAME)) {
                        return currentCell.getColumnIndex();
                    }
                }
            }
        }
        return -1;
    }
}
