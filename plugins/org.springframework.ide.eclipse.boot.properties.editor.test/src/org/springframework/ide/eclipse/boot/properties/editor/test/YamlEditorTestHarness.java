/*******************************************************************************
 * Copyright (c) 2015 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.boot.properties.editor.test;

import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.JavaElementLabels;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.springframework.ide.eclipse.boot.properties.editor.FuzzyMap;
import org.springframework.ide.eclipse.boot.properties.editor.HoverInfo;
import org.springframework.ide.eclipse.boot.properties.editor.IPropertyHoverInfoProvider;
import org.springframework.ide.eclipse.boot.properties.editor.PropertyInfo;
import org.springframework.ide.eclipse.boot.properties.editor.PropertyInfo.PropertySource;
import org.springframework.ide.eclipse.boot.properties.editor.SpringPropertyHoverInfo;
import org.springframework.ide.eclipse.boot.properties.editor.util.SpringPropertyIndexProvider;
import org.springframework.ide.eclipse.boot.properties.editor.util.TypeUtil;
import org.springframework.ide.eclipse.boot.properties.editor.util.TypeUtilProvider;
import org.springframework.ide.eclipse.yaml.editor.YamlHoverInfoProvider;
import org.springframework.ide.eclipse.yaml.editor.ast.YamlASTProvider;
import org.springframework.ide.eclipse.yaml.editor.ast.YamlFileAST;
import org.springframework.ide.eclipse.yaml.editor.reconcile.SpringYamlReconcileEngine;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Node;

public class YamlEditorTestHarness extends YamlOrPropertyEditorTestHarness {

	protected Yaml yaml = new Yaml();
	protected YamlASTProvider parser = new YamlASTProvider(yaml);
	protected IJavaProject javaProject = null;

	private SpringPropertyIndexProvider indexProvider = new SpringPropertyIndexProvider() {
		public FuzzyMap<PropertyInfo> getIndex(IDocument doc) {
			return index;
		}
	};

	private TypeUtilProvider typeUtil = new TypeUtilProvider() {
		public TypeUtil getTypeUtil(IDocument doc) {
			return new TypeUtil(javaProject);
		}
	};

	private IPropertyHoverInfoProvider hoverProvider = new YamlHoverInfoProvider(parser, indexProvider, documentContextFinder);

	protected SpringYamlReconcileEngine createReconcileEngine() {
		return new SpringYamlReconcileEngine(parser, indexProvider);
	}


	public class YamlEditor extends MockEditor {
		public YamlEditor(String string) {
			super(string);
		}

		public YamlFileAST parse() {
			return parser.getAST(this.document);
		}

		public int startOf(String nodeText) {
			return document.get().indexOf(nodeText);
		}

		public int endOf(String nodeText) {
			int start = startOf(nodeText);
			if (start>=0) {
				return start+nodeText.length();
			}
			return -1;
		}

		public int middleOf(String nodeText) {
			int start = startOf(nodeText);
			if (start>=0) {
				return start + nodeText.length()/2;
			}
			return -1;
		}

		public String textUnder(Node node) throws Exception {
			int start = node.getStartMark().getIndex();
			int end = node.getEndMark().getIndex();
			return document.get(start, end-start);
		}

		public String textUnder(IRegion r) throws BadLocationException {
			return document.get(r.getOffset(), r.getLength());
		}

		public IRegion getHoverRegion(int offset) {
			return hoverProvider.getHoverRegion(document, offset);
		}

		public HoverInfo getHoverInfo(int offset) {
			IRegion r = getHoverRegion(offset);
			if (r!=null) {
				return hoverProvider.getHoverInfo(document, r);
			}
			return null;
		}
	}


	public void assertNoHover(YamlEditor editor, String hoverOver) {
		HoverInfo info = editor.getHoverInfo(editor.middleOf(hoverOver));
		assertNull(info);
	}

	public void assertIsHoverRegion(YamlEditor editor, String string) throws BadLocationException {
		assertHoverRegionCovers(editor, editor.middleOf(string), string);
		assertHoverRegionCovers(editor, editor.startOf(string), string);
		assertHoverRegionCovers(editor, editor.endOf(string)-1, string);
	}

	public void assertHoverRegionCovers(YamlEditor editor, int offset, String expect) throws BadLocationException {
		IRegion r = editor.getHoverRegion(offset);
		String actual = editor.textUnder(r);
		assertEquals(expect, actual);
	}

	public void assertHoverContains(YamlEditor editor, String hoverOver, String expect) {
		HoverInfo info = editor.getHoverInfo(editor.middleOf(hoverOver));
		assertNotNull("No hover info for '"+ hoverOver +"'", info);
		assertContains(expect, info.getHtml());
	}

	//TODO: the link targets bits are almost dupiclates from the SpringProperties editor test harness.
	//  should be able to pull up with some reworking of the SpringProperties harness (i.e. add required
	//  abstract methods to MockEditor and make a subclass for SpringProperties harness.
	protected List<IJavaElement> getLinkTargets(YamlEditor editor, int pos) {
		HoverInfo info = editor.getHoverInfo(pos);
		if (info!=null && info instanceof SpringPropertyHoverInfo) {
			return info.getJavaElements();
		}
		return Collections.emptyList();
	}

	public void assertLinkTargets(YamlEditor editor, String hoverOver, String... expecteds) {
		int pos = editor.middleOf(hoverOver);
		assertTrue("Not found in editor: '"+hoverOver+"'", pos>=0);

		List<PropertySource> rawTargets = getRawLinkTargets(editor, pos);
		assertEquals(expecteds.length, rawTargets.size());

		List<IJavaElement> targets = getLinkTargets(editor, pos);
		assertEquals(expecteds.length, targets.size());
		for (int i = 0; i < expecteds.length; i++) {
			assertEquals(expecteds[i], JavaElementLabels.getElementLabel(targets.get(i), JavaElementLabels.DEFAULT_QUALIFIED | JavaElementLabels.M_PARAMETER_TYPES));
		}
	}

	private List<PropertySource> getRawLinkTargets(YamlEditor editor, int pos) {
		HoverInfo hover = editor.getHoverInfo(pos);
		if (hover!=null && hover instanceof SpringPropertyHoverInfo) {
			return ((SpringPropertyHoverInfo)hover).getSources();
		}
		return Collections.emptyList();
	}

}
