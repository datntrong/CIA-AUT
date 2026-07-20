package uet.fit.report.html.data;

import java.util.List;

public class TableData {
	private final String header;
	private final List<String> fieldNames;
	private final TableRecords tableRecords;

	public TableData(String header, List<String> fieldNames, TableRecords tableRecords) {
		this.header = header;
		this.fieldNames = fieldNames;
		this.tableRecords = tableRecords;
	}

	public String getHeader() {
		return header;
	}

	public List<String> getFieldNames() {
		return fieldNames;
	}

	public TableRecords getTableRecords() {
		return tableRecords;
	}
}
