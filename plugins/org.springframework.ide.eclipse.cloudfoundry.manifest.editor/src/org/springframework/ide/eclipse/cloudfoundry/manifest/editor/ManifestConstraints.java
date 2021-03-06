/*******************************************************************************
 * Copyright (c) 2017 Spring IDE Developers
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Spring IDE Developers - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.cloudfoundry.manifest.editor;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.Assert;
import org.springframework.ide.eclipse.editor.support.yaml.ast.NodeUtil;
import org.springframework.ide.eclipse.editor.support.yaml.path.YamlPath;
import org.springframework.ide.eclipse.editor.support.yaml.path.YamlPathSegment;
import org.springframework.ide.eclipse.editor.support.yaml.path.YamlTraversal;
import org.springframework.ide.eclipse.editor.support.yaml.reconcile.YamlSchemaProblems;
import org.springframework.ide.eclipse.editor.support.yaml.schema.constraints.Constraint;
import org.yaml.snakeyaml.nodes.Node;

/**
 * Constraints for Manifest YAML structure
 *
 * @author Alex Boyko
 * @author Kris De Volder
 */
public class ManifestConstraints {

	public static Constraint mutuallyExclusive(String target, String... propertyIds) {
		return (dc, parent, node, type, problems) -> {
			Node targetNode = YamlPathSegment.keyAt(target).traverseNode(node);
			if (targetNode != null) {
				YamlTraversal conflictingTraversal = getConflictingNodesTraversal(dc.getPath(), propertyIds);
				List<Node> conflictingNodes = conflictingTraversal.traverseAmbiguously(dc.getAST()).collect(Collectors.toList());
				if (!conflictingNodes.isEmpty()) {
					Set<String> conflicts = conflictingNodes.stream().map(NodeUtil::asScalar).collect(Collectors.toCollection(TreeSet::new));
					problems.accept(YamlSchemaProblems.problem(
							ManifestYamlSchemaProblemsTypes.MUTUALLY_EXCLUSIVE_PROPERTY_PROBLEM,
							"Property cannot co-exist with properties " + conflicts, targetNode));
					for (Node cn : conflictingNodes) {
						problems.accept(YamlSchemaProblems.problem(
							ManifestYamlSchemaProblemsTypes.MUTUALLY_EXCLUSIVE_PROPERTY_PROBLEM,
							"Property cannot co-exist with property '" + target + "'", cn));
					}
				}
			}
		};
	}

	private static YamlTraversal getConflictingNodesTraversal(YamlPath path, String[] propertyIds) {
		Assert.isLegal(propertyIds.length > 0);
		YamlTraversal properties = null;
		for (String id : propertyIds) {
			properties = properties == null ? YamlPathSegment.keyAt(id) : properties.or(YamlPathSegment.keyAt(id));
		}
		YamlTraversal traversal = path.then(properties);
		if (path.size() > 2) {
			traversal = traversal.or(path.dropLast(2).then(properties));
		}
		return traversal;
	}

}
