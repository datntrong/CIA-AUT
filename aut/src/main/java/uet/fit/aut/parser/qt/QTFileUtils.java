package uet.fit.aut.parser.qt;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import uet.fit.aut.config.FolderConfig;
import uet.fit.aut.parser.obj.AbstractFunctionNode;
import uet.fit.aut.parser.obj.ConstructorNode;
import uet.fit.aut.parser.obj.IVariableNode;
import uet.fit.aut.testdata.object.QTDataNode;
import uet.fit.aut.util.TemplateUtils;
import uet.fit.aut.util.Utils;
import uet.fit.config.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class QTFileUtils {

	public static void exportQTTypes(List<ConstructorNode> constructorNodes, String env) {

		constructorNodes.removeIf(constructorNode -> constructorNode.getSimpleName().startsWith("operator"));

		Collections.sort(constructorNodes, Comparator.comparing(AbstractFunctionNode::getSimpleName));

		String qtConstructorsFolder = Config.getHomePath() + File.separator + "qtConstructors";
		new File(qtConstructorsFolder).mkdirs();

		QTDataNode qtDataNode = new QTDataNode();
		List<QTDataNode.QTConstructorNode> qtConstructorNodes = new ArrayList<>();
		qtDataNode.setName("samp");

		for (ConstructorNode constructorNode : constructorNodes) {

			List<QTDataNode.ParamNode> paramNodes = new ArrayList<>();
			for (IVariableNode arg : constructorNode.getArguments()) {
				paramNodes.add(new QTDataNode.ParamNode(arg.getName(), arg.getRawType(), TemplateUtils.isTemplate(arg.getRawType())));
			}

			if (!constructorNode.getSimpleName().equals(qtDataNode.getName())) {
				qtDataNode.setConstructorNodes(qtConstructorNodes);
				String json = new GsonBuilder()
						.excludeFieldsWithoutExposeAnnotation()
						.setPrettyPrinting()
						.create()
						.toJson(qtDataNode, QTDataNode.class);
				//remove namespace
				String qtFileName = qtDataNode.getName().contains("::")?
						qtDataNode.getName().substring(qtDataNode.getName().lastIndexOf("::") + 2)
						: qtDataNode.getName();

				String path = qtConstructorsFolder + File.separator + qtFileName.replaceAll("\"","");
				if (!qtDataNode.getName().equals("samp")) Utils.writeContentToFile(json, path);

				qtDataNode.setName(constructorNode.getSimpleName());
				qtConstructorNodes.clear();
			}

			qtConstructorNodes.add(new QTDataNode.QTConstructorNode(constructorNode.getName(), paramNodes));
		}

	}

}
