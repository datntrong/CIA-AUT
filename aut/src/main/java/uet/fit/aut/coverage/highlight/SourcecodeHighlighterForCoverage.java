package uet.fit.aut.coverage.highlight;

import uet.fit.aut.autogen.cfg.ICFG;
import uet.fit.aut.autogen.cfg.object.ConditionCfgNode;
import uet.fit.aut.autogen.cfg.object.ICfgNode;
import uet.fit.aut.autogen.cfg.object.NormalCfgNode;
import uet.fit.aut.env.CoverageType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SourcecodeHighlighterForCoverage extends AbstractHighlighterForSourcecodeLevel {

	// all cfg of functions in source code file after updating visited statements and branches
	protected List<ICFG> allCFG;
	protected String typeOfCoverage;

	@Override
	public void highlight() {
		if (sourcecode == null || sourcecode.length() == 0 || testpathContent == null ||
				testpathContent.length() == 0 || sourcecodePath == null || !(new File(sourcecodePath).exists()))
			return;

		List<HighlightedOffset> offsets = getVisitedInstructionsInASourcecodeFile(getAllCFG());
		offsets = arrangeByStartingOffset(offsets);

		for (HighlightedOffset offset : offsets) {
			int start = offset.getStartOffset();
			int end = offset.getEndOffset();
			String pre = sourcecode.substring(0, start);
			String after = sourcecode.substring(end);
			String middle = sourcecode.substring(start, end);

			if (offset instanceof HighlightedOffsetForNormalStatement) {
				middle = "HIGHLIGHT_NORMAL_STATEMENT_BEGIN" + middle + "HIGHLIGHT_NORMAL_STATEMENT_END";
			} else if (offset instanceof HighlightedOffsetForBranch) {
				switch (typeOfCoverage) {
					case CoverageType.STATEMENT: {
						middle = "HIGHLIGHT_CONDITIONAL_STATEMENT_BEGIN" + middle + "HIGHLIGHT_CONDITIONAL_STATEMENT_END";
						break;
					}

					case CoverageType.BASIS_PATH:
					case CoverageType.BRANCH:
					case CoverageType.MCDC: {
						if (((HighlightedOffsetForBranch) offset).isVisitedFalse() && ((HighlightedOffsetForBranch) offset).isVisitedTrue())
							middle = "TRUE_MARKER" + "FALSE_MARKER" + "HIGHLIGHT_CONDITIONAL_STATEMENT_BEGIN" + middle + "HIGHLIGHT_CONDITIONAL_STATEMENT_END";
						else if (((HighlightedOffsetForBranch) offset).isVisitedFalse())
							middle = "FALSE_MARKER" + "HIGHLIGHT_CONDITIONAL_STATEMENT_BEGIN" + middle + "HIGHLIGHT_CONDITIONAL_STATEMENT_END";
						else if (((HighlightedOffsetForBranch) offset).isVisitedTrue())
							middle = "TRUE_MARKER" + "HIGHLIGHT_CONDITIONAL_STATEMENT_BEGIN" + middle + "HIGHLIGHT_CONDITIONAL_STATEMENT_END";
						break;
					}
				}

			}

			sourcecode = pre + middle + after;
		}

		// credit: http://ijotted.blogspot.com/2012/05/which-characters-should-be-escaped.html
		sourcecode = sourcecode.
				replace("&", "&amp;").replace("'", "&#39;")
				.replace("\"", "&quot;").
				replace(">", "&gt;").replace("<", "&lt;");

		sourcecode = sourcecode.replace("HIGHLIGHT_NORMAL_STATEMENT_BEGIN", highlightSignalStartForNormalStatement)
				.replace("HIGHLIGHT_NORMAL_STATEMENT_END", highlightSignalEnd);
		sourcecode = sourcecode.replace("HIGHLIGHT_CONDITIONAL_STATEMENT_BEGIN", highlightSignalStartForConditionalStatement)
				.replace("HIGHLIGHT_CONDITIONAL_STATEMENT_END", highlightSignalEnd);
		sourcecode = sourcecode.replace("TRUE_MARKER", trueMarker).replace("FALSE_MARKER", falseMarker);

		sourcecode = addLineNumber(sourcecode);

		fullHighlightedSourcecode = addPre(sourcecode);

		simpliedHighlightedSourcecode = removeRedundantLines(sourcecode);
		simpliedHighlightedSourcecode = addPre(simpliedHighlightedSourcecode);
	}

	protected List<HighlightedOffset> getVisitedInstructionsInASourcecodeFile(List<ICFG> allCFG) {
		List<HighlightedOffset> offsets = new ArrayList<>();
		for (ICFG cfg : allCFG) {
			int deltaOffset = 0;
			for (ICfgNode cfgNode : cfg.getAllNodes())
				if (cfgNode instanceof ConditionCfgNode) {
					if (cfgNode.isVisited()) {
						HighlightedOffsetForBranch offsetForBranch = new HighlightedOffsetForBranch();

						offsetForBranch.setStartOffset(cfgNode.getAstLocation().getNodeOffset() + deltaOffset);
						offsetForBranch.setEndOffset(cfgNode.getAstLocation().getNodeLength() + cfgNode.getAstLocation().getNodeOffset() + deltaOffset);
						offsets.add(offsetForBranch);

						if (((ConditionCfgNode) cfgNode).isVisitedFalseBranch() && ((ConditionCfgNode) cfgNode).isVisitedTrueBranch()) {
							offsetForBranch.setVisitedTrue(true);
							offsetForBranch.setVisitedFalse(true);
						} else if (((ConditionCfgNode) cfgNode).isVisitedTrueBranch()) {
							offsetForBranch.setVisitedTrue(true);
						} else if (((ConditionCfgNode) cfgNode).isVisitedFalseBranch()) {
							offsetForBranch.setVisitedFalse(true);
						}
					}
				} else if (cfgNode instanceof NormalCfgNode && cfgNode.isVisited()) {
					HighlightedOffsetForNormalStatement offsetForNormalStatement = new HighlightedOffsetForNormalStatement();
					offsetForNormalStatement.setStartOffset(cfgNode.getAstLocation().getNodeOffset() + deltaOffset);
					offsetForNormalStatement.setEndOffset(cfgNode.getAstLocation().getNodeLength() + cfgNode.getAstLocation().getNodeOffset() + deltaOffset);
					offsets.add(offsetForNormalStatement);
				}
		}

		return offsets;
	}

	public List<ICFG> getAllCFG() {
		return allCFG;
	}

	public void setAllCFG(List<ICFG> allCFG) {
		this.allCFG = allCFG;
	}

	@Override
	public void setTestpathContent(String testpathContent) {
		super.setTestpathContent(testpathContent);
	}

	@Override
	public String getTestpathContent() {
		return super.getTestpathContent();
	}

	public void setTypeOfCoverage(String typeOfCoverage) {
		this.typeOfCoverage = typeOfCoverage;
	}

	public String getTypeOfCoverage() {
		return typeOfCoverage;
	}
}

