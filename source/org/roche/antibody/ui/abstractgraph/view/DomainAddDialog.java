/*******************************************************************************
 * Copyright C 2015, Roche pREDi (Roche Innovation Center Penzberg)
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
package org.roche.antibody.ui.abstractgraph.view;

import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import org.roche.antibody.services.ConfigFileService;
import org.roche.antibody.services.PreferencesService;

import com.quattroresearch.antibody.DomainDetectionStandalone;

/**
 * 
 * {@code DomainAddDialog}: dialog for selecting a domain from the library to add to the editor.
 * 
 * @author <b>Stefan Klostermann:</b> Stefan DOT Klostermann AT roche DOT com, Roche Pharma Research and Early
 *         Development - Informatics, Roche Innovation Center Penzberg
 * @author <b>Marco Lanig:</b> lanig AT quattro-research DOT com, quattro research GmbH
 * 
 * @version $Id$
 */
public class DomainAddDialog extends JDialog implements ActionListener {

  JTable domainTable;

  private JButton btnCancel;

  private JButton btnAddSelected;

  private DomainAddAction caller;

  private DomainDetectionStandalone domainDetection;

  /** Generated UID */
  private static final long serialVersionUID = 6102405755459856212L;

  public DomainAddDialog(JFrame owner, Dialog.ModalityType isModal,
      DomainAddAction caller) {
    super(owner, isModal);

    this.caller = caller;

    try {
      initComponents();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      JOptionPane.showMessageDialog(owner, "Configuration file could not be loaded while initialization.", "Exception while opening dialog.", JOptionPane.ERROR_MESSAGE);
    }
  }

  /**
   * UI Initialization
   * 
   * @throws FileNotFoundException
   */
  private void initComponents() throws FileNotFoundException {
    setResizable(false);
    setTitle("Add Domain Dialog");
    setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    getContentPane().setLayout(
        new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
    getContentPane().add(buildShowDomainsPanel());
    getContentPane().add(buildButtonPanel());
    pack();
  }

  private JPanel buildShowDomainsPanel() throws FileNotFoundException {

    JPanel showDomainsPanel = new JPanel();
    JScrollPane scrollPane;

    domainDetection =
        new DomainDetectionStandalone(null, null, PreferencesService.getInstance()/* , null */);
    try {
      domainDetection.makeBlastDatabase();
    } catch (FileNotFoundException e) {
      JOptionPane.showMessageDialog(this, "Please check the filepath in the Antibody Editor Settings", "Domain definition file not found", JOptionPane.ERROR_MESSAGE);
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this, "Creating blast database failed: " + e.getMessage(), "Blast failed.", JOptionPane.ERROR_MESSAGE);
    }
    Object[][] lib = ConfigFileService.getInstance().getCachedDomainLibAsRawData();

    JTable domainTable = null;
    if (lib != null) {
      domainTable = buildDomainTable(lib);
    }

    scrollPane = new JScrollPane(domainTable);
    showDomainsPanel.add(scrollPane);

    return showDomainsPanel;
  }

  private JPanel buildButtonPanel() {
    JPanel buttonPanel = new JPanel(new FlowLayout());
    btnCancel = new JButton("Cancel");
    btnCancel.addActionListener(this);
    btnAddSelected = new JButton("Add selected domain");
    btnAddSelected.addActionListener(this);

    buttonPanel.add(btnCancel);
    buttonPanel.add(btnAddSelected);

    return buttonPanel;
  }

  private JTable buildDomainTable(Object[][] lib) {
    DefaultTableModel model = new DefaultTableModel(new String[] {"Name",
        "Sequence", "Disulfide Bonds"}, lib.length);
    this.domainTable = new JTable(model);
    this.domainTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    for (int i = 0; i < lib.length; i++) {
      model.setValueAt(lib[i][ConfigFileService.INDEX_DOMAINLIB_SHORT_NAME], i, 0);
      model.setValueAt(lib[i][ConfigFileService.INDEX_DOMAINLIB_SEQUENCE], i, 1);
      model.setValueAt(lib[i][ConfigFileService.INDEX_DOMAINLIB_CYS_PATTERN], i, 2);
    }

    return domainTable;

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == btnCancel) {
      this.dispose();
    } else if (e.getSource() == btnAddSelected) {
      if (this.domainTable.getSelectedRowCount() <= 0)
        JOptionPane.showMessageDialog(this, "No row selected");
      else {
        this.caller.setSelectedSequence(this.domainTable.getValueAt(
            this.domainTable.getSelectedRow(), 1).toString());
        this.dispose();
      }
    }

  }
}
