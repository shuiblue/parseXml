package Main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.xml.bind.ValidationEvent;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Parser {
	static String TAG_CONCAT = "CONCAT";
	static String TAG_TARGET = "target-models";
	static String TAG_PREQS = "PREQS";
	static String TAG_RCP = "RCPs";
	static String TAG_P = "P";
	static String TAG_SELECT = "SELECT";
	static String TAG_TRUE = "TRUE";
	static String TAG_FALSE = "FALSE";
	static String TAG_LEFT = "LEFT";
	static String TAG_RIGHT = "RIGHT";
	static String TAG_VALUE = "value";

	public static void main(String[] args) throws Exception {

		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dbBuilder = dbFactory.newDocumentBuilder();
		Document doc = null;
		doc = dbBuilder
				.parse("file:///Users/shuruiz/Documents/SPL/Symake/samples/a/run123.xml");
		/*
		 * symake\_sample/runsample.xml  (works) 2
		 * paper/runpaper.xml   (works) 2
		 * testing/Actiongame/run1112.xml ( preqsNode : cube.h.gch) 3
		 * chatsystem/chatsys.xml (target[i] = "src/start/MainServer.java" (id=840)	)1
		 * 
		 */
		NodeList rule_list = doc.getElementsByTagName("RULE");
		ArrayList<XMLTreeNode> ruleTree = new ArrayList<XMLTreeNode>();

		// LinkedHashMap<String, TreeNode> map = new LinkedHashMap<String,
		// TreeNode>();
		HashMap<String, TreeNode> map = new HashMap<String, TreeNode>();
		// get rule
		for (int i = 0; i < rule_list.getLength(); i++) {

			ArrayList<XMLTreeNode> rcpTree = new ArrayList<XMLTreeNode>();
			ArrayList<XMLTreeNode> preqTree = new ArrayList<XMLTreeNode>();
			String targetName = "";

			// get target
			Node targetNode = getNodeByTagInSon(rule_list.item(i), TAG_TARGET);
			targetName = concatName(targetNode);// insert if

			// get PREQS
			Node preqsNode = getNodeByTagInSon(rule_list.item(i), TAG_PREQS);
			if (preqsNode.hasChildNodes()) {
				for (int t = 0; t < preqsNode.getChildNodes().getLength(); t++) {
					Node pNode = preqsNode.getChildNodes().item(t);
					NodeList pNode_list = pNode.getChildNodes();
					// P
					if (pNode.getNodeName().trim().equals(TAG_P)) {
						for (int p = 0; p < pNode_list.getLength(); p++) {
							// CONCAT
							if (pNode_list.item(p).getNodeName().trim()
									.equals(TAG_CONCAT)) {
								preqTree.add(new XMLTreeNode(
										concatName(pNode_list.item(p))));
								// SELECT
							} else if (pNode_list.item(p).getNodeName().trim()
									.equals(TAG_SELECT)) {

								Node select = pNode_list.item(p);
								preqTree.addAll(parseSelectNode(select));

							}
						}
					}
				}
			}
			Node rpcsNode = getNodeByTagInSon(rule_list.item(i), TAG_RCP);
			if (rpcsNode.hasChildNodes()) {
				for (int t = 0; t < rpcsNode.getChildNodes().getLength(); t++) {
					Node rpcNode = rpcsNode.getChildNodes().item(t);
					XMLTreeNode x = concat(rpcNode);
					if (x != null) {
						rcpTree.add(x);
					}
					// System.out.println();
				}
			}

			XMLTreeNode x = new XMLTreeNode(targetName, preqTree, rcpTree);
			ruleTree.add(x);
			combineRcpLeafs(x);
			// x.print("");
			//
			// x.printRcp("");
			String rcp = x.combinRcp();

			TreeNode t = new TargetTree(null, null, targetName, rcp);
			map.put(targetName, t);
			// System.out.println("rcp:\n"+x.combinRcp()+"\n");
		}
		for (Iterator<XMLTreeNode> i = ruleTree.iterator(); i.hasNext();) {
			XMLTreeNode node = i.next();

			for (Iterator<XMLTreeNode> j = node.getPrerequisite().iterator(); j
					.hasNext();) {
				XMLTreeNode preqsNode = j.next();

				String[] targets = preqsNode.getTarget().split(" ");
				String a = "";
				for (int t = 0; t < targets.length; t++) {
					// work for paper example
					a = targets[t]  ;
							//+ " " + targets[t + 1];

					// System.out.println(a);
					if (map.get(a) != null) {
						((TargetTree) map.get(a)).setName(targets[t]);
							//	+ targets[t + 1]);
						// map.get(node.getTarget()).getpChildren()
						// .add(map.get(a));
						TreeNode fatherNode = map.get(node.getTarget());
						TreeNode sonNode = map.get(a);
						fatherNode.getpChildren().add(sonNode);
						sonNode.setpParent(fatherNode);
					}
					//t = t + 1;
				}
			}
		}
		TargetTree root = new TargetTree(null, new ArrayList<TreeNode>());
		root.setName("root");

		for (String key : map.keySet()) {
			if (map.get(key).getpParent() == null) {
				root.getpChildren().add(map.get(key));
			}
		}
		root.defLayer(0);
		JTreeFrame tFrame = new JTreeFrame(root);

		root.printAllNode(root);

	}

	static public ArrayList<XMLTreeNode> combineRcpLeafs(XMLTreeNode root) {
		ArrayList<XMLTreeNode> add = new ArrayList<XMLTreeNode>();
		ArrayList<XMLTreeNode> del = new ArrayList<XMLTreeNode>();
		ArrayList<XMLTreeNode> tmp = null;
		if (root.getRcp() != null) {
			// ArrayList<XMLTreeNode> copy = new ArrayList<XMLTreeNode>();
			// copy.addAll(root.getRcp());
			for (Iterator<XMLTreeNode> i = root.getRcp().iterator(); i
					.hasNext();) {
				XMLTreeNode node = i.next();
				node.setParent(root);
				tmp = combineRcpLeafs(node);
				if (tmp != null) {
					add.addAll(tmp);
					del.add(node);
				}
			}
			root.getRcp().addAll(0, add);
			root.getRcp().removeAll(del);
		}

		if ((root.getTarget() == null || root.getTarget().equals(""))
				&& (root.getPrerequisite() == null || root.getPrerequisite()
						.isEmpty())) {

			for (Iterator<XMLTreeNode> i = root.getRcp().iterator(); i
					.hasNext();) {
				XMLTreeNode node = i.next();
				node.setParent(root.getParent());
			}
			return root.getRcp();

		}
		return null;
	}

	static public ArrayList<XMLTreeNode> parseSelectNode(Node select) {
		NodeList sele_children = select.getChildNodes();
		Node sel_list = sele_children.item(0);
		Node item = sel_list.getChildNodes().item(0);
		// ����ע�⣬getNodeByTagInSon����������㷨������������ȷ��ǰ���ǣ�sel_list item
		// cr���Ǹ��ڵ�ĵ�һ����Ҷ�ӽڵ�
		Node cr = getNodeByTagInSonWideFrist(select, "CR");
		String condition = "";
		String val = "";
		if (cr != null) {
			condition = cr.getAttributes().getNamedItem("condition").toString();
			val = cr.getAttributes().getNamedItem("isNot").toString();
		}
		ConditionTreeNode trueNode = null;
		ConditionTreeNode falseNode = null;
		ArrayList<XMLTreeNode> selectTree = new ArrayList<XMLTreeNode>();

		for (int s = 0; s < sele_children.getLength(); s++) {
			String concatName = concatName(sele_children.item(s));
			if (sele_children.item(s).getNodeName().trim().equals(TAG_TRUE)) {

				if (concatName == null || concatName.equals("")) {
					continue;
				}

				trueNode = new ConditionTreeNode(condition,
						val.equals("isNot=\"0\"") ? "true" : "false",
						concatName);
			} else if (sele_children.item(s).getNodeName().trim()
					.equals(TAG_FALSE)) {

				if (concatName == null || concatName.equals("")) {
					continue;
				}
				falseNode = new ConditionTreeNode(condition,
						val.equals("isNot=\"1\"") ? "false" : "true",
						concatName);
			} else if (sele_children.item(s).getNodeName().trim()
					.equals(TAG_CONCAT)) {

			}
		}
		if (trueNode != null) {
			selectTree.add(trueNode);
		}
		if (falseNode != null) {
			selectTree.add(falseNode);
		}
		return selectTree;
	}

	static public String concatName(Node node) {
		if (node == null) {
			return "";
		}
		if (node.getNodeName().trim().equals(TAG_CONCAT)) {
			String left = "";
			String right = "";

			for (int j = 0; j < node.getChildNodes().getLength(); j++) {
				if (node.getChildNodes().item(j).getNodeName().trim()
						.equals(TAG_LEFT)) {
					left = concatName(node.getChildNodes().item(j));
				} else if (node.getChildNodes().item(j).getNodeName().trim()
						.equals(TAG_RIGHT)) {
					right = concatName(node.getChildNodes().item(j));
				}
			}
			if (left.trim().equals("")) {
				return right;
			} else if (right.trim().equals("")) {
				return left;
			} else {
				return left + " " + right;
			}
		} else {
			if (node.hasChildNodes()) {
				NodeList nl = node.getChildNodes();
				for (int i = 0; i < nl.getLength(); i++) {
					if (nl.item(0).getNodeName().trim().equals(TAG_CONCAT)) {
						return concatName(nl.item(0));
					} else if (nl.item(0).getNodeName().trim()
							.equals(TAG_VALUE)) {
						return nl.item(0).getTextContent().trim();
					}
				}
				return "";
			} else {
				return "";
			}
		}
	}

	static public Node getNodeByTagInSon(Node node, String tagNam) {
		if (node.getNodeName().trim().equals(tagNam)) {
			return node;
		}
		if (!node.hasChildNodes()) {
			return null;
		}
		NodeList children = node.getChildNodes();
		Node n;
		for (int i = 0; i < children.getLength(); i++) {
			n = getNodeByTagInSon(children.item(i), tagNam);
			if (n != null) {
				return n;
			}
		}
		return null;
	}

	static public Node getNodeByTagInSonWideFrist(Node node, String tagNam) {
		if (node.getNodeName().trim().equals(tagNam)) {
			return node;
		}
		if (!node.hasChildNodes()) {
			return null;
		}
		NodeList children = node.getChildNodes();
		Node n;
		for (int i = 0; i < children.getLength(); i++) {
			if (children.item(i).getNodeName().trim().equals(tagNam)) {
				return children.item(i);
			}
		}
		for (int i = 0; i < children.getLength(); i++) {
			n = getNodeByTagInSon(children.item(i), tagNam);
			if (n != null) {
				return n;
			}
		}
		return null;
	}

	static public XMLTreeNode concat(Node n) {
		if (!n.hasChildNodes()) {
			String name = n.getTextContent().trim();
			if (name.equals("")) {
				return null;
			}
			return new XMLTreeNode(name);
		}
		NodeList chilren = n.getChildNodes();
		for (int i = 0; i < chilren.getLength(); i++) {
			Node nodeI = chilren.item(i);
			String nodeName = nodeI.getNodeName().trim();
			if (nodeName.equals(TAG_CONCAT)) {
				Node leftNode = getNodeByTagInSonWideFrist(nodeI, TAG_LEFT);
				Node rightNode = getNodeByTagInSonWideFrist(nodeI, TAG_RIGHT);
				XMLTreeNode leftTreeNode = concat(leftNode);
				XMLTreeNode rightTreeNode = concat(rightNode);

				XMLTreeNode x = new XMLTreeNode("",
						new ArrayList<XMLTreeNode>(),
						new ArrayList<XMLTreeNode>());
				if (leftTreeNode != null) {
					x.getRcp().add(leftTreeNode);
				}
				if (rightTreeNode != null) {
					x.getRcp().add(rightTreeNode);
				}
				return x;
			} else if (nodeName.equals(TAG_RIGHT) || nodeName.equals(TAG_LEFT)) {
				return concat(nodeI);
			} else if (nodeName.equals(TAG_SELECT)) {
				NodeList sele_children = nodeI.getChildNodes();
				Node sel_list = sele_children.item(0);
				Node item = sel_list.getChildNodes().item(0);
				// ����ע�⣬getNodeByTagInSon����������㷨������������ȷ��ǰ���ǣ�sel_list
				// item cr���Ǹ��ڵ�ĵ�һ����Ҷ�ӽڵ�
				Node cr = getNodeByTagInSonWideFrist(n, "CR");
				String condition = "";
				String val = "";
				if (cr != null) {
					condition = cr.getAttributes().getNamedItem("condition")
							.toString();
					val = cr.getAttributes().getNamedItem("isNot").toString();
				}
				ConditionTreeNode trueNode = null;
				ConditionTreeNode falseNode = null;
				ConditionTreeNode selectTree = new ConditionTreeNode("");
				selectTree.setRcp(new ArrayList<XMLTreeNode>());
				for (int s = 0; s < sele_children.getLength(); s++) {
					ConditionTreeNode cnode;
					XMLTreeNode tnode = null;
					if (sele_children.item(s).getNodeName().trim()
							.equals(TAG_TRUE)) {
						tnode = concat(sele_children.item(s));
						if (tnode != null) {
							cnode = new ConditionTreeNode(tnode);
							cnode.setCondition(condition);
							if (val.equals("isNot=\"0\"")) {
								cnode.setValue("true");
							} else {
								cnode.setValue("false");
							}
							selectTree.getRcp().add(cnode);
						}

					} else if (sele_children.item(s).getNodeName().trim()
							.equals(TAG_FALSE)) {

						tnode = concat(sele_children.item(s));
						if (tnode != null) {
							cnode = new ConditionTreeNode(tnode);
							cnode.setCondition(condition);

							if (val.equals("isNot=\"1\"")) {
								cnode.setValue("false");
							} else {
								cnode.setValue("true");
							}

							selectTree.getRcp().add(cnode);
						}

					}
				}
				return selectTree;
			} else if (nodeName.equals(TAG_TRUE) || nodeName.equals(TAG_FALSE)) {
				return concat(nodeI);
			} else if (nodeName.equals(TAG_VALUE)) {
				String name = nodeI.getTextContent().trim();
				if (name.equals("")) {
					return null;
				}
				return new XMLTreeNode(name);
			}
		}
		return null;
	}
}