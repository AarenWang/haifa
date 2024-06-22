package org.wrj.office;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.enums.CellExtraTypeEnum;

public class EasyExcelTest {

    public static void main(String[] args) {

        String fileName = "extra.xlsx";

        // 这里 需要指定读用哪个class去读，然后读取第一个sheet
        EasyExcel.read(fileName, DemoExtraData.class, new DemoExtraListener())
                // 需要读取批注 默认不读取
                .extraRead(CellExtraTypeEnum.COMMENT)
                // 需要读取超链接 默认不读取
                .extraRead(CellExtraTypeEnum.HYPERLINK)
                // 需要读取合并单元格信息 默认不读取
                .extraRead(CellExtraTypeEnum.MERGE).sheet().doRead();

    }
}
