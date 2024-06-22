package org.wrj.office;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jiaju Zhuang
 */
@Getter
@Setter
@EqualsAndHashCode
public class DemoExtraData {

    @ExcelProperty(index = 1)
    private String site;

    @ExcelProperty(index = 2)
    private String url;
}