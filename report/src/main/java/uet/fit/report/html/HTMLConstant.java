package uet.fit.report.html;

public class HTMLConstant {

	public static final String FILE_BEGIN = "<!DOCTYPE html>\n" + "<html>";
	public static final String HEAD_STYLE =
			"<head>\n" +
					"    <style>\n" +
					" body { margin-right: 25px;}\n" +
					"    </style>\n" +
					"  </head>";
	public static final String BODY_BEGIN = "<body>";
	public static final String BODY_END = "</body>";
	public static final String FILE_END = "</html>";

	public static final String VERTICAL_TABLE_CSS = ".vertical > table {\n" +
			"        width: 100%;\n" +
			"        border-collapse: collapse;\n" +
			"        outline: 1px solid black;\n" +
			"        margin-top: 20px;\n" +
			"        margin-bottom: 20px;\n" +
			"        font-family: Arial, Helvetica, sans-serif;\n" +
			"        font-size: 14px;\n" +
			"        font-weight: lighter;\n" +
			"      }\n" +
			"      .vertical > table > thead th {\n" +
			"        background-color: #338ac9;\n" +
			"        color: #ffffff;\n" +
			"        text-align: center;\n" +
			"        height: 30px;\n" +
			"      }\n" +
			"      .vertical > table > tbody {\n" +
			"        text-indent: 5px;\n" +
			"      }\n" +
			"      .vertical > table > tbody > tr {\n" +
			"        height: 30px;\n" +
			"      }\n" +
			"      .vertical > table > tbody > tr > th {\n" +
			"        background-color: #cfe2f4;\n" +
			"        text-align: left;\n" +
			"        border: 1px solid #b7b7b7;\n" +
			"      }\n" +
			"      .vertical > table > tbody > tr > td {\n" +
			"        text-align: left;\n" +
			"        border: 1px solid #b7b7b7;\n" +
			"		 word-break: break-all;\n" +
			"        white-space: pre-wrap;\n" +
			"      }";

	public static final String HORIZONTAL_TABLE_CSS = ".horizontal > table {\n" +
			"        width: 100%;\n" +
			"        border-collapse: collapse;\n" +
			"        outline: 1px solid black;\n" +
			"        margin-top: 20px;\n" +
			"        margin-bottom: 20px;\n" +
			"        font-family: Arial, Helvetica, sans-serif;\n" +
			"        font-size: 14px;\n" +
			"        font-weight: lighter;\n" +
			"      }\n" +
			"\n" +
			"      .horizontal > table > thead th {\n" +
			"        background-color: #338ac9;\n" +
			"        color: #ffffff;\n" +
			"        text-align: center;\n" +
			"        height: 30px;\n" +
			"      }\n" +
			"      .horizontal > table > tbody {\n" +
			"        text-indent: 5px;\n" +
			"      }\n" +
			"      .horizontal > table > tbody > tr {\n" +
			"        height: 30px;\n" +
			"      }\n" +
			"      .horizontal > table > tbody > tr > th {\n" +
			"        background-color: #cfe2f4;\n" +
			"        border: 1px solid #b7b7b7;\n" +
			"      }\n" +
			"      .horizontal > table > tbody > tr > td {\n" +
			"        text-align: left;\n" +
			"        border: 1px solid #b7b7b7;\n" +
			"		 word-break: break-all;\n" +
			"        white-space: pre-wrap;\n" +
			"      }";

}
