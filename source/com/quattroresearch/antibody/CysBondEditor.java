/*******************************************************************************
 * Copyright C 2015, quattro research GmbH, Roche pREDi (Roche Innovation Center Penzberg)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/
package com.quattroresearch.antibody;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultCellEditor;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;

/**
 * 
 * {@code CysBondEditor}
 * 
 * @author <b>Stefan Klostermann:</b> Stefan DOT Klostermann AT roche DOT com, Roche Pharma Research and Early
 *         Development - Informatics, Roche Innovation Center Penzberg
 * @author <b>Anne Mund</b>, quattro research GmbH
 * @version $Id$
 */
public class CysBondEditor extends DefaultCellEditor {
	/** Generated UID */
	private static final long serialVersionUID = 4092838934350864756L;
	private JTextField textField;
	private int row;
	private JTable table;

	public CysBondEditor(JTextField field) {
		super(field);
		this.textField = field;
	}

	@Override
	public boolean stopCellEditing() {
		String temp = String.valueOf(table.getValueAt(row, 8));
		int cysNr = Integer.parseInt(temp);
		String text = textField.getText();
		text = text.replaceAll("\\s+", ""); // strip all whitespace, tab etc.
		String[] bonds = text.split(","); // split into different bonds
		String[] cys;
		List<Integer> allocatedCys = new ArrayList<Integer>();
		int cyspos;
		for (String bond : bonds) {
			if (bond.equals(""))
				break;
			cys = bond.split("-");
			if (cys.length > 2) {
				JOptionPane.showMessageDialog(null,
						"Please separate bond pairs by comma", "Invalid Input",
						JOptionPane.ERROR_MESSAGE);
				return false;
			} else {
				for (String place : cys) {
					try {
						cyspos = Integer.parseInt(place);
						if (cyspos > cysNr) {
							JOptionPane.showMessageDialog(null,
									"Not enough C in chain", "Invalid Input",
									JOptionPane.ERROR_MESSAGE);
							return false;
						} else if (allocatedCys.contains(cyspos)) {
							JOptionPane.showMessageDialog(null, "Allocated C "
									+ String.valueOf(cyspos)
									+ " more than once", "Invalid Input",
									JOptionPane.ERROR_MESSAGE);
							return false;
						} else {
							allocatedCys.add(cyspos);
						}
					} catch (java.lang.NumberFormatException e) {
						if (!place.equals("H") && !place.equals("LC")) {
							JOptionPane.showMessageDialog(null,
									"Invalid character used", "Invalid Input",
									JOptionPane.ERROR_MESSAGE);
							return false;
						}
					}
				}
			}
		}
		textField.setText(text);
		return super.stopCellEditing();
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		this.row = row;
		this.table = table;
		return super.getTableCellEditorComponent(table, value, isSelected, row,
				column);
	}

}
