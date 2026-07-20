package uet.fit.report.html.component.table;


import uet.fit.report.html.data.TableData;
import uet.fit.report.html.data.TableRecords;

import java.util.List;

public abstract class Table {

    protected String header;

    protected List<String> fieldNames;

    protected TableRecords records;

    public Table(TableData tableData) {
        this.header = tableData.getHeader();
        this.fieldNames = tableData.getFieldNames();
        this.records = tableData.getTableRecords();
    }

    public abstract String toHTML();

    public abstract String getCSS();

}
