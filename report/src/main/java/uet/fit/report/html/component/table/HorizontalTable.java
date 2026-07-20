package uet.fit.report.html.component.table;

import uet.fit.report.html.data.TableData;

import java.util.List;

public class HorizontalTable extends Table {

	public HorizontalTable(TableData tableData) {
		super(tableData);
	}

	@Override
	public String toHTML() {
		StringBuilder result = new StringBuilder();
		String beginSection = "<div class=\"horizontal\">\n" +
				"<table id=\"" + header.toLowerCase().replaceAll(" ", "") + "\">";
		int numOfColumn = fieldNames.size();

		String headerSection =
				"<thead>\n" +
						"<tr>\n" +
						"<th colspan=\"" + numOfColumn + "\">" + header + "</th>\n" +
						"</tr>\n" +
						"</thead>\n";

		StringBuilder bodySection = new StringBuilder("<tbody>\n");

		bodySection.append("<tr>\n");
		for (String fieldName : fieldNames) {
			bodySection.append("<th>" + fieldName + "</th>\n");
		}
		bodySection.append("</tr>\n");

		for (List<String> record : records) {
			bodySection.append("<tr>\n");
			for (String value : record) {
				bodySection.append("<td>" + value + "</td>\n");
			}
			bodySection.append("</tr>\n");
		}

		bodySection.append("</tbody>\n");

		String endSection = "</table>\n" +
				"</div>";

		result.append(beginSection);
		result.append(headerSection);
		result.append(bodySection);
		result.append(endSection);

		return result.toString();
	}

	@Override
	public String getCSS() {
		return "HORIZONTAL_TABLE_CSS";
	}
}
