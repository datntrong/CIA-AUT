package uet.fit.report.html.component.table;

import uet.fit.report.html.data.TableData;

import java.util.List;

public class VerticalTable extends Table {
	public VerticalTable(TableData tableData) {
		super(tableData);
	}

	@Override
	public String toHTML() {
		final StringBuilder result = new StringBuilder()
				.append("<div class=\"vertical\">\n<table id=\"")
				.append(header.toLowerCase().replaceAll(" ", ""))
				.append("\"><thead>\n<tr>\n<th colspan=\"")
				.append(records.size() + 1)
				.append("\">")
				.append(header)
				.append("</th>\n</tr>\n</thead>\n<tbody>\n");
		for (int i = 0; i < fieldNames.size(); i++) {
			String fieldName = fieldNames.get(i);
			result.append("<tr>\n<th>").append(fieldName).append("</th>\n");
			for (List<String> record : records) {
				result.append("<td>").append(record.get(i)).append("</td>\n");
			}
			result.append("</tr>\n");
		}
		result.append("</tbody>\n</table>\n</div>");
		return result.toString();
	}

	@Override
	public String getCSS() {
		return "VERTICAL_TABLE_CSS";
	}
}
