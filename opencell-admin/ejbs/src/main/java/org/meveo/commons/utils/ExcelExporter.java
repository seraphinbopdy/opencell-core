/*
 * (C) Copyright 2015-2020 Opencell SAS (https://opencellsoft.com/) and contributors.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN
 * OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS
 * IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO
 * THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE,
 * YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
 *
 * For more information on the GNU Affero General Public License, please consult
 * <https://www.gnu.org/licenses/agpl-3.0.en.html>.
 */
package org.meveo.commons.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ConditionalFormatting;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;

/**
 * @author Hicham EL YOUSSOUFI
 * @lastModifiedVersion 5.1.1
 **/
public final class ExcelExporter {

	/**
	 * Private Default Construct.
	 */
	private ExcelExporter() {
		super();
	}

	/**
	 * This function create and fill an Excel file (output) using the sheetData list
	 * and the excel template.
	 * 
	 * @param sheetDataList List of structured Data
	 * @param input         xls template
	 * @param output        xls output (final result report)
	 */
	public static void exportToExcel(List<SheetData> sheetDataList, File input, File output) {

		try (InputStream inputStream = FileUtils.getInputStream(input);
				Workbook workbook = WorkbookFactory.create(inputStream)) {

			if (sheetDataList != null && !sheetDataList.isEmpty()) {

				for (SheetData sheetData : sheetDataList) {

					Sheet sheet = workbook.getSheetAt(sheetData.getIndex());
					sheet.setForceFormulaRecalculation(sheetData.isForceFormulaRecalculation());

					Map<String, ConditionalFormatting> cfMap = getConditionalFormattingMap(sheet);

					// Dynamic data table feature
					// ====================================================================

					int sheetDataRowFrom = sheetData.getRowFrom();
					int sheetDataRowsNumber = sheetData.getNumberOfRows();

					if (sheetDataRowFrom > 0) {

						if (sheetDataRowsNumber == 1) {

							removeRow(sheet, sheetDataRowFrom);

						} else if (sheetDataRowsNumber == 0) {

							removeRow(sheet, sheetDataRowFrom);
							removeRow(sheet, sheetDataRowFrom - 1);

						} else {

							int oddRowsNumber = 0;
							if (sheetDataRowsNumber % 2 != 0) {
								oddRowsNumber = 1;
							}

							int halfSize = (sheetDataRowsNumber / 2) + oddRowsNumber - 1;

							for (int i = 0; i < halfSize; i++) {

								CopyRow.copyRow(sheet, (sheetDataRowFrom - 1), ((sheetDataRowFrom + 1) + 2 * i), cfMap);

								if (sheetDataRowsNumber % 2 == 0 || i < halfSize - 1) {
									CopyRow.copyRow(sheet, sheetDataRowFrom, ((sheetDataRowFrom + 2) + 2 * i), cfMap);
								}
							}
						}
					}
					// =========================================================

					for (String position : sheetData.getDatas().keySet()) {

						CellRangeAddress address = CellRangeAddress.valueOf(position);

						int rowStart = address.getFirstRow();
						int columnStart = address.getFirstColumn();

						if (sheetDataRowFrom > 0 && rowStart > sheetDataRowFrom) {
							rowStart = rowStart - 2 + sheetDataRowsNumber;
						}

						Object[][] datas = sheetData.getDatas().get(position);

						int rowIndex = rowStart;

						for (Object[] row : datas) {

							int columnIndex = columnStart;

							for (Object field : row) {

								Row currentRow = sheet.getRow(rowIndex);

								if (currentRow == null) {
									currentRow = sheet.createRow(rowIndex);
								}

								Cell cell = currentRow.getCell(columnIndex);

								if (cell == null) {
									cell = currentRow.createCell(columnIndex);
								}

								if (field instanceof Date) {
									cell.setCellValue((Date) field);
								} else if (field instanceof Boolean) {
									cell.setCellValue((Boolean) field);
								} else if (field instanceof String) {
									cell.setCellValue((String) field);
								} else if (field instanceof Double) {
									cell.setCellValue((Double) field);
								} else if (field instanceof Integer) {
									cell.setCellValue((Integer) field);
								}

								columnIndex++;
							}
							rowIndex++;
						}
					}
				}
				// Save to file
				try (OutputStream outputStream = FileUtils.getOutputStream(output)){
					workbook.write(outputStream);
				} catch (Exception e) {
					throw new RuntimeException("Unable to export to Excel", e);
				}
			}

		} catch (IOException e) {
			throw new RuntimeException("Unable to create file", e);
		}

	}

	/**
	 * export data To Excel.
	 * 
	 * @param sheetDataList List of Excel Sheet Data
	 * @param template      input template file name
	 * @param output        target output file
	 */
	public static void exportToExcel(List<SheetData> sheetDataList, String template, File output) {

		File input;
		try {
			input = getFile(template);
		} catch (IOException e) {
			throw new RuntimeException("Unable to open template file : ", e);
		}
		exportToExcel(sheetDataList, input, output);
	}

	/**
	 * Single Value To Matrix.
	 * 
	 * @param value input
	 * @return Object[][]
	 */
	public static Object[][] singleValueToMatrix(Object value) {
		return new Object[][] { new Object[] { value } };
	}

	/**
	 * transform array To Column.
	 * 
	 * @param values column values
	 * @return Column Object[][]
	 */
	public static Object[][] arrayToColumn(Object[] values) {

		Object[][] datas = new Object[values.length][1];

		int i = 0;
		for (Object value : values) {
			datas[i++] = new Object[] { value };
		}
		return datas;
	}

	/**
	 * Transform Object[] array to Row.
	 * 
	 * @param values values to be transformed
	 * @return Row result
	 */
	public static Object[][] arrayToRow(Object[] values) {
		return new Object[][] { values };
	}

	/**
	 * Remove Row from Sheet.
	 * 
	 * @param sheet    target sheet
	 * @param rowIndex row Index
	 */
	private static void removeRow(Sheet sheet, int rowIndex) {
		int lastRowNum = sheet.getLastRowNum();
		if (rowIndex >= 0 && rowIndex < lastRowNum) {
			sheet.shiftRows(rowIndex + 1, lastRowNum, -1);
		}
		if (rowIndex == lastRowNum) {
			Row removingRow = sheet.getRow(rowIndex);
			if (removingRow != null) {
				sheet.removeRow(removingRow);
			}
		}
	}

	/**
	 * Gets File by resource name.
	 * 
	 * @param resource resource name
	 * @return target file
	 * @throws IOException IO Exception may be throwed
	 */
	private static File getFile(String resource) throws IOException {
		ClassLoader cl = ExcelExporter.class.getClassLoader();
		InputStream cpResource = cl.getResourceAsStream(resource);
		File tmpFile = FileUtils.createTempFile("file", "temp");
		FileUtils.copyInputStreamToFile(cpResource, tmpFile);
		tmpFile.deleteOnExit();
		return tmpFile;
	}

	/**
	 * Gets Conditional Formatting Map.
	 * 
	 * @param sheet target sheet
	 * @return target Conditional Formatting Map
	 */
	private static Map<String, ConditionalFormatting> getConditionalFormattingMap(Sheet sheet) {

		int numConditionalFormattings = sheet.getSheetConditionalFormatting().getNumConditionalFormattings();

		Map<String, ConditionalFormatting> cfMap = new HashMap<>();

		for (int i = 0; i < numConditionalFormattings; i++) {

			ConditionalFormatting cf = sheet.getSheetConditionalFormatting().getConditionalFormattingAt(i);
			for (CellRangeAddress cellRangeAddress : cf.getFormattingRanges()) {
				cfMap.put(cellRangeAddress.formatAsString(), cf);
			}
		}
		return cfMap;
	}
}
