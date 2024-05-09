package org.meveo.apiv2.generic.common;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.xssf.streaming.SXSSFSheet;

import java.util.function.BiFunction;
import java.util.function.Function;

public class ExcelExportConfiguration {
    
    private Function<SXSSFSheet, Integer> header;
    
    private BiFunction<SXSSFSheet, Integer, Integer> footer;
    
    private BorderStyle borderStyle;

    public Function<SXSSFSheet, Integer> getHeader() {
        return header;
    }

    public ExcelExportConfiguration setHeader(Function<SXSSFSheet, Integer> header) {
        this.header = header;
        return this;
    }

    public BiFunction<SXSSFSheet, Integer, Integer> getFooter() {
        return footer;
    }

    public ExcelExportConfiguration setFooter(BiFunction<SXSSFSheet, Integer, Integer> footer) {
        this.footer = footer;
        return this;
    }

    public BorderStyle getBorderStyle() {
        return borderStyle;
    }

    public ExcelExportConfiguration setBorderStyle(BorderStyle borderStyle) {
        this.borderStyle = borderStyle;
        return this;
    }
}
