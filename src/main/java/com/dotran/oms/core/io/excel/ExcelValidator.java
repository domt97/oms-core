package com.dotran.oms.core.io.excel;

import com.dotran.oms.core.exception.ImportValidationException;
import com.dotran.oms.core.util.POIUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExcelValidator {

    public void validateHeader(Sheet sheet, List<String> expected) {
        Row header = sheet.getRow(0);

        if (header == null) {
            throw new ImportValidationException("Missing header in sheet: " + sheet.getSheetName());
        }

        for (int i = 0; i < expected.size(); i++) {
            String actual = POIUtils.getString(header, i);

            if (!expected.get(i).equals(actual)) {
                throw new ImportValidationException("Invalid header in sheet " +
                        sheet.getSheetName() +
                        ", column " +
                        (i + 1) +
                        ". Expected '" +
                        expected.get(i) +
                        "' but got '" +
                        actual +
                        "'"
                );
            }
        }
    }
}
