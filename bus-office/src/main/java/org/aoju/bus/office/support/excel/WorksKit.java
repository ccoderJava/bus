/*********************************************************************************
 *                                                                               *
 * The MIT License (MIT)                                                         *
 *                                                                               *
 * Copyright (c) 2015-2021 aoju.org and other contributors.                      *
 *                                                                               *
 * Permission is hereby granted, free of charge, to any person obtaining a copy  *
 * of this software and associated documentation files (the "Software"), to deal *
 * in the Software without restriction, including without limitation the rights  *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     *
 * copies of the Software, and to permit persons to whom the Software is         *
 * furnished to do so, subject to the following conditions:                      *
 *                                                                               *
 * The above copyright notice and this permission notice shall be included in    *
 * all copies or substantial portions of the Software.                           *
 *                                                                               *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, *
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN     *
 * THE SOFTWARE.                                                                 *
 *                                                                               *
 ********************************************************************************/
package org.aoju.bus.office.support.excel;

import org.aoju.bus.core.lang.exception.InstrumentException;
import org.aoju.bus.core.toolkit.FileKit;
import org.aoju.bus.core.toolkit.IoKit;
import org.aoju.bus.core.toolkit.StringKit;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Excel工作簿{@link Workbook}相关工具类
 *
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
public class WorksKit {

    /**
     * 创建或加载工作簿
     *
     * @param excelFilePath Excel文件路径，绝对路径或相对于ClassPath路径
     * @return {@link Workbook}
     */
    public static Workbook createBook(String excelFilePath) {
        return createBook(FileKit.file(excelFilePath), null);
    }

    /**
     * 创建或加载工作簿
     *
     * @param excelFile Excel文件
     * @return {@link Workbook}
     */
    public static Workbook createBook(File excelFile) {
        return createBook(excelFile, null);
    }

    /**
     * 创建工作簿，用于Excel写出
     *
     * <pre>
     * 1. excelFile为null时直接返回一个空的工作簿，默认xlsx格式
     * 2. 文件已存在则通过流的方式读取到这个工作簿
     * 3. 文件不存在则检查传入文件路径是否以xlsx为扩展名，是则创建xlsx工作簿，否则创建xls工作簿
     * </pre>
     *
     * @param excelFile Excel文件
     * @return {@link Workbook}
     */
    public static Workbook createBookForWriter(File excelFile) {
        if (null == excelFile) {
            return createBook(true);
        }

        if (excelFile.exists()) {
            return createBook(FileKit.getInputStream(excelFile));
        }

        return createBook(StringKit.endWithIgnoreCase(excelFile.getName(), ".xlsx"));
    }

    /**
     * 创建或加载工作簿，只读模式
     *
     * @param excelFile Excel文件
     * @param password  Excel工作簿密码，如果无密码传{@code null}
     * @return {@link Workbook}
     */
    public static Workbook createBook(File excelFile, String password) {
        try {
            return WorkbookFactory.create(excelFile, password);
        } catch (Exception e) {
            throw new InstrumentException(e);
        }
    }

    /**
     * 创建或加载工作簿
     *
     * @param in Excel输入流
     * @return {@link Workbook}
     */
    public static Workbook createBook(InputStream in) {
        return createBook(in, null);
    }

    /**
     * 创建或加载工作簿
     *
     * @param in       Excel输入流，使用完毕自动关闭流
     * @param password 密码
     * @return {@link Workbook}
     */
    public static Workbook createBook(InputStream in, String password) {
        try {
            return WorkbookFactory.create(IoKit.toMarkSupportStream(in), password);
        } catch (Exception e) {
            throw new InstrumentException(e);
        } finally {
            IoKit.close(in);
        }
    }

    /**
     * 根据文件类型创建新的工作簿，文件路径
     *
     * @param isXlsx 是否为xlsx格式的Excel
     * @return {@link Workbook}
     */
    public static Workbook createBook(boolean isXlsx) {
        Workbook workbook;
        if (isXlsx) {
            workbook = new XSSFWorkbook();
        } else {
            workbook = new org.apache.poi.hssf.usermodel.HSSFWorkbook();
        }
        return workbook;
    }

    /**
     * 创建或加载SXSSFWorkbook工作簿
     *
     * @param excelFilePath Excel文件路径，绝对路径或相对于ClassPath路径
     * @return {@link SXSSFWorkbook}
     */
    public static SXSSFWorkbook createSXSSFBook(String excelFilePath) {
        return createSXSSFBook(FileKit.file(excelFilePath), null);
    }

    /**
     * 创建或加载SXSSFWorkbook工作簿
     *
     * @param excelFile Excel文件
     * @return {@link SXSSFWorkbook}
     */
    public static SXSSFWorkbook createSXSSFBook(File excelFile) {
        return createSXSSFBook(excelFile, null);
    }

    /**
     * 创建或加载SXSSFWorkbook工作簿，只读模式
     *
     * @param excelFile Excel文件
     * @param password  Excel工作簿密码，如果无密码传{@code null}
     * @return {@link SXSSFWorkbook}
     */
    public static SXSSFWorkbook createSXSSFBook(File excelFile, String password) {
        return toSXSSFBook(createBook(excelFile, password));
    }

    /**
     * 创建或加载SXSSFWorkbook工作簿
     *
     * @param in Excel输入流
     * @return {@link SXSSFWorkbook}
     */
    public static SXSSFWorkbook createSXSSFBook(InputStream in) {
        return createSXSSFBook(in, null);
    }

    /**
     * 创建或加载SXSSFWorkbook工作簿
     *
     * @param in       Excel输入流
     * @param password 密码
     * @return {@link SXSSFWorkbook}
     */
    public static SXSSFWorkbook createSXSSFBook(InputStream in, String password) {
        return toSXSSFBook(createBook(in, password));
    }

    /**
     * 创建SXSSFWorkbook，用于大批量数据写出
     *
     * @return {@link SXSSFWorkbook}
     */
    public static SXSSFWorkbook createSXSSFBook() {
        return new SXSSFWorkbook();
    }

    /**
     * 创建SXSSFWorkbook，用于大批量数据写出
     *
     * @param rowAccessWindowSize 在内存中的行数
     * @return {@link Workbook}
     */
    public static SXSSFWorkbook createSXSSFBook(int rowAccessWindowSize) {
        return new SXSSFWorkbook(rowAccessWindowSize);
    }

    /**
     * 将Excel Workbook刷出到输出流，不关闭流
     *
     * @param book {@link Workbook}
     * @param out  输出流
     * @throws InstrumentException IO异常
     */
    public static void writeBook(Workbook book, OutputStream out) throws InstrumentException {
        try {
            book.write(out);
        } catch (IOException e) {
            throw new InstrumentException(e);
        }
    }

    /**
     * 获取或者创建sheet表
     * 如果sheet表在Workbook中已经存在，则获取之，否则创建之
     *
     * @param book      工作簿{@link Workbook}
     * @param sheetName 工作表名
     * @return 工作表{@link Sheet}
     */
    public static Sheet getOrCreateSheet(Workbook book, String sheetName) {
        if (null == book) {
            return null;
        }
        sheetName = StringKit.isBlank(sheetName) ? "sheet1" : sheetName;
        Sheet sheet = book.getSheet(sheetName);
        if (null == sheet) {
            sheet = book.createSheet(sheetName);
        }
        return sheet;
    }

    /**
     * 获取或者创建sheet表
     * 自定义需要读取或写出的Sheet，如果给定的sheet不存在，创建之（命名为默认）
     * 在读取中，此方法用于切换读取的sheet，在写出时，此方法用于新建或者切换sheet
     *
     * @param book       工作簿{@link Workbook}
     * @param sheetIndex 工作表序号
     * @return 工作表{@link Sheet}
     */
    public static Sheet getOrCreateSheet(Workbook book, int sheetIndex) {
        Sheet sheet = null;
        try {
            sheet = book.getSheetAt(sheetIndex);
        } catch (IllegalArgumentException ignore) {
            // ignore
        }
        if (null == sheet) {
            sheet = book.createSheet();
        }
        return sheet;
    }

    /**
     *
     * sheet是否为空
     *
     * @param sheet {@link Sheet}
     * @return sheet是否为空
     */
    public static boolean isEmpty(Sheet sheet) {
        return null == sheet || (sheet.getLastRowNum() == 0 && sheet.getPhysicalNumberOfRows() == 0);
    }

    /**
     * 将普通工作簿转换为SXSSFWorkbook
     *
     * @param book 工作簿
     * @return SXSSFWorkbook

     */
    private static SXSSFWorkbook toSXSSFBook(Workbook book) {
        if (book instanceof SXSSFWorkbook) {
            return (SXSSFWorkbook) book;
        }
        if (book instanceof XSSFWorkbook) {
            return new SXSSFWorkbook((XSSFWorkbook) book);
        }
        throw new InstrumentException("The input is not a [xlsx] format.");
    }

}
