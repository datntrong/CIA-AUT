package uet.fit.report.html.component.page;

import uet.fit.report.html.HTMLConstant;
import uet.fit.report.html.component.table.Table;

import java.util.ArrayList;
import java.util.List;

public class Document {

	private String title;

	private List<Table> tableList = new ArrayList<>();

	public Document() {
	}

	public Document(List<Table> tableList) {
		this.tableList = tableList;
	}

	public List<Table> getTableList() {
		return tableList;
	}

	public void setTableList(List<Table> tableList) {
		this.tableList = tableList;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public String toHTML() {

		StringBuilder html = new StringBuilder();

		html.append(HTMLConstant.FILE_BEGIN);
		html.append(HTMLConstant.HEAD_STYLE);
		html.append(HTMLConstant.BODY_BEGIN);

		html.insert(html.indexOf("</style>"), HTMLConstant.VERTICAL_TABLE_CSS);
		html.insert(html.indexOf("</style>"), HTMLConstant.HORIZONTAL_TABLE_CSS);

		for (Table table : tableList) {
			html.append(table.toHTML());
		}

		html.append(HTMLConstant.BODY_END);
		html.append(HTMLConstant.FILE_END);

		return html.toString();
	}
}
