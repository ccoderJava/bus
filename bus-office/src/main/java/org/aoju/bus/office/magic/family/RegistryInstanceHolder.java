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
package org.aoju.bus.office.magic.family;

import org.aoju.bus.core.toolkit.ObjectKit;

/**
 * 保存默认的{@link FormatRegistry}实例.
 *
 * @author Kimi Liu
 * @version 6.3.2
 * @since JDK 1.8+
 */
public final class RegistryInstanceHolder {

    private static final String json = "[" +
            "  {" +
            "    \"name\": \"Portable Document Format\"," +
            "    \"extensions\": [\"pdf\"]," +
            "    \"mediaType\": \"application/pdf\"," +
            "    \"storeProperties\": {" +
            "      \"DRAWING\": {" +
            "        \"FilterName\": \"draw_pdf_Export\"" +
            "      }," +
            "      \"SPREADSHEET\": {" +
            "        \"FilterName\": \"calc_pdf_Export\"" +
            "      }," +
            "      \"PRESENTATION\": {" +
            "        \"FilterName\": \"impress_pdf_Export\"" +
            "      }," +
            "      \"TEXT\": {" +
            "        \"FilterName\": \"writer_pdf_Export\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"Macromedia Flash\"," +
            "    \"extensions\": [\"swf\"]," +
            "    \"mediaType\": \"application/x-shockwave-flash\"," +
            "    \"storeProperties\": {" +
            "      \"DRAWING\": {" +
            "        \"FilterName\": \"draw_flash_Export\"" +
            "      }," +
            "      \"PRESENTATION\": {" +
            "        \"FilterName\": \"impress_flash_Export\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"Visio\"," +
            "    \"extensions\": [\"vsd\"]," +
            "    \"mediaType\": \"application/vnd-visio\"," +
            "    \"inputFamily\": \"DRAWING\"," +
            "    \"storeProperties\": {" +
            "      \"DRAWING\": {" +
            "        \"FilterName\": \"draw_pdf_Export\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"Visio XML\"," +
            "    \"extensions\": [\"vsdx\"]," +
            "    \"mediaType\": \"application/vnd-ms-visio.drawing\"," +
            "    \"inputFamily\": \"DRAWING\"," +
            "    \"storeProperties\": {" +
            "      \"DRAWING\": {" +
            "        \"FilterName\": \"draw_pdf_Export\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"HTML\"," +
            "    \"extensions\": [\"html\"]," +
            "    \"mediaType\": \"text/html\"," +
            "    \"inputFamily\": \"TEXT\"," +
            "    \"storeProperties\": {" +
            "      \"SPREADSHEET\": {" +
            "        \"FilterName\": \"HTML (StarCalc)\"" +
            "      }," +
            "      \"PRESENTATION\": {" +
            "        \"FilterName\": \"impress_html_Export\"" +
            "      }," +
            "      \"TEXT\": {" +
            "        \"FilterName\": \"HTML (StarWriter)\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"XHTML\"," +
            "    \"extensions\": [\"xhtml\"]," +
            "    \"mediaType\": \"application/xhtml+xml\"," +
            "    \"inputFamily\": \"TEXT\"," +
            "    \"storeProperties\": {" +
            "      \"SPREADSHEET\": {" +
            "        \"FilterName\": \"XHTML Calc File\"" +
            "      }," +
            "      \"PRESENTATION\": {" +
            "        \"FilterName\": \"XHTML Impress File\"" +
            "      }," +
            "      \"TEXT\": {" +
            "        \"FilterName\": \"XHTML Writer File\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"OpenDocument Text\"," +
            "    \"extensions\": [\"odt\"]," +
            "    \"mediaType\": \"application/vnd.oasis.opendocument.text\"," +
            "    \"inputFamily\": \"TEXT\"," +
            "    \"storeProperties\": {" +
            "      \"TEXT\": {" +
            "        \"FilterName\": \"writer8\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"OpenDocument Text Template\"," +
            "    \"extensions\": [\"ott\"]," +
            "    \"mediaType\": \"application/vnd.oasis.opendocument.text-template\"," +
            "    \"inputFamily\": \"TEXT\"," +
            "    \"storeProperties\": {" +
            "      \"TEXT\": {" +
            "        \"FilterName\": \"writer8_template\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"OpenDocument Text Flat XML\"," +
            "    \"extensions\": [\"fodt\"]," +
            "    \"mediaType\": \"application/vnd.oasis.opendocument.text-flat-xml\"," +
            "    \"inputFamily\": \"TEXT\"," +
            "    \"storeProperties\": {" +
            "      \"TEXT\": {" +
            "        \"FilterName\": \"OpenDocument Text Flat XML\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"OpenOffice.org 1.0 Text Document\"," +
            "    \"extensions\": [\"sxw\"]," +
            "    \"mediaType\": \"application/vnd.sun.xml.writer\"," +
            "    \"inputFamily\": \"TEXT\"," +
            "    \"storeProperties\": {" +
            "      \"TEXT\": {" +
            "        \"FilterName\": \"StarOffice XML (Writer)\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"Microsoft Word 97-2003\"," +
            "    \"extensions\": [\"doc\"]," +
            "    \"mediaType\": \"application/msword\"," +
            "    \"inputFamily\": \"TEXT\"," +
            "    \"storeProperties\": {" +
            "      \"TEXT\": {" +
            "        \"FilterName\": \"MS Word 97\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"Microsoft Word 2007-2013 XML\"," +
            "    \"extensions\": [\"docx\"]," +
            "    \"mediaType\": \"application/vnd.openxmlformats-officedocument.wordprocessingml.document\"," +
            "    \"inputFamily\": \"TEXT\"," +
            "    \"storeProperties\": {" +
            "      \"TEXT\": {" +
            "        \"FilterName\": \"MS Word 2007 XML\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"Rich Text Format\"," +
            "    \"extensions\": [\"rtf\"]," +
            "    \"mediaType\": \"text/rtf\"," +
            "    \"inputFamily\": \"TEXT\"," +
            "    \"storeProperties\": {" +
            "      \"TEXT\": {" +
            "        \"FilterName\": \"Rich Text Format\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"WordPerfect\"," +
            "    \"extensions\": [\"wpd\"]," +
            "    \"mediaType\": \"application/wordperfect\"," +
            "    \"inputFamily\": \"TEXT\"" +
            "  }," +
            "  {" +
            "    \"name\": \"Plain Text\"," +
            "    \"extensions\": [\"txt\"]," +
            "    \"mediaType\": \"text/plain\"," +
            "    \"inputFamily\": \"TEXT\"," +
            "    \"loadProperties\": {" +
            "      \"FilterName\": \"Text (encoded)\"," +
            "      \"FilterOptions\": \"utf8\"" +
            "    }," +
            "    \"storeProperties\": {" +
            "      \"TEXT\": {" +
            "        \"FilterName\": \"Text (encoded)\"," +
            "        \"FilterOptions\": \"utf8\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"OpenDocument Spreadsheet\"," +
            "    \"extensions\": [\"ods\"]," +
            "    \"mediaType\": \"application/vnd.oasis.opendocument.spreadsheet\"," +
            "    \"inputFamily\": \"SPREADSHEET\"," +
            "    \"storeProperties\": {" +
            "      \"SPREADSHEET\": {" +
            "        \"FilterName\": \"calc8\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"OpenDocument Spreadsheet Template\"," +
            "    \"extensions\": [\"ots\"]," +
            "    \"mediaType\": \"application/vnd.oasis.opendocument.spreadsheet-template\"," +
            "    \"inputFamily\": \"SPREADSHEET\"," +
            "    \"storeProperties\": {" +
            "      \"SPREADSHEET\": {" +
            "        \"FilterName\": \"calc8_template\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"OpenDocument Spreadsheet Flat XML\"," +
            "    \"extensions\": [\"fods\"]," +
            "    \"mediaType\": \"application/vnd.oasis.opendocument.spreadsheet-flat-xml\"," +
            "    \"inputFamily\": \"SPREADSHEET\"," +
            "    \"storeProperties\": {" +
            "      \"SPREADSHEET\": {" +
            "        \"FilterName\": \"OpenDocument Spreadsheet Flat XML\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"OpenOffice.org 1.0 Spreadsheet\"," +
            "    \"extensions\": [\"sxc\"]," +
            "    \"mediaType\": \"application/vnd.sun.xml.calc\"," +
            "    \"inputFamily\": \"SPREADSHEET\"," +
            "    \"storeProperties\": {" +
            "      \"SPREADSHEET\": {" +
            "        \"FilterName\": \"StarOffice XML (Calc)\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"Microsoft Excel 97-2003\"," +
            "    \"extensions\": [\"xls\"]," +
            "    \"mediaType\": \"application/vnd.ms-excel\"," +
            "    \"inputFamily\": \"SPREADSHEET\"," +
            "    \"storeProperties\": {" +
            "      \"SPREADSHEET\": {" +
            "        \"FilterName\": \"MS Excel 97\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"Microsoft Excel 2007-2013 XML\"," +
            "    \"extensions\": [\"xlsx\"]," +
            "    \"mediaType\": \"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\"," +
            "    \"inputFamily\": \"SPREADSHEET\"," +
            "    \"storeProperties\": {" +
            "      \"SPREADSHEET\": {" +
            "        \"FilterName\": \"Calc MS Excel 2007 XML\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"Comma Separated Values\"," +
            "    \"extensions\": [\"csv\"]," +
            "    \"mediaType\": \"text/csv\"," +
            "    \"inputFamily\": \"SPREADSHEET\"," +
            "    \"loadProperties\": {" +
            "      \"FilterName\": \"Text - txt - csv (StarCalc)\"," +
            "      \"FilterOptions\": \"44,34,0\"" +
            "    }," +
            "    \"storeProperties\": {" +
            "      \"SPREADSHEET\": {" +
            "        \"FilterName\": \"Text - txt - csv (StarCalc)\"," +
            "        \"FilterOptions\": \"44,34,0\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"Tab Separated Values\"," +
            "    \"extensions\": [\"tsv\"]," +
            "    \"mediaType\": \"text/tab-separated-values\"," +
            "    \"inputFamily\": \"SPREADSHEET\"," +
            "    \"loadProperties\": {" +
            "      \"FilterName\": \"Text - txt - csv (StarCalc)\"," +
            "      \"FilterOptions\": \"9,34,0\"" +
            "    }," +
            "    \"storeProperties\": {" +
            "      \"SPREADSHEET\": {" +
            "        \"FilterName\": \"Text - txt - csv (StarCalc)\"," +
            "        \"FilterOptions\": \"9,34,0\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"OpenDocument Presentation\"," +
            "    \"extensions\": [\"odp\"]," +
            "    \"mediaType\": \"application/vnd.oasis.opendocument.presentation\"," +
            "    \"inputFamily\": \"PRESENTATION\"," +
            "    \"storeProperties\": {" +
            "      \"PRESENTATION\": {" +
            "        \"FilterName\": \"impress8\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"OpenDocument Presentation Template\"," +
            "    \"extensions\": [\"otp\"]," +
            "    \"mediaType\": \"application/vnd.oasis.opendocument.presentation-template\"," +
            "    \"inputFamily\": \"PRESENTATION\"," +
            "    \"storeProperties\": {" +
            "      \"PRESENTATION\": {" +
            "        \"FilterName\": \"impress8_template\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"OpenDocument Presentation Flat XML\"," +
            "    \"extensions\": [\"fodp\"]," +
            "    \"mediaType\": \"application/vnd.oasis.opendocument.presentation-flat-xml\"," +
            "    \"inputFamily\": \"PRESENTATION\"," +
            "    \"storeProperties\": {" +
            "      \"PRESENTATION\": {" +
            "        \"FilterName\": \"OpenDocument Presentation Flat XML\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"OpenOffice.org 1.0 Presentation\"," +
            "    \"extensions\": [\"sxi\"]," +
            "    \"mediaType\": \"application/vnd.sun.xml.impress\"," +
            "    \"inputFamily\": \"PRESENTATION\"," +
            "    \"storeProperties\": {" +
            "      \"PRESENTATION\": {" +
            "        \"FilterName\": \"StarOffice XML (Impress)\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"Microsoft PowerPoint 97-2003\"," +
            "    \"extensions\": [\"ppt\"]," +
            "    \"mediaType\": \"application/vnd.ms-powerpoint\"," +
            "    \"inputFamily\": \"PRESENTATION\"," +
            "    \"storeProperties\": {" +
            "      \"PRESENTATION\": {" +
            "        \"FilterName\": \"MS PowerPoint 97\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"Microsoft PowerPoint 2007-2013 XML\"," +
            "    \"extensions\": [\"pptx\"]," +
            "    \"mediaType\": \"application/vnd.openxmlformats-officedocument.presentationml.presentation\"," +
            "    \"inputFamily\": \"PRESENTATION\"," +
            "    \"storeProperties\": {" +
            "      \"PRESENTATION\": {" +
            "        \"FilterName\": \"Impress MS PowerPoint 2007 XML\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"OpenDocument Drawing\"," +
            "    \"extensions\": [\"odg\"]," +
            "    \"mediaType\": \"application/vnd.oasis.opendocument.graphics\"," +
            "    \"inputFamily\": \"DRAWING\"," +
            "    \"storeProperties\": {" +
            "      \"DRAWING\": {" +
            "        \"FilterName\": \"draw8\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"OpenDocument Drawing Template\"," +
            "    \"extensions\": [\"otg\"]," +
            "    \"mediaType\": \"application/vnd.oasis.opendocument.graphics-template\"," +
            "    \"inputFamily\": \"DRAWING\"," +
            "    \"storeProperties\": {" +
            "      \"DRAWING\": {" +
            "        \"FilterName\": \"draw8_template\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"OpenDocument Drawing Flat XML\"," +
            "    \"extensions\": [\"fodg\"]," +
            "    \"mediaType\": \"application/vnd.oasis.opendocument.graphics-flat-xml\"," +
            "    \"inputFamily\": \"DRAWING\"," +
            "    \"storeProperties\": {" +
            "      \"DRAWING\": {" +
            "        \"FilterName\": \"OpenDocument Drawing Flat XML\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"Scalable Vector Graphics\"," +
            "    \"extensions\": [\"svg\"]," +
            "    \"mediaType\": \"image/svg+xml\"," +
            "    \"storeProperties\": {" +
            "      \"DRAWING\": {" +
            "        \"FilterName\": \"draw_svg_Export\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"Portable Network Graphics\"," +
            "    \"extensions\": [\"png\"]," +
            "    \"mediaType\": \"image/png\"," +
            "    \"storeProperties\": {" +
            "      \"DRAWING\": {" +
            "        \"FilterName\": \"draw_png_Export\"" +
            "      }," +
            "      \"SPREADSHEET\": {" +
            "        \"FilterName\": \"calc_png_Export\"" +
            "      }," +
            "      \"PRESENTATION\": {" +
            "        \"FilterName\": \"impress_png_Export\"" +
            "      }," +
            "      \"TEXT\": {" +
            "        \"FilterName\": \"writer_png_Export\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"Joint Photographic Experts Group\"," +
            "    \"extensions\": [\"jpg\", \"jpeg\"]," +
            "    \"mediaType\": \"image/jpeg\"," +
            "    \"storeProperties\": {" +
            "      \"DRAWING\": {" +
            "        \"FilterName\": \"draw_jpg_Export\"" +
            "      }," +
            "      \"SPREADSHEET\": {" +
            "        \"FilterName\": \"calc_jpg_Export\"" +
            "      }," +
            "      \"PRESENTATION\": {" +
            "        \"FilterName\": \"impress_jpg_Export\"" +
            "      }," +
            "      \"TEXT\": {" +
            "        \"FilterName\": \"writer_jpg_Export\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"Tagged Image File Format\"," +
            "    \"extensions\": [\"tif\", \"tiff\"]," +
            "    \"mediaType\": \"image/tiff\"," +
            "    \"storeProperties\": {" +
            "      \"DRAWING\": {" +
            "        \"FilterName\": \"draw_tif_Export\"" +
            "      }," +
            "      \"PRESENTATION\": {" +
            "        \"FilterName\": \"impress_tif_Export\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"Graphics Interchange Format\"," +
            "    \"extensions\": [\"gif\"]," +
            "    \"mediaType\": \"image/gif\"," +
            "    \"storeProperties\": {" +
            "      \"DRAWING\": {" +
            "        \"FilterName\": \"draw_gif_Export\"" +
            "      }," +
            "      \"PRESENTATION\": {" +
            "        \"FilterName\": \"impress_gif_Export\"" +
            "      }" +
            "    }" +
            "  }," +
            "  {" +
            "    \"name\": \"Windows Bitmap\"," +
            "    \"extensions\": [\"bmp\"]," +
            "    \"mediaType\": \"image/bmp\"," +
            "    \"storeProperties\": {" +
            "      \"DRAWING\": {" +
            "        \"FilterName\": \"draw_bmp_Export\"" +
            "      }," +
            "      \"PRESENTATION\": {" +
            "        \"FilterName\": \"impress_bmp_Export\"" +
            "      }" +
            "    }" +
            "  }" +
            "]";

    private static FormatRegistry instance;

    /**
     * 获取默认的{@link FormatRegistry}实例.
     *
     * @return 默认的{@link FormatRegistry}.
     */
    public static FormatRegistry getInstance() {
        synchronized (FormatRegistry.class) {
            if (ObjectKit.isEmpty(instance)) {
                instance = JsonFormatRegistry.create(json);
            }
            return instance;
        }
    }

    /**
     * 设置默认的{@link FormatRegistry}实例.
     *
     * @param registry 要设置的{@link FormatRegistry}.
     */
    public static void setInstance(final FormatRegistry registry) {
        synchronized (FormatRegistry.class) {
            instance = registry;
        }
    }

}
